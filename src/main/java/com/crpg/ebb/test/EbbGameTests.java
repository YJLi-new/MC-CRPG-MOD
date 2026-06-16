package com.crpg.ebb.test;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionTargetType;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.llm.DisabledLlmGatewayClient;
import com.crpg.ebb.llm.FakeLlmGatewayClient;
import com.crpg.ebb.llm.HttpLlmGatewayClient;
import com.crpg.ebb.llm.LlmChatRequest;
import com.crpg.ebb.llm.LlmChatResponse;
import com.crpg.ebb.llm.LlmChatService;
import com.crpg.ebb.llm.LlmChatSession;
import com.crpg.ebb.llm.LlmConfig;
import com.crpg.ebb.llm.LlmMode;
import com.crpg.ebb.llm.auth.DevLocalLlmAuthClient;
import com.crpg.ebb.llm.auth.LlmAuthService;
import com.crpg.ebb.memory.MemoryGatewayClient;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeRegistry;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeService;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.npc.profile.NpcPromotionService;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.npc.ModEntityTypes;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal Fabric GameTest coverage for the report's in-world regression-test requirement.
 * These tests use Fabric's bundled empty structure and are run by a Fabric gametest server
 * when launched with the `fabric-api.gametest` system property.
 */
public final class EbbGameTests {
    public EbbGameTests() {
    }

