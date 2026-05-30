package com.crpg.ebb.client.render;

import com.crpg.ebb.client.interaction.ClientInteractionState;
import com.crpg.ebb.interaction.BlockGroupTarget;
import com.crpg.ebb.interaction.InteractionTarget;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class TargetHighlightRenderer {
    private static final int CLOSE_COLOR = 0xFF64E6FF;
    private static final int FAR_COLOR = 0xAA64E6FF;
    private static final int MAX_BLOCK_OUTLINES = 64;

    private TargetHighlightRenderer() {
    }

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(TargetHighlightRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        ClientInteractionState.Snapshot snapshot = ClientInteractionState.snapshot();
        if (snapshot.target().isEmpty()) {
            return;
        }

        Minecraft minecraft = context.gameRenderer().getMinecraft();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        InteractionTarget target = snapshot.target().get();
        Vec3 camera = context.gameRenderer().getMainCamera().position();
        RenderType renderType = RenderTypes.linesTranslucent();
        VertexConsumer consumer = context.bufferSource().getBuffer(renderType);
        int color = snapshot.withinInteractionRange() ? CLOSE_COLOR : FAR_COLOR;

        if (target instanceof BlockGroupTarget blockGroupTarget) {
            renderBlockGroup(context, consumer, camera, blockGroupTarget, color);
        } else {
            renderAabb(context, consumer, camera, target.bounds().inflate(0.01D), color);
        }

        context.bufferSource().endBatch(renderType);
    }

    private static void renderBlockGroup(
            LevelRenderContext context,
            VertexConsumer consumer,
            Vec3 camera,
            BlockGroupTarget target,
            int color
    ) {
        if (target.blocks().size() <= MAX_BLOCK_OUTLINES) {
            for (BlockPos blockPos : target.blocks()) {
                ShapeRenderer.renderShape(
                        context.poseStack(),
                        consumer,
                        Shapes.block(),
                        blockPos.getX() - camera.x,
                        blockPos.getY() - camera.y,
                        blockPos.getZ() - camera.z,
                        color,
                        1.0F
                );
            }
            return;
        }

        renderAabb(context, consumer, camera, target.bounds().inflate(0.01D), color);
    }

    private static void renderAabb(LevelRenderContext context, VertexConsumer consumer, Vec3 camera, AABB bounds, int color) {
        ShapeRenderer.renderShape(
                context.poseStack(),
                consumer,
                Shapes.create(bounds),
                -camera.x,
                -camera.y,
                -camera.z,
                color,
                1.0F
        );
    }
}
