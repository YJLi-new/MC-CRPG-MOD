package com.crpg.ebb.client.interaction;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.BlockGroupDefinition;
import com.crpg.ebb.interaction.BlockGroupTarget;
import com.crpg.ebb.interaction.EntityTarget;
import com.crpg.ebb.interaction.HighlightStyle;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.interaction.entity.SyncedEntityTarget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ClientTargetDetector {
    public static final double HIGHLIGHT_RANGE = 10.0D;
    public static final double INTERACTION_RANGE = 2.0D;
    private static int tickCounter;

    private ClientTargetDetector() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTargetDetector::onEndTick);
    }

    private static void onEndTick(Minecraft minecraft) {
        tickCounter++;
        if ((tickCounter & 1) != 0) {
            return;
        }
        ClientInteractionState.set(detect(minecraft));
    }

    public static ClientInteractionState.Snapshot detect(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isSpectator() || minecraft.screen != null) {
            ClientInteractionState.clear();
            return ClientInteractionState.Snapshot.empty("inactive_client_state");
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(HIGHLIGHT_RANGE));

        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        double blockDistanceSqr = hitDistanceSqr(eye, blockHit);
        Optional<BlockGroupDefinition> blockGroupDefinition = detectBlockGroup(player, blockHit);

        EntityHitResult entityHit = detectEntity(player, eye, look, end);
        double entityDistanceSqr = entityHit == null ? Double.POSITIVE_INFINITY : eye.distanceToSqr(entityHit.getLocation());

        InteractionTarget target = null;
        double distance = Double.NaN;
        double interactionRange = INTERACTION_RANGE;
        double highlightRange = HIGHLIGHT_RANGE;
        boolean entityTargetCandidate = false;
        String reason = "no_target";
        HighlightStyle highlightStyle = HighlightStyle.blockDefault();

        if (entityHit != null && entityDistanceSqr <= blockDistanceSqr) {
            Entity entity = entityHit.getEntity();
            Optional<SyncedEntityTarget> syncedTarget = ClientEntityTargetIndex.byUuid(entity.getUUID());
            net.minecraft.resources.Identifier dialogueId;
            entityTargetCandidate = true;
            if (syncedTarget.isPresent()) {
                SyncedEntityTarget targetData = syncedTarget.get();
                dialogueId = targetData.dialogueId();
                interactionRange = targetData.interactionRange();
                highlightRange = targetData.highlightRange();
                highlightStyle = targetData.highlightStyle();
                reason = "entity_target_sync_hit:" + targetData.bindingId();
            } else {
                var binding = EntityBindingRegistry.resolve(entity).orElse(null);
                if (binding == null) {
                    ClientInteractionState.set(ClientInteractionState.Snapshot.empty("unbound_entity"));
                    return ClientInteractionState.Snapshot.empty("unbound_entity");
                }
                dialogueId = binding.dialogueId();
                interactionRange = binding.interactionRange();
                highlightRange = binding.highlightRange();
                highlightStyle = binding.highlightStyle();
                reason = "entity_binding_hit:" + binding.id();
            }
            target = new EntityTarget(
                    EbbMod.id("entity/" + entity.getUUID()),
                    entity.getUUID(),
                    entity.getBoundingBox().getCenter(),
                    entity.getBoundingBox(),
                    dialogueId
            );
            distance = eye.distanceTo(target.interactionPoint());
        } else if (blockGroupDefinition.isPresent()) {
            BlockGroupDefinition definition = blockGroupDefinition.get();
            target = definition.asTarget();
            distance = target.bounds().distanceToSqr(player.position()) == 0.0D
                    ? 0.0D
                    : Math.sqrt(target.bounds().distanceToSqr(player.position()));
            highlightStyle = definition.highlightStyle();
            reason = "block_group_hit";
        }

        if (target == null) {
            String emptyReason = blockHit.getType() == HitResult.Type.BLOCK ? "blocked_or_unregistered_block" : "no_target";
            ClientInteractionState.set(ClientInteractionState.Snapshot.empty(emptyReason));
            return ClientInteractionState.Snapshot.empty(emptyReason);
        }
        if (entityTargetCandidate && distance > highlightRange) {
            ClientInteractionState.set(ClientInteractionState.Snapshot.empty("outside_binding_highlight_range"));
            return ClientInteractionState.Snapshot.empty("outside_binding_highlight_range");
        }

        boolean withinInteractionRange = distance <= interactionRange;
        return new ClientInteractionState.Snapshot(Optional.of(target), distance, withinInteractionRange, true, reason, highlightStyle);
    }

    private static Optional<BlockGroupDefinition> detectBlockGroup(LocalPlayer player, BlockHitResult blockHit) {
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return ClientBlockGroupIndex.byBlock(player.level().dimension(), blockHit.getBlockPos());
    }

    private static EntityHitResult detectEntity(LocalPlayer player, Vec3 eye, Vec3 look, Vec3 end) {
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(HIGHLIGHT_RANGE)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                player,
                eye,
                end,
                searchBox,
                entity -> entity != player
                        && entity.isPickable()
                        && !entity.isSpectator()
                        && (ClientEntityTargetIndex.contains(entity.getUUID()) || EntityBindingRegistry.isRegisteredTarget(entity)),
                HIGHLIGHT_RANGE * HIGHLIGHT_RANGE
        );
    }

    private static double hitDistanceSqr(Vec3 eye, HitResult hitResult) {
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return Double.POSITIVE_INFINITY;
        }
        return eye.distanceToSqr(hitResult.getLocation());
    }
}
