package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.ward.ItemVillagerWard;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * The ledger and the lectern that holds it - the only things this mod registers.
 *
 * <p>Registering anything makes the mod client-required: a client without it cannot map the
 * server's item ids and is turned away on join. That used to be the line this mod would not
 * cross, which is why the quest log was a vanilla written book driven from chat. A real GUI
 * with item icons, highlighting and a Deliver button is worth more than the server-only
 * claim, so 2.2.0 crosses it deliberately.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID)
public final class ModContent {

    public static ItemLedger LEDGER;
    public static BlockLedgerLectern LEDGER_LECTERN;
    public static Item LEDGER_LECTERN_ITEM;
    public static ItemVillagerWard VILLAGER_WARD;

    /**
     * 1.14's book sounds, the same files VillagerBackport ships - 1.12 has no book sounds of
     * its own, so a page flip and a book being set down have to come with the mod.
     */
    public static SoundEvent LEDGER_OPEN;
    public static SoundEvent LEDGER_PUT;
    /** Parchment unrolling and rolling back up - the bounty scroll. */
    public static SoundEvent SCROLL_OPEN;
    public static SoundEvent SCROLL_CLOSE;

    private ModContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        LEDGER_LECTERN = new BlockLedgerLectern(Material.WOOD, SoundType.WOOD, 2.5F);
        name(LEDGER_LECTERN, "ledger_lectern");
        LEDGER_LECTERN.setCreativeTab(CreativeTabs.DECORATIONS);
        event.getRegistry().register(LEDGER_LECTERN);

        GameRegistry.registerTileEntity(TileLedgerLectern.class,
                new ResourceLocation(RLCraftVillagerTomes.MODID, "ledger_lectern"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        LEDGER = new ItemLedger();
        LEDGER.setRegistryName(new ResourceLocation(RLCraftVillagerTomes.MODID, "ledger"));
        LEDGER.setTranslationKey(RLCraftVillagerTomes.MODID + ".ledger");
        LEDGER.setCreativeTab(CreativeTabs.MISC);
        event.getRegistry().register(LEDGER);

        VILLAGER_WARD = new ItemVillagerWard();
        VILLAGER_WARD.setRegistryName(
                new ResourceLocation(RLCraftVillagerTomes.MODID, "villager_ward"));
        VILLAGER_WARD.setTranslationKey(RLCraftVillagerTomes.MODID + ".villager_ward");
        VILLAGER_WARD.setCreativeTab(CreativeTabs.MISC);
        event.getRegistry().register(VILLAGER_WARD);

        LEDGER_LECTERN_ITEM = new ItemBlock(LEDGER_LECTERN)
                .setRegistryName(LEDGER_LECTERN.getRegistryName());
        event.getRegistry().register(LEDGER_LECTERN_ITEM);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new LedgerCopyRecipe()
                .setRegistryName(new ResourceLocation(RLCraftVillagerTomes.MODID, "ledger_copy")));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        LEDGER_OPEN = sound(event, "item.ledger.open");
        LEDGER_PUT = sound(event, "block.ledger_lectern.put");
        SCROLL_OPEN = sound(event, "ui.bounty_scroll.open");
        SCROLL_CLOSE = sound(event, "ui.bounty_scroll.close");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String path) {
        ResourceLocation id = new ResourceLocation(RLCraftVillagerTomes.MODID, path);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }

    private static void name(Block block, String path) {
        block.setRegistryName(new ResourceLocation(RLCraftVillagerTomes.MODID, path));
        block.setTranslationKey(RLCraftVillagerTomes.MODID + "." + path);
    }
}
