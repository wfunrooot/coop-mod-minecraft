package com.coopkit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public class CoopKitClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(Keys.CHEATS);
        KeyBindingHelper.registerKeyBinding(Keys.ADD_WAYPOINT);
        KeyBindingHelper.registerKeyBinding(Keys.WAYPOINTS);
        KeyBindingHelper.registerKeyBinding(Keys.BHOP);

        ClientNet.register();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ClientBhop.tick(mc.player);
            if (mc.player == null || mc.screen != null) return;
            while (Keys.BHOP.consumeClick()) ClientBhop.toggle(mc.player);
            while (Keys.CHEATS.consumeClick()) mc.setScreen(new CheatScreen());
            while (Keys.ADD_WAYPOINT.consumeClick()) mc.setScreen(new WaypointAddScreen());
            while (Keys.WAYPOINTS.consumeClick()) mc.setScreen(new WaypointListScreen());
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            WaypointHud.render(graphics, mc.getWindow().getGuiScaledWidth());
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx ->
                WaypointWorldRenderer.render(ctx.matrixStack(), ctx.camera()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientState.reset();
            ClientBhop.reset();
        });
    }
}
