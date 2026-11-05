package com.exiledradio.rlcraftvillagertomes.bounty;

import com.exiledradio.rlcraftvillagertomes.Feedback;
import com.exiledradio.rlcraftvillagertomes.ModConfig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.quest.QuestBinding;

import java.util.List;

/**
 * Slots, and what a villager wants before it opens one.
 *
 * <p>Every slot is bought. A villager rolls its demand the first time somebody asks what it
 * wants, keeps that demand forever, and opens a slot only once the whole list is delivered.
 * Nothing here re-rolls: an unaffordable villager is a villager you walk away from, which
 * is what makes the ones you have invested in worth protecting.
 *
 * <p>A demand is live from the moment a villager is below its slot cap, regardless of
 * whether the slots it already has are empty. Buying all five before handing over a single
 * book is a deliberate strategy - it is the only route to the mod's best odds that does not
 * run through gambling books at its worst ones.
 */
public final class SlotRequests {

    private static final String PREFIX =
            TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;

    private SlotRequests() {
    }

    /**
     * How many slots a villager effectively has open.
     *
     * <p>With locking off this is simply the configured maximum, so turning the setting off
     * restores the old behavior for every villager at once rather than only for new ones.
     *
     * <p>This is also what the chance formulas count as the villager's experience: slots it
     * has opened, not books it holds. Paying for a slot is the investment, so that is what
     * should make the villager better - otherwise a villager you had just bought a slot on
     * would be no more capable than one you had not.
     */
    public static int unlockedSlots(ITomeKnowledge tomes) {
        return ModConfig.LOCK_SLOTS
                ? Math.min(tomes.getUnlockedSlots(), ModConfig.MAX_TOMES_PER_VILLAGER)
                : ModConfig.MAX_TOMES_PER_VILLAGER;
    }

    /** Unlocked slots not yet holding a book. */
    public static int openSlots(ITomeKnowledge tomes) {
        return Math.max(0, unlockedSlots(tomes) - tomes.count());
    }

    /**
     * Whether this villager still has a slot left to sell, and so a bounty to ask for.
     *
     * <p>Deliberately says nothing about whether the slots it already has are empty. A
     * villager owes a bounty for its next slot from the moment it is short of the cap, so a
     * player can buy all five before handing over a single book - which is the only way to
     * reach the best odds the mod offers before risking anything on them. Gating this on a
     * full slot instead meant the only route to a developed villager ran through feeding it
     * books at its worst chances, which is exactly backwards.
     */
    public static boolean canUnlockMore(ITomeKnowledge tomes) {
        return ModConfig.LOCK_SLOTS
                && unlockedSlots(tomes) < ModConfig.MAX_TOMES_PER_VILLAGER;
    }

    /**
     * Why a book cannot be taught right now, or null when there is room.
     *
     * <p>Rolls the demand as a side effect when there is none, so the first player to offer
     * a book is told what it would take rather than simply refused.
     */
    public static String checkSlotAvailable(EntityPlayer player, EntityVillager villager,
                                            ITomeKnowledge tomes) {
        if (!ModConfig.LOCK_SLOTS || openSlots(tomes) > 0) {
            return null;
        }
        if (unlockedSlots(tomes) >= ModConfig.MAX_TOMES_PER_VILLAGER) {
            return "This villager is full at " + tomes.count() + " trade(s). Try another.";
        }

        ensureRequest(villager, tomes);
        if (tomes.getRequest().isEmpty()) {
            // Nothing in the tier list resolved, so there is no demand anybody could satisfy.
            // Opening the slot is the only outcome that is not a dead end.
            tomes.unlockSlot();
            return null;
        }
        describeRequest(player, villager, tomes);
        return "This villager has no room yet - it wants payment for its next slot.";
    }

    /**
     * Rolls a demand if this villager has not been asked before.
     *
     * <p>Guarded on {@link #canUnlockMore} here rather than at every call site, because the
     * demand is now live at all times and so is reached from far more places than when it
     * only existed between a filled slot and the next payment. A villager at the cap must
     * never roll one - there is no slot six for it to be asking towards.
     */
    public static void ensureRequest(EntityVillager villager, ITomeKnowledge tomes) {
        if (!canUnlockMore(tomes) || !tomes.getRequest().isEmpty()) {
            return;
        }
        // Seeded from the world's random rather than anything stable, so two villagers in
        // the same village want different things.
        List<BountyItem> rolled = BountyGenerator.roll(
                unlockedSlots(tomes) + 1, villager.world.rand);
        tomes.setRequest(rolled);
    }

