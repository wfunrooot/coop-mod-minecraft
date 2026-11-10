package com.coopkit.client;

import com.coopkit.world.WorldControl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Погода и время суток. */
public class WeatherScreen extends Screen {
    private final Screen parent;
    private EditBox timeBox;
    private int top;

    public WeatherScreen(Screen parent) {
        super(Component.translatable("coopkit.screen.weather"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 74, gap = 4;
        int left = this.width / 2 - (4 * w + 3 * gap) / 2;
        top = this.height / 2 - 60;

        // погода
        int[] types = {WorldControl.WEATHER_CLEAR, WorldControl.WEATHER_RAIN, WorldControl.WEATHER_THUNDER};
        String[] keys = {"coopkit.weather.clear", "coopkit.weather.rain", "coopkit.weather.thunder"};
        int ww = (4 * w + 3 * gap - 2 * gap) / 3;
        for (int i = 0; i < 3; i++) {
            int type = types[i];
            addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> ClientNet.setWeather(type))
                    .bounds(left + i * (ww + gap), top + 12, ww, 20).build());
        }

        // время суток
        String[] tkeys = {"coopkit.time.morning", "coopkit.time.day", "coopkit.time.evening", "coopkit.time.night"};
        int[] ticks = {1000, 6000, 12000, 18000};
        for (int i = 0; i < 4; i++) {
            int t = ticks[i];
            addRenderableWidget(Button.builder(Component.translatable(tkeys[i]), b -> ClientNet.setTime(t))
                    .bounds(left + i * (w + gap), top + 54, w, 20).build());
        }

        // точное время
        timeBox = new EditBox(this.font, left, top + 96, 2 * w + gap, 20, Component.translatable("coopkit.time.exact"));
        timeBox.setMaxLength(5);
        timeBox.setFilter(s -> s.isEmpty() || s.matches("\\d{1,5}"));
        timeBox.setHint(Component.literal("0 – 23999"));
        addRenderableWidget(timeBox);
        addRenderableWidget(Button.builder(Component.translatable("coopkit.time.set"), b -> applyExact())
                .bounds(left + 2 * (w + gap), top + 96, 2 * w + gap, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(this.width / 2 - 50, top + 130, 100, 20).build());
    }

    private void applyExact() {
        String v = timeBox.getValue();
        if (v.isEmpty()) return;
        int t = Integer.parseInt(v);
        if (t > 23999) {
            timeBox.setTextColor(0xFF5555);
            return;
        }
        timeBox.setTextColor(0xE0E0E0);
        ClientNet.setTime(t);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (timeBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applyExact();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (!ClientState.cheatsAllowed) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, top - 24, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("coopkit.weather"), cx, top, 0xAAAAAA);
        g.drawCenteredString(this.font, Component.translatable("coopkit.time"), cx, top + 42, 0xAAAAAA);
        String now = "";
        if (this.minecraft != null && this.minecraft.level != null) {
            now = " (" + (this.minecraft.level.getDayTime() % 24000L) + ")";
        }
        g.drawCenteredString(this.font, Component.translatable("coopkit.time.exact").append(now), cx, top + 84, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
