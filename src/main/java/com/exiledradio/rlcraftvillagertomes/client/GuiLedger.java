package com.exiledradio.rlcraftvillagertomes.client;

import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import com.exiledradio.rlcraftvillagertomes.bounty.ItemPattern;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.ledger.AtlasCompat;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerActions;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerNetwork;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerView;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import com.exiledradio.rlcraftvillagertomes.quest.QuestEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import com.exiledradio.rlcraftvillagertomes.ward.WardMode;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The ledger screen - the same screen whether the ledger is in your hand or on a lectern.
 *
 * <p>One card per villager: who and where on the first line, then one row per item it wants.
 * Each row shows the item's icon, its name, how many you are carrying, and how many the
 * villager has had out of how many it wants. A row you are carrying enough to finish is
 * banded green, so a glance down the page says what you can pay off right now. Finished
 * rows go quiet and read "done".
 *
 * <p>On a lectern each card also has a Deliver button, live only while that villager is
 * loaded, which hands over everything you carry that it still needs.
 *
 * <p>Double-clicking a villager's name edits it in place: Enter names the villager (the same
 * as a name tag would), Escape or a click elsewhere leaves it alone. An empty name takes a
 * name tag off again. Only while the villager is loaded, since the name lives on the villager.
 *
 * <p>Drawn entirely with flat rectangles rather than a texture, in the lectern's dark spruce
 * around a parchment page, so it scales to any GUI size without a stretched bitmap.
 */
public class GuiLedger extends GuiScreen implements NoticeTarget {

    // Palette: dark spruce frame around a warm parchment page.
    private static final int FRAME_OUTER = 0xFF1E140C;
    private static final int FRAME = 0xFF4A3424;
    private static final int FRAME_LIGHT = 0xFF6B4D33;
    private static final int TITLE_TEXT = 0xFFF3E6C8;
    private static final int PAPER = 0xFFEFE4C8;
    // Cards are washes over the parchment rather than boxes on it, so the page shows through.
    private static final int CARD = 0x38FFF8E6;
    private static final int CARD_EDGE = 0x50806040;
    private static final int INK = 0xFF33241A;
    private static final int INK_SOFT = 0xFF7D6A54;
    private static final int GREEN_TEXT = 0xFF1C6E2E;
    private static final int GREEN_BAND = 0x6688C47A;
    private static final int GREEN_EDGE = 0x806DAF6B;
    private static final int DONE_TEXT = 0xFFA6977E;
    private static final int BUTTON = 0xFF3F7A3A;
    private static final int BUTTON_HOVER = 0xFF4E9448;
    private static final int BUTTON_OFF = 0xFFB9AD93;
    private static final int REMOVE = 0xFF9A3B2E;
    private static final int REMOVE_HOVER = 0xFFB84A3A;
    private static final int NAV = 0xFF5E4430;
    private static final int NAV_HOVER = 0xFF7A5A3E;

    private static final net.minecraft.util.ResourceLocation BOOK_TEXTURE =
            new net.minecraft.util.ResourceLocation(
                    com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes.MODID,
                    "textures/gui/ledger_book.png");
    private static final int COVER_W = 344;
    private static final int COVER_H = 264;
    private static final int PAGE_V = 264;
    private static final int PAGE_W = 330;
    private static final int PAGE_H = 222;
    /** How far in from the right edge the ribbon hangs; the footer text stops short of it. */
    private static final int RIBBON_INSET = 30;
    private static final int RIBBON_W = 10;

    /** Where a card's name starts - after the favorite star. */
    private static final int NAME_X = 18;
    private static ItemStack wardIcon;

    private static final int HEADER_H = 16;
    private static final int ROW_H = 18;
    private static final int CARD_PAD = 4;
    private static final int CARD_GAP = 6;
    private static final int TITLE_H = 18;
    private static final int FOOTER_H = 16;

    /** Not sent anywhere - the page buttons are handled here on the client. */
    private static final byte NAV_PREV = -1;
    private static final byte NAV_NEXT = -2;
    /** A click on a villager's name; two in quick succession start editing it. */
    private static final byte NAME = -3;
    /** The name tag button after a name: one click starts editing. */
    private static final byte RENAME = -4;
    private static final ItemStack NAME_TAG = new ItemStack(net.minecraft.init.Items.NAME_TAG);

    /** How close together two clicks on a name must be to count as a double click. */
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final int EDIT_BOX = 0xFFFFFBEE;
    private static final int EDIT_EDGE = 0xFF8C7660;
    private static final int EDIT_SELECTION = 0xFFB9D3EE;

    private UUID lastNameClick;
    private long lastNameClickAt;

    /** The name being edited, and whose it is - null when nothing is being edited. */
    private GuiTextField nameField;
    private UUID editing;

