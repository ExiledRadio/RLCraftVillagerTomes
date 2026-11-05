package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.Feedback;
import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.BountyItem;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
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

import java.util.List;
import java.util.UUID;

/**
 * Writing villagers into a ledger, and keeping a carried ledger in step with them.
 *
 * <p>There used to be a naming step here - an unnamed villager prompted for a name through
 * chat buttons before it could be logged, and the name was then put on the villager. It
 * existed because a written book could only show text, so a name was the only way to tell
 * two librarians apart. The ledger screen shows each villager's coordinates and its live
 * demand, which does that job better, and renaming somebody else's villager as a side effect
 * of writing it down was never a good thing to do. A villager is now recorded under its name
 * tag if it has one, and its career otherwise.
 */
public final class QuestBinding {

    private QuestBinding() {
    }

    /** Writes this villager into the ledger, or refreshes it if it is already there. */
    public static void offer(EntityPlayer player, EntityVillager villager, ItemStack log,
                             ITomeKnowledge tomes) {
        // Only the slot cap silences this. A villager with an empty slot still owes a bounty
        // for its next one, and that errand is exactly the kind worth writing down.
        if (!SlotRequests.canUnlockMore(tomes)) {
            Feedback.notify(player, Feedback.INFO,
                    "This villager has every slot it can have - nothing left to save for");
            return;
        }

        // Demands are rolled lazily, so a villager nobody has offered a book to has none
        // yet. Rolling it here lets the ledger be the first thing you show a villager.
        SlotRequests.ensureRequest(villager, tomes);
        if (tomes.getRequest().isEmpty()) {
            Feedback.notify(player, Feedback.INFO, "This villager is not asking for anything");
            return;
        }

        write(player, villager, log, tomes);
    }

    /** Writes or refreshes the entry, and reports what happened. */
    public static void write(EntityPlayer player, EntityVillager villager, ItemStack log,
                             ITomeKnowledge tomes) {
        ITextComponent name = villager.getDisplayName();
        QuestEntry entry = new QuestEntry(villager.getUniqueID(), storedName(villager),
                (int) Math.floor(villager.posX), (int) Math.floor(villager.posY),
                (int) Math.floor(villager.posZ), villager.world.provider.getDimension(),
                SlotRequests.unlockedSlots(tomes) + 1);
        entry.setWanted(tomes.getRequest());

        boolean existed = QuestLog.find(log, villager.getUniqueID()) != null;
        if (!QuestLog.record(log, entry)) {
            Feedback.notify(player, Feedback.WARN, "Your ledger is full at "
                    + QuestLog.capacity() + " villagers - remove one first");
            return;
        }

        ITextComponent message = new TextComponentString(existed ? "Updated " : "Wrote down ");
        message.appendSibling(plain(name));
        message.appendSibling(new TextComponentString(" in your ledger - "
                + QuestLog.countLabel(QuestLog.count(log))));
        Feedback.notify(player, Feedback.GOOD, message);
    }

    /**
     * Strips the hover and click events {@code getDisplayName} attaches - an entity hover
     * carries the villager's UUID and type, which is noise in a ledger and in chat alike.
     */
    /**
     * The villager's name as a ledger stores it: its name tag if it has one, its career
     * otherwise, as JSON so a career name stays translated on every client.
     */
    public static String storedName(EntityVillager villager) {
        return ITextComponent.Serializer.componentToJson(plain(villager.getDisplayName()));
    }

    private static ITextComponent plain(ITextComponent name) {
        ITextComponent copy = name.createCopy();
        copy.getStyle().setHoverEvent(null).setClickEvent(null).setInsertion(null);
        return copy;
    }

