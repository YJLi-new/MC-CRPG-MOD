package com.crpg.ebb.dialogue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DialogueDefinition(
        Identifier id,
        String start,
        Map<String, DialogueNode> nodes
) {
    public DialogueDefinition {
        nodes = Map.copyOf(nodes);
    }

    public static Optional<DialogueDefinition> parse(Identifier id, JsonObject json, List<String> messages) {
        String path = "dialogue " + id;
        optionalString(json, "id").ifPresent(declaredId -> {
            if (!declaredId.equals(id.toString())) {
                messages.add(path + ": top-level id \"" + declaredId + "\" does not match resource id " + id);
            }
        });

        Optional<String> start = requiredString(json, "start", path, messages);
        if (!json.has("nodes") || !json.get("nodes").isJsonObject()) {
            messages.add(path + ": missing required object \"nodes\"");
            return Optional.empty();
        }

        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        JsonObject nodeObject = json.getAsJsonObject("nodes");
        for (Map.Entry<String, JsonElement> entry : nodeObject.entrySet()) {
            String nodeId = entry.getKey();
            if (!entry.getValue().isJsonObject()) {
                messages.add(path + ".nodes." + nodeId + ": node must be an object");
                continue;
            }
            DialogueNode.parse(nodeId, entry.getValue().getAsJsonObject(), path + ".nodes." + nodeId, messages)
                    .ifPresent(node -> nodes.put(nodeId, node));
        }

        if (nodes.isEmpty()) {
            messages.add(path + ": nodes must contain at least one valid node");
        }
        if (start.isEmpty() || nodes.isEmpty()) {
            return Optional.empty();
        }
        if (!nodes.containsKey(start.get())) {
            messages.add(path + ": start node \"" + start.get() + "\" is not defined");
            return Optional.empty();
        }

        if (!validateReferences(id, nodes, messages)) {
            return Optional.empty();
        }
        return Optional.of(new DialogueDefinition(id, start.get(), nodes));
    }

    public Optional<DialogueNode> startNode() {
        return node(start);
    }

    public Optional<DialogueNode> node(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    private static boolean validateReferences(Identifier id, Map<String, DialogueNode> nodes, List<String> messages) {
        boolean valid = true;
        for (DialogueNode node : nodes.values()) {
            for (DialogueChoice choice : node.choices()) {
                valid &= validateRef(id, nodes, node.id(), choice.id(), "next", choice.next(), messages);
                if (choice.check().isPresent()) {
                    DialogueCheck check = choice.check().get();
                    valid &= validateRef(id, nodes, node.id(), choice.id(), "check.success", check.success(), messages);
                    valid &= validateRef(id, nodes, node.id(), choice.id(), "check.failure", check.failure(), messages);
                    valid &= validateRef(id, nodes, node.id(), choice.id(), "check.critical_success", check.criticalSuccess(), messages);
                    valid &= validateRef(id, nodes, node.id(), choice.id(), "check.critical_failure", check.criticalFailure(), messages);
                }
            }
        }
        return valid;
    }

    private static boolean validateRef(
            Identifier id,
            Map<String, DialogueNode> nodes,
            String nodeId,
            String choiceId,
            String field,
            Optional<String> ref,
            List<String> messages
    ) {
        if (ref.isEmpty()) {
            return true;
        }
        String next = ref.get();
        if (!nodes.containsKey(next)) {
            messages.add("dialogue " + id + ": node \"" + nodeId + "\" choice \"" + choiceId
                    + "\" has " + field + "=\"" + next + "\", but that node is missing");
            return false;
        }
        return true;
    }

    private static Optional<String> requiredString(JsonObject json, String key, String path, List<String> messages) {
        Optional<String> value = optionalString(json, key);
        if (value.isEmpty() || value.get().isBlank()) {
            messages.add(path + ": missing required string \"" + key + "\"");
            return Optional.empty();
        }
        return value;
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }
}
