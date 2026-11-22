package com.coopkit;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Файл настроек: .minecraft/config/coopkit.properties (у того, кто хостит мир). */
public final class Config {
    public static boolean cheatsEnabled = true;
    public static boolean forceOfflineLan = true;

    private Config() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("coopkit.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (Exception ignored) {
            }
        }
        cheatsEnabled = Boolean.parseBoolean(p.getProperty("cheats.enabled", "true"));
        forceOfflineLan = Boolean.parseBoolean(p.getProperty("network.forceOfflineLan", "true"));

        p.setProperty("cheats.enabled", String.valueOf(cheatsEnabled));
        p.remove("cheats.opsOnly");
        p.setProperty("network.forceOfflineLan", String.valueOf(forceOfflineLan));
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(w, "CoopKit: cheats.enabled - cheats allowed; "
                        + "per-player access is set by the host in the K menu; "
                        + "network.forceOfflineLan - let unlicensed players join Open to LAN");
            }
        } catch (Exception ignored) {
        }
    }
}
