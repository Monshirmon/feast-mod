package com.etherealfeast.invasion;

import net.minecraft.ChatFormatting;

/**
 * Five stages of the invasion event.
 * Stage 0-4, cycling every 30 minutes.
 */
public enum InvasionStage {
    EROSION(0, "侵蚀", ChatFormatting.DARK_GREEN,
            "腐化动物", "corrupted_meat", "污染肉排", "corrupted_steak",
            "饱食下降更快"),
    INFILTRATION(1, "渗入", ChatFormatting.DARK_PURPLE,
            "孢囊僵尸", "acid_gland", "酸液炖菜", "acid_stew",
            "酸液护盾，反伤中毒"),
    DISTORTION(2, "扭曲", ChatFormatting.DARK_RED,
            "虚空之眼", "twisted_eye", "虚空蛋糕", "void_cake",
            "穿墙5秒，结束扣血"),
    FUSION(3, "融合", ChatFormatting.DARK_BLUE,
            "血肉傀儡", "fusion_core", "融合浓汤", "fusion_stew",
            "攻击附带腐蚀，破甲"),
    FINAL(4, "终结", ChatFormatting.GOLD,
            "入侵者·伪神", "god_scale", "伪神盛宴", "god_feast",
            "全服3分钟全属性翻倍+死亡不掉落");

    public final int id;
    public final String chineseName;
    public final ChatFormatting color;
    public final String monsterName;
    public final String ingredientId;
    public final String foodName;
    public final String foodId;
    public final String effectDesc;

    InvasionStage(int id, String chineseName, ChatFormatting color,
                  String monsterName, String ingredientId,
                  String foodName, String foodId, String effectDesc) {
        this.id = id;
        this.chineseName = chineseName;
        this.color = color;
        this.monsterName = monsterName;
        this.ingredientId = ingredientId;
        this.foodName = foodName;
        this.foodId = foodId;
        this.effectDesc = effectDesc;
    }

    public static InvasionStage fromId(int id) {
        for (InvasionStage stage : values()) {
            if (stage.id == id) return stage;
        }
        return EROSION;
    }
}
