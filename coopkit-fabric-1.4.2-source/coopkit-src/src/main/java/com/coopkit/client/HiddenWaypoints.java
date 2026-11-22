package com.coopkit.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Скрытые «только для себя» метки. Хранятся у игрока: config/coopkit-hidden.properties */
public final class HiddenWaypoints {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("coopkit-hidden.properties");
    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    private static String cachedKey = null;
    private static Set<Integer> cached = new HashSet<>();

    private HiddenWaypoints() {}

    /** Ключ мира: имя сохранения для своего мира или адрес сервера. */
    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer local = mc.getSingleplayerServer();
        if (local != null) return "local:" + local.getWorldData().getLevelName();
        ServerData server = mc.getCurrentServer();
        if (server != null) return "server:" + server.ip;
        return "unknown";
    }

    private static Set<Integer> current() {
        if (!loaded) {
            loaded = true;
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                    PROPS.load(r);
                } catch (Exception ignored) {
                }
            }
        }
        String key = worldKey();
        if (!key.equals(cachedKey)) {
            cachedKey = key;
            cached = new HashSet<>();
            String v = PROPS.getProperty(key, "");
            for (String part : v.split(",")) {
                try {
                    if (!part.isBlank()) cached.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return cached;
    }

    public static boolean isHidden(int id) {
        return current().contains(id);
    }

    public static void toggle(int id) {
        Set<Integer> set = current();
        if (!set.remove(id)) set.add(id);
        save();
    }

    public static void setAll(Collection<Integer> ids, boolean hidden) {
        Set<Integer> set = current();
        if (hidden) set.addAll(ids);
        else set.removeAll(ids);
        save();
    }

    private static void save() {
        StringBuilder sb = new StringBuilder();
        for (int id : cached) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        PROPS.setProperty(cachedKey, sb.toString());
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                PROPS.store(w, "CoopKit: waypoints hidden only for you");
            }
        } catch (Exception ignored) {
        }
    }
}
