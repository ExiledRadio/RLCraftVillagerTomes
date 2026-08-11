package com.exiledradio.rlcraftvillagertomes.capability;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
