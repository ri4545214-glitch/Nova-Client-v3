package dev.nova;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Hold right-click with a Respawn Anchor aimed at a block:
 * the anchor is placed, charged with glowstone, then detonated with the item in the Explode Slot.
 * Safe Distance refuses to detonate when you are closer than that to the anchor.
 */
public final class AnchorMacro extends Module {
    final Setting.Num charges = num("Charges", 1, 1, 4, true);
    final Setting.Num delay = num("Delay (ticks)", 1, 0, 10, true);
    final Setting.Num explodeSlot = num("Explode Slot", 3, 1, 9, true);
    final Setting.Num safeDistance = num("Safe Distance", 0, 0, 12, true);

    private enum State { IDLE, PLACED, CHARGE, EXPLODE, DONE }
    private State state = State.IDLE;
    private BlockPos anchor;
    private int wait, timeout, startSlot;

    public AnchorMacro() { super("Anchor Macro", "Place, charge and detonate a Respawn Anchor.", "Combat", true); }

    @Override public void onDisable() { state = State.IDLE; }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        switch (state) {
            case IDLE -> {
                if (mc.currentScreen != null || !mc.options.useKey.isPressed()) return;
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) return;
                if (mc.world.getDimension().respawnAnchorWorks()) return; // Nether: anchors don't explode
                if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
                anchor = hit.getBlockPos().offset(hit.getSide());
                startSlot = mc.player.getInventory().getSelectedSlot();
                timeout = 0;
                state = State.PLACED; // vanilla right-click places the anchor
            }
            case PLACED -> {
                if (mc.world.getBlockState(anchor).isOf(Blocks.RESPAWN_ANCHOR)) { state = State.CHARGE; wait = (int) delay.value; }
                else if (++timeout > 10) state = State.DONE;
            }
            case CHARGE -> {
                if (wait-- > 0) return;
                BlockState st = mc.world.getBlockState(anchor);
                if (!st.isOf(Blocks.RESPAWN_ANCHOR)) { state = State.DONE; return; }
                if (st.get(RespawnAnchorBlock.CHARGES) >= charges.value) { state = State.EXPLODE; wait = (int) delay.value; return; }
                int slot = find(Items.GLOWSTONE);
                if (slot < 0) { say("No glowstone in hotbar"); state = State.DONE; return; }
                select(slot);
                use();
                wait = (int) delay.value;
            }
            case EXPLODE -> {
                if (wait-- > 0) return;
                if (safeDistance.value > 0 && mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(anchor)) < safeDistance.value) {
                    say("Too close - not detonating");
                } else {
                    select((int) explodeSlot.value - 1);
                    use();
                }
                state = State.DONE;
            }
            case DONE -> {
                select(startSlot);
                if (!mc.options.useKey.isPressed()) state = State.IDLE;
            }
        }
    }

    private int find(Item item) {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private void select(int slot) { mc.player.getInventory().setSelectedSlot(slot); }

    private void use() {
        BlockHitResult r = new BlockHitResult(Vec3d.ofCenter(anchor), Direction.UP, anchor, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, r);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void say(String s) { mc.player.sendMessage(Text.literal("[Nova] " + s), true); }
}
