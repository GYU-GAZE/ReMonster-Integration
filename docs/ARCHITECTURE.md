# Architecture

## Principle

Tensura: Reincarnated is the gameplay spine. Other mods are integrated through project-owned adapters and data instead of being allowed to behave as isolated systems.

The central rule is: if several features need to know what a mob is, that information belongs in one shared profile rather than being hard-coded independently in guards, quests, spawners and EP logic.

## Module layout

```text
remonster_integration
├─ profile/
│  ├─ MobProfile
│  ├─ MobProfileManager
│  ├─ MobProfileReloadListener
│  └─ MobClassification
├─ tensura/
│  ├─ TensuraExistenceAdapter
│  ├─ TensuraSkillAdapter
│  ├─ TensuraSubordinateAdapter
│  └─ TensuraEvolutionAdapter
├─ ecology/
│  ├─ EcologyProfile
│  ├─ Habitat
│  └─ SpawnDirector
├─ compat/
│  ├─ mca/
│  │  ├─ McaThreatBridge
│  │  └─ McaSubordinateAdapter
│  ├─ townstead/
│  │  └─ TownsteadRaceBridge
│  └─ optional mod adapters
├─ learning/
│  └─ KillLearningHandler
├─ spawner/
│  └─ SpawnerDirector
└─ starter/
   └─ StartingSkillSelector
```

Exact package names may move while APIs are discovered; module boundaries should remain.

## Mob Profile

Mob Profiles are project-owned JSON data loaded from datapack resources.

Proposed resource path:

```text
data/remonster_integration/mob_profiles/<namespace>/<entity>.json
```

Example:

```json
{
  "entity": "cataclysm:ignis",
  "tier": "boss",
  "ep": 850000,
  "skills": [
    "tensura:flame_manipulation"
  ],
  "learning_multiplier": 1.5,
  "guard_hostile": true,
  "dungeon_weight": 2,
  "themes": ["fire", "boss", "dark_fantasy"],
  "ecology": {
    "natural_spawn": false,
    "habitats": ["dungeon_only"]
  }
}
```

Profiles for missing entity ids must be ignored safely. This allows future Saint's Dragons and Opposing Force profiles to ship before their 1.21.1 NeoForge ports are installed.

## Tensura ownership model

Initial inspection of Tensura 2.0.1.2 shows that subordinate ownership is not limited to entities implementing Tensura's `ISubordinate` interface.

`ExistenceStorage` stores:

- permanent owner UUID
- temporary owner UUID
- summoner UUID

`SubordinateHelper.getSubordinateOwnerUUID(LivingEntity)` resolves those ownership fields first and only falls back to `ISubordinate` afterwards.

This is strategically important for MCA villagers: an MCA villager can potentially remain an MCA entity and still be recognized as a Tensura subordinate by assigning ownership in Tensura existence storage.

## MCA villager invariant

An MCA villager must never be replaced with a Tensura mob merely to gain subordinate or race behavior.

The MCA entity remains authoritative for:

- Brain and navigation schedule
- relationships and family tree
- profession and workplace
- village/home
- memories
- inventory/equipment
- personality/traits
- MCA rendering state
- Townstead genetics

Tensura state is attached alongside it.

## Thought Communication boundary

Tensura 2.0.1.2 exposes `SubordinateHelper.setStay`, `setFollow`, `setWander`, `setNeutral`, `setPassive`, `setAggressive` and `setProtect`.

Initial bytecode inspection shows the movement setters only modify movement-specific state when the target implements Tensura's `ISubordinate`. Ownership-only MCA villagers therefore appear capable of being recognized as subordinates without automatically receiving Tensura's native subordinate movement implementation.

This is a strong basis for an MCA-specific command adapter:

- Stay -> MCA stay/halt behavior
- Follow -> MCA follow-player behavior
- Wander -> release special order and resume MCA schedule
- Neutral -> normal MCA combat behavior
- Passive -> suppress voluntary combat
- Protect -> defend owner/village
- Aggressive -> likely restricted or explicitly mapped

This must be verified in a running environment before being treated as final behavior.

## MCA guard threat resolution

Threat resolution should be dynamic and ordered:

```text
1. Is target actively attacking an MCA villager/guard?
   yes -> hostile

2. Is target a Tensura subordinate / owned friendly mob?
   yes -> normally ignore

3. Is target explicitly classified guard_hostile by project profile/tag?
   yes -> hostile

4. Fall back to MCA behavior
```

Subordinate status is therefore an immunity from passive guard targeting, not permission to attack villagers.

## Ecology

Spawn behavior belongs to the same entity profile because it affects encounter frequency, EP progression, quest frequency and dungeon composition.

The director should prefer the least invasive mechanism available:

1. datapack biome tags / NeoForge biome modifiers
2. existing mod configuration
3. InControl rules
4. custom NeoForge spawn interception only when necessary

For Tensura 2.0.1.2, natural mobs use datapack biome tags and NeoForge `add_spawns` biome modifiers, so much of its ecology can be overridden without Java patches.

## Compatibility philosophy

- existing official compatibility addons win where they already implement correct behavior
- Re:Monster Integration fills gaps and normalizes behavior
- avoid duplicate EP rewards and duplicate skills
- integrations for optional mods must not prevent startup when those mods are missing
- reflective/mixin access is a last resort after public APIs/events/storage are exhausted
- third-party jars are development inputs, never source-controlled project assets
