package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, EtherealFeast.MOD_ID);

    public static final Supplier<CreativeModeTab> ETHEREAL_FEAST_TAB = CREATIVE_MODE_TABS.register("ethereal_feast",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ethereal_feast"))
                    .icon(() -> new ItemStack(ModItems.BAIWEI_DUZHUO.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.BAIWEI_DUZHUO.get());
                        output.accept(ModItems.BAIWEI_GONGXIANG.get());
                        output.accept(ModItems.COOKBOOK.get());
                        output.accept(ModItems.CORRUPTED_MEAT.get());
                        output.accept(ModItems.CORRUPTED_STEAK.get());
                        output.accept(ModItems.ACID_GLAND.get());
                        output.accept(ModItems.ACID_STEW.get());
                        output.accept(ModItems.TWISTED_EYE.get());
                        output.accept(ModItems.VOID_CAKE.get());
                        output.accept(ModItems.FUSION_CORE.get());
                        output.accept(ModItems.FUSION_STEW.get());
                        output.accept(ModItems.GOD_SCALE.get());
                        output.accept(ModItems.GOD_FEAST.get());
                    })
                    .build());
}
