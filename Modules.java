package dev.nova;

import java.util.List;

public final class Modules {
    public static final Aimbot AIMBOT = new Aimbot();
    public static final AutoAttack AUTO_ATTACK = new AutoAttack();
    public static final AnchorMacro ANCHOR_MACRO = new AnchorMacro();
    public static final Esp ESP = new Esp();
    public static final Fullbright FULLBRIGHT = new Fullbright();
    public static final Overlay OVERLAY = new Overlay();
    public static final Sprint SPRINT = new Sprint();
    public static final ConfigModule CONFIG = new ConfigModule();
    /** Order matters: modules of the same category must be next to each other. */
    public static final List<Module> ALL = List.of(AIMBOT, AUTO_ATTACK, ANCHOR_MACRO, ESP, FULLBRIGHT, OVERLAY, SPRINT, CONFIG);

    private Modules() {}
}
