package com.crpg.ebb.client.npc;

import com.crpg.ebb.npc.EbbNpcEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

final class EbbNpcRenderer extends GeoEntityRenderer<EbbNpcEntity, EntityRenderState> {
    EbbNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new EbbNpcModel());
        withScale(1.0F);
    }

    @Override
    public void captureDefaultRenderState(EbbNpcEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, renderState, partialTick);
        if (renderState instanceof GeoRenderState geoRenderState) {
            geoRenderState.addGeckolibData(EbbNpcRenderData.VISUAL_ROLE, animatable.visualRole());
        }
    }
}
