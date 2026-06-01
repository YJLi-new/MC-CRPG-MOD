package com.crpg.ebb.api;

import java.util.Optional;

public record DialogueStepResult(boolean advanced, Optional<String> status) {
    public DialogueStepResult {
        status = status == null ? Optional.empty() : status;
    }
}
