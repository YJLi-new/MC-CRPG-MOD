package com.crpg.ebb.network.sync;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.HighlightStyle;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.InteractionSyncLimits;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.npc.profile.NpcTier;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record EntityBindingSyncPayload(
        List<EntityBindingDefinition> definitions,
        InteractionSettings.Snapshot settings
) implements CustomPacketPayload {
    public static final int MAX_BINDINGS = InteractionSyncLimits.MAX_ENTITY_BINDINGS;
    public static final int MAX_TAGS = InteractionSyncLimits.MAX_ENTITY_BINDING_TAGS;
    public static final int MAX_ENTITY_TYPES = InteractionSyncLimits.MAX_ENTITY_BINDING_TYPES;
    public static final int MAX_STRING_LENGTH = InteractionSyncLimits.MAX_ENTITY_BINDING_STRING_LENGTH;
    public static final Type<EntityBindingSyncPayload> TYPE = new Type<>(EbbMod.id("sync/entity_bindings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityBindingSyncPayload> CODEC = StreamCodec.ofMember(
            EntityBindingSyncPayload::write,
            EntityBindingSyncPayload::read
    );

    public EntityBindingSyncPayload {
        definitions = List.copyOf(definitions);
        settings = settings == null ? InteractionSettings.snapshot() : settings;
        if (definitions.size() > MAX_BINDINGS) {
            throw new IllegalArgumentException("Cannot sync " + definitions.size() + " entity bindings; max is " + MAX_BINDINGS);
        }
        for (EntityBindingDefinition definition : definitions) {
            validateDefinition(definition);
        }
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        writeSettings(buffer, settings);
        buffer.writeVarInt(definitions.size());
        for (EntityBindingDefinition definition : definitions) {
            writeDefinition(buffer, definition);
        }
    }

    private static EntityBindingSyncPayload read(RegistryFriendlyByteBuf buffer) {
        InteractionSettings.Snapshot settings = readSettings(buffer);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BINDINGS) {
            throw new DecoderException("Invalid entity binding sync count: " + count);
        }
        List<EntityBindingDefinition> definitions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            definitions.add(readDefinition(buffer));
        }
        return new EntityBindingSyncPayload(definitions, settings);
    }

    private static void writeDefinition(RegistryFriendlyByteBuf buffer, EntityBindingDefinition definition) {
        validateDefinition(definition);
        buffer.writeIdentifier(definition.id());
        buffer.writeBoolean(definition.uuid().isPresent());
        definition.uuid().ifPresent(buffer::writeUUID);
        buffer.writeVarInt(definition.tags().size());
        for (String tag : definition.tags()) {
            buffer.writeUtf(tag, MAX_STRING_LENGTH);
        }
        buffer.writeBoolean(definition.name().isPresent());
        definition.name().ifPresent(name -> buffer.writeUtf(name, MAX_STRING_LENGTH));
        buffer.writeVarInt(definition.entityTypes().size());
        for (Identifier entityType : definition.entityTypes()) {
            buffer.writeIdentifier(entityType);
        }
        buffer.writeIdentifier(definition.dialogueId());
        buffer.writeDouble(definition.interactionRange());
        buffer.writeDouble(definition.highlightRange());
        buffer.writeVarInt(definition.priority());
        writeHighlightStyle(buffer, definition.highlightStyle());
        buffer.writeBoolean(definition.npcProfileId().isPresent());
        definition.npcProfileId().ifPresent(buffer::writeIdentifier);
        buffer.writeUtf(definition.npcTier().serializedName(), MAX_STRING_LENGTH);
        buffer.writeBoolean(definition.promoteOnFirstChat());
        buffer.writeVarInt(definition.profileSeedArchetypes().size());
        for (String archetype : definition.profileSeedArchetypes()) {
            buffer.writeUtf(archetype, MAX_STRING_LENGTH);
        }
    }

    private static EntityBindingDefinition readDefinition(RegistryFriendlyByteBuf buffer) {
        Identifier id = buffer.readIdentifier();
        Optional<UUID> uuid = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        int tagCount = buffer.readVarInt();
        if (tagCount < 0 || tagCount > MAX_TAGS) {
            throw new DecoderException("Invalid tag count for entity binding " + id + ": " + tagCount);
        }
        List<String> tags = new ArrayList<>(tagCount);
        for (int i = 0; i < tagCount; i++) {
            tags.add(buffer.readUtf(MAX_STRING_LENGTH));
        }
        Optional<String> name = buffer.readBoolean() ? Optional.of(buffer.readUtf(MAX_STRING_LENGTH)) : Optional.empty();
        int typeCount = buffer.readVarInt();
        if (typeCount < 0 || typeCount > MAX_ENTITY_TYPES) {
            throw new DecoderException("Invalid entity type count for entity binding " + id + ": " + typeCount);
        }
        List<Identifier> entityTypes = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; i++) {
            entityTypes.add(buffer.readIdentifier());
        }
        Identifier dialogueId = buffer.readIdentifier();
        double interactionRange = buffer.readDouble();
        double highlightRange = buffer.readDouble();
        int priority = buffer.readVarInt();
        HighlightStyle highlightStyle = readHighlightStyle(buffer);
        Optional<Identifier> npcProfileId = buffer.readBoolean() ? Optional.of(buffer.readIdentifier()) : Optional.empty();
        NpcTier npcTier = NpcTier.parse(buffer.readUtf(MAX_STRING_LENGTH));
        boolean promoteOnFirstChat = buffer.readBoolean();
        int archetypeCount = buffer.readVarInt();
        if (archetypeCount < 0 || archetypeCount > MAX_TAGS) {
            throw new DecoderException("Invalid seed archetype count for entity binding " + id + ": " + archetypeCount);
        }
        List<String> profileSeedArchetypes = new ArrayList<>(archetypeCount);
        for (int i = 0; i < archetypeCount; i++) {
            profileSeedArchetypes.add(buffer.readUtf(MAX_STRING_LENGTH));
        }
        if (interactionRange <= 0.0D || highlightRange < interactionRange) {
            throw new DecoderException("Invalid range for entity binding " + id + ": " + interactionRange + "/" + highlightRange);
        }
        return new EntityBindingDefinition(id, uuid, tags, name, entityTypes, dialogueId, interactionRange, highlightRange, priority, highlightStyle, npcProfileId, npcTier, promoteOnFirstChat, profileSeedArchetypes);
    }

    private static void writeSettings(RegistryFriendlyByteBuf buffer, InteractionSettings.Snapshot settings) {
        buffer.writeBoolean(settings.enableDebugEntityFallback());
        buffer.writeIdentifier(settings.debugEntityFallbackDialogue());
        buffer.writeDouble(settings.debugEntityFallbackInteractionRange());
        buffer.writeDouble(settings.debugEntityFallbackHighlightRange());
        buffer.writeVarInt(settings.maxBlocksPerGroup());
    }

    private static InteractionSettings.Snapshot readSettings(RegistryFriendlyByteBuf buffer) {
        boolean fallback = buffer.readBoolean();
        Identifier fallbackDialogue = buffer.readIdentifier();
        double fallbackInteractionRange = buffer.readDouble();
        double fallbackHighlightRange = buffer.readDouble();
        int maxBlocksPerGroup = buffer.readVarInt();
        if (fallbackInteractionRange <= 0.0D || fallbackHighlightRange < fallbackInteractionRange) {
            throw new DecoderException("Invalid debug fallback range in entity binding sync: "
                    + fallbackInteractionRange + "/" + fallbackHighlightRange);
        }
        if (maxBlocksPerGroup <= 0 || maxBlocksPerGroup > InteractionSyncLimits.MAX_BLOCKS_PER_GROUP) {
            throw new DecoderException("Invalid max_blocks_per_group in entity binding sync: " + maxBlocksPerGroup);
        }
        return new InteractionSettings.Snapshot(
                fallback,
                fallbackDialogue,
                fallbackInteractionRange,
                fallbackHighlightRange,
                maxBlocksPerGroup
        );
    }

    private static void validateDefinition(EntityBindingDefinition definition) {
        if (definition.tags().size() > MAX_TAGS) {
            throw new IllegalArgumentException("Entity binding " + definition.id() + " has too many tags: " + definition.tags().size());
        }
        if (definition.entityTypes().size() > MAX_ENTITY_TYPES) {
            throw new IllegalArgumentException("Entity binding " + definition.id() + " has too many entity types: " + definition.entityTypes().size());
        }
        if (definition.profileSeedArchetypes().size() > MAX_TAGS) {
            throw new IllegalArgumentException("Entity binding " + definition.id() + " has too many seed archetypes: " + definition.profileSeedArchetypes().size());
        }
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
    public Type<EntityBindingSyncPayload> type() {
        return TYPE;
    }
}
