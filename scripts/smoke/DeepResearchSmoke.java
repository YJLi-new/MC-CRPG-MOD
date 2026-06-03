import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeDefinition;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.chime.ChimeResolver;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.conflict.ConflictService;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueEffect;
import com.crpg.ebb.dialogue.DialogueNodeType;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.dialogue.RollMode;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionTargetType;
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
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeepResearchSmoke {
    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

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
        require(DialogueRegistry.size() >= 3, "bundled dialogues should load");
        require(AttributeRegistry.size() == 8, "DND-8 attributes should load");
        require(QuestBranchRegistry.size() >= 2, "bundled quest branches should load");
        require(FeatRegistry.size() >= 4, "bundled feats should load");
        require(ChimeRegistry.size() >= 8, "bundled P26 chimes should load all eight attribute voices");
        require(JournalEntryRegistry.size() >= 4, "bundled journal entries should load");
        require(RelationshipRegistry.size() >= 4, "bundled relationships should load");
        require(InvestigationRegistry.clueCount() >= 5, "bundled clues should load");
        require(InvestigationRegistry.sceneCount() >= 1, "bundled investigation scene should load");
        require(ConflictRegistry.size() >= 1, "bundled conflict should load");
        require(BlockGroupIndex.groupCount() >= 8, "vertical slice should expose at least 8 interactable points");
        require(DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).isPresent(), "ending placeholder dialogue should load");

        JsonObject invalidNoFailure = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[{"id":"bad","type":"action","text":"bad","check":{"attribute":"charisma","dc":10,"success":"ok"}}]},"ok":{"text":"ok"}}}
                """).getAsJsonObject();
        java.util.ArrayList<String> messages = new java.util.ArrayList<>();
        require(DialogueDefinition.parse(Identifier.parse("ebb:test/no_failure"), invalidNoFailure, messages).isEmpty(), "checked choices must fail forward");
        require(messages.stream().anyMatch(message -> message.contains("failing forward")), "failing-forward validation message should be present");

        NarrativeSavedData state = new NarrativeSavedData();
        UUID player = UUID.randomUUID();
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"set_var\",\"key\":\"clerk_attitude\",\"value\":\"intimidated\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require("intimidated".equals(state.getPlayerVariable(player, "clerk_attitude")), "set_var effect writes player variable");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"add_trait\",\"key\":\"forger\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require(state.hasPlayerFlag(player, "trait:forger"), "add_trait effect writes normalized trait flag");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"unlock_retry\",\"key\":\"office_backdoor_check\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require(state.hasPlayerFlag(player, "unlock:office_backdoor_check"), "unlock_retry effect writes normalized unlock flag");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"set_story_var\",\"layer\":\"branch\",\"key\":\"tavern_route\",\"value\":\"public\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"add_story_int\",\"layer\":\"major\",\"key\":\"innkeeper_trust\",\"amount\":2}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"set_story_var\",\"layer\":\"minor\",\"key\":\"met_innkeeper\",\"value\":true}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require("public".equals(state.getPlayerStoryVariable(player, StoryVarLayer.BRANCH, "tavern_route")), "branch story var stored");
        require("2".equals(state.getPlayerStoryVariable(player, StoryVarLayer.MAJOR, "innkeeper_trust")), "major story int incremented");
        require("true".equals(state.getPlayerStoryVariable(player, StoryVarLayer.MINOR, "met_innkeeper")), "minor bool story var stored");
        TakeRootService.completeBranch(state, player, "ebb:demo/tavern_public").orElseThrow();
        require("take_rooted".equals(state.getQuestState(player, "ebb:demo/tavern_public")), "major quest branch take-rooted");
        require(state.hasFeat(player, "ebb:demo/tavern_authority"), "take-root grants feat");
        require(FeatRegistry.totalCheckModifier(state, player, "charisma") >= 1, "feat modifies charisma checks");
        var innkeeper = DialogueRegistry.byId(Identifier.parse("ebb:demo/innkeeper_intro")).orElseThrow();
        var startNode = innkeeper.node("start").orElseThrow();
        // P26 chime coverage: each attribute voice has tone/cooldown/active-route metadata.
        Map<String, String> expectedChimeAttributes = new LinkedHashMap<>();
        expectedChimeAttributes.put("dread", "luck");
        expectedChimeAttributes.put("empathy", "wisdom");
        expectedChimeAttributes.put("endurance", "constitution");
        expectedChimeAttributes.put("finesse", "dexterity");
        expectedChimeAttributes.put("force", "strength");
        expectedChimeAttributes.put("instinct", "perception");
        expectedChimeAttributes.put("logic", "intelligence");
        expectedChimeAttributes.put("rhetoric", "charisma");
        require(Set.copyOf(expectedChimeAttributes.values()).equals(ChimeRegistry.definitions().values().stream()
                .map(ChimeDefinition::sourceAttribute)
                .collect(java.util.stream.Collectors.toSet())), "P26 chimes should cover DND-8 attributes exactly");
        for (Map.Entry<String, String> entry : expectedChimeAttributes.entrySet()) {
            ChimeDefinition definition = ChimeRegistry.byId(Identifier.parse("ebb:demo/" + entry.getKey())).orElseThrow();
            require(!definition.toneGuide().isBlank(), "P26 chime toneGuide present for " + entry.getKey());
            require(definition.cooldownTicks() > 0, "P26 chime cooldownTicks present for " + entry.getKey());
            require(definition.oneShotPerNode(), "P26 chime one-shot tuning present for " + entry.getKey());
            require(!definition.activeThoughtIds().isEmpty(), "P26 chime active thought metadata present for " + entry.getKey());
            String choiceId = "rhetoric".equals(entry.getKey()) ? "rhetoric_insight" : entry.getKey() + "_chime_thought";
            require(startNode.choice(choiceId).isPresent(), "P26 active thought route exists for " + entry.getKey());
        }
        state.setAttribute(player, "charisma", 1);
        require(ChimeResolver.resolve(innkeeper, startNode, state, player, 1000L).orElseThrow().contains("Rhetoric"), "rhetoric chime resolves from charisma build");
        require(ChimeResolver.resolve(innkeeper, startNode, state, player, 1001L).isEmpty(), "chime one-shot prevents repeated same-node passive insert");
        require(startNode.choice("rhetoric_insight").orElseThrow().conditions().getFirst().matches(state, player), "passive chime unlocks insight path");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"add_journal_entry\",\"id\":\"ebb:demo/door_scratches\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require(state.hasJournalEntry(player, "ebb:demo/door_scratches"), "journal entry effect persists clue");
        require(JournalService.addEntry(state, player, "ebb:demo/public_pressure").orElseThrow().contains("journal_entry_added"), "journal service returns UI status");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"add_relation\",\"relation\":\"ebb:demo/innkeeper\",\"amount\":2}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"set_npc_state\",\"npc\":\"ebb:demo/innkeeper\",\"tag\":\"guarded\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require(state.getRelation(player, "ebb:demo/innkeeper") == 2, "relationship effect writes score");
        require(state.hasPlayerNpcState(player, "ebb:demo/innkeeper", "guarded"), "npc state effect persists memory tag");
        JsonObject relationshipDialogue = JsonParser.parseString("""
                {"start":"start","nodes":{"start":{"text":"x","choices":[
                  {"id":"relation","type":"dialogue","text":"relation","conditions":[{"type":"relation_at_least","relation":"ebb:demo/innkeeper","min":2}]},
                  {"id":"npc","type":"dialogue","text":"npc","conditions":[{"type":"npc_state","npc":"ebb:demo/innkeeper","tag":"guarded"}]},
                  {"id":"night","type":"thought","text":"night","conditions":[{"type":"time_window","start":12000,"end":24000}]}
                ]}}}
                """).getAsJsonObject();
        DialogueDefinition relationshipDefinition = DialogueDefinition.parse(Identifier.parse("ebb:test/relationships"), relationshipDialogue, new java.util.ArrayList<>()).orElseThrow();
        require(relationshipDefinition.node("start").orElseThrow().choice("relation").orElseThrow().conditions().getFirst().matches(state, player), "relationship condition matches");
        require(relationshipDefinition.node("start").orElseThrow().choice("npc").orElseThrow().conditions().getFirst().matches(state, player), "npc state condition matches");
        require(relationshipDefinition.node("start").orElseThrow().choice("night").orElseThrow().conditions().getFirst().matches(state, player, 18000L), "time-window condition matches night");
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/door_scratches\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        DialogueEffect.parse(JsonParser.parseString("{\"type\":\"reveal_clue\",\"id\":\"ebb:demo/witness_knock_pattern\"}").getAsJsonObject(), "effect", new java.util.ArrayList<>())
                .orElseThrow().apply(state, player);
        require(state.hasClue(player, "ebb:demo/door_scratches"), "reveal_clue persists investigation clue");
        require(state.hasJournalEntry(player, "ebb:demo/door_scratches"), "linked clue reveal writes journal entry");
        require(InvestigationRegistry.totalCheckModifier(state, player, "charisma") >= 1, "clue-to-DC hook contributes charisma modifier");
        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        ConflictService.addStress(state, player, "ebb:demo/hallway_confrontation", 3);
        require("failed_forward".equals(state.getConflictState(player, "ebb:demo/hallway_confrontation")), "conflict stress fails forward");
        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        ConflictService.addResolve(state, player, "ebb:demo/hallway_confrontation", 2);
        require("resolved".equals(state.getConflictState(player, "ebb:demo/hallway_confrontation")), "conflict resolve can succeed");
        var hallwayConflict = ConflictRegistry.byId(Identifier.parse("ebb:demo/hallway_confrontation")).orElseThrow();
        require(hallwayConflict.phases().containsAll(java.util.List.of("setup", "pressure", "turn", "consequence", "resolution")),
                "P28 conflict phases are formalized");
        require(hallwayConflict.outcomes().stream().filter(outcome -> outcome.isFailureForwardKind()).count() >= 2,
                "P28 conflict has at least two failure-forward outcomes");
        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        String p28Status = ConflictService.statusLine(state, player, Identifier.parse("ebb:demo/hallway_confrontation"));
        require(p28Status.contains("stress=0/3") && p28Status.contains("resolve=0/2") && p28Status.contains("leverage="),
                "P28 conflict status exposes stress, resolve, and leverage");
        ConflictService.applyOutcome(state, player, "ebb:demo/hallway_confrontation", "quiet_resolve");
        require("resolved_nonviolent".equals(state.getConflictState(player, "ebb:demo/hallway_confrontation")),
                "P28 quiet nonviolent conflict outcome applies");
        ConflictService.start(state, player, "ebb:demo/hallway_confrontation");
        ConflictService.applyOutcome(state, player, "ebb:demo/hallway_confrontation", "public_pressure_fail");
        require("failed_forward_public".equals(state.getConflictState(player, "ebb:demo/hallway_confrontation"))
                        && "consequence".equals(state.getConflictPhase(player, "ebb:demo/hallway_confrontation")),
                "P28 failure-forward conflict outcome advances consequence phase");
        JsonObject legacySave = JsonParser.parseString("""
                {
                  "version": 1,
                  "players": {
                    "%s": {
                      "narrative_states": {"conflict:ebb:demo/hallway_confrontation": "failed_forward"},
                      "conflict_scores": {"stress:ebb:demo/hallway_confrontation": 3}
                    }
                  }
                }
                """.formatted(player)).getAsJsonObject();
        NarrativeSavedData migrated = NarrativeSavedData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, legacySave)
                .resultOrPartial(message -> {
                    throw new IllegalStateException(message);
                })
                .orElseThrow();
        require(migrated.schemaVersion() == NarrativeSavedData.CURRENT_SCHEMA_VERSION, "P29 save migration bumps schema version");
        require("consequence".equals(migrated.getConflictPhase(player, "ebb:demo/hallway_confrontation")),
                "P29 save migration infers conflict phase from legacy state");
        DialogueService.clearAll("p29_smoke");
        DialogueService.clearSecurityEventSnapshot();
        Map<UUID, DialogueSession> sessions = privateStaticMap(DialogueService.class, "SESSIONS");
        Map<UUID, UUID> playerToSession = privateStaticMap(DialogueService.class, "PLAYER_TO_SESSION");
        UUID conversation = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        DialogueSession p29Session = new DialogueSession(
                conversation,
                player,
                Identifier.parse("ebb:demo/guard_intro"),
                Identifier.parse("ebb:demo/guard_ebb_npc"),
                InteractionTargetType.ENTITY,
                java.util.Optional.of(entity),
                "start",
                100L
        );
        sessions.put(conversation, p29Session);
        playerToSession.put(player, conversation);
        require(DialogueService.validateChoicePacket(player, conversation, 101L).allowed(), "P29 own session packet allowed");
        require("session_player_mismatch".equals(DialogueService.validateChoicePacket(playerTwo, conversation, 101L).reason()),
                "P29 spoofed session packet rejected");
        require(DialogueService.entityReservedByAnotherPlayer(entity, playerTwo), "P29 same-NPC contention detected");
        require(DialogueService.securityEventSnapshot().containsKey("session_player_mismatch"), "P29 security event snapshot records spoofing");
        DialogueService.clearAll("p29_smoke_cleanup");
        DialogueService.clearSecurityEventSnapshot();
        require(BlockGroupIndex.groupCount() >= 12, "P30 vertical slice exposes at least 12 block-group investigation points");
        require(EntityBindingRegistry.size() >= 14, "P30 vertical slice includes cook/courier NPC bindings");
        require(NpcRoutineRegistry.size() >= 7, "P30 vertical slice includes cook/courier routines");
        long p30MajorBranches = QuestBranchRegistry.orderedDefinitions().stream()
                .filter(branch -> branch.kind() == com.crpg.ebb.quest.QuestBranchKind.MAJOR)
                .count();
        long p30MinorBranches = QuestBranchRegistry.orderedDefinitions().stream()
                .filter(branch -> branch.kind() == com.crpg.ebb.quest.QuestBranchKind.MINOR)
                .count();
        require(p30MajorBranches >= 4 && p30MinorBranches >= 8, "P30 quest branch counts meet major/minor target");
        require(FeatRegistry.size() >= 12, "P30 feat count meets target");
        int p30ChimeLines = ChimeRegistry.orderedDefinitions().stream().mapToInt(chime -> chime.lines().size()).sum();
        require(ChimeRegistry.size() >= 8 && p30ChimeLines >= 40, "P30 chime count and line count meet target");
        require(JournalEntryRegistry.size() >= 20 && InvestigationRegistry.clueCount() >= 20, "P30 journal/clue counts meet target");
        require(ConflictRegistry.size() >= 3, "P30 conflict count meets target");
        require(DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).orElseThrow().node("trade_end").isPresent()
                        && DialogueRegistry.byId(Identifier.parse("ebb:demo/back_door_dialogue")).orElseThrow().node("mercy_end").isPresent(),
                "P30 ending placeholders include trade and mercy routes");

        Path generated = Path.of("build/generated/ebb_authoring/data/ebb");
        DialogueRegistry.rebuild(load(generated.resolve("dialogues")));
        BlockGroupIndex.rebuild(load(generated.resolve("interactions/block_groups")));
        EntityBindingRegistry.rebuild(load(generated.resolve("interactions/entity_bindings")));
        NpcRoutineRegistry.rebuild(load(generated.resolve("npc_routines")));
        var clerk = DialogueRegistry.byId(Identifier.parse("ebb:harbor_clerk_intro")).orElseThrow();
        require(clerk.node("start").orElseThrow().type() == DialogueNodeType.LINE, "compiled YAML preserves node type");
        var bluff = clerk.node("choices_main").orElseThrow().choice("bluff_badge").orElseThrow();
        require(bluff.singleUse(), "ONE_SHOT checks become single-use choices");
        require(bluff.check().orElseThrow().mode() == RollMode.ONE_SHOT, "roll mode parsed");
        require(bluff.check().orElseThrow().staticModifier() == 2, "modifiers array contributes static modifier");
        require(bluff.check().orElseThrow().failureEffects().size() == 2, "failure effects parsed from authoring YAML");
        var inner = clerk.node("choices_main").orElseThrow().choice("inner_read").orElseThrow();
        require(!inner.conditions().getFirst().matches(state, player), "flag-gated thought hidden before flag");
        state.setPlayerFlag(player, "noticed_tired_face", true);
        require(inner.conditions().getFirst().matches(state, player), "flag-gated thought visible after flag");
        require(BlockGroupIndex.groupCount() == 1, "compiled interactable block group loaded");
        require(EntityBindingRegistry.size() == 1, "compiled NPC entity binding loaded");
        require(NpcRoutineRegistry.size() == 1, "compiled NPC routine loaded");

        System.out.println("DeepResearchSmoke passed: generated_dialogues=" + DialogueRegistry.size()
                + ", generated_block_groups=" + BlockGroupIndex.groupCount()
                + ", generated_entity_bindings=" + EntityBindingRegistry.size()
                + ", generated_routines=" + NpcRoutineRegistry.size());
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

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> privateStaticMap(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<K, V>) field.get(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
