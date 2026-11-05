package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Taking the ledger back off a lectern before Carry On can pick the lectern up.
 *
 * <p>Sneak-click with an empty hand is the take-back gesture, and it is exactly Carry On's
 * gesture for lifting any block with a tile entity. Carry On does that from a
 * {@code RightClickBlock} handler that ignores canceled clicks, so claiming the click first
 * here - and only while a ledger is on the stand - hands the book back and leaves Carry On
 * nothing to do. An empty lectern is still carried like any other block.
 *
 * <p>Canceled on both sides: the client has to stop too, or it goes on to try the block and
 * Carry On's client half shows a pick-up that the server never makes.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class LecternClickEvents {

    private LecternClickEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking() || !player.getHeldItemMainhand().isEmpty()) {
            return;
        }
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockLedgerLectern)
                || !state.getValue(BlockLedgerLectern.HAS_BOOK).booleanValue()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        // Both hands fire for one click; only the main hand takes the book.
        if (event.getHand() == EnumHand.MAIN_HAND && !event.getWorld().isRemote) {
            BlockLedgerLectern.takeBook(event.getWorld(), event.getPos(), state, player);
        }
    }
}
