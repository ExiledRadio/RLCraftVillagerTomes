package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

/**
 * One thing a villager has been taught: an enchantment at a particular level.
 *
 * <p>One tome is one trade and one slot. That is the whole reason this exists as a type
 * rather than the enchantment-to-level map it replaced - with UPGRADE_TAKES_NEW_SLOT on, a
 * villager can hold Unbreaking II and Unbreaking III at the same time, and a map keyed by
 * enchantment cannot say that.
 *
 * <p>Immutable, so the lists holding these can be handed out without copying.
 */
public final class Tome {

    private final ResourceLocation enchantment;
    private final int level;

    public Tome(ResourceLocation enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    public ResourceLocation getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tome)) {
            return false;
        }
        Tome tome = (Tome) other;
        return level == tome.level
                && (enchantment == null ? tome.enchantment == null
                        : enchantment.equals(tome.enchantment));
    }

    @Override
    public int hashCode() {
        return 31 * (enchantment == null ? 0 : enchantment.hashCode()) + level;
    }

    @Override
    public String toString() {
        return enchantment + " " + level;
    }
}
