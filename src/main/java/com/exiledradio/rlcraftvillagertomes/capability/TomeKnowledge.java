package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The only implementation of {@link ITomeKnowledge}. */
public class TomeKnowledge implements ITomeKnowledge {

    /**
     * Ordered, so the trade list a player sees is in the order they taught it rather than
     * in whatever order a hash happens to produce. That order is written to NBT and read
     * back, so it survives a world reload too.
     */
    private final List<Tome> tomes = new ArrayList<Tome>();

    @Override
    public int getHighestLevel(ResourceLocation enchantment) {
        int highest = 0;
        for (Tome tome : tomes) {
            if (tome.getEnchantment().equals(enchantment) && tome.getLevel() > highest) {
                highest = tome.getLevel();
            }
        }
        return highest;
    }

    @Override
    public boolean knows(ResourceLocation enchantment) {
        for (Tome tome : tomes) {
            if (tome.getEnchantment().equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean has(ResourceLocation enchantment, int level) {
        for (Tome tome : tomes) {
            if (tome.getLevel() == level && tome.getEnchantment().equals(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void add(ResourceLocation enchantment, int level) {
        if (enchantment == null || level < 1 || has(enchantment, level)) {
            return;
        }
        tomes.add(new Tome(enchantment, level));
    }

    @Override
    public void setOnly(ResourceLocation enchantment, int level) {
        if (enchantment == null) {
            return;
        }
        if (level < 1) {
            forget(enchantment);
            return;
        }

        // Overwrite the first entry in place and drop the rest, so an upgraded trade keeps
        // its position in the villager's list instead of jumping to the end.
        int first = -1;
        for (int i = 0; i < tomes.size(); i++) {
            if (tomes.get(i).getEnchantment().equals(enchantment)) {
                first = i;
                break;
            }
        }

        if (first < 0) {
            tomes.add(new Tome(enchantment, level));
            return;
        }

        tomes.set(first, new Tome(enchantment, level));
        // Walking backwards so removals cannot shift an index we have not looked at yet.
        for (int i = tomes.size() - 1; i > first; i--) {
            if (tomes.get(i).getEnchantment().equals(enchantment)) {
                tomes.remove(i);
            }
        }
    }

    @Override
    public void forget(ResourceLocation enchantment) {
        for (Iterator<Tome> it = tomes.iterator(); it.hasNext(); ) {
            if (it.next().getEnchantment().equals(enchantment)) {
                it.remove();
            }
        }
    }

    @Override
    public void clear() {
        tomes.clear();
        bankedChance = 0.0F;
        pity.clear();
    }

    // ------------------------------------------------------------------ chance

    private float bankedChance;

    private final Map<ResourceLocation, Float> pity =
            new LinkedHashMap<ResourceLocation, Float>();

    @Override
    public float getBankedChance() {
        return bankedChance;
    }

    @Override
    public void addBankedChance(float percent) {
        if (percent > 0.0F) {
            bankedChance += percent;
        }
    }

    @Override
    public void clearBankedChance() {
        bankedChance = 0.0F;
    }

    @Override
    public float getPityBonus(ResourceLocation enchantment) {
        Float owed = pity.get(enchantment);
        return owed == null ? 0.0F : owed.floatValue();
    }

    @Override
    public void addPityBonus(ResourceLocation enchantment, float percent) {
        if (enchantment != null && percent > 0.0F) {
            pity.put(enchantment, Float.valueOf(getPityBonus(enchantment) + percent));
        }
    }

    @Override
    public void clearPity(ResourceLocation enchantment) {
        pity.remove(enchantment);
    }

    @Override
    public Map<ResourceLocation, Float> pityView() {
        return Collections.unmodifiableMap(pity);
    }

    /** Used by storage when loading; sets rather than accumulates. */
    public void setPityBonus(ResourceLocation enchantment, float percent) {
        if (enchantment != null && percent > 0.0F) {
            pity.put(enchantment, Float.valueOf(percent));
        }
    }

    /** Used by storage when loading. */
    public void setBankedChance(float percent) {
        bankedChance = Math.max(0.0F, percent);
    }

    @Override
    public int count() {
        return tomes.size();
    }

    @Override
    public List<Tome> view() {
        return Collections.unmodifiableList(tomes);
    }
}
