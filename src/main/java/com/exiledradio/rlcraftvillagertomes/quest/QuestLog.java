package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import com.exiledradio.rlcraftvillagertomes.ledger.ItemLedger;
import com.exiledradio.rlcraftvillagertomes.ledger.ModContent;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reading and writing the villager entries a ledger carries.
 *
 * <p>Since 2.2.0 the log is a real item, {@link ItemLedger}. Before that it was a vanilla
 * written book whose pages the server rewrote, so the mod could stay server-only; those old
 * books are recognised by their marker tag and turned into ledgers the first time they are
 * used, entries and all ({@link #convertLegacy}).
 *
 * <p>The entry format is unchanged from the written-book days, which is what makes that
 * conversion a straight copy of one tag.
 *
 * <p>The entries themselves live in the world save ({@link LedgerData}), keyed by a number the
 * ledger carries, so copies made with a book all share one list. Everything here that reads
 * or writes them is server-side; on a client a ledger is just its number and a cached count.
 */
public final class QuestLog {

    /** Marks a pre-2.2.0 written book as one of our logs. */
    private static final String TAG_MARKER = "rlcvt_log";
    private static final String TAG_ENTRIES = "rlcvt_entries";
    /** The number of the shared list in {@link LedgerData} this ledger reads and writes. */
    private static final String TAG_LEDGER = "rlcvt_ledger";
    /** How many entries the shared list held when this stack last looked - for the tooltip. */
    private static final String TAG_COUNT = "rlcvt_count";

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
    private static final String TAG_FAV = "fav";
    private static final String TAG_WARD = "ward";

    /**
     * The most entries any ledger holds, whatever the config says. Opening a ledger sends
     * the whole list in one packet (about 1 MB at most), and 1000 entries stays far below it.
     */
    public static final int HARD_CAP = 1000;

    private QuestLog() {
    }

    // ------------------------------------------------------------- identifying

    /** True when this stack is a ledger. */
    public static boolean isLog(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModContent.LEDGER;
    }

    /** True when this stack is a pre-2.2.0 written-book log waiting to be converted. */
    public static boolean isLegacyLog(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.WRITTEN_BOOK
                && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(TAG_MARKER);
    }

    /** True when this stack could become a ledger - a blank book and quill. */
    public static boolean isBlank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.WRITABLE_BOOK;
    }

    /**
     * Turns a book and quill into an empty ledger.
     *
     * <p>A book and quill rather than any written book on purpose: a book somebody actually
     * wrote would be destroyed, and there is no telling it apart from one meant as a ledger.
     */
    public static ItemStack createFrom(ItemStack blank) {
        ItemStack ledger = new ItemStack(ModContent.LEDGER);
        ledger.setTagCompound(new NBTTagCompound());
        ledger.getTagCompound().setTag(TAG_ENTRIES, new NBTTagList());
        blank.shrink(1);
        return ledger;
    }

    /** A ledger holding the same entries as an old written-book log. */
    public static ItemStack convertLegacy(ItemStack old) {
        ItemStack ledger = new ItemStack(ModContent.LEDGER);
        ledger.setTagCompound(new NBTTagCompound());
        ledger.getTagCompound().setTag(TAG_ENTRIES,
                old.getTagCompound().getTagList(TAG_ENTRIES, 10).copy());
        return ledger;
    }

    // ----------------------------------------------------------------- reading

    // ----------------------------------------------------------------- linking

    /** The ledger's number, or 0 when it has not been linked to the shared store yet. */
    public static int idOf(ItemStack log) {
        return log.hasTagCompound() ? log.getTagCompound().getInteger(TAG_LEDGER) : 0;
    }

    /** The entry count this stack last saw. Safe on the client, where only the stack exists. */
    public static int cachedCount(ItemStack log) {
        if (!log.hasTagCompound()) {
            return 0;
        }
        NBTTagCompound tag = log.getTagCompound();
        return tag.hasKey(TAG_COUNT) ? tag.getInteger(TAG_COUNT)
                : tag.getTagList(TAG_ENTRIES, 10).tagCount();
    }

    /**
     * The shared list this ledger reads and writes, linking the ledger first if needed.
     *
     * <p>A ledger made before entries moved into the world save still has them on the item;
     * the first time the server touches it they are moved across and the ledger gets a
     * number. A ledger whose number the world has never seen (carried in from another save,
     * say) starts that number with an empty list rather than being renumbered, so any copies
     * of it stay together.
     *
     * @return null on a client, where there is no shared store
     */
    private static NBTTagList shared(ItemStack log) {
        LedgerData data = LedgerData.get();
        if (data == null) {
            return null;
        }
        if (!log.hasTagCompound()) {
            log.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = log.getTagCompound();
        int id = tag.getInteger(TAG_LEDGER);
        if (id > 0) {
            NBTTagList list = data.get(id);
            if (list == null) {
                list = new NBTTagList();
                data.put(id, list);
            }
            return list;
        }
        NBTTagList list = tag.hasKey(TAG_ENTRIES)
                ? (NBTTagList) tag.getTagList(TAG_ENTRIES, 10).copy() : new NBTTagList();
        id = data.newId();
        data.put(id, list);
        tag.removeTag(TAG_ENTRIES);
        tag.setInteger(TAG_LEDGER, id);
        tag.setInteger(TAG_COUNT, list.tagCount());
        return list;
    }

    /**
     * Server side: makes sure the ledger has a number and its tooltip count is current.
     * Run from the ledger's inventory tick, which is what keeps every copy's tooltip in step
     * with the others. Only touches the stack when something actually changed, since any
     * change to it is sent to the client.
     */
    public static void sync(ItemStack log) {
        if (!isLog(log)) {
            return;
        }
        NBTTagList list = shared(log);
        if (list != null && log.getTagCompound().getInteger(TAG_COUNT) != list.tagCount()) {
            log.getTagCompound().setInteger(TAG_COUNT, list.tagCount());
        }
    }

    /** A new ledger stack sharing this one's list - what the copying recipe makes. */
    public static ItemStack copyOf(ItemStack log) {
        if (LedgerData.get() != null) {
            shared(log);
        }
        ItemStack copy = new ItemStack(ModContent.LEDGER);
        if (log.hasTagCompound()) {
            copy.setTagCompound(log.getTagCompound().copy());
        }
        return copy;
    }

    // ----------------------------------------------------------------- reading

    public static List<QuestEntry> readEntries(ItemStack log) {
        if (!isLog(log)) {
            return new ArrayList<QuestEntry>();
        }
        NBTTagList list = shared(log);
        if (list == null) {
            return log.hasTagCompound()
                    ? readEntries(log.getTagCompound().getTagList(TAG_ENTRIES, 10))
                    : new ArrayList<QuestEntry>();
        }
        return readEntries(list);
    }

    /** Entries from a bare list - a ledger's, or the copy a screen is sent. */
    public static List<QuestEntry> readEntries(NBTTagList list) {
        List<QuestEntry> entries = new ArrayList<QuestEntry>();
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
            entry.setFavorite(tag.getLong(TAG_FAV));
            entry.setWard(tag.getByte(TAG_WARD));
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

    public static void writeEntries(ItemStack log, List<QuestEntry> entries) {
        if (!log.hasTagCompound()) {
            log.setTagCompound(new NBTTagCompound());
        }
        LedgerData data = LedgerData.get();
        if (data == null) {
            log.getTagCompound().setTag(TAG_ENTRIES, toTag(entries));
            return;
        }
        shared(log);
        data.put(log.getTagCompound().getInteger(TAG_LEDGER), toTag(entries));
        log.getTagCompound().setInteger(TAG_COUNT, entries.size());
    }

    /** The stored form of a list of entries, also what a screen is sent. */
    public static NBTTagList toTag(List<QuestEntry> entries) {
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
            if (entry.getFavorite() > 0) {
                tag.setLong(TAG_FAV, entry.getFavorite());
            }
            if (entry.getWard() > 0) {
                tag.setByte(TAG_WARD, (byte) entry.getWard());
            }
            list.appendTag(tag);
        }
        return list;
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
                // Writing a villager down again must not cost it its star.
                entry.setFavorite(entries.get(i).getFavorite());
                entries.set(i, entry);
                writeEntries(log, entries);
                return true;
            }
        }
        if (entries.size() >= capacity()) {
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

    /** How many entries a ledger may hold: the config value, or {@link #HARD_CAP} for 0. */
    public static int capacity() {
        int configured = ModConfig.QUEST_LOG_CAPACITY;
        return configured <= 0 ? HARD_CAP : Math.min(configured, HARD_CAP);
    }

    /** True when the config sets no limit of its own, so counts are shown without a "/max". */
    public static boolean unlimited() {
        return ModConfig.QUEST_LOG_CAPACITY <= 0;
    }

    /** "12 logged", or "12/64 logged" when a limit is configured. */
    public static String countLabel(int count) {
        return unlimited() ? count + " logged" : count + "/" + capacity() + " logged";
    }

    // --------------------------------------------------------------- bookmark

    /** The bookmarked villager's id as a string, or "" for none. Server side only. */
    public static String getBookmark(ItemStack log) {
        LedgerData data = LedgerData.get();
        if (data == null || !isLog(log) || shared(log) == null) {
            return "";
        }
        return data.getBookmark(idOf(log));
    }

    public static void setBookmark(ItemStack log, UUID villager) {
        LedgerData data = LedgerData.get();
        if (data == null || !isLog(log) || shared(log) == null) {
            return;
        }
        data.setBookmark(idOf(log), villager == null ? null : villager.toString());
    }

    // -------------------------------------------------------------- favorite

    /**
     * Stars or unstars a villager. A new star goes after every existing one, so favorites
     * stack up in the order they were added.
     */
    public static boolean toggleFavorite(ItemStack log, UUID villager) {
        List<QuestEntry> entries = readEntries(log);
        long next = 0;
        QuestEntry target = null;
        for (QuestEntry entry : entries) {
            next = Math.max(next, entry.getFavorite());
            if (entry.getVillager().equals(villager)) {
                target = entry;
            }
        }
        if (target == null) {
            return false;
        }
        target.setFavorite(target.getFavorite() > 0 ? 0 : next + 1);
        writeEntries(log, entries);
        return true;
    }
}
