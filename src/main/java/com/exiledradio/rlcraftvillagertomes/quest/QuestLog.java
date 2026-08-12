package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The quest log: a vanilla written book whose pages the server rewrites.
 *
 * <p>No item is registered for this, and that is the whole point. Registering one would
 * make the mod client-required - a client without it cannot map the server's item ID and is
 * disconnected on join, whatever {@code acceptableRemoteVersions} says. A written book
 * renders its own pages, paginates itself and is already on every client, so the log works
 * on a completely vanilla connection.
 *
 * <p>The readable pages are generated, not authored. The real data lives in a private tag
 * alongside them and the pages are rebuilt from it whenever anything changes, so an entry
 * can be updated in place as you deliver items rather than the book filling up with stale
 * snapshots.
 */
public final class QuestLog {

    /** Marks a written book as ours. Anything without this is somebody's actual book. */
    private static final String TAG_MARKER = "rlcvt_log";
    private static final String TAG_ENTRIES = "rlcvt_entries";

    private static final String TAG_UUID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";
    private static final String TAG_DIM = "dim";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_WANT = "want";
    private static final String TAG_ITEM = "item";
    private static final String TAG_META = "m";
    private static final String TAG_NEED = "need";
    private static final String TAG_GOT = "got";

    private static final String TITLE = "Villager Tomes Log";

    private QuestLog() {
    }

    // ------------------------------------------------------------- identifying

    /** True when this stack is one of our logs. */
    public static boolean isLog(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.WRITTEN_BOOK
                && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(TAG_MARKER);
    }

