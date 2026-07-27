package com.etherealfeast.mixin;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add the Cookbook (厨典) slot to the player inventory screen.
 * Renders a visual slot above the player model preview area,
 * similar to Curios/accessory slots.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {

    @Unique
    private static final int SLOT_X = 26;
    @Unique
    private static final int SLOT_Y = 8;
    @Unique
    private static final int SLOT_SIZE = 18;

    public InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderCookbookSlot(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int slotX = x + SLOT_X;
        int slotY = y + SLOT_Y;

        // Draw a simple bordered slot
        // Outer dark border
        guiGraphics.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, 0xFF373737);
        guiGraphics.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, 0xFF373737);
        guiGraphics.fill(slotX - 1, slotY, slotX, slotY + SLOT_SIZE, 0xFF373737);
        guiGraphics.fill(slotX + SLOT_SIZE, slotY, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE, 0xFF373737);

        // Inner dark background
        guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF8B8B8B);

        // Get the player's current bound BaiWei item
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(this.minecraft.player);
        if (data.isBound()) {
            BaiWeiItem.IdentityType type = data.getIdentityType();
            ItemStack displayStack;

            if (data.isDamaged()) {
                displayStack = type == BaiWeiItem.IdentityType.SOLO
                        ? new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_DUZHUO_DAMAGED.get())
                        : new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_GONGXIANG_DAMAGED.get());
            } else {
                displayStack = type == BaiWeiItem.IdentityType.SOLO
                        ? new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_DUZHUO.get())
                        : new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_GONGXIANG.get());
            }

            displayStack.setCount(1);

            // Render the item in the slot
            guiGraphics.renderItem(displayStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, displayStack, slotX + 1, slotY + 1);

            // Tooltip on hover
            if (isHoveringOverSlot(slotX, slotY, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, displayStack, mouseX, mouseY);
            }

            // Render level text below the slot
            String levelText = "Lv." + data.getFeastLevel();
            int textColor = type == BaiWeiItem.IdentityType.SOLO ? 0x55CCFF : 0xFFCC44;
            guiGraphics.drawString(this.font, levelText,
                    slotX + (SLOT_SIZE - this.font.width(levelText)) / 2,
                    slotY + SLOT_SIZE + 2,
                    textColor);
        }
    }

    @Unique
    private boolean isHoveringOverSlot(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }
}
