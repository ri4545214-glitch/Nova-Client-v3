package dev.nova;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

/** Attacks whatever is under your crosshair as soon as the weapon cooldown is ready. */
public final class AutoAttack extends Module {
    final Setting.Num minCooldown = num("Min Cooldown", 1.0, 0.5, 1.0, false);
    final Setting.Num extraDelay = num("Extra Delay (ticks)", 0, 0, 10, true);
    final Setting.Bool onlyCrits = bool("Only Crits", false), pauseUsing = bool("Pause While Using Item", true);
    final Setting.Bool players = bool("Players", true), hostile = bool("Hostile", true), passive = bool("Passive", false);

    private int ready;

    public AutoAttack() { super("AutoAttack", "Hits your crosshair target when the cooldown is ready.", "Combat", true); }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.currentScreen != null) return;
        if (pauseUsing.value && p.isUsingItem()) return;

        Entity t = mc.targetedEntity;
        if (t == null || !accepts(t, players, hostile, passive)) { ready = 0; return; }
        if (p.getAttackCooldownProgress(0.5f) < minCooldown.value) { ready = 0; return; }
        if (onlyCrits.value && !(p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater())) return;
        if (ready++ < extraDelay.value) return;

        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        ready = 0;
    }
}
