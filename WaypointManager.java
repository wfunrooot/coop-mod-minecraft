package com.coopkit.waypoints;

import com.coopkit.cheats.CheatManager;
import com.coopkit.net.Net;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WaypointManager {
    private static final int MAX = 200;

    private WaypointManager() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            server.execute(() -> Net.sendWaypoints(p, WaypointData.get(server).list));
        });
    }

    public static void add(ServerPlayer p, String rawName, int color, int size, int opacity) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        WaypointData data = WaypointData.get(server);
        if (data.list.size() >= MAX) {
            p.displayClientMessage(Component.translatable("coopkit.msg.limit"), true);
            return;
        }

        String name = rawName.replace("§", "").trim();
        if (name.length() > 32) name = name.substring(0, 32);
        if (name.isEmpty()) name = "#" + data.nextId;

        Waypoint w = new Waypoint(
                data.nextId++,
                name,
                p.level().dimension().location(),
                p.blockPosition(),
                color & 0xFFFFFF,
                p.getGameProfile().getName(),
                Waypoint.clampSize(size),
                Waypoint.clampOpacity(opacity));
        data.list.add(w);
        data.setDirty();

        broadcast(server);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("coopkit.msg.added", p.getDisplayName(), name), false);
    }

    public static void remove(ServerPlayer p, int id) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        WaypointData data = WaypointData.get(server);
        Optional<Waypoint> found = find(data, id);
        if (found.isEmpty()) return;

        data.list.remove(found.get());
        data.setDirty();
        broadcast(server);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("coopkit.msg.removed", p.getDisplayName(), found.get().name()), false);
    }

    public static final int TP_SELF = 0;
    public static final int TP_PLAYER = 1;
    public static final int TP_ALL = 2;

    /** Телепорт к метке — это чит: доступен хосту и тем, кому хост разрешил читы. */
    public static void teleport(ServerPlayer requester, int waypointId, int mode, UUID target) {
        if (!CheatManager.teleportAllowed(requester)) {
            requester.displayClientMessage(Component.translatable("coopkit.msg.tp_denied"), true);
            return;
        }
        MinecraftServer server = requester.getServer();
        if (server == null) return;
        Optional<Waypoint> found = find(WaypointData.get(server), waypointId);
        if (found.isEmpty()) return;
        Waypoint w = found.get();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, w.dim()));
        if (level == null) return;

        List<ServerPlayer> targets = new ArrayList<>();
        switch (mode) {
            case TP_SELF -> targets.add(requester);
            case TP_PLAYER -> {
                ServerPlayer t = target == null ? null : server.getPlayerList().getPlayer(target);
                if (t != null) targets.add(t);
            }
            case TP_ALL -> targets.addAll(server.getPlayerList().getPlayers());
            default -> {
            }
        }

        for (ServerPlayer t : targets) {
            t.teleportTo(level, w.pos().getX() + 0.5, w.pos().getY(), w.pos().getZ() + 0.5, t.getYRot(), t.getXRot());
            if (t != requester) {
                t.displayClientMessage(Component.translatable("coopkit.msg.teleported_by",
                        requester.getDisplayName(), w.name()), false);
            }
        }
        if (mode != TP_SELF && !targets.isEmpty()) {
            requester.displayClientMessage(Component.translatable("coopkit.msg.teleported_others",
                    targets.size(), w.name()), true);
        }
    }

    private static Optional<Waypoint> find(WaypointData data, int id) {
        return data.list.stream().filter(w -> w.id() == id).findFirst();
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Net.sendWaypoints(p, WaypointData.get(server).list);
        }
    }
}
