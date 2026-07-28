package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.ITEM, EtherealFeast.MOD_ID);

    // Identity items
    public static final Supplier<Item> BAIWEI_DUZHUO = ITEMS.register("baiwei_duzhuo",
            () -> new BaiWeiItem(BaiWeiItem.IdentityType.SOLO, new Item.Properties().stacksTo(1).fireResistant()));

    public static final Supplier<Item> BAIWEI_GONGXIANG = ITEMS.register("baiwei_gongxiang",
            () -> new BaiWeiItem(BaiWeiItem.IdentityType.TEAM, new Item.Properties().stacksTo(1).fireResistant()));

    public static final Supplier<Item> COOKBOOK = ITEMS.register("cookbook",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Stage 1: 侵蚀 - 腐化动物 → 污染肉排
    public static final Supplier<Item> CORRUPTED_MEAT = ITEMS.register("corrupted_meat",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> CORRUPTED_STEAK = ITEMS.register("corrupted_steak",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f)
                            .build())));

    // Stage 2: 渗入 - 孢囊僵尸 → 酸液炖菜
    public static final Supplier<Item> ACID_GLAND = ITEMS.register("acid_gland",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> ACID_STEW = ITEMS.register("acid_stew",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f)
                            .build()).stacksTo(1)));

    // Stage 3: 扭曲 - 虚空之眼 → 虚空蛋糕
    public static final Supplier<Item> TWISTED_EYE = ITEMS.register("twisted_eye",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> VOID_CAKE = ITEMS.register("void_cake",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.4f)
                            .build()).stacksTo(1)));

    // Stage 4: 融合 - 血肉傀儡 → 融合浓汤
    public static final Supplier<Item> FUSION_CORE = ITEMS.register("fusion_core",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> FUSION_STEW = ITEMS.register("fusion_stew",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f)
                            .build()).stacksTo(1)));

    // Stage 5: 终结 - 入侵者·伪神 → 伪神盛宴
    public static final Supplier<Item> GOD_SCALE = ITEMS.register("god_scale",
            () -> new Item(new Item.Properties().fireResistant()));

    public static final Supplier<Item> GOD_FEAST = ITEMS.register("god_feast",
            () -> new Item(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(20).saturationModifier(2.0f)
                            .alwaysEdible().build()).stacksTo(1).fireResistant()));

    // Block items
    public static final Supplier<Item> SACRIFICIAL_ALTAR = ITEMS.register("sacrificial_altar",
            () -> new BlockItem(ModBlocks.SACRIFICIAL_ALTAR.get(), new Item.Properties()));

    // Spawn eggs for placeholder invasion monsters
    public static final Supplier<Item> CORRUPTED_ANIMAL_SPAWN_EGG = ITEMS.register("corrupted_animal_spawn_egg",
            () -> new SpawnEggItem(ModEntities.CORRUPTED_ANIMAL.get(),
                    0x556B2F, 0x8B4513, new Item.Properties()));

    public static final Supplier<Item> SPORE_ZOMBIE_SPAWN_EGG = ITEMS.register("spore_zombie_spawn_egg",
            () -> new SpawnEggItem(ModEntities.SPORE_ZOMBIE.get(),
                    0x4B0082, 0x32CD32, new Item.Properties()));

    public static final Supplier<Item> MIRROR_CREEPER_SPAWN_EGG = ITEMS.register("mirror_creeper_spawn_egg",
            () -> new SpawnEggItem(ModEntities.MIRROR_CREEPER.get(),
                    0x8B0000, 0xC0C0C0, new Item.Properties()));

    public static final Supplier<Item> FLESH_GOLEM_SPAWN_EGG = ITEMS.register("flesh_golem_spawn_egg",
            () -> new SpawnEggItem(ModEntities.FLESH_GOLEM.get(),
                    0x00008B, 0x8B0000, new Item.Properties()));

    public static final Supplier<Item> FALSE_GOD_SPAWN_EGG = ITEMS.register("false_god_spawn_egg",
            () -> new SpawnEggItem(ModEntities.FALSE_GOD.get(),
                    0xFFD700, 0x4B0082, new Item.Properties()));
}
