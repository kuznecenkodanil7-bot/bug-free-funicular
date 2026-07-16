package dev.raidmine.stafftool.ui;

import dev.raidmine.stafftool.RaidMineStaffMod;
import dev.raidmine.stafftool.chat.UiNotificationCenter;
import dev.raidmine.stafftool.rules.PunishmentType;
import dev.raidmine.stafftool.stats.SessionStats;
import dev.raidmine.stafftool.util.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudOverlay {
    public static final int BASE_WIDTH = 298;
    public static final int BASE_HEIGHT = 38;
    private static volatile boolean editingInteraction;

    private HudOverlay() {
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HudEditorScreen) return;
        SessionStats stats = renderInternal(context, false);
        if (stats != null) HintSidebarOverlay.render(context, stats);
    }

    public static void renderEditable(DrawContext context) {
        renderInternal(context, true);
    }

    public static void setEditingInteraction(boolean interacting) {
        editingInteraction = interacting;
    }

    private static SessionStats renderInternal(DrawContext context, boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || !RaidMineStaffMod.config().hudEnabled || !AuthManager.canUseMod()) {
            return null;
        }

        SessionStats stats = RaidMineStaffMod.stats();
        Layout l = layout(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        UiNotificationCenter.Notice notice = UiNotificationCenter.top();
        if (notice != null && !editing) renderNoticeBar(context, l, notice);
        else renderStatsBar(context, l, stats, editing);
        return stats;
    }

    private static void renderStatsBar(DrawContext context, Layout l, SessionStats stats, boolean editing) {
        int border = stats.goalReached() ? UiTheme.SUCCESS : UiTheme.accent();
        if (editing && editingInteraction) {
            UiTheme.glow(context, l.x() - 1, l.y() - 1, l.width() + 2, l.height() + 2,
                    Math.max(8, scale(11, l.scale())), border);
        }
        UiTheme.shadow(context, l.x(), l.y(), l.width(), l.height(), Math.max(7, scale(10, l.scale())));
        UiTheme.roundedRect(context, l.x(), l.y(), l.width(), l.height(), Math.max(7, scale(10, l.scale())), border);
        int inset = Math.max(2, scale(2, l.scale()));
        UiTheme.roundedRect(context, l.x() + inset, l.y() + inset, l.width() - inset * 2, l.height() - inset * 2,
                Math.max(6, scale(8, l.scale())), UiTheme.argb(250, 13, 15, 19));

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int logoW = scale(30, l.scale());
        int logoH = scale(30, l.scale());
        int cursor = l.x() + scale(7, l.scale());
        int logoY = l.y() + (l.height() - logoH) / 2;
        UiTheme.logo(context, cursor, logoY, logoW, logoH, 255);
        cursor += logoW + scale(5, l.scale());
        UiTheme.text(context, tr, "RM", cursor, l.y() + scale(13, l.scale()),
                Math.max(8F, 10.2F * l.scale()), UiTheme.TEXT, true);
        cursor += scale(28, l.scale());
        context.fill(cursor, l.y() + scale(7, l.scale()), cursor + 1,
                l.y() + l.height() - scale(7, l.scale()), UiTheme.BORDER);
        cursor += scale(5, l.scale());

        cursor = verticalStat(context, tr, cursor, l, UiIcon.BAN, stats.bans(), UiTheme.DANGER,
                Math.max(stats.pulse(PunishmentType.BAN), stats.pulse(PunishmentType.PERMANENT_BAN)));
        cursor = verticalStat(context, tr, cursor, l, UiIcon.MUTE, stats.mutes(), UiTheme.WARNING,
                stats.pulse(PunishmentType.MUTE));
        cursor = verticalStat(context, tr, cursor, l, UiIcon.WARN, stats.warns(), UiTheme.accent(),
                stats.pulse(PunishmentType.WARN));

        int timeColor = stats.goalReached() ? UiTheme.SUCCESS : UiTheme.TEXT;
        int timeBlockW = scale(67, l.scale());
        UiTheme.roundedRect(context, cursor, l.y() + scale(4, l.scale()), timeBlockW, l.height() - scale(8, l.scale()),
                Math.max(5, scale(7, l.scale())), UiTheme.argb(120, 31, 35, 44));
        int clockSize = scale(12, l.scale());
        UiTheme.icon(context, UiIcon.CLOCK, cursor + scale(6, l.scale()), l.y() + scale(13, l.scale()), clockSize, timeColor);
        UiTheme.text(context, tr, formatTime(stats.elapsedSeconds()), cursor + scale(22, l.scale()),
                l.y() + scale(13, l.scale()), Math.max(8F, 9.2F * l.scale()), timeColor, true);
        cursor += timeBlockW + scale(4, l.scale());

        int eyeSize = scale(18, l.scale());
        int eyeColor = stats.isVanished() ? 0xFFB36BFF : UiTheme.argb(255, 68, 72, 82);
        UiTheme.icon(context, UiIcon.EYE, cursor, l.y() + (l.height() - eyeSize) / 2, eyeSize, eyeColor);

        if (editing) renderHandles(context, l, border);
    }

    private static int verticalStat(DrawContext context, TextRenderer tr, int x, Layout l,
                                    UiIcon icon, int value, int accent, float pulse) {
        int blockW = scale(32, l.scale());
        int blockH = l.height() - scale(8, l.scale());
        int y = l.y() + scale(4, l.scale());
        int bg = pulse > 0F ? UiTheme.withAlpha(accent, 36 + Math.round(70F * pulse)) : UiTheme.argb(112, 31, 35, 44);
        UiTheme.roundedRect(context, x, y, blockW, blockH, Math.max(5, scale(7, l.scale())), bg);
        int iconSize = scale(12, l.scale());
        UiTheme.icon(context, icon, x + (blockW - iconSize) / 2, y + scale(3, l.scale()), iconSize, accent);
        String text = Integer.toString(value);
        int textW = UiTheme.textWidth(text, Math.max(7F, 8.7F * l.scale()), true);
        UiTheme.text(context, tr, text, x + (blockW - textW) / 2, y + scale(17, l.scale()),
                Math.max(7F, 8.7F * l.scale()), UiTheme.TEXT, true);
        return x + blockW + scale(3, l.scale());
    }

    private static void renderNoticeBar(DrawContext context, Layout l, UiNotificationCenter.Notice notice) {
        float p = UiNotificationCenter.progress(notice);
        int width = Math.max(scale(210, l.scale()), Math.round(l.width() * p));
        int x = l.x() + (l.width() - width) / 2;
        int y = l.y() - Math.round((1F - p) * scale(8, l.scale()));
        int alpha = Math.round(245F * p);
        int accent = switch (notice.kind()) {
            case VIOLATION -> UiTheme.accent();
            case MENTION -> 0xFFFFD24A;
            case INFO -> UiTheme.SUCCESS;
        };
        UiTheme.roundedRect(context, x, y, width, l.height(), Math.max(7, scale(10, l.scale())), UiTheme.withAlpha(accent, alpha));
        UiTheme.roundedRect(context, x + 2, y + 2, width - 4, l.height() - 4, Math.max(6, scale(8, l.scale())),
                UiTheme.withAlpha(UiTheme.PANEL_2, alpha));

        int iconSize = scale(15, l.scale());
        int iconX = x + scale(9, l.scale());
        UiTheme.icon(context, notice.kind() == UiNotificationCenter.Kind.INFO ? UiIcon.BELL : UiIcon.WARN,
                iconX, y + (l.height() - iconSize) / 2, iconSize, UiTheme.withAlpha(accent, alpha));
        int textX = iconX + iconSize + scale(7, l.scale());
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer,
                UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.title(), width - (textX - x) - 10),
                textX, y + scale(7, l.scale()), Math.max(8F, 9.3F * l.scale()), UiTheme.withAlpha(UiTheme.TEXT, alpha), true);
        UiTheme.text(context, MinecraftClient.getInstance().textRenderer,
                UiTheme.ellipsize(MinecraftClient.getInstance().textRenderer, notice.message(), width - (textX - x) - 10),
                textX, y + scale(21, l.scale()), Math.max(7F, 7.8F * l.scale()), UiTheme.withAlpha(UiTheme.MUTED, alpha), false);
    }

    private static void renderHandles(DrawContext context, Layout l, int color) {
        for (Handle handle : l.handles()) {
            Rect r = handle.rect();
            UiTheme.roundedRect(context, r.x(), r.y(), r.w(), r.h(), Math.max(3, r.w() / 2), color);
        }
    }

    public static Layout layout(int screenWidth, int screenHeight) {
        float scale = RaidMineStaffMod.config().hudScale;
        int width = Math.max(164, Math.round(BASE_WIDTH * scale));
        int height = Math.max(26, Math.round(BASE_HEIGHT * scale));
        int availableX = Math.max(0, screenWidth - width);
        int availableY = Math.max(0, screenHeight - height);
        int x = Math.round(availableX * RaidMineStaffMod.config().hudX);
        int y = Math.round(availableY * RaidMineStaffMod.config().hudY);
        x = Math.max(0, Math.min(availableX, x));
        y = Math.max(0, Math.min(availableY, y));
        return new Layout(x, y, width, height, scale);
    }

    public static void setPosition(int screenWidth, int screenHeight, int x, int y) {
        Layout current = layout(screenWidth, screenHeight);
        int maxX = Math.max(1, screenWidth - current.width());
        int maxY = Math.max(1, screenHeight - current.height());
        RaidMineStaffMod.config().hudX = Math.max(0, Math.min(maxX, x)) / (float) maxX;
        RaidMineStaffMod.config().hudY = Math.max(0, Math.min(maxY, y)) / (float) maxY;
    }

    public static void nudge(int screenWidth, int screenHeight, int dx, int dy) {
        Layout current = layout(screenWidth, screenHeight);
        setPosition(screenWidth, screenHeight, current.x() + dx, current.y() + dy);
        RaidMineStaffMod.config().save();
    }

    public static void setScale(float scale) {
        RaidMineStaffMod.config().hudScale = Math.max(0.55F, Math.min(1.65F, scale));
    }

    public static void centerTop() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().save();
    }

    public static void reset() {
        RaidMineStaffMod.config().hudX = 0.5F;
        RaidMineStaffMod.config().hudY = 0.015F;
        RaidMineStaffMod.config().hudScale = 0.82F;
        RaidMineStaffMod.config().save();
    }

    private static int scale(int value, float scale) { return Math.max(1, Math.round(value * scale)); }

    private static String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, secs) : String.format("%02d:%02d", minutes, secs);
    }

    public enum Edge { MOVE, N, S, E, W, NE, NW, SE, SW }

    public record Handle(Edge edge, Rect rect) { }

    public record Layout(int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        public Handle[] handles() {
            int size = Math.max(6, Math.round(7 * scale));
            int half = size / 2;
            int cx = x + width / 2;
            int cy = y + height / 2;
            return new Handle[]{
                    new Handle(Edge.NW, new Rect(x - half, y - half, size, size)),
                    new Handle(Edge.N, new Rect(cx - half, y - half, size, size)),
                    new Handle(Edge.NE, new Rect(x + width - half, y - half, size, size)),
                    new Handle(Edge.W, new Rect(x - half, cy - half, size, size)),
                    new Handle(Edge.E, new Rect(x + width - half, cy - half, size, size)),
                    new Handle(Edge.SW, new Rect(x - half, y + height - half, size, size)),
                    new Handle(Edge.S, new Rect(cx - half, y + height - half, size, size)),
                    new Handle(Edge.SE, new Rect(x + width - half, y + height - half, size, size))
            };
        }

        public Edge edgeAt(double mouseX, double mouseY) {
            for (Handle handle : handles()) if (handle.rect().contains(mouseX, mouseY)) return handle.edge();
            return contains(mouseX, mouseY) ? Edge.MOVE : null;
        }
    }

    public record Rect(int x, int y, int w, int h) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        }
    }
}
