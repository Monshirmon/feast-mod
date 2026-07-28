package com.etherealfeast.event;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.experience.FeastExperience;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.taste.TasteSystem;
import com.etherealfeast.taste.TasteType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

public class ModEvents {

    private static final ResourceLocation LONELY_DEBUFF_ID =
            ResourceLocation.fromNamespaceAndPath("ethereal_feast", "lonely_attack_debuff");

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
                boolean hasTeammate = server.getPlayerList().getPlayers().stream()
                        .anyMatch(p -> p != player && p.distanceTo(player) <= 32.0
                                && PlayerIdentityData.isBound(p)
                                && PlayerIdentityData.getIdentityType(p) == BaiWeiItem.IdentityType.TEAM);

                AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null) {
                    if (!hasTeammate) {
                        // No teammates nearby - apply 30% attack reduction via attribute modifier
                        if (attackAttr.getModifier(LONELY_DEBUFF_ID) == null) {
                            attackAttr.addTransientModifier(
                                    new AttributeModifier(LONELY_DEBUFF_ID, -0.3,
                                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                        }
                    } else {
                        // Has teammates - remove debuff
                        attackAttr.removeModifier(LONELY_DEBUFF_ID);
                    }
                }
            }
            // Solo buffs handled by item attributes
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getFoodProperties(null) == null) return;

        List<TasteType.TasteValue> tastes = TasteSystem.getInstance().getTastes(stack.getItem());
        if (tastes.isEmpty()) return;

        // Build taste display line: "甜++ 鲜+++ 咸+"
        StringBuilder sb = new StringBuilder();
        for (TasteType.TasteValue tv : tastes) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tv.type().chineseName);
            sb.append(switch (tv.strength()) {
                case STRONG -> "+++";
                case MEDIUM -> "++";
                case WEAK -> "+";
            });
        }

        event.getToolTip().add(
                Component.literal(sb.toString()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
