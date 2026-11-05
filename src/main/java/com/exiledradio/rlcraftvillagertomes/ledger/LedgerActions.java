package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.Feedback;
import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.quest.QuestBinding;
import com.exiledradio.rlcraftvillagertomes.quest.QuestEntry;
import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Everything the ledger screen asks the server to do.
 *
 * <p>Every request is re-checked here from scratch - which ledger, whether the player can
 * still reach the lectern, whether the villager is loaded - because the client only ever
 * says what button was pressed.
 */
public final class LedgerActions {

    private static final String PREFIX =
            TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;

    private LedgerActions() {
    }

    // ------------------------------------------------------------------ open

    public static void openHeld(EntityPlayerMP player, EnumHand hand) {
        ItemStack ledger = player.getHeldItem(hand);
        if (!QuestLog.isLog(ledger)) {
            return;
        }
        send(player, LedgerView.SOURCE_HAND, hand, BlockPos.ORIGIN, ledger);
        flip(player, player.posX, player.posY + player.getEyeHeight(), player.posZ,
                SoundCategory.PLAYERS);
    }

    public static void openLectern(EntityPlayerMP player, BlockPos pos) {
        TileLedgerLectern tile = lectern(player, pos);
        if (tile == null) {
            return;
        }
        send(player, LedgerView.SOURCE_LECTERN, EnumHand.MAIN_HAND, pos, tile.getBook());
        tile.bookChanged();
        flip(player, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                SoundCategory.BLOCKS);
    }

    /**
     * The page flip on opening. Played into the world rather than on the reader's client,
     * so whoever is standing nearby hears the ledger open - the way a 1.14 lectern sounds.
     * Only on opening: a refresh after Deliver or Remove happens with the book already open.
     */
    private static void flip(EntityPlayerMP player, double x, double y, double z,
                             SoundCategory category) {
        player.world.playSound(null, x, y, z, ModContent.LEDGER_OPEN, category, 1.0F,
                0.9F + player.world.rand.nextFloat() * 0.2F);
    }

