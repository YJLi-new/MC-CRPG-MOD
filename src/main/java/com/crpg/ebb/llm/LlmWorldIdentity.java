package com.crpg.ebb.llm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/** Stable IDs sent to the LLM gateway so memories cannot bleed across saves/servers. */
public final class LlmWorldIdentity {
    private LlmWorldIdentity() {
    }

    public static String serverId(MinecraftServer server) {
        LlmConfig config = LlmConfig.current();
        if (!config.serverId().isBlank()) {
            return safe(config.serverId());
        }
        return "server-" + shortHash(rootPath(server) + "|ebb-server");
    }

    public static String worldId(ServerLevel level) {
        LlmConfig config = LlmConfig.current();
        if (!config.worldIdOverride().isBlank()) {
            return safe(config.worldIdOverride());
        }
        String strategy = config.worldIdStrategy().toLowerCase(Locale.ROOT);
        String dimension = level.dimension().toString();
        String root = rootPath(level.getServer());
        if ("configured".equals(strategy)) {
            return "world-" + shortHash(root + "|" + dimension);
        }
        if ("world_seed_hash".equals(strategy)) {
            return "world-seed-" + shortHash(level.getSeed() + "|" + dimension);
        }
        return "world-dir-" + shortHash(root + "|" + dimension);
    }

    private static String rootPath(MinecraftServer server) {
        try {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return root.toString();
        } catch (RuntimeException ex) {
            return "unknown-world-path";
        }
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception ex) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.strip().replaceAll("[^A-Za-z0-9_.:-]", "_");
    }
}
