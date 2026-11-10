package com.coopkit.waypoints;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * size — размер в процентах (50..200), opacity — непрозрачность в процентах (10..100).
 */
public record Waypoint(int id, String name, ResourceLocation dim, BlockPos pos, int color, String owner,
                       int size, int opacity) {

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

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeUtf(name, 64);
        buf.writeResourceLocation(dim);
        buf.writeBlockPos(pos);
        buf.writeInt(color);
        buf.writeUtf(owner, 64);
        buf.writeVarInt(size);
        buf.writeVarInt(opacity);
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
        return new Waypoint(id, name, dim, pos, color, owner, size, opacity);
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
                opacity);
    }
}