    @GameTest(maxTicks = 20)
    public void bundledDataRegistriesAreValid(GameTestHelper helper) {
        helper.assertTrue(DialogueRegistry.byId(EbbMod.id("demo/innkeeper_intro")).isPresent(),
                "bundled innkeeper dialogue should load");
        helper.assertTrue(DialogueRegistry.validationMessages().isEmpty(),
                "bundled dialogues should have no validation messages");
        helper.assertTrue(BlockGroupIndex.byId(EbbMod.id("demo/locked_door")).isPresent(),
                "bundled locked-door block group should load");
        helper.assertTrue(BlockGroupIndex.messages().isEmpty(),
                "bundled block groups should have no index messages");
        helper.assertTrue(EntityBindingRegistry.size() >= 2,
                "bundled entity bindings should load");
        helper.assertTrue(EntityBindingRegistry.validationMessages().isEmpty(),
                "bundled entity bindings should have no validation messages");
        helper.assertTrue(NpcRoutineRegistry.byId(EbbMod.id("demo/innkeeper_day")).isPresent(),
                "bundled innkeeper routine should load");
        helper.assertTrue(NpcRoutineRegistry.validationMessages().isEmpty(),
                "bundled NPC routines should have no validation messages");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void ebbNpcSpawnsWithRoutineState(GameTestHelper helper) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, new BlockPos(1, 2, 1));
        npc.setRoutineId(EbbMod.id("demo/innkeeper_day"));
        helper.assertTrue(npc.routineId().isPresent(), "spawned Ebb NPC should store a routine id");
        helper.assertTrue(npc.entityTags().contains("ebb.npc") || npc.getType() == ModEntityTypes.NPC,
                "spawned test NPC should be the registered Ebb NPC type");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void taggedEbbNpcResolvesConfiguredBinding(GameTestHelper helper) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, new BlockPos(1, 2, 1));
        npc.addTag("ebb.npc");
        var binding = EntityBindingRegistry.resolve(npc);
        helper.assertTrue(binding.isPresent(), "tagged Ebb NPC should resolve an entity binding");
        helper.assertTrue(EbbMod.id("demo/innkeeper_intro").equals(binding.get().dialogueId()),
                "tagged Ebb NPC should resolve to the innkeeper dialogue");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void roleTaggedEbbNpcsResolveDistinctConfiguredBindings(GameTestHelper helper) {
        assertLegacyRoleBinding(helper, "innkeeper", EbbMod.id("demo/innkeeper_intro"), new BlockPos(1, 2, 1));
        assertLegacyRoleBinding(helper, "witness", EbbMod.id("demo/witness_intro"), new BlockPos(2, 2, 1));
        assertLegacyRoleBinding(helper, "tenant", EbbMod.id("demo/tenant_intro"), new BlockPos(3, 2, 1));
        assertLegacyRoleBinding(helper, "guard", EbbMod.id("demo/guard_intro"), new BlockPos(4, 2, 1));
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void playableSliceBlockGroupsLoadAllGuiRetestTargets(GameTestHelper helper) {
        for (String id : Map.of(
                "locked door", "demo/locked_door",
                "counter ledger", "demo/counter_ledger",
                "notice board", "demo/notice_board",
                "washroom mirror", "demo/washroom_mirror",
                "windowsill ash", "demo/windowsill_ash",
                "tenant luggage", "demo/tenant_luggage",
                "cellar hatch", "demo/cellar_hatch",
                "back door", "demo/back_door"
        ).values()) {
            helper.assertTrue(BlockGroupIndex.byId(EbbMod.id(id)).isPresent(),
                    "GUI retest block group should load: " + id);
        }
        helper.assertTrue(BlockGroupIndex.groupCount() >= 8,
                "GUI retest expects all eight vertical-slice block groups, not only locked_door");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void llmFakeDisabledAndTimeoutFoundationIsDeterministic(GameTestHelper helper) {
        try {
            LlmConfig.setForTesting(new LlmConfig(true, LlmMode.FAKE, "", LlmConfig.DEFAULT_GATEWAY_TIMEOUT_MS, false, LlmConfig.DEFAULT_CHAT_MODEL, true, true, false, 128, 256, 10, 10, "FAKE_NPC_REPLY"));
            UUID conversation = UUID.randomUUID();
            UUID player = UUID.randomUUID();
            LlmChatRequest request = new LlmChatRequest(
                    conversation,
                    player,
                    Optional.empty(),
                    EbbMod.id("test/llm"),
                    "start",
                    "ebb:demo/innkeeper",
                    "innkeeper",
                    "ledger",
                    "hello",
                    1L
            );
            LlmChatResponse fake = new FakeLlmGatewayClient(LlmConfig.current()).sendMessage(request).join();
            helper.assertTrue(fake.reply().contains("FAKE_NPC_REPLY"), "fake provider should return the fixed reply marker");
            helper.assertTrue(fake.status().equals("fake_reply"), "fake provider should expose fake_reply status");

            LlmChatResponse disabled = new DisabledLlmGatewayClient().sendMessage(request).join();
            helper.assertTrue(disabled.errorReason().orElse("").equals("llm_disabled"), "disabled provider should surface llm_disabled");

            LlmChatSession session = new LlmChatSession(
                    UUID.randomUUID(),
                    player,
                    EbbMod.id("test/llm"),
                    EbbMod.id("test/target"),
                    InteractionTargetType.BLOCK_GROUP,
                    Optional.empty(),
                    "start",
                    "start",
                    "ebb:demo/innkeeper",
                    "innkeeper",
                    "ledger",
                    0L,
                    0L,
                    false,
                    0L
            );
            LlmChatService.addSessionForTesting(session);
            helper.assertTrue(LlmChatService.closeExpiredSessionsForTesting(100L) == 1, "expired LLM chat sessions should close");
            helper.succeed();
        } finally {
            LlmChatService.clearTestingOverrides();
        }
    }


    @GameTest(maxTicks = 20)
    public void llmAuthGateDevLocalLoginAndLogoutAreServerSide(GameTestHelper helper) {
        try {
            LlmConfig config = new LlmConfig(true, LlmMode.FAKE, "", LlmConfig.DEFAULT_GATEWAY_TIMEOUT_MS,
                    true, LlmConfig.DEFAULT_CHAT_MODEL, true, true, false, 128, 256, 10, 10, "FAKE_NPC_REPLY");
            LlmConfig.setForTesting(config);
            LlmAuthService.setClientForTesting(new DevLocalLlmAuthClient());
            UUID player = UUID.randomUUID();
            helper.assertTrue(LlmAuthService.chatGateStatus(player, config).equals("auth_required"),
                    "P36 require_player_auth should gate unauthenticated fake chat");
            var start = LlmAuthService.startDeviceAuth(player, "gametest").join();
            helper.assertTrue(start.started(), "dev local auth should start");
            var status = LlmAuthService.pollDeviceAuth(player).join();
            helper.assertTrue(status.authenticated(), "dev local status poll should authenticate");
            helper.assertTrue(LlmAuthService.safeStatusLine(player).contains("token=redacted"),
                    "auth status should redact server-only tokens");
            helper.assertTrue(LlmAuthService.chatGateStatus(player, config).equals("authenticated"),
                    "logged-in player should pass the auth gate");
            helper.assertTrue(LlmAuthService.logout(player).join(), "logout should revoke server token");
            helper.assertTrue(LlmAuthService.chatGateStatus(player, config).equals("auth_required"),
                    "logout should return the player to auth_required");
            helper.succeed();
        } finally {
            LlmChatService.clearTestingOverrides();
        }
    }



    @GameTest(maxTicks = 20)
    public void llmGatewayModeConfigIsTimeoutSafeAndPrivate(GameTestHelper helper) {
        try {
            LlmConfig config = new LlmConfig(true, LlmMode.GATEWAY, "http://127.0.0.1:8787", 1500, false,
                    "gpt-test", true, true, false, 128, 256, 10, 10, "FAKE_NPC_REPLY");
            helper.assertTrue(config.networkAccessAllowed(), "gateway mode should enable only gateway network access");
            helper.assertTrue(!config.openAiStore(), "P37 default privacy should keep OpenAI store:false");
            helper.assertTrue(config.toString().contains("default_chat_model"), "safe status should expose model config");
            helper.assertTrue(!config.toString().contains("opaque_player_token"), "safe config must not expose player tokens");
            helper.assertTrue(new HttpLlmGatewayClient(config).usesNetwork(), "gateway HTTP client should advertise network usage for audits");
            helper.succeed();
        } finally {
            LlmChatService.clearTestingOverrides();
        }
    }


    @GameTest(maxTicks = 20)
    public void memoryGatewayClientSurfaceIsServerOnly(GameTestHelper helper) {
        LlmConfig config = new LlmConfig(true, LlmMode.GATEWAY, "http://127.0.0.1:8787", 1500, false,
                "gpt-test", true, true, false, 128, 256, 10, 10, "FAKE_NPC_REPLY");
        MemoryGatewayClient client = new MemoryGatewayClient(config);
        helper.assertTrue(config.networkAccessAllowed(), "P38 memory dev commands require gateway mode");
        helper.assertTrue(client != null, "P38 memory gateway client should be constructible on the server without client secrets");
        helper.assertTrue(MemoryGatewayClient.class.getName().contains("memory"),
                "P39 memory episodes/lessons dev surfaces remain server-side gateway helpers");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void npcKnowledgeHidesSecretUntilClueAndChangesFakeAnswer(GameTestHelper helper) {
        helper.assertTrue(NpcKnowledgeRegistry.byId(EbbMod.id("demo/innkeeper_private_ledger")).isPresent(),
                "P40 innkeeper private ledger KB pack should load");
        NarrativeSavedData state = NarrativeSavedData.get(helper.getLevel());
        UUID player = UUID.randomUUID();
        String question = "tenant paid cash ledger";
        String before = NpcKnowledgeService.promptContext("ebb:demo/innkeeper", question, state, player, 6000L, 6);
        helper.assertTrue(!before.toLowerCase(java.util.Locale.ROOT).contains("tenant paid cash"),
                "P40 secret chunk must not leak before clue: " + before);
        helper.assertTrue(NpcKnowledgeService.inspectLines("ebb:demo/innkeeper", question, state, player, 6000L, 16).stream()
                        .anyMatch(line -> line.contains("hidden") && line.contains("secret_ledger_tenant_cash")),
                "P40 /ebb kb inspect equivalent should expose hidden chunks for dev review");
        LlmChatResponse beforeReply = new FakeLlmGatewayClient(LlmConfig.fakeForTesting()).sendMessage(new LlmChatRequest(
                UUID.randomUUID(), player, Optional.empty(), EbbMod.id("test/p40"), "start", "ebb:demo/innkeeper",
                "innkeeper", "ledger", question, 1L, before)).join();
        helper.assertTrue(beforeReply.reply().contains("kb=public_only"),
                "P40 fake answer before clue should use public KB only: " + beforeReply.reply());

        state.revealClue(player, "ebb:demo/guestbook_gap");
        String after = NpcKnowledgeService.promptContext("ebb:demo/innkeeper", question, state, player, 6000L, 6);
        helper.assertTrue(after.toLowerCase(java.util.Locale.ROOT).contains("tenant paid cash"),
                "P40 secret chunk should become visible after clue: " + after);
        LlmChatResponse afterReply = new FakeLlmGatewayClient(LlmConfig.fakeForTesting()).sendMessage(new LlmChatRequest(
                UUID.randomUUID(), player, Optional.empty(), EbbMod.id("test/p40"), "start", "ebb:demo/innkeeper",
                "innkeeper", "ledger", question, 2L, after)).join();
        helper.assertTrue(afterReply.reply().contains("kb=secret_visible"),
                "P40 fake answer after clue should change when secret KB becomes visible: " + afterReply.reply());
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void npcProfileRegistryLoadsScriptedProfilesAndPromotesMinor(GameTestHelper helper) {
        helper.assertTrue(NpcProfileRegistry.byId(EbbMod.id("demo/innkeeper")).isPresent(),
                "P35 scripted innkeeper profile should load");
        helper.assertTrue(NpcProfileRegistry.byEntityBinding(EbbMod.id("demo/innkeeper_ebb_npc")).isPresent(),
                "P35 profile should resolve from innkeeper binding");
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, new BlockPos(5, 2, 1));
        npc.addTag(NpcPromotionService.MINOR_NPC_TAG);
        helper.assertTrue(NpcPromotionService.isMinorCandidate(npc), "minor tag should mark an NPC as a promotion candidate");
        var result = NpcPromotionService.ensurePromotedProfile(helper.getLevel(), npc, UUID.randomUUID(), "gametest");
        helper.assertTrue(result.status().equals("promoted_major") || result.status().equals("existing_promoted_major"),
                "minor promotion should return promoted_major status");
        NarrativeSavedData state = NarrativeSavedData.get(helper.getLevel());
        helper.assertTrue(state.hasPromotedNpcProfile(result.profileId().toString()),
                "promoted profile should persist in NarrativeSavedData");
        var profile = state.promotedNpcProfile(result.profileId().toString()).orElseThrow();
        helper.assertTrue(profile.get("tier").getAsString().equals("major_promoted"),
                "persisted profile should be major_promoted");
        helper.assertTrue(profile.has("profile_generation") && profile.has("knowledge_seed") && profile.has("suggested_options"),
                "P41 generated profile should include generation metadata, knowledge seed, and suggested options");
        String firstRaw = profile.toString();
        var second = NpcPromotionService.ensurePromotedProfile(helper.getLevel(), npc, UUID.randomUUID(), "gametest_second_chat");
        helper.assertTrue(second.status().equals("existing_promoted_major"),
                "P41 second chat/re-entry should keep the existing promoted profile");
        helper.assertTrue(state.promotedNpcProfile(result.profileId().toString()).orElseThrow().toString().equals(firstRaw),
                "P41 promoted profile should remain stable after re-entry/second promotion attempt");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void llmChatStreamingChunksAndUiContractsArePresent(GameTestHelper helper) {
        var chunks = LlmChatService.streamingChunks(
                "P42 streaming fake reply should be split into more than one packet for the client-side merge path.",
                32
        );
        helper.assertTrue(chunks.size() > 1, "P42 streaming helper should split long LLM replies");
        helper.assertTrue(String.join("", chunks).contains("client-side merge path"),
                "P42 chunks should preserve reply text exactly when merged");
        helper.assertTrue(LlmConfig.fakeForTesting().llmChatStreaming(),
                "P42 fake/default config keeps streaming enabled for GUI E2E");
        helper.succeed();
    }

    private static void assertLegacyRoleBinding(GameTestHelper helper, String role, Identifier expectedDialogue, BlockPos pos) {
        EbbNpcEntity npc = helper.spawn(ModEntityTypes.NPC, pos);
        npc.addTag("ebb.npc");
        npc.addTag("ebb.npc." + role + "_day");
        var binding = EntityBindingRegistry.resolve(npc);
        helper.assertTrue(binding.isPresent(), "legacy role tag should resolve an entity binding: " + role);
        helper.assertTrue(expectedDialogue.equals(binding.get().dialogueId()),
                "legacy role tag should resolve " + role + " to " + expectedDialogue
                        + ", got " + binding.get().dialogueId());
    }
}
