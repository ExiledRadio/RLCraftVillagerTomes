package com.exiledradio.rlcraftvillagertomes.ward;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Villager Ward. Sneak-click a villager to use it up and protect that villager for good.
 * The work is done in {@link WardEvents}; the item itself only carries the tooltip.
 */
public class ItemVillagerWard extends Item {

    public ItemVillagerWard() {
        setMaxStackSize(16);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + "Sneak-click a villager to ward it for good.");
        tooltip.add(TextFormatting.GRAY + "Switch what can hurt it from its trade screen.");
    }
}
