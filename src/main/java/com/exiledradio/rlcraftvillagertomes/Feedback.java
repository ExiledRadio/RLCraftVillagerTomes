package com.exiledradio.rlcraftvillagertomes;

import com.exiledradio.rlcraftvillagertomes.bounty.BountyNote;
import com.exiledradio.rlcraftvillagertomes.capability.ITomeKnowledge;
import com.exiledradio.rlcraftvillagertomes.ledger.LedgerNetwork;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Everything the mod tells a player, kept out of chat.
 *
 * <ul>
 *   <li>{@link #scroll} - the villager's bounty scroll, with an optional banner line on top.
 *       For anything that changes what a villager wants: a delivery, an unlock, being told it
 *       has no room. The answer and the new state arrive together.</li>
 *   <li>{@link #notify} - one short line. Shown inside the ledger or scroll if one is open,
 *       otherwise on the action bar above the hotbar.</li>
 * </ul>
 *
 * <p>Messages carry a tone rather than color codes: the same line is drawn light on the
 * action bar and dark on parchment, and chat's bright greens and yellows are unreadable on
 * paper. Command replies are the exception and still go to chat, where commands answer.
 */
public final class Feedback {

    public static final byte GOOD = 0;
    public static final byte INFO = 1;
    public static final byte WARN = 2;
    public static final byte BAD = 3;

    private Feedback() {
    }

    public static void notify(EntityPlayer player, byte tone, String text) {
        notify(player, tone, new TextComponentString(text));
    }

    /**
     * A component rather than a string where it holds translatable parts (enchantment and
     * item names), so the client translates them into its own language.
     */
    public static void notify(EntityPlayer player, byte tone, ITextComponent text) {
        if (player instanceof EntityPlayerMP) {
            LedgerNetwork.CHANNEL.sendTo(new LedgerNetwork.NoticeMessage(tone,
                    ITextComponent.Serializer.componentToJson(text)), (EntityPlayerMP) player);
        }
    }

    /** Unrolls the villager's bounty scroll, optionally with a banner line on top. */
    public static void scroll(EntityPlayer player, EntityVillager villager, ITomeKnowledge tomes,
                              byte tone, String banner) {
        if (player instanceof EntityPlayerMP) {
            BountyNote note = BountyNote.of(villager, tomes);
            if (banner != null) {
                note.banner = banner;
                note.bannerTone = tone;
            }
            LedgerNetwork.CHANNEL.sendTo(new LedgerNetwork.NoteMessage(note.toTag()),
                    (EntityPlayerMP) player);
        }
    }
}
