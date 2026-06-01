package com.crpg.ebb.api;

import net.minecraft.server.level.ServerPlayer;

public interface InteractableTarget {
    TargetRef ref();

    boolean canFocus(HitContext context);

    boolean canInteract(ServerPlayer player);

    InteractionOpenResult open(ServerPlayer player);
}
