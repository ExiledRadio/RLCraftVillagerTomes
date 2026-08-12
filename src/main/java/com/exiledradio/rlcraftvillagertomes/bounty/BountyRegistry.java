package com.exiledradio.rlcraftvillagertomes.bounty;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What villagers are allowed to demand, parsed from config.
 *
 * <p>Deliberately separate from the catalyst list. The two overlap in places - a glowing
 * ingot is both a fine catalyst and a fine thing to be asked for - but they answer
 * different questions, and most of what a villager demands is ordinary material that would
 * never belong in a percentage table.
 *
 * <p>Follows the same two-stage pattern as {@code CatalystRegistry}: syntax is checked when
 * the config loads so a typo is named immediately, and registry names are resolved once
 * every mod has finished registering. Nothing here throws on bad input.
 */
public final class BountyRegistry {

    private static Map<String, BountyTier> tiers = new LinkedHashMap<String, BountyTier>();
    private static List<BountyEntry> entries = new ArrayList<BountyEntry>();
    private static List<String> unresolved = new ArrayList<String>();
    private static boolean resolved;

    private BountyRegistry() {
    }

    public static void reload(String[] tierLines, String[] itemLines) {
        tiers = new LinkedHashMap<String, BountyTier>();
        entries = new ArrayList<BountyEntry>();
        unresolved = new ArrayList<String>();

        for (String line : tierLines) {
            parseTier(line);
        }
        for (String line : itemLines) {
            parseItem(line);
        }

        RLCraftVillagerTomes.LOGGER.info("Bounty list loaded - {} tier(s), {} item(s).",
                Integer.valueOf(tiers.size()), Integer.valueOf(entries.size()));

        if (resolved) {
            resolveItems();
        }
    }

    /** {@code name=min-max} */
    private static void parseTier(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        int equals = line.indexOf('=');
        if (equals < 1) {
            warnLine("tier", line, "expected name=min-max");
            return;
        }
        String name = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
        String[] range = line.substring(equals + 1).trim().split("-");
        if (name.isEmpty() || range.length != 2) {
            warnLine("tier", line, "expected name=min-max, such as low=8-24");
            return;
        }

        int min;
        int max;
        try {
            min = Integer.parseInt(range[0].trim());
            max = Integer.parseInt(range[1].trim());
        } catch (NumberFormatException e) {
            warnLine("tier", line, "counts must be numbers");
            return;
        }
        if (min < 1 || max < min) {
            warnLine("tier", line, "count range must be at least 1 and not backwards");
            return;
        }
        if (tiers.containsKey(name)) {
            warnLine("tier", line, "a tier called '" + name + "' is already defined");
            return;
        }

        tiers.put(name, new BountyTier(name, min, max));
    }

    /** {@code modid:item=tier} or {@code modid:item:meta=tier,min-max} */
    private static void parseItem(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        int equals = line.indexOf('=');
        if (equals < 1) {
            warnLine("item", line, "expected modid:item=tier");
            return;
        }
        String spec = line.substring(0, equals).trim();
        String[] value = line.substring(equals + 1).trim().split(",");
        String tierName = value[0].trim().toLowerCase(Locale.ROOT);

        BountyTier tier = tiers.get(tierName);
        if (tier == null) {
            warnLine("item", line, "no tier called '" + tierName + "' is defined");
            return;
        }

        int min = 0;
        int max = 0;
        if (value.length > 1) {
            String[] range = value[1].trim().split("-");
            if (range.length != 2) {
                warnLine("item", line, "count override should look like 4-12");
                return;
            }
            try {
                min = Integer.parseInt(range[0].trim());
                max = Integer.parseInt(range[1].trim());
            } catch (NumberFormatException e) {
                warnLine("item", line, "count override must be numbers");
                return;
            }
            if (min < 1 || max < min) {
                warnLine("item", line, "count override must be at least 1 and not backwards");
                return;
            }
        }

        int meta = BountyEntry.ANY_META;
        String[] pieces = spec.split(":");
        if (pieces.length == 3) {
            try {
                meta = Integer.parseInt(pieces[2].trim());
            } catch (NumberFormatException e) {
                warnLine("item", line, "metadata must be a number");
                return;
            }
            spec = pieces[0] + ":" + pieces[1];
        } else if (pieces.length != 2) {
            warnLine("item", line, "expected modid:item or modid:item:meta");
            return;
        }

        entries.add(new BountyEntry(new ResourceLocation(spec), meta, tier, min, max));
    }

    private static void warnLine(String kind, String line, String why) {
        RLCraftVillagerTomes.LOGGER.warn("Ignoring bounty {} line \"{}\" - {}.", kind, line, why);
    }

    /**
     * Checks which configured items actually exist.
     *
     * <p>Matters more here than it does for catalysts: a request is never re-rolled, so
     * demanding an item this instance does not have would strand the villager permanently.
     * Missing names are filtered out of the pool rather than merely noted.
     */
    public static void resolveItems() {
        unresolved = new ArrayList<String>();
        resolved = true;

        for (BountyEntry entry : entries) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getItemName());
            if (item == null) {
                unresolved.add(entry.describe());
            }
        }

        int found = entries.size() - unresolved.size();
        RLCraftVillagerTomes.LOGGER.info("Bounty items resolved - {} of {} found.",
                Integer.valueOf(found), Integer.valueOf(entries.size()));
        if (!unresolved.isEmpty()) {
            RLCraftVillagerTomes.LOGGER.info(
                    "{} bounty item(s) are not registered by any loaded mod and will never be "
                            + "asked for: {}", Integer.valueOf(unresolved.size()), unresolved);
        }
    }

    public static Collection<BountyTier> getTiers() {
        return Collections.unmodifiableCollection(tiers.values());
    }

    public static List<BountyEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static List<String> getUnresolved() {
        return Collections.unmodifiableList(unresolved);
    }

    /** Every installed item in a tier. Missing ones are left out, so this is safe to draw from. */
    public static List<BountyEntry> getUsableEntriesInTier(String tierName) {
        List<BountyEntry> list = new ArrayList<BountyEntry>();
        for (BountyEntry entry : entries) {
            if (entry.getTier().getName().equals(tierName)
                    && !unresolved.contains(entry.describe())) {
                list.add(entry);
            }
        }
        return list;
    }

    public static boolean isResolved() {
        return resolved;
    }
}
