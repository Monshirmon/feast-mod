package com.etherealfeast.registry;

import com.etherealfeast.EtherealFeast;
import com.etherealfeast.entity.InvasionMonster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, EtherealFeast.MOD_ID);

    // Stage 0: 侵蚀 - 腐化动物 (30 HP)
    public static final Supplier<EntityType<InvasionMonster>> CORRUPTED_ANIMAL =
            ENTITY_TYPES.register("corrupted_animal",
                    () -> EntityType.Builder.of(InvasionMonster::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .eyeHeight(1.74f)
                            .build("corrupted_animal"));

    // Stage 1: 渗入 - 孢囊僵尸 (40 HP)
    public static final Supplier<EntityType<InvasionMonster>> SPORE_ZOMBIE =
            ENTITY_TYPES.register("spore_zombie",
                    () -> EntityType.Builder.of(InvasionMonster::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .eyeHeight(1.74f)
                            .build("spore_zombie"));

    // Stage 2: 扭曲 - 镜像苦力怕 (50 HP, placeholder zombie)
    public static final Supplier<EntityType<InvasionMonster>> MIRROR_CREEPER =
            ENTITY_TYPES.register("mirror_creeper",
                    () -> EntityType.Builder.of(InvasionMonster::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .eyeHeight(1.74f)
                            .build("mirror_creeper"));

    // Stage 3: 融合 - 血肉傀儡 (75 HP)
    public static final Supplier<EntityType<InvasionMonster>> FLESH_GOLEM =
            ENTITY_TYPES.register("flesh_golem",
                    () -> EntityType.Builder.of(InvasionMonster::new, MobCategory.MONSTER)
                            .sized(0.8f, 2.2f)
                            .eyeHeight(1.9f)
                            .build("flesh_golem"));

    // Stage 4: 终结 - 伪神BOSS (150 HP)
    public static final Supplier<EntityType<InvasionMonster>> FALSE_GOD =
            ENTITY_TYPES.register("false_god",
                    () -> EntityType.Builder.of(InvasionMonster::new, MobCategory.MONSTER)
                            .sized(1.0f, 2.8f)
                            .eyeHeight(2.4f)
                            .build("false_god"));

    /**
     * Get the entity type for a given invasion stage (0-4).
     */
    public static EntityType<? extends InvasionMonster> getEntityForStage(int stageId) {
        return switch (stageId) {
            case 1 -> SPORE_ZOMBIE.get();
            case 2 -> MIRROR_CREEPER.get();
            case 3 -> FLESH_GOLEM.get();
            case 4 -> FALSE_GOD.get();
            default -> CORRUPTED_ANIMAL.get();
        };
    }
}
