package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The only implementation of {@link ITomeKnowledge}. */
public class TomeKnowledge implements ITomeKnowledge {

    /**
     * Linked, not hashed, so the trade list a player sees is in the order they taught it
     * rather than in whatever order the hash buckets happen to fall. That order is written
     * to NBT and read back, so it survives a world reload too.
     */
    private final Map<ResourceLocation, Integer> tomes =
            new LinkedHashMap<ResourceLocation, Integer>();

    @Override
    public int getLevel(ResourceLocation enchantment) {
        Integer level = tomes.get(enchantment);
        return level == null ? 0 : level.intValue();
    }

    @Override
    public boolean knows(ResourceLocation enchantment) {
        return tomes.containsKey(enchantment);
    }

    @Override
    public void setLevel(ResourceLocation enchantment, int level) {
        if (enchantment == null) {
            return;
        }
        if (level < 1) {
            tomes.remove(enchantment);
        } else {
            tomes.put(enchantment, Integer.valueOf(level));
        }
    }

    @Override
    public void forget(ResourceLocation enchantment) {
        tomes.remove(enchantment);
    }

    @Override
    public void clear() {
        tomes.clear();
    }

    @Override
    public int count() {
        return tomes.size();
    }

    @Override
    public Map<ResourceLocation, Integer> view() {
        return Collections.unmodifiableMap(tomes);
    }
}
