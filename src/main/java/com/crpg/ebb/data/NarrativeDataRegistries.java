package com.crpg.ebb.data;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.journal.JournalEntryRegistry;
import com.crpg.ebb.quest.QuestBranchRegistry;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.npc.knowledge.NpcKnowledgeRegistry;
import com.crpg.ebb.dialogue.DialogueRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.stream.Collectors;

public final class NarrativeDataRegistries {
    public static final JsonDataRegistry DIALOGUES = new JsonDataRegistry("dialogue", "dialogues");
    public static final JsonDataRegistry INTERACTION_SETTINGS = new JsonDataRegistry("interaction settings", "interactions/settings");
    public static final JsonDataRegistry BLOCK_GROUPS = new JsonDataRegistry("block group", "interactions/block_groups");
    public static final JsonDataRegistry ENTITY_BINDINGS = new JsonDataRegistry("entity binding", "interactions/entity_bindings");
    public static final JsonDataRegistry NPC_ROUTINES = new JsonDataRegistry("npc routine", "npc_routines");
    public static final JsonDataRegistry NPC_PROFILES = new JsonDataRegistry("npc profile", "npc_profiles");
    public static final JsonDataRegistry NPC_KNOWLEDGE_PACKS = new JsonDataRegistry("npc knowledge pack", "npc_knowledge_packs");
    public static final JsonDataRegistry ATTRIBUTES = new JsonDataRegistry("attribute", "attributes");
    public static final JsonDataRegistry QUEST_BRANCHES = new JsonDataRegistry("quest branch", "quest_branches");
    public static final JsonDataRegistry FEATS = new JsonDataRegistry("feat", "feats");
    public static final JsonDataRegistry CHIMES = new JsonDataRegistry("chime", "chimes");
    public static final JsonDataRegistry JOURNAL_ENTRIES = new JsonDataRegistry("journal entry", "journal_entries");
    public static final JsonDataRegistry RELATIONSHIPS = new JsonDataRegistry("relationship", "relationships");
    public static final JsonDataRegistry CLUES = new JsonDataRegistry("clue", "clues");
    public static final JsonDataRegistry INVESTIGATION_SCENES = new JsonDataRegistry("investigation scene", "investigation_scenes");
    public static final JsonDataRegistry CONFLICTS = new JsonDataRegistry("conflict", "conflicts");

    private static final List<JsonDataRegistry> ALL = List.of(
            DIALOGUES,
            INTERACTION_SETTINGS,
            BLOCK_GROUPS,
            ENTITY_BINDINGS,
            NPC_ROUTINES,
            NPC_PROFILES,
            NPC_KNOWLEDGE_PACKS,
            ATTRIBUTES,
            QUEST_BRANCHES,
            FEATS,
            CHIMES,
            JOURNAL_ENTRIES,
            RELATIONSHIPS,
            CLUES,
            INVESTIGATION_SCENES,
            CONFLICTS
    );

    private NarrativeDataRegistries() {
    }

    public static void registerReloadListeners() {
        DIALOGUES.addReloadObserver(registry -> DialogueRegistry.rebuild(registry.entries()));
        INTERACTION_SETTINGS.addReloadObserver(registry -> InteractionSettings.rebuild(registry.entries()));
        BLOCK_GROUPS.addReloadObserver(registry -> BlockGroupIndex.rebuild(registry.entries()));
        ENTITY_BINDINGS.addReloadObserver(registry -> EntityBindingRegistry.rebuild(registry.entries()));
        NPC_ROUTINES.addReloadObserver(registry -> NpcRoutineRegistry.rebuild(registry.entries()));
        NPC_PROFILES.addReloadObserver(registry -> NpcProfileRegistry.rebuild(registry.entries()));
        NPC_KNOWLEDGE_PACKS.addReloadObserver(registry -> NpcKnowledgeRegistry.rebuild(registry.entries()));
        ATTRIBUTES.addReloadObserver(registry -> AttributeRegistry.rebuild(registry.entries()));
        QUEST_BRANCHES.addReloadObserver(registry -> QuestBranchRegistry.rebuild(registry.entries()));
        FEATS.addReloadObserver(registry -> FeatRegistry.rebuild(registry.entries()));
        CHIMES.addReloadObserver(registry -> ChimeRegistry.rebuild(registry.entries()));
        JOURNAL_ENTRIES.addReloadObserver(registry -> JournalEntryRegistry.rebuild(registry.entries()));
        RELATIONSHIPS.addReloadObserver(registry -> RelationshipRegistry.rebuild(registry.entries()));
        CLUES.addReloadObserver(registry -> InvestigationRegistry.rebuildClues(registry.entries()));
        INVESTIGATION_SCENES.addReloadObserver(registry -> InvestigationRegistry.rebuildScenes(registry.entries()));
        CONFLICTS.addReloadObserver(registry -> ConflictRegistry.rebuild(registry.entries()));

        ResourceLoader serverData = ResourceLoader.get(PackType.SERVER_DATA);
        for (JsonDataRegistry registry : ALL) {
            serverData.registerReloadListener(registry.reloadListenerId(), registry.createReloadListener());
        }
    }

    public static List<JsonDataRegistry> all() {
        return ALL;
    }

    public static int totalEntryCount() {
        return ALL.stream().mapToInt(JsonDataRegistry::size).sum();
    }

    public static int totalMessageCount() {
        return ALL.stream().mapToInt(registry -> registry.validationMessages().size()).sum();
    }

    public static String summaryLine() {
        String counts = ALL.stream()
                .map(registry -> registry.directory() + "=" + registry.size())
                .collect(Collectors.joining(", "));
        return "Ebb data registries: " + counts
                + "; validation_messages=" + totalMessageCount()
                + "; " + DialogueRegistry.summaryLine()
                + "; " + AttributeRegistry.summaryLine()
                + "; " + InteractionSettings.summaryLine()
                + "; " + BlockGroupIndex.summaryLine()
                + "; " + EntityBindingRegistry.summaryLine()
                + "; " + NpcRoutineRegistry.summaryLine()
                + "; " + NpcProfileRegistry.summaryLine()
                + "; " + NpcKnowledgeRegistry.summaryLine()
                + "; " + QuestBranchRegistry.summaryLine()
                + "; " + FeatRegistry.summaryLine()
                + "; " + ChimeRegistry.summaryLine()
                + "; " + JournalEntryRegistry.summaryLine()
                + "; " + RelationshipRegistry.summaryLine()
                + "; " + InvestigationRegistry.summaryLine()
                + "; " + ConflictRegistry.summaryLine();
    }
}
