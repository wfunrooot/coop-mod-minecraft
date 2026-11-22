package com.coopkit.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Только для хоста: кому разрешены читы и ТП. */
public class PermissionsScreen extends Screen {
    private static final int PER_PAGE = 7;
    private static final int ROW = 24;
    private static final int TOP = 40;

    private final Screen parent;
    private int page = 0;
    private List<PlayerInfo> shown = List.of();
    private Set<UUID> seenPerms;
    private Set<UUID> seenTeleports;
    private int seenCount;

    public PermissionsScreen(Screen parent) {
        super(Component.translatable("coopkit.screen.access"));
        this.parent = parent;
    }

    private int left() {
        return this.width / 2 - 150;
    }

    @Override
    protected void init() {
        List<PlayerInfo> all = OnlinePlayers.list(true);
        seenPerms = ClientState.allowedPlayers;
        seenTeleports = ClientState.teleportPlayers;
        seenCount = all.size();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Mth.clamp(page, 0, pages - 1);
        int from = page * PER_PAGE;
        shown = all.subList(from, Math.min(all.size(), from + PER_PAGE));

        int left = left();
        for (int i = 0; i < shown.size(); i++) {
            UUID id = shown.get(i).getProfile().getId();
            boolean allowed = seenPerms.contains(id);
            addRenderableWidget(Button.builder(permissionLabel("coopkit.category.cheats", allowed),
                    b -> ClientNet.setPermission(id, !allowed, false))
                    .bounds(left + 100, TOP + i * ROW, 98, 20).build());
            boolean teleport = seenTeleports.contains(id);
            addRenderableWidget(Button.builder(permissionLabel("coopkit.category.teleport", teleport),
                    b -> ClientNet.setPermission(id, !teleport, true))
                    .bounds(left + 202, TOP + i * ROW, 98, 20).build());
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
        }).bounds(left + 240, by, 20, 20).build());
        next.active = page < pages - 1;

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(this.width / 2 - 50, by, 100, 20).build());
    }

    private static Component permissionLabel(String category, boolean allowed) {
        return Component.translatable(category).append(": ").append(
                Component.translatable(allowed ? "coopkit.on" : "coopkit.off")
                        .withStyle(allowed ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void tick() {
        if (ClientState.allowedPlayers != seenPerms || ClientState.teleportPlayers != seenTeleports || OnlinePlayers.list(true).size() != seenCount) rebuild();
        if (!ClientState.isHost) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("coopkit.access.hint"), this.width / 2, 26, 0xAAAAAA);
        if (shown.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("coopkit.no_players"), this.width / 2, 70, 0xAAAAAA);
        }
        int left = left();
        for (int i = 0; i < shown.size(); i++) {
            PlayerInfo info = shown.get(i);
            int y = TOP + i * ROW;
            PlayerFaceRenderer.draw(g, info.getSkinLocation(), left, y + 2, 16);
            g.drawString(this.font, this.font.plainSubstrByWidth(info.getProfile().getName(), 74), left + 22, y + 6, 0xFFFFFF);
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
