package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/** Выбор получателя метки или игрока для телепортации. */
public class TeleportScreen extends Screen {
    private static final int PER_PAGE = 6;
    private static final int ROW = 24;
    private static final int TOP = 42;

    private final Screen parent;
    private Waypoint waypoint;
    private final boolean sharing;
    private List<PlayerInfo> seenPlayers = List.of();
    private int page = 0;
    private List<PlayerInfo> shown = List.of();

    public TeleportScreen(Screen parent, Waypoint waypoint, boolean sharing) {
        super(Component.translatable(sharing ? "coopkit.screen.share" : "coopkit.screen.teleport", waypoint.name()));
        this.parent = parent;
        this.waypoint = waypoint;
        this.sharing = sharing;
    }

    private int left() {
        return this.width / 2 - 110;
    }

    @Override
    protected void init() {
        int left = left();
        seenPlayers = OnlinePlayers.list(true);
        List<PlayerInfo> all = seenPlayers.stream().filter(info ->
                waypoint.visibleTo(info.getProfile().getId(), info.getProfile().getName()) != sharing).toList();
        if (!sharing && !all.isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("coopkit.tp.all"), b -> send(2, null))
                    .bounds(left + 56, 24, 108, 16).build());
        }
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Mth.clamp(page, 0, pages - 1);
        int from = page * PER_PAGE;
        shown = all.subList(from, Math.min(all.size(), from + PER_PAGE));
        for (int i = 0; i < shown.size(); i++) {
            PlayerInfo info = shown.get(i);
            addRenderableWidget(Button.builder(Component.literal(info.getProfile().getName()),
                            b -> send(1, info.getProfile().getId()))
                    .bounds(left + 22, TOP + i * ROW, 198, 20).build());
        }

        int by = TOP + PER_PAGE * ROW + 6;
        Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--;
            rebuild();
        }).bounds(left, by, 20, 20).build());
        prev.active = page > 0;
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            rebuild();
        }).bounds(left + 200, by, 20, 20).build());
        next.active = page < pages - 1;
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(this.width / 2 - 50, by, 100, 20).build());
    }

    private void send(int mode, java.util.UUID target) {
        if (sharing) {
            ClientNet.shareWaypoint(waypoint.id(), target);
            return;
        }
        ClientNet.teleport(waypoint.id(), mode, target);
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void tick() {
        Waypoint current = ClientState.waypoints.stream().filter(w -> w.id() == waypoint.id()).findFirst().orElse(null);
        if (current == null || (!sharing && !ClientState.teleportAllowed)) {
            onClose();
            return;
        }
        if (current != waypoint || !seenPlayers.equals(OnlinePlayers.list(true))) {
            waypoint = current;
            rebuild();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        if (shown.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("coopkit.no_players"), this.width / 2, 65, 0xAAAAAA);
        }
        int left = left();
        for (int i = 0; i < shown.size(); i++) {
            PlayerFaceRenderer.draw(g, shown.get(i).getSkinLocation(), left, TOP + i * ROW + 2, 16);
        }
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
