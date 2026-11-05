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
     * Whether this line takes more than one item - a wildcard, or a single item that a
     * wildcard in the config now covers. Shown in chat, because the item's own name cannot
     * say so: every color of dragon scale is called "Dragon Scales".
     */
    public boolean acceptsAnyKind() {
        if (ItemPattern.isPattern(itemName)) {
            return true;
        }
        for (BountyEntry entry : BountyRegistry.getEntries()) {
            if (ItemPattern.isPattern(entry.getItemName())
                    && ItemPattern.matches(entry.getItemName(), itemName)) {
                return true;
            }
        }
        return false;
    }

    /** The name a player reads for this line, falling back to the registry name. */
    public String displayName() {
        net.minecraft.item.Item item = ItemPattern.representative(itemName);
        String name = item == null
                ? String.valueOf(itemName)
                : new ItemStack(item, 1, meta < 0 ? 0 : meta).getDisplayName();
        return acceptsAnyKind() ? name + " (any kind)" : name;
    }

    /**
     * Whether a wildcard line in the current config covers both this line's item and the
     * offered one.
     *
     * <p>Requests are saved on the villager and never re-roll, so a villager that asked for
     * {@code dragonscales_blue} before the list grouped scales would otherwise hold out for
     * blue forever. Checking the live config rather than what was saved lets the grouping
     * reach those villagers too, without rewriting anything in the world.
     */
    private boolean sameFamily(ResourceLocation actual) {
        if (ItemPattern.isPattern(itemName)) {
            return false;
        }
        for (BountyEntry entry : BountyRegistry.getEntries()) {
            ResourceLocation family = entry.getItemName();
            if (ItemPattern.isPattern(family)
                    && ItemPattern.matches(family, itemName)
                    && ItemPattern.matches(family, actual)) {
                return true;
            }
        }
        return false;
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
        if (actual == null) {
            return false;
        }
        if (!ItemPattern.matches(itemName, actual) && !sameFamily(actual)) {
            return false;
        }
        // On a tool or armor piece metadata is wear, not a variant, so a config line that
        // pins one (minecraft:iron_axe:0) must not turn into "only a pristine axe will do".
        // A used pickaxe is still a pickaxe.
        if (stack.getItem().isDamageable()) {
            return true;
        }
        return meta == ANY_META || meta == stack.getMetadata();
    }
}
