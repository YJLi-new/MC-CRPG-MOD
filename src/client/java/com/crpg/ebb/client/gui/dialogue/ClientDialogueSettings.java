package com.crpg.ebb.client.gui.dialogue;

import com.crpg.ebb.EbbMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ClientDialogueSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ebb-client.json");
    private static final double MIN_FONT_SCALE = 0.85D;
    private static final double MAX_FONT_SCALE = 1.25D;
    private static final double FONT_SCALE_STEP = 0.05D;

    private static double fontScale = 1.0D;
    private static TextSpeed textSpeed = TextSpeed.NORMAL;

    private ClientDialogueSettings() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("dialogue_font_scale") && json.get("dialogue_font_scale").isJsonPrimitive()) {
                fontScale = clamp(json.get("dialogue_font_scale").getAsDouble(), MIN_FONT_SCALE, MAX_FONT_SCALE);
            }
            if (json.has("dialogue_text_speed") && json.get("dialogue_text_speed").isJsonPrimitive()) {
                textSpeed = TextSpeed.parse(json.get("dialogue_text_speed").getAsString());
            }
        } catch (Exception ex) {
            EbbMod.LOGGER.warn("Could not read Ebb client settings from {}: {}", CONFIG_PATH, ex.getMessage());
            fontScale = 1.0D;
            textSpeed = TextSpeed.NORMAL;
            save();
        }
    }

    public static double fontScale() {
        return fontScale;
    }

    public static TextSpeed textSpeed() {
        return textSpeed;
    }

    public static void increaseFontScale() {
        fontScale = clamp(roundStep(fontScale + FONT_SCALE_STEP), MIN_FONT_SCALE, MAX_FONT_SCALE);
        save();
    }

    public static void decreaseFontScale() {
        fontScale = clamp(roundStep(fontScale - FONT_SCALE_STEP), MIN_FONT_SCALE, MAX_FONT_SCALE);
        save();
    }

    public static void cycleTextSpeed() {
        textSpeed = textSpeed.next();
        save();
    }

    public static Component fontScaleLabel() {
        return Component.translatable("screen.ebb.dialogue.font_scale", String.format(Locale.ROOT, "%.2f", fontScale));
    }

    public static Component textSpeedLabel() {
        return Component.translatable("screen.ebb.dialogue.text_speed", textSpeed.label());
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("dialogue_font_scale", fontScale);
        json.addProperty("dialogue_text_speed", textSpeed.serializedName());
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            EbbMod.LOGGER.warn("Could not write Ebb client settings to {}: {}", CONFIG_PATH, ex.getMessage());
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double roundStep(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    public enum TextSpeed {
        SLOW("slow", 18),
        NORMAL("normal", 32),
        FAST("fast", 72),
        INSTANT("instant", Integer.MAX_VALUE);

        private final String serializedName;
        private final int charsPerSecond;

        TextSpeed(String serializedName, int charsPerSecond) {
            this.serializedName = serializedName;
            this.charsPerSecond = charsPerSecond;
        }

        public String serializedName() {
            return serializedName;
        }

        public int charsPerSecond() {
            return charsPerSecond;
        }

        public Component label() {
            return Component.translatable("screen.ebb.dialogue.text_speed." + serializedName);
        }

        public TextSpeed next() {
            TextSpeed[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static TextSpeed parse(String raw) {
            for (TextSpeed value : values()) {
                if (value.serializedName.equalsIgnoreCase(raw)) {
                    return value;
                }
            }
            return NORMAL;
        }
    }
}
