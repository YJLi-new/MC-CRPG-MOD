package com.crpg.ebb.api;

import com.crpg.ebb.dialogue.DialogueDefinition;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface DialogueRepository {
    Optional<DialogueDefinition> find(Identifier id);

    ValidationReport validateAll();

    ReloadReport reload();
}
