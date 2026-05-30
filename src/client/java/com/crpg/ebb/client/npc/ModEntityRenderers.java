package com.crpg.ebb.client.npc;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.npc.ModEntityTypes;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    public static void register() {
        EntityRendererRegistry.register(ModEntityTypes.NPC, context ->
                new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<>(EbbMod.id("npc"))).withScale(1.0F)
        );
    }
}
