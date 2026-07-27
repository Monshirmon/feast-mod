package com.etherealfeast.command;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.invasion.InvasionManager;
import com.etherealfeast.invasion.InvasionStage;
import com.etherealfeast.item.BaiWeiItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

public class FeastCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("feast").requires(s -> s.hasPermission(2));

        // Player commands
        root.then(Commands.literal("status")
                .executes(ctx -> showStatus(ctx.getSource().getPlayerOrException())));

        root.then(Commands.literal("level")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> setLevel(
                                ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "level")))));

        root.then(Commands.literal("exp")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> addExp(
                                ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "amount")))));

        root.then(Commands.literal("unbind")
                .executes(ctx -> unbind(ctx.getSource().getPlayerOrException())));

        // Invasion commands
        var invasion = Commands.literal("invasion");
        invasion.then(Commands.literal("status")
                .executes(ctx -> invasionStatus(ctx.getSource())));
        invasion.then(Commands.literal("trigger")
                .executes(ctx -> invasionTrigger(ctx.getSource())));
        invasion.then(Commands.literal("end")
                .executes(ctx -> invasionEnd(ctx.getSource())));
        invasion.then(Commands.literal("offer")
                .executes(ctx -> invasionOffer(ctx.getSource())));
        invasion.then(Commands.literal("vote")
                .then(Commands.literal("strengthen")
                        .executes(ctx -> invasionVote(ctx.getSource(), true)))
                .then(Commands.literal("standard")
                        .executes(ctx -> invasionVote(ctx.getSource(), false))));
        root.then(invasion);

        dispatcher.register(root);
    }

    // === Player status ===

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

    // === Invasion ===

    private static InvasionManager getInvasion(CommandSourceStack src) {
        return InvasionManager.get(src.getServer());
    }

    private static int invasionStatus(CommandSourceStack src) {
        InvasionManager inv = getInvasion(src);
        InvasionStage stage = inv.getCurrentStage();
        int remaining = Math.max(0, inv.getMaxTicks() - inv.getTimerTicks());
        int minutes = remaining / 1200;
        int seconds = (remaining % 1200) / 20;
        String state = inv.isActive() ? "§c进行中" : "§a等待中";

        src.sendSystemMessage(Component.literal(
                "§6===== 入侵状态 =====\n" +
                "§7状态: " + state + "\n" +
                "§7当前阶段: " + stage.color + stage.chineseName + " §7(" + (stage.id + 1) + "/5)\n" +
                "§7倒计时: §e" + minutes + ":" + String.format("%02d", seconds) + "\n" +
                "§7献祭进度: §e" + inv.getOfferingCount() + "/" + inv.getMaxOfferings() + "\n" +
                "§7强化模式: " + (inv.isStrengthened() ? "§c是" : "§7否")
        ));
        return 1;
    }

    private static int invasionTrigger(CommandSourceStack src) {
        getInvasion(src).triggerInvasion(src.getServer(), true);
        return 1;
    }

    private static int invasionEnd(CommandSourceStack src) {
        getInvasion(src).endInvasion();
        return 1;
    }

    private static int invasionOffer(CommandSourceStack src) {
        if (src.getEntity() instanceof ServerPlayer player) {
            getInvasion(src).playerOffering(player);
        }
        return 1;
    }

    private static int invasionVote(CommandSourceStack src, boolean strengthened) {
        if (src.getEntity() instanceof ServerPlayer player) {
            getInvasion(src).playerVote(player, strengthened);
        }
        return 1;
    }
}
