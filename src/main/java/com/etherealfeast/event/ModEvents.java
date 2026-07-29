package com.etherealfeast.event;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.experience.FeastExperience;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.taste.TasteSystem;
import com.etherealfeast.taste.TasteType;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModEvents {

    private static final ResourceLocation LONELY_DEBUFF_ID =
            ResourceLocation.fromNamespaceAndPath("ethereal_feast", "lonely_attack_debuff");

    // ==================== Prevent BaiWei from existing as dropped item ====================

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity
                && itemEntity.getItem().getItem() instanceof BaiWeiItem) {
            event.setCanceled(true);
        }
    }

    // ==================== XP penalty while damaged ====================

    @SubscribeEvent
    public void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PlayerIdentityData.isBound(player)
                && PlayerIdentityData.isDamaged(player)) {
            event.setAmount(event.getAmount() / 2);
        }
    }

    // ==================== Totem-like death prevention ====================

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // Totem effect: bound, undamaged BaiWei saves the player once
        if (event.getEntity() instanceof ServerPlayer player
                && PlayerIdentityData.isBound(player)
                && !PlayerIdentityData.isDamaged(player)) {

            event.setCanceled(true);
            player.setHealth(1.0f);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            player.level().broadcastEntityEvent(player, (byte) 35); // totem animation

            PlayerIdentityData.setDamaged(player, true);
            PlayerIdentityData.sync(player);

            List<String> tastes = PlayerIdentityData.getRepairTastes(player);
            String tasteNames = tastes.stream()
                    .map(id -> { TasteType t = TasteType.fromId(id); return t != null ? t.chineseName : id; })
                    .collect(Collectors.joining("§f、§e"));
            player.sendSystemMessage(Component.literal(
                    "§6========================================\n" +
                    "§c  ⚡ 你的百味替你承受了致命一击！\n" +
                    "§7  但它已经破损了…\n" +
                    "§e  重生的饥饿感驱使你渴望：" + tasteNames + "\n" +
                    "§7  吃下这些口味的食物来修复它 (" + PlayerIdentityData.getRepairProgress(player) + "/" + tastes.size() + ")\n" +
                    "§6========================================"));
        }

        // Grant exp for killing mobs (regardless of BaiWei state)
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int exp = event.getEntity() instanceof Monster ? 50
                    : event.getEntity().getMaxHealth() > 20f ? 30 : 10;
            if (exp > 0) FeastExperience.grantExp(player, exp);
        }
    }

    // ==================== Eating speed modulation ====================

    private static final ResourceLocation ADV_ROOT =
            ResourceLocation.fromNamespaceAndPath("ethereal_feast", "root");
    private static final ResourceLocation ADV_DISLIKED_5 =
            ResourceLocation.fromNamespaceAndPath("ethereal_feast", "eat_disliked_5");

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!PlayerIdentityData.isBound(player)) return;

        ItemStack stack = event.getItem();
        if (stack.getFoodProperties(player) == null) return;

        List<TasteType.TasteValue> tastes = TasteSystem.getInstance().getTastes(stack.getItem());
        if (tastes.isEmpty()) return;

        List<String> likes = PlayerIdentityData.getTasteLikes(player);
        List<String> dislikes = PlayerIdentityData.getTasteDislikes(player);

        // Check if food has BOTH liked and disliked tastes → weird, no speed change
        boolean hasLiked = false, hasDisliked = false;
        for (TasteType.TasteValue tv : tastes) {
            if (likes.contains(tv.type().id)) hasLiked = true;
            if (dislikes.contains(tv.type().id)) hasDisliked = true;
        }
        if (hasLiked && hasDisliked) return; // weird food, no speed change

        int baseDuration = event.getDuration();

        if (hasLiked) {
            int stacks = PlayerIdentityData.getPleasureStacks(player);
            float speedMult = 1.0f - (stacks * 0.05f);
            event.setDuration(Math.max(1, (int)(baseDuration * speedMult)));
        }

        if (hasDisliked) {
            int stacks = PlayerIdentityData.getDisgustStacks(player);
            if (stacks > 0) {
                // 3x → 2.5x decreasing as stacks increase
                float slowMult = 3.0f - (stacks - 1) * 0.125f;
                event.setDuration((int)(baseDuration * slowMult));
            }
        }
    }

    // ==================== Food repair + taste buff/debuff + exp tracking ====================

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!PlayerIdentityData.isBound(player)) return;

        ItemStack stack = event.getItem();
        if (stack.getFoodProperties(player) == null) return;

        List<TasteType.TasteValue> foodTastes = TasteSystem.getInstance().getTastes(stack.getItem());
        if (foodTastes.isEmpty()) return;

        List<String> likes = PlayerIdentityData.getTasteLikes(player);
        List<String> dislikes = PlayerIdentityData.getTasteDislikes(player);

        // Check taste categories
        boolean hasLiked = false, hasDisliked = false;
        for (TasteType.TasteValue tv : foodTastes) {
            if (likes.contains(tv.type().id)) hasLiked = true;
            if (dislikes.contains(tv.type().id)) hasDisliked = true;
        }
        boolean isWeird = hasLiked && hasDisliked;

        // Taste buff/debuff stacks + messages
        if (isWeird) {
            player.displayClientMessage(Component.literal(
                    "§7味道怪怪的.."), true);
            FeastExperience.grantExp(player, 2);
        } else if (hasLiked) {
            PlayerIdentityData.addPleasureStack(player);
            PlayerIdentityData.reduceDisgustStacks(player);
            player.displayClientMessage(Component.literal(
                    "§a这无疑是美味的"), true);
            FeastExperience.grantExp(player, 5);
        } else if (hasDisliked) {
            PlayerIdentityData.addDisgustStack(player);
            PlayerIdentityData.addDislikedEaten(player);
            player.displayClientMessage(Component.literal(
                    "§cyue~哪来的史？"), true);
            FeastExperience.grantExp(player, 1);

            // Grant epic advancement after eating 5 disliked foods
            if (PlayerIdentityData.getDislikedEaten(player) >= 5) {
                AdvancementHolder adv = player.getServer().getAdvancements()
                        .get(ADV_DISLIKED_5);
                if (adv != null) {
                    player.getAdvancements().award(adv, "impossible");
                }
            }
        } else {
            FeastExperience.grantExp(player, 2);
        }

        PlayerIdentityData.sync(player);

        // Repair tracking (damaged BaiWei only)
        if (PlayerIdentityData.isDamaged(player)) {
            List<String> required = PlayerIdentityData.getRepairTastes(player);
            for (TasteType.TasteValue tv : foodTastes) {
                if (required.contains(tv.type().id)) {
                    boolean repaired = PlayerIdentityData.addRepairProgress(player, tv.type().id);
                    PlayerIdentityData.sync(player);

                    if (repaired) {
                        player.sendSystemMessage(Component.literal(
                                "§a========================================\n" +
                                "§6  ✨ 你的百味已经修复！恢复如初！\n" +
                                "§a========================================"));
                    } else {
                        int prog = PlayerIdentityData.getRepairProgress(player);
                        List<String> updated = PlayerIdentityData.getRepairTastes(player);
                        String remaining = updated.stream()
                                .map(id -> { TasteType t = TasteType.fromId(id); return t != null ? t.chineseName : id; })
                                .collect(Collectors.joining("§f、§e"));
                        player.sendSystemMessage(Component.literal(
                                "§e你吃下了 §6" + TasteType.fromId(tv.type().id).chineseName +
                                " §e口味的食物！修复进度 §6" + prog + "/" + (prog + updated.size()) +
                                "§e  剩余渴望：§6" + remaining));
                    }
                    break;
                }
            }
        }
    }

    // ==================== Team debuff ====================

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!PlayerIdentityData.isBound(player)) continue;

            // Hunger II while BaiWei is damaged
            if (PlayerIdentityData.isDamaged(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1,
                        false, false, true));
            }

            if (PlayerIdentityData.getIdentityType(player) == BaiWeiItem.IdentityType.TEAM) {
                boolean hasTeammate = server.getPlayerList().getPlayers().stream()
                        .anyMatch(p -> p != player && p.distanceTo(player) <= 32.0
                                && PlayerIdentityData.isBound(p)
                                && PlayerIdentityData.getIdentityType(p) == BaiWeiItem.IdentityType.TEAM);

                AttributeInstance attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackAttr != null) {
                    if (!hasTeammate) {
                        if (attackAttr.getModifier(LONELY_DEBUFF_ID) == null) {
                            attackAttr.addTransientModifier(
                                    new AttributeModifier(LONELY_DEBUFF_ID, -0.3,
                                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                        }
                    } else {
                        attackAttr.removeModifier(LONELY_DEBUFF_ID);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // Show repair progress on damaged BaiWei
        if (stack.getItem() instanceof BaiWeiItem) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.getBoolean("IsDamaged")) {
                String repairRaw = tag.getString("RepairTastes");
                if (!repairRaw.isEmpty()) {
                    List<String> tastes = Arrays.asList(repairRaw.split(","));
                    int progress = tag.getInt("RepairProgress");
                    String tasteNames = tastes.stream()
                            .map(id -> { TasteType t = TasteType.fromId(id); return t != null ? "§6" + t.chineseName : id; })
                            .collect(Collectors.joining("§7、"));
                    event.getToolTip().add(Component.literal(
                            "§c破损 §7- 渴望：" + tasteNames + " §7(§e" + progress + "/" + (progress + tastes.size()) + "§7)")
                            .withStyle(ChatFormatting.DARK_RED));
                }
            }
        }

        // Food taste tooltip
        if (stack.getFoodProperties(null) == null) return;

        List<TasteType.TasteValue> tastes = TasteSystem.getInstance().getTastes(stack.getItem());
        if (tastes.isEmpty()) return;

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
