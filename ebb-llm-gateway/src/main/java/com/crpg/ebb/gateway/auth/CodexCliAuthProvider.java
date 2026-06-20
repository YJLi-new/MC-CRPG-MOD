package com.crpg.ebb.gateway.auth;

import com.crpg.ebb.gateway.HttpJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI Codex/ChatGPT OAuth device-code auth adapter.
 *
 * <p>Important: this provider intentionally delegates the real ChatGPT OAuth
 * device-code flow to the official Codex CLI (`codex login --device-auth`). It
 * does not call undocumented token endpoints and it never returns Codex tokens
 * to Minecraft. Codex's auth cache stays under the configured gateway-private
 * CODEX_HOME directory; Minecraft receives only the opaque Ebb player token
 * minted by {@link DeviceAuthService} after the Codex CLI completes login.</p>
 */
public final class CodexCliAuthProvider implements AuthProvider {
    public static final String DEFAULT_PROVIDER_NAME = "openai_codex";
    public static final String DEFAULT_COMMAND = "codex";
    public static final String CODEX_LOGIN_DEVICE_AUTH = "login --device-auth";
    public static final String CODEX_LOGIN_STATUS = "codex login status";
    public static final String CODEX_HOME_ENV = "CODEX_HOME";

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\u001B\\[[;?0-9]*[ -/]*[@-~]");
    private static final Pattern URL_PATTERN = Pattern.compile("https://[^\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_PATTERN = Pattern.compile("\\b[A-Z0-9]{4,}(?:-[A-Z0-9]{4,})+\\b");
    private static final Pattern EXPIRES_MINUTES_PATTERN = Pattern.compile("expires in (\\d+) minutes", Pattern.CASE_INSENSITIVE);
    private static final long DEFAULT_EXPIRES_SECONDS = 15L * 60L;
    private static final long DEFAULT_INTERVAL_SECONDS = 5L;

    private final String command;
    private final Path codexHomeRoot;
    private final Duration startOutputTimeout;
    private final Duration processGraceAfterExpiry;
    private final Map<String, RunningLogin> running = new ConcurrentHashMap<>();

    public CodexCliAuthProvider(String command, Path codexHomeRoot, Duration startOutputTimeout) {
        this.command = command == null || command.isBlank() ? DEFAULT_COMMAND : command.trim();
        this.codexHomeRoot = codexHomeRoot == null ? Path.of("./ebb-llm-gateway-data/codex-auth") : codexHomeRoot;
        this.startOutputTimeout = startOutputTimeout == null ? Duration.ofSeconds(45) : startOutputTimeout;
        this.processGraceAfterExpiry = Duration.ofSeconds(30);
    }

    @Override
    public ProviderStart start(String minecraftUuid, String serverId, String authSessionId, String userCode) {
        String sessionId = "codex-cli:" + authSessionId;
        try {
            Path playerHome = playerCodexHome(minecraftUuid, serverId);
            Files.createDirectories(playerHome);
            StringBuilder output = new StringBuilder();
            ProcessBuilder builder = new ProcessBuilder(command, "login", "--device-auth");
            builder.redirectErrorStream(true);
            builder.environment().put(CODEX_HOME_ENV, playerHome.toAbsolutePath().normalize().toString());
            builder.environment().put("NO_COLOR", "1");
            builder.environment().put("TERM", "dumb");
            Process process = builder.start();
            RunningLogin login = new RunningLogin(sessionId, process, playerHome, output, Instant.now());
            running.put(sessionId, login);
            startReader(login);

            Optional<DeviceCodeInfo> info = waitForDeviceCode(output, startOutputTimeout);
            if (info.isEmpty()) {
                destroy(login, true);
                running.remove(sessionId);
                return errorStart(userCode, "codex_device_code_not_emitted");
            }

            long expires = Math.max(60L, info.get().expiresInSeconds());
            scheduleExpiryCleanup(sessionId, process, Duration.ofSeconds(expires).plus(processGraceAfterExpiry));
            return new ProviderStart(
                    info.get().verificationUrl(),
                    info.get().userCode(),
                    sessionId,
                    expires,
                    DEFAULT_INTERVAL_SECONDS,
                    Map.of(
                            "auth_flow", "codex_cli_device_auth",
                            "codex_home", playerHome.toAbsolutePath().normalize().toString(),
                            "codex_command", command,
                            "codex_cli_invocation", CODEX_LOGIN_DEVICE_AUTH
                    )
            );
        } catch (IOException | RuntimeException ex) {
            running.remove(sessionId);
            return errorStart(userCode, "codex_cli_start_failed");
        }
    }

    @Override
    public ProviderStatus poll(ProviderSession session) {
        RunningLogin login = running.get(session.providerSessionId());
        if (login == null) {
            return ProviderStatus.error("codex_auth_session_not_running");
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            destroy(login, true);
            running.remove(session.providerSessionId());
            return ProviderStatus.error("codex_device_code_expired");
        }
        if (login.process().isAlive()) {
            return ProviderStatus.pending();
        }
        int exit = login.process().exitValue();
        running.remove(session.providerSessionId());
        if (exit == 0 && codexStatusLoggedIn(login.codexHome())) {
            return ProviderStatus.authenticated(
                    "openai-codex:" + safeSubject(session.minecraftUuid()),
                    List.of("llm:chat", "memory:read_self", "memory:write_self"),
                    8L * 24L * 60L * 60L
            );
        }
        return ProviderStatus.error("codex_cli_login_exit_" + exit);
    }

    @Override
    public String providerName() {
        return DEFAULT_PROVIDER_NAME;
    }

    public static Optional<DeviceCodeInfo> extractDeviceCodeInfo(String rawOutput) {
        String text = stripAnsi(rawOutput == null ? "" : rawOutput);
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        String url = "";
        while (urlMatcher.find()) {
            String candidate = trimUrl(urlMatcher.group());
            if (candidate.contains("auth.openai.com") || candidate.contains("chatgpt.com") || url.isBlank()) {
                url = candidate;
            }
        }
        Matcher codeMatcher = CODE_PATTERN.matcher(text.toUpperCase(Locale.ROOT));
        String code = "";
        while (codeMatcher.find()) {
            code = codeMatcher.group();
        }
        if (url.isBlank() || code.isBlank()) {
            return Optional.empty();
        }
        long expires = DEFAULT_EXPIRES_SECONDS;
        Matcher expiresMatcher = EXPIRES_MINUTES_PATTERN.matcher(text);
        if (expiresMatcher.find()) {
            try {
                expires = Math.max(60L, Long.parseLong(expiresMatcher.group(1)) * 60L);
            } catch (RuntimeException ignored) {
                expires = DEFAULT_EXPIRES_SECONDS;
            }
        }
        return Optional.of(new DeviceCodeInfo(url, code, expires));
    }

    private Optional<DeviceCodeInfo> waitForDeviceCode(StringBuilder output, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        Optional<DeviceCodeInfo> info;
        while (System.nanoTime() < deadline) {
            synchronized (output) {
                info = extractDeviceCodeInfo(output.toString());
            }
            if (info.isPresent()) {
                return info;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        synchronized (output) {
            return extractDeviceCodeInfo(output.toString());
        }
    }

    private void startReader(RunningLogin login) {
        Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(login.process().getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (login.output()) {
                        if (login.output().length() < 16_384) {
                            login.output().append(line).append('\n');
                        }
                    }
                }
            } catch (IOException ignored) {
                // Output is best-effort diagnostics only; tokens are never parsed from here.
            }
        });
    }

    private void scheduleExpiryCleanup(String sessionId, Process process, Duration delay) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(Math.max(1000L, delay.toMillis()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            RunningLogin login = running.remove(sessionId);
            if (login != null && process.isAlive()) {
                destroy(login, true);
            }
        });
    }

