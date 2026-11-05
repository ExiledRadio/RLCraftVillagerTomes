package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.ledger.BlockLedgerLectern;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Item models, and the lectern's state mapper.
 *
 * <p>The book on a lectern is drawn by {@link LecternBookRenderer}, so whether one is there
 * changes nothing about the block model - ignoring the property here keeps the blockstate
 * file to the four facings, and keeps one baked model per facing.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID, value = Side.CLIENT)
public final class ClientModels {

    private ClientModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomStateMapper(ModContent.LEDGER_LECTERN,
                new StateMap.Builder().ignore(BlockLedgerLectern.HAS_BOOK).build());
        inventory(ModContent.LEDGER);
        inventory(ModContent.LEDGER_LECTERN_ITEM);
        inventory(ModContent.VILLAGER_WARD);
    }

    private static void inventory(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
