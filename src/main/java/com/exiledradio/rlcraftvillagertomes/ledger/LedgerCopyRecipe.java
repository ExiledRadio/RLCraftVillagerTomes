package com.exiledradio.rlcraftvillagertomes.ledger;

import com.exiledradio.rlcraftvillagertomes.quest.QuestLog;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;

/**
 * Ledger + book = two ledgers sharing one list.
 *
 * <p>The ledger is never used up by crafting ({@link ItemLedger#getContainerItem}), so it
 * stays in the grid and the result is the second copy. The result carries the same ledger
 * number, which is the whole trick: the entries live in the world save under that number
 * ({@code LedgerData}), so both copies always show the same villagers.
 *
 * <p>A plain shapeless recipe underneath so JEI shows it without any plugin code; only the
 * result is special, a copy of the ledger in the grid rather than a blank one.
 */
public class LedgerCopyRecipe extends ShapelessRecipes {

    public LedgerCopyRecipe() {
        super("", new ItemStack(ModContent.LEDGER),
                NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(ModContent.LEDGER),
                        Ingredient.fromItem(Items.BOOK)));
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (QuestLog.isLog(stack)) {
                return QuestLog.copyOf(stack);
            }
        }
        return super.getCraftingResult(inv);
    }
}
