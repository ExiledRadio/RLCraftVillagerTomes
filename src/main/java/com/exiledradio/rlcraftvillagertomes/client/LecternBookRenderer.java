package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.ledger.BlockLedgerLectern;
import com.exiledradio.rlcraftvillagertomes.ledger.TileLedgerLectern;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * The ledger lying open on its lectern.
 *
 * <p>The geometry and placement are VillagerBackport's lectern book, which is 1.14's: the
 * enchanting table's book model, lying still at the slope of the stand. Drawn a shade darker
 * than the enchanting table's copy so it reads as the ledger's leather rather than a stray
 * enchanting book.
 */
public class LecternBookRenderer extends TileEntitySpecialRenderer<TileLedgerLectern> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/enchanting_table_book.png");

    private static final float SPREAD = 1.2F;
    private static final float FLIP_LEFT = 0.1F;
    private static final float FLIP_RIGHT = 0.9F;
    private static final float TILT = 67.5F;

    private final ModelBook model = new ModelBook();

    @Override
    public void render(TileLedgerLectern lectern, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        IBlockState state = lectern.getWorld().getBlockState(lectern.getPos());
        if (!(state.getBlock() instanceof BlockLedgerLectern)
                || !state.getValue(BlockLedgerLectern.HAS_BOOK).booleanValue()) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.0F + 0.0625F, (float) z + 0.5F);

        float angle = state.getValue(BlockLedgerLectern.FACING).rotateY().getHorizontalAngle();
        GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(TILT, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.0F, -0.125F, 0.0F);

        bindTexture(TEXTURE);
        GlStateManager.enableCull();
        GlStateManager.color(0.72F, 0.62F, 0.52F, 1.0F);
        model.render(null, 0.0F, FLIP_LEFT, FLIP_RIGHT, SPREAD, 0.0F, 0.0625F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
    }
}
