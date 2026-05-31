package com.crpg.ebb.interaction.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public record SyncedEntityTarget(
        UUID entityUuid,
        Identifier bindingId,
        Identifier dialogueId,
        double interactionRange,
        double highlightRange
) {
    public SyncedEntityTarget {
        if (entityUuid == null) {
            throw new IllegalArgumentException("entityUuid cannot be null");
        }
        if (bindingId == null) {
            throw new IllegalArgumentException("bindingId cannot be null");
        }
        if (dialogueId == null) {
            throw new IllegalArgumentException("dialogueId cannot be null");
        }
        if (!Double.isFinite(interactionRange) || interactionRange <= 0.0D) {
            throw new IllegalArgumentException("interactionRange must be finite and > 0");
        }
        if (!Double.isFinite(highlightRange) || highlightRange < interactionRange) {
            throw new IllegalArgumentException("highlightRange must be finite and >= interactionRange");
        }
    }

    public static SyncedEntityTarget from(Entity entity, EntityBindingDefinition binding) {
        return new SyncedEntityTarget(
                entity.getUUID(),
                binding.id(),
                binding.dialogueId(),
                binding.interactionRange(),
                binding.highlightRange()
        );
    }
}
