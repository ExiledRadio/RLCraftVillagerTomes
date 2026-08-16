package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Naming a villager so it can be written into a quest log.
 *
 * <p>Done entirely with clickable chat components rather than a screen. A text field with
 * Confirm and Cancel buttons would mean a GUI and a packet channel, and both of those make
 * the mod client-required - so instead the prompt is a chat line with two buttons on it,
 * which a completely vanilla client renders and handles on its own.
 *
 * <p>Clicking the name button pre-fills the chat box with the command; typing the name and
 * pressing enter is the confirm. That is one keystroke more than a dialog would be, and it
 * costs nothing in compatibility.
 */
public final class QuestBinding {

    /** Who is part-way through naming what. */
    private static final Map<UUID, Pending> PENDING = new HashMap<UUID, Pending>();

    private QuestBinding() {
    }

    private static final class Pending {
        final UUID villager;
        final long expiresAt;

        Pending(UUID villager, long expiresAt) {
            this.villager = villager;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Offers to write this villager down, or writes it immediately when it is already named.
     *
     * <p>A villager that has a name needs no prompt - the interesting part of naming is
     * giving it one, and asking again for a villager already called Gareth is a click for
     * nothing.
     */
    public static void offer(EntityPlayer player, EntityVillager villager, ItemStack log,
                             ITomeKnowledge tomes) {
        if (SlotRequests.openSlots(tomes) > 0) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "This villager already has room - nothing to write down."));
            return;
        }
        if (SlotRequests.unlockedSlots(tomes) >= ModConfig.MAX_TOMES_PER_VILLAGER) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "This villager is full - nothing left to save for."));
            return;
        }

        // Demands are rolled lazily, so a villager nobody has offered a book to has none
        // yet. Rolling it here is what lets the log be the first thing you show a villager
        // rather than something you can only use after being refused once.
        SlotRequests.ensureRequest(villager, tomes);
        if (tomes.getRequest().isEmpty()) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "This villager is not asking for anything."));
            return;
        }

        if (villager.hasCustomName()) {
            write(player, villager, log, tomes, villager.getCustomNameTag());
            return;
        }

        PENDING.put(player.getUniqueID(), new Pending(villager.getUniqueID(),
                System.currentTimeMillis() + 60000L));

        ITextComponent message = new TextComponentString(prefix() + TextFormatting.AQUA
                + "Name this villager to log it: ");
        message.appendSibling(button("[Name it]", TextFormatting.GREEN,
                ClickEvent.Action.SUGGEST_COMMAND, "/villagertomes name ",
                "Type a name and press enter"));
        message.appendSibling(new TextComponentString(" "));
        message.appendSibling(button("[Cancel]", TextFormatting.RED,
                ClickEvent.Action.RUN_COMMAND, "/villagertomes cancel",
                "Forget it"));
        player.sendMessage(message);
    }

    /** Completes a pending naming. Returns a message to show the player. */
    public static String completeNaming(EntityPlayer player, String name) {
        Pending pending = PENDING.remove(player.getUniqueID());
        if (pending == null) {
            return "You are not naming a villager right now. Sneak-click one holding your log.";
        }
        if (System.currentTimeMillis() > pending.expiresAt) {
            return "That took too long - sneak-click the villager again.";
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "A name cannot be blank.";
        }
        if (trimmed.length() > 32) {
            trimmed = trimmed.substring(0, 32);
        }

        EntityVillager villager = findVillager(player, pending.villager);
        if (villager == null) {
            return "That villager is no longer nearby.";
        }

        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return "You are not holding your quest log.";
        }

        ITomeKnowledge tomes = CapabilityTomeKnowledge.get(villager);
        if (tomes == null) {
            return "That villager cannot be logged.";
        }

        villager.setCustomNameTag(trimmed);
        write(player, villager, log, tomes, trimmed);
        return null;
    }

    public static String cancel(EntityPlayer player) {
        return PENDING.remove(player.getUniqueID()) == null
                ? "You were not naming anything."
                : "Cancelled.";
    }

    /** Writes or refreshes the entry, and reports what happened. */
    public static void write(EntityPlayer player, EntityVillager villager, ItemStack log,
                             ITomeKnowledge tomes, String name) {
        QuestEntry entry = new QuestEntry(villager.getUniqueID(), name,
                (int) Math.floor(villager.posX), (int) Math.floor(villager.posY),
                (int) Math.floor(villager.posZ), villager.world.provider.getDimension(),
                SlotRequests.unlockedSlots(tomes) + 1);
        entry.setWanted(tomes.getRequest());

        boolean existed = QuestLog.find(log, villager.getUniqueID()) != null;
        if (!QuestLog.record(log, entry)) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.YELLOW
                    + "Your log is full at " + ModConfig.QUEST_LOG_CAPACITY
                    + " villagers. Finish one off, or start another book."));
            return;
        }

        ITextComponent message = new TextComponentString(prefix() + TextFormatting.GREEN
                + (existed ? "Updated " : "Wrote down ") + name + "."
                + TextFormatting.GRAY + " " + QuestLog.count(log) + "/"
                + ModConfig.QUEST_LOG_CAPACITY + " logged. ");
        // Offered right where the entry was made, because that is the moment you know
        // whether you meant to log this one - and a full log otherwise needs a page-by-page
        // hunt for something to drop.
        message.appendSibling(button("[Remove]", TextFormatting.RED,
                ClickEvent.Action.RUN_COMMAND,
                "/villagertomes unlog " + villager.getUniqueID(),
                "Delete this villager from your log"));
        player.sendMessage(message);
    }

    /**
     * Prints the whole log to chat, one line per villager, each with its own remove button.
     *
     * <p>The written book renders the same entries far more nicely, so this is not a
     * duplicate of it - it exists because the book cannot carry buttons. Removing an entry
     * any other way means standing next to the villager it belongs to, and the entries you
     * most want gone are exactly the ones you cannot get back to: a villager that died, got
     * carted off by a zombie siege, or is buried under a village you have since abandoned.
     * Those would otherwise sit in the book forever, eating one of ten slots.
     */
    public static void listEntries(EntityPlayer player, ItemStack log) {
        List<QuestEntry> entries = QuestLog.readEntries(log);
        if (entries.isEmpty()) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "Your log is empty. Sneak-click a villager with it to write one down."));
            return;
        }

        player.sendMessage(new TextComponentString(prefix() + TextFormatting.AQUA
                + "Quest log " + TextFormatting.WHITE + entries.size() + "/"
                + ModConfig.QUEST_LOG_CAPACITY + TextFormatting.GRAY
                + " - [Remove] drops an entry without needing the villager"));

        for (QuestEntry entry : entries) {
            int outstanding = 0;
            for (BountyItem line : entry.getWanted()) {
                if (!line.isSatisfied()) {
                    outstanding++;
                }
            }

            ITextComponent row = new TextComponentString(TextFormatting.WHITE + "  "
                    + entry.getName() + TextFormatting.DARK_GRAY + "  "
                    + entry.getX() + " " + entry.getY() + " " + entry.getZ()
                    + TextFormatting.GRAY + "  slot " + entry.getSlot() + ", "
                    + (outstanding == 0
                            ? "paid off"
                            : outstanding + " item" + (outstanding == 1 ? "" : "s") + " left")
                    + " ");
            row.appendSibling(button("[Remove]", TextFormatting.RED,
                    ClickEvent.Action.RUN_COMMAND,
                    "/villagertomes unlog " + entry.getVillager(),
                    "Delete " + entry.getName() + " from your log"));
            player.sendMessage(row);
        }
    }

    /** Drops one villager's entry by id, which is what the [Remove] button calls. */
    public static String unlog(EntityPlayer player, String rawId) {
        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return "You are not carrying a quest log.";
        }
        UUID id;
        try {
            id = UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException e) {
            return "That is not a villager id.";
        }
        QuestEntry entry = QuestLog.find(log, id);
        if (entry == null) {
            return "That villager is not in your log.";
        }
        QuestLog.remove(log, id);
        return "Removed " + entry.getName() + " from your log. "
                + QuestLog.count(log) + "/" + ModConfig.QUEST_LOG_CAPACITY + " logged.";
    }

    /**
     * Clears a villager's entry from a held log once its slot is paid for.
     *
     * <p>Silent when there is nothing to clear, since most deliveries are made by somebody
     * not carrying a log at all.
     */
    public static void clearIfLogged(EntityPlayer player, EntityVillager villager) {
        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return;
        }
        if (QuestLog.remove(log, villager.getUniqueID())) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "Crossed off your log."));
        }
    }

    /** Refreshes a held log's entry for this villager, so progress shows without re-naming. */
    public static void refreshIfLogged(EntityPlayer player, EntityVillager villager,
                                       ITomeKnowledge tomes) {
        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return;
        }
        QuestEntry existing = QuestLog.find(log, villager.getUniqueID());
        if (existing == null) {
            return;
        }
        existing.setSlot(SlotRequests.unlockedSlots(tomes) + 1);
        existing.setWanted(tomes.getRequest());
        QuestLog.record(log, existing);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The player's quest log - hands first, then the rest of the inventory.
     *
     * <p>Searching past the hands matters because a freshly made log does not always end up
     * in one: turning a stack of quills into a log leaves the quills where they were and the
     * log wherever there was room. Returning the live stack rather than a copy is the whole
     * point, since everything written afterwards goes through this.
     */
    public static ItemStack findLog(EntityPlayer player) {
        for (ItemStack stack : player.getHeldEquipment()) {
            if (QuestLog.isLog(stack)) {
                return stack;
            }
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (QuestLog.isLog(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Puts a newly made log where the player can reach it, and hands back the live stack.
     *
     * <p>{@code addItemStackToInventory} stores a <em>copy</em> and empties the stack it was
     * given, so writing to the stack you passed it writes into nothing - which is exactly
     * why a first entry reported "0/10 logged". Whatever route the log takes, the stack that
     * actually lives in the inventory is looked up again afterwards.
     */
    public static ItemStack placeNewLog(EntityPlayer player, ItemStack log) {
        if (player.getHeldItemMainhand().isEmpty()) {
            player.setHeldItem(EnumHand.MAIN_HAND, log);
        } else if (!player.inventory.addItemStackToInventory(log)) {
            player.dropItem(log, false);
            return ItemStack.EMPTY;
        }
        return findLog(player);
    }

    private static EntityVillager findVillager(EntityPlayer player, UUID id) {
        for (EntityVillager candidate : player.world.getEntitiesWithinAABB(
                EntityVillager.class, player.getEntityBoundingBox().grow(16.0D))) {
            if (candidate.getUniqueID().equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    private static ITextComponent button(String label, TextFormatting colour,
                                         ClickEvent.Action action, String command,
                                         String tooltip) {
        ITextComponent button = new TextComponentString(colour + label);
        button.getStyle()
                .setClickEvent(new ClickEvent(action, command))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString(tooltip)));
        return button;
    }

    private static String prefix() {
        return TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;
    }
}
