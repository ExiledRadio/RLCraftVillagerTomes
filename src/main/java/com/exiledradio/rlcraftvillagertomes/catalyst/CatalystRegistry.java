package com.exiledradio.rlcraftvillagertomes.catalyst;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The rarity tier list, parsed from config.
 *
 * <p>Two config lists feed this. Tiers name a rarity band and give it a percentage and a
 * count range; items assign a registry name to one of those bands. Splitting them means a
 * whole band can be retuned in one edit instead of hunting through every item that shares
 * it.
 *
 * <p>Parsing and item resolution deliberately happen at different times. Syntax is checked
 * the moment the config loads, so a typo is reported immediately with the line that caused
 * it. Turning a registry name into an actual {@link Item} waits until every mod has
 * finished registering, because config load runs during pre-init and asking the item
 * registry about another mod's item that early is a coin flip. {@link #resolveItems()} is
 * what closes that gap, and until it runs every lookup here simply finds nothing rather
 * than reporting a false absence.
 *
 * <p>Nothing in this class throws on bad input. A pack author with one malformed line
 * should get that line named in the log and everything else still working, not a crash.
 */
public final class CatalystRegistry {

    /** Tier name to definition, in the order the config listed them. */
    private static Map<String, CatalystTier> tiers = new LinkedHashMap<String, CatalystTier>();

    /** Every configured item, in config order, whether or not its item exists. */
    private static List<CatalystEntry> entries = new ArrayList<CatalystEntry>();

    /** Resolved lookup, rebuilt by {@link #resolveItems()}. Empty until then. */
    private static Map<Item, List<CatalystEntry>> byItem = new HashMap<Item, List<CatalystEntry>>();

    /** Config entries naming an item no loaded mod registers. Reported once, then ignored. */
    private static List<String> unresolved = new ArrayList<String>();

    /** False until every mod has registered its items, so lookups know to stay quiet. */
    private static boolean resolved;

    private CatalystRegistry() {
    }

    // ------------------------------------------------------------------ parse

    /**
     * Rebuilds the tier list from raw config lines.
     *
     * <p>Safe to call repeatedly - the in-game config screen does exactly that on every
     * save. When items have already been resolved once, they are resolved again here so a
     * live config edit takes effect without a restart.
     */
    public static void reload(String[] tierLines, String[] itemLines) {
        tiers = new LinkedHashMap<String, CatalystTier>();
        entries = new ArrayList<CatalystEntry>();
        byItem = new HashMap<Item, List<CatalystEntry>>();
        unresolved = new ArrayList<String>();

        for (String line : tierLines) {
            parseTier(line);
        }
        for (String line : itemLines) {
            parseItem(line);
        }

        RLCraftVillagerTomes.LOGGER.info("Catalyst list loaded - {} tier(s), {} item(s).",
                Integer.valueOf(tiers.size()), Integer.valueOf(entries.size()));

        if (resolved) {
            resolveItems();
        }
    }

    /** {@code name=percent,min-max} */
    private static void parseTier(String raw) {
        String line = raw == null ? "" : raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        int equals = line.indexOf('=');
        if (equals < 1) {
            warnLine("tier", line, "expected name=percent,min-max");
            return;
        }
        String name = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
        String[] parts = line.substring(equals + 1).split(",");
        if (name.isEmpty() || parts.length != 2) {
            warnLine("tier", line, "expected name=percent,min-max");
            return;
        }

        String[] range = parts[1].split("-");
        if (range.length != 2) {
            warnLine("tier", line, "count range should look like 2-6");
            return;
        }

        float percent;
        int min;
        int max;
        try {
            percent = Float.parseFloat(parts[0].trim());
            min = Integer.parseInt(range[0].trim());
            max = Integer.parseInt(range[1].trim());
        } catch (NumberFormatException e) {
            warnLine("tier", line, "percent and counts must be numbers");
            return;
        }

        if (percent < 0.0F) {
            warnLine("tier", line, "percent cannot be negative");
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

        tiers.put(name, new CatalystTier(name, percent, min, max));
    }

    /** {@code namespace:path=tier} or {@code namespace:path:meta=tier} */
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
        String tierName = line.substring(equals + 1).trim().toLowerCase(Locale.ROOT);

        CatalystTier tier = tiers.get(tierName);
        if (tier == null) {
            warnLine("item", line, "no tier called '" + tierName + "' is defined");
            return;
        }

        // A third colon-separated part is metadata. Two parts is the common case and means
        // any metadata matches, which is what you want for the overwhelming majority of
        // items and avoids making every author think about damage values.
        int meta = CatalystEntry.ANY_META;
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

        entries.add(new CatalystEntry(new ResourceLocation(spec), meta, tier));
    }

