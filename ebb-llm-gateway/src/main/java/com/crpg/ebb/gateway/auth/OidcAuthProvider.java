package com.crpg.ebb.gateway.auth;

import com.crpg.ebb.gateway.HttpJson;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OidcAuthProvider implements AuthProvider {
    // Generic OIDC device-flow adapter for production providers such as Keycloak/Auth0/Stytch (keycloak, auth0, stytch).
    public static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    private final String providerName;
    private final URI deviceAuthorizationEndpoint;
    private final URI tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final HttpClient httpClient;
    private final Duration timeout;

    public OidcAuthProvider(
            String providerName,
            URI deviceAuthorizationEndpoint,
            URI tokenEndpoint,
            String clientId,
            String clientSecret,
            String scope,
            Duration timeout
    ) {
        this.providerName = providerName == null || providerName.isBlank() ? "oidc" : providerName;
        this.deviceAuthorizationEndpoint = deviceAuthorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId == null ? "" : clientId;
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.scope = scope == null || scope.isBlank() ? "openid profile llm:chat" : scope;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
        if (deviceAuthorizationEndpoint == null || tokenEndpoint == null || this.clientId.isBlank()) {
            throw new IllegalArgumentException("OIDC mode requires EBB_OIDC_DEVICE_AUTH_URL, EBB_OIDC_TOKEN_URL, and EBB_OIDC_CLIENT_ID");
        }
    }

    @Override
    public ProviderStart start(String minecraftUuid, String serverId, String authSessionId, String userCode) {
        String form = form(Map.of(
                "client_id", clientId,
                "scope", scope
        ));
        try {
            HttpRequest request = HttpRequest.newBuilder(deviceAuthorizationEndpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return new ProviderStart("", userCode, "", 0L, 5L,
                        Map.of("error", "oidc_device_start_http_" + response.statusCode()));
            }
            String body = response.body();
            String deviceCode = HttpJson.stringValue(body, "device_code").orElse("");
            String verification = HttpJson.stringValue(body, "verification_uri_complete")
                    .or(() -> HttpJson.stringValue(body, "verification_uri"))
                    .or(() -> HttpJson.stringValue(body, "verification_url"))
                    .orElse("");
            String returnedUserCode = HttpJson.stringValue(body, "user_code").orElse(userCode);
            long expires = HttpJson.longValue(body, "expires_in", 600L);
            long interval = HttpJson.longValue(body, "interval", 5L);
            return new ProviderStart(verification, returnedUserCode, deviceCode, expires, interval,
                    Map.of("device_code", deviceCode, "provider", providerName));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ProviderStart("", userCode, "", 0L, 5L,
                    Map.of("error", "oidc_device_start_exception"));
        }
    }

    @Override
    public ProviderStatus poll(ProviderSession session) {
        String deviceCode = session.providerMetadata().getOrDefault("device_code", session.providerSessionId());
        if (deviceCode == null || deviceCode.isBlank()) {
            return ProviderStatus.error("missing_device_code");
        }
        Map<String, String> formValues = new LinkedHashMap<>();
        formValues.put("grant_type", DEVICE_CODE_GRANT);
        formValues.put("device_code", deviceCode);
        formValues.put("client_id", clientId);
        if (!clientSecret.isBlank()) {
            formValues.put("client_secret", clientSecret);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(tokenEndpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(formValues)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() / 100 == 2 && HttpJson.stringValue(body, "access_token").isPresent()) {
                String subject = HttpJson.stringValue(body, "sub").orElse("oidc:" + session.minecraftUuid());
                long expires = HttpJson.longValue(body, "expires_in", 3600L);
                return ProviderStatus.authenticated(subject, List.of("llm:chat", "memory:read_self", "memory:write_self"), expires);
            }
            String error = HttpJson.stringValue(body, "error").orElse("http_" + response.statusCode());
            if ("authorization_pending".equals(error) || "slow_down".equals(error)) {
                return ProviderStatus.pending();
            }
            return ProviderStatus.error(error);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ProviderStatus.error("oidc_poll_exception");
        }
    }

    @Override
    public String providerName() {
        return providerName;
    }

    private static String form(Map<String, String> values) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                out.append('&');
            }
            first = false;
            out.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            out.append('=');
            out.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return out.toString();
    }
}
