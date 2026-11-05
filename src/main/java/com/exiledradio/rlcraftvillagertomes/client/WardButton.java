package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerNetwork;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import com.exiledradio.rlcraftvillagertomes.ward.WardMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

import java.util.Arrays;

/**
 * The ward toggle on a warded villager's trade screen.
 *
 * <p>Drawn on top of whichever merchant screen is open - vanilla's {@link GuiMerchant}, or
 * VillagerBackport's 1.14-style replacement, which is a plain {@link GuiContainer} and is
 * recognised by class name so this mod never has to compile against it. Neither screen is
 * changed or replaced: the button is pixels and a click handler, so any other mod decorating
 * the trade screen keeps working.
 *
 * <p>It sits just outside the window's top-right corner, like a tab, where neither screen
 * or anything drawn into them can be underneath it.
 */
@Mod.EventBusSubscriber(modid = RLCraftVillagerTomes.MODID, value = Side.CLIENT)
public final class WardButton {

    private static final String BACKPORT_SCREEN =
            "com.exiledradio.villagerbackport.client.GuiVillagerMerchant";

    private static final int SIZE = 20;

    private static int entityId = -1;
    private static WardMode mode = WardMode.NONE;

    private WardButton() {
    }

    static void setState(int id, int ordinal) {
        entityId = id;
        mode = WardMode.of(ordinal);
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        // Leaving the trade screen forgets the villager, so its button can never appear on
        // the next one. Opening a merchant screen does not clear it: the status packet is sent
        // before the window opens, and would be wiped by its own screen.
        if (event.getGui() == null || !isMerchantScreen(event.getGui())) {
            entityId = -1;
            mode = WardMode.NONE;
        }
    }

    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiContainer screen = activeScreen(event.getGui());
        if (screen == null) {
            return;
        }
        int x = x(screen);
        int y = y(screen);
        boolean over = contains(screen, event.getMouseX(), event.getMouseY());

        int border = color(mode);
        Gui.drawRect(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, 0xFF1E140C);
        Gui.drawRect(x, y, x + SIZE, y + SIZE, border);
        Gui.drawRect(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, over ? 0xFF4A3424 : 0xFF33241A);

        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(
                new ItemStack(ModContent.VILLAGER_WARD), x + 2, y + 2);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();

        if (over) {
            screen.drawHoveringText(Arrays.asList(
                    TextFormatting.GOLD + "Villager Ward",
                    TextFormatting.WHITE + mode.getLabel(),
                    TextFormatting.GRAY + mode.getDescription(),
                    TextFormatting.DARK_GRAY + "Click to switch"),
                    event.getMouseX(), event.getMouseY());
            GlStateManager.disableLighting();
        }
    }

    @SubscribeEvent
    public static void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        GuiContainer screen = activeScreen(event.getGui());
        if (screen == null || !Mouse.getEventButtonState() || Mouse.getEventButton() != 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * screen.width / screen.mc.displayWidth;
        int mouseY = screen.height - Mouse.getEventY() * screen.height / screen.mc.displayHeight - 1;
        if (!contains(screen, mouseX, mouseY)) {
            return;
        }
        event.setCanceled(true);
        screen.mc.getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        LedgerNetwork.CHANNEL.sendToServer(new LedgerNetwork.WardCycleMessage(entityId));
    }

    // ---------------------------------------------------------------- helpers

    private static GuiContainer activeScreen(GuiScreen gui) {
        if (mode == WardMode.NONE || entityId < 0 || !isMerchantScreen(gui)) {
            return null;
        }
        return (GuiContainer) gui;
    }

    private static boolean isMerchantScreen(GuiScreen gui) {
        return gui instanceof GuiMerchant
                || (gui instanceof GuiContainer && gui.getClass().getName().equals(BACKPORT_SCREEN));
    }

    private static int x(GuiContainer screen) {
        return screen.getGuiLeft() + screen.getXSize() + 3;
    }

    private static int y(GuiContainer screen) {
        return screen.getGuiTop() + 2;
    }

    private static boolean contains(GuiContainer screen, int mouseX, int mouseY) {
        int x = x(screen);
        int y = y(screen);
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }

    private static int color(WardMode mode) {
        switch (mode) {
            case ALL:
                return 0xFFE0B84A;
            case PLAYERS:
                return 0xFF4A90D9;
            default:
                return 0xFF4CAF50;
        }
    }
}
