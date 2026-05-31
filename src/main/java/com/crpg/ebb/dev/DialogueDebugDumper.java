package com.crpg.ebb.dev;

import com.crpg.ebb.dialogue.DialogueChoice;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.routine.NpcRoutineDefinition;
import com.crpg.ebb.routine.NpcRoutineRegistry;

import java.util.Comparator;
import java.util.List;

public final class DialogueDebugDumper {
    private DialogueDebugDumper() {
    }

    public static void appendDialogueTrees(List<String> lines) {
        lines.add("");
        lines.add("Dialogue trees (" + DialogueRegistry.size() + "):");
        if (DialogueRegistry.definitions().isEmpty()) {
            lines.add("- none");
            return;
        }
        DialogueRegistry.definitions().values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(definition -> appendDialogue(lines, definition));
    }

    public static void appendEntityBindings(List<String> lines) {
        lines.add("");
        lines.add("Entity bindings (" + EntityBindingRegistry.size() + "):");
        lines.add("- debug fallback enabled: " + InteractionSettings.enableDebugEntityFallback()
                + " -> " + InteractionSettings.snapshot().debugEntityFallbackDialogue());
        if (EntityBindingRegistry.definitions().isEmpty()) {
            lines.add("- none");
            return;
        }
        EntityBindingRegistry.definitions().values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(definition -> lines.add("- " + definition.debugSummary()));
    }

    public static void appendRoutines(List<String> lines) {
        lines.add("");
        lines.add("NPC routines (" + NpcRoutineRegistry.size() + "):");
        if (NpcRoutineRegistry.definitions().isEmpty()) {
            lines.add("- none");
            return;
        }
        NpcRoutineRegistry.definitions().values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .forEach(definition -> appendRoutine(lines, definition));
    }

    private static void appendDialogue(List<String> lines, DialogueDefinition definition) {
        lines.add("Dialogue " + definition.id());
        lines.add("  start: " + definition.start());
        definition.nodes().values().stream()
                .sorted(Comparator.comparing(DialogueNode::id))
                .forEach(node -> {
                    lines.add("  " + node.debugSummary());
                    if (node.choices().isEmpty()) {
                        lines.add("    choices: none (terminal node)");
                    }
                    for (DialogueChoice choice : node.choices()) {
                        lines.add("    " + choice.debugSummary());
                    }
                });
    }

    private static void appendRoutine(List<String> lines, NpcRoutineDefinition definition) {
        lines.add("Routine " + definition.id());
        lines.add("  " + definition.lookAtPlayer().debugSummary());
        if (definition.steps().isEmpty()) {
            lines.add("  steps: none");
        }
        for (int i = 0; i < definition.steps().size(); i++) {
            lines.add("  step " + i + ": " + definition.steps().get(i).debugSummary());
        }
    }
}
