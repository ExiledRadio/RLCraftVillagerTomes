package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.Tome;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Turns what a villager knows into what a villager sells.
 *
 * <p>Vanilla has no hook for "a villager is about to show its trade list", and the list
 * itself ({@code EntityVillager.buyingList}) is rebuilt by the game whenever the villager
 * levels up or restocks. So rather than adding trades once and hoping they survive, this
 * runs immediately before the trade screen opens and reconciles the list against the
 * capability every single time. Whatever the game did to the list in between, the taught
 * trades are correct by the time the player sees them.
 *
 * <p>Reconciling rather than clearing-and-rebuilding matters for one specific reason: a
 * merchant recipe carries how many times it has been used, and throwing it away to build
 * an identical one resets that to zero. A player could exhaust a taught trade, step back,
 * click again and have full stock - which would quietly make MAX_TRADE_USES meaningless.
 * An unchanged trade is therefore left strictly alone.
 *
 * <p>The list has to be reached by reflection, and there is no way around it.
 * {@code EntityVillager.getRecipes} looks like the obvious accessor but does not return the
 * villager's list - Forge routes it through {@code ForgeEventFactory.listTradeOffers},
 * which shallow-copies into a fresh {@code MerchantRecipeList} before firing
 * {@code MerchantTradeOffersEvent}. Adding to what it hands back adds to a copy that is
 * discarded the moment the caller is finished with it. The shared recipe <em>objects</em>
 * can still be mutated through it, which is why restocking helpers get away with using it,
 * but adding and removing entries cannot. {@code setRecipes} is no help either: on
 * {@code EntityVillager} it is client-only and has an empty body.
 */
public final class TomeTradeSync {

    /** {@code EntityVillager.buyingList}, as named in a dev run. */
    private static final String BUYING_LIST_DEOBF = "buyingList";
    /** The same field as named in the shipped game. Verified against the SRG binary. */
    private static final String BUYING_LIST_SRG = "field_70963_i";

    private static final Field BUYING_LIST = findBuyingListField();

    /**
     * How many spare uses an unlocked trade is kept topped up to.
     *
     * <p>Large enough that no realistic session gets near it, so the top-up happens once and
     * then never again. It is not {@code Integer.MAX_VALUE} because the value is added to an
     * int that gets written to NBT and read back; leaving several orders of magnitude of room
     * means repeated top-ups over a world's lifetime can never approach an overflow.
     */
    private static final int UNLOCK_HEADROOM = 100000;

    private TomeTradeSync() {
    }

    /**
     * Resolves the trade list field under whichever name the current environment uses.
     *
     * <p>The argument order matters and is easy to get backwards.
     * {@code ReflectionHelper.findField} does not try both names - it picks one, using the
     * first in a deobfuscated (dev) environment and the second everywhere else:
     *
     * <pre>
     *   String nameToFind = FMLLaunchHandler.isDeobfuscatedEnvironment()
     *           ? fieldName : MoreObjects.firstNonNull(fieldObfName, fieldName);
     * </pre>
     *
     * <p>So the deobfuscated name goes first and the SRG name second. Reversed, it works
     * perfectly in a dev run and fails on every single villager in a real game, which is
     * the worst possible way for it to be wrong.
     */
    private static Field findBuyingListField() {
        try {
            return ReflectionHelper.findField(EntityVillager.class,
                    BUYING_LIST_DEOBF, BUYING_LIST_SRG);
        } catch (Throwable t) {
            // Survivable: taught trades stop appearing, and the log says why, rather than
            // every villager interaction in the world throwing.
            RLCraftVillagerTomes.LOGGER.error("Could not find EntityVillager's trade list field. "
                    + "Taught trades will not be offered. This means the mod is running against "
                    + "an unexpected Minecraft build.", t);
            return null;
        }
    }

    /**
     * The villager's real trade list, populating it first if the villager has never traded.
     *
     * <p>Returns null when reflection is unavailable, which every caller treats as "do
     * nothing" rather than as an error worth throwing over.
     */
    private static MerchantRecipeList realBuyingList(EntityVillager villager, EntityPlayer player) {
        if (BUYING_LIST == null) {
            return null;
        }
        try {
            MerchantRecipeList list = (MerchantRecipeList) BUYING_LIST.get(villager);
            if (list == null) {
                // Vanilla builds the list lazily on the first getRecipes call. The returned
                // copy is useless to us, but the side effect on the field is exactly what we
                // are after.
                villager.getRecipes(player);
                list = (MerchantRecipeList) BUYING_LIST.get(villager);
            }
            return list;
        } catch (IllegalAccessException e) {
            RLCraftVillagerTomes.LOGGER.error("Could not read a villager's trade list.", e);
            return null;
        }
    }

