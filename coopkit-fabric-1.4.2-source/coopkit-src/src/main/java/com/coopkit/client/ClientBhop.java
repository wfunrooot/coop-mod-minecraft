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
    private static final double TURN_BONUS_END = 0.50;
    private static final double TURN_START_DOT = Math.cos(Math.toRadians(25));
    private static final double TURN_FULL_DOT = Math.cos(Math.toRadians(80));
    private static final double TURN_REVERSE_DOT = Math.cos(Math.toRadians(135));

    private static boolean enabled;
    private static boolean boosting;
    private static double hops;
    private static int groundTicks;
    private static Vec3 launchDirection;
    private static double strongestTurn;

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
        Vec3 direction = inputDirection(player);
        double turn = Math.max(strongestTurn, turnStrength(direction));
        double earlyBoost = Math.max(0.0, 1.0 - progress() / TURN_BONUS_END);
        hops = Math.min(HOPS_TO_MAX, hops + 1.0 + 3.0 * turn * earlyBoost);
        groundTicks = 0;
        boosting = true;
        launchDirection = direction;
        strongestTurn = 0.0;

        double progress = progress();
        // At 0.54 blocks/tick, vanilla gravity gives a jump just under two blocks.
        double vertical = Math.max(player.getDeltaMovement().y, 0.42 + 0.12 * progress);
        applyVelocity(player, direction, vertical);
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
            Vec3 direction = inputDirection(player);
            strongestTurn = Math.max(strongestTurn, turnStrength(direction));
            applyVelocity(player, direction, player.getDeltaMovement().y);
        }
    }

    private static boolean canBoost(LocalPlayer player) {
        boolean forward = player.input.forwardImpulse > 0;
        boolean sideways = player.input.forwardImpulse == 0 && player.input.leftImpulse != 0;
        boolean running = (player.isSprinting() && forward) || (hops > 0 && (forward || sideways));
        return enabled && Minecraft.getInstance().screen == null
                && player.isAlive() && running
                && !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying
                && !player.isInWaterOrBubble() && !player.isInLava() && !player.onClimbable();
    }

    private static double progress() {
        double remaining = 1.0 - hops / HOPS_TO_MAX;
        return 1.0 - remaining * remaining;
    }

    private static Vec3 inputDirection(LocalPlayer player) {
        float yaw = player.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0, yaw);
        Vec3 left = Vec3.directionFromRotation(0, yaw - 90);
        return forward.scale(player.input.forwardImpulse)
                .add(left.scale(player.input.leftImpulse)).normalize();
    }

    private static double turnStrength(Vec3 direction) {
        if (launchDirection == null) return 0.0;
        double dot = launchDirection.dot(direction);
        if (dot >= TURN_START_DOT || dot <= TURN_REVERSE_DOT) return 0.0;
        return Math.min(1.0, (TURN_START_DOT - dot) / (TURN_START_DOT - TURN_FULL_DOT));
    }

    private static void applyVelocity(LocalPlayer player, Vec3 direction, double vertical) {
        double speed = START_SPEED + (MAX_SPEED - START_SPEED) * progress();
        player.setDeltaMovement(direction.x * speed, vertical, direction.z * speed);
    }

    private static void clearMomentum() {
        boosting = false;
        hops = 0;
        groundTicks = 0;
        launchDirection = null;
        strongestTurn = 0.0;
    }

    public static void reset() {
        enabled = false;
        clearMomentum();
    }
}
