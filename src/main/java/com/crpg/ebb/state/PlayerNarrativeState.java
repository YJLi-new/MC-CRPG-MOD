package com.crpg.ebb.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerNarrativeState {
    public static final Codec<PlayerNarrativeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("attributes", Map.of()).forGetter(PlayerNarrativeState::attributesForCodec),
            Codec.STRING.listOf().optionalFieldOf("flags", List.of()).forGetter(PlayerNarrativeState::flagsForCodec)
    ).apply(instance, PlayerNarrativeState::new));

    private final Map<String, Integer> attributes = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();

    public PlayerNarrativeState() {
    }

    private PlayerNarrativeState(Map<String, Integer> attributes, List<String> flags) {
        this.attributes.putAll(attributes);
        this.flags.addAll(flags);
    }

    public Map<String, Integer> attributes() {
        return attributes;
    }

    public Set<String> flags() {
        return flags;
    }

    private Map<String, Integer> attributesForCodec() {
        return Map.copyOf(attributes);
    }

    private List<String> flagsForCodec() {
        return List.copyOf(flags);
    }
}
