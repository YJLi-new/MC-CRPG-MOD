package com.crpg.ebb.gateway;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpJson {
    private static final Pattern STRING_FIELD = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");
    private static final Pattern NUMBER_FIELD = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(-?\\d+)");
    private static final Pattern BOOLEAN_FIELD = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(true|false)");

    private HttpJson() {
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static Map<String, String> objectStrings(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null) {
            return values;
        }
        Matcher matcher = STRING_FIELD.matcher(json);
        while (matcher.find()) {
            values.put(matcher.group(1), unescape(matcher.group(2)));
        }
        Matcher numbers = NUMBER_FIELD.matcher(json);
        while (numbers.find()) {
            values.putIfAbsent(numbers.group(1), numbers.group(2));
        }
        Matcher booleans = BOOLEAN_FIELD.matcher(json);
        while (booleans.find()) {
            values.putIfAbsent(booleans.group(1), booleans.group(2));
        }
        return values;
    }

    public static Optional<String> stringValue(String json, String key) {
        return Optional.ofNullable(objectStrings(json).get(key));
    }


    public static List<String> stringArrayValue(String json, String key) {
        if (json == null || key == null || key.isBlank()) {
            return List.of();
        }
        Pattern arrayPattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
        Matcher array = arrayPattern.matcher(json);
        if (!array.find()) {
            return List.of();
        }
        List<String> values = new java.util.ArrayList<>();
        Matcher strings = Pattern.compile("\\\"((?:\\\\\\.|[^\\\\\\\\\"])*)\\\"").matcher(array.group(1));
        while (strings.find()) {
            values.add(unescape(strings.group(1)));
        }
        return List.copyOf(values);
    }

    public static boolean booleanValue(String json, String key, boolean fallback) {
        return stringValue(json, key).map(value -> {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            return fallback;
        }).orElse(fallback);
    }

    public static long longValue(String json, String key, long fallback) {
        return stringValue(json, key).map(value -> {
            try {
                return Long.parseLong(value);
            } catch (RuntimeException ex) {
                return fallback;
            }
        }).orElse(fallback);
    }

    public static Map<String, String> query(URI uri) {
        Map<String, String> values = new LinkedHashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                values.put(decode(pair), "");
            } else {
                values.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return values;
    }

    public static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        writeJson(exchange, 405, object(Map.of("error", "method_not_allowed")));
    }

    public static String object(Map<String, ?> values) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(escape(entry.getKey())).append("\":");
            appendValue(out, entry.getValue());
        }
        return out.append('}').toString();
    }

    public static String array(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append('"').append(escape(values.get(i))).append('"');
        }
        return out.append(']').toString();
    }

    private static void appendValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append('"').append(escape(String.valueOf(entry.getKey()))).append("\":");
                appendValue(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                appendValue(out, list.get(i));
            }
            out.append(']');
        } else {
            out.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
