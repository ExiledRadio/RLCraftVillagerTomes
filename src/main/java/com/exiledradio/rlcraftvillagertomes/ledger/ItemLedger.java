package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The villager ledger: a record of what each logged villager still wants.
 *
 * <p>Made by sneak-clicking a villager with a book and quill. Sneak-clicking a villager with
 * the ledger writes that villager down; right-clicking anywhere else opens it. Placed on a
 * ledger lectern it becomes a delivery desk - see {@link BlockLedgerLectern}.
 *
 * <p>The screen is opened by the server rather than here on the client. The server refreshes
 * every entry whose villager is loaded before it sends the page over, so what you read is the
 * villager's live demand, not the snapshot from when you last stood next to it.
 */
public class ItemLedger extends Item {

    public ItemLedger() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            LedgerActions.openHeld((EntityPlayerMP) player, hand);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * Crafting never uses a ledger up: it stays in the grid, every entry intact, the way a
     * milk bucket leaves its bucket. The lectern recipe asks for one so a lectern is only
     * made by someone who has a ledger to put on it, and a list of 64 villagers is far too
     * much work to lose to a crafting table.
     */
    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        ItemStack kept = stack.copy();
        kept.setCount(1);
        return kept;
    }

    /**
     * Keeps this copy linked and its tooltip count in step with the shared list, so a villager
     * logged in one copy shows up in the others' tooltips too. Once a second is plenty, and
     * {@link QuestLog#sync} only touches the stack when the count actually changed.
     */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isRemote && world.getTotalWorldTime() % 20L == slot % 20) {
            QuestLog.sync(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        int count = QuestLog.cachedCount(stack);
        tooltip.add(TextFormatting.GRAY + (count == 0
                ? "Sneak-click a villager to write it down"
                : count + (count == 1 ? " villager" : " villagers") + " logged"));
        int id = QuestLog.idOf(stack);
        if (id > 0) {
            tooltip.add(TextFormatting.DARK_GRAY + "Ledger #" + id
                    + " - copies share one list");
        }
        tooltip.add(TextFormatting.DARK_GRAY + "Craft with a book to make a copy");
    }
}