    private boolean codexStatusLoggedIn(Path codexHome) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command, "login", "status");
            builder.redirectErrorStream(true);
            builder.environment().put(CODEX_HOME_ENV, codexHome.toAbsolutePath().normalize().toString());
            builder.environment().put("NO_COLOR", "1");
            Process process = builder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null && lines.size() < 20) {
                    lines.add(line);
                }
                output = String.join("\n", lines);
            }
            boolean exited = process.waitFor(Math.max(1L, startOutputTimeout.toSeconds()), java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0 && stripAnsi(output).toLowerCase(Locale.ROOT).contains("logged in");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private Path playerCodexHome(String minecraftUuid, String serverId) {
        String server = safePathSegment(serverId == null || serverId.isBlank() ? "default" : serverId);
        String player = safePathSegment(minecraftUuid == null || minecraftUuid.isBlank() ? UUID.randomUUID().toString() : minecraftUuid);
        return codexHomeRoot.resolve(server).resolve(player);
    }

    private static ProviderStart errorStart(String userCode, String error) {
        return new ProviderStart("", userCode == null ? "" : userCode, "", 0L, DEFAULT_INTERVAL_SECONDS, Map.of("error", error));
    }

    private static void destroy(RunningLogin login, boolean force) {
        if (login.process().isAlive()) {
            login.process().destroy();
            if (force && login.process().isAlive()) {
                login.process().destroyForcibly();
            }
        }
    }

    private static String stripAnsi(String value) {
        return ANSI_PATTERN.matcher(value == null ? "" : value).replaceAll("");
    }

    private static String trimUrl(String value) {
        return value == null ? "" : value.replaceAll("[\\])}>,.;]+$", "");
    }

    private static String safeSubject(String value) {
        return value == null || value.isBlank() ? "unknown-player" : value;
    }

    private static String safePathSegment(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9_.-]", "_");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    public record DeviceCodeInfo(String verificationUrl, String userCode, long expiresInSeconds) {
        public String safeJson() {
            return HttpJson.object(Map.of(
                    "verification_url", verificationUrl,
                    "user_code", userCode,
                    "expires_in_seconds", expiresInSeconds
            ));
        }
    }

    private record RunningLogin(String providerSessionId, Process process, Path codexHome, StringBuilder output, Instant startedAt) {
    }
}
