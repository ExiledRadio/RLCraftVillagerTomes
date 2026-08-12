package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One villager recorded in a quest log: who, where, and what it is still owed. */
public final class QuestEntry {

    private final UUID villager;
    private String name;
    private int x;
    private int y;
    private int z;
    private int dimension;
    private int slot;
    private final List<BountyItem> wanted = new ArrayList<BountyItem>();

    public QuestEntry(UUID villager, String name, int x, int y, int z, int dimension, int slot) {
        this.villager = villager;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.slot = slot;
    }

    public UUID getVillager() {
        return villager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getDimension() {
        return dimension;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * Where the villager stood when it was written down.
     *
     * <p>Not updated afterwards, and that is deliberate - a villager wanders a few blocks
     * around its village, and rewriting the page every time it shuffles would make the book
     * churn without ever being more useful. The coordinates are there to get you back to the
     * right village, not to the exact block.
     */
    public void setPosition(int x, int y, int z, int dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    public List<BountyItem> getWanted() {
        return wanted;
    }

    public void setWanted(List<BountyItem> lines) {
        wanted.clear();
        if (lines != null) {
            wanted.addAll(lines);
        }
    }
}
