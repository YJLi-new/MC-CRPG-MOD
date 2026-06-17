package com.crpg.ebb;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeDefinition;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.chime.ChimeResolver;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.conflict.ConflictService;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueEffect;
import com.crpg.ebb.dialogue.DialogueNodeType;
import com.crpg.ebb.dialogue.ChoiceType;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.dialogue.RollMode;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionRaycastPolicy;
import com.crpg.ebb.interaction.InteractionTargetType;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.journal.JournalEntryRegistry;
import com.crpg.ebb.journal.JournalService;
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
import com.crpg.ebb.llm.auth.LlmAuthToken;
import com.crpg.ebb.memory.MemoryGatewayClient;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeRegistry;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeService;
import com.crpg.ebb.network.dialogue.RollResultPayload;
import com.crpg.ebb.npc.profile.NpcProfileGenerator;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.npc.profile.NpcPromotionService;
import com.crpg.ebb.npc.profile.NpcTier;
import com.crpg.ebb.network.dialogue.VisibleDialogueChoice;
import com.crpg.ebb.quest.QuestBranchRegistry;
import com.crpg.ebb.quest.TakeRootService;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.registry.ModCommands;
import com.crpg.ebb.routine.NpcRoutineDefinition;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.story.StoryVarLayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeepResearchDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void bundledRegistriesLoadWithoutValidationMessages() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        AttributeRegistry.rebuild(load(bundled.resolve("attributes")));
        BlockGroupIndex.rebuild(load(bundled.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(bundled.resolve("npc_routines")));
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));
        QuestBranchRegistry.rebuild(load(bundled.resolve("quest_branches")));
        FeatRegistry.rebuild(load(bundled.resolve("feats")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));
        JournalEntryRegistry.rebuild(load(bundled.resolve("journal_entries")));
        RelationshipRegistry.rebuild(load(bundled.resolve("relationships")));
        InvestigationRegistry.rebuildClues(load(bundled.resolve("clues")));
        InvestigationRegistry.rebuildScenes(load(bundled.resolve("investigation_scenes")));
        ConflictRegistry.rebuild(load(bundled.resolve("conflicts")));

        assertTrue(DialogueRegistry.size() >= 3, "bundled dialogues should load");
        assertEquals(8, AttributeRegistry.size(), "DND-8 attributes should load");
        assertTrue(BlockGroupIndex.groupCount() >= 8, "bundled vertical-slice block groups should load");
        assertTrue(EntityBindingRegistry.size() >= 2, "bundled entity bindings should load");
        assertTrue(NpcRoutineRegistry.size() >= 5, "bundled routines should load");
        assertTrue(NpcProfileRegistry.size() >= 6, "bundled P35 NPC profiles should load");
        assertTrue(QuestBranchRegistry.size() >= 2, "bundled quest branches should load");
        assertTrue(FeatRegistry.size() >= 4, "bundled feats should load");
        assertTrue(ChimeRegistry.size() >= 8, "bundled P26 chimes should load all eight voices");
        assertTrue(JournalEntryRegistry.size() >= 4, "bundled journal entries should load");
        assertTrue(RelationshipRegistry.size() >= 4, "bundled relationships should load");
        assertTrue(InvestigationRegistry.clueCount() >= 5, "bundled clues should load");
        assertTrue(InvestigationRegistry.sceneCount() >= 1, "bundled investigation scenes should load");
        assertTrue(ConflictRegistry.size() >= 1, "bundled conflicts should load");
        assertEquals(0, DialogueRegistry.validationMessages().size(), "dialogues should be clean");
        assertEquals(0, AttributeRegistry.validationMessages().size(), "attributes should be clean");
        assertEquals(0, BlockGroupIndex.messages().size(), "block groups should be clean");
        assertEquals(0, EntityBindingRegistry.validationMessages().size(), "entity bindings should be clean");
        assertEquals(0, NpcRoutineRegistry.validationMessages().size(), "routines should be clean");
        assertEquals(0, NpcProfileRegistry.validationMessages().size(), "NPC profiles should be clean");
        assertEquals(0, QuestBranchRegistry.validationMessages().size(), "quest branches should be clean");
        assertEquals(0, FeatRegistry.validationMessages().size(), "feats should be clean");
        assertEquals(0, ChimeRegistry.validationMessages().size(), "chimes should be clean");
        assertEquals(0, JournalEntryRegistry.validationMessages().size(), "journal entries should be clean");
        assertEquals(0, RelationshipRegistry.validationMessages().size(), "relationships should be clean");
        assertEquals(0, InvestigationRegistry.validationMessages().size(), "investigations should be clean");
        assertEquals(0, ConflictRegistry.validationMessages().size(), "conflicts should be clean");
    }

    @Test
    void checkedChoicesMustFailForward() {
        JsonObject invalidNoFailure = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[{"id":"bad","type":"action","text":"bad","check":{"attribute":"charisma","dc":10,"success":"ok"}}]},"ok":{"text":"ok"}}}
                """).getAsJsonObject();
        java.util.ArrayList<String> messages = new java.util.ArrayList<>();
        assertTrue(DialogueDefinition.parse(Identifier.parse("ebb:test/no_failure"), invalidNoFailure, messages).isEmpty());
        assertTrue(messages.stream().anyMatch(message -> message.contains("failing forward")), messages::toString);
    }

    @Test
    void hiddenDcAndHiddenRollAffectPlayerFacingSummaries() {
        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"hidden","type":"action","text":"hidden","next":"done","check":{"attribute":"empathy","dc":15,"hidden_dc":true,"hidden_roll":true,"success":"done","failure":"done"}}
                ]},"done":{"text":"done"}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/hidden_roll"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        var choice = definition.node("start").orElseThrow().choice("hidden").orElseThrow();

        assertFalse(choice.check().orElseThrow().showDc());
        assertFalse(choice.check().orElseThrow().showRoll());
        assertEquals("empathy DC ? hidden roll", VisibleDialogueChoice.fromChoice(choice).checkSummary().orElseThrow());

        RollResultPayload result = new RollResultPayload("empathy", 15, 12, 3, 15, true, false, "success", false, false);
        assertEquals("empathy hidden roll vs hidden DC (success)", result.summary());
    }

    @Test
    void variablesTraitsThoughtsAndUnlockEffectsPersistInNarrativeState() {
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        parseEffect("{\"type\":\"set_var\",\"key\":\"clerk_attitude\",\"value\":\"intimidated\"}").apply(state, player);
        parseEffect("{\"type\":\"add_trait\",\"key\":\"forger\"}").apply(state, player);
        parseEffect("{\"type\":\"add_thought\",\"key\":\"tired_clerk\"}").apply(state, player);
        parseEffect("{\"type\":\"unlock_retry\",\"key\":\"office_backdoor_check\"}").apply(state, player);

        assertEquals("intimidated", state.getPlayerVariable(player, "clerk_attitude"));
        assertTrue(state.hasPlayerFlag(player, "trait:forger"));
        assertTrue(state.hasPlayerFlag(player, "thought:tired_clerk"));
        assertTrue(state.hasPlayerFlag(player, "unlock:office_backdoor_check"));
    }

    @Test
    void storyVariablesSupportBranchMajorMinorEffectsConditionsAndCodec() {
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        parseEffect("{\"type\":\"set_story_var\",\"layer\":\"branch\",\"key\":\"tavern_route\",\"value\":\"public\"}").apply(state, player);
        parseEffect("{\"type\":\"add_story_int\",\"layer\":\"major\",\"key\":\"innkeeper_trust\",\"amount\":2}").apply(state, player);
        parseEffect("{\"type\":\"set_story_var\",\"layer\":\"minor\",\"key\":\"met_innkeeper\",\"value\":true}").apply(state, player);

        assertEquals("public", state.getPlayerStoryVariable(player, StoryVarLayer.BRANCH, "tavern_route"));
        assertEquals("2", state.getPlayerStoryVariable(player, StoryVarLayer.MAJOR, "innkeeper_trust"));
        assertEquals("true", state.getPlayerStoryVariable(player, StoryVarLayer.MINOR, "met_innkeeper"));

        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"branch","type":"thought","text":"branch","conditions":[{"type":"story_var","layer":"branch","key":"tavern_route","value":"public"}]},
                  {"id":"major","type":"thought","text":"major","conditions":[{"type":"story_var","layer":"major","key":"innkeeper_trust","min":2}]},
                  {"id":"minor","type":"thought","text":"minor","conditions":[{"type":"story_var","layer":"minor","key":"met_innkeeper","value":true}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/story_vars"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        assertTrue(definition.node("start").orElseThrow().choice("branch").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("major").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("minor").orElseThrow().conditions().getFirst().matches(state, player));

        var encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();
        NarrativeSavedData restored = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();
        assertEquals("public", restored.getPlayerStoryVariable(player, StoryVarLayer.BRANCH, "tavern_route"));
        assertEquals("2", restored.getPlayerStoryVariable(player, StoryVarLayer.MAJOR, "innkeeper_trust"));
        assertEquals("true", restored.getPlayerStoryVariable(player, StoryVarLayer.MINOR, "met_innkeeper"));
    }

    @Test
    void narrativeVariablesRoundTripThroughSavedDataCodec() {
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        state.setPlayerVariable(player, "case_note", "door remembers");
        state.setWorldVariable("weather", "wet ash");
        state.setWorldStoryVariable(StoryVarLayer.BRANCH, "tavern_route", "quiet");
        state.addWorldStoryInt(StoryVarLayer.MAJOR, "bell_count", 3);
        state.setPlayerFlag(player, "trait:detective", true);
        state.setWorldFlag("demo.world_seen_rain", true);

        var encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();
        NarrativeSavedData restored = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();

        assertEquals("door remembers", restored.getPlayerVariable(player, "case_note"));
        assertEquals("wet ash", restored.getWorldVariable("weather"));
        assertEquals("quiet", restored.getWorldStoryVariable(StoryVarLayer.BRANCH, "tavern_route"));
        assertEquals("3", restored.getWorldStoryVariable(StoryVarLayer.MAJOR, "bell_count"));
        assertTrue(restored.hasPlayerFlag(player, "trait:detective"));
        assertTrue(restored.hasWorldFlag("demo.world_seen_rain"));
        assertEquals(NarrativeSavedData.CURRENT_SCHEMA_VERSION, restored.schemaVersion());
    }

    @Test
    void questTakeRootGrantsFeatsAndFeatModifiersApplyToChecks() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        QuestBranchRegistry.rebuild(load(bundled.resolve("quest_branches")));
        FeatRegistry.rebuild(load(bundled.resolve("feats")));

        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        TakeRootService.completeBranch(state, player, "ebb:demo/tavern_public").orElseThrow();

        assertEquals("take_rooted", state.getQuestState(player, "ebb:demo/tavern_public"));
        assertTrue(state.hasFeat(player, "ebb:demo/tavern_authority"));
        assertTrue(state.hasFeat(player, "ebb:demo/cheap_empathy"));
        assertTrue(state.isFeatActive(player, "ebb:demo/tavern_authority"));
        assertTrue(FeatRegistry.totalCheckModifier(state, player, "charisma") >= 2);

        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"feat","type":"dialogue","text":"feat","conditions":[{"type":"has_feat","id":"ebb:demo/tavern_authority"}]},
                  {"id":"quest","type":"dialogue","text":"quest","conditions":[{"type":"quest_state","id":"ebb:demo/tavern_public","value":"take_rooted"}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/quest_feat"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        assertTrue(definition.node("start").orElseThrow().choice("feat").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("quest").orElseThrow().conditions().getFirst().matches(state, player));
    }

    @Test
    void majorQuestCannotTakeRootTwice() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        QuestBranchRegistry.rebuild(load(bundled.resolve("quest_branches")));
        FeatRegistry.rebuild(load(bundled.resolve("feats")));

        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        String first = TakeRootService.completeBranch(state, player, "ebb:demo/tavern_public").orElseThrow();
        String second = TakeRootService.completeBranch(state, player, "ebb:demo/tavern_public").orElseThrow();

        assertTrue(first.contains("take_root:"), "first completion should include Take Root text");
        assertTrue(second.contains("quest_already_take_rooted:ebb:demo/tavern_public"), "second completion should be idempotent");
        assertEquals("take_rooted", state.getQuestState(player, "ebb:demo/tavern_public"));
        assertEquals(2, state.unlockedFeatIds(player).size(), "second Take Root must not duplicate feat grants");
        assertEquals(2, state.activeFeatIds(player).size(), "second Take Root must not duplicate active slots");
    }

    @Test
    void chimesResolveFromBuildAndUnlockPassiveInsightPath() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        AttributeRegistry.rebuild(load(bundled.resolve("attributes")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));

        DialogueDefinition innkeeper = DialogueRegistry.byId(Identifier.parse("ebb:demo/innkeeper_intro")).orElseThrow();
        var start = innkeeper.node("start").orElseThrow();
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        assertTrue(ChimeResolver.resolve(innkeeper, start, state, player).isEmpty(), "no default build chime before spending attributes");

        state.setAttribute(player, "charisma", 1);
        String chime = ChimeResolver.resolve(innkeeper, start, state, player).orElseThrow();
        assertTrue(chime.contains("Rhetoric"), chime);
        assertTrue(state.hasPlayerFlag(player, "chime:ebb:demo/rhetoric"));
        assertTrue(start.choice("rhetoric_insight").orElseThrow().conditions().getFirst().matches(state, player));
    }

    @Test
    void p26ChimeSetCoversEightAttributesAndActiveThoughtRoutes() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        AttributeRegistry.rebuild(load(bundled.resolve("attributes")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));

        assertEquals(Set.of(
                "strength",
                "dexterity",
                "constitution",
                "intelligence",
                "wisdom",
                "charisma",
                "perception",
                "luck"
        ), ChimeRegistry.definitions().values().stream()
                .map(ChimeDefinition::sourceAttribute)
                .collect(java.util.stream.Collectors.toSet()));

        DialogueDefinition innkeeper = DialogueRegistry.byId(Identifier.parse("ebb:demo/innkeeper_intro")).orElseThrow();
        var start = innkeeper.node("start").orElseThrow();
        Map<String, String> chimeAttribute = new LinkedHashMap<>();
        chimeAttribute.put("dread", "luck");
        chimeAttribute.put("empathy", "wisdom");
        chimeAttribute.put("endurance", "constitution");
        chimeAttribute.put("finesse", "dexterity");
        chimeAttribute.put("force", "strength");
        chimeAttribute.put("instinct", "perception");
        chimeAttribute.put("logic", "intelligence");
        chimeAttribute.put("rhetoric", "charisma");

        for (Map.Entry<String, String> entry : chimeAttribute.entrySet()) {
            Identifier chimeId = Identifier.parse("ebb:demo/" + entry.getKey());
            ChimeDefinition definition = ChimeRegistry.byId(chimeId).orElseThrow();
            assertFalse(definition.toneGuide().isBlank(), chimeId + " should define a tone guide");
            assertFalse(definition.triggerTags().isEmpty(), chimeId + " should define trigger tags");
            assertTrue(definition.cooldownTicks() > 0, chimeId + " should tune cooldown");
            assertTrue(definition.oneShotPerNode(), chimeId + " should be one-shot per node");
            assertFalse(definition.activeThoughtIds().isEmpty(), chimeId + " should link an active thought route");

            NarrativeSavedData state = new NarrativeSavedData();
            UUID player = UUID.randomUUID();
            state.setAttribute(player, entry.getValue(), 1);
            assertTrue(ChimeResolver.explain(innkeeper, start, state, player, 1000L).stream()
                    .anyMatch(line -> line.contains(chimeId.toString()) && line.contains("ready")),
                    "dev chime explain should show why " + chimeId + " can trigger");
            String passive = ChimeResolver.resolve(innkeeper, start, state, player, 1000L).orElseThrow();
            assertTrue(passive.contains(definition.name()), passive);
            assertTrue(state.hasPlayerFlag(player, "chime:" + chimeId));

            String choiceId = "rhetoric".equals(entry.getKey()) ? "rhetoric_insight" : entry.getKey() + "_chime_thought";
            var routeChoice = start.choice(choiceId).orElseThrow();
            assertTrue(routeChoice.conditions().getFirst().matches(state, player), "active route should unlock for " + chimeId);
            assertTrue(routeChoice.effects().stream().anyMatch(effect -> effect.type() == DialogueEffect.EffectType.ADD_THOUGHT),
                    "active route should add a thought for " + chimeId);
            routeChoice.effects().forEach(effect -> effect.apply(state, player));
            assertTrue(state.hasPlayerFlag(player, "thought:" + definition.activeThoughtIds().getFirst()));
        }
    }

    @Test
    void chimeCooldownPreventsRepeatedPassiveInsertsAcrossNodes() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        AttributeRegistry.rebuild(load(bundled.resolve("attributes")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));

        JsonObject synthetic = JsonParser.parseString("""
                {"start":"a","nodes":{
                  "a":{"text":"a","chime_tags":["innkeeper.read"],"choices":[{"id":"to_b","type":"dialogue","text":"b","next":"b"}]},
                  "b":{"text":"b","chime_tags":["innkeeper.read"],"choices":[{"id":"to_a","type":"dialogue","text":"a","next":"a"}]}
                }}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/chime_cooldown"), synthetic, new java.util.ArrayList<>()).orElseThrow();
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        state.setAttribute(player, "strength", 1);

        assertTrue(ChimeResolver.resolve(definition, definition.node("a").orElseThrow(), state, player, 1000L).orElseThrow().contains("Force"));
        assertTrue(ChimeResolver.explain(definition, definition.node("b").orElseThrow(), state, player, 1050L).stream()
                .anyMatch(line -> line.contains("ebb:demo/force") && line.contains("skip:cooldown_remaining_")),
                "dev chime explain should show cooldown skip reasons");
        assertTrue(ChimeResolver.resolve(definition, definition.node("b").orElseThrow(), state, player, 1050L).isEmpty(),
                "same voice should respect cooldown even on a different node");
        assertTrue(ChimeResolver.resolve(definition, definition.node("b").orElseThrow(), state, player, 1300L).orElseThrow().contains("Force"),
                "voice should be available again after cooldown on an unseen node");
    }

    @Test
    void journalEntriesCanBeUnlockedDisplayedAndGateChoices() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        JournalEntryRegistry.rebuild(load(bundled.resolve("journal_entries")));
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();

        parseEffect("{\"type\":\"add_journal_entry\",\"id\":\"ebb:demo/door_scratches\"}").apply(state, player);
        assertTrue(state.hasJournalEntry(player, "ebb:demo/door_scratches"));
        assertTrue(JournalService.addEntry(state, player, "ebb:demo/door_scratches").orElseThrow().contains("clue_gained"));

        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"clue","type":"thought","text":"clue","conditions":[{"type":"has_clue","id":"ebb:demo/door_scratches"}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/journal"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        assertTrue(definition.node("start").orElseThrow().choice("clue").orElseThrow().conditions().getFirst().matches(state, player));
    }

    @Test
    void relationshipsNpcStateAndTimeWindowConditionsWork() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        RelationshipRegistry.rebuild(load(bundled.resolve("relationships")));
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();

        parseEffect("{\"type\":\"add_relation\",\"relation\":\"ebb:demo/innkeeper\",\"amount\":2}").apply(state, player);
        parseEffect("{\"type\":\"set_npc_state\",\"npc\":\"ebb:demo/innkeeper\",\"tag\":\"guarded\"}").apply(state, player);
        state.setWorldNpcState("ebb:demo/guard", "challenged", true);

        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"relation","type":"dialogue","text":"relation","conditions":[{"type":"relation_at_least","relation":"ebb:demo/innkeeper","min":2}]},
                  {"id":"npc","type":"dialogue","text":"npc","conditions":[{"type":"npc_state","npc":"ebb:demo/innkeeper","tag":"guarded"}]},
                  {"id":"world_npc","type":"dialogue","text":"world","conditions":[{"type":"npc_state","scope":"world","npc":"ebb:demo/guard","tag":"challenged"}]},
                  {"id":"night","type":"thought","text":"night","conditions":[{"type":"time_window","start":12000,"end":24000}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/relationships"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        assertTrue(definition.node("start").orElseThrow().choice("relation").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("npc").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("world_npc").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("night").orElseThrow().conditions().getFirst().matches(state, player, 18000L));
        assertFalse(definition.node("start").orElseThrow().choice("night").orElseThrow().conditions().getFirst().matches(state, player, 6000L));

        var encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();
        NarrativeSavedData restored = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();
        assertEquals(2, restored.getRelation(player, "ebb:demo/innkeeper"));
        assertTrue(restored.hasPlayerNpcState(player, "ebb:demo/innkeeper", "guarded"));
        assertTrue(restored.hasWorldNpcState("ebb:demo/guard", "challenged"));
    }

    @Test
    void p27RoutineValidationAndConversationAnimationNamesAreExplicit() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        NpcRoutineRegistry.rebuild(load(bundled.resolve("npc_routines")));
        assertEquals(0, NpcRoutineRegistry.validationMessages().size(), "bundled routines should pass P27 validation");
        assertTrue(NpcRoutineDefinition.ALLOWED_ACTIONS.contains("walk_path"));
        assertTrue(NpcRoutineDefinition.ALLOWED_ANIMATIONS.contains("talk"));
        assertTrue(NpcRoutineDefinition.ALLOWED_ANIMATIONS.contains("think"));
        assertTrue(NpcRoutineDefinition.ALLOWED_ANIMATIONS.contains("dismiss"));
        assertTrue(NpcRoutineDefinition.ALLOWED_ANIMATIONS.contains("nervous_idle"));
        assertTrue(NpcRoutineDefinition.ALLOWED_POSES.contains("conversation"));

        java.util.ArrayList<String> messages = new java.util.ArrayList<>();
        JsonObject invalid = JsonParser.parseString("""
                {"steps":[
                  {"time":[0,1000],"action":"moonwalk","pos":[0,64,0]},
                  {"time":[1000,2000],"action":"walk_path","path":[[0,64,0]],"animation":"idle"},
                  {"time":[2000,3000],"action":"stand","pos":[0,64,0],"pose":"impossible_pose"},
                  {"time":[3000,4000],"action":"play_animation","pos":[0,64,0],"animation":"spin_forever"}
                ]}
                """).getAsJsonObject();
        assertTrue(NpcRoutineDefinition.parse(Identifier.parse("ebb:test/invalid_routine"), invalid, messages).isEmpty(),
                "invalid routine steps should reject the routine rather than silently loading an empty routine");
        assertTrue(messages.stream().anyMatch(message -> message.contains("action \"moonwalk\" is invalid")), messages::toString);
        assertTrue(messages.stream().anyMatch(message -> message.contains("path must contain at least two waypoints")), messages::toString);
        assertTrue(messages.stream().anyMatch(message -> message.contains("pose \"impossible_pose\" is invalid")), messages::toString);
        assertTrue(messages.stream().anyMatch(message -> message.contains("animation \"spin_forever\" is invalid")), messages::toString);
    }

    @Test
    void investigationCluesModifyChecksAndConflictFailsForward() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        JournalEntryRegistry.rebuild(load(bundled.resolve("journal_entries")));
        InvestigationRegistry.rebuildClues(load(bundled.resolve("clues")));
        InvestigationRegistry.rebuildScenes(load(bundled.resolve("investigation_scenes")));
        ConflictRegistry.rebuild(load(bundled.resolve("conflicts")));
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();

        parseEffect("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/door_scratches\"}").apply(state, player);
        parseEffect("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/witness_knock_pattern\"}").apply(state, player);
        assertTrue(state.hasClue(player, "ebb:demo/door_scratches"));
        assertTrue(state.hasJournalEntry(player, "ebb:demo/door_scratches"), "clue reveal should also add linked journal entry");
        assertTrue(InvestigationRegistry.totalCheckModifier(state, player, "charisma") >= 1);
        assertTrue(InvestigationRegistry.totalCheckModifier(state, player, "perception") >= 1);

        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        ConflictService.addStress(state, player, "ebb:demo/hallway_confrontation", 3);
        assertEquals("failed_forward", state.getConflictState(player, "ebb:demo/hallway_confrontation"));
        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        ConflictService.addResolve(state, player, "ebb:demo/hallway_confrontation", 2);
        assertEquals("resolved", state.getConflictState(player, "ebb:demo/hallway_confrontation"));
        state.setScenePhase(player, "ebb:demo/locked_room", "confrontation");

        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"clue","type":"thought","text":"clue","conditions":[{"type":"clue_found","id":"ebb:demo/witness_knock_pattern"}]},
                  {"id":"conflict","type":"dialogue","text":"conflict","conditions":[{"type":"conflict_state","id":"ebb:demo/hallway_confrontation","state":"resolved"}]},
                  {"id":"scene","type":"dialogue","text":"scene","conditions":[{"type":"scene_phase","id":"ebb:demo/locked_room","phase":"confrontation"}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/investigation_conflict"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        assertTrue(definition.node("start").orElseThrow().choice("clue").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("conflict").orElseThrow().conditions().getFirst().matches(state, player));
        assertTrue(definition.node("start").orElseThrow().choice("scene").orElseThrow().conditions().getFirst().matches(state, player));
    }

    @Test
    void p28ConflictPhasesLeverageOutcomesPersistAndFailForward() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        InvestigationRegistry.rebuildClues(load(bundled.resolve("clues")));
        ConflictRegistry.rebuild(load(bundled.resolve("conflicts")));
        Identifier conflictId = Identifier.parse("ebb:demo/hallway_confrontation");
        var definition = ConflictRegistry.byId(conflictId).orElseThrow();

        assertEquals(java.util.List.of("setup", "pressure", "turn", "consequence", "resolution"), definition.phases());
        assertTrue(definition.leverageClues().contains(Identifier.parse("ebb:demo/witness_knock_pattern")));
        assertTrue(definition.outcomes().stream().anyMatch(outcome -> outcome.isNonViolentKind() && outcome.state().equals("resolved_nonviolent")));
        assertTrue(definition.outcomes().stream().anyMatch(outcome -> outcome.isMessyKind() && outcome.state().equals("resolved_messy")));
        assertTrue(definition.outcomes().stream().filter(outcome -> outcome.isFailureForwardKind()).count() >= 2,
                "P28 requires at least two failure-forward conflict outcomes");

        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        parseEffect("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/door_scratches\"}").apply(state, player);
        parseEffect("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/witness_knock_pattern\"}").apply(state, player);

        String startStatus = ConflictService.start(state, player, conflictId.toString()).orElseThrow();
        assertEquals("active", state.getConflictState(player, conflictId.toString()));
        assertEquals("setup", state.getConflictPhase(player, conflictId.toString()));
        assertTrue(startStatus.contains("stress=0/3"), startStatus);
        assertTrue(startStatus.contains("resolve=0/2"), startStatus);
        assertTrue(startStatus.contains("leverage=door_scratches+witness_knock_pattern")
                || startStatus.contains("leverage=witness_knock_pattern+door_scratches"), startStatus);

        ConflictService.addResolve(state, player, conflictId.toString(), 1);
        assertEquals(1, state.getConflictScore(player, "resolve", conflictId.toString()));
        assertEquals("turn", state.getConflictPhase(player, conflictId.toString()));

        ConflictService.applyOutcome(state, player, conflictId.toString(), "public_pressure_fail");
        assertEquals("failed_forward_public", state.getConflictState(player, conflictId.toString()));
        assertEquals("consequence", state.getConflictPhase(player, conflictId.toString()));

        ConflictService.start(state, player, conflictId.toString());
        ConflictService.applyOutcome(state, player, conflictId.toString(), "quiet_resolve");
        assertEquals("resolved_nonviolent", state.getConflictState(player, conflictId.toString()));
        assertEquals("resolution", state.getConflictPhase(player, conflictId.toString()));

        ConflictService.start(state, player, conflictId.toString());
        ConflictService.applyOutcome(state, player, conflictId.toString(), "messy_resolve");
        assertEquals("resolved_messy", state.getConflictState(player, conflictId.toString()));
        assertEquals("resolution", state.getConflictPhase(player, conflictId.toString()));

        ConflictService.start(state, player, conflictId.toString());
        ConflictService.addStress(state, player, conflictId.toString(), 3);
        assertEquals("failed_forward", state.getConflictState(player, conflictId.toString()));
        assertEquals("consequence", state.getConflictPhase(player, conflictId.toString()));
    }

    @Test
    void p29SavedDataMigrationAddsConflictPhaseAndPreservesLegacyState() {
        UUID player = UUID.randomUUID();
        JsonObject legacy = JsonParser.parseString("""
                {
                  "version": 1,
                  "players": {
                    "%s": {
                      "narrative_states": {
                        "conflict:ebb:demo/hallway_confrontation": "failed_forward",
                        "scene:ebb:demo/locked_room": "messy"
                      },
                      "conflict_scores": {
                        "stress:ebb:demo/hallway_confrontation": 3
                      }
                    }
                  }
                }
                """.formatted(player)).getAsJsonObject();
        NarrativeSavedData migrated = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, legacy)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow();

        assertEquals(NarrativeSavedData.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals("failed_forward", migrated.getConflictState(player, "ebb:demo/hallway_confrontation"));
        assertEquals("consequence", migrated.getConflictPhase(player, "ebb:demo/hallway_confrontation"));
        assertEquals("messy", migrated.getScenePhase(player, "ebb:demo/locked_room"));
        assertEquals(3, migrated.getConflictScore(player, "stress", "ebb:demo/hallway_confrontation"));
        assertEquals(com.crpg.ebb.state.PlayerNarrativeState.DEFAULT_ATTRIBUTE_POINTS, migrated.getAttributePoints(player),
                "older saves without attribute_points should keep safe defaults");

        JsonObject encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, migrated)
                .resultOrPartial(message -> {
                    throw new AssertionError(message);
                })
                .orElseThrow().getAsJsonObject();
        assertEquals(NarrativeSavedData.CURRENT_SCHEMA_VERSION, encoded.get("version").getAsInt());
        JsonObject playerJson = encoded.getAsJsonObject("players").getAsJsonObject(player.toString());
        assertEquals("consequence", playerJson.getAsJsonObject("narrative_states")
                .get("conflict_phase:ebb:demo/hallway_confrontation").getAsString());
    }

    @Test
    void p29DialogueSessionPreflightRejectsSpoofedStaleAndContendedSessions() throws Exception {
        DialogueService.clearAll("p29_test");
        DialogueService.clearSecurityEventSnapshot();
        Map<UUID, DialogueSession> sessions = privateStaticMap(DialogueService.class, "SESSIONS");
        Map<UUID, UUID> playerToSession = privateStaticMap(DialogueService.class, "PLAYER_TO_SESSION");
        sessions.clear();
        playerToSession.clear();

        UUID conversation = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        DialogueSession session = new DialogueSession(
                conversation,
                playerOne,
                Identifier.parse("ebb:demo/guard_intro"),
                Identifier.parse("ebb:demo/guard_ebb_npc"),
                InteractionTargetType.ENTITY,
                java.util.Optional.of(entity),
                "start",
                100L
        );
        sessions.put(conversation, session);
        playerToSession.put(playerOne, conversation);

        assertTrue(DialogueService.validateChoicePacket(playerOne, conversation, 101L).allowed());
        assertEquals("session_player_mismatch", DialogueService.validateChoicePacket(playerTwo, conversation, 101L).reason());
        assertEquals("missing_or_expired_session", DialogueService.validateChoicePacket(playerOne, UUID.randomUUID(), 101L).reason());
        assertEquals("session_timeout", DialogueService.validateChoicePacket(playerOne, conversation, 20L * 60L * 6L).reason());
        assertTrue(DialogueService.entityReservedByAnotherPlayer(entity, playerTwo));
        assertFalse(DialogueService.entityReservedByAnotherPlayer(entity, playerOne));
        Map<String, Integer> security = DialogueService.securityEventSnapshot();
        assertTrue(security.getOrDefault("session_player_mismatch", 0) >= 1, security::toString);
        assertTrue(security.getOrDefault("missing_or_expired_session", 0) >= 1, security::toString);
        assertTrue(security.getOrDefault("session_timeout", 0) >= 1, security::toString);

        DialogueService.clearAll("p29_test_cleanup");
        DialogueService.clearSecurityEventSnapshot();
    }

    @Test
    void p29CommandPermissionSurfaceKeepsAdminAndSelfInspectionSplit() throws Exception {
        String commands = Files.readString(Path.of("src/main/java/com/crpg/ebb/registry/ModCommands.java"));
        String permissionGuards = Files.readString(Path.of("src/main/java/com/crpg/ebb/registry/commands/EbbCommandPermissionGuards.java"));
        for (String adminPermission : java.util.List.of(
                "command.dev",
                "command.dialogue",
                "command.routine",
                "command.export",
                "command.summon_npc",
                "command.attributes.grant",
                "command.attributes.set",
                "command.attributes.reset"
        )) {
            assertTrue(permissionGuards.contains("group(\"" + adminPermission + "\")"),
                    "admin command should remain permission-gated: " + adminPermission);
        }
        assertTrue(commands.contains(".then(Commands.argument(\"player\", EntityArgument.player())\n                                        .requires(EbbCommandPermissionGuards.dialogue())"),
                "nested /ebb dialogue vars <player> must require dialogue gamemaster permission");
        assertTrue(commands.contains("Commands.literal(\"vars\")\n                        .executes"),
                "player self vars command should remain executable without OP branch");
        assertTrue(commands.contains("Commands.literal(\"journal\")\n                        .executes"),
                "journal self-inspection should remain player-safe");
        assertTrue(commands.contains("Commands.literal(\"quest\")\n                        .executes"),
                "quest self-inspection should remain player-safe");
        assertTrue(commands.contains("Commands.literal(\"spend\")"),
                "attribute spending should remain player-facing while grant/set/reset stay OP-gated");
    }

    @Test
    void p30VerticalSliceContentExpansionMeetsMinimumCounts() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(bundled.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(bundled.resolve("npc_routines")));
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));
        QuestBranchRegistry.rebuild(load(bundled.resolve("quest_branches")));
        FeatRegistry.rebuild(load(bundled.resolve("feats")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));
        JournalEntryRegistry.rebuild(load(bundled.resolve("journal_entries")));
        InvestigationRegistry.rebuildClues(load(bundled.resolve("clues")));
        ConflictRegistry.rebuild(load(bundled.resolve("conflicts")));

        assertTrue(BlockGroupIndex.groupCount() >= 12, "P30 requires at least 12 block-group investigation points");
        assertTrue(EntityBindingRegistry.size() >= 14, "P30 adds cook/courier tag and name bindings");
        assertTrue(NpcRoutineRegistry.size() >= 7, "P30 adds cook and courier routines");
        long majorBranches = QuestBranchRegistry.orderedDefinitions().stream()
                .filter(branch -> branch.kind() == com.crpg.ebb.quest.QuestBranchKind.MAJOR)
                .count();
        long minorBranches = QuestBranchRegistry.orderedDefinitions().stream()
                .filter(branch -> branch.kind() == com.crpg.ebb.quest.QuestBranchKind.MINOR)
                .count();
        assertTrue(majorBranches >= 4, "P30 requires at least 4 major branches");
        assertTrue(minorBranches >= 8, "P30 requires at least 8 minor branches");
        assertTrue(FeatRegistry.size() >= 12, "P30 requires at least 12 feats");
        assertTrue(ChimeRegistry.size() >= 8, "P30 keeps at least 8 chimes");
        int chimeLines = ChimeRegistry.orderedDefinitions().stream().mapToInt(chime -> chime.lines().size()).sum();
        assertTrue(chimeLines >= 40, "P30 requires at least 40 chime lines");
        assertTrue(JournalEntryRegistry.size() >= 20, "P30 requires at least 20 journal entries");
        assertTrue(InvestigationRegistry.clueCount() >= 20, "P30 requires at least 20 clues");
        assertTrue(ConflictRegistry.size() >= 3, "P30 requires at least 3 set-piece conflicts");
        for (String id : java.util.List.of(
                "ebb:demo/cook_intro",
                "ebb:demo/courier_intro",
                "ebb:demo/kitchen_manifest_dialogue",
                "ebb:demo/stable_mud_dialogue"
        )) {
            assertTrue(DialogueRegistry.byId(Identifier.parse(id)).isPresent(), "P30 dialogue should load: " + id);
        }
        DialogueDefinition ending = DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).orElseThrow();
        for (String node : java.util.List.of("public_end", "quiet_end", "messy_end", "trade_end", "mercy_end")) {
            assertTrue(ending.node(node).isPresent(), "each major/messy route needs an ending placeholder: " + node);
        }
    }

    @Test
    void playableVerticalSliceMeetsContentMinimums() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(bundled.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(bundled.resolve("npc_routines")));
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));
        QuestBranchRegistry.rebuild(load(bundled.resolve("quest_branches")));
        FeatRegistry.rebuild(load(bundled.resolve("feats")));
        ChimeRegistry.rebuild(load(bundled.resolve("chimes")));
        InvestigationRegistry.rebuildClues(load(bundled.resolve("clues")));
        ConflictRegistry.rebuild(load(bundled.resolve("conflicts")));

        assertTrue(BlockGroupIndex.groupCount() >= 8, "P8 requires at least 8 interactable investigation points");
        for (String id : java.util.List.of(
                "ebb:demo/innkeeper_intro",
                "ebb:demo/witness_intro",
                "ebb:demo/tenant_intro",
                "ebb:demo/guard_intro",
                "ebb:demo/back_door_dialogue"
        )) {
            assertTrue(DialogueRegistry.byId(Identifier.parse(id)).isPresent(), id + " should be present");
        }
        assertTrue(EntityBindingRegistry.size() >= 6, "role-specific NPC bindings should be present");
        assertTrue(NpcRoutineRegistry.size() >= 5, "four role routines plus innkeeper backroom should be present");
        assertTrue(QuestBranchRegistry.size() >= 2, "two major branches should be present");
        assertTrue(FeatRegistry.size() >= 4, "four feats should be present");
        assertTrue(ChimeRegistry.size() >= 8, "P26 eight chimes should be present");
        assertTrue(InvestigationRegistry.clueCount() >= 5, "investigation clues should be present");
        assertTrue(ConflictRegistry.size() >= 1, "one set-piece conflict should be present");
        DialogueDefinition ending = DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).orElseThrow();
        assertTrue(ending.node("public_end").isPresent(), "public ending placeholder should exist");
        assertTrue(ending.node("quiet_end").isPresent(), "quiet ending placeholder should exist");
        assertTrue(ending.node("messy_end").isPresent(), "fail-forward ending placeholder should exist");
    }

    @Test
    void guiRetestCommandsRoleBindingsAndBlockGroupsAreRegistered() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);

        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "journal");
        requireCommandPath(ebb, "quest");
        requireCommandPath(ebb, "quest", "tree");
        requireCommandPath(ebb, "dialogue", "vars");
        requireCommandPath(ebb, "vars");
        requireCommandPath(ebb, "llm");
        requireCommandPath(ebb, "llm", "status");

        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(bundled.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));

        Map<String, String> expectedRoleDialogues = Map.of(
                "innkeeper", "ebb:demo/innkeeper_intro",
                "witness", "ebb:demo/witness_intro",
                "tenant", "ebb:demo/tenant_intro",
                "guard", "ebb:demo/guard_intro"
        );
        for (Map.Entry<String, String> role : expectedRoleDialogues.entrySet()) {
            Identifier bindingId = Identifier.parse("ebb:demo/" + role.getKey() + "_ebb_npc");
            var binding = EntityBindingRegistry.definitions().get(bindingId);
            assertNotNull(binding, "role-specific binding should exist: " + bindingId);
            assertEquals(Identifier.parse(role.getValue()), binding.dialogueId(),
                    "role-specific binding should not fall back to innkeeper dialogue");
            assertTrue(binding.tags().contains("ebb.npc." + role.getKey() + "_day"),
                    "role-specific binding should match existing save legacy tag for " + role.getKey());

            Identifier nameBindingId = Identifier.parse("ebb:demo/" + role.getKey() + "_ebb_npc_name");
            var nameBinding = EntityBindingRegistry.definitions().get(nameBindingId);
            assertNotNull(nameBinding, "role-specific name binding should exist for client prediction: " + nameBindingId);
            assertEquals(Identifier.parse(role.getValue()), nameBinding.dialogueId(),
                    "name binding should preserve the role-specific dialogue");
            assertEquals("Ebb NPC: " + role.getKey() + "_day", nameBinding.name().orElseThrow(),
                    "name binding should match the custom name visible to dedicated clients");
        }

        for (String id : java.util.List.of(
                "ebb:demo/locked_door",
                "ebb:demo/counter_ledger",
                "ebb:demo/notice_board",
                "ebb:demo/washroom_mirror",
                "ebb:demo/windowsill_ash",
                "ebb:demo/tenant_luggage",
                "ebb:demo/cellar_hatch",
                "ebb:demo/back_door"
        )) {
            assertTrue(BlockGroupIndex.byId(Identifier.parse(id)).isPresent(),
                    "all GUI retest block targets should be registered: " + id);
        }
    }

    @Test
    void authoringCompilerOutputLoadsAsRuntimeData() throws Exception {
        Path generated = Path.of("build/generated/ebb_authoring/data/ebb");
        DialogueRegistry.rebuild(load(generated.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(generated.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(generated.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(generated.resolve("npc_routines")));

        var clerk = DialogueRegistry.byId(Identifier.parse("ebb:harbor_clerk_intro")).orElseThrow();
        assertEquals(DialogueNodeType.LINE, clerk.node("start").orElseThrow().type());
        var bluff = clerk.node("choices_main").orElseThrow().choice("bluff_badge").orElseThrow();
        assertTrue(bluff.singleUse(), "ONE_SHOT checks become single-use choices");
        assertEquals(RollMode.ONE_SHOT, bluff.check().orElseThrow().mode());
        assertEquals(2, bluff.check().orElseThrow().staticModifier());
        assertEquals(2, bluff.check().orElseThrow().failureEffects().size());
        assertEquals(1, BlockGroupIndex.groupCount());
        assertEquals(1, EntityBindingRegistry.size());
        assertEquals(1, NpcRoutineRegistry.size());
    }

    @Test
    void p33ActiveFeatDisadvantageAndCheckedChoiceSemanticsAreExplicit() throws Exception {
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        state.unlockFeat(player, "ebb:demo/tavern_authority");

        JsonObject featDialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"unlocked","type":"dialogue","text":"unlocked","conditions":[{"type":"has_feat","id":"ebb:demo/tavern_authority"}]},
                  {"id":"active","type":"dialogue","text":"active","conditions":[{"type":"has_active_feat","id":"ebb:demo/tavern_authority"}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition featDefinition = DialogueDefinition.parse(Identifier.parse("ebb:test/active_feat"), featDialogue, new java.util.ArrayList<>()).orElseThrow();
        var unlocked = featDefinition.node("start").orElseThrow().choice("unlocked").orElseThrow().conditions().getFirst();
        var active = featDefinition.node("start").orElseThrow().choice("active").orElseThrow().conditions().getFirst();
        assertTrue(unlocked.matches(state, player), "has_feat should accept unlocked feats");
        assertFalse(active.matches(state, player), "has_active_feat must not collapse to unlocked feat semantics");
        state.activateFeat(player, "ebb:demo/tavern_authority");
        assertTrue(active.matches(state, player), "has_active_feat should require an active feat slot");

        JsonObject rollDialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"dis","type":"action","text":"dis","next":"done","check":{"attribute":"wisdom","dc":12,"disadvantage":true,"success":"done","failure":"done"}},
                  {"id":"cancel","type":"action","text":"cancel","next":"done","check":{"attribute":"wisdom","dc":12,"advantage":true,"disadvantage":true,"success":"done","failure":"done"}}
                ]},"done":{"text":"done"}}}
                """).getAsJsonObject();
        DialogueDefinition rollDefinition = DialogueDefinition.parse(Identifier.parse("ebb:test/disadvantage"), rollDialogue, new java.util.ArrayList<>()).orElseThrow();
        var disChoice = rollDefinition.node("start").orElseThrow().choice("dis").orElseThrow();
        var cancelChoice = rollDefinition.node("start").orElseThrow().choice("cancel").orElseThrow();
        assertTrue(disChoice.check().orElseThrow().disadvantage());
        assertTrue(VisibleDialogueChoice.fromChoice(disChoice).checkSummary().orElseThrow().contains("disadvantage"));
        assertTrue(VisibleDialogueChoice.fromChoice(cancelChoice).checkSummary().orElseThrow().contains("normal"));

        RollResultPayload result = new RollResultPayload(
                "wisdom", 12, 4, 6, 10, false, false, "failure", true, true,
                3, 1, 2, 0, 17, java.util.Optional.of(4), "disadvantage"
        );
        assertTrue(result.summary().contains("17/4 dis =>4"), result.summary());
        assertTrue(result.summary().contains("3+1 static+2 feat=6"), result.summary());
    }

    @Test
    void p33CheckedChoiceEndOnSuccessPreEffectsAndRetryLocksAreLinted() throws Exception {
        JsonObject warned = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"warn","type":"action","text":"warn","check":{"attribute":"charisma","dc":10,"failure":"fail"}}
                ]},"fail":{"text":"fail"}}}
                """).getAsJsonObject();
        java.util.ArrayList<String> warnMessages = new java.util.ArrayList<>();
        assertTrue(DialogueDefinition.parse(Identifier.parse("ebb:test/warn_success_end"), warned, warnMessages).isPresent());
        assertTrue(warnMessages.stream().anyMatch(message -> message.contains("end_on_success=true")), warnMessages::toString);

        JsonObject explicitEnd = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"end","type":"action","text":"end","end_on_success":true,"check":{"attribute":"charisma","dc":10,"failure":"fail"}}
                ]},"fail":{"text":"fail"}}}
                """).getAsJsonObject();
        java.util.ArrayList<String> endMessages = new java.util.ArrayList<>();
        assertTrue(DialogueDefinition.parse(Identifier.parse("ebb:test/end_success"), explicitEnd, endMessages).isPresent());
        assertTrue(endMessages.stream().noneMatch(message -> message.contains("end_on_success=true")), endMessages::toString);

        JsonObject preEffects = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"pre","type":"action","text":"pre","next":"done","pre_effects":[{"type":"set_flag","id":"safe_pre"}],"check":{"attribute":"charisma","dc":10,"success":"done","failure":"done"}},
                  {"id":"branch","type":"action","text":"branch","next":"done","pre_effects":[{"type":"complete_quest_branch","id":"ebb:demo/tavern_public"}],"check":{"attribute":"charisma","dc":10,"success":"done","failure":"done"}}
                ]},"done":{"text":"done"}}}
                """).getAsJsonObject();
        java.util.ArrayList<String> effectMessages = new java.util.ArrayList<>();
        DialogueDefinition preDefinition = DialogueDefinition.parse(Identifier.parse("ebb:test/pre_effects"), preEffects, effectMessages).orElseThrow();
        assertEquals(1, preDefinition.node("start").orElseThrow().choice("pre").orElseThrow().effects().size(),
                "pre_effects should be parsed as legacy pre-roll effects");
        assertTrue(effectMessages.stream().anyMatch(message -> message.contains("branch-specific state before the roll")), effectMessages::toString);

        var retryChoice = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"white_check","type":"action","text":"try","next":"done","check":{"attribute":"wisdom","dc":15,"mode":"retryable","success":"done","failure":"done"}}
                ]},"done":{"text":"done"}}}
                """).getAsJsonObject();
        DialogueDefinition retryDefinition = DialogueDefinition.parse(Identifier.parse("ebb:test/retry_lock"), retryChoice, new java.util.ArrayList<>()).orElseThrow();
        var choice = retryDefinition.node("start").orElseThrow().choice("white_check").orElseThrow();
        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        String lockFlag = DialogueService.retryLockFlag(Identifier.parse("ebb:test/retry_lock"), "white_check");
        state.setPlayerFlag(player, lockFlag, true);
        Method consume = DialogueService.class.getDeclaredMethod(
                "consumeRetryUnlockOrDeny",
                NarrativeSavedData.class,
                UUID.class,
                Identifier.class,
                com.crpg.ebb.dialogue.DialogueChoice.class
        );
        consume.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Optional<String> denied = (java.util.Optional<String>) consume.invoke(null, state, player, Identifier.parse("ebb:test/retry_lock"), choice);
        assertEquals(java.util.Optional.of("check_locked:white_check"), denied);
        state.setPlayerFlag(player, "unlock:white_check", true);
        @SuppressWarnings("unchecked")
        java.util.Optional<String> allowed = (java.util.Optional<String>) consume.invoke(null, state, player, Identifier.parse("ebb:test/retry_lock"), choice);
        assertTrue(allowed.isEmpty(), "unlock flag should consume the retry lock");
        assertFalse(state.hasPlayerFlag(player, lockFlag));
        assertFalse(state.hasPlayerFlag(player, "unlock:white_check"));
    }

    @Test
    void p33RaycastBlockGroupAndRoutineHardeningRegressions() {
        assertEquals(ClipContext.Block.COLLIDER, InteractionRaycastPolicy.blockModeForPrediction());
        assertEquals(ClipContext.Block.COLLIDER, InteractionRaycastPolicy.blockModeForAuthority());
        assertEquals(ClipContext.Block.COLLIDER, InteractionRaycastPolicy.blockModeForDevInspect());

        Map<Identifier, JsonObject> blockGroups = new LinkedHashMap<>();
        blockGroups.put(Identifier.parse("ebb:test/first"), JsonParser.parseString("""
                {"dimension":"minecraft:overworld","dialogue":"ebb:test/dialogue","blocks":[[1,64,1]],"interaction_point":[1.5,64.5,1.5]}
                """).getAsJsonObject());
        blockGroups.put(Identifier.parse("ebb:test/second"), JsonParser.parseString("""
                {"dimension":"minecraft:overworld","dialogue":"ebb:test/dialogue","blocks":[[1,64,1]],"interaction_point":[1.5,64.5,1.5]}
                """).getAsJsonObject());
        BlockGroupIndex.rebuild(blockGroups);
        assertTrue(BlockGroupIndex.byId(Identifier.parse("ebb:test/first")).isPresent());
        assertTrue(BlockGroupIndex.byId(Identifier.parse("ebb:test/second")).isEmpty());
        assertEquals(1, BlockGroupIndex.groupCount());
        assertTrue(BlockGroupIndex.messages().stream().anyMatch(message -> message.contains("duplicate block membership")), BlockGroupIndex.messages()::toString);

        java.util.ArrayList<String> emptyMessages = new java.util.ArrayList<>();
        assertTrue(NpcRoutineDefinition.parse(Identifier.parse("ebb:test/empty_routine"), JsonParser.parseString("{\"steps\":[]}").getAsJsonObject(), emptyMessages).isEmpty());
        assertTrue(emptyMessages.stream().anyMatch(message -> message.contains("at least one valid step")), emptyMessages::toString);

        java.util.ArrayList<String> overlapMessages = new java.util.ArrayList<>();
        JsonObject overlap = JsonParser.parseString("""
                {"steps":[
                  {"time":[0,1000],"action":"stand","pos":[0,64,0]},
                  {"time":[500,1500],"action":"stand","pos":[1,64,0]}
                ]}
                """).getAsJsonObject();
        assertTrue(NpcRoutineDefinition.parse(Identifier.parse("ebb:test/overlap_routine"), overlap, overlapMessages).isEmpty());
        assertTrue(overlapMessages.stream().anyMatch(message -> message.contains("overlaps")), overlapMessages::toString);

        java.util.ArrayList<String> teleportMessages = new java.util.ArrayList<>();
        JsonObject invalidTeleport = JsonParser.parseString("""
                {"steps":[{"time":[0,1000],"action":"stand","pos":[0,64,0],"teleport_distance":0}]}
                """).getAsJsonObject();
        assertTrue(NpcRoutineDefinition.parse(Identifier.parse("ebb:test/teleport_routine"), invalidTeleport, teleportMessages).isEmpty());
        assertTrue(teleportMessages.stream().anyMatch(message -> message.contains("teleport_distance must be > 0")), teleportMessages::toString);
    }


    @Test
    void p34LlmConfigFakeProviderAndChoiceParsingAreDeterministic() throws Exception {
        LlmConfig disabled = LlmConfig.disabled();
        assertFalse(disabled.active(), "default LLM config must be disabled");
        assertFalse(disabled.networkAccessAllowed(), "disabled mode must not allow network access");

        LlmConfig fake = LlmConfig.parse(JsonParser.parseString("""
                {"enabled":true,"mode":"fake","max_input_chars":128,"session_timeout_ticks":40,"fake_reply":"FAKE_NPC_REPLY"}
                """).getAsJsonObject());
        assertTrue(fake.fakeMode());
        assertFalse(fake.networkAccessAllowed(), "fake mode must not access network");
        assertTrue(fake.summary().contains("network=blocked"), fake.summary());

        LlmChatResponse response = new FakeLlmGatewayClient(fake).sendMessage(new LlmChatRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Optional.empty(),
                Identifier.parse("ebb:test/llm"),
                "start",
                "ebb:demo/innkeeper",
                "innkeeper",
                "ledger",
                "hello there",
                42L
        )).get();
        assertTrue(response.reply().contains("FAKE_NPC_REPLY"), response.reply());
        assertTrue(response.reply().contains("innkeeper"), response.reply());
        assertEquals("fake_reply", response.status());
        assertFalse(response.citationIds().isEmpty(), "fake provider should expose deterministic citation ids for dev UI");

        assertEquals(ChoiceType.LLM_CHAT, ChoiceType.parse("llm_chat").orElseThrow());
        assertEquals(ChoiceType.LLM_CHAT, ChoiceType.parse("free_chat").orElseThrow());
        JsonObject dialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"speaker":"innkeeper","text":"x","choices":[
                  {"id":"chat","type":"free_chat","text":"chat","llm":{"npc":"ebb:demo/innkeeper","topic_hint":"rumors","return_node":"start"}}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition definition = DialogueDefinition.parse(Identifier.parse("ebb:test/llm_choice"), dialogue, new java.util.ArrayList<>()).orElseThrow();
        var choice = definition.node("start").orElseThrow().choice("chat").orElseThrow();
        assertEquals(ChoiceType.LLM_CHAT, choice.type());
        assertEquals("ebb:demo/innkeeper", choice.llmSettings().npc().orElseThrow());
        assertEquals("rumors", choice.llmSettings().topicHint().orElseThrow());
    }

    @Test
    void p34LlmDisabledModeTimeoutAndCommandsAreAuditable() throws Exception {
        try {
            LlmConfig.setForTesting(new LlmConfig(true, LlmMode.FAKE, "", LlmConfig.DEFAULT_GATEWAY_TIMEOUT_MS, false, LlmConfig.DEFAULT_CHAT_MODEL, true, true, false, 128, 256, 20, 10, "FAKE_NPC_REPLY"));
            LlmChatService.setClientForTesting(new FakeLlmGatewayClient(LlmConfig.current()));
            UUID conversation = UUID.randomUUID();
            UUID player = UUID.randomUUID();
            LlmChatSession session = new LlmChatSession(
                    conversation,
                    player,
                    Identifier.parse("ebb:test/llm"),
                    Identifier.parse("ebb:test/target"),
                    InteractionTargetType.BLOCK_GROUP,
                    Optional.empty(),
                    "start",
                    "start",
                    "ebb:demo/innkeeper",
                    "innkeeper",
                    "rumors",
                    0L,
                    0L,
                    false,
                    0L
            );
            LlmChatService.addSessionForTesting(session);
            assertEquals(1, LlmChatService.activeSessionCount());
            assertEquals(1, LlmChatService.closeExpiredSessionsForTesting(100L), "expired fake LLM chat session should close");
            assertEquals(0, LlmChatService.activeSessionCount());

            LlmChatResponse disabled = new DisabledLlmGatewayClient().sendMessage(new LlmChatRequest(
                    UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/llm"), "start",
                    "npc", "npc", "", "hello", 0L
            )).get();
            assertEquals(Optional.of("llm_disabled"), disabled.errorReason(), "disabled mode should surface explicit llm_disabled");

            CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
            Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
            register.setAccessible(true);
            register.invoke(null, dispatcher);
            CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
            requireCommandPath(ebb, "llm");
            requireCommandPath(ebb, "llm", "status");
        } finally {
            LlmChatService.clearTestingOverrides();
        }
    }






    @Test
    void p38MemoryGatewayClientCommandSurfaceAndFilesAreAuditable() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/memory/search", exchange -> {
            String response = "{\"status\":\"ok\",\"matches\":[{\"id\":\"memrec_test\",\"citation_id\":\"memory:record:memrec_test\",\"score\":0.9,\"role\":\"player\",\"text\":\"fact:player.favorite=blue\"}],\"citation_ids\":[\"memory:record:memrec_test\"]}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/inspect", exchange -> {
            String response = "{\"type\":\"record\",\"record\":{\"id\":\"memrec_test\"}}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/conflicts", exchange -> {
            String response = "{\"status\":\"ok\",\"count\":1,\"conflicts\":[{\"id\":\"memconf_test\",\"citation_ids\":[\"memory:fact:a\",\"memory:fact:b\"]}]}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/episodes", exchange -> {
            String response = "{\"status\":\"ok\",\"episodes\":[{\"id\":\"memrec_test\",\"raw_episode\":\"I question the ledger\",\"summary\":\"Player previously questioned the ledger\"}]}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/lessons", exchange -> {
            String response = "{\"status\":\"ok\",\"safety_lessons\":[{\"id\":\"memsafe_test\",\"lesson\":\"canonical owner remains innkeeper\"}]}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/correct", exchange -> {
            String response = "{\"status\":\"ok\",\"accepted\":true,\"append_only\":true,\"correction_lesson_id\":\"memcorr_test\"}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/memory/player", exchange -> {
            String response = "{\"status\":\"ok\",\"deleted\":true,\"records\":1}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.createContext("/v1/player/quota", exchange -> {
            String response = "{\"status\":\"ok\",\"scopes\":[\"llm:chat\",\"memory:delete_self\"],\"max_output_tokens\":700}";
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            LlmConfig config = new LlmConfig(true, LlmMode.GATEWAY, "http://127.0.0.1:" + server.getAddress().getPort(), 1500, false,
                    "gpt-test", true, true, false, 128, 256, 10, 10, "FAKE_NPC_REPLY");
            MemoryGatewayClient client = new MemoryGatewayClient(config);
            var search = client.search(UUID.randomUUID(), "favorite blue", 5).get(3, TimeUnit.SECONDS);
            assertTrue(search.ok(), search.status());
            assertEquals(List.of("memory:record:memrec_test"), search.citationIds(), "P38 retrieval should expose citation ids");
            assertTrue(search.lines().getFirst().contains("fact:player.favorite=blue"));
            assertTrue(client.inspect("memrec_test").get(3, TimeUnit.SECONDS).contains("memrec_test"));
            assertTrue(client.conflicts(5).get(3, TimeUnit.SECONDS).contains("memconf_test"));
            assertTrue(client.episodes(5).get(3, TimeUnit.SECONDS).contains("raw_episode"));
            assertTrue(client.lessons(5).get(3, TimeUnit.SECONDS).contains("canonical owner remains innkeeper"));
            assertTrue(client.correct("memfact_test", "green").get(3, TimeUnit.SECONDS).contains("\"append_only\":true"));
            assertTrue(client.deletePlayer(UUID.randomUUID()).get(3, TimeUnit.SECONDS).contains("\"deleted\":true"));
            assertTrue(client.quota(UUID.randomUUID()).get(3, TimeUnit.SECONDS).contains("llm:chat"));
        } finally {
            server.stop(0);
        }

        for (String rel : List.of(
                "ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryRecord.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryFact.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryConflict.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/LlmMemoryOperationExtractor.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/DeterministicMemoryValidator.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryConsolidator.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemorySafetyLesson.java",
                "src/main/java/com/crpg/ebb/memory/MemoryGatewayClient.java",
                "scripts/p38_memory_smoke.sh")) {
            assertTrue(Files.isRegularFile(Path.of(rel)), "P38 file should exist: " + rel);
        }
        String gateway = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java"));
        assertTrue(gateway.contains("/v1/memory/search"));
        assertTrue(gateway.contains("/v1/memory/inspect"));
        assertTrue(gateway.contains("/v1/memory/conflicts"));
        assertTrue(gateway.contains("/v1/memory/episodes"));
        assertTrue(gateway.contains("/v1/memory/lessons"));
        assertTrue(gateway.contains("/v1/memory/ingest"));
        assertTrue(gateway.contains("/v1/memory/correct"));
        assertTrue(gateway.contains("/v1/memory/player"));
        assertTrue(gateway.contains("deletePlayer"));
        assertTrue(gateway.contains("correctFact"));
        String smoke = Files.readString(Path.of("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java"));
        assertTrue(smoke.contains("P38 memory smoke passed"));
        assertTrue(smoke.contains("P39 memory consolidation smoke passed"));
        assertTrue(smoke.contains("canonical owner remains innkeeper"));
        assertTrue(smoke.contains("questioned_ledger"));
        assertTrue(smoke.contains("/v1/memory/correct"));
        assertTrue(smoke.contains("/v1/memory/player"));

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "memory", "search", "query");
        requireCommandPath(ebb, "memory", "inspect", "id");
        requireCommandPath(ebb, "memory", "conflicts");
        requireCommandPath(ebb, "memory", "episodes");
        requireCommandPath(ebb, "memory", "lessons");
        requireCommandPath(ebb, "memory", "correct", "fact_id", "new_value");
        requireCommandPath(ebb, "memory", "export");
        requireCommandPath(ebb, "memory", "delete_player", "player");
    }

    @Test
    void p39MemoryExtractionConsolidationAndSafetyAreAuditable() throws Exception {
        String migration = Files.readString(Path.of("ebb-llm-gateway/src/main/resources/db/migration/V001__memory_store.sql"));
        assertTrue(migration.contains("memory_operations"));
        assertTrue(migration.contains("memory_summaries"));
        assertTrue(migration.contains("memory_links"));
        assertTrue(migration.contains("memory_safety_lessons"));
        String extractor = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/LlmMemoryOperationExtractor.java"));
        assertTrue(extractor.contains("memory_writes"));
        assertTrue(extractor.contains("questioned_ledger"));
        assertTrue(extractor.contains("我是旅馆老板"));
        String validator = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/DeterministicMemoryValidator.java"));
        assertTrue(validator.contains("CANONICAL_FACTS"));
        assertTrue(validator.contains("tavern"));
        assertTrue(validator.contains("owner"));
        String store = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/memory/MemoryStore.java"));
        assertTrue(store.contains("llmExtractor.propose"));
        assertTrue(store.contains("validator.validate"));
        assertTrue(store.contains("backgroundSummarize"));
        assertTrue(store.contains("evolveOldSummary"));
        assertTrue(store.contains("raw_episode"));
        assertTrue(store.contains("extracted_facts"));
        assertTrue(store.contains("safetyLessons"));
    }


    @Test
    void p40NpcKnowledgeHidesSecretsUntilClueAndSupportsStoryEffects() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));
        NpcKnowledgeRegistry.rebuild(load(bundled.resolve("npc_knowledge_packs")));

        assertTrue(NpcKnowledgeRegistry.size() >= 7, "P40 should load all demo NPC knowledge packs referenced by profiles");
        assertTrue(NpcKnowledgeRegistry.validationMessages().isEmpty(), NpcKnowledgeRegistry.validationMessages().toString());
        for (String pack : List.of("tavern_public_lore", "innkeeper_private_ledger", "kitchen_manifest", "courier_route", "guard_case_notes", "tenant_private_alibi", "witness_heard_knocks")) {
            assertTrue(NpcKnowledgeRegistry.byId(Identifier.parse("ebb:demo/" + pack)).isPresent(), "missing KB pack: " + pack);
        }

        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        String question = "What did the tenant pay in the ledger?";
        String before = NpcKnowledgeService.promptContext("ebb:demo/innkeeper", question, state, player, 6000L, 6);
        assertTrue(before.contains("ledger_evasion_public"), before);
        assertFalse(before.toLowerCase(Locale.ROOT).contains("tenant paid cash"), "secret chunk should not leak before clue: " + before);
        assertTrue(NpcKnowledgeService.inspectLines("ebb:demo/innkeeper", question, state, player, 6000L, 16).stream()
                .anyMatch(line -> line.contains("hidden") && line.contains("secret_ledger_tenant_cash")),
                "KB inspect should show hidden secret chunks before clue");

        LlmChatResponse beforeReply = new FakeLlmGatewayClient(LlmConfig.fakeForTesting()).sendMessage(new LlmChatRequest(
                UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/p40"), "start",
                "ebb:demo/innkeeper", "innkeeper", "ledger", question, 1L, before)).get(3, TimeUnit.SECONDS);
        assertTrue(beforeReply.reply().contains("kb=public_only"), beforeReply.reply());

        state.revealClue(player, "ebb:demo/guestbook_gap");
        String after = NpcKnowledgeService.promptContext("ebb:demo/innkeeper", question, state, player, 6000L, 6);
        assertTrue(after.toLowerCase(Locale.ROOT).contains("tenant paid cash"), "secret chunk should become visible after clue: " + after);
        LlmChatResponse afterReply = new FakeLlmGatewayClient(LlmConfig.fakeForTesting()).sendMessage(new LlmChatRequest(
                UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/p40"), "start",
                "ebb:demo/innkeeper", "innkeeper", "ledger", question, 2L, after)).get(3, TimeUnit.SECONDS);
        assertTrue(afterReply.reply().contains("kb=secret_visible"), afterReply.reply());

        assertTrue(parseEffect("{\"type\":\"npc_kb_add_fact\",\"npc\":\"ebb:demo/innkeeper\",\"fact\":\"player_checked_back_door\"}")
                .apply(state, player).orElseThrow().contains("npc_kb_fact_added"));
        assertTrue(parseEffect("{\"type\":\"npc_kb_add_pack\",\"npc\":\"ebb:demo/innkeeper\",\"pack\":\"ebb:demo/kitchen_manifest\"}")
                .apply(state, player).orElseThrow().contains("npc_kb_pack_added"));
        assertTrue(parseEffect("{\"type\":\"npc_stance_shift\",\"npc\":\"ebb:demo/innkeeper\",\"stance\":\"defensive\"}")
                .apply(state, player).orElseThrow().contains("npc_stance_shift"));
        List<String> lines = NpcKnowledgeService.inspectLines("ebb:demo/innkeeper", "kitchen manifest", state, player, 6000L, 16);
        assertTrue(lines.stream().anyMatch(line -> line.contains("stance=defensive")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.contains("kitchen_manifest")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.contains("player_checked_back_door")), lines.toString());

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "kb", "inspect", "npc");
        requireCommandPath(ebb, "kb", "inspect", "npc", "query");
        requireCommandPath(ebb, "kb", "add_pack", "npc", "pack");
    }

    @Test
    void p41MinorNpcProfileGenerationRateLimitAndDevReviewAreAuditable() throws Exception {
        assertTrue(NpcProfileGenerator.promptTemplate().contains("knowledge_seed"));
        assertEquals("ebb.npc_profile_generator.v1", NpcProfileGenerator.SCHEMA_ID);
        JsonObject schema = NpcProfileGenerator.schema();
        assertTrue(schema.toString().contains("suggested_options"));
        assertTrue(schema.toString().contains("knowledge_seed"));

        NarrativeSavedData state = new NarrativeSavedData();
        long gameTime = 42_000L;
        assertTrue(NpcPromotionService.canPromoteThisWorldHour(state, gameTime));
        for (int i = 0; i < NpcPromotionService.MAX_PROMOTIONS_PER_WORLD_HOUR; i++) {
            state.addWorldStoryInt(StoryVarLayer.MINOR, NpcPromotionService.rateLimitKey(gameTime), 1);
        }
        assertFalse(NpcPromotionService.canPromoteThisWorldHour(state, gameTime),
                "P41 should enforce a per-world-hour promoted minor NPC limit");
        assertTrue(NpcPromotionService.canPromoteThisWorldHour(state, gameTime + NpcPromotionService.WORLD_HOUR_TICKS),
                "P41 rate limit should reset for a later world hour");

        JsonObject generated = JsonParser.parseString("""
                {"id":"ebb:promoted/review","tier":"major_promoted","display_name":"Review NPC",
                 "profile_generation":{"schema_id":"ebb.npc_profile_generator.v1","prompt_version":"npc_profile_generator_v1"},
                 "character":{"speech_rules":["stay consistent"]},
                 "knowledge_seed":{"initial_packs":["ebb:demo/tavern_public_lore"]},
                 "suggested_options":["问你看见了什么？"]}
                """).getAsJsonObject();
        List<String> review = NpcProfileGenerator.devReviewLines(Identifier.parse("ebb:promoted/review"), generated);
        assertTrue(review.stream().anyMatch(line -> line.contains("schema=ebb.npc_profile_generator.v1")), review.toString());
        assertTrue(review.stream().anyMatch(line -> line.contains("suggested_options=1")), review.toString());

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "npc", "review", "npc_key");
        requireCommandPath(ebb, "npc", "reject_profile", "npc_key");
        requireCommandPath(ebb, "npc", "regenerate_profile", "npc_key");
        requireCommandPath(ebb, "npc", "demote", "npc_key");

        String service = Files.readString(Path.of("src/main/java/com/crpg/ebb/npc/profile/NpcPromotionService.java"));
        assertTrue(service.contains("MAX_PROMOTIONS_PER_WORLD_HOUR"));
        assertTrue(service.contains("rate_limited"));
        assertTrue(service.contains("NpcProfileGenerator.generatePromotedProfileJson"));
    }

    @Test
    void p37HttpGatewayClientParsesResponseAndErrorsWithoutHanging() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/message", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            capturedBody.set(body);
            String response;
            int status;
            if (body.contains("force failure")) {
                status = 502;
                response = "{\"error\":\"llm_gateway_error\"}";
            } else {
                status = 200;
                response = "{\"conversation_id\":\"c\",\"npc_reply\":\"Gateway hello\",\"suggested_options\":[\"追问\",{\"label\":\"离开\"}],\"citations\":[\"openai:responses:test\"],\"status\":\"openai_responses_streamed_store_false\",\"store\":false}";
            }
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            UUID player = UUID.randomUUID();
            LlmAuthService.grantTokenForTesting(player, new LlmAuthToken("ebb_player_junit_token", List.of("llm:chat"), 9999999999L, "junit"));
            LlmConfig config = new LlmConfig(true, LlmMode.GATEWAY, "http://127.0.0.1:" + server.getAddress().getPort(), 1500,
                    true, "gpt-test", true, true, false, 256, 333, 20, 10, "FAKE_NPC_REPLY");
            HttpLlmGatewayClient client = new HttpLlmGatewayClient(config);
            LlmChatRequest request = new LlmChatRequest(UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/gateway"),
                    "start", "ebb:demo/innkeeper", "innkeeper", "P37", "hello gateway", 99L);
            LlmChatResponse response = client.sendMessage(request).get(3, TimeUnit.SECONDS);
            assertEquals("Gateway hello", response.reply());
            assertEquals("openai_responses_streamed_store_false", response.status());
            assertEquals(List.of("追问", "离开"), response.suggestedOptions());
            assertTrue(capturedBody.get().contains("\"model\":\"gpt-test\""), capturedBody.get());
            assertTrue(capturedBody.get().contains("\"store\":false"), capturedBody.get());
            assertTrue(capturedBody.get().contains("ebb_player_junit_token"), "opaque token should be sent only server-to-gateway");

            LlmChatResponse error = client.sendMessage(new LlmChatRequest(UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/gateway"),
                    "start", "ebb:demo/innkeeper", "innkeeper", "P37", "force failure", 100L)).get(3, TimeUnit.SECONDS);
            assertEquals(Optional.of("llm_gateway_error"), error.errorReason(), "gateway HTTP failures should return an error response instead of hanging UI flow");
        } finally {
            server.stop(0);
            LlmChatService.clearTestingOverrides();
        }
    }

    @Test
    void p37GatewayOpenAiResponsesIntegrationIsAuditable() throws Exception {
        JsonObject json = JsonParser.parseString("""
                {"enabled":true,"mode":"gateway","gateway_base_url":"http://127.0.0.1:8787","default_chat_model":"gpt-test","llm_chat_streaming":true,"structured_output":true,"openai_store":false}
                """).getAsJsonObject();
        LlmConfig parsed = LlmConfig.parse(json);
        assertEquals(LlmMode.GATEWAY, parsed.mode());
        assertTrue(parsed.networkAccessAllowed());
        assertEquals("gpt-test", parsed.defaultChatModel());
        assertTrue(parsed.llmChatStreaming());
        assertTrue(parsed.structuredOutput());
        assertFalse(parsed.openAiStore(), "P37 privacy default should keep OpenAI store:false unless explicitly configured");
        assertTrue(parsed.toSafeJson().has("default_chat_model"));
        assertFalse(parsed.toString().contains("OPENAI_API_KEY"));

        String build = Files.readString(Path.of("ebb-llm-gateway/build.gradle.kts"));
        assertTrue(build.contains("com.openai:openai-java"), "gateway must use the official OpenAI Java SDK dependency");
        String gateway = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java"));
        assertTrue(gateway.contains("/v1/chat/message"));
        assertTrue(gateway.contains("SimpleCircuitBreaker"));
        String provider = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/OpenAiResponsesChatProvider.java"));
        assertTrue(provider.contains("ResponseCreateParams"));
        assertTrue(provider.contains("createStreaming"));
        assertTrue(provider.contains("ResponseFormatTextJsonSchemaConfig"));
        assertTrue(provider.contains("store(request.store() && allowStore)"));
        String service = Files.readString(Path.of("src/main/java/com/crpg/ebb/llm/LlmChatService.java"));
        assertTrue(service.contains("new HttpLlmGatewayClient"));
    }

    @Test
    void p36GatewayAuthFlowRequiresLoginAndKeepsTokensServerSide() throws Exception {
        try {
            UUID player = UUID.randomUUID();
            LlmConfig config = new LlmConfig(true, LlmMode.FAKE, "", LlmConfig.DEFAULT_GATEWAY_TIMEOUT_MS,
                    true, LlmConfig.DEFAULT_CHAT_MODEL, true, true, false, 128, 256, 20, 10, "FAKE_NPC_REPLY");
            LlmConfig.setForTesting(config);
            LlmAuthService.setClientForTesting(new DevLocalLlmAuthClient());

            assertEquals("auth_required", LlmAuthService.chatGateStatus(player, config),
                    "P36 fake/gateway chat should be gated when require_player_auth=true and no token is stored");
            var start = LlmAuthService.startDeviceAuth(player, "junit-server").get();
            assertTrue(start.started(), "dev local auth should start without an external browser dependency");
            assertFalse(start.authSessionId().isBlank());
            assertTrue(start.verificationUrl().contains("auth_session_id"));

            var status = LlmAuthService.pollDeviceAuth(player).get();
            assertTrue(status.authenticated(), "dev local provider should authenticate on status poll");
            String opaque = status.token().orElseThrow().opaqueToken();
            assertTrue(LlmAuthService.hasValidToken(player));
            assertEquals("authenticated", LlmAuthService.chatGateStatus(player, config));
            String safeLine = LlmAuthService.safeStatusLine(player);
            assertTrue(safeLine.contains("token=redacted"), safeLine);
            assertFalse(safeLine.contains(opaque), "status/debug surfaces must not print the raw opaque player token");

            LlmChatResponse fake = new FakeLlmGatewayClient(config).sendMessage(new LlmChatRequest(
                    UUID.randomUUID(), player, Optional.empty(), Identifier.parse("ebb:test/llm"), "start",
                    "ebb:demo/innkeeper", "innkeeper", "auth smoke", "hello after login", 7L
            )).get();
            assertTrue(fake.reply().contains("FAKE_NPC_REPLY"), "logged-in fake provider chat should still work");

            assertTrue(LlmAuthService.logout(player).get(), "logout should remove and revoke the server token");
            assertFalse(LlmAuthService.hasValidToken(player));
            assertEquals("auth_required", LlmAuthService.chatGateStatus(player, config),
                    "logout should invalidate the auth gate");
        } finally {
            LlmChatService.clearTestingOverrides();
        }
    }

    @Test
    void p36GatewayProjectConfigAndCommandSurfaceAreAuditable() throws Exception {
        JsonObject json = JsonParser.parseString("""
                {"enabled":true,"mode":"gateway","gateway_base_url":"http://127.0.0.1:8787","gateway_timeout_ms":12345,"require_player_auth":false}
                """).getAsJsonObject();
        LlmConfig parsed = LlmConfig.parse(json);
        assertEquals(LlmMode.GATEWAY, parsed.mode());
        assertEquals("http://127.0.0.1:8787", parsed.gatewayUrl());
        assertEquals(12345, parsed.gatewayTimeoutMs());
        assertFalse(parsed.requirePlayerAuth());
        assertTrue(parsed.toSafeJson().has("require_player_auth"));
        assertFalse(parsed.toString().contains("opaque_player_token"));

        for (String rel : java.util.List.of(
                "ebb-llm-gateway/build.gradle.kts",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/DevLocalAuthProvider.java",
                "ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/auth/OidcAuthProvider.java",
                "src/main/java/com/crpg/ebb/network/llm/LlmAuthStartPayload.java",
                "src/main/java/com/crpg/ebb/network/llm/LlmAuthStatusRequestPayload.java",
                "src/main/java/com/crpg/ebb/network/llm/LlmAuthUrlPayload.java",
                "src/main/java/com/crpg/ebb/network/llm/LlmAuthStatusPayload.java",
                "src/main/java/com/crpg/ebb/network/llm/NpcProfileSyncPayload.java",
                "src/main/java/com/crpg/ebb/network/llm/MemoryDebugSnapshotPayload.java",
                "scripts/p36_gateway_smoke.sh")) {
            assertTrue(Files.isRegularFile(Path.of(rel)), "P36 file should exist: " + rel);
        }
        String gateway = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/GatewayServer.java"));
        assertTrue(gateway.contains("/v1/health"));
        assertTrue(gateway.contains("/v1/auth/device/start"));
        assertTrue(gateway.contains("/v1/auth/device/status"));
        assertTrue(gateway.contains("/v1/player/quota"));
        assertTrue(gateway.contains("/v1/npc/profile/ensure"));
        assertTrue(gateway.contains("/v1/chat/start"));
        assertTrue(gateway.contains("/v1/chat/cancel"));
        assertTrue(gateway.contains("/v1/chat/session"));
        assertTrue(gateway.contains("/v1/knowledge/update"));

        String clientScreen = Files.readString(Path.of("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java"));
        assertFalse(clientScreen.contains("opaque_player_token"), "client UI must not know or log opaque player tokens");
        String packets = Files.readString(Path.of("src/main/java/com/crpg/ebb/network/ModPackets.java"));
        assertTrue(packets.contains("LlmAuthStartPayload"));
        assertTrue(packets.contains("LlmAuthStatusRequestPayload"));
        assertTrue(packets.contains("LlmAuthUrlPayload"));
        assertTrue(packets.contains("LlmAuthStatusPayload"));
        assertTrue(packets.contains("NpcProfileSyncPayload"));
        assertTrue(packets.contains("MemoryDebugSnapshotPayload"));

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "llm", "auth");
        requireCommandPath(ebb, "llm", "status");
        requireCommandPath(ebb, "llm", "logout");
        requireCommandPath(ebb, "llm", "quota");
        requireCommandPath(ebb, "llm", "consent", "view");
        requireCommandPath(ebb, "llm", "consent", "revoke");
        requireCommandPath(ebb, "llm", "auth_debug", "player");
    }

    @Test
    void p35NpcProfilesLoadAndResolveByBinding() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));

        assertTrue(NpcProfileRegistry.size() >= 6, "P35 requires the six P30 role NPC profiles");
        for (String role : java.util.List.of("innkeeper", "witness", "tenant", "guard", "cook", "courier")) {
            Identifier profileId = Identifier.parse("ebb:demo/" + role);
            var profile = NpcProfileRegistry.byId(profileId).orElseThrow();
            assertEquals(NpcTier.MAJOR_SCRIPTED, profile.tier(), role + " should be a scripted major NPC");
            assertTrue(profile.llm().enabled(), role + " profile should allow default fake/gateway chat policy");
            assertFalse(profile.character().speechRules().isEmpty(), role + " should have authored speech rules");
        }
        var innkeeperBinding = EntityBindingRegistry.definitions().get(Identifier.parse("ebb:demo/innkeeper_ebb_npc"));
        assertNotNull(innkeeperBinding, "innkeeper binding should load");
        assertEquals(Optional.of(Identifier.parse("ebb:demo/innkeeper")), innkeeperBinding.npcProfileId());
        assertEquals(NpcTier.MAJOR_SCRIPTED, innkeeperBinding.npcTier());
        assertEquals(Identifier.parse("ebb:demo/innkeeper"),
                NpcProfileRegistry.byEntityBinding(Identifier.parse("ebb:demo/innkeeper_ebb_npc")).orElseThrow().id());

        var minorBinding = EntityBindingRegistry.definitions().get(Identifier.parse("ebb:llm/minor_villager"));
        assertNotNull(minorBinding, "minor villager candidate binding should load");
        assertEquals(NpcTier.MINOR_GENERATABLE, minorBinding.npcTier());
        assertTrue(minorBinding.promoteOnFirstChat(), "minor candidate should promote on first chat");
        assertTrue(minorBinding.profileSeedArchetypes().contains("townsperson"));
    }

    @Test
    void p35PromotedProfilesPersistThroughSavedDataCodec() {
        NarrativeSavedData state = new NarrativeSavedData();
        JsonObject profile = JsonParser.parseString("""
                {
                  "id":"ebb:promoted/test_minor",
                  "tier":"major_promoted",
                  "display_name":"Promoted Test Minor",
                  "entity_uuid":"00000000-0000-0000-0000-000000000001",
                  "character":{"archetype":"townsperson","voice":"plain"},
                  "stance":{"default_attitude_to_player":"curious"},
                  "knowledge":{"initial_packs":["ebb:demo/tavern_public_lore"]},
                  "promotion":{"can_be_demoted":false}
                }
                """).getAsJsonObject();
        state.putPromotedNpcProfile("ebb:promoted/test_minor", profile);
        state.setWorldNpcState("ebb:promoted/test_minor", "promoted_major", true);

        assertTrue(state.hasPromotedNpcProfile("ebb:promoted/test_minor"));
        assertEquals("Promoted Test Minor", state.promotedNpcProfile("ebb:promoted/test_minor").orElseThrow().get("display_name").getAsString());
        assertTrue(state.summaryLine().contains("promoted_npc_profiles=1"));
        assertTrue(state.promotedNpcProfileDebugLines(8).stream().anyMatch(line -> line.contains("major_promoted")));

        var encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(message -> { throw new AssertionError(message); })
                .orElseThrow();
        NarrativeSavedData restored = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> { throw new AssertionError(message); })
                .orElseThrow();
        assertEquals(NarrativeSavedData.CURRENT_SCHEMA_VERSION, restored.schemaVersion());
        assertTrue(restored.hasPromotedNpcProfile("ebb:promoted/test_minor"));
        assertEquals("major_promoted", restored.promotedNpcProfile("ebb:promoted/test_minor").orElseThrow().get("tier").getAsString());
    }

    @Test
    void p35NpcCommandSurfaceIsRegistered() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "npc", "profile", "target");
        requireCommandPath(ebb, "npc", "profile", "npc_key");
        requireCommandPath(ebb, "npc", "minorize", "entity");
        requireCommandPath(ebb, "npc", "promote", "entity");
        requireCommandPath(ebb, "npc", "regenerate_profile", "npc_key");

        String commands = Files.readString(Path.of("src/main/java/com/crpg/ebb/registry/ModCommands.java"));
        assertTrue(commands.contains("NpcPromotionService.ensurePromotedProfile"));
        assertTrue(commands.contains("NpcProfileRegistry.byEntityBinding"));
    }

    @Test
    void p42LlmChatUiCompletionSurfacesAreAuditable() throws Exception {
        List<String> chunks = LlmChatService.streamingChunks(
                "This is a deliberately long fake reply for the streaming text verifier. "
                        + "It should split into multiple chunks so the client can merge them into one visible NPC line.",
                48
        );
        assertTrue(chunks.size() > 1, "P42 streaming helper should split long replies into multiple chunks");
        assertEquals(String.join("", chunks), "This is a deliberately long fake reply for the streaming text verifier. "
                + "It should split into multiple chunks so the client can merge them into one visible NPC line.");

        String screen = Files.readString(Path.of("src/client/java/com/crpg/ebb/client/gui/llm/NpcChatScreen.java"));
        assertTrue(screen.contains("appendNpcChunk"), "client should merge streaming NPC chunks instead of appending each chunk as a new line");
        assertTrue(screen.contains("return_to_script"), "P42 requires a return-to-script button/action");
        assertTrue(screen.contains("memory_correction"), "P42 requires a memory correction button/action");
        assertTrue(screen.contains("renderCitationsOverlay"), "P42 requires a dev citations overlay");
        assertTrue(screen.contains("CLIENT_REPLY_TIMEOUT_MS"), "P42 timeout/cancel/error handling should not leave controls stuck");
        assertFalse(screen.contains("String.join(\", \", payload.citationIds())"),
                "citations should be hidden behind the dev overlay instead of appended inline to every NPC line");

        String service = Files.readString(Path.of("src/main/java/com/crpg/ebb/llm/LlmChatService.java"));
        assertTrue(service.contains("sendStreamingNpcResponse"), "server should emit chunk payloads for streaming text");
        assertTrue(service.contains("DialogueService.reopenFromLlmChat"), "server-authoritative return to scripted dialogue should be wired");
        String dialogueService = Files.readString(Path.of("src/main/java/com/crpg/ebb/dialogue/DialogueService.java"));
        assertTrue(dialogueService.contains("reopenFromLlmChat"), "DialogueService should reopen a scripted DialogueScreen from LLM chat metadata");

        String menu = Files.readString(Path.of("src/client/java/com/crpg/ebb/client/gui/menu/EbbMenuScreen.java"))
                + Files.readString(Path.of("src/client/java/com/crpg/ebb/client/gui/llm/LlmAuthStatusWidget.java"));
        assertTrue(menu.contains("screen.ebb.menu.llm_auth_status_hint"));
        assertTrue(menu.contains("ebb llm status"));
        assertTrue(menu.contains("ebb llm auth"));

        String gui = Files.readString(Path.of("scripts/gui_e2e_run.py"));
        assertTrue(gui.contains("scenario_llm_chat"), "P42 GUI E2E scenario should be present");
        assertTrue(gui.contains("llm_suggested_option_clicked"), "GUI E2E should click a suggested option");
        assertTrue(gui.contains("llm_return_to_script"), "GUI E2E should exercise return to scripted dialogue");

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        Method register = ModCommands.class.getDeclaredMethod("registerEbbCommand", CommandDispatcher.class);
        register.setAccessible(true);
        register.invoke(null, dispatcher);
        CommandNode<CommandSourceStack> ebb = requireChild(dispatcher.getRoot(), "ebb");
        requireCommandPath(ebb, "llm", "status");
        requireCommandPath(ebb, "llm", "auth");
        requireCommandPath(ebb, "llm", "logout");
    }

    @Test
    void p43TestingEvaluationAndSafetyGatesAreAuditable() throws Exception {
        JsonObject npcProfileSchema = JsonParser.parseString(Files.readString(Path.of("docs/schemas/ebb.npc_profile.schema.json"))).getAsJsonObject();
        JsonObject npcKnowledgeSchema = JsonParser.parseString(Files.readString(Path.of("docs/schemas/ebb.npc_knowledge.schema.json"))).getAsJsonObject();
        assertTrue(npcProfileSchema.toString().contains("major_promoted"), "P43 NPC profile schema should cover promoted profiles");
        assertTrue(npcKnowledgeSchema.toString().contains("reveal_conditions"), "P43 NPC knowledge schema should require reveal conditions for secret chunks");

        String docs = Files.readString(Path.of("docs/json_authoring_guide.md"));
        assertTrue(docs.contains("P43 Testing / Evaluation Authoring Reference"));
        assertTrue(docs.contains("NPC profile data"));
        assertTrue(docs.contains("NPC knowledge packs / KB"));
        assertTrue(docs.contains("LLM server config"));
        assertTrue(docs.contains("Memory effects and LLM memory writes"));

        String safetyAudit = Files.readString(Path.of("scripts/p43_llm_safety_audit.py"));
        assertTrue(safetyAudit.contains("audit_no_api_key_literals"));
        assertTrue(safetyAudit.contains("audit_fake_provider_in_tests"));
        assertTrue(safetyAudit.contains("audit_hidden_knowledge_not_in_client_sync"));
        assertTrue(safetyAudit.contains("audit_high_risk_effects_not_direct_llm_output"));

        String gatewayResponse = Files.readString(Path.of("ebb-llm-gateway/src/main/java/com/crpg/ebb/gateway/chat/GatewayChatResponse.java"));
        assertTrue(gatewayResponse.contains("sanitizeProposedEffects"));
        assertTrue(gatewayResponse.contains("high_risk_effects_rejected_from_llm_output"));
        assertTrue(gatewayResponse.contains("complete_quest_branch"));
        assertFalse(Files.readString(Path.of("src/main/java/com/crpg/ebb/llm/HttpLlmGatewayClient.java")).contains("proposed_effects"),
                "Minecraft-side gateway client should ignore direct LLM proposed_effects");

        String smoke = Files.readString(Path.of("ebb-llm-gateway/src/test/java/com/crpg/ebb/gateway/GatewaySmoke.java"));
        assertTrue(smoke.contains("favorite=blue") && smoke.contains("favorite=red"),
                "P43 JUnit should confirm gateway smoke contains explicit memory conflict coverage");
        assertTrue(smoke.contains("FAKE_GATEWAY_REPLY") && smoke.contains("mock_openai_responses"),
                "P43 tests/smokes should use fake/mock providers");

        NarrativeSavedData state = new NarrativeSavedData();
        JsonObject profile = JsonParser.parseString("""
                {"id":"ebb:promoted/p43","tier":"major_promoted","display_name":"P43 Promoted",
                 "profile_generation":{"schema_id":"ebb.npc_profile_generator.v1"},
                 "character":{"archetype":"witness","voice":"careful","speech_rules":["stay consistent"]},
                 "stance":{"default_attitude_to_player":"curious"},
                 "knowledge":{"initial_packs":["ebb:demo/tavern_public_lore"]},
                 "knowledge_seed":{"initial_packs":["ebb:demo/tavern_public_lore"]},
                 "suggested_options":["你看见了什么？"]}
                """).getAsJsonObject();
        state.putPromotedNpcProfile("ebb:promoted/p43", profile);
        var encoded = NarrativeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .resultOrPartial(message -> { throw new AssertionError(message); })
                .orElseThrow();
        NarrativeSavedData restored = NarrativeSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .resultOrPartial(message -> { throw new AssertionError(message); })
                .orElseThrow();
        assertTrue(restored.hasPromotedNpcProfile("ebb:promoted/p43"),
                "P43 promotion persistence should survive saved-data codec round trip");
        assertTrue(restored.promotedNpcProfile("ebb:promoted/p43").orElseThrow().has("suggested_options"));

        Path bundled = Path.of("src/main/resources/data/ebb");
        NpcProfileRegistry.rebuild(load(bundled.resolve("npc_profiles")));
        NpcKnowledgeRegistry.rebuild(load(bundled.resolve("npc_knowledge_packs")));
        UUID player = UUID.randomUUID();
        String publicContext = NpcKnowledgeService.promptContext("ebb:demo/innkeeper", "guestbook ledger", new NarrativeSavedData(), player, 6000L, 8);
        assertTrue(publicContext.contains("visible chunks only"));
        assertTrue(publicContext.contains("ebb:demo/tavern_public_lore") || publicContext.contains("ledger_evasion_public"),
                "P43 prompt pack assembly should include visible profile KB packs");
        assertFalse(publicContext.toLowerCase(Locale.ROOT).contains("tenant paid cash"),
                "P43 prompt pack assembly must not include hidden KB without reveal state");

        String gui = Files.readString(Path.of("scripts/gui_e2e_run.py"));
        assertTrue(gui.contains("scenario_llm_validation"));
        assertTrue(gui.contains("auth_disabled"));
        assertTrue(gui.contains("fake_provider_chat_route"));
        assertTrue(gui.contains("real_gateway_dry_run"));
    }

    private static DialogueEffect parseEffect(String json) {
        return DialogueEffect.parse(JsonParser.parseString(json).getAsJsonObject(), "test.effect", new java.util.ArrayList<>()).orElseThrow();
    }

    private static Map<Identifier, JsonObject> load(Path root) throws Exception {
        Map<Identifier, JsonObject> map = new LinkedHashMap<>();
        if (!Files.exists(root)) return map;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                Path rel = root.relativize(path);
                String idPath = rel.toString().replace('\\', '/').replaceAll("\\.json$", "");
                map.put(Identifier.fromNamespaceAndPath("ebb", idPath), JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            }
        }
        return map;
    }

    private static void requireCommandPath(CommandNode<CommandSourceStack> root, String... path) {
        CommandNode<CommandSourceStack> cursor = root;
        for (String segment : path) {
            cursor = requireChild(cursor, segment);
        }
        assertNotNull(cursor.getCommand(), "command path should execute: /ebb " + String.join(" ", path));
    }

    private static CommandNode<CommandSourceStack> requireChild(CommandNode<CommandSourceStack> root, String name) {
        CommandNode<CommandSourceStack> child = root.getChild(name);
        assertNotNull(child, "command node should exist: " + name);
        return child;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> privateStaticMap(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<K, V>) field.get(null);
    }
}
