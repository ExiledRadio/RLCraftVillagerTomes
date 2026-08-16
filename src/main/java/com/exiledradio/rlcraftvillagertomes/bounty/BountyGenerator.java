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

        List<BountyTier> tiers = new ArrayList<BountyTier>(BountyRegistry.getTiers());
        if (tiers.isEmpty()) {
            return request;
        }
        float[] weights = ModConfig.getTierWeights(slotNumber, tiers.size());

        int lines = ModConfig.REQUEST_ITEMS_BASE
                + (Math.max(1, slotNumber) - 1) * ModConfig.REQUEST_ITEMS_PER_SLOT;
        lines = Math.max(1, Math.min(lines, ModConfig.REQUEST_ITEMS_MAX));

        List<BountyEntry> picked = new ArrayList<BountyEntry>();
        for (int i = 0; i < lines; i++) {
            BountyEntry entry = pickItem(tiers, weights, picked, random);
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
     * Picks one item: first a band by weight, then an item evenly within that band.
     *
     * <p>Choosing the band first is the whole point. Flattening every band into one pool
     * and picking evenly makes a band's real odds its share of the item count, so adding
     * items to a band silently makes it more likely and no amount of tuning can make a late
     * slot harder. Two stages keep the shape of a request a decision rather than a side
     * effect of how the list happens to be filled in.
     *
     * <p>Bands with nothing left to give are dropped and their weight redistributed, which
     * is what stops a five-item request from running short once a band is exhausted.
     */
    private static BountyEntry pickItem(List<BountyTier> tiers, float[] weights,
                                        List<BountyEntry> exclude, Random random) {
        List<List<BountyEntry>> pools = new ArrayList<List<BountyEntry>>();
        for (BountyTier tier : tiers) {
            List<BountyEntry> pool = new ArrayList<BountyEntry>();
            for (BountyEntry entry : BountyRegistry.getUsableEntriesInTier(tier.getName())) {
                if (!containsItem(exclude, entry)) {
                    pool.add(entry);
                }
            }
            pools.add(pool);
        }

        float total = 0.0F;
        for (int i = 0; i < pools.size(); i++) {
            if (!pools.get(i).isEmpty() && weightOf(weights, i) > 0.0F) {
                total += weightOf(weights, i);
            }
        }

        // No weights parsed, or every band with weight is exhausted. Falling back to an even
        // pick across whatever is left beats returning nothing and cutting the request short.
        if (total <= 0.0F) {
            List<BountyEntry> any = new ArrayList<BountyEntry>();
            for (List<BountyEntry> pool : pools) {
                any.addAll(pool);
            }
            return any.isEmpty() ? null : any.get(random.nextInt(any.size()));
        }

        float roll = random.nextFloat() * total;
        for (int i = 0; i < pools.size(); i++) {
            float weight = weightOf(weights, i);
            if (pools.get(i).isEmpty() || weight <= 0.0F) {
                continue;
            }
            roll -= weight;
            if (roll <= 0.0F) {
                List<BountyEntry> pool = pools.get(i);
                return pool.get(random.nextInt(pool.size()));
            }
        }

        // Only reachable on floating point rounding at the very top of the range.
        for (int i = pools.size() - 1; i >= 0; i--) {
            if (!pools.get(i).isEmpty() && weightOf(weights, i) > 0.0F) {
                List<BountyEntry> pool = pools.get(i);
                return pool.get(random.nextInt(pool.size()));
            }
        }
        return null;
    }

    /** A band with no configured weight counts as unweighted rather than as zero. */
    private static float weightOf(float[] weights, int index) {
        if (weights == null) {
            return 1.0F;
        }
        return index < weights.length ? weights[index] : 0.0F;
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
