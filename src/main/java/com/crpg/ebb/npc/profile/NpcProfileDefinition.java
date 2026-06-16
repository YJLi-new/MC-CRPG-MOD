package com.crpg.ebb.npc.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record NpcProfileDefinition(
        Identifier id,
        NpcTier tier,
        String displayName,
        Optional<Identifier> entityBinding,
        LlmSettings llm,
        CharacterProfile character,
        Stance stance,
        Knowledge knowledge,
        Promotion promotion
) {
    public NpcProfileDefinition {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        tier = tier == null ? NpcTier.STATIC_NON_LLM : tier;
        displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName.strip();
        entityBinding = entityBinding == null ? Optional.empty() : entityBinding;
        llm = llm == null ? LlmSettings.disabled() : llm;
        character = character == null ? CharacterProfile.empty() : character;
        stance = stance == null ? Stance.empty() : stance;
        knowledge = knowledge == null ? Knowledge.empty() : knowledge;
        promotion = promotion == null ? Promotion.defaultPolicy() : promotion;
    }

    public static Optional<NpcProfileDefinition> parse(Identifier dataId, JsonObject json, List<String> messages) {
        try {
            Optional<String> declaredId = optionalString(json, "id");
            if (declaredId.isPresent() && !Identifier.parse(declaredId.get()).equals(dataId)) {
                messages.add("npc profile " + dataId + ": declared id " + declaredId.get() + " does not match file id; using file id");
            }
            NpcTier tier = optionalString(json, "tier")
                    .map(NpcTier::parse)
                    .orElse(NpcTier.MAJOR_SCRIPTED);
            String displayName = optionalString(json, "display_name")
                    .or(() -> optionalString(json, "name"))
                    .orElse(dataId.toString());
            Optional<Identifier> entityBinding = optionalString(json, "entity_binding")
                    .map(value -> parseIdentifier(value, "ebb"));
            LlmSettings llm = parseLlm(object(json, "llm"));
            CharacterProfile character = parseCharacter(object(json, "character"));
            Stance stance = parseStance(object(json, "stance"));
            Knowledge knowledge = parseKnowledge(object(json, "knowledge"));
            Promotion promotion = parsePromotion(object(json, "promotion"));
            if (tier == NpcTier.MINOR_GENERATABLE) {
                messages.add("npc profile " + dataId + ": npc_profiles should define major/static profiles; minor candidates belong in entity bindings");
            }
            return Optional.of(new NpcProfileDefinition(dataId, tier, displayName, entityBinding, llm, character, stance, knowledge, promotion));
        } catch (RuntimeException ex) {
            messages.add("npc profile " + dataId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public String debugSummary() {
        return "profile " + id
                + " tier=" + tier.serializedName()
                + " display='" + displayName + "'"
                + " binding=" + entityBinding.map(Identifier::toString).orElse("-")
                + " llm=" + llm.debugSummary()
                + " archetype='" + character.archetype() + "'"
                + " stance=" + stance.debugSummary()
                + " knowledge_packs=" + knowledge.initialPacks().size();
    }

    private static LlmSettings parseLlm(JsonObject json) {
        if (json == null) return LlmSettings.disabled();
        return new LlmSettings(
                optionalBoolean(json, "enabled").orElse(false),
                optionalString(json, "provider").orElse("fake"),
                optionalString(json, "chat_model").orElse("default_chat"),
                optionalDouble(json, "temperature").orElse(0.7D),
                optionalInt(json, "max_output_tokens").orElse(450),
                optionalBoolean(json, "allow_memory_write").orElse(false),
                optionalBoolean(json, "allow_dynamic_options").orElse(false)
        );
    }

    private static CharacterProfile parseCharacter(JsonObject json) {
        if (json == null) return CharacterProfile.empty();
        return new CharacterProfile(
                optionalString(json, "archetype").orElse("unspecified"),
                optionalString(json, "voice").orElse("plain"),
                parseStringList(json, "values"),
                parseStringList(json, "fears"),
                parseStringList(json, "speech_rules")
        );
    }

    private static Stance parseStance(JsonObject json) {
        if (json == null) return Stance.empty();
        return new Stance(
                optionalString(json, "faction").map(value -> parseIdentifier(value, "ebb")),
                optionalString(json, "default_attitude_to_player").or(() -> optionalString(json, "attitude")).orElse("neutral"),
                optionalInt(json, "trust").orElse(0),
                optionalInt(json, "fear").orElse(0),
                optionalInt(json, "resentment").orElse(0),
                parseIdentifierList(json, "secrets", "ebb")
        );
    }

    private static Knowledge parseKnowledge(JsonObject json) {
        if (json == null) return Knowledge.empty();
        List<ForbiddenFact> forbidden = new ArrayList<>();
        if (json.has("forbidden_to_reveal_until") && json.get("forbidden_to_reveal_until").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("forbidden_to_reveal_until")) {
                if (element.isJsonObject()) {
                    JsonObject fact = element.getAsJsonObject();
                    forbidden.add(new ForbiddenFact(
                            optionalString(fact, "fact").orElse(""),
                            optionalString(fact, "condition").orElse("")
                    ));
                }
            }
        }
        return new Knowledge(parseIdentifierList(json, "initial_packs", "ebb"), forbidden);
    }

    private static Promotion parsePromotion(JsonObject json) {
        if (json == null) return Promotion.defaultPolicy();
        return new Promotion(optionalBoolean(json, "can_be_demoted").orElse(false));
    }

    private static JsonObject object(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
    }

    private static List<String> parseStringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        if (!json.has(key) || json.get(key).isJsonNull()) return values;
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                values.add(item.getAsString());
            }
        } else {
            values.add(element.getAsString());
        }
        return List.copyOf(values);
    }

    private static List<Identifier> parseIdentifierList(JsonObject json, String key, String defaultNamespace) {
        return parseStringList(json, key).stream()
                .map(value -> parseIdentifier(value, defaultNamespace))
                .toList();
    }

    public static Identifier parseIdentifier(String value, String defaultNamespace) {
        return value.contains(":") ? Identifier.parse(value) : Identifier.fromNamespaceAndPath(defaultNamespace, value);
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }

    private static Optional<Double> optionalDouble(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsDouble(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json != null && json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }

    public record LlmSettings(boolean enabled, String provider, String chatModel, double temperature, int maxOutputTokens,
                              boolean allowMemoryWrite, boolean allowDynamicOptions) {
        public LlmSettings {
            provider = provider == null || provider.isBlank() ? "fake" : provider.strip();
            chatModel = chatModel == null || chatModel.isBlank() ? "default_chat" : chatModel.strip();
            if (!Double.isFinite(temperature)) temperature = 0.7D;
            maxOutputTokens = Math.max(1, maxOutputTokens);
        }

        public static LlmSettings disabled() {
            return new LlmSettings(false, "disabled", "default_chat", 0.0D, 1, false, false);
        }

        public String debugSummary() {
            return "enabled=" + enabled + ", provider=" + provider + ", model=" + chatModel
                    + ", memory_write=" + allowMemoryWrite + ", dynamic_options=" + allowDynamicOptions;
        }
    }

    public record CharacterProfile(String archetype, String voice, List<String> values, List<String> fears, List<String> speechRules) {
        public CharacterProfile {
            archetype = archetype == null || archetype.isBlank() ? "unspecified" : archetype.strip();
            voice = voice == null || voice.isBlank() ? "plain" : voice.strip();
            values = values == null ? List.of() : List.copyOf(values);
            fears = fears == null ? List.of() : List.copyOf(fears);
            speechRules = speechRules == null ? List.of() : List.copyOf(speechRules);
        }

        public static CharacterProfile empty() {
            return new CharacterProfile("unspecified", "plain", List.of(), List.of(), List.of());
        }
    }

    public record Stance(Optional<Identifier> faction, String defaultAttitudeToPlayer, int trust, int fear, int resentment,
                         List<Identifier> secrets) {
        public Stance {
            faction = faction == null ? Optional.empty() : faction;
            defaultAttitudeToPlayer = defaultAttitudeToPlayer == null || defaultAttitudeToPlayer.isBlank()
                    ? "neutral" : defaultAttitudeToPlayer.strip();
            secrets = secrets == null ? List.of() : List.copyOf(secrets);
        }

        public static Stance empty() {
            return new Stance(Optional.empty(), "neutral", 0, 0, 0, List.of());
        }

        public String debugSummary() {
            return "faction=" + faction.map(Identifier::toString).orElse("-")
                    + ", attitude=" + defaultAttitudeToPlayer
                    + ", trust=" + trust + ", fear=" + fear + ", resentment=" + resentment
                    + ", secrets=" + secrets.size();
        }
    }

    public record Knowledge(List<Identifier> initialPacks, List<ForbiddenFact> forbiddenToRevealUntil) {
        public Knowledge {
            initialPacks = initialPacks == null ? List.of() : List.copyOf(initialPacks);
            forbiddenToRevealUntil = forbiddenToRevealUntil == null ? List.of() : List.copyOf(forbiddenToRevealUntil);
        }

        public static Knowledge empty() {
            return new Knowledge(List.of(), List.of());
        }
    }

    public record ForbiddenFact(String fact, String condition) {
        public ForbiddenFact {
            fact = fact == null ? "" : fact.strip();
            condition = condition == null ? "" : condition.strip();
        }
    }

    public record Promotion(boolean canBeDemoted) {
        public static Promotion defaultPolicy() {
            return new Promotion(false);
        }
    }
}
