package com.crpg.ebb.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record BlockGroupTarget(
        Identifier id,
        ResourceKey<Level> dimension,
        List<BlockPos> blocks,
        Vec3 interactionPoint,
        Identifier dialogueId,
        AABB bounds
) implements InteractionTarget {
    public BlockGroupTarget {
        blocks = List.copyOf(blocks);
    }

    @Override
    public InteractionTargetType type() {
        return InteractionTargetType.BLOCK_GROUP;
    }
}
