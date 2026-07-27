package com.etherealfeast.taste;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the six taste dimensions for the flavor system.
 * Each ingredient contributes taste values (strong/medium/weak) to these dimensions.
 */
public enum TasteType {
    SWEET("sweet", "甜"),
    SOUR("sour", "酸"),
    SPICY("spicy", "辣"),
    SALTY("salty", "咸"),
    BITTER("bitter", "苦"),
    UMAMI("umami", "鲜");

    public final String id;
    public final String chineseName;

    TasteType(String id, String chineseName) {
        this.id = id;
        this.chineseName = chineseName;
    }

    /**
     * Strength levels: WEAK(+), MEDIUM(++), STRONG(+++)
     */
    public enum Strength {
        WEAK(1),
        MEDIUM(2),
        STRONG(3);

        public final int value;

        Strength(int value) {
            this.value = value;
        }

        public static Strength fromCode(String code) {
            return switch (code.toLowerCase()) {
                case "+++", "strong" -> STRONG;
                case "++", "medium" -> MEDIUM;
                default -> WEAK;
            };
        }
    }

    /**
     * A single taste contribution from an ingredient.
     */
    public record TasteValue(TasteType type, Strength strength) {
        public int getIntensity() {
            return strength.value;
        }
    }

    /**
     * Result of calculating the dominant taste and flavor tags.
     */
    public record TasteResult(TasteType dominant, int dominantIntensity, List<String> flavorTags) {
        public boolean isEmpty() {
            return dominant == null;
        }
    }

    /**
     * Calculate the dominant taste from a list of taste values.
     * Dominant = the taste type with the highest summed intensity.
     * Flavor tags = all taste types that exceed 30% of the dominant's intensity.
     */
    public static TasteResult calculateDominant(List<TasteValue> values) {
        if (values.isEmpty()) {
            return new TasteResult(null, 0, java.util.Collections.emptyList());
        }

        int[] totals = new int[values().length];
        for (TasteValue value : values) {
            totals[value.type.ordinal()] += value.getIntensity();
        }

        // Find dominant
        int maxIdx = 0;
        int maxVal = 0;
        for (int i = 0; i < totals.length; i++) {
            if (totals[i] > maxVal) {
                maxVal = totals[i];
                maxIdx = i;
            }
        }

        TasteType dominant = values()[maxIdx];

        // Flavor tags: tastes >= 30% of max
        int threshold = Math.max(1, maxVal * 30 / 100);
        List<String> tags = new java.util.ArrayList<>();
        for (int i = 0; i < totals.length; i++) {
            if (i != maxIdx && totals[i] >= threshold) {
                tags.add(values()[i].chineseName);
            }
        }

        return new TasteResult(dominant, maxVal, tags);
    }

    public static TasteType fromId(String id) {
        return Arrays.stream(values())
                .filter(t -> t.id.equals(id))
                .findFirst()
                .orElse(null);
    }
}
