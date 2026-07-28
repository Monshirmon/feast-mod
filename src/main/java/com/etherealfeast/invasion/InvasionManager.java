package com.etherealfeast.invasion;

import com.etherealfeast.entity.InvasionMonster;
import com.etherealfeast.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public class InvasionManager {

    public static final int INVASION_INTERVAL = 1800;   // 30 min in seconds
    public static final int WARNING_SECONDS = 30;
    private static final int VOTE_DURATION = 20;         // 20 seconds voting window before trigger
    private static final int INVASION_DURATION = 300;    // 5 min
    private static final int SPAWN_INTERVAL = 10;        // 10 seconds
    private static final int MAX_MOBS_NEAR_PLAYER = 5;
    private static final int SPAWN_MIN_RADIUS = 15;
    private static final int SPAWN_MAX_RADIUS = 25;
    private static final int OFFERINGS_TO_TRIGGER = 5;

    private static InvasionManager instance;

    private MinecraftServer server;
    private int currentStage = 0;
    private int timerTicks = 0;
    private int activeTicks = 0;
    private int spawnCooldown = 0;
    private int voteTicks = 0;
    private boolean active = false;
    private boolean voting = false;
    private boolean strengthened = false;

    private final Map<UUID, Boolean> votes = new HashMap<>();
    private boolean warningSent = false;
    private int offeringCount = 0;

    /** Initialize singleton (call from mod constructor) */
    public static InvasionManager init() {
        if (instance == null) instance = new InvasionManager();
        return instance;
    }

    public static InvasionManager get(MinecraftServer server) {
        if (instance == null) instance = new InvasionManager();
        instance.server = server;
        return instance;
    }

    public static InvasionManager getInstance() { return instance; }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer srv = event.getServer();
        if (this.server != srv) this.server = srv;
        if (srv.getTickCount() % 20 != 0) return;

        if (active) {
            activeTicks++;
            updateActiveInvasion(srv);
        } else if (voting) {
            voteTicks++;
            updateVoting(srv);
        } else {
            timerTicks++;
            updateIdleState(srv);
        }
    }

    // ==================== Idle countdown ====================

    private void updateIdleState(MinecraftServer server) {
        int remaining = INVASION_INTERVAL - timerTicks;

        if (remaining <= WARNING_SECONDS && !warningSent) {
            warningSent = true;
            broadcastWarning(server, remaining);
        }

        // At VOTE_DURATION seconds before trigger, start voting
        if (remaining <= VOTE_DURATION && !voting) {
            startVoting(server);
        }

        if (timerTicks >= INVASION_INTERVAL) {
            triggerInvasion(server, false);
        }
    }

    // ==================== Voting ====================

    private void startVoting(MinecraftServer server) {
        voting = true;
        voteTicks = 0;
        votes.clear();
        InvasionStage stage = InvasionStage.fromId(currentStage);

        Component header = Component.literal(
                "§6========================================\n" +
                "§e  " + stage.color + "异界入侵 · " + stage.chineseName + " §7投票开始！\n" +
                "§7  距离入侵降临还有 " + VOTE_DURATION + " 秒\n" +
                "§6========================================");

        Component voteStrengthen = Component.literal("[✔ 强化入侵]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/feast invasion vote strengthen"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("点击投票 §c强化入侵§f — 怪物更强但掉落翻倍"))));

        Component voteStandard = Component.literal("[✘ 标准入侵]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/feast invasion vote standard"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("点击投票 §7标准入侵"))));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(header);
            player.sendSystemMessage(Component.empty().append(voteStrengthen).append(Component.literal("  ")).append(voteStandard));
        }
    }

    private void updateVoting(MinecraftServer server) {
        if (voteTicks >= VOTE_DURATION) {
            voting = false;
            // Tally and trigger
            triggerInvasion(server, false);
        }
    }

    // ==================== Active invasion ====================

    private void updateActiveInvasion(MinecraftServer server) {
        if (activeTicks >= INVASION_DURATION) {
            endInvasion();
            return;
        }
        if (spawnCooldown > 0) {
            spawnCooldown--;
            return;
        }
        spawnCooldown = SPAWN_INTERVAL;
        spawnMonsters(server);
    }

    private void spawnMonsters(MinecraftServer server) {
        EntityType<? extends InvasionMonster> entityType = ModEntities.getEntityForStage(currentStage);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            ServerLevel level = player.serverLevel();

            // Count nearby invasion monsters
            long nearby = level.getEntitiesOfClass(InvasionMonster.class,
                    player.getBoundingBox().inflate(32)).size();
            if (nearby >= MAX_MOBS_NEAR_PLAYER) continue;

            int count = getSpawnCountForStage(currentStage, strengthened);
            for (int i = 0; i < count && nearby + i < MAX_MOBS_NEAR_PLAYER; i++) {
                BlockPos spawnPos = findSpawnPos(level, player.blockPosition());
                if (spawnPos == null) continue;

                InvasionMonster monster = entityType.create(level);
                if (monster != null) {
                    monster.moveTo(spawnPos, 0, 0);
                    monster.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                            MobSpawnType.EVENT, null);

                    // Glow effect for visibility
                    monster.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                            INVASION_DURATION * 20, 0, false, false));

                    if (strengthened) {
                        monster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                                .setBaseValue(monster.getMaxHealth() * 1.3);
                        monster.setHealth(monster.getMaxHealth());
                        monster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                .setBaseValue(monster.getAttributeValue(
                                        net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.2);
                    }

                    level.addFreshEntity(monster);
                }
            }
        }
    }

    private int getSpawnCountForStage(int stageId, boolean strengthened) {
        int base = switch (stageId) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 1; // Boss only 1
            default -> 1;
        };
        if (strengthened) base = Math.max(1, (int)(base * 1.5));
        return base;
    }

    private BlockPos findSpawnPos(ServerLevel level, BlockPos center) {
        RandomSource rand = level.getRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rand.nextIntBetweenInclusive(-SPAWN_MAX_RADIUS, SPAWN_MAX_RADIUS);
            int dz = rand.nextIntBetweenInclusive(-SPAWN_MAX_RADIUS, SPAWN_MAX_RADIUS);
            if (Math.abs(dx) < SPAWN_MIN_RADIUS && Math.abs(dz) < SPAWN_MIN_RADIUS) continue;
            BlockPos pos = center.offset(dx, 0, dz);
            pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (level.getBlockState(pos.below()).isSolid()) return pos.above();
        }
        return null;
    }

    // ==================== Broadcasts ====================

    private void broadcastWarning(MinecraftServer server, int remainingSeconds) {
        InvasionStage stage = InvasionStage.fromId(currentStage);

        Component header = Component.literal(
                "§c⚠ 异界能量正在汇聚… " + remainingSeconds + "秒后降临！第" + (currentStage + 1) + "阶段：" +
                stage.color + stage.chineseName);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(header);
        }
    }

    // ==================== Triggers ====================

    public void playerOffering(Player player) {
        if (active) {
            player.sendSystemMessage(Component.literal("§c入侵已在进行中！"));
            return;
        }
        offeringCount++;
        int remaining = Math.max(0, INVASION_INTERVAL - timerTicks);
        int minutes = remaining / 60;
        int secs = remaining % 60;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(
                    "§e" + player.getName().getString() + " 献祭了一份料理！(" +
                    offeringCount + "/" + OFFERINGS_TO_TRIGGER + ") §7剩余 " + minutes + ":" + String.format("%02d", secs)));
        }
        if (offeringCount >= OFFERINGS_TO_TRIGGER) {
            voting = false;
            triggerInvasion(server, true);
        }
    }

    public void playerVote(Player player, boolean voteStrengthened) {
        if (active) return;
        votes.put(player.getUUID(), voteStrengthened);
        player.sendSystemMessage(Component.literal(
                "§a你投票了 " + (voteStrengthened ? "§c强化入侵" : "§7标准入侵")));
    }

    public void triggerInvasion(MinecraftServer server, boolean manual) {
        active = true;
        voting = false;
        activeTicks = 0;
        spawnCooldown = 3;
        InvasionStage stage = InvasionStage.fromId(currentStage);

        // Tally votes
        int yesVotes = 0, noVotes = 0;
        for (boolean v : votes.values()) {
            if (v) yesVotes++; else noVotes++;
        }
        strengthened = yesVotes > noVotes;

        String triggerMsg = manual
                ? "§c献祭完成！异界之门提前开启！"
                : "§c异界能量满溢！第" + (currentStage + 1) + "阶段入侵降临！";
        String modeMsg = strengthened
                ? "§c⚔ 强化入侵模式！怪物血量+30%，伤害+20%，掉落翻倍！"
                : "§7标准入侵模式";

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§6========================================\n" +
                    "§e  " + stage.color + "异界入侵 · " + stage.chineseName + "\n" +
                    triggerMsg + "\n" + modeMsg + "\n" +
                    "§6========================================"));
        }

        timerTicks = 0;
        offeringCount = 0;
        votes.clear();
        warningSent = false;
    }

    public void endInvasion() {
        active = false;
        voting = false;
        activeTicks = 0;
        spawnCooldown = 0;
        voteTicks = 0;
        currentStage = (currentStage + 1) % InvasionStage.values().length;
        strengthened = false;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§a入侵结束！下一个阶段：" +
                    InvasionStage.fromId(currentStage).color +
                    InvasionStage.fromId(currentStage).chineseName));
        }
    }

    // ==================== Getters ====================

    public int getCurrentStageId() { return currentStage; }
    public InvasionStage getCurrentStage() { return InvasionStage.fromId(currentStage); }
    public boolean isActive() { return active; }
    public boolean isVoting() { return voting; }
    public boolean isStrengthened() { return strengthened; }
    public int getTimerTicks() { return timerTicks; }
    public int getMaxTicks() { return INVASION_INTERVAL; }
    public int getOfferingCount() { return offeringCount; }
    public int getMaxOfferings() { return OFFERINGS_TO_TRIGGER; }
    public int getActiveTicks() { return activeTicks; }
    public int getMaxActiveTicks() { return INVASION_DURATION; }
    public int getVoteTicks() { return voteTicks; }
}
