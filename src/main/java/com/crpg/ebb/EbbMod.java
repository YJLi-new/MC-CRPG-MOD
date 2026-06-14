package com.crpg.ebb;

import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.network.ModPackets;
import com.crpg.ebb.llm.LlmChatService;
import com.crpg.ebb.network.sync.InteractionSyncService;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.npc.ModEntityTypes;
import com.crpg.ebb.registry.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EbbMod implements ModInitializer {
    public static final String MOD_ID = "ebb";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Esoteric Ebb CRPG mod skeleton.");
        ModEntityTypes.register();
        ModPackets.register();
        NarrativeDataRegistries.registerReloadListeners();
        DialogueService.registerLifecycleEvents();
        LlmChatService.registerLifecycleEvents();
        InteractionSyncService.registerLifecycleEvents();
        ModCommands.register();
    }
}
