package com.exiledradio.rlcraftvillagertomes.catalyst;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** One item assigned to a tier. */
public final class CatalystEntry {

    /** Metadata value meaning "any", used when the config names an item with no {@code :meta}. */
    public static final int ANY_META = -1;

    private final ResourceLocation itemName;
    private final int meta;
    private final CatalystTier tier;

    public CatalystEntry(ResourceLocation itemName, int meta, CatalystTier tier) {
        this.itemName = itemName;
        this.meta = meta;
        this.tier = tier;
    }

    public ResourceLocation getItemName() {
        return itemName;
    }

    public int getMeta() {
        return meta;
    }

    public CatalystTier getTier() {
        return tier;
    }

    /**
     * Whether a stack the player is holding counts as this catalyst.
     *
     * <p>Only the item and its metadata are considered. NBT is deliberately ignored: an
     * enchanted or renamed copy of a crafting material is still that material, and a player
     * who has to work out why their slightly-different-looking dragon bone was refused is
     * having a worse time than the strictness is worth.
     */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || itemName == null) {
            return false;
        }
        ResourceLocation actual = stack.getItem().getRegistryName();
        if (actual == null || !actual.equals(itemName)) {
            return false;
        }
        return meta == ANY_META || meta == stack.getMetadata();
    }

    /** The form written in the config, so error messages and dumps read back identically. */
    public String describe() {
        return meta == ANY_META ? String.valueOf(itemName) : itemName + ":" + meta;
    }

    @Override
    public String toString() {
        return describe() + " -> " + tier.getName();
    }
}
