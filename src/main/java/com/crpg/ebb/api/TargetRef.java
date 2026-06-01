package com.crpg.ebb.api;

import com.crpg.ebb.interaction.InteractionTargetType;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public record TargetRef(
        InteractionTargetType type,
        Identifier id,
        Optional<UUID> entityUuid
) {
    public TargetRef {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
    }
}
