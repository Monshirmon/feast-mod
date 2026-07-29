package com.etherealfeast.event;

import com.etherealfeast.EtherealFeast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class FeastHudOverlay {

    public static final List<FloatingExpText> floatingTexts = new ArrayList<>();

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(
                (net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) -> {
                    event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR,
                            ResourceLocation.fromNamespaceAndPath(EtherealFeast.MOD_ID, "feast_floating_exp"),
                            (guiGraphics, delta) -> renderFloatingTexts(guiGraphics, delta.getGameTimeDeltaTicks()));
                });
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

    public static class FloatingExpText {
        final String text; float lifetime, alpha = 1f; int yOffset;
        static final float DURATION = 2f;
        FloatingExpText(String t) { text = t; }
        boolean update(float pt) { lifetime += pt * 0.05f; if (lifetime > DURATION) return true; alpha = 1f - lifetime / DURATION; yOffset = (int)(lifetime * 15); return false; }
    }
}
