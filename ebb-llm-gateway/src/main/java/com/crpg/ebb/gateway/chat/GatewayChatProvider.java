package com.crpg.ebb.gateway.chat;

public interface GatewayChatProvider {
    GatewayChatResponse send(GatewayChatRequest request) throws Exception;

    String providerName();

    default boolean usesOpenAiNetwork() {
        return false;
    }
}
