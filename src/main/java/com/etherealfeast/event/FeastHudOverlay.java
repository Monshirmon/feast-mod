package com.etherealfeast.event;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.taste.TasteType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        int barX = sw / 2 - BAR_WIDTH / 2, barY = sh - 44 - BAR_HEIGHT;

        guiGraphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF332244);

        int cur = data.getFeastExp() - data.getCurrentLevelThreshold();
        int max = Math.max(1, data.getExpForNextLevel() - data.getCurrentLevelThreshold());
        if (data.getExpForNextLevel() < 0) { cur = 1; max = 1; }
        int fw = Math.min((int)((float)cur / max * BAR_WIDTH), BAR_WIDTH);
        guiGraphics.fill(barX, barY, barX + fw, barY + BAR_HEIGHT, 0xFF9955FF);
        guiGraphics.fill(barX, barY, barX + fw, barY + 2, 0xFFBB88FF);

        guiGraphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY, 0xFF555555);
        guiGraphics.fill(barX - 1, barY + BAR_HEIGHT, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF555555);
        guiGraphics.fill(barX - 1, barY, barX, barY + BAR_HEIGHT, 0xFF555555);
        guiGraphics.fill(barX + BAR_WIDTH, barY, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT, 0xFF555555);

        String lt = "Lv." + data.getFeastLevel() + " " + formatExp(data.getFeastExp());
        guiGraphics.drawString(mc.font, lt, barX - mc.font.width(lt) - 4, barY - 1, 0xCCAAFF);
        String rt = data.getExpForNextLevel() > 0 ? String.valueOf(data.getExpForNextLevel()) : "MAX";
        guiGraphics.drawString(mc.font, rt, barX + BAR_WIDTH + 4, barY - 1, 0xCCAAFF);

        // Taste preferences display (below the exp bar)
        int tasteY = barY + BAR_HEIGHT + 3;
        String likesStr = data.getTasteLikes().stream()
                .map(s -> { TasteType tt = TasteType.fromId(s); return tt != null ? tt.chineseName : s; })
                .collect(Collectors.joining(" "));
        String dislikesStr = data.getTasteDislikes().stream()
                .map(s -> { TasteType tt = TasteType.fromId(s); return tt != null ? tt.chineseName : s; })
                .collect(Collectors.joining(" "));
        if (!likesStr.isEmpty()) {
            guiGraphics.drawString(mc.font, "§a♥ " + likesStr, barX, tasteY, 0x55FF55);
        }
        if (!dislikesStr.isEmpty()) {
            int dw = mc.font.width("§a♥ " + likesStr + "  ");
            guiGraphics.drawString(mc.font, "§c✗ " + dislikesStr, barX + Math.max(0, dw), tasteY, 0xFF5555);
        }
    }

    private static void renderFloatingTexts(GuiGraphics guiGraphics, float pt) {
        floatingTexts.removeIf(t -> t.update(pt));
        int cx = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int by = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 40;
        for (FloatingExpText t : floatingTexts) {
            int a = (int)(t.alpha * 255), color = (a << 24) | 0xCCAAFF;
            guiGraphics.drawString(Minecraft.getInstance().font, t.text,
                    cx - Minecraft.getInstance().font.width(t.text) / 2, by - t.yOffset, color);
        }
    }

    public static void addFloatingText(String text) { floatingTexts.add(new FloatingExpText(text)); }

    private static String formatExp(int exp) { return exp >= 10000 ? String.format("%d,%03d", exp/1000, exp%1000) : String.valueOf(exp); }

    public static class FloatingExpText {
        final String text; float lifetime, alpha = 1f; int yOffset;
        static final float DURATION = 2f;
        FloatingExpText(String t) { text = t; }
        boolean update(float pt) { lifetime += pt * 0.05f; if (lifetime > DURATION) return true; alpha = 1f - lifetime / DURATION; yOffset = (int)(lifetime * 15); return false; }
    }
}
