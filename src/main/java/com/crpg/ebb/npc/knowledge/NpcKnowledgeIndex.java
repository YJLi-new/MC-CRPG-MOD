package com.crpg.ebb.npc.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class NpcKnowledgeIndex {
    private static final int DIMENSIONS = 24;

    public double score(String query, String text) {
        return cosine(embed(query), embed(text));
    }

    private double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        String safe = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String token : safe.split("[^\\p{L}\\p{N}_:.-]+")) {
            if (token.isBlank()) continue;
            int hash = fnv1a(token);
            vector[Math.floorMod(hash, DIMENSIONS)] += 1.0D + (token.length() % 7) * 0.05D;
        }
        double norm = Math.sqrt(Arrays.stream(vector).map(value -> value * value).sum());
        if (norm > 0.000001D) {
            for (int i = 0; i < vector.length; i++) vector[i] /= norm;
        }
        return vector;
    }

    private static int fnv1a(String token) {
        int hash = 0x811c9dc5;
        for (byte value : token.getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xff;
            hash *= 0x01000193;
        }
        return hash;
    }

    private static double cosine(double[] left, double[] right) {
        double dot = 0.0D;
        double leftNorm = 0.0D;
        double rightNorm = 0.0D;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm <= 0.000001D || rightNorm <= 0.000001D) return 0.0D;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
