package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.block.SacrificialAltarBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, EtherealFeast.MOD_ID);

    public static final Supplier<Block> SACRIFICIAL_ALTAR = BLOCKS.register("sacrificial_altar",
            () -> new SacrificialAltarBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
}
