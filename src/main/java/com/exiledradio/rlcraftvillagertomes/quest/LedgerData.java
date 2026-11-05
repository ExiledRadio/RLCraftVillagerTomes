package com.exiledradio.rlcraftvillagertomes.quest;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

/**
 * Every ledger's entries, kept in the world save rather than on the item.
 *
 * <p>A ledger stack only carries a number. Copies made with a book carry the same number, so
 * they all read and write the one list here, the way copied Antique Atlases share one map:
 * log a villager in one copy and it is in all of them, deliver from a lectern and the copy in
 * your pocket shows it paid.
 *
 * <p>Stored in the global map storage (the one {@code map_N.dat} lives in), so it is the same
 * list in every dimension.
 */
public class LedgerData extends net.minecraft.world.storage.WorldSavedData {

    public static final String NAME = "rlcraftvillagertomes_ledgers";

    private static final String TAG_NEXT = "next";
    private static final String TAG_LEDGERS = "ledgers";
    private static final String TAG_ID = "id";
    private static final String TAG_ENTRIES = "entries";
    private static final String TAG_BOOKMARK = "bookmark";

    private int nextId = 1;
    private final Map<Integer, NBTTagList> ledgers = new HashMap<Integer, NBTTagList>();
    /** Per ledger: the villager whose page it opens to. Shared by copies, like the list. */
    private final Map<Integer, String> bookmarks = new HashMap<Integer, String>();

    public LedgerData(String name) {
        super(name);
    }

    /**
     * The server's ledger store, or null off the server thread. The thread check matters in
     * single player, where the client thread can see the integrated server too - the client's
     * crafting preview and tooltips must never touch the real lists.
     */
    public static LedgerData get() {
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
            return null;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        World overworld = server.getWorld(0);
        if (overworld == null) {
            return null;
        }
        MapStorage storage = overworld.getMapStorage();
        LedgerData data = (LedgerData) storage.getOrLoadData(LedgerData.class, NAME);
        if (data == null) {
            data = new LedgerData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    /** A number no ledger has used yet. */
    public int newId() {
        int id = nextId++;
        markDirty();
        return id;
    }

    public NBTTagList get(int id) {
        return ledgers.get(id);
    }

    public void put(int id, NBTTagList entries) {
        ledgers.put(id, entries);
        if (id >= nextId) {
            nextId = id + 1;
        }
        markDirty();
    }

    public String getBookmark(int id) {
        String b = bookmarks.get(id);
        return b == null ? "" : b;
    }

    public void setBookmark(int id, String villager) {
        if (villager == null || villager.isEmpty()) {
            bookmarks.remove(id);
        } else {
            bookmarks.put(id, villager);
        }
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        nextId = Math.max(1, tag.getInteger(TAG_NEXT));
        ledgers.clear();
        NBTTagList list = tag.getTagList(TAG_LEDGERS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound one = list.getCompoundTagAt(i);
            int id = one.getInteger(TAG_ID);
            ledgers.put(id, one.getTagList(TAG_ENTRIES, 10));
            if (one.hasKey(TAG_BOOKMARK)) {
                bookmarks.put(id, one.getString(TAG_BOOKMARK));
            }
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger(TAG_NEXT, nextId);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, NBTTagList> e : ledgers.entrySet()) {
            NBTTagCompound one = new NBTTagCompound();
            one.setInteger(TAG_ID, e.getKey());
            one.setTag(TAG_ENTRIES, e.getValue());
            String bookmark = bookmarks.get(e.getKey());
            if (bookmark != null) {
                one.setString(TAG_BOOKMARK, bookmark);
            }
            list.appendTag(one);
        }
        tag.setTag(TAG_LEDGERS, list);
        return tag;
    }
}
