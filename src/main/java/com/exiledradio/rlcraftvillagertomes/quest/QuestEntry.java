package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

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
    /**
     * 0 when not a favorite; otherwise the order it was starred in. Favorites are shown
     * first, oldest star first, so new ones stack up behind the earlier ones.
     */
    private long favorite;
    /** The villager's ward as of the last refresh while it was loaded ({@code WardMode}). */
    private int ward;

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

    /**
     * The stored name: a serialised text component for entries written since 2.2.0, plain
     * text for older ones. Stored as a component so a villager with no name tag is recorded
     * by its career ("Librarian") and translated on whichever client reads it, rather than
     * frozen in the server's language.
     */
    public String getName() {
        return name;
    }

    /** The name ready to show, whichever form it was stored in. */
    public ITextComponent getNameComponent() {
        if (name != null && (name.startsWith("{") || name.startsWith("\""))) {
            try {
                ITextComponent parsed = ITextComponent.Serializer.jsonToComponent(name);
                if (parsed != null) {
                    return parsed;
                }
            } catch (RuntimeException ignored) {
                // An old plain-text name that happens to start with a brace. Shown as written.
            }
        }
        return new TextComponentString(name == null ? "" : name);
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

    public long getFavorite() {
        return favorite;
    }

    public void setFavorite(long favorite) {
        this.favorite = favorite;
    }

    public int getWard() {
        return ward;
    }

    public void setWard(int ward) {
        this.ward = ward;
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
