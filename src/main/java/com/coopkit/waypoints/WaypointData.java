package com.coopkit.waypoints;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Хранится в мире: saves/<мир>/data/coopkit_waypoints.dat */
public class WaypointData extends SavedData {
    private static final String NAME = "coopkit_waypoints";

    public final List<Waypoint> list = new ArrayList<>();
    public int nextId = 1;

    public static WaypointData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WaypointData::load, WaypointData::new, NAME);
    }

    public static WaypointData load(CompoundTag tag) {
        WaypointData data = new WaypointData();
        ListTag l = tag.getList("list", Tag.TAG_COMPOUND);
        int maxId = 0;
        for (int i = 0; i < l.size(); i++) {
            Waypoint w = Waypoint.fromTag(l.getCompound(i));
            data.list.add(w);
            maxId = Math.max(maxId, w.id());
        }
        data.nextId = Math.max(tag.getInt("nextId"), maxId + 1);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (Waypoint w : list) l.add(w.toTag());
        tag.put("list", l);
        tag.putInt("nextId", nextId);
        return tag;
    }
}
