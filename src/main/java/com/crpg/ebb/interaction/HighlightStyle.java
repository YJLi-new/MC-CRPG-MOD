package com.crpg.ebb.interaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Data-driven target highlight styling shared by block groups and entity
 * bindings. The server owns parsing/sync; the client only renders the synced
 * style and still relies on server validation for real interaction success.
 */
public record HighlightStyle(
        int closeColor,
        int farColor,
        RenderMode renderMode,
        int priority
) {
    public static final int DEFAULT_CLOSE_COLOR = 0xFF64E6FF;
    public static final int DEFAULT_FAR_COLOR = 0xAA64E6FF;

    public HighlightStyle {
        renderMode = renderMode == null ? RenderMode.MERGED : renderMode;
    }

    public static HighlightStyle blockDefault() {
        return new HighlightStyle(DEFAULT_CLOSE_COLOR, DEFAULT_FAR_COLOR, RenderMode.MERGED, 0);
    }

    public static HighlightStyle entityDefault() {
        return new HighlightStyle(DEFAULT_CLOSE_COLOR, DEFAULT_FAR_COLOR, RenderMode.OUTLINE, 0);
    }

    public static Optional<HighlightStyle> parseOptional(JsonObject json, HighlightStyle fallback, List<String> messages, String owner) {
        if (!json.has("highlight") || !json.get("highlight").isJsonObject()) {
            return Optional.of(fallback);
        }
        try {
            return Optional.of(parse(json.getAsJsonObject("highlight"), fallback));
        } catch (RuntimeException ex) {
            messages.add(owner + ": invalid highlight style: " + ex.getMessage());
            return Optional.of(fallback);
        }
    }

    public static HighlightStyle parse(JsonObject json, HighlightStyle fallback) {
        int close = fallback.closeColor;
        int far = fallback.farColor;

        if (json.has("color")) {
            int base = parseColor(GsonHelper.getAsString(json, "color"), 0xFF);
            close = withAlpha(base, alpha(close));
            far = withAlpha(base, alpha(far));
        }
        if (json.has("opacity")) {
            close = withAlpha(close, parseOpacity(GsonHelper.getAsDouble(json, "opacity")));
        }
        if (json.has("far_opacity")) {
            far = withAlpha(far, parseOpacity(GsonHelper.getAsDouble(json, "far_opacity")));
        }
        if (json.has("close_color")) {
            close = parseColor(GsonHelper.getAsString(json, "close_color"), alpha(close));
        }
        if (json.has("far_color")) {
            far = parseColor(GsonHelper.getAsString(json, "far_color"), alpha(far));
        }

        RenderMode renderMode = json.has("render_mode")
                ? RenderMode.parse(GsonHelper.getAsString(json, "render_mode"))
                : fallback.renderMode;
        int priority = json.has("priority") ? GsonHelper.getAsInt(json, "priority") : fallback.priority;
        return new HighlightStyle(close, far, renderMode, priority);
    }

    public String debugSummary() {
        return "highlight(close=#" + hex(closeColor)
                + ", far=#" + hex(farColor)
                + ", render_mode=" + renderMode.serializedName()
                + ", priority=" + priority
                + ")";
    }

    private static int parseColor(String raw, int defaultAlpha) {
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.length() == 6) {
            return (defaultAlpha << 24) | Integer.parseUnsignedInt(value, 16);
        }
        if (value.length() == 8) {
            return (int) Long.parseLong(value, 16);
        }
        throw new JsonParseException("color must be #RRGGBB or #AARRGGBB, got " + raw);
    }

    private static int parseOpacity(double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new JsonParseException("opacity must be between 0 and 1");
        }
        return Math.max(0, Math.min(255, (int) Math.round(value * 255.0D)));
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static String hex(int color) {
        return String.format("%08X", color);
    }

    public enum RenderMode {
        OUTLINE("outline"),
        MERGED("merged"),
        BOUNDS("bounds");

        private final String serializedName;

        RenderMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static RenderMode parse(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (RenderMode mode : values()) {
                if (mode.serializedName.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return mode;
                }
            }
            throw new JsonParseException("unknown render_mode " + value + "; expected outline, merged, or bounds");
        }
    }
}
