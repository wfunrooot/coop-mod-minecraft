package com.coopkit.client;

import com.coopkit.cheats.Cheat;
import com.coopkit.net.Net;
import com.coopkit.waypoints.Waypoint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientNet {
    private ClientNet() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Net.CHEAT_STATE, (client, handler, buf, sender) -> {
            int mask = buf.readVarInt();
            boolean allowed = buf.readBoolean();
            boolean host = buf.readBoolean();
            boolean teleportAllowed = buf.readBoolean();
            client.execute(() -> {
                ClientState.cheatMask = mask;
                ClientState.cheatsAllowed = allowed;
                ClientState.isHost = host;
                ClientState.teleportAllowed = teleportAllowed;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.WAYPOINTS, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            List<Waypoint> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(Waypoint.read(buf));
            List<Waypoint> copy = List.copyOf(list);
            client.execute(() -> ClientState.waypoints = copy);
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.PERMISSIONS, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            Set<UUID> set = new HashSet<>();
            for (int i = 0; i < n; i++) set.add(buf.readUUID());
            Set<UUID> copy = Set.copyOf(set);
                        int count = buf.readVarInt();
            Set<UUID> teleports = new HashSet<>();
            for (int i = 0; i < count; i++) teleports.add(buf.readUUID());
            Set<UUID> teleportCopy = Set.copyOf(teleports);
            client.execute(() -> {
                ClientState.allowedPlayers = copy;
                ClientState.teleportPlayers = teleportCopy;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.POSITIONS, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            List<ClientState.PlayerPos> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new ClientState.PlayerPos(buf.readUUID(), buf.readResourceLocation(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            List<ClientState.PlayerPos> copy = List.copyOf(list);
            client.execute(() -> ClientState.positions = copy);
        });
    }

    public static void toggleCheat(Cheat c) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(c.ordinal());
        ClientPlayNetworking.send(Net.TOGGLE_CHEAT, buf);
    }

    public static void addWaypoint(String name, int color, int size, int opacity, boolean publicMarker) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(name, 64);
        buf.writeInt(color);
        buf.writeVarInt(size);
        buf.writeVarInt(opacity);
        buf.writeBoolean(publicMarker);
        ClientPlayNetworking.send(Net.ADD_WAYPOINT, buf);
    }

    public static void shareWaypoint(int id, UUID recipient) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(id);
        buf.writeUUID(recipient);
        ClientPlayNetworking.send(Net.SHARE_WAYPOINT, buf);
    }

    public static void removeWaypoint(int id) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(id);
        buf.writeVarInt(Net.ACTION_REMOVE);
        ClientPlayNetworking.send(Net.WAYPOINT_ACTION, buf);
    }

    /** mode: 0 — себя, 1 — игрока target, 2 — всех. */
    public static void teleport(int waypointId, int mode, UUID target) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(waypointId);
        buf.writeVarInt(mode);
        buf.writeBoolean(target != null);
        if (target != null) buf.writeUUID(target);
        ClientPlayNetworking.send(Net.TELEPORT, buf);
    }

    public static void setPermission(UUID target, boolean allow, boolean teleport) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(target);
        buf.writeBoolean(allow);
        buf.writeBoolean(teleport);
        ClientPlayNetworking.send(Net.SET_PERMISSION, buf);
    }

    public static void setWeather(int type) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(type);
        ClientPlayNetworking.send(Net.SET_WEATHER, buf);
    }

    public static void setTime(int time) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(time);
        ClientPlayNetworking.send(Net.SET_TIME, buf);
    }
}
