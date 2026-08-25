# Target mob registries

Source-verified entity inventories used when authoring Mob Profiles. This file intentionally separates registry discovery from balance: EP, Tensura skills, disposition, tier, ecology and guard policy are not inferred merely from an entity name.

## Wyrmroost 1.21.1

Source: `NordAct/Wyrmroost`, `WREntities.java` on the current 1.21.1 NeoForge line.

Namespace: `wyrmroost`

Living dragon/creature entity ids registered by the mod:

- `wyrmroost:lesser_desertwyrm`
- `wyrmroost:overworld_drake`
- `wyrmroost:silver_glider`
- `wyrmroost:roost_stalker`
- `wyrmroost:butterfly_leviathan`
- `wyrmroost:dragon_fruit_drake`
- `wyrmroost:canari_wyvern`
- `wyrmroost:royal_red`
- `wyrmroost:coin_dragon`
- `wyrmroost:alpine`

Excluded from Mob Profile authoring by default: dragon eggs, arrows, breath entities and other projectile/effect entities.

Notes:

- Wyrmroost registers its dragon mobs mostly as `MobCategory.CREATURE`, so vanilla hostile-category assumptions are not useful for project guard/ecology policy.
- The mod has its own taming/ownership/home-defense behavior. Re:Monster Integration must preserve that behavior and should not treat every dragon as generically hostile.

## Legendary Monsters 2.2.2 / 1.21.1 NeoForge

Source: `Miauczel/Legendary-Monsters-1.21.1-NeoForge`, `ModEntities.java`.

Namespace: `legendary_monsters`

Source-verified living mob candidates (`MONSTER` or `CREATURE`) currently relevant to Mob Profiles:

- `legendary_monsters:skeletosaurus`
- `legendary_monsters:overgrown_colossus`
- `legendary_monsters:endersent`
- `legendary_monsters:warped_fungussus`
- `legendary_monsters:lava_eater`
- `legendary_monsters:frostbitten_golem`
- `legendary_monsters:shulker_mimic`
- `legendary_monsters:chorusling`
- `legendary_monsters:withered_abomination`
- `legendary_monsters:ancient_guardian`
- `legendary_monsters:ambusher`
- `legendary_monsters:spiky_bug`
- `legendary_monsters:cloud_golem`
- `legendary_monsters:haunted_knight`
- `legendary_monsters:haunted_guard`
- `legendary_monsters:posessed_paladin`
- `legendary_monsters:knights_armor`
- `legendary_monsters:guard`
- `legendary_monsters:mossy_golem`
- `legendary_monsters:skeloraptor`
- `legendary_monsters:dune_sentinel`
- `legendary_monsters:bomber`
- `legendary_monsters:stratling`
- `legendary_monsters:hovering_hurricane`
- `legendary_monsters:the_obliterator`
- `legendary_monsters:flameborn_guard`
- `legendary_monsters:flameborn_warrior`
- `legendary_monsters:flame_drifter`
- `legendary_monsters:annihilation_pursuer`
- `legendary_monsters:wandering_eye`
- `legendary_monsters:beheaded_knight`
- `legendary_monsters:resurrected_knight`
- `legendary_monsters:fractured_apostle`
- `legendary_monsters:fractured`

`legendary_monsters:resurrected_javelin` is registered as `MobCategory.MONSTER` in source but behaves like a combat/projectile helper and is therefore excluded from the ordinary mob roster unless runtime inspection proves otherwise.

The source also registers many projectiles, beams, clouds, shockwaves, camera helpers, boss clones and effect entities. These must not accidentally receive normal Mob Profiles just because they are entity registry entries.

### Boss candidates explicitly visible in source organization

The source imports dedicated `IAnimatedBoss` implementations for:

- `legendary_monsters:cloud_golem`
- `legendary_monsters:posessed_paladin`
- `legendary_monsters:the_obliterator`

This is a source-level classification clue only. Final Re:Monster `tier`, EP and skill assignments still require runtime/balance review.

## Removed generic variation layer

`Tensura Unique Monsters` / `Tensura: Unique Monster` is intentionally not part of the project roster. MobChampions is the sole generic random elite/champion variation system.

## Future ports

Profiles may be prepared fail-soft for these namespaces once their official 1.21.1 NeoForge ports exist:

- Saint's Dragons
- Opposing Force

Missing entity ids are ignored by the profile loader, so future profiles do not require hard dependencies.
