package dev.nova;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Floating particles confined to a rectangle (the GUI window). Settings come from the Config tab. */
final class ParticleField {
    private static final class P { float x, y, vx, vy, phase; }

    private final List<P> list = new ArrayList<>();
    private final Random rnd = new Random();
    private long last = System.nanoTime();

    private P spawn(int w, int h) {
        P p = new P();
        p.x = rnd.nextFloat() * w; p.y = rnd.nextFloat() * h;
        p.vx = (rnd.nextFloat() - 0.5f) * 0.8f; p.vy = -(0.2f + rnd.nextFloat() * 0.8f);
        p.phase = rnd.nextFloat() * 6.28f;
        return p;
    }

    void render(DrawContext c, int ox, int oy, int w, int h) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1e9f);
        last = now;

        ConfigModule cfg = Modules.CONFIG;
        int target = (int) cfg.particleCount.value, size = (int) cfg.particleSize.value, col = cfg.particleColor.get();
        float speed = (float) cfg.particleSpeed.value * 30f * dt;
        while (list.size() < target) list.add(spawn(w, h));
        while (list.size() > target) list.remove(list.size() - 1);

        double t = now / 4e8;
        for (P p : list) {
            p.x += p.vx * speed; p.y += p.vy * speed;
            if (p.y < 2) { p.y = h - size - 2; p.x = rnd.nextFloat() * (w - size); }
            if (p.x < 2) p.x = w - size - 2; else if (p.x > w - size - 2) p.x = 2;
            int a = (int) (90 + 70 * Math.sin(p.phase + t));
            int x = ox + (int) p.x, y = oy + (int) p.y;
            c.fill(x, y, x + size, y + size, Draw.alpha(col, a));
        }
    }
}
