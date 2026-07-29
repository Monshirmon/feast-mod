package com.etherealfeast.command;

import com.etherealfeast.capability.PlayerIdentityData;
import com.etherealfeast.invasion.InvasionManager;
import com.etherealfeast.invasion.InvasionStage;
import com.etherealfeast.item.BaiWeiItem;
import com.etherealfeast.taste.TasteType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FeastCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("feast");
        // Admin commands (open for testing)
        root.then(Commands.literal("status").executes(ctx -> showStatus(ctx.getSource().getPlayerOrException())));
        root.then(Commands.literal("level")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 6))
                .executes(ctx -> setLevel(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "level")))));
        root.then(Commands.literal("exp")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                .executes(ctx -> addExp(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))));
        root.then(Commands.literal("unbind").executes(ctx -> unbind(ctx.getSource().getPlayerOrException())));
        // Player commands (no permission required)
        root.then(Commands.literal("taste").executes(ctx -> showTaste(ctx.getSource().getPlayerOrException())));

        var inv = Commands.literal("invasion");
        inv.then(Commands.literal("status").executes(ctx -> invasionStatus(ctx.getSource())));
        inv.then(Commands.literal("end").executes(ctx -> invasionEnd(ctx.getSource())));
        inv.then(Commands.literal("offer").executes(ctx -> invasionOffer(ctx.getSource())));
        root.then(inv);
        dispatcher.register(root);
    }

    private static int showStatus(ServerPlayer player) {
        if (!PlayerIdentityData.isBound(player)) {
            player.sendSystemMessage(Component.literal("§c未绑定")); return 0;
        }
        String tn = PlayerIdentityData.getIdentityType(player) == BaiWeiItem.IdentityType.SOLO ? "独行之道" : "共飨之道";
        String cn = PlayerIdentityData.getIdentityType(player) == BaiWeiItem.IdentityType.SOLO ? "§b" : "§6";
        int ne = PlayerIdentityData.getExpForNextLevel(player);
        String ns = ne > 0 ? String.valueOf(ne) : "MAX";
        player.sendSystemMessage(Component.literal(
                "§6===== 异界食缘 =====\n§7身份: " + cn + tn +
                (PlayerIdentityData.isDamaged(player) ? " §c⚡" : "") +
                "\n§7等级: §eLv." + PlayerIdentityData.getFeastLevel(player) +
                "\n§7经验: §a" + PlayerIdentityData.getFeastExp(player) + " §7/ §e" + ns));
        return 1;
    }

    private static int setLevel(ServerPlayer p, int lv) {
        if (!PlayerIdentityData.isBound(p)) {
            p.sendSystemMessage(Component.literal("§c请先绑定")); return 0;
        }
        PlayerIdentityData.setLevel(p, lv);
        PlayerIdentityData.setExp(p, PlayerIdentityData.getThresholdForLevel(lv));
        p.sendSystemMessage(Component.literal("§a已设 Lv." + lv)); return 1;
    }

    private static int addExp(ServerPlayer p, int a) {
        if (!PlayerIdentityData.isBound(p)) {
            p.sendSystemMessage(Component.literal("§c请先绑定")); return 0;
        }
        PlayerIdentityData.addExp(p, a);
        p.sendSystemMessage(Component.literal("§a+" + a + " 厨典经验 Lv." +
                PlayerIdentityData.getFeastLevel(p) + " (" + PlayerIdentityData.getFeastExp(p) + ")"));
        return 1;
    }

    private static int unbind(ServerPlayer p) {
        // Clear the Curios cookbook slot
        PlayerIdentityData.equipCookbook(p, ItemStack.EMPTY);
        PlayerIdentityData.sync(p);
        p.sendSystemMessage(Component.literal("§a已解绑")); return 1;
    }

    private static int showTaste(ServerPlayer p) {
        if (!PlayerIdentityData.isBound(p)) {
            p.sendSystemMessage(Component.literal("§c请先绑定厨典")); return 0;
        }
        List<String> likes = PlayerIdentityData.getTasteLikes(p);
        List<String> dislikes = PlayerIdentityData.getTasteDislikes(p);
        StringBuilder sb = new StringBuilder("§6===== 口味偏好 =====\n");
        sb.append("§a喜欢: ");
        for (String s : likes) {
            TasteType tt = TasteType.fromId(s);
            sb.append(tt != null ? tt.chineseName : s).append(" ");
        }
        sb.append("\n§c厌恶: ");
        for (String s : dislikes) {
            TasteType tt = TasteType.fromId(s);
            sb.append(tt != null ? tt.chineseName : s).append(" ");
        }
        p.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static InvasionManager inv(CommandSourceStack src) { return InvasionManager.get(src.getServer()); }

    private static int invasionStatus(CommandSourceStack src) {
        InvasionManager im = inv(src); InvasionStage st = im.getCurrentStage();
        int remaining = Math.max(0, im.getMaxActiveTicks() - im.getActiveTicks());
        src.sendSystemMessage(Component.literal("§6===== 入侵 =====\n§7状态: " + (im.isActive() ? "§c进行中 " + (remaining/60) + ":" + String.format("%02d", remaining%60) : "§a等待献祭") + "\n§7阶段: " + st.color + st.chineseName + "\n§7献祭进度: §e" + im.getOfferingCount() + "/" + im.getMaxOfferings() + "\n§7强化概率: §c" + im.getStrengthenChance() + "%"));
        return 1;
    }
    private static int invasionEnd(CommandSourceStack src) { inv(src).endInvasion(); return 1; }
    private static int invasionOffer(CommandSourceStack src) { if (src.getEntity() instanceof ServerPlayer p) inv(src).playerOffering(p, false); return 1; }
}
