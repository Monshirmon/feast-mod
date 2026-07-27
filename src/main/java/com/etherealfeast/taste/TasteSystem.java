package com.etherealfeast.taste;

import com.etherealfeast.EtherealFeast;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven taste system.
 * Loads ingredient taste data from JSON files in data/etherealfeast/taste_data/.
 *
 * Each JSON maps item registry names to taste values:
 * {
 *   "minecraft:porkchop": {"sweet": "++", "umami": "+++"},
 *   "minecraft:beef": {"umami": "+++", "salty": "+"}
 * }
 */
public class TasteSystem extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static TasteSystem INSTANCE;

    /** Item → List of taste values */
    private final Map<Item, List<TasteType.TasteValue>> ingredientTastes = new HashMap<>();

    /** Default taste data loaded from code as fallback */
    private static final Map<Item, List<TasteType.TasteValue>> DEFAULT_TASTES = new HashMap<>();

    static {
        // Pork: sweet & umami
        DEFAULT_TASTES.put(Items.PORKCHOP, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.COOKED_PORKCHOP, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));

        // Beef: umami rich
        DEFAULT_TASTES.put(Items.BEEF, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.COOKED_BEEF, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.WEAK)
        ));

        // Chicken: mild umami
        DEFAULT_TASTES.put(Items.CHICKEN, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM)
        ));
        DEFAULT_TASTES.put(Items.COOKED_CHICKEN, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));

        // Mutton: strong gamey (bitter + umami)
        DEFAULT_TASTES.put(Items.MUTTON, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.COOKED_MUTTON, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.WEAK),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));

        // Fish: umami + salty
        DEFAULT_TASTES.put(Items.COD, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.COOKED_COD, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.MEDIUM)
        ));
        DEFAULT_TASTES.put(Items.SALMON, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.COOKED_SALMON, List.of(
                new TasteType.TasteValue(TasteType.UMAMI, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.MEDIUM)
        ));

        // Apple: sweet + sour
        DEFAULT_TASTES.put(Items.APPLE, List.of(
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.SOUR, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.GOLDEN_APPLE, List.of(
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SOUR, TasteType.Strength.WEAK)
        ));

        // Squid (ink sac): salty + bitter
        DEFAULT_TASTES.put(Items.INK_SAC, List.of(
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.GLOW_INK_SAC, List.of(
                new TasteType.TasteValue(TasteType.SALTY, TasteType.Strength.MEDIUM),
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.WEAK),
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.WEAK)
        ));

        // Villager meat (rotten flesh): very bitter
        DEFAULT_TASTES.put(Items.ROTTEN_FLESH, List.of(
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.STRONG),
                new TasteType.TasteValue(TasteType.SOUR, TasteType.Strength.MEDIUM)
        ));

        // Spices and seasonings
        DEFAULT_TASTES.put(Items.SUGAR, List.of(
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.STRONG)
        ));
        DEFAULT_TASTES.put(Items.HONEY_BOTTLE, List.of(
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.STRONG)
        ));

        // Flowers: various subtle tastes
        DEFAULT_TASTES.put(Items.DANDELION, List.of(
                new TasteType.TasteValue(TasteType.BITTER, TasteType.Strength.WEAK)
        ));
        DEFAULT_TASTES.put(Items.POPPY, List.of(
                new TasteType.TasteValue(TasteType.SWEET, TasteType.Strength.WEAK)
        ));
    }

    public TasteSystem() {
        super(GSON, "ethereal_feast/taste_data");
    }

    public static TasteSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TasteSystem();
        }
        return INSTANCE;
    }

    public List<TasteType.TasteValue> getTastes(Item item) {
        List<TasteType.TasteValue> tastes = ingredientTastes.get(item);
        if (tastes != null) return tastes;

        // Fallback to defaults
        return DEFAULT_TASTES.getOrDefault(item, List.of());
    }

    /**
     * Calculate the taste profile for a list of items.
     * Sums all taste values and determines dominant taste and flavor tags.
     */
    public TasteType.TasteResult calculateTasteProfile(List<Item> items) {
        List<TasteType.TasteValue> allValues = new ArrayList<>();
        for (Item item : items) {
            allValues.addAll(getTastes(item));
        }
        return TasteType.calculateDominant(allValues);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        ingredientTastes.clear();
        EtherealFeast.LOGGER.info("Loading taste data from {} entries", data.size());

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> itemEntry : json.entrySet()) {
                    String itemId = itemEntry.getKey();
                    JsonObject tasteData = itemEntry.getValue().getAsJsonObject();

                    Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                    if (item == Items.AIR && !itemId.equals("minecraft:air")) {
                        EtherealFeast.LOGGER.warn("Unknown item in taste data: {}", itemId);
                        continue;
                    }

                    List<TasteType.TasteValue> values = new ArrayList<>();
                    for (TasteType tasteType : TasteType.values()) {
                        if (tasteData.has(tasteType.id)) {
                            String strengthStr = tasteData.get(tasteType.id).getAsString();
                            values.add(new TasteType.TasteValue(tasteType, TasteType.Strength.fromCode(strengthStr)));
                        }
                    }
                    ingredientTastes.put(item, values);
                }
            } catch (Exception e) {
                EtherealFeast.LOGGER.error("Failed to parse taste data: {}", entry.getKey(), e);
            }
        }

        EtherealFeast.LOGGER.info("Loaded tastes for {} ingredients", ingredientTastes.size());
    }

    @EventBusSubscriber(modid = EtherealFeast.MOD_ID)
    public static class Events {
        @SubscribeEvent
        public static void onAddReloadListener(AddReloadListenerEvent event) {
            event.addListener(getInstance());
        }
    }
}
