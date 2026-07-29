package com.etherealfeast.item;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.taste.TasteType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BaiWeiItem extends Item implements ICurioItem {
    public enum IdentityType {
        SOLO("solo", ChatFormatting.AQUA),
        TEAM("team", ChatFormatting.GOLD);

        public final String id;
        public final ChatFormatting color;

        IdentityType(String id, ChatFormatting color) { this.id = id; this.color = color; }
    }

    private final IdentityType identityType;

    public BaiWeiItem(IdentityType identityType, Properties properties) {
        super(properties);
        this.identityType = identityType;
    }

    public IdentityType getIdentityType() { return identityType; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {

            // Check if the Curios cookbook slot already has a bound item
            if (PlayerIdentityData.isBound(serverPlayer)) {
                BaiWeiItem.IdentityType existing = PlayerIdentityData.getIdentityType(serverPlayer);
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.ethereal_feast.identity_bound",
                                Component.translatable("identity.ethereal_feast." + existing.id)
                                        .withStyle(existing.color)));
                return InteractionResultHolder.fail(stack);
            }

            // Equip into Curios cookbook slot
            PlayerIdentityData.equipCookbook(serverPlayer, stack);
            // Initialize binding data
            PlayerIdentityData.bindIdentity(serverPlayer, identityType);

            serverPlayer.sendSystemMessage(
                    Component.translatable("message.ethereal_feast.bind_success",
                            Component.translatable("identity.ethereal_feast." + identityType.id)
                                    .withStyle(identityType.color)));

            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Read CustomData from the stack
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();

        if (tag.getBoolean("IsBound")) {
            // Show identity type
            String typeId = tag.getString("IdentityType");
            BaiWeiItem.IdentityType type = "team".equals(typeId) ? IdentityType.TEAM : IdentityType.SOLO;
            String name = type == IdentityType.SOLO ? "百味·独酌" : "百味·共飨";
            tooltip.add(Component.literal(name).withStyle(type.color));

            // Exp and level
            int lv = Math.max(1, tag.getInt("FeastLevel"));
            int exp = tag.getInt("FeastExp");
            int[] thresholds = {0, 3000, 8000, 14000, 20000, 30000};
            int cur = exp - (lv > 0 ? thresholds[lv - 1] : 0);
            int max = lv < thresholds.length ? thresholds[lv] : thresholds[thresholds.length - 1];
            int nextThreshold = lv < thresholds.length ? thresholds[lv] : -1;
            String expStr;
            if (nextThreshold > 0) {
                int curExp = exp - (lv > 0 ? thresholds[lv - 1] : 0);
                int needExp = nextThreshold - (lv > 0 ? thresholds[lv - 1] : 0);
                expStr = curExp + "/" + needExp;
            } else {
                expStr = "MAX";
            }
            tooltip.add(Component.literal("§7厨典 Lv." + lv + "  §d" + expStr).withStyle(ChatFormatting.LIGHT_PURPLE));

            // Taste likes
            String likesStr = tag.getString("TasteLikes");
            if (!likesStr.isEmpty()) {
                String likes = Arrays.stream(likesStr.split(","))
                        .map(s -> { TasteType tt = TasteType.fromId(s); return tt != null ? tt.chineseName : s; })
                        .collect(Collectors.joining(" "));
                tooltip.add(Component.literal("§a♥ 喜好: " + likes));
            }

            // Taste dislikes
            String dislikesStr = tag.getString("TasteDislikes");
            if (!dislikesStr.isEmpty()) {
                String dislikes = Arrays.stream(dislikesStr.split(","))
                        .map(s -> { TasteType tt = TasteType.fromId(s); return tt != null ? tt.chineseName : s; })
                        .collect(Collectors.joining(" "));
                tooltip.add(Component.literal("§c✗ 厌恶: " + dislikes));
            }

            // Damaged status
            if (tag.getBoolean("IsDamaged")) {
                tooltip.add(Component.literal("§c⚡ 百味已破损").withStyle(ChatFormatting.RED));
            }
        } else {
            // Not yet bound
            tooltip.add(Component.translatable("identity.ethereal_feast." + identityType.id).withStyle(identityType.color));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("message.ethereal_feast.cannot_remove").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        return false;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // Auto-bind identity when manually placed into Curios slot (e.g. via GUI)
        if (slotContext.entity() instanceof ServerPlayer serverPlayer
                && !PlayerIdentityData.isBound(serverPlayer)) {
            PlayerIdentityData.bindIdentity(serverPlayer, identityType);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.ethereal_feast.bind_success",
                            Component.translatable("identity.ethereal_feast." + identityType.id)
                                    .withStyle(identityType.color)));
        }
    }
}
