package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Персональный порядок меток, локальный для игрока и конкретного мира/сервера. */
public final class WaypointOrder {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("coopkit-order.properties");
    private static final Properties PROPS = new Properties();
    private static boolean loaded;

    private WaypointOrder() {}

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                PROPS.load(reader);
            } catch (Exception ignored) {}
        }
    }

    private static String key() {
        Minecraft mc = Minecraft.getInstance();
        UUID player = mc.player == null ? null : mc.player.getUUID();
        IntegratedServer local = mc.getSingleplayerServer();
        String world;
        if (local != null) world = "local:" + local.getWorldData().getLevelName();
        else {
            ServerData server = mc.getCurrentServer();
            world = server == null ? "unknown" : "server:" + server.ip;
        }
        return world + "|" + (player == null ? "unknown" : player);
    }

    public static List<Waypoint> sort(List<Waypoint> input) {
        load();
        String value = PROPS.getProperty(key(), "");
        List<Integer> ids = parse(value);
        List<Waypoint> result = new ArrayList<>(input);
        result.sort(Comparator.comparingInt(w -> {
            int index = ids.indexOf(w.id());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        return result;
    }

    public static void moveToIndex(List<Waypoint> all, int fromId, int targetIndex) {
        load();
        List<Waypoint> ordered = sort(all);
        int from = indexOf(ordered, fromId);
        if (from < 0 || ordered.isEmpty()) return;
        Waypoint moving = ordered.remove(from);
        int to = Math.max(0, Math.min(targetIndex, ordered.size()));
        ordered.add(to, moving);
        StringBuilder value = new StringBuilder();
        for (Waypoint waypoint : ordered) {
            if (value.length() > 0) value.append(',');
            value.append(waypoint.id());
        }
        PROPS.setProperty(key(), value.toString());
        save();
    }

    public static void move(List<Waypoint> all, int fromId, int toId) {
        load();
        List<Waypoint> ordered = sort(all);
        int from = indexOf(ordered, fromId);
        int to = indexOf(ordered, toId);
        if (from < 0 || to < 0 || from == to) return;
        Waypoint moving = ordered.remove(from);
        ordered.add(to, moving);
        StringBuilder value = new StringBuilder();
        for (Waypoint waypoint : ordered) {
            if (value.length() > 0) value.append(',');
            value.append(waypoint.id());
        }
        PROPS.setProperty(key(), value.toString());
        save();
    }

    private static int indexOf(List<Waypoint> list, int id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id() == id) return i;
        return -1;
    }

    private static List<Integer> parse(String value) {
        List<Integer> result = new ArrayList<>();
        for (String part : value.split(",")) {
            try { if (!part.isBlank()) result.add(Integer.parseInt(part.trim())); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                PROPS.store(writer, "CoopKit: personal waypoint order per world and player");
            }
        } catch (Exception ignored) {}
    }
}
