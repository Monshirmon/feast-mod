package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.item.BaiWeiItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
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
}
