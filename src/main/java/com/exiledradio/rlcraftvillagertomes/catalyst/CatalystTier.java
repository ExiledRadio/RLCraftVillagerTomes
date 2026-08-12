package com.exiledradio.rlcraftvillagertomes.catalyst;

/**
 * One catalyst rarity band: what an item of this rarity is worth when banked into a
 * villager to improve a teaching attempt.
 *
 * <p>Purely a percentage. Bounty quantities live on {@code BountyTier} instead, because the
 * two answer different questions - how much an item helps has nothing to do with how many
 * of it a villager asks for, and a great bounty item is very often not a catalyst at all.
 */
public final class CatalystTier {

    private final String name;
    private final float percent;

    public CatalystTier(String name, float percent) {
        this.name = name;
        this.percent = percent;
    }

    /** Lower-cased identifier, as written in the config and referenced by item lines. */
    public String getName() {
        return name;
    }

    /** Percentage points one of these adds to a teaching attempt. */
    public float getPercent() {
        return percent;
    }

    @Override
    public String toString() {
        return name + " (+" + percent + "%)";
    }
}
