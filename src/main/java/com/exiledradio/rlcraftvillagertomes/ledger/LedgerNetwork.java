package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.RLCraftVillagerTomes;
import com.exiledradio.rlcraftvillagertomes.ward.WardEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/**
 * The two packets the ledger needs.
 *
 * <p>{@link ViewMessage} goes to the client and opens (or refreshes) the ledger screen.
 * {@link ActionMessage} comes back when a button is pressed. Neither handler names a client
 * class: the view handler goes through {@link RLCraftVillagerTomes#proxy}, so a dedicated
 * server never has to load anything client-only to verify this file.
 */
public final class LedgerNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(RLCraftVillagerTomes.MODID);

    private LedgerNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(ViewHandler.class, ViewMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(ActionHandler.class, ActionMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(WardStatusHandler.class, WardStatusMessage.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(WardCycleHandler.class, WardCycleMessage.class, 3, Side.SERVER);
        CHANNEL.registerMessage(NoteHandler.class, NoteMessage.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(NoticeHandler.class, NoticeMessage.class, 5, Side.CLIENT);
    }

    // ------------------------------------------------------------------ ward

    /** Which villager the open trade screen belongs to, and its ward. Mode 0 means none. */
    public static final class WardStatusMessage implements IMessage {
        public int entityId;
        public int mode;

        public WardStatusMessage() {
        }

        public WardStatusMessage(int entityId, int mode) {
            this.entityId = entityId;
            this.mode = mode;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            entityId = buf.readInt();
            mode = buf.readByte();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(entityId);
            buf.writeByte(mode);
        }
    }

    public static final class WardStatusHandler implements IMessageHandler<WardStatusMessage, IMessage> {
        @Override
        public IMessage onMessage(WardStatusMessage message, MessageContext ctx) {
            RLCraftVillagerTomes.proxy.showWard(message.entityId, message.mode);
            return null;
        }
    }

    /** The ward button on a trade screen was clicked. */
    public static final class WardCycleMessage implements IMessage {
        public int entityId;

        public WardCycleMessage() {
        }

        public WardCycleMessage(int entityId) {
            this.entityId = entityId;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            entityId = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(entityId);
        }
    }

    public static final class WardCycleHandler implements IMessageHandler<WardCycleMessage, IMessage> {
        @Override
        public IMessage onMessage(final WardCycleMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    WardEvents.cycle(player, message.entityId);
                }
            });
            return null;
        }
    }

    // ------------------------------------------------------------------ note

    /** A villager's bounty scroll, sent in answer to a sneak-click with an empty hand. */
    public static final class NoteMessage implements IMessage {
        public NBTTagCompound note;

        public NoteMessage() {
        }

        public NoteMessage(NBTTagCompound note) {
            this.note = note;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            note = ByteBufUtils.readTag(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeTag(buf, note);
        }
    }

    public static final class NoteHandler implements IMessageHandler<NoteMessage, IMessage> {
        @Override
        public IMessage onMessage(NoteMessage message, MessageContext ctx) {
            RLCraftVillagerTomes.proxy.showNote(message.note);
            return null;
        }
    }

    /** One short line for the player - see {@code Feedback}. The text is component JSON. */
    public static final class NoticeMessage implements IMessage {
        public byte tone;
        public String json;

        public NoticeMessage() {
        }

        public NoticeMessage(byte tone, String json) {
            this.tone = tone;
            this.json = json;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            tone = buf.readByte();
            json = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(tone);
            ByteBufUtils.writeUTF8String(buf, json);
        }
    }

    public static final class NoticeHandler implements IMessageHandler<NoticeMessage, IMessage> {
        @Override
        public IMessage onMessage(NoticeMessage message, MessageContext ctx) {
            RLCraftVillagerTomes.proxy.showNotice(message.tone, message.json);
            return null;
        }
    }

    // ------------------------------------------------------------------ view

    public static final class ViewMessage implements IMessage {
        public NBTTagCompound view;

        public ViewMessage() {
        }

        public ViewMessage(NBTTagCompound view) {
            this.view = view;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            view = ByteBufUtils.readTag(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeTag(buf, view);
        }
    }

    public static final class ViewHandler implements IMessageHandler<ViewMessage, IMessage> {
        @Override
        public IMessage onMessage(final ViewMessage message, MessageContext ctx) {
            RLCraftVillagerTomes.proxy.showLedger(message.view);
            return null;
        }
    }

    // ---------------------------------------------------------------- action

    public static final byte ACTION_DELIVER = 0;
    public static final byte ACTION_REMOVE = 1;
    /** Renames the villager. The new name rides in the {@code where} tag, under "name". */
    public static final byte ACTION_RENAME = 2;
    /** Puts the villager on the player's Antique Atlas. */
    public static final byte ACTION_MARK = 3;
    /** Stars or unstars a villager - favorites go to the front of the ledger. */
    public static final byte ACTION_FAVORITE = 4;
    /** Bookmarks the page holding this villager; the same villager again clears it. */
    public static final byte ACTION_BOOKMARK = 5;
    /** Switches a loaded, warded villager to its next ward mode. */
    public static final byte ACTION_WARD = 6;

    public static final class ActionMessage implements IMessage {
        public NBTTagCompound where;
        public byte action;
        public UUID villager;

        public ActionMessage() {
        }

        public ActionMessage(NBTTagCompound where, byte action, UUID villager) {
            this.where = where;
            this.action = action;
            this.villager = villager;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            where = ByteBufUtils.readTag(buf);
            action = buf.readByte();
            villager = new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeTag(buf, where);
            buf.writeByte(action);
            buf.writeLong(villager.getMostSignificantBits());
            buf.writeLong(villager.getLeastSignificantBits());
        }
    }

    public static final class ActionHandler implements IMessageHandler<ActionMessage, IMessage> {
        @Override
        public IMessage onMessage(final ActionMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LedgerActions.handle(player, message);
                }
            });
            return null;
        }
    }
}
