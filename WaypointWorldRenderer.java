package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Подписи меток прямо в мире, видны сквозь стены. */
public final class WaypointWorldRenderer {
    private static final double MAX_LABEL_DIST = 48.0;
    private static final int FULL_BRIGHT = 0xF000F0;

    private WaypointWorldRenderer() {}

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.showHud || mc.player == null || mc.level == null || mc.options.hideGui) return;
        java.util.List<com.coopkit.waypoints.Waypoint> list = ClientState.visibleWaypoints();
        if (list.isEmpty()) return;

        Vec3 cam = camera.getPosition();
        ResourceLocation dim = mc.level.dimension().location();
        Font font = mc.font;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Waypoint w : list) {
            if (!w.dim().equals(dim)) continue;

            double dx = w.pos().getX() + 0.5 - cam.x;
            double dy = w.pos().getY() + 1.5 - cam.y;
            double dz = w.pos().getZ() + 0.5 - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 0.5) continue;

            // Дальние метки рисуем на расстоянии 48 блоков в их направлении — так они всегда видны.
            double shown = Math.min(dist, MAX_LABEL_DIST);
            double k = shown / dist;

            poseStack.pushPose();
            poseStack.translate(dx * k, dy * k, dz * k);
            poseStack.mulPose(camera.rotation());
            float scale = 0.0025F * (float) Math.max(shown, 4.0) * (w.size() / 100F);
            poseStack.scale(-scale, -scale, scale);
            Matrix4f matrix = poseStack.last().pose();

            int a = w.alpha();
            int color = (a << 24) | w.color();
            int bg = Math.round(a * 0.33F) << 24;
            int subColor = (a << 24) | 0xDDDDDD;
            String title = "◆ " + w.name();
            String sub = I18n.get("coopkit.dist", (int) dist);

            font.drawInBatch(title, -font.width(title) / 2F, 0F, color, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, bg, FULL_BRIGHT);
            font.drawInBatch(sub, -font.width(sub) / 2F, 10F, subColor, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, bg, FULL_BRIGHT);

            poseStack.popPose();
        }
        buffers.endBatch();
    }
}
