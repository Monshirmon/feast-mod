package com.etherealfeast.mixin;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
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

    @Unique private static final int SLOT_X = 26, SLOT_Y = 8, SLOT_SIZE = 18;

    public InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void renderCookbookSlot(GuiGraphics g, float pt, int mx, int my, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        int xo = (this.width - this.imageWidth) / 2, yo = (this.height - this.imageHeight) / 2;
        int sx = xo + SLOT_X, sy = yo + SLOT_Y;

        // Draw slot border
        g.fill(sx - 1, sy - 1, sx + SLOT_SIZE + 1, sy, 0xFF373737);
        g.fill(sx - 1, sy + SLOT_SIZE, sx + SLOT_SIZE + 1, sy + SLOT_SIZE + 1, 0xFF373737);
        g.fill(sx - 1, sy, sx, sy + SLOT_SIZE, 0xFF373737);
        g.fill(sx + SLOT_SIZE, sy, sx + SLOT_SIZE + 1, sy + SLOT_SIZE, 0xFF373737);
        g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF8B8B8B);

        // Show accessory slot data from synced IdentityData
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(this.minecraft.player);
        if (data.isBound()) {
            ItemStack stack = data.getIdentityType() == BaiWeiItem.IdentityType.SOLO
                    ? new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_DUZHUO.get())
                    : new ItemStack(com.etherealfeast.registry.ModItems.BAIWEI_GONGXIANG.get());
            stack.setCount(1);
            g.renderItem(stack, sx + 1, sy + 1);
            g.renderItemDecorations(this.font, stack, sx + 1, sy + 1);
            if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE)
                g.renderTooltip(this.font, stack, mx, my);

            String lt = "Lv." + data.getFeastLevel();
            if (data.isDamaged()) lt += " ⚡";
            int c = data.getIdentityType() == BaiWeiItem.IdentityType.SOLO ? 0x55CCFF : 0xFFCC44;
            g.drawString(this.font, lt, sx + (SLOT_SIZE - this.font.width(lt)) / 2, sy + SLOT_SIZE + 2, c);
        }
    }
}
