package com.crpg.ebb.gateway.auth;

import java.net.URI;
import java.util.List;
import java.util.Map;

public final class DevLocalAuthProvider implements AuthProvider {
    private final URI publicBaseUri;

    public DevLocalAuthProvider(URI publicBaseUri) {
        this.publicBaseUri = publicBaseUri == null ? URI.create("http://127.0.0.1:8787") : publicBaseUri;
    }

    @Override
    public ProviderStart start(String minecraftUuid, String serverId, String authSessionId, String userCode) {
        String url = publicBaseUri.resolve("/v1/auth/dev-local/complete?auth_session_id=" + authSessionId
                + "&user_code=" + userCode).toString();
        return new ProviderStart(url, userCode, "dev-local:" + authSessionId, 600L, 1L,
                Map.of("dev_local", "true", "minecraft_uuid", minecraftUuid == null ? "" : minecraftUuid));
    }

    @Override
    public ProviderStatus poll(ProviderSession session) {
        return ProviderStatus.authenticated("dev-local:" + session.minecraftUuid(),
                List.of("llm:chat", "memory:read_self", "memory:write_self"), 3600L);
    }

    @Override
    public String providerName() {
        return "dev_local";
    }
}
