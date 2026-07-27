package com.etherealfeast.invasion;

import com.etherealfeast.EtherealFeast;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the invasion event cycle.
 * - 30 minute timer (36000 ticks) between invasions
 * - 5 stages cycling
 * - Vote system for teams (standard vs strengthened)
 * - Progress bar synced to clients
 */
public class InvasionManager {

    /** 30 minutes in ticks */
    public static final int INVASION_INTERVAL = 36000;

    /** Warning at 30 seconds before invasion */
    public static final int WARNING_TICKS = 600;

    private static InvasionManager instance;

    private MinecraftServer server;
    private int currentStage = 0;
    private int timerTicks = 0;
    private boolean active = false;
    private boolean strengthened = false;

    /** Players who have voted this cycle (UUID -> vote: true=strengthened) */
    private final Map<UUID, Boolean> votes = new HashMap<>();

    /** Whether warning has been sent this cycle */
    private boolean warningSent = false;

    /** List of players who have offered food this cycle */
    private int offeringCount = 0;
    private static final int OFFERINGS_TO_TRIGGER = 5;

    public static InvasionManager get(MinecraftServer server) {
        if (instance == null) {
            instance = new InvasionManager();
        }
        instance.server = server;
        return instance;
    }

    public static InvasionManager getInstance() {
        return instance;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer srv = event.getServer();
        if (srv.getTickCount() % 20 != 0) return; // Every second

        timerTicks++;
        updateInvasionState(srv);
    }

    private void updateInvasionState(MinecraftServer server) {
        if (active) {
            // Invasion is ongoing - handled by spawn logic
            return;
        }

        int remaining = INVASION_INTERVAL - timerTicks;

        // Warning at 30 seconds
        if (remaining <= WARNING_TICKS && !warningSent) {
            warningSent = true;
            broadcastWarning(server, remaining);
        }

        // Auto-trigger when timer runs out
        if (timerTicks >= INVASION_INTERVAL) {
            triggerInvasion(server, false);
        }
    }

    private void broadcastWarning(MinecraftServer server, int remainingTicks) {
        int seconds = remainingTicks / 20;
        String msg = "§c⚠ 异界能量正在汇聚… " + seconds + "秒后降临！第" + (currentStage + 1) + "阶段：" +
                InvasionStage.fromId(currentStage).color + InvasionStage.fromId(currentStage).chineseName;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(msg));
        }
    }

    /**
     * Player offers food to accelerate the invasion.
     */
    public void playerOffering(Player player) {
        if (active) {
            player.sendSystemMessage(Component.literal("§c入侵已在进行中！"));
            return;
        }

        offeringCount++;
        int remaining = Math.max(0, INVASION_INTERVAL - timerTicks);
        int minutes = remaining / 1200;
        int seconds = (remaining % 1200) / 20;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(
                    "§e" + player.getName().getString() + " 献祭了一份料理！(" +
                    offeringCount + "/" + OFFERINGS_TO_TRIGGER + ") §7剩余 " + minutes + ":" + String.format("%02d", seconds)));
        }

        if (offeringCount >= OFFERINGS_TO_TRIGGER) {
            triggerInvasion(server, true);
        }
    }

    /**
     * Player votes for strengthened invasion (team line only).
     */
    public void playerVote(Player player, boolean voteStrengthened) {
        if (active) return;
        votes.put(player.getUUID(), voteStrengthened);

        player.sendSystemMessage(Component.literal(
                "§a你投票了 " + (voteStrengthened ? "§c强化入侵" : "§7标准入侵")));
    }

    /**
     * Trigger the invasion event.
     */
    public void triggerInvasion(MinecraftServer server, boolean manual) {
        active = true;
        InvasionStage stage = InvasionStage.fromId(currentStage);

        String triggerMsg = manual ?
                "§c献祭完成！异界之门提前开启！" :
                "§c异界能量满溢！第" + (currentStage + 1) + "阶段入侵降临！";

        // Tally votes
        int yesVotes = 0;
        int noVotes = 0;
        for (boolean v : votes.values()) {
            if (v) yesVotes++;
            else noVotes++;
        }
        // Default to strengthened on tie or no votes
        strengthened = yesVotes > noVotes;

        String modeMsg = strengthened ?
                "§c⚔ 强化入侵模式！怪物血量+30%，伤害+20%，掉落翻倍！" :
                "§7标准入侵模式";

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§6========================================\n" +
                    "§e  " + stage.color + "异界入侵 · " + stage.chineseName + "\n" +
                    triggerMsg + "\n" +
                    modeMsg + "\n" +
                    "§6========================================"
            ));
        }

        // Reset for next cycle
        timerTicks = 0;
        offeringCount = 0;
        votes.clear();
        warningSent = false;
    }

    /**
     * End current invasion stage.
     */
    public void endInvasion() {
        active = false;
        currentStage = (currentStage + 1) % InvasionStage.values().length;
        strengthened = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§a入侵结束！下一个阶段：" +
                    InvasionStage.fromId(currentStage).color +
                    InvasionStage.fromId(currentStage).chineseName));
        }
    }

    public int getCurrentStageId() { return currentStage; }
    public InvasionStage getCurrentStage() { return InvasionStage.fromId(currentStage); }
    public boolean isActive() { return active; }
    public boolean isStrengthened() { return strengthened; }
    public int getTimerTicks() { return timerTicks; }
    public int getMaxTicks() { return INVASION_INTERVAL; }
    public int getOfferingCount() { return offeringCount; }
    public int getMaxOfferings() { return OFFERINGS_TO_TRIGGER; }
}
