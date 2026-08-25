package com.gyugaze.remonsterintegration.profile;

/**
 * Controls how a Mob Profile EP value interacts with EP already assigned by Tensura or another compatibility addon.
 */
public enum EpApplicationMode {
    /** Only assign the profile value when the entity has no positive EP yet. */
    FALLBACK,

    /** Keep whichever value is higher. */
    MINIMUM,

    /** Force the profile value. Intended only for deliberate pack-level overrides. */
    REPLACE
}
