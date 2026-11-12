package com.coopkit.cheats;

import com.coopkit.Config;
import com.coopkit.net.Net;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Вся логика читов работает на сервере (у хоста), клиент только шлёт "переключи". */
public final class CheatManager {
    private CheatManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CheatManager::onServerTick);

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p)) return true;
            if (has(p, Cheat.GOD) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            if (has(p, Cheat.NO_FALL) && source.is(DamageTypeTags.IS_FALL)) return false;
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            server.execute(() -> {
                apply(p);
                sync(p);
                Net.sendPermissions(p, PermissionData.get(server).allowed, PermissionData.get(server).teleportAllowed);
            });
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            apply(newPlayer);
            sync(newPlayer);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> apply(player));
    }

    public static int mask(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        return server == null ? 0 : CheatData.get(server).get(p.getUUID()) & Cheat.validMask();
    }

    public static boolean has(ServerPlayer p, Cheat c) {
        return (mask(p) & c.bit()) != 0;
    }

    private static void setMask(ServerPlayer p, int m) {
        MinecraftServer server = p.getServer();
        if (server != null) CheatData.get(server).set(p.getUUID(), m);
    }

    /** Хост: владелец мира при «Открыть для сети», на выделенном сервере — оператор. */
    public static boolean isHost(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return false;
        if (server.isSingleplayerOwner(p.getGameProfile())) return true;
        return server.isDedicatedServer() && p.hasPermissions(2);
    }

    public static boolean allowed(ServerPlayer p) {
        if (!Config.cheatsEnabled) return false;
        if (isHost(p)) return true;
        MinecraftServer server = p.getServer();
        return server != null && PermissionData.get(server).allowed.contains(p.getUUID());
    }

    public static boolean teleportAllowed(ServerPlayer p) {
        if (isHost(p)) return true;
        MinecraftServer server = p.getServer();
        return server != null && PermissionData.get(server).teleportAllowed.contains(p.getUUID());
    }

    /** Хост выдаёт или забирает доступ к выбранной категории. */
    public static void setPermission(ServerPlayer host, java.util.UUID target, boolean allow, boolean teleport) {
        if (!isHost(host)) return;
        MinecraftServer server = host.getServer();
        if (server == null) return;
        PermissionData data = PermissionData.get(server);
        java.util.Set<java.util.UUID> category = teleport ? data.teleportAllowed : data.allowed;
        boolean changed = allow ? category.add(target) : category.remove(target);
        if (!changed) return;
        data.setDirty();

        ServerPlayer t = server.getPlayerList().getPlayer(target);
        if (t != null) {
            if (!allowed(t)) clearAll(t);
            else sync(t);
            t.displayClientMessage(Component.translatable(
                    teleport ? (allow ? "coopkit.msg.tp_granted" : "coopkit.msg.tp_revoked") : (allow ? "coopkit.msg.perm_granted" : "coopkit.msg.perm_revoked"), host.getDisplayName()), false);
        }
        broadcastPermissions(server);
    }

    public static void broadcastPermissions(MinecraftServer server) {
        java.util.Set<java.util.UUID> set = PermissionData.get(server).allowed;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) Net.sendPermissions(p, set, PermissionData.get(server).teleportAllowed);
    }

    public static void toggle(ServerPlayer p, Cheat c) {
        boolean turningOn = !has(p, c);
        if (turningOn && !allowed(p)) {
            p.displayClientMessage(Component.translatable("coopkit.msg.cheats_denied"), true);
            sync(p);
            return;
        }
        setMask(p, mask(p) ^ c.bit());
        if (!turningOn) disable(p, c);
        apply(p);
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        Net.sendCheatState(p, mask(p), allowed(p), isHost(p), teleportAllowed(p));
    }

    private static void clearAll(ServerPlayer p) {
        for (Cheat c : Cheat.values()) {
            if (has(p, c)) disable(p, c);
        }
        setMask(p, 0);
        sync(p);
    }

    private static void disable(ServerPlayer p, Cheat c) {
        switch (c) {
            case FLY -> {
                if (!p.isCreative() && !p.isSpectator()) {
                    p.getAbilities().mayfly = false;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                }
            }
            case NIGHT_VISION -> p.removeEffect(MobEffects.NIGHT_VISION);
            case WATER_BREATHING -> p.removeEffect(MobEffects.WATER_BREATHING);
            default -> {
            }
        }
    }

    private static void apply(ServerPlayer p) {
        int m = mask(p);
        if (m == 0) return;

        if ((m & Cheat.FLY.bit()) != 0 && !p.getAbilities().mayfly) {
            p.getAbilities().mayfly = true;
            p.onUpdateAbilities();
        }
        if ((m & Cheat.NIGHT_VISION.bit()) != 0) effect(p, MobEffects.NIGHT_VISION, 0);
        if ((m & Cheat.WATER_BREATHING.bit()) != 0) effect(p, MobEffects.WATER_BREATHING, 0);
    }

    private static void effect(ServerPlayer p, MobEffect effect, int amplifier) {
        MobEffectInstance current = p.getEffect(effect);
        if (current == null || current.getDuration() < 300) {
            p.addEffect(new MobEffectInstance(effect, 400, amplifier, false, false, true));
        }
    }

    private static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % 10 == 0) Net.broadcastPositions(server);
        boolean refreshEffects = server.getTickCount() % 20 == 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int active = mask(p);
            if (active == 0 || (!refreshEffects && (active & Cheat.NO_HUNGER.bit()) == 0)) continue;
            if (!allowed(p)) {
                clearAll(p);
                continue;
            }
            if ((active & Cheat.NO_HUNGER.bit()) != 0) {
                p.getFoodData().setFoodLevel(20);
                p.getFoodData().setExhaustion(0.0F);
            }
            if (refreshEffects) apply(p);
        }
    }
}
