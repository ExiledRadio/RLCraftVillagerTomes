package com.exiledradio.rlcraftvillagertomes.bounty;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code *} wildcards in a bounty item name, so one line can stand for a family of items.
 *
 * <p>Exists for Ice and Fire's scales. Every dragon scale color is its own item, yet every
 * one is called plain "Dragon Scales" with the color only in a tooltip - so a villager
 * asking for {@code dragonscales_blue} showed as "Dragon Scales" in chat and then refused the
 * red ones the player was holding. {@code iceandfire:dragonscales_*} asks for scales and
 * takes whichever color you bring, and also covers the extra colors newer Ice and Fire
 * versions add without the list having to name them.
 *
 * <p>A wildcard only ever spans the path, never the mod id, so {@code *:ingot} is not a
 * thing - a request that could be satisfied by any mod's item is not a request.
 */
public final class ItemPattern {

    private static final Map<String, Pattern> COMPILED = new HashMap<String, Pattern>();
    private static final Map<String, Item> REPRESENTATIVE = new HashMap<String, Item>();

    private ItemPattern() {
    }

    public static boolean isPattern(ResourceLocation name) {
        return name != null && name.toString().indexOf('*') >= 0;
    }

    /** Whether {@code actual} is the named item, or one the wildcard name covers. */
    public static boolean matches(ResourceLocation name, ResourceLocation actual) {
        if (name == null || actual == null) {
            return false;
        }
        if (!isPattern(name)) {
            return name.equals(actual);
        }
        return compile(name).matcher(actual.toString()).matches();
    }

    /**
     * The item to show for a line: the item itself, or for a wildcard the first registered
     * item it covers in registry-name order, so the name shown never changes between runs.
     * Null when nothing installed matches.
     */
    public static Item representative(ResourceLocation name) {
        if (name == null) {
            return null;
        }
        if (!isPattern(name)) {
            return ForgeRegistries.ITEMS.getValue(name);
        }
        String key = name.toString();
        if (REPRESENTATIVE.containsKey(key)) {
            return REPRESENTATIVE.get(key);
        }
        Item best = null;
        String bestName = null;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = item.getRegistryName();
            if (id == null || !matches(name, id)) {
                continue;
            }
            if (bestName == null || id.toString().compareTo(bestName) < 0) {
                best = item;
                bestName = id.toString();
            }
        }
        REPRESENTATIVE.put(key, best);
        return best;
    }

    private static Pattern compile(ResourceLocation name) {
        String key = name.toString();
        Pattern pattern = COMPILED.get(key);
        if (pattern == null) {
            // Each * becomes "any run of path characters". The mod id cannot contain one of
            // those runs that crosses the colon, so a wildcard never escapes its own mod.
            StringBuilder regex = new StringBuilder();
            String[] pieces = key.split("\\*", -1);
            for (int i = 0; i < pieces.length; i++) {
                if (i > 0) {
                    regex.append("[a-z0-9_./-]*");
                }
                regex.append(Pattern.quote(pieces[i]));
            }
            pattern = Pattern.compile(regex.toString());
            COMPILED.put(key, pattern);
        }
        return pattern;
    }
}
