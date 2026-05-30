package com.crpg.ebb.interaction;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public sealed interface InteractionTarget permits EntityTarget, BlockGroupTarget {
    Identifier id();

    InteractionTargetType type();

    Vec3 interactionPoint();

    AABB bounds();

    Identifier dialogueId();
}
