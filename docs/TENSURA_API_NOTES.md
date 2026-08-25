# Tensura 2.0.1.2 API reconnaissance

Source basis: direct inspection of the installed `tensura-neoforge-2.0.1.2.jar` supplied for development.

This document records observed class signatures/bytecode behavior. It is not a substitute for the upstream source and should be updated whenever the target Tensura version changes.

## Mod metadata

- mod id: `tensura`
- version: `2.0.1.2`
- license declared by jar: `All Rights Reserved`
- NeoForge requirement: `[21.1,)`
- Minecraft requirement: `[1.21.1,)`
- ManasCore modules required: storage, skill and race >= `3.0.2.9`
- Architectury >= `13.0.8`
- GeckoLib >= `4.7.5`
- SmartBrainLib >= `1.16.7`
- TerraBlender >= `4.1.0.0`

Do not commit or redistribute this jar in this repository.

## Existence storage

Classes:

```text
io.github.manasmods.tensura.storage.ep.IExistence
io.github.manasmods.tensura.storage.ep.ExistenceStorage
io.github.manasmods.tensura.storage.TensuraStorages
```

Observed `IExistence` methods include:

```text
getEP / setEP
getGainedEP / setGainedEP
getMagicule / setMagicule
getAura / setAura
getPermanentOwner / setPermanentOwner
getTemporaryOwner / setTemporaryOwner
getSummoner / setSummoner
getTargetNeutralList
isTargetNeutral
addNeutralTarget
removeNeutralTarget
clearNeutralTargets
isNameable / setNameable
getName / setName
markDirty
```

`ExistenceStorage` is a ManasCore `Storage` bound to `LivingEntity` and persists ownership data in NBT.

It also exposes static Harvest Festival helpers:

```text
enterHarvestFestival(IExistence, LivingEntity)
applySubordinateHarvestFestival(LivingEntity, int)
```

## Subordinate helper

Class:

```text
io.github.manasmods.tensura.util.SubordinateHelper
```

Public methods observed:

```text
isAlly(LivingEntity, LivingEntity)
isSubordinate(LivingEntity, LivingEntity)
isSubordinate(LivingEntity, LivingEntity, IExistence)
getSubordinateOwner(LivingEntity)
getSubordinateOwnerUUID(LivingEntity)
isOrderedToStay(LivingEntity)
setStay(Mob)
setFollow(Mob)
setWander(Mob)
setNeutral(Mob)
setPassive(Mob)
setAggressive(LivingEntity)
setProtect(Mob)
removeTarget(Mob)
removeTarget(LivingEntity)
```

### Ownership resolution order

Bytecode inspection of `getSubordinateOwnerUUID` shows this order:

1. `IExistence.getTemporaryOwner()`
2. `IExistence.getSummoner()`
3. `IExistence.getPermanentOwner()`
4. if the entity implements `ISubordinate`, use `ISubordinate.getOwnerUUID()`
5. otherwise `null`

This means a generic `LivingEntity` does not need to implement `ISubordinate` to be recognized as owned/subordinate when its Tensura existence storage contains an owner UUID.

### Recursive subordinate relation

`isSubordinate(owner, target)` recursively follows subordinate ownership chains with a depth guard. It can therefore recognize indirect ownership relationships rather than only direct owner UUID equality.

### Ally relation

`isAlly(a, b)` compares subordinate owners, direct owner relationships, recursive subordinate relationships and vanilla team alliance.

This is a strong candidate for the common ally predicate used by our MCA guard bridge.

## `ISubordinate` boundary

Tensura has:

```text
io.github.manasmods.tensura.entity.template.subclass.ISubordinate
```

The movement helper methods `setStay`, `setFollow` and `setWander` check whether the target/head entity implements `ISubordinate` before mutating Tensura subordinate movement state.

Implication for MCA:

