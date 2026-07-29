package com.etherealfeast.invasion;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.entity.InvasionMonster;
import com.etherealfeast.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public class InvasionManager {

    private static final int INVASION_DURATION = 300;    // 5 min
    private static final int SPAWN_INTERVAL = 10;        // 10 seconds
    private static final int MAX_MOBS_NEAR_PLAYER = 5;
    private static final int SPAWN_MIN_RADIUS = 7;
    private static final int SPAWN_MAX_RADIUS = 15;
    private static final int OFFERINGS_TO_TRIGGER = 5;

    private static InvasionManager instance;

    private MinecraftServer server;
    private int currentStage = 0;
    private int activeTicks = 0;
    private int spawnCooldown = 0;
    private boolean active = false;
    private boolean strengthened = false;
    private int offeringCount = 0;
    private int strengthenChance = 10; // 初始10%
    private ServerBossEvent bossBar;

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
        if (this.server != srv) {
            this.server = srv;
            EtherealFeast.LOGGER.info("InvasionManager server set");
        }
        if (srv.getTickCount() % 20 != 0) return;

        if (srv.getTickCount() % 600 == 0) {
            EtherealFeast.LOGGER.info("Invasion tick: active={}, activeTicks={}, offeringCount={}, strengthenChance={}",
                    active, activeTicks, offeringCount, strengthenChance);
        }

        if (active) {
            activeTicks++;
            updateActiveInvasion(srv);
            updateBossBar();
        }
    }

    // ==================== Boss Bar ====================

    private void updateBossBar() {
        if (bossBar == null) return;
        int remaining = INVASION_DURATION - activeTicks;
        int minutes = remaining / 60;
        int secs = remaining % 60;
        InvasionStage stage = InvasionStage.fromId(currentStage);
        String mode = strengthened ? "§c⚔强化" : "§7标准";
        bossBar.setName(Component.literal(stage.color + "异界入侵 · " + stage.chineseName +
                " §f| " + mode + " §f| §e" + minutes + ":" + String.format("%02d", secs)));
        bossBar.setProgress((float) remaining / INVASION_DURATION);
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
        int totalSpawned = 0;
        int playerCount = server.getPlayerList().getPlayers().size();
        EtherealFeast.LOGGER.info("spawnMonsters start: players={}, stage={}, entityType={}",
                playerCount, currentStage, entityType);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) {
                EtherealFeast.LOGGER.info("Skipping player {} (spectator/creative)", player.getName().getString());
                continue;
            }
            ServerLevel level = player.serverLevel();

            long nearby = level.getEntitiesOfClass(InvasionMonster.class,
                    player.getBoundingBox().inflate(32)).size();
            EtherealFeast.LOGGER.info("Player {}: nearby monsters={}", player.getName().getString(), nearby);
            if (nearby >= MAX_MOBS_NEAR_PLAYER) continue;

            int count = getSpawnCountForStage(currentStage, strengthened);
            EtherealFeast.LOGGER.info("Attempting to spawn {} monsters for stage {}", count, currentStage);
            for (int i = 0; i < count && nearby + i < MAX_MOBS_NEAR_PLAYER; i++) {
                BlockPos spawnPos = findSpawnPos(level, player.blockPosition());
                if (spawnPos == null) {
                    EtherealFeast.LOGGER.warn("findSpawnPos null for player at {}", player.blockPosition());
                    continue;
                }

                InvasionMonster monster = entityType.create(level);
                if (monster == null) {
                    EtherealFeast.LOGGER.error("entityType.create returned null!");
                    continue;
                }
                monster.moveTo(spawnPos, 0, 0);
                monster.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.EVENT, null);

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
                totalSpawned++;
            }
        }
        EtherealFeast.LOGGER.info("spawnMonsters done: totalSpawned={}, stage={}", totalSpawned, currentStage);
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
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = rand.nextIntBetweenInclusive(-SPAWN_MAX_RADIUS, SPAWN_MAX_RADIUS);
            int dz = rand.nextIntBetweenInclusive(-SPAWN_MAX_RADIUS, SPAWN_MAX_RADIUS);
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) continue;
            BlockPos pos = center.offset(dx, 0, dz);
            int surfaceY = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY();
            pos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
            return pos;
        }
        return center.offset(rand.nextIntBetweenInclusive(2, 5), 0, rand.nextIntBetweenInclusive(2, 5));
    }

    // ==================== Offering & Trigger ====================

    public void playerOffering(Player player, boolean isEvil) {
        if (active) {
            player.sendSystemMessage(Component.literal("§c入侵已在进行中！"));
            return;
        }
        offeringCount++;

        if (isEvil) {
            strengthenChance = Math.min(100, strengthenChance + 5);
            player.sendSystemMessage(Component.literal("§c你感到些许躁动... (强化概率 " + strengthenChance + "%)"));
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal(
                    "§e" + player.getName().getString() + " 献祭了一份" + (isEvil ? "§c邪恶混合物" : "料理") + "！(" +
                    offeringCount + "/" + OFFERINGS_TO_TRIGGER + ") §7强化概率: " + strengthenChance + "%"));
        }

        if (offeringCount >= OFFERINGS_TO_TRIGGER) {
            strengthened = new Random().nextInt(100) < strengthenChance;
            triggerInvasion(server);
        }
    }

    public void triggerInvasion(MinecraftServer server) {
        active = true;
        activeTicks = 0;
        spawnCooldown = 0;
        InvasionStage stage = InvasionStage.fromId(currentStage);

        // Create boss bar for countdown display
        bossBar = new ServerBossEvent(
                Component.literal(stage.color + "异界入侵 · " + stage.chineseName),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.NOTCHED_20);
        bossBar.setVisible(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
        }

        String modeMsg = strengthened
                ? "§c⚔ 强化入侵模式！怪物血量+30%，伤害+20%，掉落翻倍！"
                : "§7标准入侵模式";

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§6========================================\n" +
                    "§e  " + stage.color + "异界入侵 · " + stage.chineseName + "\n" +
                    "§c献祭完成！异界之门开启！\n" + modeMsg + "\n" +
                    "§6========================================"));
        }

        offeringCount = 0;
        strengthenChance = 10;

        EtherealFeast.LOGGER.info("Invasion triggered: stage={}, active={}, strengthened={}", currentStage, active, strengthened);

        spawnMonsters(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("§e[调试] 侵入已激活，正在刷怪... activeTicks=" + activeTicks));
        }
    }

    public void endInvasion() {
        active = false;
        activeTicks = 0;
        spawnCooldown = 0;
        currentStage = (currentStage + 1) % InvasionStage.values().length;
        strengthened = false;

        // Remove boss bar
        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar.setVisible(false);
            bossBar = null;
        }

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
    public boolean isStrengthened() { return strengthened; }
    public int getOfferingCount() { return offeringCount; }
    public int getMaxOfferings() { return OFFERINGS_TO_TRIGGER; }
    public int getActiveTicks() { return activeTicks; }
    public int getMaxActiveTicks() { return INVASION_DURATION; }
    public int getStrengthenChance() { return strengthenChance; }
}
