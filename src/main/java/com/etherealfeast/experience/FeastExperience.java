package com.etherealfeast.experience;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Experience system for Ethereal Feast.
 * Levels: 1→2→3→4→5→6
 * Thresholds: 0 / 3000 / 8000 / 14000 / 20000 / 30000
 *
 * Solo: double EXP when killing alone, halved when near other players
 * Team: EXP shared among teammates within 32 blocks
 */
public class FeastExperience {

    /** EXP thresholds for each level (index 0 = Lv1 threshold, etc.) */
    public static final int[] EXP_FOR_LEVEL = {0, 3000, 8000, 14000, 20000, 30000};

    public static int getMaxLevel() {
        return EXP_FOR_LEVEL.length;
    }

    /**
     * Grant experience to a player. Handles solo/team logic.
     *
     * @param player The player receiving EXP
     * @param baseAmount Base EXP amount before modifiers
     */
    public static void grantExp(Player player, int baseAmount) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerIdentityData.IdentityData data = PlayerIdentityData.get(serverPlayer);

            if (!data.isBound()) return;

            int amount = baseAmount;

            if (data.getIdentityType() == BaiWeiItem.IdentityType.SOLO) {
                List<ServerPlayer> nearbyPlayers = player.getServer().getPlayerList().getPlayers().stream()
                        .filter(p -> p != player && p.distanceTo(player) < 64.0)
                        .toList();

                if (nearbyPlayers.isEmpty()) {
                    amount = baseAmount * 2;
                } else {
                    amount = baseAmount / 2;
                }

                // Lv4+ perk: 1.2x EXP
                if (data.getFeastLevel() >= 4) {
                    amount = (int)(amount * 1.2);
                }
            } else {
                // Team: share EXP among teammates within 32 blocks
                List<ServerPlayer> teammates = player.getServer().getPlayerList().getPlayers().stream()
                        .filter(p -> {
                            if (p == player) return false;
                            if (p.distanceTo(player) > 32.0) return false;
                            PlayerIdentityData.IdentityData otherData = PlayerIdentityData.get(p);
                            return otherData.isBound() && otherData.getIdentityType() == BaiWeiItem.IdentityType.TEAM;
                        })
                        .toList();

                int totalRecipients = 1 + teammates.size();
                int sharedAmount = baseAmount / totalRecipients;

                data.addExp(sharedAmount);
                notifyExp(serverPlayer, sharedAmount);
                checkLevelUp(serverPlayer);

                for (ServerPlayer teammate : teammates) {
                    PlayerIdentityData.IdentityData teammateData = PlayerIdentityData.get(teammate);
                    teammateData.addExp(sharedAmount);
                    notifyExp(teammate, sharedAmount);
                    checkLevelUp(teammate);
                    PlayerIdentityData.sync(teammate);
                }

                PlayerIdentityData.sync(serverPlayer);
                return;
            }

            int oldLevel = data.getFeastLevel();
            data.addExp(amount);
            notifyExp(serverPlayer, amount);
            checkLevelUp(serverPlayer);

            if (data.getFeastLevel() > oldLevel) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("experience.ethereal_feast.level_up", data.getFeastLevel()));
            }

            PlayerIdentityData.sync(serverPlayer);
        }
    }

    private static void notifyExp(ServerPlayer player, int amount) {
        player.sendSystemMessage(
                Component.translatable("experience.ethereal_feast.gained", amount));
    }

    private static void checkLevelUp(ServerPlayer player) {
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
        int oldLevel = data.getFeastLevel();

        while (data.getFeastLevel() < EXP_FOR_LEVEL.length
                && data.getFeastExp() >= EXP_FOR_LEVEL[data.getFeastLevel()]) {
            data.setLevel(data.getFeastLevel() + 1);
        }

        if (data.getFeastLevel() > oldLevel) {
            player.sendSystemMessage(
                    Component.translatable("experience.ethereal_feast.level_up", data.getFeastLevel()));

            if (data.getFeastLevel() >= EXP_FOR_LEVEL.length) {
                // Max level reached - title notification
                player.sendSystemMessage(Component.literal(
                        "§6========================================\n" +
                        "§e    🍖 异界食缘 · 伪神已临 🍖\n" +
                        "§7    你已达到厨典最高境界 Lv." + data.getFeastLevel() + "\n" +
                        "§7    后续内容敬请期待更新...\n" +
                        "§6========================================"
                ));
            }
        }
    }
}