    private LedgerView view;
    private boolean openOnBookmark = true;
    /** A {@code Feedback} line shown in the footer for a few seconds, in place of the hint. */
    private String notice;
    private byte noticeTone;
    private long noticeAt;
    private static final long NOTICE_MS = 5000L;
    private int page;
    /** Each page as [first entry, last entry exclusive], packed to fit the page height. */
    private final List<int[]> pages = new ArrayList<int[]>();

    // Layout, recomputed in initGui.
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    /** Clickable regions laid out by the last draw. */
    private final List<Hit> hits = new ArrayList<Hit>();
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private String hoveredHint;
    /** A multi-line hover, used by the coordinates. */
    private List<String> hoveredLines;

    private static final class Hit {
        final int x;
        final int y;
        final int w;
        final int h;
        final byte action;
        final UUID villager;

        Hit(int x, int y, int w, int h, byte action, UUID villager) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
            this.villager = villager;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public GuiLedger(LedgerView view) {
        this.view = view;
    }

    public boolean sameLedger(LedgerView other) {
        return other.source == view.source
                && (view.isLectern() ? other.pos.equals(view.pos) : other.hand == view.hand);
    }

    public void setView(LedgerView view) {
        this.view = view;
        paginate();
        if (editing != null && indexOf(editing) < 0) {
            stopEditing();
        }
    }

