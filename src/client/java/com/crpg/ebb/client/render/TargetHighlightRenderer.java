package com.crpg.ebb.client.render;

import com.crpg.ebb.client.interaction.ClientInteractionState;
import com.crpg.ebb.interaction.HighlightStyle;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TargetHighlightRenderer {
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
        HighlightStyle style = snapshot.highlightStyle();
        int color = snapshot.withinInteractionRange() ? style.closeColor() : style.farColor();

        if (target instanceof BlockGroupTarget blockGroupTarget) {
            renderBlockGroup(context, consumer, camera, blockGroupTarget, style, color);
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
            HighlightStyle style,
            int color
    ) {
        if (style.renderMode() == HighlightStyle.RenderMode.BOUNDS || target.blocks().size() > MAX_BLOCK_OUTLINES) {
            renderAabb(context, consumer, camera, target.bounds().inflate(0.01D), color);
            return;
        }

        if (style.renderMode() == HighlightStyle.RenderMode.MERGED) {
            for (AABB mergedBox : mergeAdjacentBlockBoxes(target.blocks())) {
                renderAabb(context, consumer, camera, mergedBox.inflate(0.01D), color);
            }
            return;
        }

        if (style.renderMode() == HighlightStyle.RenderMode.OUTLINE) {
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

    private static List<AABB> mergeAdjacentBlockBoxes(List<BlockPos> blocks) {
        Set<BlockPos> remaining = new HashSet<>(blocks);
        List<AABB> boxes = new ArrayList<>();
        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            int minX = start.getX();
            int minY = start.getY();
            int minZ = start.getZ();
            int maxX = minX;
            int maxY = minY;
            int maxZ = minZ;

            while (containsCuboid(remaining, minX, minY, minZ, maxX + 1, maxY, maxZ)) {
                maxX++;
            }
            while (containsCuboid(remaining, minX, minY, minZ, maxX, maxY, maxZ + 1)) {
                maxZ++;
            }
            while (containsCuboid(remaining, minX, minY, minZ, maxX, maxY + 1, maxZ)) {
                maxY++;
            }

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        remaining.remove(new BlockPos(x, y, z));
                    }
                }
            }
            boxes.add(AABB.encapsulatingFullBlocks(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)));
        }
        return boxes;
    }

    private static boolean containsCuboid(Set<BlockPos> blocks, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!blocks.contains(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
