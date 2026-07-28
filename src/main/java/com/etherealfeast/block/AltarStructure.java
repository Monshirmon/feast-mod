package com.etherealfeast.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Interface for altar multi-block structure validation.
 * Implement this to define a specific block pattern required around the altar.
 * Different altar tiers can have different structure requirements.
 */
@FunctionalInterface
public interface AltarStructure {

    /**
     * Check if the required multi-block structure exists around the altar at the given position.
     * Called on the server side when a player attempts to offer.
     *
     * @param level the world
     * @param altarPos the position of the altar block
     * @return true if the structure is valid
     */
    boolean matches(Level level, BlockPos altarPos);

    /** Default: no structure required (just the altar block itself) */
    AltarStructure NONE = (level, altarPos) -> true;
}
