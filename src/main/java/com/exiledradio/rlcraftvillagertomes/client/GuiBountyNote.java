package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyNote;
import com.exiledradio.rlcraftvillagertomes.bounty.ItemPattern;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;

/**
 * A villager's bounty, on a scroll that unrolls - what a sneak-click with an empty hand shows.
 *
 * <p>The animation is the two rolled ends starting together in the middle of the screen and
 * sliding apart, the parchment revealed between them by a scissor box. Only the reveal moves:
 * the text is laid out once at its final place and never scaled or rotated, because
 * Minecraft's pixel font looks awful mid-transform, and a scroll opening is exactly a reveal
 * rather than a zoom. Closing (Escape, the inventory key, or any click) runs it backwards.
 *
 * <p>Same reading rules as the ledger: rows you carry enough to finish are banded green,
 * finished rows are struck through.
 */
public class GuiBountyNote extends GuiScreen implements NoticeTarget {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            RLCraftVillagerTomes.MODID, "textures/gui/bounty_scroll.png");

    private static final int PAPER_W = 200;
    private static final int PAPER_TEX_H = 240;
    private static final int ROLL_W = 224;
    private static final int ROLL_H = 16;
    /** How far a rolled end sits over the paper edge. */
    private static final int ROLL_OVERLAP = 5;
    private static final int PAD_X = 14;

    private static final long OPEN_MS = 420L;
    private static final long CLOSE_MS = 230L;

    private static final int INK = 0xFF3B2A1C;
    private static final int INK_SOFT = 0xFF7D6A54;
    private static final int GREEN_TEXT = 0xFF1C6E2E;
    private static final int GREEN_BAND = 0x6688C47A;
    private static final int DONE_TEXT = 0xFFA6977E;

    private static final int ROW_H = 18;

    private final BountyNote note;
    private final String title;

    private long openedAt;
    private long closingAt;

    private int paperH;
    /** The banner, wrapped to the scroll's width when laid out. */
    private java.util.List<String> bannerLines = new java.util.ArrayList<String>();

    public GuiBountyNote(BountyNote note) {
        this.note = note;
        this.title = parseName(note.name).getFormattedText();
    }

    private static ITextComponent parseName(String json) {
        try {
            ITextComponent parsed = ITextComponent.Serializer.jsonToComponent(json);
            if (parsed != null) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
            // Fall through to plain text.
        }
        return new TextComponentString(json == null ? "Villager" : json);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void showNotice(byte tone, String text) {
        note.banner = text;
        note.bannerTone = tone;
        layout();
    }

    private void layout() {
        bannerLines = note.banner.isEmpty() ? new java.util.ArrayList<String>()
                : fontRenderer.listFormattedStringToWidth(note.banner, PAPER_W - PAD_X * 2);
        paperH = Math.max(70, Math.min(contentHeight(), height - 2 * ROLL_H - 16));
    }

    private static int bannerColor(byte tone) {
        switch (tone) {
            case com.exiledradio.rlcraftvillagertomes.Feedback.GOOD:
                return GREEN_TEXT;
            case com.exiledradio.rlcraftvillagertomes.Feedback.WARN:
                return 0xFF8A5A00;
            case com.exiledradio.rlcraftvillagertomes.Feedback.BAD:
                return 0xFF9A3B2E;
            default:
                return INK;
        }
    }

    @Override
    public void initGui() {
        layout();
        if (openedAt == 0L) {
            openedAt = Minecraft.getSystemTime();
            play(ModContent.SCROLL_OPEN, 0.95F + mc.world.rand.nextFloat() * 0.1F);
        }
    }

    private void play(net.minecraft.util.SoundEvent sound, float pitch) {
        if (sound != null) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(sound, pitch));
        }
    }

    // ---------------------------------------------------------------- layout

    private int contentHeight() {
        int h = 10 + 12 + 12 + 8 + (bannerLines.isEmpty() ? 0 : bannerLines.size() * 10 + 4);
        h += note.full || note.wanted.isEmpty() ? 14 : note.wanted.size() * ROW_H;
        if (note.chance) {
            h += 8 + 10 + note.bonuses.size() * 10;
        }
        if (!note.ward.isEmpty()) {
            h += 6 + 10;
        }
        return h + 10 + 10 + 10;
    }

    /** 0 = rolled up, 1 = fully open. */
    private float openness() {
        long now = Minecraft.getSystemTime();
        if (closingAt != 0L) {
            float t = Math.min(1.0F, (now - closingAt) / (float) CLOSE_MS);
            return 1.0F - easeInOut(t);
        }
        float t = Math.min(1.0F, (now - openedAt) / (float) OPEN_MS);
        return easeOutCubic(t);
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static float easeInOut(float t) {
        return t < 0.5F ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }

    // ------------------------------------------------------------------ draw

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float p = openness();
        if (closingAt != 0L && p <= 0.0F) {
            mc.displayGuiScreen(null);
            return;
        }

        int dim = (int) (0x88 * Math.min(1.0F, p * 1.4F));
        drawRect(0, 0, width, height, dim << 24);

        int cx = width / 2;
        int cy = height / 2;
        int paperX = cx - PAPER_W / 2;
        int paperY = cy - paperH / 2;
        int reveal = Math.round(paperH * p);
        int top = cy - reveal / 2;
        int bottom = top + reveal;

        ItemStack hovered = ItemStack.EMPTY;
        if (reveal > 0) {
            scissor(paperX, top, PAPER_W, reveal);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            drawPaper(paperX, paperY);
            hovered = drawContent(paperX + PAD_X, paperY, mouseX, mouseY,
                    p >= 1.0F && closingAt == 0L);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        int rollX = cx - ROLL_W / 2;
        drawRoll(rollX, top - ROLL_H + ROLL_OVERLAP);
        drawRoll(rollX, bottom - ROLL_OVERLAP);

        // Last, so the rolled ends never paint over a tooltip.
        if (!hovered.isEmpty()) {
            renderToolTip(hovered, mouseX, mouseY);
        }
    }

    private void drawPaper(int x, int y) {
        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        for (int dy = 0; dy < paperH; dy += PAPER_TEX_H) {
            int h = Math.min(PAPER_TEX_H, paperH - dy);
            Gui.drawModalRectWithCustomSizedTexture(x, y + dy, 0, 0, PAPER_W, h, 256, 256);
        }
    }

    private void drawRoll(int x, int y) {
        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, PAPER_TEX_H, ROLL_W, ROLL_H, 256, 256);
    }

    /** @return the item under the mouse, for its tooltip */
    private ItemStack drawContent(int x, int y, int mouseX, int mouseY, boolean interactive) {
        int w = PAPER_W - PAD_X * 2;
        ItemStack hovered = ItemStack.EMPTY;
        int ly = y + 10;

        centered(title, x, w, ly, INK);
        ly += 12;
        String sub = note.full ? "Has no more slots to sell"
                : "Wants for slot " + note.slot;
        if (note.open > 0) {
            sub += " - " + note.open + " free";
        }
        centered(sub, x, w, ly, INK_SOFT);
        ly += 12;

        // What just happened - a delivery, an unlock, a refusal - right under the name.
        if (!bannerLines.isEmpty()) {
            int color = bannerColor(note.bannerTone);
            for (String b : bannerLines) {
                centered(b, x, w, ly, color);
                ly += 10;
            }
            ly += 4;
        }

        // A hand-drawn rule: slightly uneven, like ink.
        for (int i = 0; i < w - 16; i += 6) {
            int jitter = ((i * 7) % 5 == 0) ? 1 : 0;
            drawRect(x + 8 + i, ly + jitter, x + 8 + Math.min(i + 6, w - 16), ly + jitter + 1,
                    0x993B2A1C);
        }
        ly += 8;

        if (note.full || note.wanted.isEmpty()) {
            centered(note.full ? "Teach it with an enchanted book" : "It wants nothing more",
                    x, w, ly + 2, INK);
            ly += 14;
        } else {
            for (BountyItem line : note.wanted) {
                ItemStack h = drawRow(line, x, ly, w, mouseX, mouseY, interactive);
                if (!h.isEmpty()) {
                    hovered = h;
                }
                ly += ROW_H;
            }
        }

        if (note.chance) {
            ly += 8;
            centered("Success " + BountyNote.percent(note.total) + " of "
                    + BountyNote.percent(note.max) + " max", x, w, ly, INK);
            ly += 10;
            for (BountyNote.Bonus b : note.bonuses) {
                String s = b.name + " +" + BountyNote.percent(b.bonus) + " = "
                        + BountyNote.percent(b.total);
                centered(fontRenderer.trimStringToWidth(s, w), x, w, ly, INK_SOFT);
                ly += 10;
            }
        }

        if (!note.ward.isEmpty()) {
            ly += 6;
            centered("Warded: " + note.ward, x, w, ly, INK_SOFT);
            ly += 10;
        }

        String[] hints = note.full
                ? new String[] {"Sneak-click with a book to teach", "Teach with a book"}
                : new String[] {"Sneak-click with items to deliver", "Sneak-click to deliver"};
        String hint = hints[hints.length - 1];
        for (String h : hints) {
            if (fontRenderer.getStringWidth(h) <= w) {
                hint = h;
                break;
            }
        }
        centered(hint, x, w, y + paperH - 18, INK_SOFT);
        return hovered;
    }

    private ItemStack drawRow(BountyItem line, int x, int y, int w, int mouseX, int mouseY,
                              boolean interactive) {
        int remaining = line.getRemaining();
        int have = SlotRequests.countDeliverable(mc.player, line);
        boolean done = remaining <= 0;
        boolean enough = !done && have >= remaining;
        if (enough) {
            drawRect(x - 3, y, x + w + 3, y + ROW_H - 1, GREEN_BAND);
        }

        ItemStack hovered = ItemStack.EMPTY;
        ItemStack icon = iconFor(line);
        if (!icon.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(icon, x, y + 1);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            if (interactive && mouseX >= x && mouseX < x + 16 && mouseY >= y + 1
                    && mouseY < y + 17) {
                hovered = icon;
            }
        }

        int text = done ? DONE_TEXT : enough ? GREEN_TEXT : INK;
        String progress = done ? "done" : line.getDelivered() + "/" + line.getRequired();
        int progressX = x + w - fontRenderer.getStringWidth(progress);
        fontRenderer.drawString(progress, progressX, y + 5, text);

        int nameRight = progressX - 6;
        if (!done) {
            String carrying = "have " + have;
            int cw = fontRenderer.getStringWidth(carrying);
            int carryX = progressX - 8 - cw;
            fontRenderer.drawString(carrying, carryX, y + 5,
                    enough ? GREEN_TEXT : have > 0 ? INK : INK_SOFT);
            nameRight = carryX - 6;
        }

        String name = line.displayName();
        int max = nameRight - (x + 20);
        if (fontRenderer.getStringWidth(name) > max) {
            name = fontRenderer.trimStringToWidth(name, Math.max(0, max - 6)) + "...";
        }
        fontRenderer.drawString(name, x + 20, y + 5, text);
        if (done) {
            drawRect(x + 20, y + 9, x + 20 + fontRenderer.getStringWidth(name), y + 10,
                    DONE_TEXT);
        }
        return hovered;
    }

    private void centered(String s, int x, int w, int y, int color) {
        fontRenderer.drawString(s, x + (w - fontRenderer.getStringWidth(s)) / 2, y, color);
    }

    private static ItemStack iconFor(BountyItem line) {
        Item item = ItemPattern.representative(line.getItemName());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, line.getMeta() < 0 ? 0 : line.getMeta());
    }

    /** The scissor box is in real pixels from the bottom-left; GUI coordinates are scaled. */
    private void scissor(int x, int y, int w, int h) {
        ScaledResolution res = new ScaledResolution(mc);
        int f = res.getScaleFactor();
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, Math.max(0, w * f),
                Math.max(0, h * f));
    }

    // ----------------------------------------------------------------- input

    private void close() {
        if (closingAt == 0L) {
            // Closing from part-way open starts from where it is, not from fully open.
            float p = openness();
            closingAt = Minecraft.getSystemTime() - (long) ((1.0F - p) * CLOSE_MS);
            play(ModContent.SCROLL_CLOSE, 0.95F + mc.world.rand.nextFloat() * 0.1F);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        close();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            close();
        }
    }
}