    /**
     * Brings a villager's trade list in line with its tomes.
     *
     * @param player the player about to see the list; needed because vanilla's own trade
     *               generation takes one, not because anything here is per-player
     */
    public static void sync(EntityVillager villager, EntityPlayer player, ITomeKnowledge tomes) {
        if (tomes == null) {
            return;
        }
        MerchantRecipeList recipes = realBuyingList(villager, player);
        if (recipes == null) {
            debug("sync aborted: could not reach the villager's trade list (reflection {})",
                    BUYING_LIST == null ? "unavailable" : "returned null");
            return;
        }
        int sizeBefore = recipes.size();

        // Taught trades are pulled out first, then put back. Pulling them out is what makes
        // a level-up work: the old Unbreaking II trade is removed on the way through and
        // the Unbreaking III one added at the end, rather than the villager selling both.
        List<MerchantRecipe> existing = extractTomeTrades(recipes, tomes);

        if (!ModConfig.ENABLE_LEARNING) {
            debug("sync stopped: ENABLE_LEARNING is off, {} taught trade(s) stripped",
                    Integer.valueOf(existing.size()));
            // Master switch off. The tomes stay in the capability untouched - they are just
            // not offered - so flipping the switch back on restores every villager exactly
            // as it was.
            return;
        }

        int added = 0;
        int reused = 0;
        for (Tome tome : tomes.view()) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(tome.getEnchantment());
            if (enchantment == null) {
                debug("sync skipped {}: no such enchantment in the registry",
                        tome.getEnchantment());
                // The mod that added this enchantment is not loaded right now. The entry
                // stays in the capability so it comes back if the mod does; it simply has
                // nothing to sell in the meantime.
                continue;
            }
            int level = tome.getLevel();
            MerchantRecipe desired = buildRecipe(enchantment, level);
            MerchantRecipe reusable = findReusable(existing, enchantment, level, desired);
            MerchantRecipe live = reusable != null ? reusable : desired;
            if (ModConfig.NEVER_LOCK_TAUGHT_TRADES) {
                keepUnlocked(live);
            }
            recipes.add(live);
            added++;
            if (reusable != null) {
                reused++;
            }
        }

        if (ModConfig.NEVER_LOCK_ANY_TRADE) {
            // Covers the villager's ordinary trades too. Doing it here rather than in a
            // second pass means a villager that has tomes is handled in one walk.
            for (MerchantRecipe recipe : recipes) {
                keepUnlocked(recipe);
            }
        }

