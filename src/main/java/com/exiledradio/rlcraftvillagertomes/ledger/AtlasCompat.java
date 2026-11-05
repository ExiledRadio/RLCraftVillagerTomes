package com.exiledradio.rlcraftvillagertomes.ledger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Optional Antique Atlas support: putting a ledger villager on the player's atlas.
 *
 * <p>Reached by reflection so the mod has no compile or runtime dependency on the atlas; packs
 * without it simply never offer the click. Written against Antique Atlas 4.6.x for 1.12.2,
 * whose {@code AtlasAPI} has been stable for years:
 * <ul>
 *   <li>{@code AtlasAPI.getPlayerAtlases(player)} - the atlas ids the player carries (or their
 *       personal one when the atlas is configured not to need an item);</li>
 *   <li>{@code AtlasAPI.getMarkerAPI().putMarker(world, visibleAhead, atlasId, type, label, x, z)}
 *       - on the server this saves the marker and syncs it to every client.</li>
 * </ul>
 */
public final class AtlasCompat {

    public static final String MODID = "antiqueatlas";
    /**
     * The book-and-sparkle marker RLCraft-family packs (Dregora among them) add through
     * {@code config/antiqueatlas/markers.json}. Preferred because it reads as "a librarian".
     */
    public static final String BOOK_MARKER = "antiqueatlas:book";
    /** The atlas's own village icon, always registered - the fallback. */
    public static final String VILLAGE_MARKER = "antiqueatlas:village";

    private static boolean resolved;
    private static Method getPlayerAtlases;
    private static Method getMarkerAPI;
    private static Method putMarker;

    private AtlasCompat() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    private static boolean resolve() {
        if (resolved) {
            return putMarker != null;
        }
        resolved = true;
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> api = Class.forName("hunternif.mc.atlas.api.AtlasAPI");
            getPlayerAtlases = api.getMethod("getPlayerAtlases", EntityPlayer.class);
            getMarkerAPI = api.getMethod("getMarkerAPI");
            Class<?> markers = Class.forName("hunternif.mc.atlas.api.MarkerAPI");
            putMarker = markers.getMethod("putMarker", World.class, boolean.class, int.class,
                    String.class, String.class, int.class, int.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            putMarker = null;
        }
        return putMarker != null;
    }

    /**
     * Which marker to use, decided on the client.
     *
     * <p>Pack-added marker types come from {@code markers.json}, which is client config - a
     * dedicated server may never have heard of the book marker even though every player can
     * draw it. So the client, which is the side that has to render the icon, checks its own
     * marker registry and says which one to put down.
     */
    public static String preferredMarker() {
        if (!isLoaded()) {
            return VILLAGE_MARKER;
        }
        try {
            Class<?> registry = Class.forName("hunternif.mc.atlas.registry.MarkerRegistry");
            Object has = registry.getMethod("hasKey", String.class).invoke(null, BOOK_MARKER);
            return Boolean.TRUE.equals(has) ? BOOK_MARKER : VILLAGE_MARKER;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return VILLAGE_MARKER;
        }
    }

    /**
     * Marks a spot on every atlas the player carries, server side.
     *
     * @return how many atlases were marked; 0 when the player has none, -1 when the atlas mod
     *         is missing or its API could not be found
     */
    public static int mark(EntityPlayer player, World world, String type, String label,
                           int x, int z) {
        // Only the two known icons: the type comes from the client.
        String marker = BOOK_MARKER.equals(type) ? BOOK_MARKER : VILLAGE_MARKER;
        if (!resolve()) {
            return -1;
        }
        try {
            @SuppressWarnings("unchecked")
            List<Integer> atlases = (List<Integer>) getPlayerAtlases.invoke(null, player);
            if (atlases == null || atlases.isEmpty()) {
                return 0;
            }
            Object api = getMarkerAPI.invoke(null);
            int marked = 0;
            for (Integer id : atlases) {
                if (id != null) {
                    putMarker.invoke(api, world, Boolean.TRUE, id, marker, label, x, z);
                    marked++;
                }
            }
            return marked;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1;
        }
    }
}
