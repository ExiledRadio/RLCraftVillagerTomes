package com.exiledradio.rlcraftvillagertomes.capability;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Registers the tome capability and bolts one onto every villager that spawns or loads.
 *
 * <p>A capability rather than a bare NBT tag on the entity because Forge saves and loads
 * capability data with the entity for free, including across chunk unloads and world
 * saves, and because writing directly into another entity's {@code ForgeData} compound is
 * a good way to collide with whatever else in the pack had the same idea.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class CapabilityTomeKnowledge {

    /**
     * Filled in by Forge once {@link #register()} has run. Null before that, and null
     * forever if registration somehow failed - every read goes through
     * {@link #get(Entity)}, which treats null as "no tomes" rather than crashing.
     */
    @CapabilityInject(ITomeKnowledge.class)
    public static Capability<ITomeKnowledge> TOMES = null;

    /** The key the capability data is filed under in the villager's saved NBT. */
    private static final ResourceLocation KEY =
            new ResourceLocation(RLCraftVillagerTomes.MODID, "tomes");

    /** NBT tag names. Short, because they are written once per villager per save. */
    private static final String TAG_LIST = "Tomes";
    private static final String TAG_ID = "id";
    private static final String TAG_LEVEL = "lvl";
    private static final String TAG_BANKED = "Banked";
    private static final String TAG_FAILURES = "Fails";
    private static final String TAG_OWED = "n";
    private static final String TAG_SLOTS = "Slots";
    private static final String TAG_REQUEST = "Want";
    private static final String TAG_META = "m";
    private static final String TAG_NEED = "need";
    private static final String TAG_GOT = "got";

    private CapabilityTomeKnowledge() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(ITomeKnowledge.class, new Storage(),
                new Callable<ITomeKnowledge>() {
                    @Override
                    public ITomeKnowledge call() {
                        return new TomeKnowledge();
                    }
                });
    }

    /**
     * The tome data for an entity, or null when there is none.
     *
     * <p>Null is the normal answer for anything that is not a villager, so callers check
     * it rather than assuming. It is also the answer during the brief window before
     * capability registration completes, which is why nothing here throws.
     */
    public static ITomeKnowledge get(Entity entity) {
        if (TOMES == null || entity == null || !entity.hasCapability(TOMES, null)) {
            return null;
        }
        return entity.getCapability(TOMES, null);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        // Only real villagers. Zombie villagers are a different entity class entirely, so
        // curing one produces a fresh EntityVillager with no tomes - the knowledge does
        // not survive the round trip, and that is documented rather than worked around.
        if (event.getObject() instanceof EntityVillager) {
            event.addCapability(KEY, new Provider());
        }
    }

    /**
     * Reads and writes the tome map as a list rather than a compound of key/value pairs.
     *
     * <p>A list keeps the learning order, which is the order the trades appear in. A
     * compound would not: NBT compounds are unordered on disk and come back in hash order,
     * so a villager's trade list would quietly reshuffle itself every time the world
     * reloaded.
     */
    public static class Storage implements Capability.IStorage<ITomeKnowledge> {

        @Override
        public NBTBase writeNBT(Capability<ITomeKnowledge> capability, ITomeKnowledge instance,
                                EnumFacing side) {
            NBTTagList list = new NBTTagList();
            for (Tome tome : instance.view()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(TAG_ID, tome.getEnchantment().toString());
                tag.setInteger(TAG_LEVEL, tome.getLevel());
                list.appendTag(tag);
            }
            NBTTagCompound root = new NBTTagCompound();
            root.setTag(TAG_LIST, list);
            root.setFloat(TAG_BANKED, instance.getBankedChance());

            if (instance instanceof TomeKnowledge) {
                NBTTagList fails = new NBTTagList();
                for (Map.Entry<ResourceLocation, Float> entry
                        : instance.pityView().entrySet()) {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setString(TAG_ID, entry.getKey().toString());
                    tag.setFloat(TAG_OWED, entry.getValue().floatValue());
                    fails.appendTag(tag);
                }
                root.setTag(TAG_FAILURES, fails);
            }

            root.setInteger(TAG_SLOTS, instance.getUnlockedSlots());
            NBTTagList want = new NBTTagList();
            for (BountyItem line : instance.getRequest()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(TAG_ID, line.getItemName().toString());
                tag.setInteger(TAG_META, line.getMeta());
                tag.setInteger(TAG_NEED, line.getRequired());
                tag.setInteger(TAG_GOT, line.getDelivered());
                want.appendTag(tag);
            }
            root.setTag(TAG_REQUEST, want);
            return root;
        }

        @Override
        public void readNBT(Capability<ITomeKnowledge> capability, ITomeKnowledge instance,
                            EnumFacing side, NBTBase nbt) {
            instance.clear();
            if (!(nbt instanceof NBTTagCompound)) {
                return;
            }
            NBTTagList list = ((NBTTagCompound) nbt).getTagList(TAG_LIST, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tome = list.getCompoundTagAt(i);
                String id = tome.getString(TAG_ID);
                int level = tome.getInteger(TAG_LEVEL);
                // A blank id means the entry was corrupted or hand-edited. Skipping it
                // loses one trade; letting a null ResourceLocation through would take the
                // whole entity load down with it.
                // add, not replace: a villager taught under UPGRADE_TAKES_NEW_SLOT holds the
                // same enchantment at more than one level, and every entry is its own trade.
                // The on-disk format is unchanged from when only one level per enchantment
                // was possible, so villagers saved by earlier versions load as they were.
                if (id != null && !id.isEmpty() && level >= 1) {
                    instance.add(new ResourceLocation(id), level);
                }
            }

            // Both of these are absent on villagers saved before the chance system existed.
            // getFloat and getTagList both return harmless empties for a missing key, so an
            // older villager simply loads with nothing banked and no failures - which is
            // exactly right for one that has never been gambled on.
            NBTTagCompound root = (NBTTagCompound) nbt;
            if (instance instanceof TomeKnowledge) {
                ((TomeKnowledge) instance).setBankedChance(root.getFloat(TAG_BANKED));

                NBTTagList fails = root.getTagList(TAG_FAILURES, 10);
                for (int i = 0; i < fails.tagCount(); i++) {
                    NBTTagCompound tag = fails.getCompoundTagAt(i);
                    String id = tag.getString(TAG_ID);
                    if (id != null && !id.isEmpty()) {
                        ((TomeKnowledge) instance).setPityBonus(
                                new ResourceLocation(id), tag.getFloat(TAG_OWED));
                    }
                }
            }

            // Villagers taught before slots could lock have no Slots tag. Treating them as
            // having unlocked exactly what they have filled - and at least one - means an
            // established villager keeps working and simply owes a bounty for its next slot,
            // rather than waking up locked out of trades it already sells.
            if (root.hasKey(TAG_SLOTS)) {
                instance.setUnlockedSlots(root.getInteger(TAG_SLOTS));
            } else {
                instance.setUnlockedSlots(Math.max(1, instance.count()));
            }

            NBTTagList want = root.getTagList(TAG_REQUEST, 10);
            List<BountyItem> request = new ArrayList<BountyItem>();
            for (int i = 0; i < want.tagCount(); i++) {
                NBTTagCompound tag = want.getCompoundTagAt(i);
                String id = tag.getString(TAG_ID);
                if (id != null && !id.isEmpty()) {
                    request.add(new BountyItem(new ResourceLocation(id), tag.getInteger(TAG_META),
                            tag.getInteger(TAG_NEED), tag.getInteger(TAG_GOT)));
                }
            }
            instance.setRequest(request);
        }
    }

    /**
     * Holds one villager's tome data and hands it out on request.
     *
     * <p>The instance is created eagerly rather than lazily: a villager that has learned
     * nothing still needs somewhere to put the first book, and an empty
     * {@link TomeKnowledge} is a map with nothing in it - cheaper than the null check it
     * would take to avoid.
     */
    public static class Provider implements ICapabilitySerializable<NBTBase> {

        private final ITomeKnowledge instance = new TomeKnowledge();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == TOMES;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == TOMES ? (T) instance : null;
        }

        @Override
        public NBTBase serializeNBT() {
            return TOMES.getStorage().writeNBT(TOMES, instance, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt) {
            TOMES.getStorage().readNBT(TOMES, instance, null, nbt);
        }
    }
}
