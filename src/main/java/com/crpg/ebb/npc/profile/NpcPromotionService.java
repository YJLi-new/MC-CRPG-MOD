package com.crpg.ebb.npc.profile;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.interaction.entity.EntityBindingDefinition;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.story.StoryVarLayer;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class NpcPromotionService {
    public static final String MINOR_NPC_TAG = "ebb.npc.minor";
    public static final NpcTier PROMOTED_TIER = NpcTier.MAJOR_PROMOTED;
    public static final int MAX_PROMOTIONS_PER_WORLD_HOUR = 4;
    public static final long WORLD_HOUR_TICKS = 1_000L;

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
        long gameTime = level.getGameTime();
        int currentCount = promotionCountForWorldHour(state, gameTime);
        if (currentCount >= MAX_PROMOTIONS_PER_WORLD_HOUR) {
            return new PromotionResult(profileId, NpcProfileGenerator.rateLimitedProfileJson(entity, profileId, gameTime, currentCount, MAX_PROMOTIONS_PER_WORLD_HOUR), false, "rate_limited");
        }
        JsonObject generated = generatePromotedProfileJson(entity, firstPlayerUuid, gameTime, reason);
        reservePromotionSlot(state, gameTime);
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
        return NpcProfileGenerator.generatePromotedProfileJson(entity, firstPlayerUuid, gameTime, reason);
    }

    public static long currentWorldHour(long gameTime) {
        return Math.floorDiv(Math.max(0L, gameTime), WORLD_HOUR_TICKS);
    }

    public static String rateLimitKey(long gameTime) {
        return "llm_promotion_hour_" + currentWorldHour(gameTime);
    }

    public static int promotionCountForWorldHour(NarrativeSavedData state, long gameTime) {
        String raw = state.getWorldStoryVariable(StoryVarLayer.MINOR, rateLimitKey(gameTime));
        try {
            return raw == null || raw.isBlank() ? 0 : Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static boolean canPromoteThisWorldHour(NarrativeSavedData state, long gameTime) {
        return promotionCountForWorldHour(state, gameTime) < MAX_PROMOTIONS_PER_WORLD_HOUR;
    }

    private static int reservePromotionSlot(NarrativeSavedData state, long gameTime) {
        return state.addWorldStoryInt(StoryVarLayer.MINOR, rateLimitKey(gameTime), 1);
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
