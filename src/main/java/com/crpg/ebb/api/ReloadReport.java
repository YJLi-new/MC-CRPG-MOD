package com.crpg.ebb.api;

import java.util.List;

public record ReloadReport(boolean success, List<String> messages) {
    public ReloadReport {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
