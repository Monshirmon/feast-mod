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

import static com.etherealfeast.taste.TasteType.Strength.*;

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

    /** Item → List of taste values (loaded from JSON on reload) */
    private final Map<Item, List<TasteType.TasteValue>> ingredientTastes = new HashMap<>();

    /** Default taste data loaded from code as fallback */
    private static final Map<Item, List<TasteType.TasteValue>> DEFAULT_TASTES = new HashMap<>();

    /**
     * API-registered tastes from other mods (e.g. Farmer's Delight addon).
     * Persists across JSON reloads. Highest priority.
     */
    private static final Map<Item, List<TasteType.TasteValue>> API_TASTES = new HashMap<>();

    static {
        // === 肉类 / Meat (Umami主打) ===
        addTaste(Items.PORKCHOP, TasteType.UMAMI, STRONG, TasteType.SWEET, WEAK);
        addTaste(Items.COOKED_PORKCHOP, TasteType.UMAMI, STRONG, TasteType.SWEET, MEDIUM, TasteType.SALTY, WEAK);
        addTaste(Items.BEEF, TasteType.UMAMI, STRONG, TasteType.SALTY, WEAK);
        addTaste(Items.COOKED_BEEF, TasteType.UMAMI, STRONG, TasteType.SALTY, MEDIUM, TasteType.SWEET, WEAK);
        addTaste(Items.CHICKEN, TasteType.UMAMI, MEDIUM);
        addTaste(Items.COOKED_CHICKEN, TasteType.UMAMI, MEDIUM, TasteType.SALTY, WEAK);
        addTaste(Items.MUTTON, TasteType.UMAMI, MEDIUM, TasteType.BITTER, WEAK);
        addTaste(Items.COOKED_MUTTON, TasteType.UMAMI, MEDIUM, TasteType.BITTER, WEAK, TasteType.SALTY, WEAK);
        addTaste(Items.RABBIT, TasteType.UMAMI, MEDIUM);
        addTaste(Items.COOKED_RABBIT, TasteType.UMAMI, MEDIUM, TasteType.SALTY, WEAK);

        // === 鱼类 / Fish (鲜+咸) ===
        addTaste(Items.COD, TasteType.UMAMI, MEDIUM, TasteType.SALTY, WEAK);
        addTaste(Items.COOKED_COD, TasteType.UMAMI, MEDIUM, TasteType.SALTY, MEDIUM);
        addTaste(Items.SALMON, TasteType.UMAMI, STRONG, TasteType.SALTY, WEAK);
        addTaste(Items.COOKED_SALMON, TasteType.UMAMI, STRONG, TasteType.SALTY, MEDIUM);
        addTaste(Items.TROPICAL_FISH, TasteType.UMAMI, WEAK, TasteType.SALTY, WEAK);
        addTaste(Items.PUFFERFISH, TasteType.UMAMI, MEDIUM, TasteType.SALTY, STRONG, TasteType.BITTER, MEDIUM);

        // === 蔬果 / Fruits & Vegetables ===
        addTaste(Items.APPLE, TasteType.SWEET, MEDIUM, TasteType.SOUR, WEAK);
        addTaste(Items.GOLDEN_APPLE, TasteType.SWEET, STRONG, TasteType.SOUR, WEAK);
        addTaste(Items.ENCHANTED_GOLDEN_APPLE, TasteType.SWEET, STRONG, TasteType.UMAMI, MEDIUM);
        addTaste(Items.SWEET_BERRIES, TasteType.SWEET, MEDIUM, TasteType.SOUR, WEAK);
        addTaste(Items.GLOW_BERRIES, TasteType.SWEET, MEDIUM);
        addTaste(Items.MELON_SLICE, TasteType.SWEET, WEAK);
        addTaste(Items.CHORUS_FRUIT, TasteType.SWEET, WEAK, TasteType.SOUR, STRONG);
        addTaste(Items.CARROT, TasteType.SWEET, MEDIUM);
        addTaste(Items.GOLDEN_CARROT, TasteType.SWEET, STRONG, TasteType.UMAMI, WEAK);
        addTaste(Items.BEETROOT, TasteType.SWEET, MEDIUM, TasteType.SOUR, WEAK);
        addTaste(Items.POTATO, TasteType.UMAMI, WEAK);
        addTaste(Items.BAKED_POTATO, TasteType.UMAMI, MEDIUM, TasteType.SALTY, WEAK);
        addTaste(Items.POISONOUS_POTATO, TasteType.BITTER, MEDIUM, TasteType.SOUR, WEAK);

        // === 烘焙 / Baked Goods ===
        addTaste(Items.BREAD, TasteType.UMAMI, WEAK, TasteType.SWEET, WEAK);
        addTaste(Items.COOKIE, TasteType.SWEET, MEDIUM);
        addTaste(Items.CAKE, TasteType.SWEET, STRONG);
        addTaste(Items.PUMPKIN_PIE, TasteType.SWEET, STRONG, TasteType.UMAMI, WEAK, TasteType.SPICY, WEAK);

        // === 汤羹 / Soups & Stews ===
        addTaste(Items.MUSHROOM_STEW, TasteType.UMAMI, STRONG, TasteType.BITTER, WEAK);
        addTaste(Items.BEETROOT_SOUP, TasteType.SWEET, MEDIUM, TasteType.SOUR, MEDIUM, TasteType.UMAMI, WEAK);
        addTaste(Items.RABBIT_STEW, TasteType.UMAMI, STRONG, TasteType.SWEET, WEAK, TasteType.SALTY, MEDIUM, TasteType.SPICY, WEAK);

        // === 海洋 / Ocean ===
        addTaste(Items.DRIED_KELP, TasteType.SALTY, STRONG, TasteType.UMAMI, WEAK);

        // === 香料 / Spices & Herbs ===
        addTaste(Items.SUGAR, TasteType.SWEET, STRONG);
        addTaste(Items.HONEY_BOTTLE, TasteType.SWEET, STRONG);
        addTaste(Items.COCOA_BEANS, TasteType.BITTER, STRONG);

        // === 特殊 / Special (苦、辣、酸来源) ===
        addTaste(Items.SPIDER_EYE, TasteType.BITTER, STRONG, TasteType.SOUR, WEAK);
        addTaste(Items.FERMENTED_SPIDER_EYE, TasteType.SOUR, STRONG, TasteType.BITTER, MEDIUM);
        addTaste(Items.ROTTEN_FLESH, TasteType.BITTER, STRONG, TasteType.SOUR, MEDIUM);
        addTaste(Items.INK_SAC, TasteType.SALTY, MEDIUM, TasteType.BITTER, WEAK);
        addTaste(Items.GLOW_INK_SAC, TasteType.SALTY, MEDIUM, TasteType.BITTER, WEAK, TasteType.SWEET, WEAK);

        // 辣味来源 — 地狱产物
        addTaste(Items.BLAZE_POWDER, TasteType.SPICY, STRONG);
        addTaste(Items.MAGMA_CREAM, TasteType.SPICY, STRONG, TasteType.SWEET, WEAK);
        addTaste(Items.NETHER_WART, TasteType.BITTER, MEDIUM, TasteType.SPICY, WEAK);

        // === 花 / Flowers ===
        addTaste(Items.DANDELION, TasteType.BITTER, WEAK);
        addTaste(Items.POPPY, TasteType.SWEET, WEAK);
    }

    private static void addTaste(Item item, Object... tasteAndStrength) {
        List<TasteType.TasteValue> values = new ArrayList<>();
        for (int i = 0; i < tasteAndStrength.length; i += 2) {
            TasteType type = (TasteType) tasteAndStrength[i];
            TasteType.Strength strength = (TasteType.Strength) tasteAndStrength[i + 1];
            values.add(new TasteType.TasteValue(type, strength));
        }
        DEFAULT_TASTES.put(item, values);
    }

    public TasteSystem() {
        super(GSON, "taste_data");
    }

    public static TasteSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TasteSystem();
        }
        return INSTANCE;
    }

    public List<TasteType.TasteValue> getTastes(Item item) {
        // 1. API-registered (highest priority, survives reloads)
        List<TasteType.TasteValue> apiTastes = API_TASTES.get(item);
        if (apiTastes != null) return apiTastes;

        // 2. JSON-loaded data
        List<TasteType.TasteValue> tastes = ingredientTastes.get(item);
        if (tastes != null) return tastes;

        // 3. Built-in defaults
        return DEFAULT_TASTES.getOrDefault(item, List.of());
    }

    // ==================== Public API for other mods ====================

    /**
     * Register a taste for an item. Call this from your mod's constructor or common setup.
     * Survives resource reloads (data/etherealfeast/taste_data/ JSON won't override).
     *
     * @param item     the item to register a taste for
     * @param type     the taste type (e.g. TasteType.SPICY)
     * @param strength the intensity (WEAK, MEDIUM, STRONG)
     */
    public static void registerTaste(Item item, TasteType type, TasteType.Strength strength) {
        API_TASTES.computeIfAbsent(item, k -> new ArrayList<>())
                .add(new TasteType.TasteValue(type, strength));
    }

    /**
     * Register multiple taste values for an item at once.
     * Survives resource reloads.
     */
    public static void registerTastes(Item item, TasteType.TasteValue... values) {
        API_TASTES.put(item, new ArrayList<>(List.of(values)));
    }

    /**
     * Remove all API-registered tastes for an item (e.g. for mod unloading).
     */
    public static void unregisterTastes(Item item) {
        API_TASTES.remove(item);
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
