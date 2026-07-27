package com.etherealfeast.slot;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Custom slot for the "厨典" BaiWei item.
 * This slot is rendered in the player inventory screen via overlay.
 *
 * Unlike a standard inventory slot, this slot:
 * - Cannot be manually removed (bound identity)
 * - Items placed here persist through death (go into damaged state)
 * - Validates only BaiWei items can be placed
 */
public class CookbookSlot extends SlotItemHandler {

    public static final ResourceLocation EMPTY_SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("etherealfeast", "gui/empty_cookbook_slot");

    public CookbookSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof BaiWeiItem;
    }

    @Override
    public boolean mayPickup(Player player) {
        // Check if identity is bound - if so, cannot remove
        if (player != null) {
            PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
            if (data.isBound()) {
                player.sendSystemMessage(
                        Component.translatable("message.etherealfeast.cannot_remove")
                                .withStyle(ChatFormatting.RED));
                return false;
            }
        }
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
