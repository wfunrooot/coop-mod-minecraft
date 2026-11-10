package com.coopkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

final class OnlinePlayers {
    private OnlinePlayers() {}

    /** Все игроки на сервере (кроме себя, если excludeSelf), по алфавиту. */
    static List<PlayerInfo> list(boolean excludeSelf) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        List<PlayerInfo> out = new ArrayList<>();
        if (conn == null) return out;
        UUID self = mc.player != null ? mc.player.getUUID() : null;
        for (PlayerInfo info : conn.getOnlinePlayers()) {
            if (excludeSelf && info.getProfile().getId().equals(self)) continue;
            out.add(info);
        }
        out.sort(Comparator.comparing(i -> i.getProfile().getName().toLowerCase()));
        return out;
    }
}
