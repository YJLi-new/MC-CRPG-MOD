package com.crpg.ebb.npc;

import com.crpg.ebb.EbbMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntityTypes {
    public static final ResourceKey<EntityType<?>> NPC_KEY = ResourceKey.create(Registries.ENTITY_TYPE, EbbMod.id("npc"));
    public static final EntityType<EbbNpcEntity> NPC = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            EbbMod.id("npc"),
            FabricEntityTypeBuilder.create(MobCategory.MISC, EbbNpcEntity::new)
                    .dimensions(EntityDimensions.scalable(0.6F, 1.95F))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(3)
                    .build(NPC_KEY)
    );

    private ModEntityTypes() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(NPC, EbbNpcEntity.createAttributes());
    }
}
