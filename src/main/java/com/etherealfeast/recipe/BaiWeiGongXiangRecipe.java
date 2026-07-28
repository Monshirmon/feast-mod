package com.etherealfeast.recipe;

import com.etherealfeast.registry.ModItems;
import com.etherealfeast.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * "百味·共飨" Recipe - Shapeless crafting: 3 different meat types + 1 apple + 1 flower.
 */
public class BaiWeiGongXiangRecipe implements CraftingRecipe {

    private final String group;
    private final CraftingBookCategory category;

    public BaiWeiGongXiangRecipe(String group, CraftingBookCategory category) {
        this.group = group;
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        // Required: 3 different meats + 1 apple + 1 flower = 5 items
        if (items.size() != 5) return false;

        Set<String> foundMeatTypes = new HashSet<>();
        boolean hasApple = false;
        boolean hasFlower = false;

        for (ItemStack stack : items) {
            Item item = stack.getItem();

            if (item == Items.APPLE || item == Items.GOLDEN_APPLE) {
                hasApple = true;
                continue;
            }

            if (BaiWeiDuZhuoRecipe.isFlower(item)) {
                hasFlower = true;
                continue;
            }

            String meatType = BaiWeiDuZhuoRecipe.getMeatType(item);
            if (meatType != null) {
                foundMeatTypes.add(meatType);
            } else {
                return false;
            }
        }

        return hasApple && hasFlower && foundMeatTypes.size() == 3;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.BAIWEI_GONGXIANG.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BAIWEI_GONGXIANG_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BAIWEI_GONGXIANG_TYPE.get();
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.BAIWEI_GONGXIANG.get());
    }

    public static class Serializer implements RecipeSerializer<BaiWeiGongXiangRecipe> {
        public static final MapCodec<BaiWeiGongXiangRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> r.category)
                ).apply(instance, BaiWeiGongXiangRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BaiWeiGongXiangRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, r -> r.group,
                        CraftingBookCategory.STREAM_CODEC, r -> r.category,
                        BaiWeiGongXiangRecipe::new
                );

        @Override
        public MapCodec<BaiWeiGongXiangRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BaiWeiGongXiangRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
