package com.exiledradio.rlcraftvillagertomes.catalyst;

/**
 * One rarity band: what an item of this rarity is worth, and how many of it a villager
 * asks for.
 *
 * <p>The two numbers exist on the same object because the same tier definition drives both
 * halves of the 2.0 design. The percentage is what an item contributes to a teaching
 * attempt when banked into a villager as a catalyst; the count range is how many a villager
 * asks for when it rolls a slot request. Keeping them together means retuning "how rare is
 * rare" is one edit rather than two lists drifting apart.
 */
public final class CatalystTier {

    private final String name;
    private final float percent;
    private final int minCount;
    private final int maxCount;

    public CatalystTier(String name, float percent, int minCount, int maxCount) {
        this.name = name;
        this.percent = percent;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    /** Lower-cased identifier, as written in the config and referenced by item lines. */
    public String getName() {
        return name;
    }

    /** Percentage points one of these adds to a teaching attempt. */
    public float getPercent() {
        return percent;
    }

    /** Fewest a slot request will ask for. */
    public int getMinCount() {
        return minCount;
    }

    /** Most a slot request will ask for. */
    public int getMaxCount() {
        return maxCount;
    }

    @Override
    public String toString() {
        return name + " (+" + percent + "%, asks " + minCount + "-" + maxCount + ")";
    }
}
