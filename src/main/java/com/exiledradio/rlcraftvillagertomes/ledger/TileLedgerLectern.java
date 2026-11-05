package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The ledger lying on a {@link BlockLedgerLectern}. */
public class TileLedgerLectern extends TileEntity {

    private ItemStack book = ItemStack.EMPTY;

    public ItemStack getBook() {
        return book;
    }

    public boolean hasBook() {
        return QuestLog.isLog(book);
    }

    public void setBook(ItemStack stack) {
        book = stack;
        sync();
    }

    public ItemStack removeBook() {
        ItemStack taken = book;
        book = ItemStack.EMPTY;
        sync();
        return taken;
    }

    /** Called after the ledger's entries were rewritten in place, to save and resend them. */
    public void bookChanged() {
        sync();
    }

    /** Whoever is using it has to still be there, and still be near enough to reach it. */
    public boolean isUsableBy(EntityPlayer player) {
        return world.getTileEntity(pos) == this
                && hasBook()
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= 64.0D;
    }

    private void sync() {
        markDirty();
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        book = compound.hasKey("Book", 10)
                ? new ItemStack(compound.getCompoundTag("Book"))
                : ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!book.isEmpty()) {
            compound.setTag("Book", book.writeToNBT(new NBTTagCompound()));
        }
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    /** Only the book changes with the state, so the tile - and the book in it - must survive it. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState,
                                 IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** The book lies just above the block, so rendering has to survive the block being culled. */
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos).grow(1.0D);
    }
}