    /**
     * Prints the whole log to chat, one line per villager, each with its own remove button.
     *
     * <p>Kept as {@code /villagertomes log} for anyone who wants it in chat; the ledger
     * screen is the main way to read and prune it now.
     */
    public static void listEntries(EntityPlayer player, ItemStack log) {
        List<QuestEntry> entries = QuestLog.readEntries(log);
        if (entries.isEmpty()) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "Your ledger is empty. Sneak-click a villager with it to write one down."));
            return;
        }

        player.sendMessage(new TextComponentString(prefix() + TextFormatting.AQUA
                + "Ledger " + TextFormatting.WHITE + QuestLog.countLabel(entries.size())));

        for (QuestEntry entry : entries) {
            int outstanding = 0;
            for (BountyItem line : entry.getWanted()) {
                if (!line.isSatisfied()) {
                    outstanding++;
                }
            }

            ITextComponent row = new TextComponentString("  ");
            row.appendSibling(entry.getNameComponent());
            row.appendSibling(new TextComponentString(TextFormatting.DARK_GRAY + "  "
                    + entry.getX() + " " + entry.getY() + " " + entry.getZ()
                    + TextFormatting.GRAY + "  slot " + entry.getSlot() + ", "
                    + (outstanding == 0
                            ? "paid off"
                            : outstanding + " item" + (outstanding == 1 ? "" : "s") + " left")
                    + " "));
            row.appendSibling(button("[Remove]", TextFormatting.RED,
                    "/villagertomes unlog " + entry.getVillager(),
                    "Delete this villager from your ledger"));
            player.sendMessage(row);
        }
    }

    /** Drops one villager's entry by id, which is what the chat [Remove] button calls. */
    public static String unlog(EntityPlayer player, String rawId) {
        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return "You are not carrying a ledger.";
        }
        UUID id;
        try {
            id = UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException e) {
            return "That is not a villager id.";
        }
        if (!QuestLog.remove(log, id)) {
            return "That villager is not in your ledger.";
        }
        return "Removed. " + QuestLog.countLabel(QuestLog.count(log)) + ".";
    }

    /**
     * Clears a villager's entry from a carried ledger once it has nothing left to sell.
     *
     * <p>Silent when there is nothing to clear, since most deliveries are made by somebody
     * not carrying a ledger at all.
     */
    public static void clearIfLogged(EntityPlayer player, EntityVillager villager) {
        ItemStack log = findLog(player);
        if (log.isEmpty()) {
            return;
        }
        if (QuestLog.remove(log, villager.getUniqueID())) {
            Feedback.notify(player, Feedback.INFO, "Crossed off your ledger");
        }
    }

    /** Refreshes a carried ledger's entry for this villager, so progress shows straight away. */
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
     * The player's ledger - hands first, then the rest of the inventory.
     *
     * <p>An old written-book log found along the way is converted in place, so every caller
     * gets a ledger without having to know the old form ever existed. Returns the live stack,
     * since everything written afterwards goes through it.
     */
    public static ItemStack findLog(EntityPlayer player) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if (QuestLog.isLegacyLog(stack)) {
                player.setHeldItem(hand, QuestLog.convertLegacy(stack));
                stack = player.getHeldItem(hand);
            }
            if (QuestLog.isLog(stack)) {
                return stack;
            }
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (QuestLog.isLegacyLog(stack)) {
                player.inventory.setInventorySlotContents(i, QuestLog.convertLegacy(stack));
                stack = player.inventory.getStackInSlot(i);
            }
            if (QuestLog.isLog(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Puts a newly made ledger where the player can reach it, and hands back the live stack.
     *
     * <p>{@code addItemStackToInventory} stores a <em>copy</em> and empties the stack it was
     * given, so the stack that actually lives in the inventory is looked up again afterwards.
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

    private static ITextComponent button(String label, TextFormatting color, String command,
                                         String tooltip) {
        ITextComponent button = new TextComponentString(color + label);
        button.getStyle()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString(tooltip)));
        return button;
    }

    private static String prefix() {
        return TextFormatting.DARK_AQUA + "[Villager Tomes] " + TextFormatting.RESET;
    }
}
