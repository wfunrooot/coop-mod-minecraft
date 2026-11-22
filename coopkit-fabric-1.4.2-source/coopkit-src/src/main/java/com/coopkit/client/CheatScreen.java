package com.coopkit.client;

import com.coopkit.cheats.Cheat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CheatScreen extends Screen {
    private final List<Button> cheatButtons = new ArrayList<>();
    private Button accessButton;
    private Button weatherButton;
    private int top;

    public CheatScreen() {
        super(Component.translatable("coopkit.screen.cheats"));
    }

    @Override
    protected void init() {
        cheatButtons.clear();
        Cheat[] all = Cheat.values();
        int w = 150, h = 20, gap = 4, cols = 2;
        int rows = (all.length + cols - 1) / cols;
        int left = (this.width - (cols * w + gap)) / 2;
        top = this.height / 2 - rows * (h + gap) / 2 - 20;

        for (int i = 0; i < all.length; i++) {
            Cheat c = all[i];
            int x = left + (i % cols) * (w + gap);
            int y = top + (i / cols) * (h + gap);
            Button b = Button.builder(label(c), btn -> ClientNet.toggleCheat(c)).bounds(x, y, w, h).build();
            b.active = ClientState.cheatsAllowed || isOn(c);
            cheatButtons.add(addRenderableWidget(b));
        }

        int by = top + rows * (h + gap) + 10;
        weatherButton = addRenderableWidget(Button.builder(Component.translatable("coopkit.screen.weather"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(new WeatherScreen(this));
        }).bounds(left, by, w, 20).build());
        weatherButton.active = ClientState.cheatsAllowed;

        accessButton = addRenderableWidget(Button.builder(Component.translatable("coopkit.access"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(new PermissionsScreen(this));
        }).bounds(left + w + gap, by, w, 20).build());
        accessButton.visible = ClientState.isHost;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 75, by + 26, w, 20).build());
    }

    private static boolean isOn(Cheat c) {
        return (ClientState.cheatMask & c.bit()) != 0;
    }

    private static Component label(Cheat c) {
        Component state = isOn(c)
                ? Component.translatable("coopkit.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("coopkit.off").withStyle(ChatFormatting.RED);
        return Component.translatable("coopkit.cheat." + c.id).append(": ").append(state);
    }

    @Override
    public void tick() {
        Cheat[] all = Cheat.values();
        for (int i = 0; i < cheatButtons.size(); i++) {
            Button b = cheatButtons.get(i);
            b.setMessage(label(all[i]));
            b.active = ClientState.cheatsAllowed || isOn(all[i]);
        }
        if (weatherButton != null) weatherButton.active = ClientState.cheatsAllowed;
        if (accessButton != null && accessButton.visible != ClientState.isHost) {
            clearWidgets();
            init();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, top - 30, 0xFFFFFF);
        if (!ClientState.cheatsAllowed) {
            g.drawCenteredString(this.font, Component.translatable("coopkit.msg.no_access"),
                    this.width / 2, top - 16, 0xFF5555);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
