package com.coopkit;

import com.coopkit.cheats.CheatManager;
import com.coopkit.net.Net;
import com.coopkit.waypoints.WaypointManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class CoopKit implements ModInitializer {
    public static final String MODID = "coopkit";

    @Override
    public void onInitialize() {
        Config.load();
        Net.registerServer();
        CheatManager.register();
        WaypointManager.register();
        ServerLifecycleEvents.SERVER_STARTED.register(CoopKit::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        // Иначе сервер кикает за "полёт" при включённом чите полёта.
        if (Config.cheatsEnabled) {
            server.setFlightAllowed(true);
        }
        // «Открыть для сети» в ванилле требует лицензию у входящих игроков.
        if (!server.isDedicatedServer() && Config.forceOfflineLan) {
            server.setUsesAuthentication(false);
        }
    }
}
