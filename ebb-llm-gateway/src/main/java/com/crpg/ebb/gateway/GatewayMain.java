package com.crpg.ebb.gateway;

public final class GatewayMain {
    private GatewayMain() {
    }

    public static void main(String[] args) throws Exception {
        GatewayConfig config = GatewayConfig.fromEnv(System.getenv());
        GatewayServer server = GatewayServer.create(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "ebb-gateway-shutdown"));
        server.start();
        System.out.println("Ebb LLM Gateway listening on http://" + config.bindHost() + ":" + server.port()
                + " auth_provider=" + config.authProviderName());
        Thread.currentThread().join();
    }
}
