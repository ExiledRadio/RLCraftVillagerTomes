package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Everything one villager has been taught, as an ordered list of {@link Tome}s.
 *
 * <p>A list rather than an enchantment-to-level map because a villager can legitimately
 * hold the same enchantment at two levels: with UPGRADE_TAKES_NEW_SLOT on, levelling a
 * trade up leaves the old one in place and the villager sells Unbreaking II and Unbreaking
 * III side by side. With the setting off there is never more than one entry per
 * enchantment, which is the invariant {@link #setOnly} maintains.
 *
 * <p>Enchantments are identified by registry name rather than by
 * {@link net.minecraft.enchantment.Enchantment} instance on purpose. A villager can outlive
 * the mod that added the enchantment it learned - somebody removes Quark from the pack and
 * the villager still has "quark:something" in its data. Storing the name means the entry
 * survives untouched and starts working again if the mod comes back, instead of being
 * silently dropped on the first load.
 *
 * <p>Order is the order the villager learned things in, so its trade list stays stable
 * across sessions rather than reshuffling on every load.
 */
public interface ITomeKnowledge {

    /**
     * The highest level held for an enchantment, or 0 if the villager has never been taught
     * it. This is the level upgrade decisions are made against.
     */
    int getHighestLevel(ResourceLocation enchantment);

    /** True when this villager holds the given enchantment at any level. */
    boolean knows(ResourceLocation enchantment);

    /** True when this villager holds this exact enchantment and level. */
    boolean has(ResourceLocation enchantment, int level);

    /**
     * Adds a tome, taking a new slot.
     *
     * <p>Does nothing when that exact enchantment and level is already held - two identical
     * trades on one villager would be a bug, not a feature. Levels below 1 are ignored,
     * since the game cannot represent a level-0 enchantment.
     */
    void add(ResourceLocation enchantment, int level);

    /**
     * Replaces every level held for an enchantment with this single one, taking no new slot.
     *
     * <p>Keeps the position of the first existing entry, so upgrading a trade in place does
     * not shuffle it to the end of the villager's list.
     */
    void setOnly(ResourceLocation enchantment, int level);

    /** Forgets an enchantment at every level. Does nothing if it was never known. */
    void forget(ResourceLocation enchantment);

    /** Forgets everything. Used by the admin command. */
    void clear();

    /**
     * How many slots are in use - the number of tomes, not the number of distinct
     * enchantments. This is what {@code MAX_TOMES_PER_VILLAGER} is measured against.
     */
    int count();

    /**
     * Read-only view in learning order. Never null, possibly empty.
     *
     * <p>Callers that mutate while iterating must copy first; this is backed by the live
     * list.
     */
    List<Tome> view();

    // ------------------------------------------------------------------ chance

    /**
     * Percentage points of catalyst banked into this villager, waiting to be spent on the
     * next teaching attempt.
     *
     * <p>Lives on the villager rather than the player because that is what the mechanic is:
     * you prime a specific villager, and walking away leaves the investment sitting on it.
     */
    float getBankedChance();

    /** Adds to the bank. Negative amounts are ignored. */
    void addBankedChance(float percent);

    /** Empties the bank, which is what happens after any attempt, win or lose. */
    void clearBankedChance();

    /**
     * Percentage points this villager owes on a given enchantment after past failures.
     *
     * <p>Per villager and per enchantment: failing Mending here makes Mending easier here,
     * and says nothing about Unbreaking or about the librarian next door.
     *
     * <p>Stored as accumulated points rather than a failure count because what a failure is
     * worth depends on the book that burned - losing a Sharpness V hurts five times as much
     * as losing a Sharpness I, and is compensated accordingly.
     */
    float getPityBonus(ResourceLocation enchantment);

    /** Adds to what is owed, raising the floor for the next attempt at that enchantment. */
    void addPityBonus(ResourceLocation enchantment, float percent);

    /** Clears what is owed on an enchantment, which is what a success does. */
    void clearPity(ResourceLocation enchantment);

    /** Read-only view of everything owed, keyed by enchantment. Never null. */
    Map<ResourceLocation, Float> pityView();
}
