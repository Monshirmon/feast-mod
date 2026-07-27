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

public class FeastCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("feast").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource().getPlayerOrException())));
        root.then(Commands.literal("level").then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                .executes(ctx -> setLevel(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))));
        root.then(Commands.literal("exp").then(Commands.argument("amount", IntegerArgumentType.integer(0))
                .executes(ctx -> addExp(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))));
        root.then(Commands.literal("unbind").executes(ctx -> unbind(ctx.getSource().getPlayerOrException())));

        var inv = Commands.literal("invasion");
        inv.then(Commands.literal("status").executes(ctx -> invasionStatus(ctx.getSource())));
        inv.then(Commands.literal("trigger").executes(ctx -> invasionTrigger(ctx.getSource())));
        inv.then(Commands.literal("end").executes(ctx -> invasionEnd(ctx.getSource())));
        inv.then(Commands.literal("offer").executes(ctx -> invasionOffer(ctx.getSource())));
        inv.then(Commands.literal("vote").then(Commands.literal("strengthen").executes(ctx -> invasionVote(ctx.getSource(), true)))
                .then(Commands.literal("standard").executes(ctx -> invasionVote(ctx.getSource(), false))));
        root.then(inv);
        dispatcher.register(root);
    }

    private static PlayerIdentityData.AccessorySlot slot(ServerPlayer p) { return PlayerIdentityData.getAccessory(p); }

    private static int showStatus(ServerPlayer player) {
        PlayerIdentityData.AccessorySlot s = slot(player);
        if (!s.isBound()) { player.sendSystemMessage(Component.literal("§c未绑定")); return 0; }
        String tn = s.getIdentityType() == BaiWeiItem.IdentityType.SOLO ? "独行之道" : "共飨之道";
        String cn = s.getIdentityType() == BaiWeiItem.IdentityType.SOLO ? "§b" : "§6";
        int ne = s.getExpForNextLevel();
        String ns = ne > 0 ? String.valueOf(ne) : "MAX";
        player.sendSystemMessage(Component.literal("§6===== 异界食缘 =====\n§7身份: " + cn + tn + (s.isDamaged() ? " §c⚡" : "") + "\n§7等级: §eLv." + s.getFeastLevel() + "\n§7经验: §a" + s.getFeastExp() + " §7/ §e" + ns));
        return 1;
    }

    private static int setLevel(ServerPlayer p, int lv) {
        PlayerIdentityData.AccessorySlot s = slot(p);
        if (!s.isBound()) { p.sendSystemMessage(Component.literal("§c请先绑定")); return 0; }
        s.setLevel(lv); s.setExp(PlayerIdentityData.AccessorySlot.getThresholdForLevel(lv)); PlayerIdentityData.sync(p);
        p.sendSystemMessage(Component.literal("§a已设 Lv." + lv)); return 1;
    }

    private static int addExp(ServerPlayer p, int a) {
        PlayerIdentityData.AccessorySlot s = slot(p);
        if (!s.isBound()) { p.sendSystemMessage(Component.literal("§c请先绑定")); return 0; }
        s.addExp(a); PlayerIdentityData.sync(p);
        p.sendSystemMessage(Component.literal("§a+" + a + " 厨典经验 Lv." + s.getFeastLevel() + " (" + s.getFeastExp() + ")")); return 1;
    }

    private static int unbind(ServerPlayer p) {
        slot(p).getNbt(); PlayerIdentityData.getAccessory(p).setItem(net.minecraft.world.item.ItemStack.EMPTY);
        PlayerIdentityData.sync(p);
        p.sendSystemMessage(Component.literal("§a已解绑")); return 1;
    }

    private static InvasionManager inv(CommandSourceStack src) { return InvasionManager.get(src.getServer()); }

    private static int invasionStatus(CommandSourceStack src) {
        InvasionManager im = inv(src); InvasionStage st = im.getCurrentStage();
        int r = Math.max(0, im.getMaxTicks() - im.getTimerTicks());
        src.sendSystemMessage(Component.literal("§6===== 入侵 =====\n§7状态: " + (im.isActive() ? "§c进行中" : "§a等待中") + "\n§7阶段: " + st.color + st.chineseName + "\n§7倒计时: §e" + (r/1200) + ":" + String.format("%02d", (r%1200)/20) + "\n§7献祭: §e" + im.getOfferingCount() + "/" + im.getMaxOfferings()));
        return 1;
    }
    private static int invasionTrigger(CommandSourceStack src) { inv(src).triggerInvasion(src.getServer(), true); return 1; }
    private static int invasionEnd(CommandSourceStack src) { inv(src).endInvasion(); return 1; }
    private static int invasionOffer(CommandSourceStack src) { if (src.getEntity() instanceof ServerPlayer p) inv(src).playerOffering(p); return 1; }
    private static int invasionVote(CommandSourceStack src, boolean s) { if (src.getEntity() instanceof ServerPlayer p) inv(src).playerVote(p, s); return 1; }
}
