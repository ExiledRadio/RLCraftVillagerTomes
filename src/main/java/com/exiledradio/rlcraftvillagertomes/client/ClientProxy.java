package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.CommonProxy;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerView;
import com.exiledradio.rlcraftvillagertomes.ledger.TileLedgerLectern;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/** Client half of {@link CommonProxy}. Only ever loaded on a client, by @SidedProxy. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileLedgerLectern.class,
                new LecternBookRenderer());
    }

    @Override
    public void showWard(final int entityId, final int mode) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                WardButton.setState(entityId, mode);
            }
        });
    }

    @Override
    public void showNotice(final byte tone, final String json) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                net.minecraft.util.text.ITextComponent text;
                try {
                    text = net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(json);
                } catch (RuntimeException e) {
                    text = null;
                }
                if (text == null) {
                    return;
                }
                // Translated here, in the player's own language, then stripped of any color
                // codes - each screen colors the line for its own background.
                String plain = net.minecraft.util.text.TextFormatting
                        .getTextWithoutFormattingCodes(text.getUnformattedText());
                if (plain == null || plain.isEmpty()) {
                    return;
                }
                if (mc.currentScreen instanceof NoticeTarget) {
                    ((NoticeTarget) mc.currentScreen).showNotice(tone, plain);
                } else if (mc.ingameGUI != null) {
                    mc.ingameGUI.setOverlayMessage(NoticeTarget.actionBarColor(tone) + plain,
                            false);
                }
            }
        });
    }

    @Override
    public void showNote(final NBTTagCompound tag) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(new GuiBountyNote(
                        com.exiledradio.rlcraftvillagertomes.bounty.BountyNote.fromTag(tag)));
            }
        });
    }

    @Override
    public void showLedger(final NBTTagCompound tag) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                LedgerView view = LedgerView.fromTag(tag);
                // A refresh after a button press updates the open screen in place, so the
                // scroll position survives a delivery.
                if (mc.currentScreen instanceof GuiLedger
                        && ((GuiLedger) mc.currentScreen).sameLedger(view)) {
                    ((GuiLedger) mc.currentScreen).setView(view);
                } else {
                    mc.displayGuiScreen(new GuiLedger(view));
                }
            }
        });
    }
}
