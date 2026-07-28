package com.etherealfeast.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Placeholder invasion monster - currently a Zombie with configurable health.
 * Each invasion stage has its own EntityType with different base max health.
 * Will be replaced by custom models/behaviors later.
 */
public class InvasionMonster extends Zombie {

    public InvasionMonster(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes(double maxHealth) {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, maxHealth)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23);
    }
}
