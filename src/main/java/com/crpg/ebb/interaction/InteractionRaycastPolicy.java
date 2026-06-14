package com.crpg.ebb.interaction;

import net.minecraft.world.level.ClipContext;

/**
 * Shared raycast semantics for client prediction, server authority, and dev inspection.
 *
 * <p>The MVP interaction model treats only collider-visible objects as directly interactable.
 * Using one policy avoids the common dedicated-server mismatch where the client predicts
 * through outline-only shapes but the server rejects the action.</p>
 */
public final class InteractionRaycastPolicy {
    private InteractionRaycastPolicy() {
    }

    public static ClipContext.Block blockModeForPrediction() {
        return ClipContext.Block.COLLIDER;
    }

    public static ClipContext.Block blockModeForAuthority() {
        return ClipContext.Block.COLLIDER;
    }

    public static ClipContext.Block blockModeForDevInspect() {
        return blockModeForAuthority();
    }

    public static ClipContext.Fluid fluidMode() {
        return ClipContext.Fluid.NONE;
    }
}
