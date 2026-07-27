package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.recipe.BaiWeiDuZhuoRecipe;
import com.etherealfeast.recipe.BaiWeiGongXiangRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, EtherealFeast.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, EtherealFeast.MOD_ID);

    public static final Supplier<RecipeSerializer<BaiWeiDuZhuoRecipe>> BAIWEI_DUZHUO_SERIALIZER =
            RECIPE_SERIALIZERS.register("baiwei_duzhuo", BaiWeiDuZhuoRecipe.Serializer::new);

    public static final Supplier<RecipeType<BaiWeiDuZhuoRecipe>> BAIWEI_DUZHUO_TYPE =
            RECIPE_TYPES.register("baiwei_duzhuo", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "baiwei_duzhuo";
                }
            });

    public static final Supplier<RecipeSerializer<BaiWeiGongXiangRecipe>> BAIWEI_GONGXIANG_SERIALIZER =
            RECIPE_SERIALIZERS.register("baiwei_gongxiang", BaiWeiGongXiangRecipe.Serializer::new);

    public static final Supplier<RecipeType<BaiWeiGongXiangRecipe>> BAIWEI_GONGXIANG_TYPE =
            RECIPE_TYPES.register("baiwei_gongxiang", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "baiwei_gongxiang";
                }
            });
}
