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
        failures.clear();
    }

    // ------------------------------------------------------------------ chance

    private float bankedChance;

    private final Map<ResourceLocation, Integer> failures =
            new LinkedHashMap<ResourceLocation, Integer>();

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
    public int getFailures(ResourceLocation enchantment) {
        Integer count = failures.get(enchantment);
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void recordFailure(ResourceLocation enchantment) {
        if (enchantment != null) {
            failures.put(enchantment, Integer.valueOf(getFailures(enchantment) + 1));
        }
    }

    @Override
    public void clearFailures(ResourceLocation enchantment) {
        failures.remove(enchantment);
    }

    /** Read-only view of the failure counts, for storage and the admin command. */
    public Map<ResourceLocation, Integer> failureView() {
        return Collections.unmodifiableMap(failures);
    }

    /** Used by storage when loading; bypasses the increment-by-one of recordFailure. */
    public void setFailures(ResourceLocation enchantment, int count) {
        if (enchantment != null && count > 0) {
            failures.put(enchantment, Integer.valueOf(count));
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
