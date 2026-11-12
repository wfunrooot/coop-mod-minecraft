package com.coopkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class ClientBhop {
    private static final double START_SPEED = 0.30;
    private static final double MAX_SPEED = 1.675;
    private static final int HOPS_TO_MAX = 300;
    private static final int GROUND_GRACE_TICKS = 8;

    private static boolean enabled;
    private static boolean boosting;
    private static int hops;
    private static int groundTicks;

    private ClientBhop() {}

    public static void toggle(LocalPlayer player) {
        enabled = !enabled;
        clearMomentum();
        player.displayClientMessage(Component.translatable(enabled ? "coopkit.bhop.on" : "coopkit.bhop.off"), true);
    }

    public static void onJump(LocalPlayer player) {
        if (!canBoost(player) || player.horizontalCollision) {
            clearMomentum();
            return;
        }
        hops = Math.min(HOPS_TO_MAX, hops + 1);
        groundTicks = 0;
        boosting = true;

        double progress = progress();
        // At 0.54 blocks/tick, vanilla gravity gives a jump just under two blocks.
        double vertical = Math.max(player.getDeltaMovement().y, 0.42 + 0.12 * progress);
        applyVelocity(player, vertical);
    }

    public static void tick(LocalPlayer player) {
        if (player == null || !canBoost(player) || player.horizontalCollision) {
            clearMomentum();
            return;
        }
        if (player.onGround()) {
            boosting = false;
            if (++groundTicks > GROUND_GRACE_TICKS) clearMomentum();
        } else if (boosting) {
            applyVelocity(player, player.getDeltaMovement().y);
        }
    }

    private static boolean canBoost(LocalPlayer player) {
        return enabled && Minecraft.getInstance().screen == null
                && player.isAlive() && player.isSprinting() && player.input.forwardImpulse > 0
                && !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable();
    }

    private static double progress() {
        double remaining = 1.0 - (double) hops / HOPS_TO_MAX;
        return 1.0 - remaining * remaining;
    }

    private static void applyVelocity(LocalPlayer player, double vertical) {
        double speed = START_SPEED + (MAX_SPEED - START_SPEED) * progress();
        Vec3 direction = Vec3.directionFromRotation(0, player.getYRot());
        player.setDeltaMovement(direction.x * speed, vertical, direction.z * speed);
    }

    private static void clearMomentum() {
        boosting = false;
        hops = 0;
        groundTicks = 0;
    }

    public static void reset() {
        enabled = false;
        clearMomentum();
    }
}
