package com.coopkit.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final String CATEGORY = "key.categories.coopkit";

    public static final KeyMapping CHEATS =
            new KeyMapping("key.coopkit.cheats", GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping ADD_WAYPOINT =
            new KeyMapping("key.coopkit.add_waypoint", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping WAYPOINTS =
            new KeyMapping("key.coopkit.waypoints", GLFW.GLFW_KEY_N, CATEGORY);

    private Keys() {}
}
