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

        PlayerIdentityData.AccessorySlot slot = PlayerIdentityData.getAccessory(serverPlayer);
        if (!slot.isBound()) return;

        int amount = baseAmount;
        if (slot.isDamaged()) baseAmount /= 5;

        if (slot.getIdentityType() == BaiWeiItem.IdentityType.SOLO) {
            List<ServerPlayer> nearby = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p != serverPlayer && p.distanceTo(serverPlayer) < 64.0).toList();

            amount = nearby.isEmpty() ? baseAmount * 2 : baseAmount / 2;
            if (slot.getFeastLevel() >= 4) amount = (int)(amount * 1.2);
        } else {
            List<ServerPlayer> teammates = serverPlayer.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> p != serverPlayer && p.distanceTo(serverPlayer) <= 32.0
                            && PlayerIdentityData.getAccessory(p).isBound()
                            && PlayerIdentityData.getAccessory(p).getIdentityType() == BaiWeiItem.IdentityType.TEAM)
                    .toList();

            int shared = baseAmount / (1 + teammates.size());
            slot.addExp(shared);
            notifyExp(serverPlayer, shared);
            checkLevelUp(serverPlayer);

            for (ServerPlayer t : teammates) {
                PlayerIdentityData.AccessorySlot ts = PlayerIdentityData.getAccessory(t);
                ts.addExp(shared);
                notifyExp(t, shared);
                checkLevelUp(t);
                PlayerIdentityData.sync(t);
            }
            PlayerIdentityData.sync(serverPlayer);
            return;
        }

        slot.addExp(amount);
        notifyExp(serverPlayer, amount);
        checkLevelUp(serverPlayer);
        PlayerIdentityData.sync(serverPlayer);
    }

    private static void notifyExp(ServerPlayer player, int amount) {
        PacketDistributor.sendToPlayer(player, new ExpGainPacket(amount));
    }

    private static void checkLevelUp(ServerPlayer player) {
        PlayerIdentityData.AccessorySlot slot = PlayerIdentityData.getAccessory(player);
        int old = slot.getFeastLevel();
        while (slot.getFeastLevel() < EXP_FOR_LEVEL.length && slot.getFeastExp() >= EXP_FOR_LEVEL[slot.getFeastLevel()])
            slot.setLevel(slot.getFeastLevel() + 1);
        if (slot.getFeastLevel() > old) {
            player.sendSystemMessage(
                    Component.translatable("experience.ethereal_feast.level_up", slot.getFeastLevel()));
            if (slot.getFeastLevel() >= EXP_FOR_LEVEL.length) {
                player.sendSystemMessage(Component.literal(
                        "§6========================================\n" +
                        "§e    🍖 异界食缘 · 伪神已临 🍖\n" +
                        "§7    你已达到厨典最高境界 Lv." + slot.getFeastLevel() + "\n" +
                        "§7    后续内容敬请期待更新...\n" +
                        "§6========================================"));
            }
        }
    }
}
