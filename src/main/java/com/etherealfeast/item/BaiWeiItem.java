package com.etherealfeast.item;

import com.etherealfeast.capability.PlayerIdentityData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BaiWeiItem extends Item {
    public enum IdentityType {
        SOLO("solo", ChatFormatting.AQUA),
        TEAM("team", ChatFormatting.GOLD);

        public final String id;
        public final ChatFormatting color;

        IdentityType(String id, ChatFormatting color) {
            this.id = id;
            this.color = color;
        }
    }

    private final IdentityType identityType;

    public BaiWeiItem(IdentityType identityType, Properties properties) {
        super(properties);
        this.identityType = identityType;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PlayerIdentityData.IdentityData data = PlayerIdentityData.get(serverPlayer);

            if (data.isBound()) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.ethereal_feast.identity_bound",
                                Component.translatable("identity.ethereal_feast." + data.getIdentityType().id)
                                        .withStyle(data.getIdentityType().color)));
                return InteractionResultHolder.fail(stack);
            }

            data.bindIdentity(identityType);
            data.setItemDamaged(false);
            PlayerIdentityData.sync(serverPlayer);

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

        tooltip.add(Component.translatable("identity.ethereal_feast." + identityType.id)
                .withStyle(identityType.color));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("message.ethereal_feast.cannot_remove")
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
