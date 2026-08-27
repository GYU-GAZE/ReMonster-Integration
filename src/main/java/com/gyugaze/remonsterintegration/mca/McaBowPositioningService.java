package com.gyugaze.remonsterintegration.mca;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Small compatibility version of MCA's ArcherMovementTask for ordinary civilians that only gain a
 * bow because Re:Monster projects one from their inventory. Native MCA guards/archers keep their own
 * richer ArcherMovementTask (emergency flee, kite and side-strafe) untouched.
 */
public final class McaBowPositioningService {
    private static final String STATE_KEY = "remonster_mca_bow_spacing_state";
    private static final String NEXT_REPATH_KEY = "remonster_mca_bow_next_repath";

    // Match MCA 1.21.1 ArcherMovementTask's distance hysteresis.
    private static final double EMERGENCY_ENTER_DISTANCE_SQUARED = 12.25D; // 3.5 blocks
    private static final double EMERGENCY_EXIT_DISTANCE_SQUARED = 25.0D;   // 5 blocks
    private static final double KITE_ENTER_DISTANCE_SQUARED = 36.0D;       // 6 blocks
    private static final double KITE_EXIT_DISTANCE_SQUARED = 81.0D;        // 9 blocks
    private static final int AWAY_HORIZONTAL_DISTANCE = 12;
    private static final int AWAY_VERTICAL_DISTANCE = 5;
    private static final int AWAY_PATH_ATTEMPTS = 8;
    private static final double MIN_USEFUL_DISTANCE_GAIN = 0.5D;

    private McaBowPositioningService() {
    }

    public static Positioning update(Mob shooter, LivingEntity target, double attackRange) {
        double distanceSquared = shooter.distanceToSqr(target);
        Positioning previous = readState(shooter);

        double emergencyThreshold = previous == Positioning.EMERGENCY_FLEE
                ? EMERGENCY_EXIT_DISTANCE_SQUARED
                : EMERGENCY_ENTER_DISTANCE_SQUARED;
        if (distanceSquared < emergencyThreshold) {
            setState(shooter, Positioning.EMERGENCY_FLEE);
            moveAway(shooter, target, 0.9D, 10);
            return Positioning.EMERGENCY_FLEE;
        }

        double kiteThreshold = previous == Positioning.EMERGENCY_FLEE || previous == Positioning.KITE
                ? KITE_EXIT_DISTANCE_SQUARED
                : KITE_ENTER_DISTANCE_SQUARED;
        if (distanceSquared < kiteThreshold) {
            setState(shooter, Positioning.KITE);
            moveAway(shooter, target, 0.85D, 16);
            return Positioning.KITE;
        }

        boolean visible = shooter.getSensing().hasLineOfSight(target);
        if (!visible || distanceSquared > attackRange * attackRange) {
            setState(shooter, Positioning.APPROACH);
            shooter.getNavigation().moveTo(target, 0.5D);
            return Positioning.APPROACH;
        }

        setState(shooter, Positioning.HOLD);
        shooter.getNavigation().stop();
        return Positioning.HOLD;
    }

    public static void reset(Mob shooter) {
        shooter.getPersistentData().remove(STATE_KEY);
        shooter.getPersistentData().remove(NEXT_REPATH_KEY);
    }

    private static void moveAway(Mob shooter, LivingEntity threat, double speed, int repathTicks) {
        long gameTime = shooter.level().getGameTime();
        long nextRepath = shooter.getPersistentData().getLong(NEXT_REPATH_KEY);
        if (!shooter.getNavigation().isDone() && gameTime < nextRepath) {
            return;
        }

        double currentDistanceSquared = shooter.distanceToSqr(threat);
        Vec3 best = null;
        double bestDistanceSquared = currentDistanceSquared;

        for (int attempt = 0; attempt < AWAY_PATH_ATTEMPTS; attempt++) {
            Vec3 candidate = LandRandomPos.getPosAway(
                    shooter,
                    AWAY_HORIZONTAL_DISTANCE,
                    AWAY_VERTICAL_DISTANCE,
                    threat.position()
            );
            if (candidate == null) {
                continue;
            }

            double candidateDistanceSquared = candidate.distanceToSqr(threat.position());
            if (candidateDistanceSquared <= currentDistanceSquared + MIN_USEFUL_DISTANCE_GAIN) {
                continue;
            }
            if (candidateDistanceSquared > bestDistanceSquared) {
                bestDistanceSquared = candidateDistanceSquared;
                best = candidate;
            }
        }

        if (best != null) {
            shooter.getNavigation().moveTo(best.x, best.y, best.z, speed);
            shooter.getPersistentData().putLong(NEXT_REPATH_KEY, gameTime + repathTicks);
        } else {
            shooter.getNavigation().stop();
            shooter.getPersistentData().putLong(NEXT_REPATH_KEY, gameTime + 4L);
        }
    }

    private static Positioning readState(Mob shooter) {
        String raw = shooter.getPersistentData().getString(STATE_KEY);
        if (raw.isEmpty()) {
            return Positioning.HOLD;
        }
        try {
            return Positioning.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return Positioning.HOLD;
        }
    }

    private static void setState(Mob shooter, Positioning state) {
        Positioning previous = readState(shooter);
        if (previous != state) {
            shooter.getPersistentData().putString(STATE_KEY, state.name());
            shooter.getPersistentData().remove(NEXT_REPATH_KEY);
        }
    }

    public enum Positioning {
        APPROACH,
        EMERGENCY_FLEE,
        KITE,
        HOLD
    }
}
