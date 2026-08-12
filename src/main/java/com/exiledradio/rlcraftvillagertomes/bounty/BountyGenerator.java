package com.exiledradio.rlcraftvillagertomes.bounty;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystEntry;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystRegistry;
import com.exiledradio.rlcraftvillagertomes.catalyst.CatalystTier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Rolls the demand a villager makes before it will open another slot.
 *
 * <p>Requests get longer and richer the deeper you go. Slot one is a short list of things
 * you already have stacks of; slot five wants something you had to go and kill a dragon
 * for. Both halves of that come from the same tier list the catalysts use - the tier's
 * percentage decides how rich it counts as, and its count range decides how many get asked
 * for, which is why one edit to a tier moves both at once.
 *
 * <p>A generated request is permanent. Nothing here ever re-rolls, so an unaffordable
 * demand is answered by finding a different villager rather than by clicking until the dice
 * are kinder.
 */
public final class BountyGenerator {

    private BountyGenerator() {
    }

    /**
     * Builds the demand for a given slot.
     *
     * @param slotNumber the slot being unlocked, counting from 1
     * @return the lines to satisfy, or an empty list when the tier list is empty or nothing
     *         in it resolved - in which case the caller should treat the slot as free rather
     *         than as impossible
     */
    public static List<BountyItem> roll(int slotNumber, Random random) {
        List<BountyItem> request = new ArrayList<BountyItem>();

        List<CatalystTier> usable = affordableTiers(slotNumber);
        if (usable.isEmpty()) {
            return request;
        }

        int lines = ModConfig.REQUEST_ITEMS_BASE
                + (Math.max(1, slotNumber) - 1) * ModConfig.REQUEST_ITEMS_PER_SLOT;
        lines = Math.max(1, Math.min(lines, ModConfig.REQUEST_ITEMS_MAX));

        List<CatalystEntry> alreadyPicked = new ArrayList<CatalystEntry>();
        for (int i = 0; i < lines; i++) {
            CatalystEntry entry = pickItem(usable, alreadyPicked, random);
            if (entry == null) {
                break;
            }
            alreadyPicked.add(entry);

            CatalystTier tier = entry.getTier();
            int span = tier.getMaxCount() - tier.getMinCount() + 1;
            int count = tier.getMinCount() + random.nextInt(Math.max(1, span));

            request.add(new BountyItem(entry.getItemName(), entry.getMeta(), count, 0));
        }
        return request;
    }

    /**
     * Which tiers a given slot is allowed to draw from.
     *
     * <p>The ceiling climbs with the slot number, so early requests cannot open with a
     * dragon skull and late ones are not padded out with coal. The cheapest tier stays
     * available at every depth on purpose - a late request that is entirely legendary items
     * reads as a wall rather than a goal.
     */
    private static List<CatalystTier> affordableTiers(int slotNumber) {
        List<CatalystTier> all = new ArrayList<CatalystTier>(CatalystRegistry.getTiers());
        List<CatalystTier> usable = new ArrayList<CatalystTier>();
        if (all.isEmpty()) {
            return usable;
        }

        // Tiers are ordered cheapest-first in the config, and that ordering is the ranking.
        int allowed = Math.min(all.size(),
                ModConfig.REQUEST_TIERS_BASE
                        + (Math.max(1, slotNumber) - 1) * ModConfig.REQUEST_TIERS_PER_SLOT);
        allowed = Math.max(1, allowed);

        for (int i = 0; i < allowed; i++) {
            CatalystTier tier = all.get(i);
            if (!CatalystRegistry.getEntriesInTier(tier.getName()).isEmpty()) {
                usable.add(tier);
            }
        }
        // Every tier in range was empty - fall back to anything that has items at all, so a
        // sparse list still produces a request instead of nothing.
        if (usable.isEmpty()) {
            for (CatalystTier tier : all) {
                if (!CatalystRegistry.getEntriesInTier(tier.getName()).isEmpty()) {
                    usable.add(tier);
                }
            }
        }
        return usable;
    }

    /**
     * Picks one item, biased towards the richest tier available, and never the same item
     * twice in one request.
     */
    private static CatalystEntry pickItem(List<CatalystTier> tiers,
                                          List<CatalystEntry> exclude, Random random) {
        List<CatalystEntry> pool = new ArrayList<CatalystEntry>();
        for (CatalystTier tier : tiers) {
            for (CatalystEntry entry : CatalystRegistry.getEntriesInTier(tier.getName())) {
                if (!containsItem(exclude, entry) && isInstalled(entry)) {
                    pool.add(entry);
                }
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static boolean containsItem(List<CatalystEntry> list, CatalystEntry candidate) {
        for (CatalystEntry entry : list) {
            if (entry.describe().equals(candidate.describe())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A tier list written for a full pack will name items this instance does not have.
     * Asking for one would be an unsatisfiable demand on a villager that never re-rolls, so
     * they are filtered out before anything is committed.
     */
    private static boolean isInstalled(CatalystEntry entry) {
        Collection<String> missing = CatalystRegistry.getUnresolved();
        return !missing.contains(entry.describe());
    }
}
