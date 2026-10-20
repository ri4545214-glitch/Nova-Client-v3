package dev.nova;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class ClickGuiScreen extends Screen {
    private static final int W = 510, H = 318, SIDE = 128, TITLE = 32, PAD = 10;
    private static final int ROW_H = 22, ROW_PITCH = 25, ROWS = 9;
    private static final int COL_W = (W - SIDE - PAD * 2 - 8) / 2;
    private static final int TRACK_PAD = 6;

    private final ParticleField particles = new ParticleField();
    private Module selected = Modules.ALL.get(0);
    private Setting.Num dragging;
    private int dragIndex, x0, y0;

    public ClickGuiScreen() { super(Text.literal("Nova")); }

    @Override protected void init() { x0 = (width - W) / 2; y0 = (height - H) / 2; }
    @Override public boolean shouldPause() { return false; }

    // ---- layout ----
    private int paneX() { return x0 + SIDE + PAD; }
    private int paneRight() { return paneX() + 2 * COL_W + 8; }
    private int rowX(int i) { return paneX() + (i / ROWS) * (COL_W + 8); }
    private int rowY(int i) { return y0 + TITLE + 54 + (i % ROWS) * ROW_PITCH; }
    private boolean newCategory(int i) { return i == 0 || !Modules.ALL.get(i).category.equals(Modules.ALL.get(i - 1).category); }
    private int sideY(int idx) {
        int y = y0 + TITLE + 8;
        for (int k = 0; k <= idx; k++) {
            if (newCategory(k)) y += 14;
            if (k == idx) return y;
            y += 22;
        }
        return y;
    }
    private int switchX() { return paneRight() - 22; }
    private int switchY() { return y0 + TITLE + 12; }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    // ---- render ----
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        int accent = Modules.CONFIG.guiColor.get();
        c.fill(0, 0, width, height, 0x99000000);

        Draw.rbox(c, x0 - 1, y0 - 1, W + 2, H + 2, Draw.alpha(accent, 0xB0));
        Draw.rbox(c, x0, y0, W, H, 0xF2101016);
        c.fill(x0 + 1, y0 + TITLE, x0 + SIDE, y0 + H - 1, 0xFF0B0B10);

        // particles live inside the window only
        particles.render(c, x0, y0, W, H);

        // title bar
        c.fillGradient(x0 + 1, y0 + 1, x0 + W - 1, y0 + TITLE, Draw.alpha(accent, 0xB0), Draw.alpha(accent, 0x18));
        c.drawText(textRenderer, Text.literal("NOVA").formatted(Formatting.BOLD), x0 + 12, y0 + 12, 0xFFFFFFFF, true);
        c.drawText(textRenderer, "client", x0 + 12 + textRenderer.getWidth("NOVA") + 4, y0 + 12, 0xFFC8C8D4, false);
        String hint = "RShift: close   |   Right-click module: toggle";
        c.drawText(textRenderer, hint, x0 + W - 10 - textRenderer.getWidth(hint), y0 + 12, 0xFFD8D8E4, false);
        c.fill(x0 + 1, y0 + TITLE, x0 + W - 1, y0 + TITLE + 1, accent);

        // sidebar
        for (int i = 0; i < Modules.ALL.size(); i++) {
            Module m = Modules.ALL.get(i);
            int sy = sideY(i);
            if (newCategory(i)) c.drawText(textRenderer, m.category.toUpperCase(), x0 + 10, sy - 11, 0xFF6C6C7C, false);
            boolean hover = in(mx, my, x0 + 6, sy, SIDE - 12, 20);
            if (m == selected) {
                Draw.rbox(c, x0 + 6, sy, SIDE - 12, 20, Draw.alpha(accent, 0x55));
                c.fill(x0 + 6, sy + 3, x0 + 9, sy + 17, accent);
            } else if (hover) {
                Draw.rbox(c, x0 + 6, sy, SIDE - 12, 20, 0x25FFFFFF);
            }
            c.drawText(textRenderer, m.name, x0 + 16, sy + 6, m.enabled || !m.toggleable ? 0xFFFFFFFF : 0xFFA0A0B0, false);
            if (m.toggleable) Draw.rbox(c, x0 + SIDE - 24, sy + 6, 12, 8, m.enabled ? 0xFF3DDC84 : 0xFF3A3A46);
        }

        // header
        c.drawText(textRenderer, Text.literal(selected.name).formatted(Formatting.BOLD), paneX(), y0 + TITLE + 12, accent, true);
        c.drawText(textRenderer, selected.description, paneX(), y0 + TITLE + 28, 0xFF8E8E9E, false);
        if (selected.toggleable) {
            String l = selected.enabled ? "Enabled" : "Disabled";
            c.drawText(textRenderer, l, switchX() - 6 - textRenderer.getWidth(l), switchY() + 2, 0xFFFFFFFF, false);
            Draw.toggle(c, switchX(), switchY(), selected.enabled, accent);
        }
        c.fill(paneX(), y0 + TITLE + 46, paneRight(), y0 + TITLE + 47, 0x30FFFFFF);

        // settings
        for (int i = 0; i < selected.settings.size(); i++) {
            Setting s = selected.settings.get(i);
            int rx = rowX(i), ry = rowY(i);
            boolean hover = in(mx, my, rx, ry, COL_W, ROW_H);
            Draw.rbox(c, rx, ry, COL_W, ROW_H, hover ? 0xFF22222C : 0xFF17171E);

            if (s instanceof Setting.Num n) {
                double t = (n.value - n.min) / (n.max - n.min);
                int tw = COL_W - TRACK_PAD * 2, fill = (int) (tw * t);
                int col = n.tint != 0 ? (0xFF000000 | n.tint) : accent;
                String v = n.integer ? String.valueOf((int) Math.round(n.value)) : String.format("%.1f", n.value);
                c.drawText(textRenderer, n.name, rx + TRACK_PAD, ry + 3, 0xFFFFFFFF, false);
                c.drawText(textRenderer, v, rx + COL_W - TRACK_PAD - textRenderer.getWidth(v), ry + 3, 0xFFC8C8D4, false);
                c.fill(rx + TRACK_PAD, ry + 16, rx + TRACK_PAD + tw, ry + 18, 0xFF34343F);
                c.fill(rx + TRACK_PAD, ry + 16, rx + TRACK_PAD + fill, ry + 18, col);
                Draw.rbox(c, rx + TRACK_PAD + fill - 2, ry + 14, 5, 6, 0xFFFFFFFF);
            } else if (s instanceof Setting.Bool b) {
                c.drawText(textRenderer, b.name, rx + TRACK_PAD, ry + 7, 0xFFFFFFFF, false);
                Draw.toggle(c, rx + COL_W - 22 - TRACK_PAD, ry + 5, b.value, accent);
            } else if (s instanceof Setting.Mode md) {
                c.drawText(textRenderer, md.name, rx + TRACK_PAD, ry + 7, 0xFFFFFFFF, false);
                String v = "< " + md.get() + " >";
                c.drawText(textRenderer, v, rx + COL_W - TRACK_PAD - textRenderer.getWidth(v), ry + 7, accent, false);
            }
        }
    }

    // ---- input ----
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        if (btn > 1) return super.mouseClicked(click, doubled);

        for (int i = 0; i < Modules.ALL.size(); i++) {
            if (!in(mx, my, x0 + 6, sideY(i), SIDE - 12, 20)) continue;
            Module m = Modules.ALL.get(i);
            if (btn == 1 && m.toggleable) { m.setEnabled(!m.enabled); ConfigIO.save(); }
            else selected = m;
            return true;
        }
        if (btn != 0) return super.mouseClicked(click, doubled);

        if (selected.toggleable && in(mx, my, switchX() - 60, switchY() - 2, 82, 15)) {
            selected.setEnabled(!selected.enabled);
            ConfigIO.save();
            return true;
        }
        for (int i = 0; i < selected.settings.size(); i++) {
            if (!in(mx, my, rowX(i), rowY(i), COL_W, ROW_H)) continue;
            Setting s = selected.settings.get(i);
            if (s instanceof Setting.Bool b) { b.value = !b.value; ConfigIO.save(); }
            else if (s instanceof Setting.Mode md) { md.cycle(1); ConfigIO.save(); }
            else if (s instanceof Setting.Num n) { dragging = n; dragIndex = i; applyDrag(mx); }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) { applyDrag(click.x()); return true; }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) { dragging = null; ConfigIO.save(); return true; }
        return super.mouseReleased(click);
    }

    private void applyDrag(double mx) {
        double t = MathHelper.clamp((mx - (rowX(dragIndex) + TRACK_PAD)) / (COL_W - TRACK_PAD * 2), 0, 1);
        double v = dragging.min + (dragging.max - dragging.min) * t;
        dragging.value = dragging.integer ? Math.round(v) : Math.round(v * 10) / 10.0;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override public void removed() { ConfigIO.save(); }
}
