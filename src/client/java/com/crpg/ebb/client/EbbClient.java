package com.crpg.ebb.client;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.client.input.ClientKeyMappings;
import com.crpg.ebb.client.interaction.ClientTargetDetector;
import com.crpg.ebb.client.network.ClientInteractionNetworking;
import com.crpg.ebb.client.render.InteractionPromptHud;
import com.crpg.ebb.client.render.TargetHighlightRenderer;
import net.fabricmc.api.ClientModInitializer;

public final class EbbClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EbbMod.LOGGER.info("Initializing Esoteric Ebb CRPG client skeleton.");
        ClientInteractionNetworking.register();
        ClientKeyMappings.register();
        ClientTargetDetector.register();
        InteractionPromptHud.register();
        TargetHighlightRenderer.register();
    }
}
