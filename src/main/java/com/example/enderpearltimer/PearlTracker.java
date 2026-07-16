package com.example.enderpearltimer;

import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class PearlTracker {

    private static final float GRAVITY = 0.03f;
    private static final float DRAG = 0.99f;
    private static final int MAX_SIMULATION_TICKS = 400;

    private static EnderPearlEntity trackedPearl;
    private static Float secondsRemaining;
    private static boolean inBubbleColumn;
    private static int bubbleFlashTicksLeft;
    private static final int BUBBLE_FLASH_DURATION_TICKS = 40;

    private PearlTracker() {
    }

    public static void startTracking(EnderPearlEntity pearl) {
        trackedPearl = pearl;
        secondsRemaining = null;
        inBubbleColumn = false;
        bubbleFlashTicksLeft = 0;
    }

    public static boolean isTracking(Entity entity) {
        return trackedPearl != null && trackedPearl == entity;
    }

    public static void stopTracking() {
        trackedPearl = null;
        secondsRemaining = null;
        inBubbleColumn = false;
        bubbleFlashTicksLeft = 0;
    }

    public static Float getSecondsRemaining() {
        return secondsRemaining;
    }

    public static boolean isShowingBubbleColumnFlash() {
        return inBubbleColumn && bubbleFlashTicksLeft > 0;
    }

    public static boolean isTrackingSomething() {
        return trackedPearl != null;
    }

    public static void tick(MinecraftClient client) {
        if (trackedPearl == null) {
            return;
        }

        if (trackedPearl.isRemoved() || !trackedPearl.isAlive()) {
            stopTracking();
            return;
        }

        World world = trackedPearl.getEntityWorld();
        Vec3d pos = trackedPearl.getEntityPos();

        // Erstkontakt merken. inBubbleColumn wird danach NICHT mehr
        // zurueckgesetzt, auch wenn die Perle durch das Auf-und-Ab-Bouncen
        // kurzzeitig wieder aus dem Blasensaeulen-Block heraus geraet -
        // sonst wuerde der Hinweis bei jedem Wiedereintritt neu starten
        // und nie verschwinden.
        if (!inBubbleColumn && isBubbleColumnAt(world, pos)) {
            inBubbleColumn = true;
            bubbleFlashTicksLeft = BUBBLE_FLASH_DURATION_TICKS;
        }

        if (inBubbleColumn) {
            secondsRemaining = null;

            if (bubbleFlashTicksLeft > 0) {
                bubbleFlashTicksLeft--;
            } else {
                stopTracking();
            }
            return;
        }

        Vec3d velocity = trackedPearl.getVelocity();

        int ticksUntilImpact = simulateFlight(world, pos, velocity);
        secondsRemaining = ticksUntilImpact / 20.0f;
    }

    private static boolean isBubbleColumnAt(World world, Vec3d pos) {
        BlockPos blockPos = BlockPos.ofFloored(pos.x, pos.y, pos.z);
        BlockState state = world.getBlockState(blockPos);
        return state.getBlock() instanceof BubbleColumnBlock;
    }

    private static int simulateFlight(World world, Vec3d startPos, Vec3d startVelocity) {
        Vec3d pos = startPos;
        Vec3d velocity = startVelocity;

        for (int tick = 0; tick < MAX_SIMULATION_TICKS; tick++) {
            Vec3d nextPos = pos.add(velocity);

            BlockHitResult hit = world.raycast(new RaycastContext(
                    pos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    trackedPearl
            ));

            if (hit.getType() != HitResult.Type.MISS) {
                return tick;
            }

            pos = nextPos;
            velocity = velocity.multiply(DRAG, DRAG, DRAG);
            velocity = velocity.add(0.0, -GRAVITY, 0.0);
        }

        return MAX_SIMULATION_TICKS;
    }
}
