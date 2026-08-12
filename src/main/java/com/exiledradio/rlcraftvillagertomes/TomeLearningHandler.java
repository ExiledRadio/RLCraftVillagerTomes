package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.Tome;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystEntry;
import com.exiledradio.rlcraftvillagertomes.quest.QuestBinding;
import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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

        // One physical right-click can reach the server twice. Minecraft.rightClickMouse
        // loops over EnumHand.values(), sending an interact packet for the main hand and
        // then the off hand, and only stops early if the CLIENT sees a SUCCESS result.
        // Everything here is server-side, so a vanilla client never breaks that loop and we
        // get the same click delivered twice - which showed up as every chat message being
        // printed twice, and would have rolled the dice twice for one book.
        if (event.getHand() != EnumHand.MAIN_HAND) {
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

        // A sneak-click now means three different things depending on what is in hand: a
        // book is an attempt, a catalyst is a deposit, and an empty hand is a question.
        // Anything else is none of our business and falls through to the trade screen.
        boolean sneakAction = ModConfig.ENABLE_LEARNING && isTeachingClick(player);
        boolean teaching = sneakAction && !held.isEmpty()
                && held.getItem() == Items.ENCHANTED_BOOK;
        boolean delivering = sneakAction && ModConfig.LOCK_SLOTS && !held.isEmpty()
                && SlotRequests.wants(tomes, held);
        boolean banking = !delivering && sneakAction && ModConfig.ENABLE_CHANCE
                && !held.isEmpty() && CatalystRegistry.find(held) != null;
        // Empty hand is the inspect gesture, and it works whether or not the gamble is
        // switched on - with ENABLE_CHANCE off there is no percentage to report but the
        // villager's outstanding demand still matters, and that is the thing people check
        // constantly while out gathering.
        boolean asking = sneakAction && held.isEmpty();
        // A book and quill becomes a log; an existing log records the villager it is
        // clicked at. Checked before the catalyst and bounty branches so neither can claim
        // the click if somebody ever lists a book in one of those lists.
        boolean logging = sneakAction && (QuestLog.isLog(held) || QuestLog.isBlank(held));

        if (logging || delivering || banking || asking) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            player.swingArm(event.getHand());
            claimClick(player);

            String blocker = findVillagerBlocker(villager);
            if (blocker != null) {
                refuse(player, villager, held, blocker);
            } else if (logging) {
                ItemStack log = held;
                if (QuestLog.isBlank(held)) {
                    log = QuestBinding.placeNewLog(player, QuestLog.createFrom(held, player));
                }
                if (log.isEmpty()) {
                    refuse(player, villager, held,
                            "No room to put the log - free up a slot and try again.");
                } else {
                    QuestBinding.offer(player, villager, log, tomes);
                }
            } else if (delivering) {
                SlotRequests.deliver(player, villager, held, tomes);
            } else if (banking) {
                bankCatalyst(player, villager, held, tomes);
            } else {
                reportChance(player, villager, tomes);
            }
            return;
        }

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

        // Only the top enchantment is attempted, and only it is peeled off the book. A book
        // holding Unbreaking III, Mending and Efficiency V is three separate gambles, and
        // whichever way the first one goes you walk away still holding the other two.
        //
        // "Top" is the first entry in the book's stored list, which is the line the tooltip
        // draws first - so what gets taken is exactly what the player sees at the top.
        // EnchantmentHelper.getEnchantments builds a LinkedHashMap in that same order.
        Map<Enchantment, Integer> attempt = new LinkedHashMap<Enchantment, Integer>();
        Map.Entry<Enchantment, Integer> top = offered.entrySet().iterator().next();
        attempt.put(top.getKey(), top.getValue());

        String locked = SlotRequests.checkSlotAvailable(player, villager, tomes);
        if (locked != null) {
            refuse(player, villager, held, locked);
            return;
        }

        Plan plan = plan(attempt, tomes);
        if (plan.refusal != null) {
            refuse(player, villager, held, plan.refusal);
            return;
        }

        // The roll. One per book, not per enchantment: a multi-enchantment book is already
        // all-or-nothing everywhere else, and rolling each line separately would let half a
        // book land, which there is no way to represent.
        ResourceLocation gambledOn = plan.primaryEnchantment();
        if (ModConfig.ENABLE_CHANCE && !player.capabilities.isCreativeMode) {
            float chance = ModConfig.getTotalChance(
                    SlotRequests.chanceSlots(tomes), tomes.getPityBonus(gambledOn), tomes.getBankedChance());

            if (villager.world.rand.nextFloat() * 100.0F >= chance) {
                failAttempt(player, villager, held, tomes, gambledOn,
                        top.getValue().intValue(), chance);
                return;
            }
            tomes.clearPity(gambledOn);
        }

        apply(plan, tomes);
        // Filling a slot spends whatever was banked, so the next book starts fresh.
        tomes.clearBankedChance();
        if (!player.capabilities.isCreativeMode) {
            consumeEnchantment(held, gambledOn);
        }
        TomeTradeSync.sync(villager, player, tomes);
        celebrate(player, villager, plan);
    }

    /**
     * Banks one catalyst into the villager, raising its odds for the next attempt.
     *
     * <p>Refused rather than eaten once the villager is already at the ceiling, because
     * silently swallowing an item that cannot possibly help is the kind of thing players
     * only notice after they have fed it six.
     */
    private static void bankCatalyst(EntityPlayer player, EntityVillager villager,
                                     ItemStack held, ITomeKnowledge tomes) {
        CatalystEntry entry = CatalystRegistry.find(held);
        if (entry == null) {
            return;
        }

        // Measured against the floor with no pity, because pity is per enchantment and this
        // deposit is not about any particular book.
        // Compared against the preparation ceiling, not the absolute one: pity can push a
        // villager past 80% but it is not something a catalyst can add to.
        float current = ModConfig.getPreparedChance(SlotRequests.chanceSlots(tomes), tomes.getBankedChance());
        if (current >= ModConfig.MAX_SUCCESS_CHANCE) {
            refuse(player, villager, held, "This villager is already at the maximum "
                    + percent(ModConfig.MAX_SUCCESS_CHANCE) + " chance - keep that for another.");
            return;
        }

        // Captured before the stack shrinks. Banking the last one in a stack empties it,
        // and an emptied stack renders as "Air" in the message that follows.
        ITextComponent banked = held.getTextComponent();

        tomes.addBankedChance(entry.getTier().getPercent());
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }

        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.4F);
        }
        spawnParticles(villager, EnumParticleTypes.VILLAGER_HAPPY);

        if (ModConfig.ANNOUNCE_LEARNED) {
            float now = ModConfig.getTotalChance(SlotRequests.chanceSlots(tomes), 0.0F,
                    tomes.getBankedChance());
            ITextComponent message = new TextComponentString(PREFIX + TextFormatting.GREEN
                    + "Banked ");
            message.appendSibling(banked);
            message.appendSibling(new TextComponentString(TextFormatting.GREEN + " (+"
                    + percent(entry.getTier().getPercent()) + "). "
                    + TextFormatting.GRAY + "Chance now " + percent(now) + "."));
            player.sendMessage(message);
            // Repeated under every deposit so the numbers you are aiming at stay in front of
            // you while you feed a villager, rather than needing an empty-handed click
            // between each one to check whether you have already overshot.
            sendBonusLines(player, tomes);
        }
    }

    /**
     * Answers the empty-handed sneak-click: where this villager currently stands, and what
     * it is still owed.
     *
     * <p>This is the inspect gesture, and it is the one people use constantly while out
     * gathering, so it has to answer both questions in one click. Before this it reported
     * only the odds, which meant the only way to see an outstanding demand was to offer a
     * book and be refused.
     */
    private static void reportChance(EntityPlayer player, EntityVillager villager,
                                     ITomeKnowledge tomes) {
        int open = SlotRequests.openSlots(tomes);
        String slots = open + " slot" + (open == 1 ? "" : "s") + " free";

        if (ModConfig.ENABLE_CHANCE) {
            float total = ModConfig.getTotalChance(
                    SlotRequests.chanceSlots(tomes), 0.0F, tomes.getBankedChance());
            player.sendMessage(new TextComponentString(PREFIX + TextFormatting.AQUA
                    + "Success: " + TextFormatting.WHITE + percent(total)
                    + TextFormatting.GRAY + " - " + slots));
            sendBonusLines(player, tomes);
        } else {
            player.sendMessage(new TextComponentString(PREFIX + TextFormatting.AQUA
                    + slots));
        }

        // No-op when the villager has room, so an inspect only mentions a demand when there
        // actually is one to satisfy.
        SlotRequests.describeRequest(player, villager, tomes);
    }

    /**
     * One line per enchantment this villager owes something on, with the number that
     * matters: the bonus and what it actually adds up to.
     *
     * <p>Pity is per enchantment, so it cannot be folded into the headline figure. Shown as
     * the bonus rather than a failure count because what a player needs to know is that
     * Mending reads better here, not the bookkeeping behind it. Iterating what is owed
     * rather than what is known also catches enchantments failed but never landed, which
     * have no tome to hang a line off.
     */
    private static void sendBonusLines(EntityPlayer player, ITomeKnowledge tomes) {
        int filled = SlotRequests.chanceSlots(tomes);
        float banked = tomes.getBankedChance();
        for (Map.Entry<ResourceLocation, Float> owed : tomes.pityView().entrySet()) {
            if (owed.getValue().floatValue() <= 0.0F) {
                continue;
            }
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(owed.getKey());
            String name = enchantment == null
                    ? String.valueOf(owed.getKey()) : plainName(enchantment);
            player.sendMessage(new TextComponentString(TextFormatting.GRAY + "  " + name
                    + " +" + percent(owed.getValue().floatValue())
                    + " | " + TextFormatting.WHITE
                    + percent(ModConfig.getTotalChance(
                            filled, owed.getValue().floatValue(), banked))));
        }
    }

    /**
     * Peels one enchantment off a book, leaving the rest of it in the player's hand.
     *
     * <p>{@code ItemEnchantedBook.getEnchantments} hands back the stack's live
     * {@code StoredEnchantments} list rather than a copy, so removing an entry from it edits
     * the book directly. When that was the last enchantment there is no book left to hold
     * and the stack goes instead - an enchanted book with an empty list would render as a
     * blank, untradeable oddity.
     *
     * <p>Safe to do in place because vanilla enchanted books have a maximum stack size of
     * one, so there is no second book in the stack to corrupt.
     */
    private static void consumeEnchantment(ItemStack book, ResourceLocation id) {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment == null) {
            book.shrink(1);
            return;
        }

        NBTTagList stored = ItemEnchantedBook.getEnchantments(book);
        int wanted = Enchantment.getEnchantmentID(enchantment);
        for (int i = 0; i < stored.tagCount(); i++) {
            if (stored.getCompoundTagAt(i).getShort("id") == wanted) {
                stored.removeTag(i);
                break;
            }
        }

        if (stored.tagCount() == 0) {
            book.shrink(1);
        }
    }

    /** A lost roll: the book burns, the bank empties, and the floor rises a little. */
    private static void failAttempt(EntityPlayer player, EntityVillager villager, ItemStack held,
                                    ITomeKnowledge tomes, ResourceLocation gambledOn,
                                    int bookLevel, float chance) {
        if (ModConfig.CONSUME_BOOK_ON_FAILURE) {
            // Only the enchantment that was gambled on burns. The rest of a multi-enchantment
            // book survives to be attempted separately.
            consumeEnchantment(held, gambledOn);
        }
        if (ModConfig.CONSUME_CATALYSTS_ON_FAILURE) {
            tomes.clearBankedChance();
        }
        // Scaled by the level that burned: a Sharpness V is five times the loss of a
        // Sharpness I, so it buys five times the consolation.
        tomes.addPityBonus(gambledOn, ModConfig.PITY_PER_BOOK_LEVEL * Math.max(1, bookLevel));

        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            villager.playSound(SoundEvents.ENTITY_ITEM_BREAK, 0.8F, 0.8F);
        }
        spawnParticles(villager, EnumParticleTypes.SMOKE_LARGE);

        if (ModConfig.ANNOUNCE_REJECTED) {
            float next = ModConfig.getTotalChance(
                    SlotRequests.chanceSlots(tomes), tomes.getPityBonus(gambledOn), tomes.getBankedChance());
            player.sendMessage(new TextComponentString(PREFIX + TextFormatting.RED
                    + "The binding failed at " + percent(chance) + "."
                    + TextFormatting.GRAY + " Next attempt at that enchantment here: "
                    + percent(next) + "."));
        }
    }

    /** Trims the trailing zero off whole percentages so chat reads "50%" rather than "50.0%". */
    private static String percent(float value) {
        return (value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value))
                + "%";
    }

    /** The last world tick on which each player's click was consumed by this mod. */
    private static final Map<java.util.UUID, Long> CLAIMED = new java.util.HashMap<java.util.UUID, Long>();

    private static void claimClick(EntityPlayer player) {
        CLAIMED.put(player.getUniqueID(), Long.valueOf(player.world.getTotalWorldTime()));
    }

    /**
     * Stops the item in hand from also being used when we have already answered the click.
     *
     * <p>Cancelling the entity interaction is not enough on its own. The client's
     * {@code rightClickMouse} tries the entity first and, seeing no success come back from
     * its own copy of the world, falls through to using the held item - so a book and quill
     * opened its writing screen a moment after the villager had already dealt with it, and
     * the log only appeared once that screen was closed.
     *
     * <p>Keyed on the tick rather than on the item so it covers anything else that might
     * gain a right-click behaviour later, and scoped to a single tick so it can never
     * swallow a genuine, separate click.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Long claimed = CLAIMED.get(event.getEntityPlayer().getUniqueID());
        if (claimed != null
                && event.getWorld().getTotalWorldTime() - claimed.longValue() <= 1L) {
            event.setCanceled(true);
        }
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
            // Naming who does take books matters more than it used to: librarians only is
            // now the default, so this is the message most players meet first, and "no"
            // without "try one of these" is a dead end.
            String allowed = ModConfig.getAllowedProfessionsLabel();
            return allowed == null
                    ? "This villager will not take books."
                    : "Only " + allowed + " will take books.";
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
            int known = tomes.getHighestLevel(id);

            if (known == 0) {
                plan.changes.add(new Change(id, bookLevel, true));
                plan.learned.put(enchantment, Integer.valueOf(bookLevel));
                slotsNeeded++;
                continue;
            }

            if (bookLevel > known) {
                if (!ModConfig.upgradesFromHigher()) {
                    plan.refusal = "This villager only levels up from matching books - it needs "
                            + "another " + plainName(enchantment) + " " + numeral(known)
                            + ", not a higher one.";
                    return plan;
                }
                plan.changes.add(new Change(id, bookLevel, ModConfig.UPGRADE_TAKES_NEW_SLOT));
                plan.upgraded.add(new Upgrade(enchantment, known, bookLevel,
                        ModConfig.UPGRADE_TAKES_NEW_SLOT));
                if (ModConfig.UPGRADE_TAKES_NEW_SLOT) {
                    slotsNeeded++;
                }
                continue;
            }

            if (bookLevel < known) {
                plan.refusal = "This villager already sells " + plainName(enchantment) + " "
                        + numeral(known) + ", which beats that book.";
                return plan;
            }

            // Equal levels. Which of the three refusals applies is ordered most-specific
            // first: being switched off beats being at the ceiling, and both beat the
            // mode-specific advice, so the player is never told to fetch a higher book that
            // cannot exist.
            if (ModConfig.upgradingIsOff()) {
                plan.refusal = "This villager already knows " + plainName(enchantment) + " "
                        + numeral(known) + ", and upgrading is switched off.";
                return plan;
            }
            if (known >= cap) {
                plan.refusal = plainName(enchantment) + " " + numeral(known)
                        + " is as high as this villager can go.";
                return plan;
            }
            if (!ModConfig.upgradesFromPair()) {
                plan.refusal = "This villager already sells " + plainName(enchantment) + " "
                        + numeral(known) + ". Combine two of them at an anvil and bring back "
                        + plainName(enchantment) + " " + numeral(known + 1) + " to level it up.";
                return plan;
            }
            plan.changes.add(new Change(id, known + 1, ModConfig.UPGRADE_TAKES_NEW_SLOT));
            plan.upgraded.add(new Upgrade(enchantment, known, known + 1,
                    ModConfig.UPGRADE_TAKES_NEW_SLOT));
            if (ModConfig.UPGRADE_TAKES_NEW_SLOT) {
                slotsNeeded++;
            }
        }

        int free = ModConfig.MAX_TOMES_PER_VILLAGER - tomes.count();
        if (slotsNeeded > free) {
            // With UPGRADE_TAKES_NEW_SLOT on, a full villager cannot be levelled up either,
            // so the "learn another" wording would be actively misleading there.
            plan.refusal = free <= 0
                    ? "This villager is full at " + tomes.count() + " trade(s) and has no room "
                    + "for another. Try a different villager."
                    : "That needs " + slotsNeeded + " free slot(s) and this villager has "
                    + free + ".";
        }
        return plan;
    }

    private static void apply(Plan plan, ITomeKnowledge tomes) {
        for (Change change : plan.changes) {
            if (change.newSlot) {
                tomes.add(change.id, change.level);
            } else {
                tomes.setOnly(change.id, change.level);
            }
        }
    }

    // ----------------------------------------------------------------- feedback

    private static void celebrate(EntityPlayer player, EntityVillager villager, Plan plan) {
        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
        }
        spawnParticles(villager, EnumParticleTypes.VILLAGER_HAPPY);

        // Counts up one line at a time, and is shared with the upgrade block below so a book
        // that both teaches and levels up keeps counting rather than restarting. Reading the
        // live count instead would print the same final total on every line of a
        // multi-enchantment book, since all of them are applied before any message is sent.
        int slotsUsed = plan.slotsUsedBefore;

        // The counter advances whether or not the matching message is switched on, so
        // turning ANNOUNCE_LEARNED off cannot leave the upgrade lines reporting stale
        // numbers.
        for (Map.Entry<Enchantment, Integer> entry : plan.learned.entrySet()) {
            slotsUsed++;
            if (!ModConfig.ANNOUNCE_LEARNED) {
                continue;
            }
            ITextComponent message = new TextComponentString(PREFIX + TextFormatting.GREEN
                    + "Learned ");
            message.appendSibling(describe(entry.getKey(), entry.getValue().intValue()));
            message.appendSibling(new TextComponentString(TextFormatting.GREEN + ". "
                    + TextFormatting.GRAY + slotsUsed + "/"
                    + ModConfig.MAX_TOMES_PER_VILLAGER + " slots used."));
            player.sendMessage(message);
        }

        for (Upgrade upgrade : plan.upgraded) {
            if (upgrade.keptOld) {
                slotsUsed++;
            }
            if (!ModConfig.ANNOUNCE_UPGRADED) {
                continue;
            }
            ITextComponent message = new TextComponentString(PREFIX + TextFormatting.AQUA
                    + (upgrade.keptOld ? "Added " : "Upgraded "));
            if (upgrade.keptOld) {
                // The old trade is still on the board, so saying "upgraded X to Y" would be a
                // lie - it now sells both, and the slot cost is the whole point of the
                // setting, so it gets said out loud.
                message.appendSibling(describe(upgrade.enchantment, upgrade.to));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA
                        + " alongside "));
                message.appendSibling(describe(upgrade.enchantment, upgrade.from));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA + ". "
                        + TextFormatting.GRAY + slotsUsed + "/"
                        + ModConfig.MAX_TOMES_PER_VILLAGER + " slots used."));
            } else {
                message.appendSibling(describe(upgrade.enchantment, upgrade.from));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA + " to "));
                message.appendSibling(describe(upgrade.enchantment, upgrade.to));
                message.appendSibling(new TextComponentString(TextFormatting.AQUA + "."));
            }
            player.sendMessage(message);
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
        spawnParticles(villager, type, 8, 0.4D);
    }

    private static void spawnParticles(EntityVillager villager, EnumParticleTypes type,
                                       int count, double spread) {
        if (!ModConfig.SPAWN_PARTICLES || !(villager.world instanceof WorldServer)) {
            return;
        }
        // Spawned from the server so every player nearby sees them, not just the one who
        // clicked. The offsets scatter the puffs across the villager's own bounding box.
        ((WorldServer) villager.world).spawnParticle(type,
                villager.posX, villager.posY + villager.height * 0.75D, villager.posZ,
                count, spread, spread, spread, 0.0D);
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
        final List<Change> changes = new ArrayList<Change>();
        final Map<Enchantment, Integer> learned = new LinkedHashMap<Enchantment, Integer>();
        final List<Upgrade> upgraded = new ArrayList<Upgrade>();
        String refusal;
        /** How many slots the villager had filled before this book was applied. */
        int slotsUsedBefore;

        /**
         * The enchantment a failed roll is recorded against.
         *
         * <p>One roll covers the whole book, so a multi-enchantment book has to pin its
         * pity on something. The first entry is the honest choice: it is the one the player
         * was most likely aiming for, and spreading a single failure across every line would
         * make one unlucky Mending-and-Unbreaking book worth two failures.
         */
        ResourceLocation primaryEnchantment() {
            return changes.isEmpty() ? null : changes.get(0).id;
        }
    }

    /** One edit to make, once the whole book is known to be acceptable. */
    private static final class Change {
        final ResourceLocation id;
        final int level;
        /** True to add a tome alongside what is there, false to replace it in place. */
        final boolean newSlot;

        Change(ResourceLocation id, int level, boolean newSlot) {
            this.id = id;
            this.level = level;
            this.newSlot = newSlot;
        }
    }

    private static final class Upgrade {
        final Enchantment enchantment;
        final int from;
        final int to;
        /** True when the old trade was kept and this one took a slot of its own. */
        final boolean keptOld;

        Upgrade(Enchantment enchantment, int from, int to, boolean keptOld) {
            this.enchantment = enchantment;
            this.from = from;
            this.to = to;
            this.keptOld = keptOld;
        }
    }
}
