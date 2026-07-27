package com.etherealfeast.command;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.item.BaiWeiItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FeastCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("feast")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(ctx -> showStatus(ctx.getSource().getPlayerOrException()))
                )
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> setLevel(
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level"))
                                )
                        )
                )
                .then(Commands.literal("exp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> addExp(
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "amount"))
                                )
                        )
                )
                .then(Commands.literal("unbind")
                        .executes(ctx -> unbind(ctx.getSource().getPlayerOrException()))
                )
        );
    }

    private static int showStatus(ServerPlayer player) {
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
        if (!data.isBound()) {
            player.sendSystemMessage(Component.literal("§c未绑定任何身份。"));
            return 0;
        }

        String typeName = data.getIdentityType() == BaiWeiItem.IdentityType.SOLO ? "独行之道" : "共飨之道";
        String colorName = data.getIdentityType() == BaiWeiItem.IdentityType.SOLO ? "§b" : "§6";
        int nextExp = data.getExpForNextLevel();
        String nextExpStr = nextExp > 0 ? String.valueOf(nextExp) : "MAX";
        String damaged = data.isDamaged() ? " §c⚡破损" : "";

        player.sendSystemMessage(Component.literal(
                "§6===== 异界食缘 状态 =====\n" +
                "§7身份: " + colorName + typeName + damaged + "\n" +
                "§7等级: §eLv." + data.getFeastLevel() + "\n" +
                "§7经验: §a" + data.getFeastExp() + " §7/ §e" + nextExpStr
        ));
        return 1;
    }

    private static int setLevel(ServerPlayer player, int level) {
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
        if (!data.isBound()) {
            player.sendSystemMessage(Component.literal("§c请先绑定身份。"));
            return 0;
        }
        data.setLevel(level);
        data.setExp(PlayerIdentityData.IdentityData.getThresholdForLevel(level));
        PlayerIdentityData.sync(player);
        player.sendSystemMessage(Component.literal("§a厨典等级已设为 Lv." + level));
        return 1;
    }

    private static int addExp(ServerPlayer player, int amount) {
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
        if (!data.isBound()) {
            player.sendSystemMessage(Component.literal("§c请先绑定身份。"));
            return 0;
        }
        data.addExp(amount);
        PlayerIdentityData.sync(player);
        player.sendSystemMessage(Component.literal("§a已获得 " + amount + " 厨典经验。当前 Lv." + data.getFeastLevel() + " (" + data.getFeastExp() + " EXP)"));
        return 1;
    }

    private static int unbind(ServerPlayer player) {
        PlayerIdentityData.IdentityData data = PlayerIdentityData.get(player);
        if (!data.isBound()) {
            player.sendSystemMessage(Component.literal("§c未绑定任何身份。"));
            return 0;
        }
        data.deserializeNBT(new net.minecraft.nbt.CompoundTag());
        PlayerIdentityData.sync(player);
        player.sendSystemMessage(Component.literal("§a已解除身份绑定。"));
        return 1;
    }
}