        debug("sync done: {} tome(s), trades {} -> {}, {} added ({} reused), list class {}",
                Integer.valueOf(tomes.count()), Integer.valueOf(sizeBefore),
                Integer.valueOf(recipes.size()), Integer.valueOf(added),
                Integer.valueOf(reused), recipes.getClass().getName());
    }

    /**
     * Unlocks every trade a villager already has, without creating trades it does not.
     *
     * <p>For villagers with no tomes, where {@link #sync} has nothing to do but
     * NEVER_LOCK_ANY_TRADE still applies. Reads the field directly and gives up when it is
     * null: forcing a trade list into existence would roll the villager's career earlier
     * than vanilla intends, and unlocking a list that does not exist yet is meaningless
     * anyway - it will be generated unlocked the moment anything asks for it.
     */
    public static void unlockExistingTrades(EntityVillager villager) {
        if (!ModConfig.NEVER_LOCK_ANY_TRADE || BUYING_LIST == null) {
            return;
        }
        try {
            MerchantRecipeList list = (MerchantRecipeList) BUYING_LIST.get(villager);
            if (list == null) {
                return;
            }
            for (MerchantRecipe recipe : list) {
                keepUnlocked(recipe);
            }
            debug("unlocked {} existing trade(s)", Integer.valueOf(list.size()));
        } catch (IllegalAccessException e) {
            RLCraftVillagerTomes.LOGGER.error("Could not read a villager's trade list.", e);
        }
    }

    /**
     * Pushes a trade's use limit far enough ahead of its use count that it cannot lock.
     *
     * <p>Vanilla's own test is {@code isRecipeDisabled() { return toolUses >= maxTradeUses; }},
     * and {@code increaseMaxTradeUses} is the only public way to move that line - there is no
     * setter for the use count. Raising the limit rather than zeroing the count also means
     * this works on a recipe read back from a villager's saved NBT, which arrives as a plain
     * {@code MerchantRecipe} with none of this mod's types involved.
     *
     * <p>Idempotent: once the headroom is there, later calls do nothing.
     */
    private static void keepUnlocked(MerchantRecipe recipe) {
        int remaining = recipe.getMaxTradeUses() - recipe.getToolUses();
        if (remaining < UNLOCK_HEADROOM) {
            recipe.increaseMaxTradeUses(UNLOCK_HEADROOM - remaining);
        }
    }

    static void debug(String message, Object... args) {
        if (ModConfig.DEBUG_LOGGING) {
            RLCraftVillagerTomes.LOGGER.info("[debug] " + message, args);
        }
    }

    /**
     * The level a villager already sells an enchantment at under its own steam, or 0.
     *
     * <p>Used to refuse a teaching attempt before anything is spent. Once slots have to be
     * bought, letting somebody pay one to replace a trade the villager could already do is
     * a genuinely expensive mistake, and it is cheaper to stop than to explain.
     *
     * <p>Only meaningful for an enchantment the villager has NOT been taught. Once it is a
     * tome, any trade selling it is ours - which is also why this cannot be asked about an
     * upgrade, and does not need to be: a natural trade would have blocked the original
     * teach.
     */
    public static int naturalTradeLevel(EntityVillager villager, EntityPlayer player,
                                        ITomeKnowledge tomes, ResourceLocation enchantment) {
        if (enchantment == null || tomes.knows(enchantment)) {
            return 0;
        }
        MerchantRecipeList recipes = realBuyingList(villager, player);
        if (recipes == null) {
            return 0;
        }
        for (MerchantRecipe recipe : recipes) {
            if (enchantment.equals(soleEnchantmentSold(recipe))) {
                Map<Enchantment, Integer> sold =
                        EnchantmentHelper.getEnchantments(recipe.getItemToSell());
                Integer level = sold.values().iterator().next();
                return level == null ? 1 : level.intValue();
            }
        }
        return 0;
    }

    /**
     * Removes and returns every trade in the list that this mod is responsible for.
     *
     * <p>"Responsible for" means: sells an enchanted book carrying exactly one enchantment,
     * and that enchantment is one the villager has been taught. There is no hidden marker
     * on the stack, deliberately - a marker would have to be either on the emeralds, which
     * would stop the player's own emeralds matching the trade, or on the book the player
     * receives, which would leave a junk tag on every book bought this way and stop it
     * stacking with normal ones. Matching on content costs nothing and leaves the item
     * clean.
     *
     * <p>Matching on the enchantment rather than the enchantment and level is what makes a
     * level-up clean: the old Unbreaking II trade has to come out when the tome becomes
     * Unbreaking III, and by then nothing holds level II to match against.
     *
     * <p>The consequence is that a natural trade for an enchantment already taught here
     * would be swept away with ours. Teaching an enchantment a villager already sells is
     * refused outright - see {@link #naturalTradeLevel} - so the only way to reach that
     * state is for the villager to level up and happen to roll the same enchantment
     * afterwards, out of the several hundred in the registry. Left alone deliberately:
     * guarding it would mean telling our trades from vanilla ones by price, which breaks
     * the moment somebody edits the config.
     */
    private static List<MerchantRecipe> extractTomeTrades(MerchantRecipeList recipes,
                                                          ITomeKnowledge tomes) {
        List<MerchantRecipe> pulled = new ArrayList<MerchantRecipe>();
        Iterator<MerchantRecipe> iterator = recipes.iterator();
        while (iterator.hasNext()) {
            MerchantRecipe recipe = iterator.next();
            ResourceLocation sold = soleEnchantmentSold(recipe);
            if (sold != null && tomes.knows(sold)) {
                pulled.add(recipe);
                iterator.remove();
            }
        }
        return pulled;
    }

    /**
     * The registry name of the single enchantment a trade sells on a book, or null when the
     * trade is not a single-enchantment book trade at all.
     *
     * <p>Books with two or more enchantments never match, which keeps hand-written pack
     * trades and modded multi-enchantment offers out of the way.
     */
    private static ResourceLocation soleEnchantmentSold(MerchantRecipe recipe) {
        ItemStack sell = recipe.getItemToSell();
        if (sell.isEmpty() || sell.getItem() != Items.ENCHANTED_BOOK) {
            return null;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(sell);
        if (enchantments.size() != 1) {
            return null;
        }
        Enchantment enchantment = enchantments.keySet().iterator().next();
        return enchantment == null ? null : enchantment.getRegistryName();
    }

    /**
     * Picks an already-existing trade out of the pulled set when it is identical to what we
     * were about to build, so its remaining uses survive.
     *
     * <p>Anything that differs - the level went up, the price changed because somebody
     * edited the config, the second input item changed - is not reused, and the player gets
     * a freshly stocked trade at the new terms. That is the right call: a trade whose price
     * just changed has no meaningful "uses so far" to preserve.
     */
    private static MerchantRecipe findReusable(List<MerchantRecipe> candidates,
                                               Enchantment enchantment, int level,
                                               MerchantRecipe desired) {
        // An explicit iterator, because the match is removed from the candidate set on the
        // way out - a for-each here would throw the moment it found anything.
        Iterator<MerchantRecipe> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            MerchantRecipe candidate = iterator.next();
            if (levelSold(candidate, enchantment) != level) {
                continue;
            }
            if (!ItemStack.areItemStacksEqual(candidate.getItemToBuy(), desired.getItemToBuy())) {
                continue;
            }
            if (!ItemStack.areItemStacksEqual(candidate.getSecondItemToBuy(),
                    desired.getSecondItemToBuy())) {
                continue;
            }
            // A recipe read back from the villager's saved NBT is a plain MerchantRecipe and
            // always claims to reward experience, so this is also what re-applies
            // TRADE_GRANTS_XP after a world reload.
            if (candidate.getRewardsExp() != ModConfig.TRADE_GRANTS_XP) {
                continue;
            }
            iterator.remove();
            return candidate;
        }
        return null;
    }

    /** The level a trade sells the given enchantment at, or 0 if it does not sell it. */
    private static int levelSold(MerchantRecipe recipe, Enchantment enchantment) {
        ItemStack sell = recipe.getItemToSell();
        if (sell.isEmpty() || sell.getItem() != Items.ENCHANTED_BOOK) {
            return 0;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(sell);
        Integer found = enchantments.get(enchantment);
        return found == null ? 0 : found.intValue();
    }

    /**
     * Builds the trade a villager should be offering for one tome, at current config prices.
     *
     * <p>Input order matches vanilla's own librarian trade, which is
     * {@code new MerchantRecipe(new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD, j),
     * enchantedBook)} - the plain book first, the emeralds second. A taught trade then looks
     * the same as a naturally rolled one instead of reading backwards next to it.
     *
     * <p>With no extra input configured the emeralds have to take the first slot instead: a
     * merchant recipe's first input is the one that is not allowed to be empty.
     */
    private static MerchantRecipe buildRecipe(Enchantment enchantment, int level) {
        ItemStack emeralds = new ItemStack(Items.EMERALD,
                ModConfig.getEmeraldCost(enchantment, level));
        ItemStack extra = ModConfig.getExtraInput();
        ItemStack sold = ItemEnchantedBook.getEnchantedItemStack(
                new EnchantmentData(enchantment, level));

        ItemStack firstInput = extra.isEmpty() ? emeralds : extra;
        ItemStack secondInput = extra.isEmpty() ? ItemStack.EMPTY : emeralds;

        return new TomeMerchantRecipe(firstInput, secondInput, sold, ModConfig.MAX_TRADE_USES,
                ModConfig.TRADE_GRANTS_XP);
    }

    /**
     * A merchant recipe whose experience reward is ours to decide.
     *
     * <p>Vanilla's {@code MerchantRecipe} hard-codes {@code rewardsExp} to true in every
     * constructor and offers no setter, and the field is private. Subclassing and
     * overriding the getter is a good deal cleaner than reflecting on an obfuscated field,
     * and the only thing lost is that the flag is not written to NBT - which the
     * reconciliation in {@link #findReusable} already handles by replacing any reloaded
     * trade whose flag disagrees with the config.
     */
    private static final class TomeMerchantRecipe extends MerchantRecipe {

        private final boolean rewardsExp;

        private TomeMerchantRecipe(ItemStack buy, ItemStack secondBuy, ItemStack sell,
                                   int maxUses, boolean rewardsExp) {
            super(buy, secondBuy, sell, 0, maxUses);
            this.rewardsExp = rewardsExp;
        }

        @Override
        public boolean getRewardsExp() {
            return rewardsExp;
        }
    }
}