    /** True when this stack could become a log - a blank book and quill. */
    public static boolean isBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.WRITABLE_BOOK;
    }

    /**
     * Turns a book and quill into a log.
     *
     * <p>A book and quill rather than any written book on purpose: rewriting a book somebody
     * had actually authored would destroy it, and there is no way to tell one of those from
     * a book they meant to use as a log.
     */
    public static ItemStack createFrom(ItemStack blank, EntityPlayer owner) {
        ItemStack log = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(TAG_MARKER, true);
        tag.setString("title", TITLE);
        tag.setString("author", owner.getName());
        // Stops the client trying to resolve translation components in pages we generated
        // ourselves, which would otherwise re-run every time the book is opened.
        tag.setByte("resolved", (byte) 1);
        tag.setTag(TAG_ENTRIES, new NBTTagList());
        log.setTagCompound(tag);
        rebuildPages(log);
        blank.shrink(1);
        return log;
    }

    // ----------------------------------------------------------------- reading

    public static List<QuestEntry> readEntries(ItemStack log) {
        List<QuestEntry> entries = new ArrayList<QuestEntry>();
        if (!isLog(log)) {
            return entries;
        }
        NBTTagList list = log.getTagCompound().getTagList(TAG_ENTRIES, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            UUID id;
            try {
                id = UUID.fromString(tag.getString(TAG_UUID));
            } catch (IllegalArgumentException e) {
                continue;
            }
            QuestEntry entry = new QuestEntry(id, tag.getString(TAG_NAME),
                    tag.getInteger(TAG_X), tag.getInteger(TAG_Y), tag.getInteger(TAG_Z),
                    tag.getInteger(TAG_DIM), tag.getInteger(TAG_SLOT));

            List<BountyItem> wanted = new ArrayList<BountyItem>();
            NBTTagList want = tag.getTagList(TAG_WANT, 10);
            for (int j = 0; j < want.tagCount(); j++) {
                NBTTagCompound line = want.getCompoundTagAt(j);
                String item = line.getString(TAG_ITEM);
                if (item != null && !item.isEmpty()) {
                    wanted.add(new BountyItem(new ResourceLocation(item),
                            line.getInteger(TAG_META), line.getInteger(TAG_NEED),
                            line.getInteger(TAG_GOT)));
                }
            }
            entry.setWanted(wanted);
            entries.add(entry);
        }
        return entries;
    }

    public static QuestEntry find(ItemStack log, UUID villager) {
        for (QuestEntry entry : readEntries(log)) {
            if (entry.getVillager().equals(villager)) {
                return entry;
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- writing

    private static void writeEntries(ItemStack log, List<QuestEntry> entries) {
        NBTTagList list = new NBTTagList();
        for (QuestEntry entry : entries) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(TAG_UUID, entry.getVillager().toString());
            tag.setString(TAG_NAME, entry.getName());
            tag.setInteger(TAG_X, entry.getX());
            tag.setInteger(TAG_Y, entry.getY());
            tag.setInteger(TAG_Z, entry.getZ());
            tag.setInteger(TAG_DIM, entry.getDimension());
            tag.setInteger(TAG_SLOT, entry.getSlot());

            NBTTagList want = new NBTTagList();
            for (BountyItem line : entry.getWanted()) {
                NBTTagCompound l = new NBTTagCompound();
                l.setString(TAG_ITEM, line.getItemName().toString());
                l.setInteger(TAG_META, line.getMeta());
                l.setInteger(TAG_NEED, line.getRequired());
                l.setInteger(TAG_GOT, line.getDelivered());
                want.appendTag(l);
            }
            tag.setTag(TAG_WANT, want);
            list.appendTag(tag);
        }
        log.getTagCompound().setTag(TAG_ENTRIES, list);
        rebuildPages(log);
    }

    /**
     * Adds or refreshes one villager's entry.
     *
     * @return false when the log is full, in which case nothing was changed
     */
    public static boolean record(ItemStack log, QuestEntry entry) {
        List<QuestEntry> entries = readEntries(log);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getVillager().equals(entry.getVillager())) {
                entries.set(i, entry);
                writeEntries(log, entries);
                return true;
            }
        }
        if (entries.size() >= ModConfig.QUEST_LOG_CAPACITY) {
            return false;
        }
        entries.add(entry);
        writeEntries(log, entries);
        return true;
    }

    /** Drops a villager's entry, which is what happens once its slot is paid for. */
    public static boolean remove(ItemStack log, UUID villager) {
        List<QuestEntry> entries = readEntries(log);
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getVillager().equals(villager)) {
                entries.remove(i);
                writeEntries(log, entries);
                return true;
            }
        }
        return false;
    }

    public static int count(ItemStack log) {
        return readEntries(log).size();
    }

    // ------------------------------------------------------------------- pages

    /**
     * Regenerates the readable pages from the stored entries.
     *
     * <p>One villager per page. Written book pages are narrow and a page break is free, so
     * splitting by villager keeps every entry starting at a predictable place rather than
     * having two half-entries share a page.
     */
    private static void rebuildPages(ItemStack log) {
        List<QuestEntry> entries = readEntries(log);
        NBTTagList pages = new NBTTagList();

        if (entries.isEmpty()) {
            pages.appendTag(new NBTTagString(page(
                    "Villager Tomes Log\n\n"
                            + "Sneak-click a villager holding this book to write down what it "
                            + "wants for its next slot.\n\n"
                            + "Entries clear themselves once the slot is paid for.")));
        } else {
            for (QuestEntry entry : entries) {
                pages.appendTag(new NBTTagString(page(describe(entry))));
            }
        }

        log.getTagCompound().setTag("pages", pages);
    }

    private static String describe(QuestEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getName()).append('\n');
        sb.append("x").append(entry.getX())
                .append(" y").append(entry.getY())
                .append(" z").append(entry.getZ());
        if (entry.getDimension() != 0) {
            sb.append(" (dim ").append(entry.getDimension()).append(')');
        }
        sb.append("\n\nSlot ").append(entry.getSlot()).append(" needs:\n");

        for (BountyItem line : entry.getWanted()) {
            int left = line.getRemaining();
            sb.append(left <= 0 ? "* " : "- ")
                    .append(left <= 0 ? 0 : left)
                    .append(' ')
                    .append(shortName(line))
                    .append('\n');
        }
        return sb.toString();
    }

    /** Display name of a bounty line's item, falling back to its registry name. */
    private static String shortName(BountyItem line) {
        Item item = ForgeRegistries.ITEMS.getValue(line.getItemName());
        if (item == null) {
            return String.valueOf(line.getItemName());
        }
        return new ItemStack(item, 1, line.getMeta() < 0 ? 0 : line.getMeta()).getDisplayName();
    }

    /**
     * Wraps page text as the JSON a written book expects.
     *
     * <p>Pages in a signed book are strings holding a serialised text component. Writing raw
     * text into them mostly works and then fails on any page containing a character the
     * parser treats as markup, so it is built properly instead.
     */
    private static String page(String text) {
        ITextComponent component = new TextComponentString(text);
        return ITextComponent.Serializer.componentToJson(component);
    }
}
