package com.crpg.ebb.api;

import java.util.List;

public record ValidationReport(List<String> messages) {
    public ValidationReport {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public boolean ok() {
        return messages.isEmpty();
    }
}
