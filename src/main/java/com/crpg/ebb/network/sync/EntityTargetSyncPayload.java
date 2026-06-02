package com.crpg.ebb.network.sync;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.HighlightStyle;
import com.crpg.ebb.interaction.InteractionSyncLimits;
import com.crpg.ebb.interaction.entity.SyncedEntityTarget;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record EntityTargetSyncPayload(
        List<SyncedEntityTarget> targets
) implements CustomPacketPayload {
    public static final int MAX_TARGETS = InteractionSyncLimits.MAX_SYNCED_ENTITY_TARGETS;
    public static final Type<EntityTargetSyncPayload> TYPE = new Type<>(EbbMod.id("sync/entity_targets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityTargetSyncPayload> CODEC = StreamCodec.ofMember(
            EntityTargetSyncPayload::write,
            EntityTargetSyncPayload::read
    );

    public EntityTargetSyncPayload {
        targets = List.copyOf(targets);
        if (targets.size() > MAX_TARGETS) {
            throw new IllegalArgumentException("Cannot sync " + targets.size() + " entity targets; max is " + MAX_TARGETS);
        }
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(targets.size());
        for (SyncedEntityTarget target : targets) {
            writeTarget(buffer, target);
        }
    }

    private static EntityTargetSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_TARGETS) {
            throw new DecoderException("Invalid entity target sync count: " + count);
        }
        List<SyncedEntityTarget> targets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            targets.add(readTarget(buffer));
        }
        return new EntityTargetSyncPayload(targets);
    }

    private static void writeTarget(RegistryFriendlyByteBuf buffer, SyncedEntityTarget target) {
        buffer.writeUUID(target.entityUuid());
        buffer.writeIdentifier(target.bindingId());
        buffer.writeIdentifier(target.dialogueId());
        buffer.writeDouble(target.interactionRange());
        buffer.writeDouble(target.highlightRange());
        writeHighlightStyle(buffer, target.highlightStyle());
    }

    private static SyncedEntityTarget readTarget(RegistryFriendlyByteBuf buffer) {
        UUID entityUuid = buffer.readUUID();
        Identifier bindingId = buffer.readIdentifier();
        Identifier dialogueId = buffer.readIdentifier();
        double interactionRange = buffer.readDouble();
        double highlightRange = buffer.readDouble();
        HighlightStyle highlightStyle = readHighlightStyle(buffer);
        if (interactionRange <= 0.0D || highlightRange < interactionRange
                || !Double.isFinite(interactionRange) || !Double.isFinite(highlightRange)) {
            throw new DecoderException("Invalid range for synced entity target " + entityUuid + ": "
                    + interactionRange + "/" + highlightRange);
        }
        return new SyncedEntityTarget(entityUuid, bindingId, dialogueId, interactionRange, highlightRange, highlightStyle);
    }

    private static void writeHighlightStyle(RegistryFriendlyByteBuf buffer, HighlightStyle style) {
        buffer.writeInt(style.closeColor());
        buffer.writeInt(style.farColor());
        buffer.writeUtf(style.renderMode().serializedName(), 16);
        buffer.writeVarInt(style.priority());
    }

    private static HighlightStyle readHighlightStyle(RegistryFriendlyByteBuf buffer) {
        int closeColor = buffer.readInt();
        int farColor = buffer.readInt();
        HighlightStyle.RenderMode renderMode = HighlightStyle.RenderMode.parse(buffer.readUtf(16));
        int priority = buffer.readVarInt();
        return new HighlightStyle(closeColor, farColor, renderMode, priority);
    }

    @Override
    public Type<EntityTargetSyncPayload> type() {
        return TYPE;
    }
}
