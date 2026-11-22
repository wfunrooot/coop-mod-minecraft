package com.coopkit.cheats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Какие читы включены у каждого игрока. saves/<мир>/data/coopkit_cheats.dat */
public class CheatData extends SavedData {
    private static final String NAME = "coopkit_cheats";
    private final Map<UUID, Integer> masks = new HashMap<>();

    public static CheatData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CheatData::load, CheatData::new, NAME);
    }

    public int get(UUID id) {
        return masks.getOrDefault(id, 0);
    }

    public void set(UUID id, int mask) {
        if (mask == 0) masks.remove(id);
        else masks.put(id, mask);
        setDirty();
    }

    public static CheatData load(CompoundTag tag) {
        CheatData d = new CheatData();
        for (String key : tag.getAllKeys()) {
            try {
                d.masks.put(UUID.fromString(key), tag.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        masks.forEach((id, m) -> tag.putInt(id.toString(), m));
        return tag;
    }
}
