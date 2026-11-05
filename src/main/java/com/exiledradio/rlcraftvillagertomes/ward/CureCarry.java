package com.exiledradio.rlcraftvillagertomes.ward;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Carries everything this mod knows about a villager - its ward, its taught books, its slots,
 * its bounty, its pity - through zombification and back out of a cure.
 *
 * <p>1.12 builds a brand new villager when a zombie villager is cured, so without this a
 * warded villager would come back unwarded, which makes the "players and zombies" mode
 * pointless, and every librarian would come back having forgotten every book it was taught.
 *
 * <p>Neither half of a conversion has an event, so this is the same handover VillagerBackport
 * and RLCraftTweaks use, and it works alongside both: a villager dies, and a zombie villager
 * appears where it fell within a few ticks; later that zombie is removed, and a villager
 * appears on the exact spot it stood. The data rides on the zombie's own saved NBT in between,
 * so it survives the world being saved and reloaded during the five-minute cure. Each half
 * takes the closest candidate rather than the first, so a row of villagers cured together
 * each get their own record back.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class CureCarry {

    private static final String CARRIED = "rlcraftvillagertomes:Carried";

    /** How far the zombie may stand up from where the villager fell. */
    private static final double HANDOVER_RANGE = 2.0D;
    /** How far a cured villager may appear from the zombie it came out of. */
    private static final double SAME_SPOT = 0.5D;
    /** How long a dead villager's record waits for its zombie. The spawn is the same tick. */
    private static final long HANDOVER_TICKS = 5L;

    private static final List<Snapshot> PENDING = new ArrayList<Snapshot>();

    private static final class Snapshot {
        final int dimension;
        final double x;
        final double y;
        final double z;
        final long at;
        final NBTTagCompound data;

        Snapshot(EntityVillager villager, NBTTagCompound data) {
            dimension = villager.world.provider.getDimension();
            x = villager.posX;
            y = villager.posY;
            z = villager.posZ;
            at = villager.world.getTotalWorldTime();
            this.data = data;
        }
    }

    private CureCarry() {
    }

    /**
     * Takes a copy of the villager's record as it dies. Whether a zombie will rise from it is
     * not known yet, so every death is copied and the unclaimed ones expire a few ticks later.
     * Only villagers with something worth keeping are copied at all.
     */
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager)
                || event.getEntityLiving().world.isRemote) {
            return;
        }
        EntityVillager villager = (EntityVillager) event.getEntityLiving();
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null || (tomes.getWard() == 0 && tomes.count() == 0
                && tomes.getUnlockedSlots() <= 1 && tomes.getRequest().isEmpty())) {
            return;
        }
        NBTBase saved = CapabilityTomeKnowledge.TOMES.writeNBT(tomes, null);
        if (!(saved instanceof NBTTagCompound)) {
            return;
        }
        long now = villager.world.getTotalWorldTime();
        expire(now);
        PENDING.add(new Snapshot(villager, (NBTTagCompound) saved));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        if (event.getEntity() instanceof EntityZombieVillager) {
            claimForZombie((EntityZombieVillager) event.getEntity());
        } else if (event.getEntity() instanceof EntityVillager) {
            restoreFromZombie((EntityVillager) event.getEntity());
        }
    }

    /** A zombie villager appeared: if a villager with a record just fell here, it is that one. */
    private static void claimForZombie(EntityZombieVillager zombie) {
        if (zombie.getEntityData().hasKey(CARRIED, 10) || PENDING.isEmpty()) {
            return;
        }
        long now = zombie.world.getTotalWorldTime();
        expire(now);
        Snapshot best = null;
        double bestDistance = HANDOVER_RANGE * HANDOVER_RANGE;
        for (Snapshot snapshot : PENDING) {
            if (snapshot.dimension != zombie.world.provider.getDimension()) {
                continue;
            }
            double dx = zombie.posX - snapshot.x;
            double dy = zombie.posY - snapshot.y;
            double dz = zombie.posZ - snapshot.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance) {
                best = snapshot;
                bestDistance = distance;
            }
        }
        if (best != null) {
            PENDING.remove(best);
            zombie.getEntityData().setTag(CARRIED, best.data);
        }
    }

    /**
     * A villager appeared: if a zombie carrying a record is standing on its spot, this is that
     * zombie being cured. Vanilla removes the zombie before spawning the villager, but a removed
     * entity stays in the world's lists until the end of the tick, so it is still found here.
     */
    private static void restoreFromZombie(EntityVillager villager) {
        AxisAlignedBB box = new AxisAlignedBB(villager.posX - SAME_SPOT, villager.posY - SAME_SPOT,
                villager.posZ - SAME_SPOT, villager.posX + SAME_SPOT, villager.posY + SAME_SPOT,
                villager.posZ + SAME_SPOT);
        EntityZombieVillager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EntityZombieVillager zombie
                : villager.world.getEntitiesWithinAABB(EntityZombieVillager.class, box)) {
            if (!zombie.getEntityData().hasKey(CARRIED, 10)) {
                continue;
            }
            double distance = zombie.getDistanceSq(villager);
            if (distance < bestDistance) {
                best = zombie;
                bestDistance = distance;
            }
        }
        if (best == null) {
            return;
        }
        // A zombie on its way out is the cure. A living one only counts on the exact same spot,
        // for a mod that spawns the cured villager before removing the zombie; one merely
        // standing beside a villager that spawned there keeps its record for its own cure.
        if (!best.isDead && bestDistance > 0.01D) {
            return;
        }
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes != null) {
            CapabilityTomeKnowledge.TOMES.readNBT(tomes, null,
                    best.getEntityData().getCompoundTag(CARRIED));
        }
        best.getEntityData().removeTag(CARRIED);
    }

    private static void expire(long now) {
        Iterator<Snapshot> it = PENDING.iterator();
        while (it.hasNext()) {
            Snapshot snapshot = it.next();
            if (now - snapshot.at > HANDOVER_TICKS || now < snapshot.at) {
                it.remove();
            }
        }
    }
}
