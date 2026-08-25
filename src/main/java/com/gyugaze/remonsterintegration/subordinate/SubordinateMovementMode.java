package com.gyugaze.remonsterintegration.subordinate;

/**
 * Cross-mod movement vocabulary used by Re:Monster subordinate adapters.
 *
 * Each integration is responsible for translating these semantic states into the native AI/state
 * system of the entity instead of replacing that system wholesale.
 */
public enum SubordinateMovementMode {
    FOLLOW,
    WANDER,
    STAY;

    public SubordinateMovementMode next() {
        return switch (this) {
            case FOLLOW -> WANDER;
            case WANDER -> STAY;
            case STAY -> FOLLOW;
        };
    }
}
