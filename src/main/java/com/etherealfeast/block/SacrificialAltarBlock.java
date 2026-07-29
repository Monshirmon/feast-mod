package com.etherealfeast.block;

import com.etherealfeast.invasion.InvasionManager;
import com.etherealfeast.registry.ModItems;
import com.etherealfeast.taste.TasteSystem;
import com.etherealfeast.taste.TasteType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sacrificial Altar - Right-click with food matching the required taste to offer.
 * Each offering advances the invasion countdown. 5 offerings trigger the invasion.
 *
 * Taste requirements are randomly generated per altar position and cycle
 * when all required tastes are fulfilled or the invasion triggers.
 */
public class SacrificialAltarBlock extends Block {

    /** Multi-block structure validator for this altar tier */
    private final AltarStructure structure;

    /** Per-position taste requirements: BlockPos -> set of taste IDs still needed */
    private static final Map<BlockPos, Set<String>> REQUIRED_TASTES = new HashMap<>();

    public SacrificialAltarBlock(Properties properties) {
        this(properties, AltarStructure.NONE);
    }

    public SacrificialAltarBlock(Properties properties, AltarStructure structure) {
        super(properties);
        this.structure = structure;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        // Check multi-block structure
        if (!structure.matches(level, pos)) {
            player.sendSystemMessage(Component.literal("§c祭坛结构不完整"));
            return ItemInteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return ItemInteractionResult.FAIL;

        InvasionManager im = InvasionManager.getInstance();
        if (im == null) {
            serverPlayer.sendSystemMessage(Component.literal("§c服务器未初始化"));
            return ItemInteractionResult.FAIL;
        }

        if (im.isActive()) {
            serverPlayer.sendSystemMessage(Component.literal("§c入侵已在进行中！"));
            return ItemInteractionResult.FAIL;
        }

        // Check for evil mixture - bypasses taste requirement
        if (stack.getItem() == ModItems.EVIL_MIXTURE.get()) {
            stack.consume(1, player);
            Set<String> required = getOrGenerateRequiredTastes(pos);
            // Remove one random taste as wildcard
            if (!required.isEmpty()) {
                required.remove(required.iterator().next());
            }
            serverPlayer.sendSystemMessage(Component.literal("祭坛尝到了§d兴奋§r的味道"));
            if (required.isEmpty()) {
                regenerateRequiredTastes(pos);
            }
            im.playerOffering(serverPlayer, true);
            return ItemInteractionResult.SUCCESS;
        }

        // Must be holding food
        if (stack.getFoodProperties(null) == null) {
            player.sendSystemMessage(Component.literal("§c需要手持食物或邪恶混合物才能献祭"));
            return ItemInteractionResult.FAIL;
        }

        // Check taste match
        List<TasteType.TasteValue> foodTastes = TasteSystem.getInstance().getTastes(stack.getItem());
        Set<String> required = getOrGenerateRequiredTastes(pos);
        String matched = findMatchingTaste(foodTastes, required);

        if (matched == null) {
            showRequiredTastes(serverPlayer, pos);
            return ItemInteractionResult.FAIL;
        }

        // Consume food and mark taste as fulfilled
        stack.consume(1, player);
        required.remove(matched);

        serverPlayer.sendSystemMessage(Component.literal(
                "§a献祭了 §e" + stack.getDisplayName().getString() + " §a（" +
                TasteType.fromId(matched).chineseName + "）§a！"));

        // Check if all tastes are fulfilled
        if (required.isEmpty()) {
            regenerateRequiredTastes(pos);
        }
        im.playerOffering(serverPlayer, false);

        return ItemInteractionResult.SUCCESS;
    }

    private Set<String> getOrGenerateRequiredTastes(BlockPos pos) {
        return REQUIRED_TASTES.computeIfAbsent(pos, k -> generateRandomTastes());
    }

    private Set<String> generateRandomTastes() {
        List<TasteType> all = new ArrayList<>(List.of(TasteType.values()));
        Collections.shuffle(all, new Random());
        // Require 3 random taste types
        return new HashSet<>(all.subList(0, 3).stream().map(t -> t.id).toList());
    }

    private void regenerateRequiredTastes(BlockPos pos) {
        REQUIRED_TASTES.put(pos, generateRandomTastes());
    }

    private String findMatchingTaste(List<TasteType.TasteValue> foodTastes, Set<String> required) {
        for (TasteType.TasteValue tv : foodTastes) {
            if (required.contains(tv.type().id)) {
                return tv.type().id;
            }
        }
        return null;
    }

    private void showRequiredTastes(ServerPlayer player, BlockPos pos) {
        Set<String> required = REQUIRED_TASTES.getOrDefault(pos, Set.of());
        String tasteNames = required.stream()
                .map(id -> { TasteType t = TasteType.fromId(id); return t != null ? t.chineseName : id; })
                .collect(Collectors.joining("§f、§e"));
        player.sendSystemMessage(Component.literal(
                "§6当前祭坛需要以下口味：§e" + tasteNames + "\n§7手持含有对应口味的食物右键祭坛献祭"));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            showRequiredTastes((ServerPlayer) player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Get the structure validator for this altar */
    public AltarStructure getStructure() { return structure; }

    public static void clearTasteData(BlockPos pos) {
        REQUIRED_TASTES.remove(pos);
    }
}
