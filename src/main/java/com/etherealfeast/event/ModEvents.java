package com.etherealfeast.event;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.experience.FeastExperience;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.recipe.BaiWeiGongXiangRecipe;
import com.etherealfeast.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class ModEvents {

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getCrafting().getItem() == ModItems.BAIWEI_GONGXIANG.get()
                && BaiWeiGongXiangRecipe.countNearbyTeammates(player) == 0) {
            player.sendSystemMessage(Component.translatable("message.ethereal_feast.recipe_wrong_bond"));
        }
    }

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PlayerIdentityData.isBound(player)
                && PlayerIdentityData.isDamaged(player)) {
            event.setAmount(event.getAmount() / 2);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int exp = event.getEntity() instanceof Monster ? 50
                    : event.getEntity().getMaxHealth() > 20f ? 30 : 10;
            if (exp > 0) FeastExperience.grantExp(player, exp);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!PlayerIdentityData.isBound(player)) continue;

            if (PlayerIdentityData.getIdentityType(player) == BaiWeiItem.IdentityType.TEAM) {
                // Team lonely check - monster courage handled by attributes on accessory item
                server.getPlayerList().getPlayers().stream()
                        .anyMatch(p -> p != player && p.distanceTo(player) <= 32.0
                                && PlayerIdentityData.isBound(p)
                                && PlayerIdentityData.getIdentityType(p) == BaiWeiItem.IdentityType.TEAM);
            }
            // Solo buffs handled by item attributes
        }
    }
}
