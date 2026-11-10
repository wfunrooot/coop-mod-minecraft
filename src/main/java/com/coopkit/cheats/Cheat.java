package com.coopkit.cheats;

public enum Cheat {
    // номер бита фиксирован, чтобы старые сохранения не перепутались
    FLY("fly", 0),
    GOD("god", 1),
    NO_FALL("nofall", 2),
    NIGHT_VISION("nightvision", 3),
    WATER_BREATHING("water", 7);

    public final String id;
    private final int bitIndex;

    Cheat(String id, int bitIndex) {
        this.id = id;
        this.bitIndex = bitIndex;
    }

    public int bit() {
        return 1 << bitIndex;
    }

    /** Все биты, которые сейчас что-то значат. */
    public static int validMask() {
        int m = 0;
        for (Cheat c : values()) m |= c.bit();
        return m;
    }
}
