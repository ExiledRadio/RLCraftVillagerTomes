package com.exiledradio.rlcraftvillagertomes.bounty;

/**
 * One bounty rarity band: how many of an item of this rarity a villager asks for.
 *
 * <p>Purely a quantity range. How much an item helps a teaching attempt is a catalyst
 * question and lives on {@code CatalystTier} - most things a villager demands are not
 * catalysts at all, and the few that are do not want their two numbers tied together.
 *
 * <p>Ordering in the config is the ranking, cheapest first. That ordering is what decides
 * which bands an early slot may draw from.
 */
public final class BountyTier {

    private final String name;
    private final int minCount;
    private final int maxCount;

    public BountyTier(String name, int minCount, int maxCount) {
        this.name = name;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public String getName() {
        return name;
    }

    /** Fewest a request will ask for. */
    public int getMinCount() {
        return minCount;
    }

    /** Most a request will ask for. */
    public int getMaxCount() {
        return maxCount;
    }

    @Override
    public String toString() {
        return name + " (asks " + minCount + "-" + maxCount + ")";
    }
}
