package com.crpg.ebb.network.dialogue;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class DialoguePayloadCodecs {
    public static final int MAX_CHOICES = 16;
    public static final int MAX_NODE_ID_LENGTH = 64;
    public static final int MAX_SPEAKER_LENGTH = 96;
    public static final int MAX_TEXT_LENGTH = 2048;
    public static final int MAX_REASON_LENGTH = 96;

    private DialoguePayloadCodecs() {
    }

    public static void writeChoices(RegistryFriendlyByteBuf buffer, List<VisibleDialogueChoice> choices) {
        int count = Math.min(choices.size(), MAX_CHOICES);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            choices.get(i).write(buffer);
        }
    }

    public static List<VisibleDialogueChoice> readChoices(RegistryFriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(), MAX_CHOICES);
        List<VisibleDialogueChoice> choices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            choices.add(VisibleDialogueChoice.read(buffer));
        }
        return List.copyOf(choices);
    }
}
