package com.etherealfeast.mixin;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        guiGraphics.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, 0xFF373737);
        guiGraphics.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, 0xFF373737);
        guiGraphics.fill(slotX - 1, slotY, slotX, slotY + SLOT_SIZE, 0xFF373737);
        guiGraphics.fill(slotX + SLOT_SIZE, slotY, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE, 0xFF373737);
        guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xFF8B8B8B);

        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(this.minecraft.player);
        if (data.isBound()) {
            BaiWeiItem.IdentityType type = data.getIdentityType();
            ItemStack displayStack = type == BaiWeiItem.IdentityType.SOLO
                    ? new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_DUZHUO.get())
                    : new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_GONGXIANG.get());

            displayStack.setCount(1);

            guiGraphics.renderItem(displayStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, displayStack, slotX + 1, slotY + 1);

            if (isHoveringOverSlot(slotX, slotY, mouseX, mouseY)) {
                guiGraphics.renderTooltip(this.font, displayStack, mouseX, mouseY);
            }

            String levelText = "Lv." + data.getFeastLevel();
            if (data.isDamaged()) {
                levelText += " ⚡"; // Lightning bolt to indicate damaged
            }
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
