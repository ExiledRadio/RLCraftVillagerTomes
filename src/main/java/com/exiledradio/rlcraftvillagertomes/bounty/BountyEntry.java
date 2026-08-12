package com.exiledradio.rlcraftvillagertomes.bounty;

import net.minecraft.util.ResourceLocation;

/**
 * One item a villager may demand, and how many of it.
 *
 * <p>The quantity normally comes from the tier, but an entry can override it. That matters
 * because rarity bands are coarse and value inside one is not: coal and diamonds are both
 * things you keep in stacks, and being asked for twenty of each is not the same ask at all.
 */
public final class BountyEntry {

    /** Metadata value meaning "any", used when the config names an item with no {@code :meta}. */
    public static final int ANY_META = -1;

    private final ResourceLocation itemName;
    private final int meta;
    private final BountyTier tier;
    private final int minOverride;
    private final int maxOverride;

    public BountyEntry(ResourceLocation itemName, int meta, BountyTier tier,
                       int minOverride, int maxOverride) {
        this.itemName = itemName;
        this.meta = meta;
        this.tier = tier;
        this.minOverride = minOverride;
        this.maxOverride = maxOverride;
    }

    public ResourceLocation getItemName() {
        return itemName;
    }

    public int getMeta() {
        return meta;
    }

    public BountyTier getTier() {
        return tier;
    }

    /** Fewest of this item a request will ask for - the override if there is one. */
    public int getMinCount() {
        return minOverride > 0 ? minOverride : tier.getMinCount();
    }

    /** Most of this item a request will ask for - the override if there is one. */
    public int getMaxCount() {
        return maxOverride > 0 ? maxOverride : tier.getMaxCount();
    }

    /** True when this entry sets its own quantity rather than inheriting the tier's. */
    public boolean hasOwnCount() {
        return minOverride > 0;
    }

    /** The form written in the config, so dumps and error messages read back identically. */
    public String describe() {
        return meta == ANY_META ? String.valueOf(itemName) : itemName + ":" + meta;
    }

    @Override
    public String toString() {
        return describe() + " -> " + tier.getName() + " " + getMinCount() + "-" + getMaxCount();
    }
}
