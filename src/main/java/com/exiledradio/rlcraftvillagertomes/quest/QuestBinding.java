package com.exiledradio.rlcraftvillagertomes.quest;

import com.exiledradio.rlcraftvillagertomes.ModConfig;
import com.exiledradio.rlcraftvillagertomes.bounty.SlotRequests;
import com.exiledradio.rlcraftvillagertomes.capability.CapabilityTomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.HashMap;
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
        if (SlotRequests.openSlots(tomes) > 0 || tomes.getRequest().isEmpty()) {
            player.sendMessage(new TextComponentString(prefix() + TextFormatting.GRAY
                    + "This villager is not waiting on anything - nothing to write down."));
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

        ItemStack log = heldLog(player);
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

        player.sendMessage(new TextComponentString(prefix() + TextFormatting.GREEN
                + (existed ? "Updated " : "Wrote down ") + name + "."
                + TextFormatting.GRAY + " " + QuestLog.count(log) + "/"
                + ModConfig.QUEST_LOG_CAPACITY + " logged."));
    }

    /**
     * Clears a villager's entry from a held log once its slot is paid for.
     *
     * <p>Silent when there is nothing to clear, since most deliveries are made by somebody
     * not carrying a log at all.
     */
    public static void clearIfLogged(EntityPlayer player, EntityVillager villager) {
        ItemStack log = heldLog(player);
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
        ItemStack log = heldLog(player);
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

    /** The quest log in either hand, or an empty stack. */
    public static ItemStack heldLog(EntityPlayer player) {
        for (ItemStack stack : player.getHeldEquipment()) {
            if (QuestLog.isLog(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
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
