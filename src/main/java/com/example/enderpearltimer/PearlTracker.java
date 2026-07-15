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

/**
 * Trackt eine vom Spieler geworfene Enderperle und berechnet jeden Tick
 * neu, in wie vielen Ticks sie voraussichtlich einschlaegt.
 *
 * Die Simulation nutzt die vanilla-typischen Bewegungswerte einer
 * Enderperle (Schwerkraft 0.03 Bloecke/Tick^2, Luftwiderstand 0.99)
 * und rasterisiert die Flugbahn gegen die tatsaechliche Blockkollision
 * der Welt. Da jeden Tick mit der echten, vom Server synchronisierten
 * Position/Geschwindigkeit neu gerechnet wird, gleicht sich die
 * Vorhersage automatisch an kleine Abweichungen an und wird kurz vor
 * dem Einschlag praktisch exakt.
 *
 * Befindet sich die Perle in einer Blasensaeule (Wasser + Seelensand/Magma),
 * wird die normale Fallsimulation ausgesetzt, da diese Schwerkraft+Drag
 * allein nicht abbildet. Stattdessen wird das nur angezeigt, ohne eine
 * (falsche) Sekundenzahl zu erfinden.
 */
public final class PearlTracker {

    private static final float GRAVITY = 0.03f;
    private static final float DRAG = 0.99f;
    // Sicherheitsgrenze, damit die Simulation nie endlos laeuft (20s)
    private static final int MAX_SIMULATION_TICKS = 400;

    private static EnderPearlEntity trackedPearl;
    private static Float secondsRemaining;
    private static boolean inBubbleColumn;
    private static int bubbleFlashTicksLeft;
    // Wie lange "Blasensäule" angezeigt wird, bevor der Timer ganz verschwindet (2s)
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

    /**
     * Verbleibende Zeit bis zum voraussichtlichen Einschlag in Sekunden,
     * oder null, wenn aktuell keine Perle getrackt wird oder gerade der
     * Blasensaeulen-Hinweis angezeigt wird (siehe isShowingBubbleColumnFlash()).
     */
    public static Float getSecondsRemaining() {
        return secondsRemaining;
    }

    /**
     * Ob gerade kurz "Blasensäule" angezeigt werden soll. Danach wird
     * das Tracking automatisch komplett beendet (Timer verschwindet ganz),
     * da die Fallsimulation Blasensaeulen nicht nachbilden kann.
     */
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
            // Perle wurde entfernt -> ist eingeschlagen (Teleport passiert)
            stopTracking();
            return;
        }

        World world = trackedPearl.getEntityWorld();
        Vec3d pos = trackedPearl.getEntityPos();

        if (isBubbleColumnAt(world, pos)) {
            if (!inBubbleColumn) {
                // Gerade erst reingeflogen -> kurzen Hinweis starten
                inBubbleColumn = true;
                bubbleFlashTicksLeft = BUBBLE_FLASH_DURATION_TICKS;
            }
            secondsRemaining = null;

            if (bubbleFlashTicksLeft > 0) {
                bubbleFlashTicksLeft--;
            } else {
                // Hinweis fertig angezeigt -> Timer komplett verschwinden lassen
                stopTracking();
            }
            return;
        }
        inBubbleColumn = false;

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
            // Luftwiderstand zuerst, danach Schwerkraft - wie bei ProjectileEntity#tick
            velocity = velocity.multiply(DRAG, DRAG, DRAG);
            velocity = velocity.add(0.0, -GRAVITY, 0.0);
        }

        return MAX_SIMULATION_TICKS;
    }
}