    private static void warnLine(String kind, String line, String why) {
        RLCraftVillagerTomes.LOGGER.warn("Ignoring catalyst {} line \"{}\" - {}.",
                kind, line, why);
    }

    // ---------------------------------------------------------------- resolve

    /**
     * Turns configured registry names into real items.
     *
     * <p>Called once every mod has finished loading. Names nothing recognises are collected
     * and reported as a single line rather than one warning each, because a tier list
     * written for a full pack and then used on a smaller one will legitimately miss a lot
     * of entries, and thirty warnings on every startup trains people to ignore the log.
     */
    public static void resolveItems() {
        byItem = new HashMap<Item, List<CatalystEntry>>();
        unresolved = new ArrayList<String>();
        resolved = true;

        for (CatalystEntry entry : entries) {
            Item item = ForgeRegistries.ITEMS.getValue(entry.getItemName());
            if (item == null) {
                unresolved.add(entry.describe());
                continue;
            }
            List<CatalystEntry> list = byItem.get(item);
            if (list == null) {
                list = new ArrayList<CatalystEntry>();
                byItem.put(item, list);
            }
            list.add(entry);
        }

        int found = entries.size() - unresolved.size();
        RLCraftVillagerTomes.LOGGER.info("Catalyst items resolved - {} of {} found.",
                Integer.valueOf(found), Integer.valueOf(entries.size()));
        if (!unresolved.isEmpty()) {
            RLCraftVillagerTomes.LOGGER.info(
                    "{} catalyst item(s) are not registered by any loaded mod and will be "
                            + "ignored: {}", Integer.valueOf(unresolved.size()), unresolved);
        }
    }

    // ----------------------------------------------------------------- lookup

    /**
     * The catalyst entry matching a stack, or null when it is not a catalyst.
     *
     * <p>An exact metadata match wins over an any-metadata one, so a list can give a tier to
     * a whole item and then single out one variant as worth more.
     */
    public static CatalystEntry find(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        List<CatalystEntry> candidates = byItem.get(stack.getItem());
        if (candidates == null) {
            return null;
        }
        CatalystEntry loose = null;
        for (CatalystEntry entry : candidates) {
            if (!entry.matches(stack)) {
                continue;
            }
            if (entry.getMeta() != CatalystEntry.ANY_META) {
                return entry;
            }
            loose = entry;
        }
        return loose;
    }

    /** Percentage points one of this stack contributes, or 0 when it is not a catalyst. */
    public static float percentFor(ItemStack stack) {
        CatalystEntry entry = find(stack);
        return entry == null ? 0.0F : entry.getTier().getPercent();
    }

    public static Collection<CatalystTier> getTiers() {
        return Collections.unmodifiableCollection(tiers.values());
    }

    public static List<CatalystEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /** Config entries whose item no loaded mod registers. Empty before {@link #resolveItems()}. */
    public static List<String> getUnresolved() {
        return Collections.unmodifiableList(unresolved);
    }

    /** Every configured item in a given tier, whether or not it resolved. */
    public static List<CatalystEntry> getEntriesInTier(String tierName) {
        List<CatalystEntry> list = new ArrayList<CatalystEntry>();
        for (CatalystEntry entry : entries) {
            if (entry.getTier().getName().equals(tierName)) {
                list.add(entry);
            }
        }
        return list;
    }

    /** True when the item registry has been consulted, so absences are trustworthy. */
    public static boolean isResolved() {
        return resolved;
    }
}
