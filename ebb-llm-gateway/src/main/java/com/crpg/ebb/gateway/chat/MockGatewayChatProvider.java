package com.crpg.ebb.gateway.chat;

public final class MockGatewayChatProvider extends FakeGatewayChatProvider {
    public MockGatewayChatProvider(String model) {
        super(model == null || model.isBlank() ? "mock-openai-responses" : model);
    }

    @Override
    public String providerName() {
        return "mock_openai_responses";
    }
}