- an MCA villager may be recognized as a Tensura subordinate through `IExistence.permanentOwner`
- it does not automatically become a native Tensura `ISubordinate`
- native movement-state setters appear to no-op for a normal MCA villager rather than replacing its Brain

This is exactly the separation required for an MCA-specific subordinate adapter, pending runtime verification.

## Thought Communication

Class:

```text
io.github.manasmods.tensura.ability.skill.common.ThoughtCommunicationSkill
```

Observed public/static methods:

```text
movementBehaviour(ManasSkillInstance, LivingEntity)
movementTelepathy(ManasSkillInstance, LivingEntity)
attackCommand(Entity, LivingEntity, LivingEntity, double)
targetingBehaviour(ManasSkillInstance, LivingEntity)
targetingTelepathy(LivingEntity)
```

Movement commands call `SubordinateHelper.setFollow`, `setWander` and related helper methods on nearby subordinate mobs.

Because those helpers gate native movement mutation behind `ISubordinate`, MCA ownership-only villagers are promising candidates for custom interception/mapping rather than suffering automatic Tensura AI replacement.

Important caveat: targeting commands can call `Mob.setTarget(...)` directly. MCA compatibility must test and possibly intercept this behavior to ensure commands do not conflict with MCA combat/schedule logic.

## Race / evolution classes

Observed classes include:

```text
io.github.manasmods.tensura.race.TensuraRace
io.github.manasmods.tensura.race.RaceHelper
io.github.manasmods.tensura.race.RaceUtils
io.github.manasmods.tensura.race.template.DefaultRace
io.github.manasmods.tensura.race.template.EvolutionRequirement
```

Evolution requirement implementations observed:

```text
AbilityRequirement
AwakenRequirement
BossRequirement
EPRequirement
ItemCarryingRequirement
ItemConsumeRequirement
NamedRequirement
PhysicalBodyRequirement
```

The jar contains explicit race classes for Goblin, Hobgoblin, Enlightened Hobgoblin, Ogre, Kijin and many other branches, so expanded evolution work can likely reuse the native race framework rather than inventing an independent race system.

## Skills

Relevant base classes:

```text
io.github.manasmods.tensura.ability.TensuraSkill
io.github.manasmods.tensura.ability.TensuraSkillInstance
io.github.manasmods.tensura.ability.SkillHelper
io.github.manasmods.tensura.ability.SkillUtils
io.github.manasmods.tensura.ability.skill.Skill
```

Registries include:

```text
CommonSkills
ExtraSkills
IntrinsicSkills
ResistanceSkills
UniqueSkills
```

Further investigation requires the matching ManasCore jar because the actual skill storage/mastery APIs are defined in ManasCore interfaces.

## Natural spawn data

Tensura 2.0.1.2 ships natural spawns through NeoForge biome modifiers and biome tags.

Example structure:

```text
data/tensura/neoforge/biome_modifier/<mob>_spawn.json
data/tensura/tags/worldgen/biome/<mob>_spawn.json
```

This allows the Re:Monster ecology layer to override much of Tensura's spawn distribution using datapack resources instead of invasive Java patches.

Known examples from the current jar:

- Horned Rabbit: taiga / woodland-mansion tag / cherry grove; weight 20; group 1-2
- Horned Bear: taiga/hills/cherry-grove-style habitats; weight 20; group 1
- Cattledeer: substantially higher weight (82 in the inspected modifier)
- Direwolf: lower weight (10) but group size 3-7

The project intends to rebalance these into a coherent cross-mod ecology, including making suitable generalist fauna visible in plains/meadows where appropriate.

## Next inspection targets

- `TensuraStorages` signatures and storage acquisition guarantees
- naming flow and where `setPermanentOwner` is written
- race assignment/evolution methods
- ManasCore skill storage / possessed vs learned/mastered state
- event hooks for death / EP changes
- how third-party `LivingEntity` receives `ExistenceStorage`
- exact Thought Communication subordinate predicate lambdas
