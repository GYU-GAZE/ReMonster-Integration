# Compatibility matrix

This file tracks the target modpack runtime and which integrations are owned by Re:Monster Integration.

## Runtime

| Component | Target |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Java | 21 |

## Core mods

| Mod | Target version | Role | Integration status |
|---|---:|---|---|
| Tensura: Reincarnated | 2.0.1.2 | gameplay spine | API integration active |
| ManasCore | 4.0.0.2 | storage/skill/race backend | compile integration working through bundled modules |
| Tensura: Better Subordinates | 1.2.4 | subordinate progression | deeper progression integration pending |
| Tensura: Skill Obtain | 1.1 | kill/skill ecosystem | inspect before duplicating behavior |
| EPGains | 0.0.18 | EP/scaling changes | must avoid double scaling |
| MCA Reborn | 7.7.36-beta.3+1.21.1 | villagers/society | threat bridge implemented; subordinate adapter in progress |
| MCA Capitals | 1.2.1 | settlement layer | preserve compatibility |
| MCA: Quests | 1.1.0 | villager quests | hunt content planned |
| Townstead | 0.7.6+1.21.1 | roots/genetics/visual races | Tensura race visual bridge planned |
| InControl | 1.21-10.2.7 | spawn control | ecology fallback/tooling |

## Mob ecosystem

| Mod | Status | Intended treatment |
|---|---|---|
| Tensura base/addons | installed | full Mob Profile coverage and aggro/ecology normalization |
| L_Ender's Cataclysm | installed | high-tier/boss/dungeon emphasis |
| Mowzie's Mobs | installed | full fantasy ecosystem integration |
| Mowzie's Cataclysm | installed | inspect overlap with both parents |
| Mobs of Mythology | installed | general fantasy ecosystem |
| Bosses'Rise | installed | boss/dungeon layer; existing Tensura compat wins where applicable |
| Ice and Fire Community Edition | installed | fantasy fauna/boss layer; use official Tensura compat if present/compatible |
| MobChampions | installed | sole random elite/champion variation layer; retain its native stat/effect system and add profile-aware EP/learning/quest treatment |
| Legendary Monsters | planned | target 2.2.2 for 1.21.1 NeoForge; boss/miniboss layer; source registry inventoried |
| Tensura Reincarnated & Legendary Monsters Compatibility | planned with Legendary Monsters | prefer as authoritative Legendary Monsters EP/magicule/aura/skill/combat-skill bridge after runtime validation |
| Wyrmroost | planned | target 1.4.1 for 1.21.1 NeoForge; dragon registry inventoried; preserve native taming/ownership behavior |
| Saint's Dragons | future | activate profiles when official 1.21.1 NeoForge port exists |
| Opposing Force | future | activate profiles when official 1.21.1 NeoForge port exists |

### Elite variation policy

MobChampions is the only generic random "this individual spawned absurdly strong" system in the pack. Re:Monster Integration should detect champion state for EP, learning, quests and dungeon balancing, but should not duplicate MobChampions' own health/armor/speed/damage/effect modifiers.

MobChampions 1.21.1 does not currently appear to apply a generic entity scale/size modifier. Physical size changes are therefore not part of the champion policy unless Re:Monster Integration intentionally adds them later.

Champion rank is already consumed by kill-learning rewards without modifying MobChampions combat stats:

- Uncommon: x1.25 learning
- Rare: x1.5 learning
- Epic: x2 learning
- Legendary: x3 learning

### Legendary Monsters policy

The dedicated 1.21.1 NeoForge compatibility addon advertises EP, magicule/aura pools, tier-scaled Tensura skills and custom combat AI that actively casts those skills for Legendary Monsters entities. If runtime testing confirms those claims on the frozen pack versions, that addon should be authoritative for those systems.

Re:Monster Integration should then own only the cross-pack metadata and systems not already covered there: ecology, MCA guard policy, kill-learning interpretation, MobChampions-aware rewards, bounty/quest metadata, dungeon weights and targeted balance corrections.

## Structure / dungeon mods

| Mod | Target integration |
|---|---|
| When Dungeons Arise | modded randomized spawner pools |
| YUNG's Better Dungeons | modded randomized spawner pools |
| YUNG's Better Mineshafts | ecology/spawner review if useful |
| YUNG's Better Strongholds | ecology/spawner review if useful |
| Tensura Boss Structure | preserve native boss placements |

## Quest / onboarding systems

| System | Plan |
|---|---|
| Bountiful | frequent monster-hunt bounties driven by project roster |
| MCA: Quests | monster-hunt objectives driven by project roster |
| FTB Quests | intentionally deferred to final stage |

## Explicitly removed from the pack plan

- Born in Chaos
- Tensura Unique Monsters / Tensura: Unique Monster; MobChampions is the sole generic random elite variation layer

## Explicitly out of scope

- MCA Conversations integration
- replacing MCA villagers with Tensura mob entities
- committing third-party mod jars into this repository
- indiscriminately treating every `Monster` subclass from every mod as a valid village threat

## Compatibility rules

1. Existing official/dedicated Tensura compatibility should be preferred when it already handles a mod correctly.
2. Re:Monster Integration must avoid duplicate EP rewards, skills and evolution effects.
3. Optional mod integrations must fail soft when the target mod/entity is absent.
4. MCA villager identity and simulation data are authoritative and must survive all Tensura integration.
5. Spawn/ecology overrides should prefer datapacks and existing configuration before mixins/runtime replacement.
6. MobChampions remains authoritative for champion stat/effect modification; the integration addon may consume champion state but should not recreate its modifier system.
7. Exact versions must be updated when the pack instance changes; compatibility is not assumed across Tensura/MCA/Townstead updates.
