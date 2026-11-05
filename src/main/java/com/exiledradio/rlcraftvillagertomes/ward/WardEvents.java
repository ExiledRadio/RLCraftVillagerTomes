package com.exiledradio.rlcraftvillagertomes.ward;

import com.exiledradio.rlcraftvillagertomes.Feedback;
import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerNetwork;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Applying a Villager Ward, and what a warded villager is protected from.
 *
 * <p>Damage is stopped at {@link LivingAttackEvent}, which every hit goes through before any
 * armor, absorption or death check - so a blocked hit leaves no trace at all: no red flash,
 * no knockback, no sound. The void and {@code /kill} still get through in every mode; a ward
 * that made a villager impossible to remove would be a bug, not a feature.
 *
 * <p>Lightning is stopped too. It does not need to deal damage to change a villager - 1.12
 * turns one struck by lightning into a witch outright - and a warded librarian becoming a
 * witch is the same loss as it dying.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class WardEvents {

    private static final String PREFIX =
            TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;

    private WardEvents() {
    }

    public static WardMode wardOf(EntityVillager villager) {
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        return tomes == null ? WardMode.NONE : WardMode.of(tomes.getWard());
    }

    // ---------------------------------------------------------------- applying

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || held.getItem() != ModContent.VILLAGER_WARD
                || !(event.getTarget() instanceof EntityVillager)
                || !event.getEntityPlayer().isSneaking()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        EntityVillager villager = (EntityVillager) event.getTarget();
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null) {
            return;
        }
        if (WardMode.of(tomes.getWard()) != WardMode.NONE) {
            Feedback.notify(player, Feedback.INFO, "Already warded ("
                    + WardMode.of(tomes.getWard()).getLabel().toLowerCase()
                    + ") - open its trades to change what can hurt it");
            return;
        }

        tomes.setWard(WardMode.PLAYERS.ordinal());
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ITEM_TOTEM_USE, SoundCategory.NEUTRAL, 0.6F, 1.2F);
        if (villager.world instanceof WorldServer) {
            ((WorldServer) villager.world).spawnParticle(EnumParticleTypes.TOTEM,
                    villager.posX, villager.posY + villager.height * 0.6D, villager.posZ,
                    40, 0.4D, 0.6D, 0.4D, 0.4D);
        }
        Feedback.notify(player, Feedback.GOOD, "Warded - only players can hurt it now."
                + " Click the ward on its trade screen to change that");
    }

    // -------------------------------------------------------------- protecting

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager)
                || event.getEntityLiving().world.isRemote) {
            return;
        }
        WardMode mode = wardOf((EntityVillager) event.getEntityLiving());
        if (mode != WardMode.NONE && !allows(mode, event.getSource())) {
            event.setCanceled(true);
        }
    }

    static boolean allows(WardMode mode, DamageSource source) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return true;
        }
        Entity attacker = source.getTrueSource();
        boolean player = attacker instanceof EntityPlayer;
        // Zombie pigmen are zombies to the class hierarchy but cannot turn a villager, so
        // letting them through would be a hole in the ward with nothing to show for it.
        boolean zombie = attacker instanceof EntityZombie && !(attacker instanceof EntityPigZombie);
        switch (mode) {
            case PLAYERS:
                return player;
            case PLAYERS_AND_ZOMBIES:
                return player || zombie;
            default:
                return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof EntityVillager && !event.getEntity().world.isRemote
                && wardOf((EntityVillager) event.getEntity()) != WardMode.NONE) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------ trade screen

    /**
     * Tells the trader's client whether this villager is warded, so the trade screen can
     * show the button. Sent for unwarded villagers too, so a stale icon from the last
     * villager can never linger.
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof ContainerMerchant)
                || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        EntityVillager villager = tradingWith(player);
        sendStatus(player, villager);
    }

    static void sendStatus(EntityPlayerMP player, EntityVillager villager) {
        int id = villager == null ? -1 : villager.getEntityId();
        int mode = villager == null ? 0 : wardOf(villager).ordinal();
        LedgerNetwork.CHANNEL.sendTo(new LedgerNetwork.WardStatusMessage(id, mode), player);
    }

    /** The villager whose trade screen this player has open, if any. */
    static EntityVillager tradingWith(EntityPlayer player) {
        for (EntityVillager villager : player.world.getEntitiesWithinAABB(EntityVillager.class,
                player.getEntityBoundingBox().grow(16.0D))) {
            if (villager.getCustomer() == player) {
                return villager;
            }
        }
        return null;
    }

    /** The trade-screen button was pressed. Re-checks that the player really is trading. */
    public static void cycle(EntityPlayerMP player, int entityId) {
        Entity entity = player.world.getEntityByID(entityId);
        if (!(entity instanceof EntityVillager)) {
            return;
        }
        EntityVillager villager = (EntityVillager) entity;
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (villager.getCustomer() != player || tomes == null
                || WardMode.of(tomes.getWard()) == WardMode.NONE) {
            return;
        }
        WardMode next = WardMode.of(tomes.getWard()).next();
        tomes.setWard(next.ordinal());
        villager.world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.BLOCK_NOTE_CHIME, SoundCategory.NEUTRAL, 0.5F,
                next == WardMode.ALL ? 1.4F : next == WardMode.PLAYERS ? 1.1F : 0.8F);
        sendStatus(player, villager);
    }
}
