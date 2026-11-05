package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A lectern for the villager ledger - a delivery desk for every villager written in it.
 *
 * <p>Put a ledger on it, and right-clicking the lectern opens the ledger with a Deliver
 * button on each villager: one click hands that villager everything it still needs from
 * your inventory, without walking over to it. Only villagers that are loaded can take a
 * delivery, which keeps this a convenience for a village you are working in rather than a
 * way to pay off villagers on the far side of the world.
 *
 * <p>Sneak-click with an empty hand takes the ledger back.
 *
 * <p>Shape and model are the 1.14 lectern as ported in VillagerBackport, re-textured in dark
 * spruce so the two never look like the same block when both mods are installed.
 */
public class BlockLedgerLectern extends Block {

    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool HAS_BOOK = PropertyBool.create("has_book");

    private static final int META_HAS_BOOK = 4;
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 15.0D / 16.0D, 1.0D);

    public BlockLedgerLectern(Material material, SoundType sound, float hardness) {
        super(material);
        setHardness(hardness);
        setResistance(hardness * 5.0F);
        setSoundType(sound);
        setLightOpacity(0);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(HAS_BOOK, Boolean.FALSE));
    }

    // ------------------------------------------------------------------ state

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, HAS_BOOK);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(HAS_BOOK, Boolean.valueOf((meta & META_HAS_BOOK) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(HAS_BOOK).booleanValue()) {
            meta |= META_HAS_BOOK;
        }
        return meta;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // ------------------------------------------------------------------ shape

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
                                            EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    // ------------------------------------------------------------------- tile

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileLedgerLectern();
    }

    // ----------------------------------------------------------------- using

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);

        if (state.getValue(HAS_BOOK).booleanValue()) {
            // Main hand only: the off hand would open the screen a second time for one click.
            if (hand != EnumHand.MAIN_HAND) {
                return true;
            }
            if (!world.isRemote) {
                if (player.isSneaking() && held.isEmpty()) {
                    takeBook(world, pos, state, player);
                } else if (player instanceof EntityPlayerMP) {
                    LedgerActions.openLectern((EntityPlayerMP) player, pos);
                }
            }
            return true;
        }

        if (QuestLog.isLog(held) || QuestLog.isLegacyLog(held)) {
            if (!world.isRemote) {
                ItemStack book = QuestLog.isLegacyLog(held) ? QuestLog.convertLegacy(held) : held;
                placeBook(world, pos, state, player.capabilities.isCreativeMode
                        ? book.copy() : book);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        return false;
    }

    private static void placeBook(World world, BlockPos pos, IBlockState state, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileLedgerLectern)) {
            return;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        ((TileLedgerLectern) tile).setBook(one);
        world.setBlockState(pos, state.withProperty(HAS_BOOK, Boolean.TRUE), 3);
        world.playSound(null, pos, ModContent.LEDGER_PUT, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    static void takeBook(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileLedgerLectern)) {
            return;
        }
        ItemStack book = ((TileLedgerLectern) tile).removeBook();
        world.setBlockState(pos, state.withProperty(HAS_BOOK, Boolean.FALSE), 3);
        if (!book.isEmpty() && !player.inventory.addItemStackToInventory(book)) {
            player.dropItem(book, false);
        }
        world.playSound(null, pos, ModContent.LEDGER_PUT, SoundCategory.BLOCKS, 1.0F, 0.9F);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileLedgerLectern) {
            ItemStack book = ((TileLedgerLectern) tile).removeBook();
            if (!book.isEmpty()) {
                EnumFacing facing = state.getValue(FACING);
                EntityItem dropped = new EntityItem(world,
                        pos.getX() + 0.5D + 0.25D * facing.getXOffset(),
                        pos.getY() + 1,
                        pos.getZ() + 0.5D + 0.25D * facing.getZOffset(),
                        book);
                dropped.setDefaultPickupDelay();
                world.spawnEntity(dropped);
            }
        }
        super.breakBlock(world, pos, state);
    }
}
