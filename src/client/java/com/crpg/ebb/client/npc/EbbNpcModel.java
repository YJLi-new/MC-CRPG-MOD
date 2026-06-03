package com.crpg.ebb.client.npc;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.npc.EbbNpcEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

final class EbbNpcModel extends DefaultedEntityGeoModel<EbbNpcEntity> {
    EbbNpcModel() {
        super(EbbMod.id("npc"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        String role = renderState.getOrDefaultGeckolibData(EbbNpcRenderData.VISUAL_ROLE, "npc");
        String fileName = "npc".equals(role) ? "npc.png" : "npc_" + role + ".png";
        return EbbMod.id("textures/entity/" + fileName);
    }
}
