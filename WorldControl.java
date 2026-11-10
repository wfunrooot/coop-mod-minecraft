package com.coopkit.world;

import com.coopkit.cheats.CheatManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Погода и время — тоже читы: доступно хосту и тем, кому он разрешил. */
public final class WorldControl {
    public static final int WEATHER_CLEAR = 0;
    public static final int WEATHER_RAIN = 1;
    public static final int WEATHER_THUNDER = 2;

    private static final String[] WEATHER_KEYS = {"coopkit.weather.clear", "coopkit.weather.rain", "coopkit.weather.thunder"};

    private WorldControl() {}

    public static void setWeather(ServerPlayer p, int type) {
        if (!check(p) || type < 0 || type > 2) return;
        MinecraftServer server = p.getServer();
        ServerLevel level = server.overworld();
        int duration = 12000; // полдня реального цикла, потом погода снова меняется сама
        switch (type) {
            case WEATHER_CLEAR -> level.setWeatherParameters(duration, 0, false, false);
            case WEATHER_RAIN -> level.setWeatherParameters(0, duration, true, false);
            case WEATHER_THUNDER -> level.setWeatherParameters(0, duration, true, true);
            default -> {
            }
        }
        server.getPlayerList().broadcastSystemMessage(Component.translatable("coopkit.msg.weather_set",
                p.getDisplayName(), Component.translatable(WEATHER_KEYS[type])), false);
    }

    /** time — время суток 0..23999. Номер дня сохраняется, время идёт только вперёд. */
    public static void setTime(ServerPlayer p, int time) {
        if (!check(p)) return;
        int t = Math.floorMod(time, 24000);
        MinecraftServer server = p.getServer();
        long current = server.overworld().getDayTime();
        long target = current - Math.floorMod(current, 24000L) + t;
        if (target < current) target += 24000L;
        for (ServerLevel level : server.getAllLevels()) {
            level.setDayTime(target);
        }
        server.getPlayerList().broadcastSystemMessage(Component.translatable("coopkit.msg.time_set",
                p.getDisplayName(), t), false);
    }

    private static boolean check(ServerPlayer p) {
        if (p.getServer() == null) return false;
        if (!CheatManager.allowed(p)) {
            p.displayClientMessage(Component.translatable("coopkit.msg.cheats_denied"), true);
            return false;
        }
        return true;
    }
}
