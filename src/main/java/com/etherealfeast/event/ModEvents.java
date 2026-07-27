package com.etherealfeast.event;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.experience.FeastExperience;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.recipe.BaiWeiGongXiangRecipe;
import com.etherealfeast.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

public class ModEvents {

    /**
     * Handle "百味·共飨" teammate detection after crafting.
     * Note: ItemCraftedEvent cannot be cancelled, so we just warn.
     * Actual prevention is done in the recipe's matches() method.
     */
    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack result = event.getCrafting();

            if (result.getItem() == ModItems.BAIWEI_GONGXIANG.get()) {
                int teammateCount = BaiWeiGongXiangRecipe.countNearbyTeammates(player);

                if (teammateCount == 0) {
                    player.sendSystemMessage(
                            Component.translatable("message.ethereal_feast.recipe_wrong_bond"));
                }
            }
        }
    }

    /**
     * Grant Feast EXP on mob kill.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity target = event.getEntity();

            int baseExp = calculateMobExp(target);
            if (baseExp > 0) {
                FeastExperience.grantExp(player, baseExp);
            }
        }
    }

    private int calculateMobExp(LivingEntity entity) {
        if (entity instanceof Monster) {
            return 50;
        } else if (entity.getMaxHealth() > 20.0f) {
            return 30;
        } else {
            return 10;
        }
    }

    /**
     * Status detection using ServerTickEvent.
     * Runs every 20 ticks (1 second) to check solo/team status effects.
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return; // Throttle to once per second

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
            if (!data.isBound()) continue;

            if (data.getIdentityType() == BaiWeiItem.IdentityType.TEAM) {
                handleTeamTick(player, server);
            } else {
                handleSoloTick(player, server);
            }
        }
    }

    private void handleTeamTick(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        boolean hasTeammatesNearby = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> {
                    if (p == player) return false;
                    if (p.distanceTo(player) > 32.0) return false;
                    PlayerIdentityData.IdentityData otherData = PlayerIdentityData.get(p);
                    return otherData.isBound() && otherData.getIdentityType() == BaiWeiItem.IdentityType.TEAM;
                });

        if (!hasTeammatesNearby) {
            List<Monster> nearbyMonsters = player.level().getEntitiesOfClass(
                    Monster.class,
                    player.getBoundingBox().inflate(32.0)
            );

            for (Monster monster : nearbyMonsters) {
                if (!monster.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    monster.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true));
                }
                if (!monster.hasEffect(MobEffects.REGENERATION)) {
                    monster.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, 100, 0, false, true));
                }
            }
        }
    }

    private void handleSoloTick(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        boolean hasPlayersNearby = server.getPlayerList().getPlayers().stream()
                .anyMatch(p -> p != player && p.distanceTo(player) < 64.0);

        if (!hasPlayersNearby) return;

        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);

        if (data.getFeastLevel() >= 4) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.HEALTH_BOOST, 100, 0, false, true));

            server.getPlayerList().getPlayers().stream()
                    .filter(p -> p != player && p.distanceTo(player) < 64.0)
                    .forEach(p -> {
                        p.addEffect(new MobEffectInstance(
                                MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true));
                        p.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SPEED, 100, 0, false, true));
                    });
        } else {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 40, 0, false, true));
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, 40, 0, false, true));
        }
    }
}
