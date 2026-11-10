package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class WaypointAddScreen extends Screen {
    static final int[] COLORS = {
            0xFF5555, 0x55FF55, 0x5599FF, 0xFFFF55, 0xFF55FF, 0x55FFFF, 0xFFAA00, 0xFFFFFF
    };
    // запоминаем выбор между открытиями
    private static int lastColor = 0;
    private static int lastSize = 100;
    private static int lastOpacity = 100;

    private EditBox nameBox;
    private int top;

    public WaypointAddScreen() {
        super(Component.translatable("coopkit.screen.add_waypoint"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        top = this.height / 2 - 70;

        nameBox = new EditBox(this.font, cx - 100, top + 30, 200, 20, Component.translatable("coopkit.waypoint.name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("coopkit.waypoint.name"));
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(Button.builder(colorLabel(), b -> {
            lastColor = (lastColor + 1) % COLORS.length;
            b.setMessage(colorLabel());
        }).bounds(cx - 100, top + 54, 200, 20).build());

        addRenderableWidget(new PercentSlider(cx - 100, top + 78, "coopkit.size",
                Waypoint.MIN_SIZE, Waypoint.MAX_SIZE, lastSize, v -> lastSize = v));
        addRenderableWidget(new PercentSlider(cx - 100, top + 102, "coopkit.opacity",
                Waypoint.MIN_OPACITY, Waypoint.MAX_OPACITY, lastOpacity, v -> lastOpacity = v));

        addRenderableWidget(Button.builder(Component.translatable("coopkit.save"), b -> save())
                .bounds(cx - 100, top + 132, 98, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, top + 132, 98, 20).build());
    }

    private static Component colorLabel() {
        int color = COLORS[lastColor];
        return Component.translatable("coopkit.color").append(": ")
                .append(Component.literal("■■■■").withStyle(s -> s.withColor(color)));
    }

    private void save() {
        ClientNet.addWaypoint(nameBox.getValue(), COLORS[lastColor], lastSize, lastOpacity);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, top, 0xFFFFFF);
        if (this.minecraft != null && this.minecraft.player != null) {
            BlockPos p = this.minecraft.player.blockPosition();
            g.drawCenteredString(this.font, Component.translatable("coopkit.coords", p.getX(), p.getY(), p.getZ()),
                    cx, top + 14, 0xAAAAAA);
        }
        super.render(g, mouseX, mouseY, partialTick);

        // предпросмотр метки справа
        int px = cx + 140, py = top + 80;
        float scale = lastSize / 100F;
        int alpha = Math.round(lastOpacity * 2.55F);
        String name = nameBox.getValue().isBlank() ? "◆" : "◆ " + nameBox.getValue();
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().scale(scale, scale, 1F);
        int w = this.font.width(name);
        g.fill(-w / 2 - 2, -2, w / 2 + 2, 10, (Math.round(alpha * 0.33F) << 24));
        g.drawString(this.font, name, -w / 2, 0, (alpha << 24) | COLORS[lastColor], false);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Ползунок в процентах с шагом 5%. */
    private static class PercentSlider extends AbstractSliderButton {
        private final String key;
        private final int min, max;
        private final java.util.function.IntConsumer onChange;

        PercentSlider(int x, int y, String key, int min, int max, int value, java.util.function.IntConsumer onChange) {
            super(x, y, 200, 20, Component.empty(), (double) (value - min) / (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int current() {
            int v = (int) Math.round(min + this.value * (max - min));
            return Mth.clamp(Math.round(v / 5F) * 5, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(key).append(": " + current() + "%"));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }
}
