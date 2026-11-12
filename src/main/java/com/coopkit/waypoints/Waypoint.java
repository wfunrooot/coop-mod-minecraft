package com.coopkit.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * size — размер в процентах (50..200), opacity — непрозрачность в процентах (10..100).
 */
public record Waypoint(int id, String name, ResourceLocation dim, BlockPos pos, int color, String owner,
                       int size, int opacity, UUID ownerId, boolean publicMarker, Set<UUID> shared, Set<UUID> removed) {

    public boolean ownedBy(UUID playerId, String playerName) {
        return ownerId != null ? ownerId.equals(playerId) : owner.equalsIgnoreCase(playerName);
    }

    public boolean visibleTo(UUID playerId, String playerName) {
        return ownedBy(playerId, playerName) || (!removed.contains(playerId)
                && (publicMarker || shared.contains(playerId)));
    }

    private static void writeIds(FriendlyByteBuf buf, Set<UUID> ids, Collection<UUID> online) {
        var visibleIds = ids.stream().filter(online::contains).toList();
        buf.writeVarInt(visibleIds.size());
        visibleIds.forEach(buf::writeUUID);
    }

    private static Set<UUID> readIds(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < count; i++) ids.add(buf.readUUID());
        return ids;
    }

    private static ListTag toList(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) list.add(StringTag.valueOf(id.toString()));
        return list;
    }

    private static Set<UUID> fromList(ListTag list) {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            try { ids.add(UUID.fromString(list.getString(i))); }
            catch (IllegalArgumentException ignored) {}
        }
        return ids;
    }

    public static final int MIN_SIZE = 50, MAX_SIZE = 200;
    public static final int MIN_OPACITY = 10, MAX_OPACITY = 100;

    public static int clampSize(int v) {
        return Mth.clamp(v, MIN_SIZE, MAX_SIZE);
    }

    public static int clampOpacity(int v) {
        return Mth.clamp(v, MIN_OPACITY, MAX_OPACITY);
    }

    /** Альфа-канал 0..255 из процента непрозрачности. */
    public int alpha() {
        return Math.round(opacity * 2.55F);
    }

    public void write(FriendlyByteBuf buf, Collection<UUID> online) {
        buf.writeVarInt(id);
        buf.writeUtf(name, 64);
        buf.writeResourceLocation(dim);
        buf.writeBlockPos(pos);
        buf.writeInt(color);
        buf.writeUtf(owner, 64);
        buf.writeVarInt(size);
        buf.writeVarInt(opacity);
        buf.writeBoolean(ownerId != null);
        if (ownerId != null) buf.writeUUID(ownerId);
        buf.writeBoolean(publicMarker);
        writeIds(buf, shared, online);
        writeIds(buf, removed, online);
    }

    public static Waypoint read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String name = buf.readUtf(64);
        ResourceLocation dim = buf.readResourceLocation();
        BlockPos pos = buf.readBlockPos();
        int color = buf.readInt();
        String owner = buf.readUtf(64);
        int size = buf.readVarInt();
        int opacity = buf.readVarInt();
        UUID ownerId = buf.readBoolean() ? buf.readUUID() : null;
        boolean publicMarker = buf.readBoolean();
        return new Waypoint(id, name, dim, pos, color, owner, size, opacity, ownerId,
                publicMarker, readIds(buf), readIds(buf));
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("name", name);
        t.putString("dim", dim.toString());
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        t.putInt("color", color);
        t.putString("owner", owner);
        t.putInt("size", size);
        t.putInt("opacity", opacity);
        if (ownerId != null) t.putUUID("ownerId", ownerId);
        t.putBoolean("public", publicMarker);
        t.put("shared", toList(shared));
        t.put("removed", toList(removed));
        return t;
    }

    public static Waypoint fromTag(CompoundTag t) {
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
        if (dim == null) dim = new ResourceLocation("minecraft", "overworld");
        int size = t.contains("size") ? clampSize(t.getInt("size")) : 100;
        int opacity = t.contains("opacity") ? clampOpacity(t.getInt("opacity")) : 100;
        return new Waypoint(
                t.getInt("id"),
                t.getString("name"),
                dim,
                new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z")),
                t.getInt("color"),
                t.getString("owner"),
                size,
                opacity,
                t.hasUUID("ownerId") ? t.getUUID("ownerId") : null,
                !t.contains("public") || t.getBoolean("public"),
                fromList(t.getList("shared", Tag.TAG_STRING)),
                fromList(t.getList("removed", Tag.TAG_STRING)));
    }
}
