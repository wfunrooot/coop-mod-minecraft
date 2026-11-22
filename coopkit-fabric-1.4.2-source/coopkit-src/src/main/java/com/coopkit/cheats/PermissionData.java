package com.coopkit.cheats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Кому хост разрешил читы. saves/<мир>/data/coopkit_permissions.dat */
public class PermissionData extends SavedData {
    private static final String NAME = "coopkit_permissions";
    public final Set<UUID> allowed = new HashSet<>();
    public final Set<UUID> teleportAllowed = new HashSet<>();

    public static PermissionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PermissionData::load, PermissionData::new, NAME);
    }

    public static PermissionData load(CompoundTag tag) {
        PermissionData d = new PermissionData();
        ListTag list = tag.getList("allowed", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                d.allowed.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (!tag.contains("teleportAllowed", Tag.TAG_LIST)) {
            d.teleportAllowed.addAll(d.allowed);
        } else {
            ListTag teleports = tag.getList("teleportAllowed", Tag.TAG_STRING);
            for (int i = 0; i < teleports.size(); i++) {
                try { d.teleportAllowed.add(UUID.fromString(teleports.getString(i))); }
                catch (IllegalArgumentException ignored) {}
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID id : allowed) list.add(StringTag.valueOf(id.toString()));
        tag.put("allowed", list);
        ListTag teleports = new ListTag();
        for (UUID id : teleportAllowed) teleports.add(StringTag.valueOf(id.toString()));
        tag.put("teleportAllowed", teleports);
        return tag;
    }
}
