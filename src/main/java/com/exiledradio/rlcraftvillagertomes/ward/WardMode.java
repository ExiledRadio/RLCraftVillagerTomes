package com.exiledradio.rlcraftvillagertomes.ward;

/**
 * What a warded villager can still be hurt by.
 *
 * <p>The ordinal is what is saved on the villager, so the order here is part of the save
 * format: add new modes at the end only.
 */
public enum WardMode {

    /** No ward. */
    NONE("None", "Not warded"),
    /** Nothing hurts it. */
    ALL("Fully immune", "Nothing can hurt this villager."),
    /** Only players can hurt it - safe from every mob, fall, fire and drowning. */
    PLAYERS("Players only", "Only players can hurt this villager."),
    /**
     * Players and zombies can hurt it. The mode for curing: a zombie can turn it, and the
     * ward comes back with it when it is cured.
     */
    PLAYERS_AND_ZOMBIES("Players and zombies",
            "Only players and zombies can hurt this villager - zombies can turn it for curing.");

    private final String label;
    private final String description;

    WardMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** The next mode the trade-screen button switches to. Never lands on NONE. */
    public WardMode next() {
        switch (this) {
            case ALL:
                return PLAYERS;
            case PLAYERS:
                return PLAYERS_AND_ZOMBIES;
            default:
                return ALL;
        }
    }

    public static WardMode of(int ordinal) {
        WardMode[] all = values();
        return ordinal > 0 && ordinal < all.length ? all[ordinal] : NONE;
    }
}
