package com.crpg.ebb.client.npc;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.npc.ModEntityTypes;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    public static void register() {
        EbbMod.LOGGER.info("Registering role-aware Ebb NPC renderer.");
        EntityRendererRegistry.register(ModEntityTypes.NPC, EbbNpcRenderer::new);
    }
}
