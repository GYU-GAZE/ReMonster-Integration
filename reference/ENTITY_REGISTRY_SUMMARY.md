# Entity registry snapshot summary

Source: the `entity_type.txt` dump from the current Re:Monster modpack instance.

Total registry ids in snapshot: **766**.

## Namespace counts

| Namespace | IDs |
|---|---:|
| tensura | 209 |
| minecraft | 130 |
| cataclysm | 103 |
| born_in_chaos_v1 | 98 |
| iceandfire | 47 |
| block_factorys_bosses | 43 |
| mowziesmobs | 41 |
| mysticism | 20 |
| malum | 16 |
| supplementaries | 15 |
| relics | 12 |
| mobs_of_mythology | 7 |
| smallships | 6 |
| mca | 6 |
| tensura_neb | 5 |
| tensura_guild | 2 |
| farmingforblockheads | 1 |
| farmersdelight | 1 |
| butchery | 1 |
| tensura_tno | 1 |
| artifacts | 1 |
| sophisticatedbackpacks | 1 |

## Important constraint

This is an `EntityType` registry dump, not a living-mob list. It contains mobs alongside projectiles, effects, helper entities, vehicles and displays.

Examples of real mob candidates:

```text
tensura:horned_rabbit
tensura:horned_bear
cataclysm:ignis
mowziesmobs:ferrous_wroughtnaut
mobs_of_mythology:kobold
iceandfire:fire_dragon
born_in_chaos_v1:dread_hound
mca:male_villager
mca:female_villager
```

Examples that must not become Mob Profiles merely because their ids are registered:

```text
tensura:fire_ball
cataclysm:laser_beam
minecraft:area_effect_cloud
minecraft:falling_block
mowziesmobs:camera_shake
supplementaries:cannonball
```

The eventual registry scanner must validate that a registered type actually produces a relevant `LivingEntity`/`Mob` before suggesting or auto-generating a Mob Profile.

## Priority namespaces for first-pass profiling

```text
tensura
born_in_chaos_v1
cataclysm
iceandfire
block_factorys_bosses
mowziesmobs
mobs_of_mythology
```

MCA entities are handled by dedicated adapters rather than normal hostile-mob profile generation.
