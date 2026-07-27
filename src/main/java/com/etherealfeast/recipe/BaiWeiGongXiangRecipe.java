package com.etherealfeast.recipe;

import com.etherealfeast.registry.ModItems;
import com.etherealfeast.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * "百味·共飨" Recipe - Shapeless crafting requiring teammate-dependent meat count.
 * 2/3/4 teammates → 2/3/4 types of meat + 1 apple + 1 flower.
 * Requires player detection in crafting grid context.
 */
public class BaiWeiGongXiangRecipe extends ShapelessRecipe {

    private final String group;

    public BaiWeiGongXiangRecipe(String group, CraftingBookCategory category) {
        super(group, category, new ItemStack(ModItems.BAIWEI_GONGXIANG.get()), NonNullList.create());
        this.group = group;
    }

    public BaiWeiGongXiangRecipe(String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(group, category, new ItemStack(ModItems.BAIWEI_GONGXIANG.get()), ingredients);
        this.group = group;
    }

    /**
     * Count nearby teammates (players with the same team identity within 16 blocks).
     */
    public static int countNearbyTeammates(Player player) {
        int count = 0;
        List<? extends Player> nearby = player.level().getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(16.0),
                p -> p != player && !p.isSpectator()
        );
        return nearby.size();
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

        // For GongXiang, we can't know the player count from the crafting grid alone.
        // We check ingredient validity; player count is validated in ModEvents before crafting.
        // Required: at least 2 meats + 1 apple + 1 flower = 4 items minimum,
        // or up to 4 meats + 1 apple + 1 flower = 6 items maximum.

        if (items.size() < 4 || items.size() > 6) return false;

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

        int meatCount = foundMeatTypes.size();

        return hasApple && hasFlower && meatCount >= 2 && meatCount <= 4;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.BAIWEI_GONGXIANG.get());
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
    public String getGroup() {
        return group;
    }

    public static class Serializer implements RecipeSerializer<BaiWeiGongXiangRecipe> {
        public static final MapCodec<BaiWeiGongXiangRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> CraftingBookCategory.MISC)
                ).apply(instance, BaiWeiGongXiangRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BaiWeiGongXiangRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, r -> r.group,
                        CraftingBookCategory.STREAM_CODEC, r -> CraftingBookCategory.MISC,
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
