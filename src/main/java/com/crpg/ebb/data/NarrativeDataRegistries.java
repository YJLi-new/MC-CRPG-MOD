package com.crpg.ebb.data;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.routine.NpcRoutineRegistry;
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
    public static final JsonDataRegistry ATTRIBUTES = new JsonDataRegistry("attribute", "attributes");

    private static final List<JsonDataRegistry> ALL = List.of(
            DIALOGUES,
            INTERACTION_SETTINGS,
            BLOCK_GROUPS,
            ENTITY_BINDINGS,
            NPC_ROUTINES,
            ATTRIBUTES
    );

    private NarrativeDataRegistries() {
    }

    public static void registerReloadListeners() {
        DIALOGUES.addReloadObserver(registry -> DialogueRegistry.rebuild(registry.entries()));
        INTERACTION_SETTINGS.addReloadObserver(registry -> InteractionSettings.rebuild(registry.entries()));
        BLOCK_GROUPS.addReloadObserver(registry -> BlockGroupIndex.rebuild(registry.entries()));
        ENTITY_BINDINGS.addReloadObserver(registry -> EntityBindingRegistry.rebuild(registry.entries()));
        NPC_ROUTINES.addReloadObserver(registry -> NpcRoutineRegistry.rebuild(registry.entries()));
        ATTRIBUTES.addReloadObserver(registry -> AttributeRegistry.rebuild(registry.entries()));

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
                + "; " + NpcRoutineRegistry.summaryLine();
    }
}
