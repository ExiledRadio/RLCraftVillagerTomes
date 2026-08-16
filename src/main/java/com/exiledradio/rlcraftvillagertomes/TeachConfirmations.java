package com.exiledradio.rlcraftvillagertomes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "are you sure" gate in front of handing a book over.
 *
 * <p>Losing a Sharpness V to a click you did not mean to make is the worst thing that can
 * happen in this mod, and one extra click is cheap insurance against it.
 *
 * <p>The debounce is the part that matters. The prompt appears in response to a click, so
 * without it a double-click would answer a question the player has not finished reading -
 * which is exactly the accident the prompt exists to prevent. Clicks inside that window do
 * nothing at all rather than confirming.
 *
 * <p>A confirmation is tied to the villager, the enchantment and the level, so it cannot be
 * set up on a cheap book and then spent on an expensive one.
 */
public final class TeachConfirmations {

    private static final Map<UUID, Pending> PENDING = new HashMap<UUID, Pending>();

    private TeachConfirmations() {
    }

    private static final class Pending {
        final UUID villager;
        final ResourceLocation enchantment;
        final int level;
        final long offeredAt;

        Pending(UUID villager, ResourceLocation enchantment, int level, long offeredAt) {
            this.villager = villager;
            this.enchantment = enchantment;
            this.level = level;
            this.offeredAt = offeredAt;
        }
    }

    /** What a click should do about a pending confirmation. */
    public enum Verdict {
        /** No confirmation was outstanding; one has been opened and the player told. */
        ASK,
        /** Too soon after the prompt to count as an answer. Do nothing at all. */
        TOO_SOON,
        /** Confirmed - go ahead and commit the book. */
        PROCEED
    }

    public static Verdict check(EntityPlayer player, UUID villager,
                                ResourceLocation enchantment, int level) {
        if (!ModConfig.CONFIRM_BEFORE_TEACHING) {
            return Verdict.PROCEED;
        }

        long now = System.currentTimeMillis();
        Pending pending = PENDING.get(player.getUniqueID());

        boolean matches = pending != null
                && pending.villager.equals(villager)
                && pending.enchantment.equals(enchantment)
                && pending.level == level;

        if (matches) {
            long elapsed = now - pending.offeredAt;
            if (elapsed < ModConfig.CONFIRM_DEBOUNCE_MS) {
                return Verdict.TOO_SOON;
            }
            if (elapsed <= ModConfig.CONFIRM_TIMEOUT_SECONDS * 1000L) {
                PENDING.remove(player.getUniqueID());
                return Verdict.PROCEED;
            }
            // Lapsed. Falls through to asking again rather than committing a book the
            // player had forgotten they were holding over a villager.
        }

        PENDING.put(player.getUniqueID(), new Pending(villager, enchantment, level, now));
        return Verdict.ASK;
    }

    /** Drops any outstanding confirmation, so a refusal cannot leave one armed. */
    public static void clear(EntityPlayer player) {
        PENDING.remove(player.getUniqueID());
    }
}
