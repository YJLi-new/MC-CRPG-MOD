package com.crpg.ebb.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.EntityTarget;
import com.crpg.ebb.interaction.InteractionTarget;
import com.crpg.ebb.interaction.InteractionTargetType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public record InteractionRequestPayload(
        InteractionTargetType targetType,
        Identifier targetId,
        Optional<UUID> entityUuid
) implements CustomPacketPayload {
    public static final Type<InteractionRequestPayload> TYPE = new Type<>(EbbMod.id("interaction/request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InteractionRequestPayload> CODEC = StreamCodec.ofMember(
            InteractionRequestPayload::write,
            InteractionRequestPayload::read
    );

    public InteractionRequestPayload {
        entityUuid = entityUuid == null ? Optional.empty() : entityUuid;
    }

    public static InteractionRequestPayload fromTarget(InteractionTarget target) {
        Optional<UUID> uuid = target instanceof EntityTarget entityTarget
                ? Optional.of(entityTarget.entityUuid())
                : Optional.empty();
        return new InteractionRequestPayload(target.type(), target.id(), uuid);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(targetType);
        buffer.writeIdentifier(targetId);
        buffer.writeBoolean(entityUuid.isPresent());
        entityUuid.ifPresent(buffer::writeUUID);
    }

    private static InteractionRequestPayload read(RegistryFriendlyByteBuf buffer) {
        InteractionTargetType targetType = buffer.readEnum(InteractionTargetType.class);
        Identifier targetId = buffer.readIdentifier();
        Optional<UUID> entityUuid = buffer.readBoolean()
                ? Optional.of(buffer.readUUID())
                : Optional.empty();
        return new InteractionRequestPayload(targetType, targetId, entityUuid);
    }

    @Override
    public Type<InteractionRequestPayload> type() {
        return TYPE;
    }
}
