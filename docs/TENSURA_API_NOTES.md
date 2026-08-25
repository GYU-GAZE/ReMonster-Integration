# Tensura 2.0.1.2 API reconnaissance

Source basis: direct inspection of the installed `tensura-neoforge-2.0.1.2.jar` supplied for development, plus the matching public ManasCore 1.21.1 source where noted.

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

Observed `TensuraStorages` access:

```text
TensuraStorages.getExistenceFrom(LivingEntity) -> IExistence
TensuraStorages.getAbilityFrom(LivingEntity) -> IAbility
TensuraStorages.getEffectFrom(LivingEntity) -> IEffect
TensuraStorages.getSpiritFrom(LivingEntity) -> ISpiritWielder
```

`getExistenceFrom` directly retrieves the `ExistenceStorage` attached through ManasCore storage to a `LivingEntity`.

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

## Better Subordinates 1.2.4 boundary

The exact target build was inspected from CurseMaven (`tensura-better-subordinates-1489224:8414486`). The important architecture is much narrower than expected.

`ExpandedSkillStorage` is a ManasCore entity storage containing the native Better Subordinates progression counters:

```text
spiderEyesEaten
spiderKills
slimeKills
centipedeKills
totalKills
damageTaken
existenceUpdated
```

It persists these fields itself and implements `IExpandedSkillAcquisition`.

### Native attachment predicate

Better Subordinates registers this storage only when the entity is:

```text
OwnableEntity
AND LivingEntity
AND not #tensura:clones
```

An MCA `VillagerEntityMCA` is intentionally not converted into a vanilla `OwnableEntity`, so an ownership-only MCA subordinate would normally fail this one attachment predicate even though Tensura already recognizes its permanent owner.

The project therefore patches only the `ExpandedSkillStorage` registration predicate so an MCA villager is also accepted. This gives MCA villagers the upstream Better Subordinates storage without changing their entity class or Brain.

### Everything after storage attachment is already generic

Observed Better Subordinates progression methods accept ordinary `LivingEntity` instances:

```text
attemptTallyKill(Entity, LivingEntity)
attemptTallyDamage(LivingEntity, float)
entityEPUpdate(LivingEntity)
IExpandedSkillAcquisition.grantSkill(LivingEntity, ...)
```

These methods use:

- `SubordinateHelper.getSubordinateOwner(...)`
- `SubordinateHelper.getSubordinateOwnerUUID(...)`
- `EnergyHelper.getMaxEP(...)`
- `SkillAPI.getSkillsFrom(...)`
- `TensuraStorages.getExistenceFrom(...)`
- `SkillHelper.learnSkill(...)`

Therefore the existing Re:Monster permanent-owner adapter supplies the ownership relation Better Subordinates expects after its storage is present.

The Thought Communication extension and Better Subordinates C2S packets also validate ownership through `SubordinateHelper.isSubordinate(owner, target)` and operate on generic `LivingEntity` skill/ability storage. The skill-grant packet has one additional gate: `BetterSubs.getSkillExpansionFrom(target) != null`, which the storage predicate bridge satisfies.

### Deliberate AI boundary

Better Subordinates also mixes combat automation such as Haki usage into `TensuraTamableEntity`. Re:Monster does **not** extend that mixin to MCA villagers.

Desired split:

```text
MCA villager
├─ MCA Brain / schedule / family / profession / home       unchanged
├─ Tensura existence / owner / EP / skill storage         enabled
└─ Better Subordinates progression storage                enabled
   ├─ kills
   ├─ damage taken
   ├─ EP threshold unlocks
   ├─ skill acquisition / mastery infrastructure
   └─ Thought Communication skill UI / commands

TensuraTamableEntity-only combat automation               not copied to MCA
```

Runtime testing still has to verify storage persistence, event tallying and UI behavior in the full pack, but the bytecode-level compatibility boundary is now explicit.

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

## Skills and learning

Relevant Tensura classes:

```text
io.github.manasmods.tensura.ability.TensuraSkill
io.github.manasmods.tensura.ability.TensuraSkillInstance
io.github.manasmods.tensura.ability.SkillHelper
io.github.manasmods.tensura.ability.SkillUtils
io.github.manasmods.tensura.ability.skill.Skill
```

Relevant ManasCore classes:

```text
io.github.manasmods.manascore.skill.api.SkillAPI
io.github.manasmods.manascore.skill.impl.SkillStorage
io.github.manasmods.manascore.skill.api.ManasSkillInstance
```

`SkillAPI.getSkillsFrom(LivingEntity)` returns a `SkillStorage` for arbitrary living entities. `SkillStorage` stores `ManasSkillInstance` objects and exposes `getSkill(ResourceLocation)`, `getLearnedSkills()`, `learnSkill(...)`, `forgetSkill(...)` and update/sync helpers.

### Critical learning-state discovery

Tensura represents an acquired-but-not-fully-learned skill using a real `ManasSkillInstance` whose mastery is **negative**.

`TensuraSkill.createLearningInstance(entity)` creates a normal `TensuraSkillInstance` and sets mastery to `getAcquirementMastery(entity)`, which is negative for the learning/acquisition state.

`TensuraSkill.addLearnPoint(instance, entity, mode, amount)`:

- immediately rejects instances with mastery >= 0
- fires `TensuraSkillEvents.SKILL_LEARNING`
- increases the negative mastery value toward 0
- marks skill storage dirty/synced
- when the threshold is crossed, the normal Tensura learning/acquisition flow completes the transition

This is almost exactly the primitive required for the planned kill-learning feature. We should **call Tensura's own `addLearnPoint`** rather than inventing a second learning counter.

Mode-specific learning is separate. `learnMode(...)` stores per-mode learning progress in the skill instance NBT tag using `getModeId(...)` and `getLearningPointRequirement(...)`.

Therefore the planned kill-learning algorithm can be:

```text
1. Resolve dead mob's effective profile skills.
2. Resolve player's SkillStorage.
3. For each matching skill id:
   - player must already have a skill instance
   - instance mastery must be < 0
   - registered skill must be a TensuraSkill
4. Call TensuraSkill.addLearnPoint(instance, player, 0, configuredAmount).
5. Let Tensura events, penalties, messages, sync and completion behavior run normally.
```

The exact mode argument and whether some skills need specialized handling still require runtime tests, but the underlying mechanic is confirmed.

Registries include:

```text
CommonSkills
ExtraSkills
IntrinsicSkills
ResistanceSkills
UniqueSkills
```

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

- naming flow and exact places where `setPermanentOwner` is written
- race assignment/evolution methods
- death/kill event ownership resolution for subordinate/clone kills
- how third-party `LivingEntity` receives `ExistenceStorage` at runtime
- exact Thought Communication targeting behavior on MCA entities
- skills that override the default learning/acquisition model
