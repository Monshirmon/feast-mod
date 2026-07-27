package com.etherealfeast.recipe;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.registry.ModItems;
import com.etherealfeast.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * "百味·独酌" Recipe - Shapeless crafting requiring 7 different meat types + 1 apple + 1 flower.
 * Must detect meat types by ingredient category, not exact item.
 */
public class BaiWeiDuZhuoRecipe extends ShapelessRecipe {

    // Meat categories we detect
    private static final Set<String> MEAT_CATEGORIES = Set.of(
            "PORK", "BEEF", "CHICKEN", "MUTTON", "FISH", "SQUID", "VILLAGER"
    );

    public static final Map<Class<? extends Item>, String> ITEM_TO_MEAT_TYPE = new HashMap<>();

    static {
        ITEM_TO_MEAT_TYPE.put(Items.PORKCHOP.getClass(), "PORK");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_PORKCHOP.getClass(), "PORK");
        ITEM_TO_MEAT_TYPE.put(Items.BEEF.getClass(), "BEEF");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_BEEF.getClass(), "BEEF");
        ITEM_TO_MEAT_TYPE.put(Items.CHICKEN.getClass(), "CHICKEN");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_CHICKEN.getClass(), "CHICKEN");
        ITEM_TO_MEAT_TYPE.put(Items.MUTTON.getClass(), "MUTTON");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_MUTTON.getClass(), "MUTTON");
        ITEM_TO_MEAT_TYPE.put(Items.COD.getClass(), "FISH");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_COD.getClass(), "FISH");
        ITEM_TO_MEAT_TYPE.put(Items.SALMON.getClass(), "FISH");
        ITEM_TO_MEAT_TYPE.put(Items.COOKED_SALMON.getClass(), "FISH");
        ITEM_TO_MEAT_TYPE.put(Items.TROPICAL_FISH.getClass(), "FISH");
        ITEM_TO_MEAT_TYPE.put(Items.INK_SAC.getClass(), "SQUID");
        ITEM_TO_MEAT_TYPE.put(Items.ROTTEN_FLESH.getClass(), "VILLAGER");
    }

    private final String group;

    public BaiWeiDuZhuoRecipe(String group, CraftingBookCategory category) {
        super(group, category, new ItemStack(ModItems.BAIWEI_DUZHUO.get()), NonNullList.create());
        this.group = group;
    }

    public BaiWeiDuZhuoRecipe(String group, CraftingBookCategory category, NonNullList<Ingredient> ingredients) {
        super(group, category, new ItemStack(ModItems.BAIWEI_DUZHUO.get()), ingredients);
        this.group = group;
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

        // Must have exactly 9 items: 7 meats + 1 apple + 1 flower
        if (items.size() != 9) return false;

        Set<String> foundMeatTypes = new HashSet<>();
        boolean hasApple = false;
        boolean hasFlower = false;

        for (ItemStack stack : items) {
            Item item = stack.getItem();

            if (item == Items.APPLE || item == Items.GOLDEN_APPLE) {
                hasApple = true;
                continue;
            }

            if (isFlower(item)) {
                hasFlower = true;
                continue;
            }

            String meatType = getMeatType(item);
            if (meatType != null) {
                foundMeatTypes.add(meatType);
            } else {
                return false;
            }
        }

        return hasApple && hasFlower && foundMeatTypes.size() >= 7;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ModItems.BAIWEI_DUZHUO.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BAIWEI_DUZHUO_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BAIWEI_DUZHUO_TYPE.get();
    }

    @Override
    public String getGroup() {
        return group;
    }

    public static String getMeatType(Item item) {
        // Direct matching for vanilla meat items
        if (item == Items.PORKCHOP || item == Items.COOKED_PORKCHOP) return "PORK";
        if (item == Items.BEEF || item == Items.COOKED_BEEF) return "BEEF";
        if (item == Items.CHICKEN || item == Items.COOKED_CHICKEN) return "CHICKEN";
        if (item == Items.MUTTON || item == Items.COOKED_MUTTON) return "MUTTON";
        if (item == Items.COD || item == Items.COOKED_COD
                || item == Items.SALMON || item == Items.COOKED_SALMON
                || item == Items.TROPICAL_FISH) return "FISH";
        if (item == Items.INK_SAC || item == Items.GLOW_INK_SAC) return "SQUID";
        if (item == Items.ROTTEN_FLESH) return "VILLAGER";
        if (item == Items.RABBIT || item == Items.COOKED_RABBIT) return "PORK";

        // Tag-based fallback
        return null;
    }

    public static boolean isFlower(Item item) {
        return item == Items.DANDELION || item == Items.POPPY
                || item == Items.BLUE_ORCHID || item == Items.ALLIUM
                || item == Items.AZURE_BLUET || item == Items.RED_TULIP
                || item == Items.ORANGE_TULIP || item == Items.WHITE_TULIP
                || item == Items.PINK_TULIP || item == Items.OXEYE_DAISY
                || item == Items.CORNFLOWER || item == Items.LILY_OF_THE_VALLEY
                || item == Items.WITHER_ROSE || item == Items.SUNFLOWER
                || item == Items.LILAC || item == Items.ROSE_BUSH
                || item == Items.PEONY || item == Items.TORCHFLOWER
                || item == Items.PINK_PETALS || item == Items.SPORE_BLOSSOM
                || item == Items.CHERRY_LEAVES;
    }

    public static class Serializer implements RecipeSerializer<BaiWeiDuZhuoRecipe> {
        public static final MapCodec<BaiWeiDuZhuoRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                        CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(r -> CraftingBookCategory.MISC)
                ).apply(instance, BaiWeiDuZhuoRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BaiWeiDuZhuoRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8, r -> r.group,
                        CraftingBookCategory.STREAM_CODEC, r -> CraftingBookCategory.MISC,
                        BaiWeiDuZhuoRecipe::new
                );

        @Override
        public MapCodec<BaiWeiDuZhuoRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BaiWeiDuZhuoRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
