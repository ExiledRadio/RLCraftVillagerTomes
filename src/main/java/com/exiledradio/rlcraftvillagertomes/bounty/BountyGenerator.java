package com.exiledradio.rlcraftvillagertomes.bounty;

import com.exiledradio.rlcraftvillagertomes.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rolls the demand a villager makes before it will open another slot.
 *
 * <p>Requests get longer and richer the deeper you go. Slot one is a short list of things
 * you already have stacks of; slot five reaches the dragon skulls. Both axes come from
 * {@link BountyRegistry} - the tier's ordering decides how rich it counts as, and its count
 * range decides how many get asked for.
 *
 * <p>A generated request is permanent. Nothing here re-rolls, so an unaffordable demand is
 * answered by finding a different villager rather than by clicking until the dice are
 * kinder. That is also why only installed items are ever drawn.
 */
public final class BountyGenerator {

    private BountyGenerator() {
    }

    /**
     * Builds the demand for a given slot.
     *
     * @param slotNumber the slot being unlocked, counting from 1
     * @return the lines to satisfy, or an empty list when the bounty list is empty or
     *         nothing in it resolved - in which case the caller treats the slot as free
     *         rather than as impossible
     */
    public static List<BountyItem> roll(int slotNumber, Random random) {
        List<BountyItem> request = new ArrayList<BountyItem>();

        List<BountyTier> usable = affordableTiers(slotNumber);
        if (usable.isEmpty()) {
            return request;
        }

        int lines = ModConfig.REQUEST_ITEMS_BASE
                + (Math.max(1, slotNumber) - 1) * ModConfig.REQUEST_ITEMS_PER_SLOT;
        lines = Math.max(1, Math.min(lines, ModConfig.REQUEST_ITEMS_MAX));

        List<BountyEntry> picked = new ArrayList<BountyEntry>();
        for (int i = 0; i < lines; i++) {
            BountyEntry entry = pickItem(usable, picked, random);
            if (entry == null) {
                break;
            }
            picked.add(entry);

            int span = entry.getMaxCount() - entry.getMinCount() + 1;
            int count = entry.getMinCount() + random.nextInt(Math.max(1, span));
            request.add(new BountyItem(entry.getItemName(), entry.getMeta(), count, 0));
        }
        return request;
    }

    /**
     * Which tiers a given slot may draw from.
     *
     * <p>The ceiling climbs with the slot number, so an opening request cannot lead with a
     * dragon skull and a late one is not padded out with coal alone. The cheapest tier
     * stays available at every depth on purpose - a request that is a solid wall of the top
     * band reads as refusal rather than as a goal.
     */
    private static List<BountyTier> affordableTiers(int slotNumber) {
        List<BountyTier> all = new ArrayList<BountyTier>(BountyRegistry.getTiers());
        List<BountyTier> usable = new ArrayList<BountyTier>();
        if (all.isEmpty()) {
            return usable;
        }

        int allowed = Math.min(all.size(),
                ModConfig.REQUEST_TIERS_BASE
                        + (Math.max(1, slotNumber) - 1) * ModConfig.REQUEST_TIERS_PER_SLOT);
        allowed = Math.max(1, allowed);

        for (int i = 0; i < allowed; i++) {
            BountyTier tier = all.get(i);
            if (!BountyRegistry.getUsableEntriesInTier(tier.getName()).isEmpty()) {
                usable.add(tier);
            }
        }
        // Everything in range was empty or uninstalled - fall back to any tier that has
        // something, so a sparse list still produces a request instead of nothing.
        if (usable.isEmpty()) {
            for (BountyTier tier : all) {
                if (!BountyRegistry.getUsableEntriesInTier(tier.getName()).isEmpty()) {
                    usable.add(tier);
                }
            }
        }
        return usable;
    }

    /** Picks one item, never the same one twice in a single request. */
    private static BountyEntry pickItem(List<BountyTier> tiers, List<BountyEntry> exclude,
                                        Random random) {
        List<BountyEntry> pool = new ArrayList<BountyEntry>();
        for (BountyTier tier : tiers) {
            for (BountyEntry entry : BountyRegistry.getUsableEntriesInTier(tier.getName())) {
                if (!containsItem(exclude, entry)) {
                    pool.add(entry);
                }
            }
        }
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    private static boolean containsItem(List<BountyEntry> list, BountyEntry candidate) {
        for (BountyEntry entry : list) {
            if (entry.describe().equals(candidate.describe())) {
                return true;
            }
        }
        return false;
    }
}
