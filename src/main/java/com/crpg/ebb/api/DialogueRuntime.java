package com.crpg.ebb.api;

import com.crpg.ebb.dialogue.DialogueSession;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface DialogueRuntime {
    DialogueSession start(ServerPlayer player, Identifier dialogueId, TargetRef target);

    DialogueStepResult choose(ServerPlayer player, UUID sessionId, String optionId);
}
