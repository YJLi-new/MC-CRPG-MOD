package com.crpg.ebb.llm;

import java.util.List;
import java.util.Optional;

public record LlmChatResponse(
        String reply,
        List<String> suggestedOptions,
        List<String> citationIds,
        String status,
        Optional<String> errorReason
) {
    public LlmChatResponse {
        reply = reply == null ? "" : reply;
        suggestedOptions = suggestedOptions == null ? List.of() : List.copyOf(suggestedOptions);
        citationIds = citationIds == null ? List.of() : List.copyOf(citationIds);
        status = status == null ? "" : status;
        errorReason = errorReason == null ? Optional.empty() : errorReason;
    }

    public static LlmChatResponse ok(String reply, List<String> suggestedOptions, List<String> citationIds, String status) {
        return new LlmChatResponse(reply, suggestedOptions, citationIds, status, Optional.empty());
    }

    public static LlmChatResponse error(String reason) {
        return new LlmChatResponse("", List.of(), List.of(), reason, Optional.of(reason));
    }
}
