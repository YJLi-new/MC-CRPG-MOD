package com.crpg.ebb;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.chime.ChimeResolver;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.conflict.ConflictService;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueEffect;
import com.crpg.ebb.dialogue.DialogueNodeType;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.RollMode;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.journal.JournalEntryRegistry;
import com.crpg.ebb.journal.JournalService;
import com.crpg.ebb.quest.QuestBranchRegistry;
import com.crpg.ebb.quest.TakeRootService;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.story.StoryVarLayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(QuestBranchRegistry.size() >= 2, "bundled quest branches should load");
        assertTrue(FeatRegistry.size() >= 4, "bundled feats should load");
        assertTrue(ChimeRegistry.size() >= 4, "bundled chimes should load");
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
    void playableVerticalSliceMeetsContentMinimums() throws Exception {
        Path bundled = Path.of("src/main/resources/data/ebb");
        DialogueRegistry.rebuild(load(bundled.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(bundled.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(bundled.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(bundled.resolve("npc_routines")));
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
        assertTrue(ChimeRegistry.size() >= 4, "four chimes should be present");
        assertTrue(InvestigationRegistry.clueCount() >= 5, "investigation clues should be present");
        assertTrue(ConflictRegistry.size() >= 1, "one set-piece conflict should be present");
        DialogueDefinition ending = DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).orElseThrow();
        assertTrue(ending.node("public_end").isPresent(), "public ending placeholder should exist");
        assertTrue(ending.node("quiet_end").isPresent(), "quiet ending placeholder should exist");
        assertTrue(ending.node("messy_end").isPresent(), "fail-forward ending placeholder should exist");
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
}
