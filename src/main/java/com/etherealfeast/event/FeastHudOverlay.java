package com.etherealfeast.event;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.capability.PlayerIdentityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class FeastHudOverlay {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    public static final List<FloatingExpText> floatingTexts = new ArrayList<>();

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) -> {
                    event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR,
                            ResourceLocation.fromNamespaceAndPath(EtherealFeast.MOD_ID, "feast_exp_bar"),
                            (guiGraphics, delta) -> renderExpBar(guiGraphics));

                    event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR,
                            ResourceLocation.fromNamespaceAndPath(EtherealFeast.MOD_ID, "feast_floating_exp"),
                            (guiGraphics, delta) -> renderFloatingTexts(guiGraphics, delta.getGameTimeDeltaTicks()));
                });
    }

    private static void renderExpBar(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(mc.player);
        if (!data.isBound()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int barX = screenWidth / 2 - BAR_WIDTH / 2;
        int barY = screenHeight - 32 - 4 - BAR_HEIGHT;

        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF332244);

        int currentExp = data.getFeastExp() - data.getCurrentLevelThreshold();
        int maxExp = Math.max(1, data.getExpForNextLevel() - data.getCurrentLevelThreshold());
        if (data.getExpForNextLevel() < 0) { currentExp = 1; maxExp = 1; }
        int filledWidth = Math.min((int)((float)currentExp / maxExp * BAR_WIDTH), BAR_WIDTH);

        guiGraphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, 0xFF9955FF);
        guiGraphics.fill(barX, barY, barX + filledWidth, barY + 2, 0xFFBB88FF);

        guiGraphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY, 0xFF555555);
        guiGraphics.fill(barX - 1, barY + BAR_HEIGHT, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF555555);
        guiGraphics.fill(barX - 1, barY, barX, barY + BAR_HEIGHT, 0xFF555555);
        guiGraphics.fill(barX + BAR_WIDTH, barY, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT, 0xFF555555);

        String leftText = "Lv." + data.getFeastLevel();
        guiGraphics.drawString(mc.font, leftText, barX - mc.font.width(leftText) - 4, barY - 1, 0xCCAAFF);

        String rightText = data.getExpForNextLevel() > 0 ? String.valueOf(data.getExpForNextLevel()) : "MAX";
        guiGraphics.drawString(mc.font, rightText, barX + BAR_WIDTH + 4, barY - 1, 0xCCAAFF);
    }

    private static void renderFloatingTexts(GuiGraphics guiGraphics, float partialTick) {
        floatingTexts.removeIf(t -> t.update(partialTick));

        int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int baseY = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 48;

        for (FloatingExpText text : floatingTexts) {
            int alpha = (int)(text.alpha * 255);
            int color = (alpha << 24) | 0xCCAAFF;
            int x = centerX - Minecraft.getInstance().font.width(text.text) / 2;
            int y = baseY - text.yOffset;
            guiGraphics.drawString(Minecraft.getInstance().font, text.text, x, y, color);
        }
    }

    public static void addFloatingText(String text) {
        floatingTexts.add(new FloatingExpText(text));
    }

    public static class FloatingExpText {
        final String text;
        float lifetime;
        float alpha = 1.0f;
        int yOffset;
        static final float DURATION = 2.0f;

        FloatingExpText(String text) { this.text = text; }

        boolean update(float partialTick) {
            lifetime += partialTick * 0.05f;
            if (lifetime > DURATION) return true;
            alpha = 1.0f - lifetime / DURATION;
            yOffset = (int)(lifetime * 30);
            return false;
        }
    }
}
