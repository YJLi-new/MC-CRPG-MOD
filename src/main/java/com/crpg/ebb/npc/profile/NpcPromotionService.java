package com.crpg.ebb.npc.profile;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.state.NarrativeSavedData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class NpcPromotionService {
    public static final String MINOR_NPC_TAG = "ebb.npc.minor";

    private NpcPromotionService() {
    }

    public static Optional<PromotionResult> ensurePromotedIfMinor(ServerPlayer player, DialogueSession session) {
        if (session.entityUuid().isEmpty() || !(player.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Entity entity = level.getEntityInAnyDimension(session.entityUuid().get());
        if (entity == null || !isMinorCandidate(entity)) {
            return Optional.empty();
        }
        return Optional.of(ensurePromotedProfile(level, entity, player.getUUID(), "first_fake_chat"));
    }

    public static boolean isMinorCandidate(Entity entity) {
        if (entity.entityTags().contains(MINOR_NPC_TAG)) {
            return true;
        }
        Optional<EntityBindingDefinition> binding = EntityBindingRegistry.resolve(entity);
        if (binding.isPresent() && binding.get().npcTier() == NpcTier.MINOR_GENERATABLE) {
            return true;
        }
        return entity instanceof EbbNpcEntity && entity.entityTags().contains("ebb.npc.minor");
    }

    public static PromotionResult ensurePromotedProfile(ServerLevel level, Entity entity, UUID firstPlayerUuid, String reason) {
        NarrativeSavedData state = NarrativeSavedData.get(level);
        Identifier profileId = promotedProfileId(entity);
        Optional<JsonObject> existing = state.promotedNpcProfile(profileId.toString());
        if (existing.isPresent()) {
            return new PromotionResult(profileId, existing.get(), false, "existing_promoted_major");
        }
        JsonObject generated = generatePromotedProfileJson(entity, firstPlayerUuid, level.getGameTime(), reason);
        state.putPromotedNpcProfile(profileId.toString(), generated);
        state.setWorldNpcState(profileId.toString(), "promoted_major", true);
        return new PromotionResult(profileId, generated, true, "promoted_major");
    }

    public static Identifier promotedProfileId(Entity entity) {
        return EbbMod.id("promoted/" + entity.getUUID().toString().replace("-", ""));
    }

    public static boolean isPromotedMajor(NarrativeSavedData state, Entity entity) {
        return state.hasPromotedNpcProfile(promotedProfileId(entity).toString());
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
        root.addProperty("id", promotedProfileId(entity).toString());
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
        character.addProperty("voice", "grounded, local, remembers why this first conversation mattered");
        character.add("values", stringArray(List.of("staying useful", "being seen as a person", "surviving local pressure")));
        character.add("fears", stringArray(List.of("being forgotten", "choosing the wrong side", "guard attention")));
        character.add("speech_rules", stringArray(List.of(
                "Keep statements consistent with the generated promoted profile.",
                "Never claim knowledge outside the current scene unless memory/knowledge later grants it."
        )));
        root.add("character", character);

        JsonObject stance = new JsonObject();
        stance.addProperty("faction", "ebb:demo/tavern_locals");
        stance.addProperty("default_attitude_to_player", "curious");
        stance.addProperty("trust", 0);
        stance.addProperty("fear", 0);
        stance.addProperty("resentment", 0);
        stance.add("secrets", new JsonArray());
        root.add("stance", stance);

        JsonObject knowledge = new JsonObject();
        knowledge.add("initial_packs", stringArray(List.of("ebb:demo/tavern_public_lore")));
        knowledge.add("forbidden_to_reveal_until", new JsonArray());
        root.add("knowledge", knowledge);

        JsonObject promotion = new JsonObject();
        promotion.addProperty("can_be_demoted", false);
        root.add("promotion", promotion);
        return root;
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    public static String summaryLine(NarrativeSavedData state) {
        return "promoted_npc_profiles=" + state.promotedNpcProfileCount();
    }

    public record PromotionResult(Identifier profileId, JsonObject profileJson, boolean created, String status) {
        public String displayName() {
            return profileJson.has("display_name") ? profileJson.get("display_name").getAsString() : profileId.toString();
        }

        public String debugSummary() {
            return profileId + " status=" + status + " created=" + created
                    + " display='" + displayName() + "' tier="
                    + (profileJson.has("tier") ? profileJson.get("tier").getAsString().toLowerCase(Locale.ROOT) : "-");
        }
    }
}
