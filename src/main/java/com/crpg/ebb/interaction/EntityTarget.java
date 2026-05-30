package com.crpg.ebb.interaction;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record EntityTarget(
        Identifier id,
        UUID entityUuid,
        Vec3 interactionPoint,
        AABB bounds,
        Identifier dialogueId
) implements InteractionTarget {
    @Override
    public InteractionTargetType type() {
        return InteractionTargetType.ENTITY;
    }
}
