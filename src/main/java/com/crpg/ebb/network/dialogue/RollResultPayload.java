package com.crpg.ebb.network.dialogue;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Locale;
import java.util.Optional;

public record RollResultPayload(
        String attribute,
        int dc,
        int dieRoll,
        int attributeScore,
        int total,
        boolean success,
        boolean critical,
        String outcome,
        boolean showDc,
        boolean showRoll,
        int baseAttribute,
        int staticModifier,
        int featModifier,
        int clueModifier,
        int firstRoll,
        Optional<Integer> secondRoll,
        String rollMode
) {
    public static final int MAX_ATTRIBUTE_LENGTH = 64;
    public static final int MAX_OUTCOME_LENGTH = 64;
    public static final int MAX_ROLL_MODE_LENGTH = 32;

    public RollResultPayload {
        secondRoll = secondRoll == null ? Optional.empty() : secondRoll;
        rollMode = rollMode == null || rollMode.isBlank() ? "normal" : rollMode.toLowerCase(Locale.ROOT);
    }

    public RollResultPayload(
            String attribute,
            int dc,
            int dieRoll,
            int attributeScore,
            int total,
            boolean success,
            boolean critical,
            String outcome,
            boolean showDc,
            boolean showRoll
    ) {
        this(
                attribute,
                dc,
                dieRoll,
                attributeScore,
                total,
                success,
                critical,
                outcome,
                showDc,
                showRoll,
                attributeScore,
                0,
                0,
                0,
                dieRoll,
                Optional.empty(),
                "normal"
        );
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(attribute, MAX_ATTRIBUTE_LENGTH);
        buffer.writeVarInt(dc);
        buffer.writeVarInt(dieRoll);
        buffer.writeVarInt(attributeScore);
        buffer.writeVarInt(total);
        buffer.writeBoolean(success);
        buffer.writeBoolean(critical);
        buffer.writeUtf(outcome, MAX_OUTCOME_LENGTH);
        buffer.writeBoolean(showDc);
        buffer.writeBoolean(showRoll);
        buffer.writeVarInt(baseAttribute);
        buffer.writeVarInt(staticModifier);
        buffer.writeVarInt(featModifier);
        buffer.writeVarInt(clueModifier);
        buffer.writeVarInt(firstRoll);
        buffer.writeBoolean(secondRoll.isPresent());
        secondRoll.ifPresent(buffer::writeVarInt);
        buffer.writeUtf(rollMode, MAX_ROLL_MODE_LENGTH);
    }

    public static RollResultPayload read(RegistryFriendlyByteBuf buffer) {
        String attribute = buffer.readUtf(MAX_ATTRIBUTE_LENGTH);
        int dc = buffer.readVarInt();
        int dieRoll = buffer.readVarInt();
        int attributeScore = buffer.readVarInt();
        int total = buffer.readVarInt();
        boolean success = buffer.readBoolean();
        boolean critical = buffer.readBoolean();
        String outcome = buffer.readUtf(MAX_OUTCOME_LENGTH);
        boolean showDc = buffer.readBoolean();
        boolean showRoll = buffer.readBoolean();
        int baseAttribute = buffer.readVarInt();
        int staticModifier = buffer.readVarInt();
        int featModifier = buffer.readVarInt();
        int clueModifier = buffer.readVarInt();
        int firstRoll = buffer.readVarInt();
        Optional<Integer> secondRoll = buffer.readBoolean() ? Optional.of(buffer.readVarInt()) : Optional.empty();
        String rollMode = buffer.readUtf(MAX_ROLL_MODE_LENGTH);
        return new RollResultPayload(
                attribute,
                dc,
                dieRoll,
                attributeScore,
                total,
                success,
                critical,
                outcome,
                showDc,
                showRoll,
                baseAttribute,
                staticModifier,
                featModifier,
                clueModifier,
                firstRoll,
                secondRoll,
                rollMode
        );
    }

    public String summary() {
        String roll = showRoll ? rollBreakdown() : "hidden roll";
        String difficulty = showDc ? "DC " + dc : "hidden DC";
        return attribute + " " + roll + " vs " + difficulty + " (" + outcome + ")";
    }

    private String rollBreakdown() {
        String dice = secondRoll
                .map(second -> "d20=" + firstRoll + "/" + second + " " + rollModeLabel() + "=>" + dieRoll)
                .orElse("d20=" + dieRoll);
        return dice + " + " + modifierBreakdown() + " => " + total;
    }

    private String rollModeLabel() {
        return switch (rollMode) {
            case "advantage" -> "adv ";
            case "disadvantage" -> "dis ";
            case "normal_cancelled" -> "cancel ";
            default -> "";
        };
    }

    private String modifierBreakdown() {
        if (staticModifier == 0 && featModifier == 0 && clueModifier == 0) {
            return Integer.toString(attributeScore);
        }
        return baseAttribute
                + signed(staticModifier, "static")
                + signed(featModifier, "feat")
                + signed(clueModifier, "clue")
                + "=" + attributeScore;
    }

    private static String signed(int value, String label) {
        if (value == 0) {
            return "";
        }
        return (value > 0 ? "+" : "") + value + " " + label;
    }
}
