package com.coopkit.client;

import com.coopkit.waypoints.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class WaypointListScreen extends Screen {
    private static final int PER_PAGE = 7;
    private static final int ROW = 24;
    private static final int TOP = 36;

    private int page = 0;
    private List<Waypoint> shown = List.of();
    private List<Waypoint> seen;
    private boolean seenAllowed;

    public WaypointListScreen() {
        super(Component.translatable("coopkit.screen.waypoints"));
    }

    private int left() {
        return this.width / 2 - 160;
    }

    @Override
    protected void init() {
        seen = ClientState.waypoints;
        seenAllowed = ClientState.teleportAllowed;
        List<Waypoint> all = seen;
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Mth.clamp(page, 0, pages - 1);
        int from = page * PER_PAGE;
        shown = all.subList(from, Math.min(all.size(), from + PER_PAGE));

        int left = left();
        for (int i = 0; i < shown.size(); i++) {
            Waypoint w = shown.get(i);
            int y = TOP + i * ROW;

            boolean hidden = HiddenWaypoints.isHidden(w.id());
            Button eye = addRenderableWidget(Button.builder(Component.literal(hidden ? "§8◎" : "◉"), b -> {
                HiddenWaypoints.toggle(w.id());
                rebuild();
            }).bounds(left + 212, y, 20, 20).build());
            eye.setTooltip(Tooltip.create(Component.translatable(hidden ? "coopkit.show" : "coopkit.hide")));

            if (ClientState.teleportAllowed) {
                addRenderableWidget(Button.builder(Component.translatable("coopkit.tp"), b -> {
                    if (this.minecraft != null) this.minecraft.setScreen(new TeleportScreen(this, w));
                }).bounds(left + 236, y, 48, 20).build());
            }
            Button del = addRenderableWidget(Button.builder(Component.literal("✕"),
                    b -> ClientNet.removeWaypoint(w.id())).bounds(left + 288, y, 32, 20).build());
            del.setTooltip(Tooltip.create(Component.translatable("coopkit.delete")));
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
        }).bounds(left + 300, by, 20, 20).build());
        next.active = page < pages - 1;

        addRenderableWidget(Button.builder(Component.translatable("coopkit.add_here"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(new WaypointAddScreen());
        }).bounds(left + 24, by, 88, 20).build());

        addRenderableWidget(Button.builder(hudLabel(), b -> {
            ClientState.showHud = !ClientState.showHud;
            b.setMessage(hudLabel());
        }).bounds(left + 116, by, 88, 20).build());

        boolean anyVisible = all.stream().anyMatch(w -> !HiddenWaypoints.isHidden(w.id()));
        Button hideAll = addRenderableWidget(Button.builder(
                Component.translatable(anyVisible ? "coopkit.hide_all" : "coopkit.show_all"), b -> {
                    HiddenWaypoints.setAll(all.stream().map(Waypoint::id).toList(), anyVisible);
                    rebuild();
                }).bounds(left + 208, by, 88, 20).build());
        hideAll.active = !all.isEmpty();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 50, by + 26, 100, 20).build());
    }

    private static Component hudLabel() {
        return Component.translatable(ClientState.showHud ? "coopkit.hud.on" : "coopkit.hud.off");
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void tick() {
        if (ClientState.waypoints != seen || ClientState.teleportAllowed != seenAllowed) rebuild();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        if (shown.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("coopkit.empty"), this.width / 2, 60, 0xAAAAAA);
        }

        Player player = this.minecraft != null ? this.minecraft.player : null;
        int left = left();
        for (int i = 0; i < shown.size(); i++) {
            Waypoint w = shown.get(i);
            int y = TOP + i * ROW;
            boolean hidden = HiddenWaypoints.isHidden(w.id());
            int nameColor = hidden ? 0x777777 : 0xFFFFFF;
            g.fill(left, y + 5, left + 10, y + 15, (hidden ? 0x66000000 : 0xFF000000) | w.color());
            g.drawString(this.font, this.font.plainSubstrByWidth(w.name(), 194), left + 14, y + 1, nameColor);

            String coords = w.pos().getX() + " " + w.pos().getY() + " " + w.pos().getZ();
            String where;
            if (player != null && player.level().dimension().location().equals(w.dim())) {
                int dist = (int) Math.sqrt(player.blockPosition().distSqr(w.pos()));
                where = I18n.get("coopkit.dist", dist) + " · " + coords;
            } else {
                where = w.dim().getPath() + " · " + coords;
            }
            g.drawString(this.font, this.font.plainSubstrByWidth(where + " · " + w.owner(), 194),
                    left + 14, y + 11, 0x999999);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
