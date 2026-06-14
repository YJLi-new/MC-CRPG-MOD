package com.crpg.ebb.interaction;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class InteractionService {
    public static final double INTERACTION_RANGE = 2.0D;
    private static final double EPSILON = 0.0001D;

    private InteractionService() {
    }

    public static InteractionValidationResult validateSessionTarget(ServerPlayer player, DialogueSession session) {
        return switch (session.targetType()) {
            case BLOCK_GROUP -> validateBlockGroup(player, session.targetId());
            case ENTITY -> session.entityUuid()
                    .map(uuid -> validateEntity(player, uuid))
                    .orElseGet(() -> InteractionValidationResult.deny("missing_session_entity_uuid"));
        };
    }

    public static InteractionValidationResult validateBlockGroup(ServerPlayer player, BlockGroupDefinition definition) {
        if (player.isSpectator()) {
            return InteractionValidationResult.deny("spectator");
        }
        if (!player.level().dimension().equals(definition.dimension())) {
            return InteractionValidationResult.deny("wrong_dimension");
        }
        if (!definition.expectedBlocksMatch(player.level())) {
            return InteractionValidationResult.deny("block_predicate_mismatch");
        }
        double distance = Math.sqrt(definition.bounds().distanceToSqr(player.position()));
        if (distance > INTERACTION_RANGE) {
            return InteractionValidationResult.deny("too_far");
        }
        if (!hasLineOfSightToBlockGroup(player, definition)) {
            return InteractionValidationResult.deny("blocked_line_of_sight");
        }
        return InteractionValidationResult.allow(definition.asTarget());
    }

    public static InteractionValidationResult validateBlockGroup(ServerPlayer player, net.minecraft.resources.Identifier blockGroupId) {
        return BlockGroupIndex.byId(blockGroupId)
                .map(definition -> validateBlockGroup(player, definition))
                .orElseGet(() -> InteractionValidationResult.deny("unknown_block_group"));
    }

    public static InteractionValidationResult validateEntity(ServerPlayer player, UUID entityUuid) {
        if (player.isSpectator()) {
            return InteractionValidationResult.deny("spectator");
        }
        ServerLevel level = (ServerLevel) player.level();
        Entity entity = level.getEntityInAnyDimension(entityUuid);
        if (entity == null) {
            return InteractionValidationResult.deny("unknown_entity");
        }
        if (!entity.level().dimension().equals(player.level().dimension())) {
            return InteractionValidationResult.deny("wrong_dimension");
        }
        if (entity.isRemoved() || !entity.isPickable() || entity.isSpectator()) {
            return InteractionValidationResult.deny("entity_not_interactable");
        }

        EntityBindingDefinition binding = EntityBindingRegistry.resolve(entity)
                .orElse(null);
        if (binding == null) {
            return InteractionValidationResult.deny("unbound_entity");
        }
        Vec3 interactionPoint = entity.getBoundingBox().getCenter();
        double distance = player.getEyePosition().distanceTo(interactionPoint);
        if (distance > binding.interactionRange()) {
            return InteractionValidationResult.deny("too_far");
        }
        if (!hasClearRay(player, interactionPoint)) {
            return InteractionValidationResult.deny("blocked_line_of_sight");
        }

        return InteractionValidationResult.allow(new EntityTarget(
                EbbMod.id("entity/" + entity.getUUID()),
                entity.getUUID(),
                interactionPoint,
                entity.getBoundingBox(),
                binding.dialogueId()
        ));
    }

    private static boolean hasLineOfSightToBlockGroup(ServerPlayer player, BlockGroupDefinition definition) {
        Vec3 eye = player.getEyePosition();
        return hasClearRayToBlockGroupPoint(player, definition, definition.nearestBlockCenter(eye))
                || hasClearRayToBlockGroupPoint(player, definition, definition.interactionPoint());
    }

    private static boolean hasClearRayToBlockGroupPoint(ServerPlayer player, BlockGroupDefinition definition, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye,
                target,
                InteractionRaycastPolicy.blockModeForAuthority(),
                InteractionRaycastPolicy.fluidMode(),
                player
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        double hitDistance = eye.distanceTo(hit.getLocation());
        double targetDistance = eye.distanceTo(target);
        return hitDistance + EPSILON >= targetDistance || definition.blocks().contains(hit.getBlockPos());
    }

    private static boolean hasClearRay(ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye,
                target,
                InteractionRaycastPolicy.blockModeForAuthority(),
                InteractionRaycastPolicy.fluidMode(),
                player
        ));
        return hit.getType() == HitResult.Type.MISS || eye.distanceTo(hit.getLocation()) + EPSILON >= eye.distanceTo(target);
    }
}
