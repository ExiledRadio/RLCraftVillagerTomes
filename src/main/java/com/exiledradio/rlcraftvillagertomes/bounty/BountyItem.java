package com.exiledradio.rlcraftvillagertomes.bounty;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** One line of a villager's demand: an item, how many it wants, and how many it has. */
public final class BountyItem {

    /** Metadata value meaning "any", matching {@code CatalystEntry.ANY_META}. */
    public static final int ANY_META = -1;

    private final ResourceLocation itemName;
    private final int meta;
    private final int required;
    private int delivered;

    public BountyItem(ResourceLocation itemName, int meta, int required, int delivered) {
        this.itemName = itemName;
        this.meta = meta;
        this.required = Math.max(1, required);
        this.delivered = Math.max(0, delivered);
    }

    public ResourceLocation getItemName() {
        return itemName;
    }

    public int getMeta() {
        return meta;
    }

    public int getRequired() {
        return required;
    }

    public int getDelivered() {
        return delivered;
    }

    public int getRemaining() {
        return Math.max(0, required - delivered);
    }

    public boolean isSatisfied() {
        return delivered >= required;
    }

    /**
     * Takes as much of a stack as this line still needs.
     *
     * @return how many were actually taken, which is zero when the line is already full
     */
    public int deliver(int available) {
        int taken = Math.min(getRemaining(), Math.max(0, available));
        delivered += taken;
        return taken;
    }

    /**
     * Whether a held stack counts towards this line.
     *
     * <p>NBT is ignored for the same reason it is ignored for catalysts: an enchanted or
     * renamed copy of a crafting material is still that material, and explaining why a
     * slightly different-looking dragon bone was refused is not worth the strictness.
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
}
