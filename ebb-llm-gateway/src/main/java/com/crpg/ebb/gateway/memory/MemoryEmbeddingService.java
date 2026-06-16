package com.crpg.ebb.gateway.memory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class MemoryEmbeddingService {
    public static final int DIMENSIONS = 32;

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        String safe = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String token : safe.split("[^\\p{L}\\p{N}_:.-]+")) {
            if (token.isBlank()) {
                continue;
            }
            int hash = fnv1a(token);
            int index = Math.floorMod(hash, DIMENSIONS);
            vector[index] += 1.0D + (token.length() % 5) * 0.1D;
        }
        double norm = Math.sqrt(Arrays.stream(vector).map(value -> value * value).sum());
        if (norm > 0.000001D) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    public String serialize(double[] vector) {
        if (vector == null || vector.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(String.format(Locale.ROOT, "%.6f", vector[i]));
        }
        return out.toString();
    }

    public double[] parse(String serialized) {
        double[] vector = new double[DIMENSIONS];
        if (serialized == null || serialized.isBlank()) {
            return vector;
        }
        String[] parts = serialized.split(",");
        for (int i = 0; i < Math.min(parts.length, DIMENSIONS); i++) {
            try {
                vector[i] = Double.parseDouble(parts[i]);
            } catch (RuntimeException ignored) {
                vector[i] = 0.0D;
            }
        }
        return vector;
    }

    public double cosine(double[] left, double[] right) {
        if (left == null || right == null) {
            return 0.0D;
        }
        double dot = 0.0D;
        double leftNorm = 0.0D;
        double rightNorm = 0.0D;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm <= 0.000001D || rightNorm <= 0.000001D) {
            return 0.0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static int fnv1a(String token) {
        int hash = 0x811c9dc5;
        for (byte value : token.getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }
}
