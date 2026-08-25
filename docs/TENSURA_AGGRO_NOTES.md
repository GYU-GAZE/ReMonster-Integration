# Tensura hostile targeting notes

Target inspected: Tensura: Reincarnated `2.0.1.2` for Minecraft 1.21.1.

These notes exist because the pack should not assume Tensura mobs behave like ordinary vanilla hostile mobs. The current behavior combines Tensura's own targeting helpers with SmartBrainLib sensors and can produce much more aggressive pursuit than expected.

## Base detection range

`TensuraTamableEntity.setAttributes()` declares `Attributes.FOLLOW_RANGE = 32.0`.

Many ordinary Tensura mobs inherit this value. SmartBrainLib's `NearbyLivingEntitySensor` uses the entity's current `FOLLOW_RANGE` as its X/Z and Y scan radius when filling nearby/visible living-entity memories.

This means reducing FOLLOW_RANGE through Re:Monster Integration can change real target acquisition without replacing each mob's Brain implementation.

## Larger native ranges found in 2.0.1.2

The exact runtime jar contains explicit FOLLOW_RANGE overrides including:

| Entity/class | Native FOLLOW_RANGE |
|---|---:|
| Giant Cod | 16 |
| Giant Salmon | 16 |
| common `TensuraTamableEntity` default | 32 |
| Megalodon | 48 |
| Akash | 64 |
| Elemental Colossus | 64 |
| Ifrit | 64 |
| Ifrit Clone | 64 |
| Metal Slime | 64 |
| One-Eyed Owl | 64 |
| Orc Disaster | 64 |
| Orc Lord | 64 |
| Sissie | 64 |
| Sylphide | 64 |
| Undine | 64 |
| War Gnome | 64 |
| Charybdis | 256 |

These values should not all be flattened blindly. Boss/calamity encounters may intentionally deserve exceptional awareness, while fast or flying ordinary fauna can feel unfair even at the 32-block default.

## Why wild mobs dogpile players

Tensura's prey targeting helper treats a player as directly eligible prey before the additional health/type checks used for ordinary animal prey.

Wild target acquisition also enables ally alerting. Tensura's custom retaliation/target behavior can propagate a newly acquired target to nearby allies. SmartBrainLib's target behavior uses FOLLOW_RANGE again as the ally-alert radius.

Practical result:

1. one wild mob can detect a visible player inside its scan radius;
2. the player is a valid prey target;
3. the mob acquires the player;
4. nearby allies can receive the same target;
5. high movement/flying speed makes the entire group close the distance immediately.

The first acquisition still relies on nearby/visible memories in ordinary SmartBrainLib targeting, so this is not literal infinite omniscience. The combination of large ranges, direct player prey eligibility, ally propagation and high mobility can nevertheless feel like it.

## Re:Monster policy

Mob Profiles now support:

```json
"aggro_range": 20
```

Rules:

- `0` means preserve the entity's native FOLLOW_RANGE;
- a positive value replaces the entity's base FOLLOW_RANGE when the profile is applied;
- use per-species/per-role values rather than a global blanket nerf;
- prioritize reducing ordinary fast/flying hostile mobs that create off-screen pursuit;
- preserve or deliberately tune exceptional boss/calamity awareness;
- playtest ally propagation after range changes before adding deeper AI mixins.
