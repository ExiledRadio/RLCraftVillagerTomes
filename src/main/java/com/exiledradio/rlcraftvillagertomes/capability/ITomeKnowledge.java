package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

import java.util.Map;

/**
 * Everything one villager has been taught: a map of enchantment registry name to the
 * level it currently offers.
 *
 * <p>Keys are registry names rather than {@code Enchantment} instances on purpose. A
 * villager can outlive the mod that added the enchantment it learned - somebody removes
 * Quark from the pack, loads the world, and the villager still has "quark:something" in
 * its data. Storing the name means that entry survives untouched and starts working again
 * if the mod comes back, instead of being silently dropped on the first load. Anything
 * that needs the actual {@link net.minecraft.enchantment.Enchantment} looks it up at the
 * point of use and skips the entry when the registry has never heard of it.
 *
 * <p>Iteration order is the order the villager learned things in, so its trade list stays
 * stable across sessions rather than reshuffling on every load.
 */
public interface ITomeKnowledge {

    /** The level this villager offers for an enchantment, or 0 if it was never taught it. */
    int getLevel(ResourceLocation enchantment);

    /** True when this villager has been taught the given enchantment at any level. */
    boolean knows(ResourceLocation enchantment);

    /**
     * Records a level for an enchantment, replacing any level already held.
     *
     * @param level must be at least 1; anything lower removes the entry instead, since a
     *              level-0 enchantment is not a thing the game can represent
     */
    void setLevel(ResourceLocation enchantment, int level);

    /** Forgets an enchantment entirely. Does nothing if it was never known. */
    void forget(ResourceLocation enchantment);

    /** Forgets everything. Used by the admin command. */
    void clear();

    /** How many distinct enchantments this villager knows - the number the cap applies to. */
    int count();

    /**
     * Read-only view in learning order. Never null, possibly empty.
     *
     * <p>Callers that mutate while iterating must copy first; this is the live map.
     */
    Map<ResourceLocation, Integer> view();
}
