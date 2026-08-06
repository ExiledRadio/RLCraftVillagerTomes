package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handing a villager a book.
 *
 * <p>Runs on {@link PlayerInteractEvent.EntityInteract}, which Forge fires before the game
 * gets as far as opening the trade screen - so cancelling it is enough to turn a click that
 * would have started a trade into a click that teaches a book instead.
 *
 * <p>Nothing here fires on a click with anything other than an enchanted book in hand. That
 * is the whole compatibility story: villagers behave exactly as they always did unless you
 * are deliberately holding a book at one.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class TomeLearningHandler {

    private static final String PREFIX =
            TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;

    private TomeLearningHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Every decision is the server's. The client fires this event too, and letting it
        // run there would consume the book twice over in single player.
        if (event.getWorld().isRemote || !(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null) {
            // Nothing to read or write, so this is not a villager we know anything about.
            // Falling through leaves vanilla entirely alone, which is the least disruptive
            // thing to do about a state that should not happen.
            return;
        }

        boolean teaching = ModConfig.ENABLE_LEARNING && !held.isEmpty()
                && held.getItem() == Items.ENCHANTED_BOOK && isTeachingClick(player);

        if (!teaching) {
            // This click is on its way to the trade screen. Reconciling the list now is what
            // makes taught trades show up at all - see TomeTradeSync for why it has to happen
            // on every open rather than once at teaching time.
            //
            // Skipped entirely for a villager that has never been taught anything, so the
            // overwhelming majority of villager clicks in a world do no extra work. Also
            // skipped for children and villagers already in a trade, where vanilla would not
            // build a trade list either and doing it early would only bring their career roll
            // forward.
            TomeTradeSync.debug("interact: not a teaching click - tomes={} child={} busy={} "
                            + "held={} trigger={} sneaking={}",
                    Integer.valueOf(tomes.count()), Boolean.valueOf(villager.isChild()),
                    Boolean.valueOf(villager.getCustomer() != null),
                    held.isEmpty() ? "empty" : String.valueOf(held.getItem().getRegistryName()),
                    ModConfig.TEACH_TRIGGER, Boolean.valueOf(player.isSneaking()));
            if (!villager.isChild() && villager.getCustomer() == null) {
                if (tomes.count() > 0) {
                    TomeTradeSync.sync(villager, player, tomes);
                } else {
                    // No tomes, so there is nothing to reconcile - but NEVER_LOCK_ANY_TRADE
                    // still applies to the villager's ordinary trades. This call is a no-op
                    // unless that setting is on.
                    TomeTradeSync.unlockExistingTrades(villager);
                }
            }
            return;
        }

        // From here on the click belongs to us either way - it either teaches something or
        // explains why it did not. Letting the trade screen open on top of a refusal message
        // would bury the explanation.
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        player.swingArm(event.getHand());

        String blocker = findVillagerBlocker(villager);
        if (blocker != null) {
            refuse(player, villager, held, blocker);
            return;
        }

        Map<Enchantment, Integer> offered = EnchantmentHelper.getEnchantments(held);
        if (offered.isEmpty()) {
            refuse(player, villager, held, "That book has no enchantment on it.");
            return;
        }
        if (offered.size() > 1 && !ModConfig.ALLOW_MULTI_ENCHANT_BOOKS) {
            refuse(player, villager, held,
                    "This villager will only take books with a single enchantment on them.");
            return;
        }

        Plan plan = plan(offered, tomes);
        if (plan.refusal != null) {
            refuse(player, villager, held, plan.refusal);
            return;
        }

        apply(plan, tomes);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        TomeTradeSync.sync(villager, player, tomes);
        celebrate(player, villager, plan);
    }

    /**
     * Whether this particular click was meant to teach, given how TEACH_TRIGGER is set.
     *
     * <p>The two modes are exact opposites, and whichever one is not chosen is the escape
     * hatch that still opens the trade screen - so there is never a state where holding a
     * book makes a villager untradeable.
     */
    private static boolean isTeachingClick(EntityPlayer player) {
        boolean sneaking = player.isSneaking();
        return ModConfig.TRIGGER_SNEAK_RIGHT_CLICK.equals(ModConfig.TEACH_TRIGGER)
                ? sneaking : !sneaking;
    }

    /** Why this villager cannot be taught at all, or null when it can. */
    private static String findVillagerBlocker(EntityVillager villager) {
        if (villager.getCustomer() != null) {
            return "This villager is busy trading with someone else.";
        }
        if (villager.isChild() && !ModConfig.TEACH_BABY_VILLAGERS) {
            return "Baby villagers are too young to learn. Come back when it has grown up.";
        }
        if (!ModConfig.isProfessionAllowed(professionName(villager))) {
            return "This villager's profession does not deal in books.";
        }
        return null;
    }

    /**
     * The registry name of a villager's profession, or null when it has none.
     *
     * <p>Careers and professions are two different things in 1.12: a villager's career
     * ("Librarian", "Cartographer") sits inside its profession ("minecraft:librarian"). The
     * profession is the coarser of the two and the one with a stable registry name across
     * mods, which makes it the sane thing to filter on.
     */
    private static ResourceLocation professionName(EntityVillager villager) {
        return villager.getProfessionForge() == null
                ? null : villager.getProfessionForge().getRegistryName();
    }

    /**
     * Works out what the book would do without changing anything yet.
     *
     * <p>Deliberately all-or-nothing. A book carrying two enchantments where only one can be
     * taught is refused whole rather than partly consumed - handing over a Mending and
     * Unbreaking book and watching it disappear to teach only the Unbreaking would be a
     * genuinely bad surprise, and there is no way to give half a book back.
     */
    private static Plan plan(Map<Enchantment, Integer> offered, ITomeKnowledge tomes) {
        Plan plan = new Plan();
        // Captured before anything is applied, so the chat messages can count up from here
        // rather than all reporting the same total after the fact.
        plan.slotsUsedBefore = tomes.count();
        int slotsNeeded = 0;

        for (Map.Entry<Enchantment, Integer> entry : offered.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null || enchantment.getRegistryName() == null) {
                plan.refusal = "That book carries an enchantment this world does not recognise.";
                return plan;
            }
            if (!ModConfig.isEnchantmentAllowed(enchantment)) {
                plan.refusal = "Villagers here will not learn "
                        + plainName(enchantment) + ".";
                return plan;
            }

            ResourceLocation id = enchantment.getRegistryName();
            int cap = ModConfig.getMaxLevel(enchantment);
            // A book above the ceiling is not refused, just trimmed - somebody who found an
            // over-levelled book should still get the best the config allows out of it.
            int bookLevel = Math.min(entry.getValue().intValue(), cap);
            int known = tomes.getLevel(id);

            if (known == 0) {
                plan.changes.put(id, Integer.valueOf(bookLevel));
                plan.learned.put(enchantment, Integer.valueOf(bookLevel));
                slotsNeeded++;
                continue;
            }

            if (bookLevel > known) {
                if (!ModConfig.HIGHER_LEVEL_REPLACES) {
                    plan.refusal = "This villager only levels up from matching books - it needs "
                            + "another " + plainName(enchantment) + " " + numeral(known)
                            + ", not a higher one.";
                    return plan;
                }
                plan.changes.put(id, Integer.valueOf(bookLevel));
                plan.upgraded.add(new Upgrade(enchantment, known, bookLevel));
                continue;
            }

            if (bookLevel < known) {
                plan.refusal = "This villager already sells " + plainName(enchantment) + " "
                        + numeral(known) + ", which beats that book.";
                return plan;
            }

            // Equal levels: the anvil rule. Two of the same make the next one up.
            if (!ModConfig.ENABLE_UPGRADING) {
                plan.refusal = "This villager already knows " + plainName(enchantment) + " "
                        + numeral(known) + ", and upgrading is disabled.";
                return plan;
            }
            if (known >= cap) {
                plan.refusal = plainName(enchantment) + " " + numeral(known)
                        + " is as high as this villager can go.";
                return plan;
            }
            plan.changes.put(id, Integer.valueOf(known + 1));
            plan.upgraded.add(new Upgrade(enchantment, known, known + 1));
        }

        int free = ModConfig.MAX_TOMES_PER_VILLAGER - tomes.count();
        if (slotsNeeded > free) {
            plan.refusal = free <= 0
                    ? "This villager is full at " + tomes.count() + " enchantment(s) and cannot "
                    + "learn another. Try a different villager."
                    : "That book needs " + slotsNeeded + " free slots and this villager has "
                    + free + ".";
        }
        return plan;
    }

    private static void apply(Plan plan, ITomeKnowledge tomes) {
        for (Map.Entry<ResourceLocation, Integer> change : plan.changes.entrySet()) {
            tomes.setLevel(change.getKey(), change.getValue().intValue());
        }
    }

    // ----------------------------------------------------------------- feedback

    private static void celebrate(EntityPlayer player, EntityVillager villager, Plan plan) {
        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
        }
        spawnParticles(villager, EnumParticleTypes.VILLAGER_HAPPY);

        if (ModConfig.ANNOUNCE_LEARNED) {
            // Counts up one line at a time. Reading the live count here instead would print
            // the same final total on every line of a multi-enchantment book, since all of
            // them are already applied by the time any message is sent. Upgrades are absent
            // from this map on purpose - they never consume a slot, so they must not advance
            // the number.
            int slotsUsed = plan.slotsUsedBefore;
            for (Map.Entry<Enchantment, Integer> entry : plan.learned.entrySet()) {
                slotsUsed++;
                ITextComponent message = new TextComponentString(PREFIX + TextFormatting.GREEN
                        + "Learned ");
                message.appendSibling(describe(entry.getKey(), entry.getValue().intValue()));
                message.appendSibling(new TextComponentString(TextFormatting.GREEN + ". "
                        + TextFormatting.GRAY + slotsUsed + "/"
                        + ModConfig.MAX_TOMES_PER_VILLAGER + " slots used."));
                player.sendMessage(message);
            }
        }

        if (ModConfig.ANNOUNCE_UPGRADED) {
            for (Upgrade upgrade : plan.upgraded) {
                ITextComponent message = new TextComponentString(PREFIX + TextFormatting.AQUA
                        + "Upgraded ");
                message.appendSibling(describe(upgrade.enchantment, upgrade.from));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA + " to "));
                message.appendSibling(describe(upgrade.enchantment, upgrade.to));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA + "."));
                player.sendMessage(message);
            }
        }
    }

    private static void refuse(EntityPlayer player, EntityVillager villager, ItemStack held,
                               String reason) {
        if (ModConfig.CONSUME_BOOK_ON_REJECT && !player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        }
        spawnParticles(villager, EnumParticleTypes.VILLAGER_ANGRY);
        if (ModConfig.ANNOUNCE_REJECTED) {
            player.sendMessage(new TextComponentString(PREFIX + TextFormatting.YELLOW + reason));
        }
    }

    private static void spawnParticles(EntityVillager villager, EnumParticleTypes type) {
        if (!ModConfig.SPAWN_PARTICLES || !(villager.world instanceof WorldServer)) {
            return;
        }
        // Spawned from the server so every player nearby sees them, not just the one who
        // clicked. The offsets scatter the puffs across the villager's own bounding box.
        ((WorldServer) villager.world).spawnParticle(type,
                villager.posX, villager.posY + villager.height * 0.75D, villager.posZ,
                8, 0.4D, 0.4D, 0.4D, 0.0D);
    }

    /**
     * "Unbreaking III" as a component the client translates itself.
     *
     * <p>Built from translation keys rather than {@code Enchantment.getTranslatedName},
     * which resolves against the server's language rather than the player's - fine in
     * single player, wrong on any server whose players are not all English.
     */
    private static ITextComponent describe(Enchantment enchantment, int level) {
        ITextComponent name = new TextComponentTranslation(enchantment.getName());
        // Vanilla only ships numerals for I through X. Past that, and past whatever a mod
        // adds, the plain number is the only thing that can be shown.
        ITextComponent numeral = level >= 1 && level <= 10
                ? new TextComponentTranslation("enchantment.level." + level)
                : new TextComponentString(String.valueOf(level));
        return name.appendText(" ").appendSibling(numeral);
    }

    /**
     * A best-effort plain-text name for refusal messages, which are assembled as strings
     * rather than component trees. Falls back to the registry name when the translation key
     * cannot be resolved server-side.
     */
    @SuppressWarnings("deprecation")
    private static String plainName(Enchantment enchantment) {
        String key = enchantment.getName();
        String translated = net.minecraft.util.text.translation.I18n.translateToLocal(key);
        return translated.equals(key) ? String.valueOf(enchantment.getRegistryName()) : translated;
    }

    /** Roman numerals for the levels players actually see, plain digits past that. */
    private static String numeral(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(level);
        }
    }

    // ------------------------------------------------------------------- types

    /**
     * What a book would do, worked out before anything is committed.
     *
     * <p>{@link #changes} is what actually gets written; the other two exist purely so the
     * chat message can tell the player which of the changes were new tomes and which were
     * level-ups.
     */
    private static final class Plan {
        final Map<ResourceLocation, Integer> changes =
                new LinkedHashMap<ResourceLocation, Integer>();
        final Map<Enchantment, Integer> learned = new LinkedHashMap<Enchantment, Integer>();
        final List<Upgrade> upgraded = new ArrayList<Upgrade>();
        String refusal;
        /** How many slots the villager had filled before this book was applied. */
        int slotsUsedBefore;
    }

    private static final class Upgrade {
        final Enchantment enchantment;
        final int from;
        final int to;

        Upgrade(Enchantment enchantment, int from, int to) {
            this.enchantment = enchantment;
            this.from = from;
            this.to = to;
        }
    }
}
