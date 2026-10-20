package dev.nova;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Aimbot extends Module {
    final Setting.Num range = num("Range", 6, 1, 100, false);
    final Setting.Num smooth = num("Smoothness", 6, 1, 50, false);
    final Setting.Num maxSpeed = num("Max Speed", 40, 5, 180, true);
    final Setting.Num fov = num("FOV", 90, 10, 360, true);
    final Setting.Num predict = num("Prediction", 0, 0, 5, false);
    final Setting.Mode priority = mode("Priority", 0, "Distance", "Crosshair", "Health");
    final Setting.Mode aimAt = mode("Aim At", 1, "Head", "Chest", "Body");
    final Setting.Bool players = bool("Players", true), hostile = bool("Hostile", true), passive = bool("Passive", false);
    final Setting.Bool visibleOnly = bool("Visible Only", true), onlyAttack = bool("Only While Attacking", false);
    final Setting.Bool lock = bool("Target Lock", true), showFov = bool("Show FOV", true);

    private final Map<Integer, Vec3d> last = new HashMap<>();
    private Entity target;

    public Aimbot() { super("Aimbot", "Smoothly aims at targets inside your FOV.", "Combat", true); }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { last.clear(); target = null; return; }

        boolean active = mc.currentScreen == null && (!onlyAttack.value || mc.options.attackKey.isPressed());
        Vec3d eye = p.getEyePos();
        Set<Integer> seen = new HashSet<>();
        Entity best = null, locked = null;
        Vec3d bestAim = null, lockedAim = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!accepts(e, players, hostile, passive)) continue;
            Vec3d pos = e.getPos();
            Vec3d prev = last.put(e.getId(), pos);
            seen.add(e.getId());
            if (!active) continue;

            Vec3d vel = prev == null ? Vec3d.ZERO : pos.subtract(prev);
            Vec3d aim = aimPoint(e).add(vel.multiply(predict.value));
            double dist = eye.distanceTo(aim);
            if (dist > range.value) continue;
            float[] rot = rotations(eye, aim);
            double ang = Math.hypot(MathHelper.wrapDegrees(rot[0] - p.getYaw()), rot[1] - p.getPitch());
            if (ang > fov.value / 2.0) continue;
            if (visibleOnly.value && !canSee(p, eye, aim)) continue;

            double score = switch (priority.index) {
                case 1 -> ang;
                case 2 -> ((LivingEntity) e).getHealth();
                default -> dist;
            };
            if (e == target) { locked = e; lockedAim = aim; }
            if (score < bestScore) { bestScore = score; best = e; bestAim = aim; }
        }
        last.keySet().retainAll(seen);

        if (!active) { target = null; return; }
        if (lock.value && locked != null) { best = locked; bestAim = lockedAim; }
        target = best;
        if (best == null) return;

        float[] rot = rotations(eye, bestAim);
        float dyaw = MathHelper.wrapDegrees(rot[0] - p.getYaw());
        float dpitch = rot[1] - p.getPitch();
        float s = (float) Math.max(1.0, smooth.value);
        float sy = dyaw / s, sp = dpitch / s;
        double mag = Math.hypot(sy, sp);
        if (mag > maxSpeed.value) { float k = (float) (maxSpeed.value / mag); sy *= k; sp *= k; }
        if (Math.hypot(dyaw, dpitch) < 0.3) { sy = dyaw; sp = dpitch; }
        p.setYaw(p.getYaw() + sy);
        p.setPitch(MathHelper.clamp(p.getPitch() + sp, -90f, 90f));
    }

    @Override
    public void onHud(DrawContext c, float td) {
        if (!showFov.value || fov.value >= 170 || mc.player == null || mc.currentScreen != null) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        double vfov = mc.options.getFov().getValue();
        double r = Math.tan(Math.toRadians(fov.value / 2.0)) / Math.tan(Math.toRadians(vfov / 2.0)) * (sh / 2.0);
        int col = Draw.alpha(Modules.CONFIG.guiColor.get(), 0xB0);
        for (int i = 0; i < 96; i++) {
            double a = i * Math.PI * 2 / 96;
            int x = (int) (sw / 2.0 + Math.cos(a) * r), y = (int) (sh / 2.0 + Math.sin(a) * r);
            c.fill(x, y, x + 2, y + 2, col);
        }
    }

    private Vec3d aimPoint(Entity e) {
        return switch (aimAt.index) {
            case 0 -> e.getEyePos();
            case 1 -> new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.7, e.getZ());
            default -> e.getBoundingBox().getCenter();
        };
    }

    private boolean canSee(ClientPlayerEntity p, Vec3d from, Vec3d to) {
        return mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, p)).getType() == HitResult.Type.MISS;
    }

    private static float[] rotations(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{yaw, pitch};
    }
}
