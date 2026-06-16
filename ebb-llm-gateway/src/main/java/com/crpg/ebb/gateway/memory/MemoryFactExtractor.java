package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.chat.GatewayChatRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MemoryFactExtractor {
    private static final Pattern EXPLICIT = Pattern.compile("(?i)(?:^|\\s)(?:fact|remember)\\s*[:：]\\s*([\\p{L}\\p{N}_:.-]+)\\s*=\\s*([^;。\\n]+)");
    private static final Pattern FIRST_PERSON = Pattern.compile("(?i)\\b(?:i am|i'm|我是)\\s+([^;。.!?\\n]+)");

    public List<ExtractedFact> extract(GatewayChatRequest request) {
        String text = request == null ? "" : request.message();
        String defaultSubject = request == null ? "player:unknown" : "player:" + request.minecraftPlayerUuid();
        List<ExtractedFact> facts = new ArrayList<>();
        Matcher explicit = EXPLICIT.matcher(text == null ? "" : text);
        while (explicit.find()) {
            String key = explicit.group(1).strip();
            String value = explicit.group(2).strip();
            if (value.isBlank()) {
                continue;
            }
            String subject = defaultSubject;
            String predicate = key;
            int dot = key.indexOf('.');
            if (dot > 0 && dot < key.length() - 1) {
                subject = key.substring(0, dot).strip();
                predicate = key.substring(dot + 1).strip();
            }
            facts.add(new ExtractedFact(subject, predicate.toLowerCase(Locale.ROOT), value));
        }
        Matcher first = FIRST_PERSON.matcher(text == null ? "" : text);
        while (first.find()) {
            String value = first.group(1).strip();
            if (!value.isBlank()) {
                facts.add(new ExtractedFact(defaultSubject, "self_description", value));
            }
        }
        return List.copyOf(facts);
    }

    public record ExtractedFact(String subject, String predicate, String value) {
        public ExtractedFact {
            subject = subject == null || subject.isBlank() ? "unknown" : subject.strip();
            predicate = predicate == null || predicate.isBlank() ? "unknown" : predicate.strip().toLowerCase(Locale.ROOT);
            value = value == null ? "" : value.strip();
        }
    }
}