    /** True when this stack counts towards what the villager is still owed. */
    public static boolean wants(ITomeKnowledge tomes, ItemStack stack) {
        for (BountyItem line : tomes.getRequest()) {
            if (!line.isSatisfied() && line.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hands over as much of a stack as the demand still needs, and opens the slot when the
     * last line is filled.
     */
    public static void deliver(EntityPlayer player, EntityVillager villager, ItemStack held,
                               ITomeKnowledge tomes) {
        int taken = 0;
        for (BountyItem line : tomes.getRequest()) {
            if (line.isSatisfied() || !line.matches(held)) {
                continue;
            }
            taken = line.deliver(held.getCount());
            if (taken > 0 && !player.capabilities.isCreativeMode) {
                held.shrink(taken);
            }
            break;
        }
        finishDelivery(player, villager, tomes, taken, false);
    }

    /**
     * Whether the lectern's Deliver button may take this stack without asking.
     *
     * <p>Bounty matching ignores NBT, so a villager asking for a diamond sword is happy with
     * an enchanted one - fine when you hand one over yourself, a disaster when a button
     * sweeps your inventory and finds the sword on your hotbar. Anything enchanted or
     * renamed is left for you to hand over by hand.
     */
    public static boolean isSafeToAutoDeliver(ItemStack stack) {
        return !stack.isEmpty() && !stack.isItemEnchanted() && !stack.hasDisplayName();
    }

    /** How many of a line's item the player is carrying that the Deliver button would take. */
    public static int countDeliverable(EntityPlayer player, BountyItem line) {
        int count = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (isArmorSlot(i)) {
                continue;
            }
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isSafeToAutoDeliver(stack) && line.matches(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Hands over everything the villager still needs that the player is carrying.
     *
     * <p>What the ledger lectern's Deliver button does. Armor being worn is never touched,
     * and neither is anything {@link #isSafeToAutoDeliver} turns down.
     *
     * @return how many items were taken in total
     */
    public static int deliverFromInventory(EntityPlayer player, EntityVillager villager,
                                           ITomeKnowledge tomes) {
        int total = 0;
        for (BountyItem line : tomes.getRequest()) {
            for (int i = 0; i < player.inventory.getSizeInventory() && !line.isSatisfied(); i++) {
                if (isArmorSlot(i)) {
                    continue;
                }
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!isSafeToAutoDeliver(stack) || !line.matches(stack)) {
                    continue;
                }
                int taken = line.deliver(stack.getCount());
                if (taken > 0 && !player.capabilities.isCreativeMode) {
                    stack.shrink(taken);
                }
                total += taken;
            }
        }
        if (total > 0) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }
        finishDelivery(player, villager, tomes, total, true);
        return total;
    }

    /** {@code InventoryPlayer} lays out main (36), then armor (4), then off hand (1). */
    private static boolean isArmorSlot(int index) {
        return index >= 36 && index < 40;
    }

    /**
     * Effects, messages and the slot unlock after items were handed over.
     *
     * @param remote true when delivered from a lectern - the ledger screen already shows
     *               the remaining list, so the per-delivery chat listing is skipped
     */
    private static void finishDelivery(EntityPlayer player, EntityVillager villager,
                                       ITomeKnowledge tomes, int taken, boolean remote) {
        if (taken <= 0) {
            return;
        }

        if (ModConfig.PLAY_SOUNDS) {
            villager.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.8F, 1.2F);
        }
        particles(villager, EnumParticleTypes.VILLAGER_HAPPY, 8);

        if (isComplete(tomes)) {
            tomes.unlockSlot();
            tomes.setRequest(null);
            if (ModConfig.PLAY_SOUNDS) {
                villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                        SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.8F, 1.4F);
            }
            particles(villager, EnumParticleTypes.ENCHANTMENT_TABLE, 40);
            String unlocked = "Slot unlocked! " + unlockedSlots(tomes) + " slot"
                    + (unlockedSlots(tomes) == 1 ? "" : "s") + ", " + openSlots(tomes) + " free";

            boolean more = canUnlockMore(tomes);
            if (more) {
                // Straight on to the next demand rather than making the player click again
                // to discover there is one - the scroll that announces the unlock shows it.
                ensureRequest(villager, tomes);
                // And the ledger follows it to the new demand instead of crossing it off,
                // which it only deserves once there is genuinely nothing left to fetch.
                QuestBinding.refreshIfLogged(player, villager, tomes);
            } else {
                QuestBinding.clearIfLogged(player, villager);
                unlocked = "Slot unlocked - its last one";
            }
            if (remote) {
                Feedback.notify(player, Feedback.GOOD, unlocked);
            } else {
                Feedback.scroll(player, villager, tomes, Feedback.GOOD, unlocked);
            }
            return;
        }

        // Keeps a carried ledger's counts in step.
        QuestBinding.refreshIfLogged(player, villager, tomes);

        if (ModConfig.ANNOUNCE_LEARNED && !remote) {
            Feedback.scroll(player, villager, tomes, Feedback.GOOD, "Delivered " + taken);
        }
    }

    private static boolean isComplete(ITomeKnowledge tomes) {
        for (BountyItem line : tomes.getRequest()) {
            if (!line.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    /** Unrolls this villager's bounty scroll, rolling a demand first if it has none. */
    public static void describeRequest(EntityPlayer player, EntityVillager villager,
                                       ITomeKnowledge tomes) {
        if (!canUnlockMore(tomes)) {
            return;
        }
        ensureRequest(villager, tomes);
        if (tomes.getRequest().isEmpty()) {
            return;
        }
        Feedback.scroll(player, villager, tomes, Feedback.INFO, null);
    }

    private static void particles(EntityVillager villager, EnumParticleTypes type, int count) {
        if (!ModConfig.SPAWN_PARTICLES || !(villager.world instanceof WorldServer)) {
            return;
        }
        ((WorldServer) villager.world).spawnParticle(type,
                villager.posX, villager.posY + villager.height * 0.75D, villager.posZ,
                count, 0.4D, 0.4D, 0.4D, 0.0D);
    }
}
