package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.quest.QuestEntry;
import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * What a ledger screen shows: the entries, which of their villagers are loaded, and where
 * the ledger is - in a hand, or on a lectern.
 *
 * <p>Built on the server, where loaded villagers can be looked up, and sent whole each time
 * anything changes. Ten entries of a few lines each is a small packet, and sending the full
 * picture means the screen never has to reconcile partial updates.
 */
public final class LedgerView {

    public static final byte SOURCE_HAND = 0;
    public static final byte SOURCE_LECTERN = 1;

    public final byte source;
    public final EnumHand hand;
    public final BlockPos pos;
    public final List<QuestEntry> entries;
    public final boolean[] loaded;
    /** 0 = no configured limit. */
    public final int capacity;
    /** The bookmarked villager's id, or "" for none. */
    public String bookmark = "";

    public LedgerView(byte source, EnumHand hand, BlockPos pos, List<QuestEntry> entries,
                      boolean[] loaded, int capacity) {
        this.source = source;
        this.hand = hand;
        this.pos = pos;
        this.entries = entries;
        this.loaded = loaded;
        this.capacity = capacity;
    }

    public boolean isLectern() {
        return source == SOURCE_LECTERN;
    }

    public NBTTagCompound toTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("source", source);
        tag.setByte("hand", (byte) hand.ordinal());
        tag.setLong("pos", pos.toLong());
        tag.setTag("entries", QuestLog.toTag(entries));
        byte[] flags = new byte[loaded.length];
        for (int i = 0; i < loaded.length; i++) {
            flags[i] = (byte) (loaded[i] ? 1 : 0);
        }
        tag.setByteArray("loaded", flags);
        tag.setInteger("capacity", capacity);
        tag.setString("bookmark", bookmark);
        return tag;
    }

    public static LedgerView fromTag(NBTTagCompound tag) {
        List<QuestEntry> entries = QuestLog.readEntries(tag.getTagList("entries", 10));
        byte[] flags = tag.getByteArray("loaded");
        boolean[] loaded = new boolean[entries.size()];
        for (int i = 0; i < loaded.length && i < flags.length; i++) {
            loaded[i] = flags[i] != 0;
        }
        int handIndex = tag.getByte("hand");
        EnumHand hand = handIndex == 1 ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        LedgerView view = new LedgerView(tag.getByte("source"), hand,
                BlockPos.fromLong(tag.getLong("pos")), entries, loaded, tag.getInteger("capacity"));
        view.bookmark = tag.getString("bookmark");
        return view;
    }
}
