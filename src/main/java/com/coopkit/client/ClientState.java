package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Данные, которые сервер присылает клиенту. */
public final class ClientState {
    public static volatile int cheatMask = 0;
    public static volatile boolean cheatsAllowed = false;
    public static volatile boolean teleportAllowed = false;
    public static volatile Set<UUID> teleportPlayers = Set.of();
    public static volatile boolean isHost = false;
    public static volatile List<Waypoint> waypoints = List.of();
    public static volatile Set<UUID> allowedPlayers = Set.of();
    public static volatile List<PlayerPos> positions = List.of();
    public static volatile boolean showHud = true;

    public record PlayerPos(UUID id, ResourceLocation dim, double x, double y, double z) {}

    private ClientState() {}

    public static void reset() {
        cheatMask = 0;
        cheatsAllowed = false;
        teleportAllowed = false;
        teleportPlayers = Set.of();
        isHost = false;
        waypoints = List.of();
        allowedPlayers = Set.of();
        positions = List.of();
    }

    /** Метки, которые этот игрок не скрыл. */
    public static List<Waypoint> visibleWaypoints() {
        return waypoints.stream().filter(w -> !HiddenWaypoints.isHidden(w.id())).toList();
    }
}
