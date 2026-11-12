package com.coopkit.net;

import com.coopkit.CoopKit;
import com.coopkit.cheats.Cheat;
import com.coopkit.cheats.CheatManager;
import com.coopkit.waypoints.Waypoint;
import com.coopkit.waypoints.WaypointManager;
import com.coopkit.world.WorldControl;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Пакеты между клиентом и сервером (Fabric Networking API). */
public final class Net {
    // клиент -> сервер
    public static final ResourceLocation TOGGLE_CHEAT = id("toggle_cheat");
    public static final ResourceLocation ADD_WAYPOINT = id("add_waypoint_v2");
    public static final ResourceLocation WAYPOINT_ACTION = id("waypoint_action");
    public static final ResourceLocation SHARE_WAYPOINT = id("share_waypoint");
    public static final ResourceLocation TELEPORT = id("teleport");
    public static final ResourceLocation SET_PERMISSION = id("set_permission");
    public static final ResourceLocation SET_WEATHER = id("set_weather");
    public static final ResourceLocation SET_TIME = id("set_time");
    // сервер -> клиент
    public static final ResourceLocation CHEAT_STATE = id("cheat_state");
    public static final ResourceLocation WAYPOINTS = id("waypoints_v2");
    public static final ResourceLocation PERMISSIONS = id("permissions");
    public static final ResourceLocation POSITIONS = id("positions");

    public static final int ACTION_REMOVE = 0;

    private Net() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CoopKit.MODID, path);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_CHEAT, (server, player, handler, buf, sender) -> {
            int ordinal = buf.readVarInt();
            server.execute(() -> {
                Cheat[] all = Cheat.values();
                if (ordinal >= 0 && ordinal < all.length) CheatManager.toggle(player, all[ordinal]);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ADD_WAYPOINT, (server, player, handler, buf, sender) -> {
            String name = buf.readUtf(64);
            int color = buf.readInt();
            int size = buf.readVarInt();
            int opacity = buf.readVarInt();
            boolean publicMarker = buf.readBoolean();
            server.execute(() -> WaypointManager.add(player, name, color, size, opacity, publicMarker));
        });
        ServerPlayNetworking.registerGlobalReceiver(WAYPOINT_ACTION, (server, player, handler, buf, sender) -> {
            int id = buf.readVarInt();
            int action = buf.readVarInt();
            server.execute(() -> {
                if (action == ACTION_REMOVE) WaypointManager.remove(player, id);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(SHARE_WAYPOINT, (server, player, handler, buf, sender) -> {
            int id = buf.readVarInt();
            UUID recipient = buf.readUUID();
            server.execute(() -> WaypointManager.share(player, id, recipient));
        });
        ServerPlayNetworking.registerGlobalReceiver(TELEPORT, (server, player, handler, buf, sender) -> {
            int waypointId = buf.readVarInt();
            int mode = buf.readVarInt();
            UUID target = buf.readBoolean() ? buf.readUUID() : null;
            server.execute(() -> WaypointManager.teleport(player, waypointId, mode, target));
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_PERMISSION, (server, player, handler, buf, sender) -> {
            UUID target = buf.readUUID();
            boolean allow = buf.readBoolean();
            boolean teleport = buf.readBoolean();
            server.execute(() -> CheatManager.setPermission(player, target, allow, teleport));
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_WEATHER, (server, player, handler, buf, sender) -> {
            int type = buf.readVarInt();
            server.execute(() -> WorldControl.setWeather(player, type));
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_TIME, (server, player, handler, buf, sender) -> {
            int time = buf.readVarInt();
            server.execute(() -> WorldControl.setTime(player, time));
        });
    }

    public static void sendCheatState(ServerPlayer p, int mask, boolean allowed, boolean host, boolean teleportAllowed) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(mask);
        buf.writeBoolean(allowed);
        buf.writeBoolean(host);
        buf.writeBoolean(teleportAllowed);
        ServerPlayNetworking.send(p, CHEAT_STATE, buf);
    }

    public static void sendWaypoints(ServerPlayer p, List<Waypoint> list, Collection<UUID> online) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(list.size());
        for (Waypoint w : list) w.write(buf, online);
        ServerPlayNetworking.send(p, WAYPOINTS, buf);
    }

    public static void sendPermissions(ServerPlayer p, Collection<UUID> allowed, Collection<UUID> teleports) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(allowed.size());
        for (UUID id : allowed) buf.writeUUID(id);
        buf.writeVarInt(teleports.size());
        for (UUID id : teleports) buf.writeUUID(id);
        ServerPlayNetworking.send(p, PERMISSIONS, buf);
    }

    /** Позиции всех игроков — для аватарок на компасе (даже если игрок далеко). */
    public static void broadcastPositions(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < 2) return;
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(players.size());
        for (ServerPlayer p : players) {
            buf.writeUUID(p.getUUID());
            buf.writeResourceLocation(p.level().dimension().location());
            buf.writeDouble(p.getX());
            buf.writeDouble(p.getY());
            buf.writeDouble(p.getZ());
        }
        for (ServerPlayer p : players) ServerPlayNetworking.send(p, POSITIONS, PacketByteBufs.copy(buf));
    }
}
