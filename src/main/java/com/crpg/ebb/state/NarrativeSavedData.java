package com.crpg.ebb.state;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.attribute.AttributeDefinition;
import com.crpg.ebb.attribute.AttributeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NarrativeSavedData extends SavedData {
    public static final Codec<NarrativeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerNarrativeState.CODEC).optionalFieldOf("players", Map.of()).forGetter(NarrativeSavedData::playersForCodec),
            Codec.STRING.listOf().optionalFieldOf("world_flags", List.of()).forGetter(NarrativeSavedData::worldFlagsForCodec)
    ).apply(instance, NarrativeSavedData::new));

    public static final SavedDataType<NarrativeSavedData> TYPE = new SavedDataType<>(
            EbbMod.id("narrative_state"),
            NarrativeSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<String, PlayerNarrativeState> players = new LinkedHashMap<>();
    private final Set<String> worldFlags = new LinkedHashSet<>();

    public NarrativeSavedData() {
    }

    private NarrativeSavedData(Map<String, PlayerNarrativeState> players, List<String> worldFlags) {
        this.players.putAll(players);
        this.worldFlags.addAll(worldFlags);
    }

    public static NarrativeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static NarrativeSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public PlayerNarrativeState player(UUID playerUuid) {
        return players.computeIfAbsent(playerUuid.toString(), ignored -> new PlayerNarrativeState());
    }

    public int getAttribute(UUID playerUuid, String key) {
        String normalized = normalize(key);
        String canonical = AttributeRegistry.canonicalKey(normalized);
        Map<String, Integer> attributes = player(playerUuid).attributes();
        if (attributes.containsKey(canonical)) {
            return attributes.get(canonical);
        }
        if (!canonical.equals(normalized) && attributes.containsKey(normalized)) {
            return attributes.get(normalized);
        }
        return AttributeRegistry.defaultScore(canonical);
    }

    public void setAttribute(UUID playerUuid, String key, int value) {
        String canonical = AttributeRegistry.canonicalKey(key);
        player(playerUuid).attributes().put(canonical, AttributeRegistry.clamp(canonical, value));
        setDirty();
    }

    public int getAttributePoints(UUID playerUuid) {
        return player(playerUuid).attributePoints();
    }

    public void setAttributePoints(UUID playerUuid, int points) {
        player(playerUuid).setAttributePoints(points);
        setDirty();
    }

    public void addAttributePoints(UUID playerUuid, int points) {
        PlayerNarrativeState state = player(playerUuid);
        state.setAttributePoints(state.attributePoints() + points);
        setDirty();
    }

    public SpendResult spendAttributePoint(UUID playerUuid, String key, int amount) {
        if (amount <= 0) {
            return new SpendResult(false, "amount_must_be_positive", getAttribute(playerUuid, key), getAttributePoints(playerUuid));
        }
        String canonical = AttributeRegistry.canonicalKey(key);
        AttributeDefinition definition = AttributeRegistry.byKey(canonical).orElse(null);
        if (definition == null) {
            return new SpendResult(false, "unknown_attribute:" + key, 0, getAttributePoints(playerUuid));
        }
        PlayerNarrativeState state = player(playerUuid);
        if (state.attributePoints() < amount) {
            return new SpendResult(false, "not_enough_attribute_points", getAttribute(playerUuid, canonical), state.attributePoints());
        }
        int current = getAttribute(playerUuid, canonical);
        int next = current + amount;
        if (next > definition.max()) {
            return new SpendResult(false, "attribute_max:" + definition.max(), current, state.attributePoints());
        }
        state.attributes().put(canonical, next);
        state.setAttributePoints(state.attributePoints() - amount);
        setDirty();
        return new SpendResult(true, canonical, next, state.attributePoints());
    }

    public void resetAttributes(UUID playerUuid) {
        PlayerNarrativeState state = player(playerUuid);
        state.attributes().clear();
        state.setAttributePoints(PlayerNarrativeState.DEFAULT_ATTRIBUTE_POINTS);
        setDirty();
    }

    public String attributeLine(UUID playerUuid, String key) {
        String canonical = AttributeRegistry.canonicalKey(key);
        AttributeDefinition definition = AttributeRegistry.byKey(canonical).orElse(null);
        String name = definition == null ? canonical : definition.displayName();
        return canonical + " (" + name + ") = " + getAttribute(playerUuid, canonical);
    }

    public boolean hasPlayerFlag(UUID playerUuid, String flag) {
        return player(playerUuid).flags().contains(flag);
    }

    public void setPlayerFlag(UUID playerUuid, String flag, boolean value) {
        if (value) {
            player(playerUuid).flags().add(flag);
        } else {
            player(playerUuid).flags().remove(flag);
        }
        setDirty();
    }

    public boolean hasWorldFlag(String flag) {
        return worldFlags.contains(flag);
    }

    public void setWorldFlag(String flag, boolean value) {
        if (value) {
            worldFlags.add(flag);
        } else {
            worldFlags.remove(flag);
        }
        setDirty();
    }

    public int playerCount() {
        return players.size();
    }

    public int worldFlagCount() {
        return worldFlags.size();
    }

    public String summaryLine() {
        return "narrative_state(players=" + players.size() + ", world_flags=" + worldFlags.size() + ")";
    }

    private Map<String, PlayerNarrativeState> playersForCodec() {
        return Map.copyOf(players);
    }

    private List<String> worldFlagsForCodec() {
        return List.copyOf(worldFlags);
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    public record SpendResult(boolean success, String reason, int score, int remainingPoints) {
    }
}
