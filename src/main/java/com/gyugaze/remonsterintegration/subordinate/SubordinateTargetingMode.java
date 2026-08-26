package com.gyugaze.remonsterintegration.subordinate;

/**
 * Cross-mod targeting vocabulary matching Tensura's native ISubordinate behaviour ids.
 * Movement and targeting are deliberately separate axes: FOLLOW + PROTECT, STAY + PASSIVE, etc.
 */
public enum SubordinateTargetingMode {
    NEUTRAL,
    PASSIVE,
    AGGRESSIVE,
    PROTECT;

    public static SubordinateTargetingMode fromNativeBehaviour(int behaviour) {
        return switch (behaviour) {
            case 1 -> PASSIVE;
            case 2 -> AGGRESSIVE;
            case 3 -> PROTECT;
            default -> NEUTRAL;
        };
    }
}
