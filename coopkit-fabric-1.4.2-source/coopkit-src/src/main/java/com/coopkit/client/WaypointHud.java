package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Полоса-компас сверху экрана: стороны света, метки и аватарки игроков. */
public final class WaypointHud {
    private static final String[] CARDINAL = {"S", "W", "N", "E"};
    private static final float[] CARDINAL_YAW = {0F, 90F, 180F, -90F};
    private static final int FACE = 8;

    private WaypointHud() {}

    public static void render(GuiGraphics g, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!ClientState.showHud || player == null || mc.options.hideGui) return;

        Font font = mc.font;
        int barW = Math.min(240, screenWidth - 40);
        int half = barW / 2;
        int cx = screenWidth / 2;
        int y = 3;

        g.fill(cx - half, y, cx + half, y + 12, 0x70000000);
        float yaw = Mth.wrapDegrees(player.getYRot());

        for (int i = 0; i < CARDINAL.length; i++) {
            float d = Mth.wrapDegrees(CARDINAL_YAW[i] - yaw);
            if (Math.abs(d) < 88F) {
                int x = cx + (int) (d / 90F * half);
                g.drawCenteredString(font, CARDINAL[i], x, y + 2, i == 2 ? 0xFF5555 : 0xFFFFFF);
            }
        }

        ResourceLocation dim = player.level().dimension().location();
        String focusText = null;
        int focusColor = 0xFFFFFF;
        float focusDiff = 8F;

        // метки
        for (Waypoint w : ClientState.visibleWaypoints()) {
            if (!w.dim().equals(dim)) continue;
            float d = angleTo(player, w.pos().getX() + 0.5, w.pos().getZ() + 0.5, yaw);
            int x = cx + (int) (Mth.clamp(d, -89F, 89F) / 90F * half);
            int hw = Math.max(1, Math.round(2 * w.size() / 100F));
            g.fill(x - hw, y + 1, x + hw, y + 11, (w.alpha() << 24) | w.color());
            if (Math.abs(d) < focusDiff) {
                focusDiff = Math.abs(d);
                int dist = (int) Math.sqrt(player.blockPosition().distSqr(w.pos()));
                focusText = w.name() + " · " + I18n.get("coopkit.dist", dist);
                focusColor = w.color();
            }
        }

        // игроки
        ClientPacketListener conn = mc.getConnection();
        if (conn != null) {
            float partial = mc.getFrameTime();
            for (ClientState.PlayerPos pos : ClientState.positions) {
                if (pos.id().equals(player.getUUID()) || !pos.dim().equals(dim)) continue;
                PlayerInfo info = conn.getPlayerInfo(pos.id());
                if (info == null) continue;

                double px = pos.x(), py = pos.y(), pz = pos.z();
                Player near = mc.level != null ? mc.level.getPlayerByUUID(pos.id()) : null;
                if (near != null) { // рядом — берём плавную позицию сущности
                    px = Mth.lerp(partial, near.xo, near.getX());
                    py = Mth.lerp(partial, near.yo, near.getY());
                    pz = Mth.lerp(partial, near.zo, near.getZ());
                }

                float d = angleTo(player, px, pz, yaw);
                int x = cx + (int) (Mth.clamp(d, -89F, 89F) / 90F * half);
                g.fill(x - FACE / 2 - 1, y + 1, x + FACE / 2 + 1, y + 3 + FACE, 0xFFFFFFFF);
                PlayerFaceRenderer.draw(g, info.getSkinLocation(), x - FACE / 2, y + 2, FACE);

                if (Math.abs(d) < focusDiff) {
                    focusDiff = Math.abs(d);
                    double dx = px - player.getX(), dy = py - player.getY(), dz = pz - player.getZ();
                    int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    focusText = info.getProfile().getName() + " · " + I18n.get("coopkit.dist", dist);
                    focusColor = 0xFFFFFF;
                }
            }
        }

        g.fill(cx, y - 1, cx + 1, y + 13, 0xFFFFFFFF);

        if (focusText != null) {
            g.drawCenteredString(font, focusText, cx, y + 16, focusColor);
        }
    }

    private static float angleTo(LocalPlayer player, double tx, double tz, float yaw) {
        double dx = tx - player.getX();
        double dz = tz - player.getZ();
        float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return Mth.wrapDegrees(target - yaw);
    }
}