    private int indexOf(UUID villager) {
        for (int i = 0; i < view.entries.size(); i++) {
            if (view.entries.get(i).getVillager().equals(villager)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        panelW = Math.min(340, width - 16);
        panelH = Math.min(260, height - 16);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        listX = left + 10;
        listY = top + TITLE_H + 6;
        listW = panelW - 22;
        listH = panelH - TITLE_H - FOOTER_H - 12;
        paginate();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ------------------------------------------------------------------ name

    private void startEditing(QuestEntry entry) {
        editing = entry.getVillager();
        nameField = new GuiTextField(0, fontRenderer, 0, 0, 120, 12);
        nameField.setEnableBackgroundDrawing(false);
        nameField.setMaxStringLength(LedgerActions.MAX_NAME_LENGTH);
        nameField.setTextColor(INK);
        nameField.setText(entry.getNameComponent().getUnformattedText());
        nameField.setCursorPositionEnd();
        nameField.setSelectionPos(0);
        nameField.setFocused(true);
    }

    /**
     * Draws the name being edited. Not {@code GuiTextField.drawTextBox}: that always draws
     * with a drop shadow, which turns dark ink on parchment into a smeared bold blur. The
     * field still does all the editing; only the drawing is ours - flat ink like the rest of
     * the page, a light selection band, and a thin blinking caret.
     */
    private void drawNameField() {
        String text = nameField.getText();
        int cur = Math.min(nameField.getCursorPosition(), text.length());
        int sel = Math.min(nameField.getSelectionEnd(), text.length());
        int x = nameField.x;
        int y = nameField.y;
        int room = nameField.width - 2;

        // Scroll so the caret stays in view when the name is wider than the box.
        int start = 0;
        while (start < cur && fontRenderer.getStringWidth(text.substring(start, cur)) > room) {
            start++;
        }
        String shown = fontRenderer.trimStringToWidth(text.substring(start), room);
        int end = start + shown.length();

        int a = Math.max(start, Math.min(Math.min(cur, sel), end));
        int b = Math.max(start, Math.min(Math.max(cur, sel), end));
        if (a < b) {
            drawRect(x + fontRenderer.getStringWidth(text.substring(start, a)), y - 1,
                    x + fontRenderer.getStringWidth(text.substring(start, b)), y + 9,
                    EDIT_SELECTION);
        }
        fontRenderer.drawString(shown, x, y, INK);
        if ((Minecraft.getSystemTime() / 500L) % 2L == 0L) {
            int cx = x + fontRenderer.getStringWidth(text.substring(start, Math.max(start, cur)));
            drawRect(cx, y - 1, cx + 1, y + 9, INK);
        }
    }

    /** The coordinates hover: where, how far and which way from you, and the atlas click. */
    private List<String> whereHint(QuestEntry entry) {
        List<String> lines = new ArrayList<String>();
        lines.add("X " + entry.getX() + "   Y " + entry.getY() + "   Z " + entry.getZ());
        if (mc.player != null && mc.player.dimension == entry.getDimension()) {
            double dx = entry.getX() + 0.5D - mc.player.posX;
            double dz = entry.getZ() + 0.5D - mc.player.posZ;
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            lines.add(TextFormatting.GRAY + (dist < 8 ? "Right here"
                    : dist + " blocks " + direction(dx, dz)));
        } else {
            lines.add(TextFormatting.GRAY + "In dimension " + entry.getDimension());
        }
        if (AtlasCompat.isLoaded()) {
            lines.add(TextFormatting.YELLOW + "Click to mark it on your Antique Atlas");
        }
        return lines;
    }

    /** Compass direction of an offset - north is -Z in Minecraft. */
    private static String direction(double dx, double dz) {
        String[] names = {"south", "south-west", "west", "north-west",
                "north", "north-east", "east", "south-east"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int i = (int) Math.round(((angle % 360.0D) + 360.0D) % 360.0D / 45.0D) % 8;
        return names[i];
    }

    private void stopEditing() {
        editing = null;
        nameField = null;
    }

    private void commitName() {
        if (editing != null) {
            NBTTagCompound where = where();
            where.setString("name", nameField.getText());
            LedgerNetwork.CHANNEL.sendToServer(
                    new LedgerNetwork.ActionMessage(where, LedgerNetwork.ACTION_RENAME, editing));
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        stopEditing();
    }

    private NBTTagCompound where() {
        NBTTagCompound where = new NBTTagCompound();
        where.setByte("source", view.source);
        where.setByte("hand", (byte) view.hand.ordinal());
        where.setLong("pos", view.pos.toLong());
        return where;
    }

    // ------------------------------------------------------------------ draw

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        hits.clear();
        hoveredStack = ItemStack.EMPTY;
        hoveredHint = null;
        hoveredLines = null;

        drawFrame(mouseX, mouseY);
        drawTitle(mouseX, mouseY);

        scissor(listX, listY, listW, listH);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        if (view.entries.isEmpty()) {
            drawEmpty();
        } else {
            int[] range = pages.get(page);
            int y = listY;
            for (int i = range[0]; i < range[1]; i++) {
                y = drawCard(view.entries.get(i), i < view.loaded.length && view.loaded[i],
                        y, mouseX, mouseY) + CARD_GAP;
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (nameField != null) {
            drawNameField();
        }

        drawFooter();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (!hoveredStack.isEmpty()) {
            renderToolTip(hoveredStack, mouseX, mouseY);
        } else if (hoveredLines != null) {
            drawHoveringText(hoveredLines, mouseX, mouseY);
        } else if (hoveredHint != null) {
            drawHoveringText(hoveredHint, mouseX, mouseY);
        }
    }

    /**
     * The book: a leather cover with stitching and brass corners, a parchment page with the
     * edges of the pages beneath peeking out, and a ribbon bookmark hanging out the bottom.
     * The cover and page come from {@code ledger_book.png} (tools/gen-ledger-gui.py), made in
     * the same hand as the bounty scroll. Drawn scaled when the screen is too small for the
     * full 340 x 260 panel.
     */
    private void drawFrame(int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BOOK_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        drawScaledCustomSizeModalRect(left - 2, top - 2, 0, 0, COVER_W, COVER_H,
                panelW + 4, panelH + 4, 512, 512);

        // Inset far enough that the page edges underneath stop short of the stitching.
        int px = left + 7;
        int py = top + TITLE_H + 2;
        int pw = panelW - 18;
        int ph = panelH - TITLE_H - FOOTER_H - 4;
        // Page edges underneath, offset down and right.
        drawRect(px + 3, py + 3, px + pw + 3, py + ph + 3, 0xFFB9A47A);
        drawRect(px + 2, py + 2, px + pw + 2, py + ph + 2, 0xFFD3BF95);
        drawRect(px + 1, py + 1, px + pw + 1, py + ph + 1, 0xFFE4D2AC);

        mc.getTextureManager().bindTexture(BOOK_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawScaledCustomSizeModalRect(px, py, 0, PAGE_V, PAGE_W, PAGE_H, pw, ph, 512, 512);

        drawRibbon(py + ph, mouseX, mouseY);
    }

    /**
     * The ribbon bookmark, and the button that sets it. Tucked under the page and hanging
     * out over the cover; on the bookmarked page it folds over the page edge and turns a
     * brighter red, the way a real ribbon marks where you stopped.
     *
     * <p>The bookmark remembers a villager rather than a page number: pages are packed to
     * fit the screen, so page 3 on one GUI scale is not page 3 on another. The ledger opens
     * on whichever page that villager is on.
     */
    private void drawRibbon(int pageBottom, int mouseX, int mouseY) {
        int rx = left + panelW - RIBBON_INSET;
        int rw = RIBBON_W;
        boolean here = bookmarkOnThisPage();
        // Only a short fold over the page edge: any further and it would sit under the
        // bottom card's buttons and steal their clicks.
        int topY = here ? pageBottom - 3 : pageBottom;
        int tail = pageBottom + FOOTER_H + 6;
        boolean over = mouseX >= rx && mouseX < rx + rw && mouseY >= topY && mouseY < tail + 3;
        int body = over ? 0xFFB83C3C : here ? 0xFFC23A3A : 0xFF7A2424;
        drawRect(rx, topY, rx + rw, tail, body);
        drawRect(rx, topY, rx + 1, tail, 0xFFC25050);
        drawRect(rx + rw - 1, topY, rx + rw, tail, 0xFF5E1A1A);
        if (here) {
            // Where it folds over the page edge.
            drawRect(rx, pageBottom - 1, rx + rw, pageBottom, 0xFF5E1A1A);
        }
        // Notched tail.
        drawRect(rx, tail, rx + 3, tail + 3, body);
        drawRect(rx + rw - 3, tail, rx + rw, tail + 3, body);

        UUID first = firstOnPage();
        if (first != null) {
            addHit(rx, topY, rw, tail + 3 - topY, LedgerNetwork.ACTION_BOOKMARK, first);
        }
        if (over) {
            hoveredLines = new ArrayList<String>();
            if (here) {
                hoveredLines.add("Bookmarked page");
                hoveredLines.add(TextFormatting.GRAY + "Click to remove the bookmark");
            } else {
                hoveredLines.add("Bookmark this page");
                hoveredLines.add(TextFormatting.GRAY + "The ledger opens here every time");
            }
        }
    }

    private UUID firstOnPage() {
        if (view.entries.isEmpty() || pages.isEmpty()) {
            return null;
        }
        return view.entries.get(pages.get(page)[0]).getVillager();
    }

    private boolean bookmarkOnThisPage() {
        if (view.bookmark.isEmpty() || pages.isEmpty()) {
            return false;
        }
        int[] range = pages.get(page);
        for (int i = range[0]; i < range[1]; i++) {
            if (view.entries.get(i).getVillager().toString().equals(view.bookmark)) {
                return true;
            }
        }
        return false;
    }

    private void drawTitle(int mouseX, int mouseY) {
        String title = view.isLectern() ? "Ledger Lectern" : "Villager Ledger";
        fontRenderer.drawString(title, left + 10, top + 7, TITLE_TEXT);
        String count = "  " + view.entries.size()
                + (view.capacity > 0 ? " / " + view.capacity : "")
                + (view.entries.size() == 1 ? " villager" : " villagers");
        int countX = left + 10 + fontRenderer.getStringWidth(title);
        fontRenderer.drawString(count, countX, top + 7, 0xFFB8A487);
        drawInfo(countX + fontRenderer.getStringWidth(count) + 6, top + 5, mouseX, mouseY);

        if (pages.size() <= 1) {
            return;
        }
        int right = left + panelW - 9;
        int nextX = right - 12;
        String label = "Page " + (page + 1) + " / " + pages.size();
        int labelX = nextX - 6 - fontRenderer.getStringWidth(label);
        int prevX = labelX - 6 - 12;
        navButton(prevX, top + 5, "<", page > 0, NAV_PREV, mouseX, mouseY);
        fontRenderer.drawString(label, labelX, top + 7, TITLE_TEXT);
        navButton(nextX, top + 5, ">", page < pages.size() - 1, NAV_NEXT, mouseX, mouseY);
    }

    /** A round blue "i" on the cover; hover for how to get the most out of the ledger. */
    private void drawInfo(int x, int y, int mouseX, int mouseY) {
        int s = 11;
        boolean over = mouseX >= x && mouseX < x + s && mouseY >= y && mouseY < y + s;
        int fill = over ? 0xFF5B8FD0 : 0xFF3E6FA8;
        // A rounded square, near enough to round at this size.
        drawRect(x + 2, y, x + s - 2, y + s, fill);
        drawRect(x, y + 2, x + s, y + s - 2, fill);
        drawRect(x + 1, y + 1, x + s - 1, y + s - 1, fill);
        drawRect(x + 5, y + 2, x + 6, y + 3, 0xFFFFFFFF);
        drawRect(x + 5, y + 4, x + 6, y + 9, 0xFFFFFFFF);
        drawRect(x + 4, y + 4, x + 5, y + 5, 0xFFFFFFFF);
        drawRect(x + 4, y + 8, x + 7, y + 9, 0xFFFFFFFF);
        if (over) {
            hoveredLines = new ArrayList<String>();
            if (view.isLectern()) {
                hoveredLines.add("Quick deposit");
                hoveredLines.add(TextFormatting.GRAY + "Deliver hands each loaded villager");
                hoveredLines.add(TextFormatting.GRAY + "everything you carry that it needs.");
                hoveredLines.add(TextFormatting.GRAY + "Enchanted or named items stay with you.");
            } else {
                hoveredLines.add("Place in a Ledger Lectern for quick deposit");
                hoveredLines.add(TextFormatting.GRAY + "A Deliver button on every villager hands");
                hoveredLines.add(TextFormatting.GRAY + "over what you carry, without walking there.");
            }
            hoveredLines.add("");
            hoveredLines.add(TextFormatting.GRAY + "Star a villager to keep it at the front.");
            hoveredLines.add(TextFormatting.GRAY + "Click the ribbon to bookmark a page.");
            hoveredLines.add(TextFormatting.GRAY + "Craft with a book to make a copy.");
        }
    }

    private void navButton(int x, int y, String arrow, boolean enabled, byte action,
                           int mouseX, int mouseY) {
        boolean over = enabled && mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + 12;
        drawRect(x, y, x + 12, y + 12, over ? NAV_HOVER : NAV);
        fontRenderer.drawString(arrow, x + 4, y + 2, enabled ? TITLE_TEXT : 0xFF8C7660);
        if (enabled) {
            addHit(x, y, 12, 12, action, null);
        }
    }

    @Override
    public void showNotice(byte tone, String text) {
        notice = text;
        noticeTone = tone;
        noticeAt = Minecraft.getSystemTime();
    }

    private void drawFooter() {
        if (notice != null && Minecraft.getSystemTime() - noticeAt < NOTICE_MS) {
            int color = noticeTone == com.exiledradio.rlcraftvillagertomes.Feedback.GOOD
                    ? 0xFF9BE08E
                    : noticeTone == com.exiledradio.rlcraftvillagertomes.Feedback.WARN
                    ? 0xFFFFD27A
                    : noticeTone == com.exiledradio.rlcraftvillagertomes.Feedback.BAD
                    ? 0xFFFF9A8A : TITLE_TEXT;
            String shown = notice;
            int room = panelW - RIBBON_INSET - 12;
            if (fontRenderer.getStringWidth(shown) > room) {
                shown = fontRenderer.trimStringToWidth(shown, room - 8) + "...";
            }
            fontRenderer.drawString(shown, left + 10, top + panelH - FOOTER_H + 3, color);
            return;
        }
        // Longest first; the first that fits the panel is used, so a hint is never cut off
        // mid-sentence on a small screen or a large GUI scale.
        String[] hints = view.isLectern()
                ? new String[] {
                        "Deliver hands over what you carry. Enchanted or named items stay.",
                        "Enchanted or named items are never delivered.",
                        "Named items stay with you."}
                : new String[] {
                        "Sneak-click a villager to log it. Deliver from a Ledger Lectern.",
                        "Sneak-click a villager to log it.",
                        "Sneak-click to log."};
        int y = top + panelH - FOOTER_H + 3;
        int room = panelW - RIBBON_INSET - 12;
        String hint = hints[hints.length - 1];
        for (String candidate : hints) {
            if (fontRenderer.getStringWidth(candidate) <= room) {
                hint = candidate;
                break;
            }
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(hint, room), left + 10, y,
                TITLE_TEXT & 0xCCFFFFFF);
    }

    private void drawEmpty() {
        String a = "Nothing written down yet.";
        String b = "Sneak-click a villager with the ledger to add it.";
        int cy = listY + listH / 2 - 10;
        fontRenderer.drawString(a, listX + (listW - fontRenderer.getStringWidth(a)) / 2, cy, INK);
        fontRenderer.drawString(b, listX + (listW - fontRenderer.getStringWidth(b)) / 2,
                cy + 12, INK_SOFT);
    }

    /** @return the y just below the card */
    private int drawCard(QuestEntry entry, boolean loaded, int y, int mouseX, int mouseY) {
        int h = cardHeight(entry);
        int x = listX;
        int w = listW;
        if (y + h < listY || y > listY + listH) {
            return y + h;
        }

        drawRect(x, y, x + w, y + h, CARD_EDGE);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, CARD);

        // Header: name, slot, coordinates, then the buttons on the right.
        int hy = y + CARD_PAD;
        String name = entry.getNameComponent().getFormattedText();
        int nameW = fontRenderer.getStringWidth(name);
        drawFavorite(entry, x + 5, hy + 3, mouseX, mouseY);
        int nx = x + NAME_X;
        int cursor;
        if (entry.getVillager().equals(editing)) {
            int fieldW = Math.max(nameW, fontRenderer.getStringWidth("MMMMMMMMMMMM")) + 8;
            drawRect(nx - 3, hy + 1, nx - 3 + fieldW, hy + 14, EDIT_EDGE);
            drawRect(nx - 2, hy + 2, nx - 4 + fieldW, hy + 13, EDIT_BOX);
            nameField.x = nx;
            nameField.y = hy + 4;
            nameField.width = fieldW - 6;
            cursor = nx - 3 + fieldW + 6;
        } else {
            boolean overName = inList(mouseX, mouseY) && mouseX >= nx - 2
                    && mouseX < nx + 2 + nameW && mouseY >= hy + 2 && mouseY < hy + 14;
            fontRenderer.drawString(name, nx, hy + 4, INK);
            if (overName) {
                drawRect(nx, hy + 13, nx + nameW, hy + 14, loaded ? INK : INK_SOFT);
                hoveredHint = loaded ? "Double-click to rename"
                        : "Not loaded - go back to its village to rename it";
            }
            cursor = nx + nameW + 6;
            if (loaded) {
                addHit(nx - 2, hy + 2, nameW + 4, 12, NAME, entry.getVillager());
                // A small name tag right after the name, so renaming can be found without
                // knowing about the double click.
                int tx = nx + nameW + 3;
                boolean overTag = inList(mouseX, mouseY) && mouseX >= tx - 1 && mouseX < tx + 13
                        && mouseY >= hy + 1 && mouseY < hy + 15;
                if (overTag) {
                    drawRect(tx - 1, hy + 1, tx + 13, hy + 15, CARD_EDGE);
                }
                GlStateManager.pushMatrix();
                GlStateManager.translate(tx, hy + 2, 0.0F);
                GlStateManager.scale(0.75F, 0.75F, 1.0F);
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(NAME_TAG, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.popMatrix();
                addHit(tx - 1, hy + 1, 14, 14, RENAME, entry.getVillager());
                if (overTag) {
                    hoveredHint = "Rename";
                }
                cursor = tx + 12 + 6;
            }
        }
        String slot = "Slot " + entry.getSlot();
        fontRenderer.drawString(slot, cursor, hy + 4, INK_SOFT);
        cursor += fontRenderer.getStringWidth(slot) + 6;
        if (entry.getWard() > 0) {
            drawWard(entry, loaded, cursor, hy + 1, mouseX, mouseY);
            cursor += 12 + 6;
        }

        int right = x + w - 4;
        int removeW = 12;
        int removeX = right - removeW;
        boolean overRemove = inList(mouseX, mouseY)
                && mouseX >= removeX && mouseX < removeX + removeW
                && mouseY >= hy + 1 && mouseY < hy + 13;
        drawRect(removeX, hy + 1, removeX + removeW, hy + 13, overRemove ? REMOVE_HOVER : REMOVE);
        fontRenderer.drawString("x", removeX + 4, hy + 3, 0xFFFFFFFF);
        addHit(removeX, hy + 1, removeW, 12, LedgerNetwork.ACTION_REMOVE, entry.getVillager());
        if (overRemove) {
            hoveredHint = "Remove from the ledger";
        }
        right = removeX - 4;

        if (view.isLectern()) {
            String label = "Deliver";
            int bw = fontRenderer.getStringWidth(label) + 10;
            int bx = right - bw;
            boolean canDeliver = loaded && hasAnythingFor(entry);
            boolean over = inList(mouseX, mouseY)
                    && mouseX >= bx && mouseX < bx + bw && mouseY >= hy + 1 && mouseY < hy + 13;
            drawRect(bx, hy + 1, bx + bw, hy + 13,
                    !canDeliver ? BUTTON_OFF : over ? BUTTON_HOVER : BUTTON);
            fontRenderer.drawString(label, bx + 5, hy + 3, canDeliver ? 0xFFFFFFFF : 0xFFF2EAD8);
            if (canDeliver) {
                addHit(bx, hy + 1, bw, 12, LedgerNetwork.ACTION_DELIVER, entry.getVillager());
            }
            if (over) {
                hoveredHint = !loaded ? "Not loaded - go back to its village"
                        : !canDeliver ? "You are not carrying anything it needs"
                        : "Hand over everything you carry that it still needs";
            }
            right = bx - 6;
        }

        String where = entry.getX() + ", " + entry.getY() + ", " + entry.getZ()
                + (entry.getDimension() != 0 ? "  dim " + entry.getDimension() : "");
        if (!loaded) {
            where = "not loaded  " + where;
        }
        int whereW = fontRenderer.getStringWidth(where);
        if (right - whereW >= cursor) {
            int wx = right - whereW;
            boolean overWhere = inList(mouseX, mouseY) && mouseX >= wx - 1
                    && mouseX < right + 1 && mouseY >= hy + 2 && mouseY < hy + 14;
            fontRenderer.drawString(where, wx, hy + 4, overWhere ? INK : INK_SOFT);
            if (overWhere) {
                drawRect(wx, hy + 13, right, hy + 14, INK);
                hoveredLines = whereHint(entry);
            }
            if (AtlasCompat.isLoaded()) {
                addHit(wx - 1, hy + 2, whereW + 2, 12, LedgerNetwork.ACTION_MARK,
                        entry.getVillager());
            }
        }

        // Item rows.
        int ry = y + CARD_PAD + HEADER_H;
        for (BountyItem line : entry.getWanted()) {
            drawRow(line, x + 3, ry, w - 6, mouseX, mouseY);
            ry += ROW_H;
        }
        return y + h;
    }

    private void drawRow(BountyItem line, int x, int y, int w, int mouseX, int mouseY) {
        int remaining = line.getRemaining();
        int have = SlotRequests.countDeliverable(mc.player, line);
        boolean done = remaining <= 0;
        boolean enough = !done && have >= remaining;

        if (enough) {
            drawRect(x, y, x + w, y + ROW_H - 1, GREEN_EDGE);
            drawRect(x + 1, y + 1, x + w - 1, y + ROW_H - 2, GREEN_BAND);
        }

        ItemStack icon = iconFor(line);
        if (!icon.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(icon, x + 3, y + 1);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableLighting();
            if (inList(mouseX, mouseY) && mouseX >= x + 3 && mouseX < x + 19
                    && mouseY >= y + 1 && mouseY < y + 17) {
                hoveredStack = icon;
            }
        }

        int text = done ? DONE_TEXT : enough ? GREEN_TEXT : INK;
        String progress = done ? "done" : line.getDelivered() + " / " + line.getRequired();
        int progressX = x + w - 6 - fontRenderer.getStringWidth(progress);
        fontRenderer.drawString(progress, progressX, y + 5, text);

        String carrying = done ? "" : "have " + have;
        int carryingX = x + w - 6 - 52 - fontRenderer.getStringWidth(carrying);
        if (!done) {
            fontRenderer.drawString(carrying, carryingX, y + 5,
                    enough ? GREEN_TEXT : have > 0 ? INK : INK_SOFT);
        }

        String name = line.displayName();
        int nameMax = carryingX - (x + 24) - 6;
        if (fontRenderer.getStringWidth(name) > nameMax) {
            name = fontRenderer.trimStringToWidth(name, nameMax - 6) + "...";
        }
        fontRenderer.drawString(name, x + 24, y + 5, text);
        if (done) {
            int nw = fontRenderer.getStringWidth(name);
            drawRect(x + 24, y + 9, x + 24 + nw, y + 10, DONE_TEXT);
        }
    }

    // ------------------------------------------------------- star and ward

    /** A 9 x 9 pixel star. */
    private static final String[] STAR = {
            "....#....",
            "...###...",
            "...###...",
            "#########",
            ".#######.",
            "..#####..",
            "..##.##..",
            ".##...##.",
            "##.....##"};

    private void drawFavorite(QuestEntry entry, int x, int y, int mouseX, int mouseY) {
        boolean fav = entry.getFavorite() > 0;
        boolean over = inList(mouseX, mouseY) && mouseX >= x - 1 && mouseX < x + 10
                && mouseY >= y - 1 && mouseY < y + 10;
        int color = fav ? (over ? 0xFFF0C040 : 0xFFD9A520)
                : (over ? INK_SOFT : 0x557D6A54);
        for (int r = 0; r < STAR.length; r++) {
            for (int c = 0; c < STAR[r].length(); c++) {
                if (STAR[r].charAt(c) == '#') {
                    drawRect(x + c, y + r, x + c + 1, y + r + 1, color);
                }
            }
        }
        addHit(x - 1, y - 1, 11, 11, LedgerNetwork.ACTION_FAVORITE, entry.getVillager());
        if (over) {
            hoveredHint = fav ? "Unfavorite" : "Favorite - keep it at the front";
        }
    }

    /**
     * The villager's ward, framed in its mode's color like the button on its trade screen.
     * Click to switch modes while it is loaded.
     */
    private void drawWard(QuestEntry entry, boolean loaded, int x, int y, int mouseX,
                          int mouseY) {
        WardMode mode = WardMode.of(entry.getWard());
        boolean over = inList(mouseX, mouseY) && mouseX >= x && mouseX < x + 12
                && mouseY >= y && mouseY < y + 12;
        drawRect(x, y, x + 12, y + 12, wardColor(mode));
        drawRect(x + 1, y + 1, x + 11, y + 11, over && loaded ? 0xFF4A3424 : 0xFF33241A);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 1, y + 1, 0.0F);
        GlStateManager.scale(0.625F, 0.625F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        if (wardIcon == null) {
            wardIcon = new ItemStack(ModContent.VILLAGER_WARD);
        }
        itemRender.renderItemAndEffectIntoGUI(wardIcon, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        if (loaded) {
            addHit(x, y, 12, 12, LedgerNetwork.ACTION_WARD, entry.getVillager());
        }
        if (over) {
            hoveredLines = new ArrayList<String>();
            hoveredLines.add("Ward: " + mode.getLabel());
            hoveredLines.add(TextFormatting.GRAY + mode.getDescription());
            hoveredLines.add(loaded ? TextFormatting.YELLOW + "Click to change"
                    : TextFormatting.GRAY + "Not loaded - go back to change it");
        }
    }

    private static int wardColor(WardMode mode) {
        switch (mode) {
            case ALL:
                return 0xFFE0B84A;
            case PLAYERS:
                return 0xFF4A90D9;
            case PLAYERS_AND_ZOMBIES:
                return 0xFF4CAF50;
            default:
                return 0xFF7D6A54;
        }
    }

    // ---------------------------------------------------------------- layout

    private int cardHeight(QuestEntry entry) {
        return CARD_PAD * 2 + HEADER_H + entry.getWanted().size() * ROW_H;
    }

    /**
     * Packs whole cards onto pages, as many as fit the page height. A card is never split
     * across two pages; a card taller than a whole page gets a page to itself.
     */
    private void paginate() {
        pages.clear();
        int start = 0;
        int used = 0;
        for (int i = 0; i < view.entries.size(); i++) {
            int h = cardHeight(view.entries.get(i));
            if (i > start && used + CARD_GAP + h > listH) {
                pages.add(new int[] {start, i});
                start = i;
                used = 0;
            }
            used += (i > start ? CARD_GAP : 0) + h;
        }
        pages.add(new int[] {start, view.entries.size()});
        page = Math.max(0, Math.min(page, pages.size() - 1));

        // First layout only: open on the bookmarked villager's page. Later refreshes (after
        // Deliver, a star, a rename) keep whatever page you have turned to.
        if (openOnBookmark && listH > 0) {
            openOnBookmark = false;
            for (int p = 0; p < pages.size() && !view.bookmark.isEmpty(); p++) {
                for (int i = pages.get(p)[0]; i < pages.get(p)[1]; i++) {
                    if (view.entries.get(i).getVillager().toString().equals(view.bookmark)) {
                        page = p;
                    }
                }
            }
        }
    }

    private void turnPage(int delta) {
        int target = Math.max(0, Math.min(page + delta, pages.size() - 1));
        if (target != page) {
            page = target;
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.getMasterRecord(ModContent.LEDGER_OPEN, 1.0F));
        }
    }

    private boolean inList(int mx, int my) {
        return mx >= listX && mx < listX + listW && my >= listY && my < listY + listH;
    }

    private void addHit(int x, int y, int w, int h, byte action, UUID villager) {
        hits.add(new Hit(x, y, w, h, action, villager));
    }

    /** The scissor box is in real pixels from the bottom-left; GUI coordinates are scaled. */
    private void scissor(int x, int y, int w, int h) {
        ScaledResolution res = new ScaledResolution(mc);
        int f = res.getScaleFactor();
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }

    private boolean hasAnythingFor(QuestEntry entry) {
        for (BountyItem line : entry.getWanted()) {
            if (!line.isSatisfied() && SlotRequests.countDeliverable(mc.player, line) > 0) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack iconFor(BountyItem line) {
        Item item = ItemPattern.representative(line.getItemName());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 1, line.getMeta() < 0 ? 0 : line.getMeta());
    }

    // ----------------------------------------------------------------- input

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && nameField == null) {
            turnPage(wheel > 0 ? -1 : 1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        if (nameField != null) {
            // A click inside the box moves the cursor; anywhere else leaves the name alone.
            if (mouseX >= nameField.x - 2 && mouseX < nameField.x + nameField.width
                    && mouseY >= nameField.y - 3 && mouseY < nameField.y + 10) {
                nameField.mouseClicked(mouseX, mouseY, button);
                return;
            }
            stopEditing();
        }
        for (Hit hit : hits) {
            if (!hit.contains(mouseX, mouseY)) {
                continue;
            }
            if (hit.action == RENAME) {
                int i = indexOf(hit.villager);
                if (i >= 0) {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                            SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    startEditing(view.entries.get(i));
                }
                return;
            }
            if (hit.action == NAME) {
                long now = Minecraft.getSystemTime();
                if (hit.villager.equals(lastNameClick) && now - lastNameClickAt < DOUBLE_CLICK_MS) {
                    int i = indexOf(hit.villager);
                    if (i >= 0) {
                        startEditing(view.entries.get(i));
                    }
                    lastNameClick = null;
                } else {
                    lastNameClick = hit.villager;
                    lastNameClickAt = now;
                }
                return;
            }
            if (hit.action == NAV_PREV || hit.action == NAV_NEXT) {
                turnPage(hit.action == NAV_PREV ? -1 : 1);
                return;
            }
            if (inList(mouseX, mouseY) || hit.action == LedgerNetwork.ACTION_BOOKMARK) {
                mc.getSoundHandler().playSound(
                        PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                NBTTagCompound where = where();
                if (hit.action == LedgerNetwork.ACTION_MARK) {
                    where.setString("marker", AtlasCompat.preferredMarker());
                }
                LedgerNetwork.CHANNEL.sendToServer(
                        new LedgerNetwork.ActionMessage(where, hit.action, hit.villager));
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (nameField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                commitName();
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                stopEditing();
            } else {
                nameField.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        if (mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_PRIOR) {
            turnPage(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT || keyCode == Keyboard.KEY_NEXT) {
            turnPage(1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
