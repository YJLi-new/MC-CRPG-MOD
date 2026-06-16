package com.crpg.ebb.npc.profile;

import java.util.Locale;

public enum NpcTier {
    MAJOR_SCRIPTED("major_scripted"),
    MINOR_GENERATABLE("minor_generatable"),
    MAJOR_PROMOTED("major_promoted"),
    STATIC_NON_LLM("static_non_llm"),
    DISABLED("disabled");

    private final String serializedName;

    NpcTier(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean isMajor() {
        return this == MAJOR_SCRIPTED || this == MAJOR_PROMOTED;
    }

    public boolean canUseLlm() {
        return this == MAJOR_SCRIPTED || this == MAJOR_PROMOTED || this == MINOR_GENERATABLE;
    }

    public static NpcTier parse(String value) {
        if (value == null || value.isBlank()) {
            return STATIC_NON_LLM;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "major", "scripted", "major_scripted" -> MAJOR_SCRIPTED;
            case "minor", "generatable", "minor_generatable", "minor_generate" -> MINOR_GENERATABLE;
            case "promoted", "major_promoted", "promoted_major" -> MAJOR_PROMOTED;
            case "static", "static_non_llm", "non_llm", "scripted_non_llm" -> STATIC_NON_LLM;
            case "disabled", "off" -> DISABLED;
            default -> throw new IllegalArgumentException("unknown npc tier: " + value);
        };
    }
}