    /** Refreshes the ledger against its loaded villagers and sends the result to the player. */
    private static void send(EntityPlayerMP player, byte source, EnumHand hand, BlockPos pos,
                             ItemStack ledger) {
        List<QuestEntry> entries = QuestLog.readEntries(ledger);
        boolean[] loaded = refresh(entries);
        QuestLog.writeEntries(ledger, entries);

        // Display order: favorites first, in the order they were starred, then everything
        // else in the order it was written down. Only the view is sorted - the stored list
        // keeps its order, so unstarring puts a villager back where it was.
        Integer[] order = new Integer[entries.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        final List<QuestEntry> stored = entries;
        java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                long fa = stored.get(a).getFavorite();
                long fb = stored.get(b).getFavorite();
                if ((fa > 0) != (fb > 0)) {
                    return fa > 0 ? -1 : 1;
                }
                if (fa > 0 && fa != fb) {
                    return fa < fb ? -1 : 1;
                }
                return a.compareTo(b);
            }
        });
        List<QuestEntry> shown = new java.util.ArrayList<QuestEntry>(entries.size());
        boolean[] shownLoaded = new boolean[entries.size()];
        for (int i = 0; i < order.length; i++) {
            shown.add(entries.get(order[i]));
            shownLoaded[i] = loaded[order[i]];
        }

        LedgerView view = new LedgerView(source, hand, pos, shown, shownLoaded,
                QuestLog.unlimited() ? 0 : QuestLog.capacity());
        view.bookmark = QuestLog.getBookmark(ledger);
        LedgerNetwork.CHANNEL.sendTo(new LedgerNetwork.ViewMessage(view.toTag()), player);
    }

    /**
     * Brings every entry whose villager is loaded up to date, and drops the ones that have
     * nothing left to sell.
     *
     * <p>An entry whose villager is not loaded keeps its last snapshot. Loaded or not cannot
     * tell a villager that died from one in an unloaded chunk, so nothing is ever removed for
     * being missing - the Remove button is for that.
     *
     * @return per entry, after removals, whether its villager is loaded
     */
    private static boolean[] refresh(List<QuestEntry> entries) {
        Iterator<QuestEntry> it = entries.iterator();
        while (it.hasNext()) {
            QuestEntry entry = it.next();
            EntityVillager villager = findLoaded(entry);
            if (villager == null) {
                continue;
            }
            ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
            if (tomes == null || !SlotRequests.canUnlockMore(tomes)) {
                it.remove();
                continue;
            }
            SlotRequests.ensureRequest(villager, tomes);
            entry.setSlot(SlotRequests.unlockedSlots(tomes) + 1);
            entry.setWanted(tomes.getRequest());
            entry.setName(QuestBinding.storedName(villager));
            entry.setWard(tomes.getWard());
            entry.setPosition((int) Math.floor(villager.posX), (int) Math.floor(villager.posY),
                    (int) Math.floor(villager.posZ), villager.world.provider.getDimension());
        }

        boolean[] loaded = new boolean[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            loaded[i] = findLoaded(entries.get(i)) != null;
        }
        return loaded;
    }

    /**
     * The villager behind an entry, if it is loaded right now.
     *
     * <p>Asks the dimension's own entity map, which only holds entities in loaded chunks, and
     * never loads a dimension to look - {@code DimensionManager.getWorld} returns null for
     * one that is not running.
     */
    private static EntityVillager findLoaded(QuestEntry entry) {
        WorldServer world = DimensionManager.getWorld(entry.getDimension());
        if (world == null) {
            return null;
        }
        Entity entity = world.getEntityFromUuid(entry.getVillager());
        if (!(entity instanceof EntityVillager) || entity.isDead) {
            return null;
        }
        return (EntityVillager) entity;
    }

    // ---------------------------------------------------------------- action

    public static void handle(EntityPlayerMP player, LedgerNetwork.ActionMessage message) {
        LedgerView where = LedgerView.fromTag(message.where);
        TileLedgerLectern tile = null;
        ItemStack ledger;
        if (where.isLectern()) {
            tile = lectern(player, where.pos);
            if (tile == null) {
                return;
            }
            ledger = tile.getBook();
        } else {
            ledger = player.getHeldItem(where.hand);
            if (!QuestLog.isLog(ledger)) {
                return;
            }
        }

        if (message.action == LedgerNetwork.ACTION_REMOVE) {
            QuestLog.remove(ledger, message.villager);
        } else if (message.action == LedgerNetwork.ACTION_DELIVER && tile != null) {
            deliver(player, ledger, message.villager);
        } else if (message.action == LedgerNetwork.ACTION_RENAME) {
            rename(player, ledger, message.villager, message.where.getString("name"));
        } else if (message.action == LedgerNetwork.ACTION_FAVORITE) {
            QuestLog.toggleFavorite(ledger, message.villager);
        } else if (message.action == LedgerNetwork.ACTION_BOOKMARK) {
            boolean same = message.villager.toString().equals(QuestLog.getBookmark(ledger));
            QuestLog.setBookmark(ledger, same ? null : message.villager);
            tell(player, same ? Feedback.INFO : Feedback.GOOD,
                    same ? "Bookmark removed" : "Bookmarked - the ledger opens on this page");
        } else if (message.action == LedgerNetwork.ACTION_WARD) {
            cycleWard(player, ledger, message.villager);
        } else if (message.action == LedgerNetwork.ACTION_MARK) {
            markOnAtlas(player, ledger, message.villager, message.where.getString("marker"));
            return; // nothing in the ledger changed, so no resend
        }

        if (tile != null) {
            send(player, LedgerView.SOURCE_LECTERN, EnumHand.MAIN_HAND, where.pos, ledger);
            tile.bookChanged();
        } else {
            send(player, LedgerView.SOURCE_HAND, where.hand, BlockPos.ORIGIN, ledger);
        }
    }

    private static void deliver(EntityPlayerMP player, ItemStack ledger, UUID id) {
        QuestEntry entry = QuestLog.find(ledger, id);
        if (entry == null) {
            return;
        }
        EntityVillager villager = findLoaded(entry);
        if (villager == null) {
            tell(player, TextFormatting.GRAY
                    + "That villager is not loaded. Head back to its village first.");
            return;
        }
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null || !SlotRequests.canUnlockMore(tomes)) {
            return;
        }
        SlotRequests.ensureRequest(villager, tomes);
        int taken = SlotRequests.deliverFromInventory(player, villager, tomes);
        if (taken <= 0) {
            tell(player, TextFormatting.GRAY + "You are not carrying anything it still needs."
                    + " Enchanted or renamed items have to be handed over in person.");
            return;
        }
        // The carried ledger, if any, was kept in step by the delivery itself. This one is
        // the lectern's, refreshed by the resend that follows.
        QuestBinding.refreshIfLogged(player, villager, tomes);
    }

    /**
     * The ledger's ward icon: moves a loaded, warded villager to its next mode, the same
     * cycle as the button on its trade screen. Applying a ward in the first place still takes
     * the Villager Ward item.
     */
    private static void cycleWard(EntityPlayerMP player, ItemStack ledger, UUID id) {
        QuestEntry entry = QuestLog.find(ledger, id);
        if (entry == null) {
            return;
        }
        EntityVillager villager = findLoaded(entry);
        if (villager == null) {
            tell(player, "That villager is not loaded. Head back to its village to change its ward.");
            return;
        }
        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null) {
            return;
        }
        com.exiledradio.rlcraftvillagertomes.ward.WardMode mode =
                com.exiledradio.rlcraftvillagertomes.ward.WardMode.of(tomes.getWard());
        if (mode == com.exiledradio.rlcraftvillagertomes.ward.WardMode.NONE) {
            tell(player, Feedback.WARN, "Not warded - use a Villager Ward on it first");
            return;
        }
        mode = mode.next();
        tomes.setWard(mode.ordinal());
        tell(player, Feedback.GOOD, "Ward: " + mode.getLabel());
    }

    /** Same cap as an anvil, so a ledger name is never longer than a name tag could be. */
    public static final int MAX_NAME_LENGTH = 35;

    /**
     * Names the villager itself, exactly as a name tag would, so the name shows over its head
     * and in every ledger it is in. An empty name takes the name off again and the villager
     * goes back to being listed by its career.
     *
     * <p>Only for a loaded villager: the name belongs to the villager, not to the ledger, and
     * a name written only in the ledger would be undone by the next refresh anyway.
     */
    private static void rename(EntityPlayerMP player, ItemStack ledger, UUID id, String name) {
        QuestEntry entry = QuestLog.find(ledger, id);
        if (entry == null) {
            return;
        }
        EntityVillager villager = findLoaded(entry);
        if (villager == null) {
            tell(player, TextFormatting.GRAY
                    + "That villager is not loaded. Head back to its village to rename it.");
            return;
        }
        String clean = TextFormatting.getTextWithoutFormattingCodes(name == null ? "" : name);
        clean = clean == null ? "" : clean.trim();
        if (clean.length() > MAX_NAME_LENGTH) {
            clean = clean.substring(0, MAX_NAME_LENGTH);
        }
        // Villagers never despawn, so unlike a named zombie nothing else needs setting.
        villager.setCustomNameTag(clean);
        // The resend that follows refreshes this ledger's entry from the villager.
    }

    /**
     * Puts the villager on every Antique Atlas the player carries, at its last known spot,
     * labelled with its name. Works for unloaded villagers too - the ledger knows where they
     * were - as long as their dimension is running, since the atlas files markers by world.
     */
    private static void markOnAtlas(EntityPlayerMP player, ItemStack ledger, UUID id,
                                    String marker) {
        QuestEntry entry = QuestLog.find(ledger, id);
        if (entry == null) {
            return;
        }
        WorldServer world = DimensionManager.getWorld(entry.getDimension());
        if (world == null) {
            tell(player, TextFormatting.GRAY + "That dimension is not loaded right now.");
            return;
        }
        String label = entry.getNameComponent().getUnformattedText();
        int marked = AtlasCompat.mark(player, world, marker, label, entry.getX(),
                entry.getZ());
        if (marked < 0) {
            tell(player, TextFormatting.GRAY + "Antique Atlas is not installed.");
        } else if (marked == 0) {
            tell(player, TextFormatting.GRAY + "You need an Antique Atlas in your inventory.");
        } else {
            tell(player, Feedback.GOOD, "Marked " + label + " on your atlas at "
                    + entry.getX() + ", " + entry.getZ());
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    ModContent.LEDGER_OPEN, SoundCategory.PLAYERS, 0.6F, 1.2F);
        }
    }

    private static TileLedgerLectern lectern(EntityPlayerMP player, BlockPos pos) {
        TileEntity tile = player.world.getTileEntity(pos);
        if (!(tile instanceof TileLedgerLectern)) {
            return null;
        }
        TileLedgerLectern lectern = (TileLedgerLectern) tile;
        return lectern.isUsableBy(player) ? lectern : null;
    }

    /** Shown in the open ledger's footer - see {@code Feedback}. */
    private static void tell(EntityPlayerMP player, String text) {
        tell(player, Feedback.INFO, text);
    }

    private static void tell(EntityPlayerMP player, byte tone, String text) {
        Feedback.notify(player, tone, TextFormatting.getTextWithoutFormattingCodes(text));
    }
}
