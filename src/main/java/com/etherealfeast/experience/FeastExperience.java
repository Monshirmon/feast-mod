package com.etherealfeast.experience;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.network.ExpGainPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Experience system. Lv1→6, thresholds: 0/3k/8k/14k/20k/30k
 */
public class FeastExperience {

    public static final int[] EXP_FOR_LEVEL = {0, 3000, 8000, 14000, 20000, 30000};

    public static int getMaxLevel() { return EXP_FOR_LEVEL.length; }

    public static void grantExp(Player player, int baseAmount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!PlayerIdentityData.isBound(serverPlayer)) return;

        int amount = baseAmount;
        if (PlayerIdentityData.isDamaged(serverPlayer)) amount /= 5;

        if (PlayerIdentityData.getIdentityType(serverPlayer) == BaiWeiItem.IdentityType.SOLO) {
            List<ServerPlayer> nearby = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p != serverPlayer && p.distanceTo(serverPlayer) < 64.0).toList();

            amount = nearby.isEmpty() ? amount * 2 : amount / 2;
            if (PlayerIdentityData.getFeastLevel(serverPlayer) >= 4) amount = (int)(amount * 1.2);

            addAndNotify(serverPlayer, amount);
        } else {
            List<ServerPlayer> teammates = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p != serverPlayer && p.distanceTo(serverPlayer) <= 32.0
                            && PlayerIdentityData.isBound(p)
                            && PlayerIdentityData.getIdentityType(p) == BaiWeiItem.IdentityType.TEAM)
                    .toList();

            int shared = amount / (1 + teammates.size());
            addAndNotify(serverPlayer, shared);

            for (ServerPlayer t : teammates) {
                addAndNotify(t, shared);
            }
        }
    }

    private static void addAndNotify(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        int oldLevel = PlayerIdentityData.getFeastLevel(player);
        PlayerIdentityData.addExp(player, amount);
        int newLevel = PlayerIdentityData.getFeastLevel(player);

        PacketDistributor.sendToPlayer(player, new ExpGainPacket(amount));

        if (newLevel > oldLevel) {
            player.sendSystemMessage(
                    Component.translatable("experience.ethereal_feast.level_up", newLevel));
            if (newLevel >= EXP_FOR_LEVEL.length) {
                player.sendSystemMessage(Component.literal(
                        "§6========================================\n" +
                        "§e    🍖 异界食缘 · 伪神已临 🍖\n" +
                        "§7    你已达到厨典最高境界 Lv." + newLevel + "\n" +
                        "§7    后续内容敬请期待更新...\n" +
                        "§6========================================"));
            }
        }
    }
}
