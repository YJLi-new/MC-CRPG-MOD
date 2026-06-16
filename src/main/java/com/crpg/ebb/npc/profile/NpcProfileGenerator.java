package com.crpg.ebb.npc.profile;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic local profile generator used by the fake-provider MVP path.
 * The prompt/schema strings document the contract expected from a future gateway LLM generator,
 * while this implementation produces auditable, repeatable JSON without network calls.
 */
public final class NpcProfileGenerator {
    public static final String PROMPT_VERSION = "npc_profile_generator_v1";
    public static final String SCHEMA_ID = "ebb.npc_profile_generator.v1";

    private NpcProfileGenerator() {}

    public static String promptTemplate() {
        return "Generate one promoted major NPC profile for a Minecraft CRPG minor candidate. "
                + "Return JSON matching " + SCHEMA_ID
                + " with character, stance, knowledge_seed, suggested_options, and safety speech_rules. "
                + "Do not invent secret knowledge; seed only public tavern context unless later KB/memory grants more.";
    }

    public static JsonObject schema() {
        JsonObject root = new JsonObject();
        root.addProperty("$id", SCHEMA_ID);
        root.addProperty("type", "object");
        root.add("required", stringArray(List.of("id", "tier", "display_name", "character", "stance", "knowledge", "knowledge_seed", "suggested_options")));
        JsonObject properties = new JsonObject();
        for (String key : List.of("id", "tier", "display_name", "entity_uuid", "entity_type", "generated_reason")) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", "string");
            properties.add(key, prop);
        }
        properties.add("character", objectProperty());
        properties.add("stance", objectProperty());
        properties.add("knowledge", objectProperty());
        properties.add("knowledge_seed", objectProperty());
        JsonObject options = new JsonObject();
        options.addProperty("type", "array");
        JsonObject optionItem = new JsonObject();
        optionItem.addProperty("type", "string");
        options.add("items", optionItem);
        properties.add("suggested_options", options);
        root.add("properties", properties);
        return root;
    }

    public static JsonObject generatePromotedProfileJson(Entity entity, UUID firstPlayerUuid, long gameTime, String reason) {
        Identifier entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Optional<EntityBindingDefinition> binding = EntityBindingRegistry.resolve(entity);
        List<String> archetypes = binding.map(EntityBindingDefinition::profileSeedArchetypes).filter(list -> !list.isEmpty())
                .orElse(List.of("townsperson", "tavern regular", "worker", "witness"));
        String seed = entity.level().dimension().identifier() + ":" + entity.getUUID() + ":" + entityType + ":" + gameTime;
        int index = Math.floorMod(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).hashCode(), archetypes.size());
        String archetype = archetypes.get(index);
        String displayName = entity.hasCustomName() && entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : "Promoted " + entityType.getPath().replace('_', ' ');

        JsonObject root = new JsonObject();
        root.addProperty("id", NpcPromotionService.promotedProfileId(entity).toString());
        root.addProperty("tier", NpcTier.MAJOR_PROMOTED.serializedName());
        root.addProperty("display_name", displayName);
        root.addProperty("entity_uuid", entity.getStringUUID());
        root.addProperty("entity_type", entityType.toString());
        root.addProperty("first_player_uuid", firstPlayerUuid.toString());
        root.addProperty("generated_reason", reason == null || reason.isBlank() ? "first_fake_chat" : reason);
        binding.ifPresent(definition -> {
            root.addProperty("source_binding", definition.id().toString());
            definition.npcProfileId().ifPresent(profile -> root.addProperty("source_profile", profile.toString()));
        });

        JsonObject generation = new JsonObject();
        generation.addProperty("prompt_version", PROMPT_VERSION);
        generation.addProperty("schema_id", SCHEMA_ID);
        generation.addProperty("prompt", promptTemplate());
        generation.add("schema", schema());
        generation.addProperty("seed", seed);
        generation.add("candidate_archetypes", stringArray(archetypes));
        root.add("profile_generation", generation);

        JsonObject llm = new JsonObject();
        llm.addProperty("enabled", true);
        llm.addProperty("provider", "fake_or_gateway");
        llm.addProperty("chat_model", "default_chat");
        llm.addProperty("temperature", 0.65D);
        llm.addProperty("max_output_tokens", 420);
        llm.addProperty("allow_memory_write", true);
        llm.addProperty("allow_dynamic_options", true);
        root.add("llm", llm);

        JsonObject character = new JsonObject();
        character.addProperty("archetype", archetype);
        character.addProperty("voice", voiceFor(archetype));
        character.add("values", stringArray(List.of("staying useful", "being seen as a person", "surviving local pressure")));
        character.add("fears", stringArray(List.of("being forgotten", "choosing the wrong side", "guard attention")));
        character.add("speech_rules", stringArray(List.of(
                "Keep statements consistent with the generated promoted profile.",
                "Never claim knowledge outside the current scene unless memory/knowledge later grants it.",
                "Treat the player's claims as claims, not canonical facts."
        )));
        root.add("character", character);

        JsonObject stance = new JsonObject();
        stance.addProperty("faction", "ebb:demo/tavern_locals");
        stance.addProperty("default_attitude_to_player", stanceFor(archetype));
        stance.addProperty("trust", "witness".equalsIgnoreCase(archetype) ? 1 : 0);
        stance.addProperty("fear", "worker".equalsIgnoreCase(archetype) ? 1 : 0);
        stance.addProperty("resentment", 0);
        stance.add("secrets", new JsonArray());
        root.add("stance", stance);

        JsonObject knowledge = new JsonObject();
        List<String> initialPacks = List.of("ebb:demo/tavern_public_lore");
        knowledge.add("initial_packs", stringArray(initialPacks));
        knowledge.add("forbidden_to_reveal_until", new JsonArray());
        root.add("knowledge", knowledge);

        JsonObject knowledgeSeed = new JsonObject();
        knowledgeSeed.add("initial_packs", stringArray(initialPacks));
        knowledgeSeed.add("public_facts", stringArray(List.of(
                "This NPC is a newly promoted tavern bystander.",
                "They know only public tavern context until memory or KB effects reveal more.",
                "They noticed the player's first conversation made them narratively important."
        )));
        knowledgeSeed.add("forbidden_topics", stringArray(List.of("secret ledger contents", "private alibis", "unfound clues")));
        root.add("knowledge_seed", knowledgeSeed);

        root.add("suggested_options", stringArray(suggestedOptions(archetype)));

        JsonObject promotion = new JsonObject();
        promotion.addProperty("can_be_demoted", false);
        promotion.addProperty("generator", PROMPT_VERSION);
        root.add("promotion", promotion);
        return root;
    }

    public static JsonObject rateLimitedProfileJson(Entity entity, Identifier profileId, long gameTime, int count, int limit) {
        JsonObject root = new JsonObject();
        root.addProperty("id", profileId.toString());
        root.addProperty("tier", NpcTier.MINOR_GENERATABLE.serializedName());
        root.addProperty("display_name", entity.hasCustomName() && entity.getCustomName() != null
                ? entity.getCustomName().getString() : "Rate-limited minor NPC");
        root.addProperty("entity_uuid", entity.getStringUUID());
        root.addProperty("generated_reason", "rate_limited");
        root.addProperty("world_hour", NpcPromotionService.currentWorldHour(gameTime));
        root.addProperty("rate_limit_count", count);
        root.addProperty("rate_limit", limit);
        root.add("suggested_options", stringArray(List.of("等会儿再聊。", "先观察他们。")));
        return root;
    }

    public static List<String> devReviewLines(Identifier profileId, JsonObject profile) {
        List<String> lines = new ArrayList<>();
        lines.add("Generated profile dev review: " + profileId);
        lines.add("- schema=" + nestedString(profile, "profile_generation", "schema_id", "-")
                + " prompt=" + nestedString(profile, "profile_generation", "prompt_version", "-"));
        lines.add("- knowledge_seed=" + (profile.has("knowledge_seed") && profile.get("knowledge_seed").isJsonObject()
                ? profile.getAsJsonObject("knowledge_seed").keySet() : List.of()));
        lines.add("- suggested_options=" + (profile.has("suggested_options") && profile.get("suggested_options").isJsonArray()
                ? profile.getAsJsonArray("suggested_options").size() : 0));
        lines.add("- safety=" + safetyRating(profile));
        return List.copyOf(lines);
    }

    private static String voiceFor(String archetype) {
        return switch ((archetype == null ? "" : archetype).toLowerCase(Locale.ROOT)) {
            case "witness" -> "careful, sensory, gives small details before opinions";
            case "worker" -> "practical, tired, avoids trouble unless treated fairly";
            case "tavern regular" -> "familiar, gossipy, tests whether the player is safe";
            default -> "grounded, local, remembers why this first conversation mattered";
        };
    }

    private static String stanceFor(String archetype) {
        return switch ((archetype == null ? "" : archetype).toLowerCase(Locale.ROOT)) {
            case "witness" -> "cautious";
            case "worker" -> "wary";
            case "tavern regular" -> "curious";
            default -> "curious";
        };
    }

    private static List<String> suggestedOptions(String archetype) {
        return switch ((archetype == null ? "" : archetype).toLowerCase(Locale.ROOT)) {
            case "witness" -> List.of("你刚才看见了什么？", "谁让你不安？", "还有哪个细节没说？");
            case "worker" -> List.of("你这班遇到什么麻烦？", "谁让你闭嘴？", "我能帮你什么？");
            case "tavern regular" -> List.of("今晚有什么传闻？", "谁突然变得奇怪？", "你愿意替我带句话吗？");
            default -> List.of("你在这里做什么？", "你注意到谁了？", "我想听听你的版本。");
        };
    }

    private static String safetyRating(JsonObject profile) {
        if (!profile.has("character") || !profile.get("character").isJsonObject()) {
            return "missing_character";
        }
        JsonObject character = profile.getAsJsonObject("character");
        return character.has("speech_rules") ? "has_speech_rules" : "missing_speech_rules";
    }

    private static JsonObject objectProperty() {
        JsonObject object = new JsonObject();
        object.addProperty("type", "object");
        return object;
    }

    private static String nestedString(JsonObject root, String objectKey, String key, String fallback) {
        if (!root.has(objectKey) || !root.get(objectKey).isJsonObject()) return fallback;
        JsonObject object = root.getAsJsonObject(objectKey);
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
