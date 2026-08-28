# Roadmap

This roadmap is ordered by dependency and risk, not by feature hype. Shared infrastructure comes before content-heavy systems.

## Status convention

- `[x]` means the implementation exists in the addon.
- **runtime-proven** means the behavior has also been observed working in the actual modpack.
- **runtime-retest pending** means a failed/partial runtime test produced a concrete fix that compiles, but the corrected behavior has not yet been re-tested in-game.
- **debug-only** means the underlying system works through pack-author commands/data, but still needs a normal gameplay trigger or UI before it is considered player-facing.

## Current milestone - Progression / evolution graduation pass

The universal progression foundations are now substantially implemented. The immediate goal is to stop treating already-built systems as future architecture work and graduate the remaining debug-only pieces into ordinary gameplay.

- [x] universal individual EP growth uses Tensura's real `LivingEntity` progression instead of a parallel XP system
- [x] exact living killers can acquire Tensura skill-learning progress from defeated mobs
- [x] wild third-party mobs can accumulate individual EP and acquisition-learning state (**runtime-proven with Mobs of Mythology Chupacabra**)
- [x] external mobs receive an aura/magicule foundation so native learning and active-skill costs can be paid
- [ ] re-test external acquisition to completion after the energy fix
- [ ] re-test external active-skill AI after the energy fix
- [x] MCA civilians can consume Tensura Battlewill manuals from inventory and learn the native skill (**runtime-proven**)
- [x] controlled generic entity-body evolution exists and preserves portable progression/state
- [x] generic physical replacement was proven with Zombie -> Husk while preserving equipment and learned skill (**runtime-proven**)
- [x] preserve MobChampions rank across replacement instead of re-rolling rarity
- [ ] re-test MobChampions rank preservation after the replacement fix
- [x] MCA armed-civilian self-defence works without changing profession/schedule (**runtime-proven foundation**)
- [x] restore MCA's native civilian self-defence semantics while suppressing only the inappropriate panic presentation during healthy armed combat
- [x] add native-style bow spacing/kiting for ordinary MCA civilians and commanded MCA civilians that equip bows from inventory
- [ ] re-test the final MCA panic/bow cleanup and kiting pass
- [ ] implement natural evolution triggering so eligible mobs can evolve without `/remonster evolve`
- [ ] implement branch selection / branch conditions so multiple valid evolutions are never chosen arbitrarily
- [ ] move owned-subordinate evolution choices into the normal subordinate gameplay/UI
- [ ] author and runtime-prove the first real natural cross-mod evolution chain, starting with the canine line
- [ ] run one consolidated progression/subordinate/evolution regression pass and then merge the batch

### First natural evolution proving ground - canine line

Provisional content target for graduating evolution from debug commands into gameplay:

```text
minecraft:wolf
  -> tensura:hound_dog
      -> tensura:barghest
      -> tensura:direwolf
          -> stronger wolf/canine body from the installed roster (TBD after source/stat/runtime comparison)
```

Rules for the proving chain:

- a single eligible edge may auto-trigger for a wild mob after a safe debounce / out-of-combat window
- owned subordinates with multiple eligible branches must expose a player choice rather than auto-picking
- wild mobs with multiple branches must be disambiguated by data-driven conditions, not an unexplained coin flip
- branch conditions should be reusable primitives such as EP, biome/environment, required/forbidden skill, ownership state and other explicitly authored conditions
- replacement must preserve the individual's progression and identity-relevant portable state while the new body keeps its own native Brain/goals/attributes
- final high-tier canine body is intentionally not selected by name alone; compare actual candidates before committing content

## Phase 0 - Foundation

- [x] Create project repository
- [x] Freeze runtime: Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21
- [x] Inspect Tensura 2.0.1.2 and ManasCore APIs
- [x] Add CI-safe compile dependencies for Tensura + ManasCore
- [x] Build successfully in CI against the real APIs
- [x] launch and use the addon inside the actual full modpack runtime
- [ ] add the complete local runtime dependency set for launching the full pack directly from the development workspace
- [x] `/remonster inspect <entity>` diagnostics for profile, EP, ownership, skills, MobChampions and FOLLOW_RANGE
- [x] `/remonster progression status <entity>` diagnostics for progression, skills, Better Subordinates state and aura/magicule capacity

## Phase 1 - Shared Mob Profile system

- [x] central `MobProfile` data model
- [x] JSON parser + validation + reload listener + registry
- [x] optional-mod-safe entity resolution
- [x] disposition and tier classification
- [x] EP values and `FALLBACK` / `MINIMUM` / `REPLACE` policy
- [x] effective Tensura skill declarations
- [x] learning multiplier
- [x] dungeon/spawner weight
- [x] MCA guard hostility policy
- [x] ecology / habitat profile
- [x] optional aggro/FOLLOW_RANGE override
- [x] semantic themes/tags
- [x] initial Wyrmroost catalog for all 10 living creature types
- [x] initial Legendary Monsters boss profiles for Cloud Golem, Possessed Paladin and The Obliterator
- [ ] automatically reapply changed profiles to already-loaded matching entities after datapack reload
- [ ] author the broader validated profile catalog for the installed target mob roster

## Phase 2 - Tensura mob integration

- [x] resolve `IExistence` for arbitrary `LivingEntity`
- [x] narrow Tensura existence/EP bridge
- [x] narrow ownership/alliance bridge
- [x] generic Mob Profile application on entity join
- [x] safe EP application preserving positive native values by default
- [x] generic ManasCore/Tensura skill assignment without duplicates
- [x] consume skills actually present at runtime so dedicated compatibility addons remain authoritative
- [x] detect MobChampions state without recreating its stat system
- [x] use MobChampions rank for kill-learning scaling
- [ ] author/balance final EP values for supported third-party mobs
- [ ] author project-owned effective Tensura skills only where no good compatibility addon exists
- [ ] explicit per-mod authority rules for the remaining compatibility addons
- [ ] use MobChampions rank for EP/quest/dungeon reward scaling where appropriate

Target roster includes Tensura/addons, Cataclysm, Mowzie's Mobs, Mowzie's Cataclysm, Mobs of Mythology, Bosses'Rise, Ice and Fire CE, Wyrmroost, Legendary Monsters, MobChampions, and future compatible Saint's Dragons / Opposing Force builds.

MobChampions remains the sole generic random elite/champion variation layer.

## Phase 3 - Universal skill learning from kills

The learner is the exact living combatant that earned the kill. Player, subordinate, MCA villager, external pet and wild compatible mob all use the same native Tensura acquisition-learning representation.

- [x] acquisition-learning representation confirmed as mastery `< 0`
- [x] native `TensuraSkill.addLearnPoint(...)` primitive used instead of parallel learning points
- [x] `KillLearningService`
- [x] `LivingDeathEvent` integration
- [x] exact `LivingEntity` kill credit
- [x] union profile-declared skills with skills actually present on the defeated mob
- [x] profile learning multiplier
- [x] MobChampions learning multipliers
- [x] wild external mob acquiring skills from kills (**runtime-proven: Chupacabra acquired Self Regeneration and Absorb & Dissolve acquisition state**)
- [x] external aura/magicule capacity foundation for native learning costs
- [ ] runtime-prove repeated acquisition progress reaches learned/mastered state after the energy fix
- [ ] runtime-test unusual skill-specific mode-learning overrides
- [ ] runtime-validate unusual clone/projectile/summon damage-source implementations
- [ ] expose base learning amount as common/configurable balance data
- [ ] optionally scale learning by EP/profile tier if playtesting supports it

## Phase 4 - Ecology / Spawn / Aggro Director

Build a coherent isekai bestiary rather than accepting every mod's independent spawn table.

- [x] inventory Tensura 2.0.1.2 natural spawn mechanism and 42 natural spawn modifiers
- [x] inventory Wyrmroost spawn mechanism, weights, group sizes and biome tags
- [x] prove clean Tensura biome-tag / NeoForge biome-modifier override with Horned Rabbit
- [x] confirm Wyrmroost is also cleanly data-overridable
- [x] semantic habitat groups and frequency/group-size model
- [x] policy keeping elite/boss entities out of ordinary spawn soup unless explicitly intended
- [x] first active Horned Rabbit ecology rebalance
- [ ] inventory spawn systems for the remaining target mods
- [ ] expand curated biome overrides to ordinary Tensura fauna
- [ ] tune Wyrmroost distribution where its defaults conflict with project ecology
- [ ] add minimum player spawn distance where useful
- [ ] use InControl only for rules that cannot be cleanly data-overridden
- [ ] custom runtime spawn hook only where required
- [ ] playtest the first active ecology pass in the full pack

### Aggro normalization

- [x] inspect Tensura SmartBrainLib target acquisition
- [x] confirm FOLLOW_RANGE-driven sensing and native range extremes
- [x] optional `aggro_range` Mob Profile field
- [x] initial Direwolf / Barghest normalization
- [ ] author normalized ranges for the remaining ordinary hostile Tensura fauna
- [ ] preserve intentionally exceptional boss/calamity detection ranges
- [ ] reduce ordinary fast/flying off-screen dogpiles
- [ ] playtest ally target propagation after range normalization

## Phase 5 - MCA threat bridge and civilian combat compatibility

- [x] hook MCA `GuardEnemiesSensor` rather than replacing guard combat AI
- [x] profile-declared hostile modded mobs can be guard targets
- [x] owned/Tensura-allied companions are normally ignored
- [x] active aggression against MCA overrides ownership immunity
- [x] `/remonster guard_priority` diagnostics
- [x] ordinary armed MCA civilians can defend themselves and nearby MCA villagers (**runtime-proven**)
- [x] any carried melee item with real positive main-hand attack damage can be selected as the best melee weapon (**runtime-proven**)
- [x] preserve MCA's own civilian self-defence package instead of replacing it with guard AI
- [x] suppress the MCA panic presentation only while an armed, engaged civilian is healthy enough to fight
- [x] return normal panic presentation below 25% health
- [x] clean up latched bow-use state after combat
- [x] native-style approach/kite/emergency-flee spacing for ordinary civilian bow users
- [x] leave native MCA guards/archers and their richer `ArcherMovementTask` untouched
- [ ] runtime-retest the final panic/bow/kiting fixes
- [ ] runtime-test native melee guards and archers against the broader modded hostile roster

## Phase 6 - Universal subordinate / NPC progression adapter

Goal: let non-native bodies participate in Tensura/Better Subordinates progression without replacing their own simulation systems.

### MCA preservation contract

- [x] MCA entity identity remains in place
- [x] Brain / AI remains MCA-owned
- [x] family / relationships remain MCA-owned
- [x] profession / work / schedule remain MCA-owned
- [x] village / home / memories remain MCA-owned
- [x] inventory/equipment/personality/Townstead genetics remain MCA-owned

### Ownership and progression

- [x] generic Tensura permanent-owner write
- [x] MCA in-place ownership adapter
- [x] raw/resolved ownership diagnostics
- [x] Better Subordinates `ExpandedSkillStorage` compatibility for non-player living bodies
- [x] Better Subordinates remains authoritative for its owner-gated kill/damage/item/EP counters and unlocks
- [x] MCA/external entities can hold Tensura skills and learning/mastery state
- [x] NPC Battlewill / Magic Tome learning uses native `SkillHelper.learnSkill` instead of a parallel teaching system
- [x] MCA inventory Battlewill learning (**runtime-proven on a non-subordinate MCA villager**)
- [x] universal external active-skill AI delegates activation/cost/cooldown/lifecycle to ManasCore
- [x] native Tensura subordinates keep their own Better Subordinates/Tensura combat-skill AI
- [x] external individual EP/stat growth foundation
- [ ] runtime-retest external active-skill casting after aura/magicule fix
- [ ] runtime-test manual/tome teaching on at least one non-MCA external inventory/hand path
- [ ] runtime-verify MCA ownership/progression survives save/reload and normal schedule changes
- [ ] runtime-verify Better Subordinates native counters such as `totalKills` and `damageTaken` on an owned compatible subordinate
- [ ] Harvest Festival compatibility

`damageTaken` is a Better Subordinates counter, not a Re:Monster XP system. Upstream uses it for native training requirements such as Strengthen Body. Wild mobs do not need Better Subordinates owner-gated counters for the universal kill-learning system.

### Command / interaction layer

- [x] preserve useful native screens
- [x] decorate known screens with Re:Monster subordinate status/commands
- [x] generic `HasCustomInventoryScreen` handling
- [x] MCA asynchronous widget rebuild compatibility
- [x] movement modes Follow / Wander / Stay
- [x] movement controls gated by the agreed Telepathy / 2,000 EP milestone
- [x] targeting modes Neutral / Passive / Aggressive / Protect
- [x] targeting controls gated by native Thought Communication mastery
- [x] native Tensura targeting routed through `SubordinateHelper`
- [x] MCA/generic targeting compatibility without replacing MCA Brain/schedule
- [ ] consolidated runtime regression of movement/targeting on Tensura, MCA and one generic external subordinate
- [ ] regression-test horse and Tensura mount native interaction paths
- [ ] regression-test one third-party custom-inventory subordinate

### Skill teaching authority

Do not build a second generic permanent skill-teaching framework unless a concrete gameplay gap appears.

- Better Subordinates already owns subordinate training/unlock progression.
- Re:Monster only bridges real Tensura manuals/tomes into compatible NPC bodies where the player-only item path would otherwise block them.
- Mysticism's Hidden Ruler / Viciel can paste copied skills to targets, but those pasted skills are temporary until target death; this is complementary gameplay, not a replacement for Better Subordinates.
- Permanent arbitrary Unique-Skill coaching is deferred unless a deliberate Re:Monster mechanic is designed for it later.

## Phase 7 - Dungeon / Spawner Director

- [ ] YUNG Better Dungeons spawner support
- [ ] When Dungeons Arise spawner support
- [ ] randomized modded-mob spawn pools
- [ ] per-structure / structure-tag pools
- [ ] use Mob Profile tier/ecology metadata
- [ ] allow MobChampions variants where compatible
- [ ] investigate `SpawnPotentials` rewriting for correct cage preview

## Phase 8 - Hunt quest integrations

### Bountiful

- [ ] high-frequency kill bounty pool for Tensura/project mobs
- [ ] reward and rarity scaling from Mob Profiles

### MCA: Quests

- [ ] monster-hunt quest templates
- [ ] generate/maintain targets from Mob Profiles where practical

## Phase 9 - Starting Unique Skill selector

- [ ] replace/intercept reincarnation skill selection
- [ ] searchable direct Unique Skill selection
- [ ] configurable selected-skill count
- [ ] random mode and configurable random count
- [ ] blacklist / pool controls
- [ ] server-side validation
- [ ] first-login/reset/reincarnation compatibility

## Phase 10 - Townstead <-> Tensura race visual bridge

Foundation status: **complete and runtime-proven for the player path**.

- [x] semantic species separated from physical body representation
- [x] data-driven semantic species -> Townstead body/root mapping
- [x] deterministic persisted phenotype seed
- [x] preserve compatible visual loci while replacing incompatible genetics
- [x] recalculate Townstead phenotype/powers after transformation
- [x] Townstead-specific scale adapter
- [x] multiplayer root/gene sync
- [x] runtime-prove Hobgoblin -> Orc physical representation
- [x] runtime-prove Ogre representation through the Townstead adapter

### Deferred playable-race visual content pass

- [ ] inventory every playable/meaningful Tensura race/evolution stage used by the pack
- [ ] map each semantic race to a Townstead base or project-owned root
- [ ] per-race body scale / palettes / horns / ears / tusks / markings / proportions
- [ ] define preserved/gained/lost Townstead traits across evolution
- [ ] custom roots/genes/attachments only where stock assets are insufficient
- [ ] validate every final mapped playable race in-game

## Phase 11 - Expanded player race evolution

- [x] locate native Tensura race/evolution framework and requirements
- [x] project semantic species state
- [x] data-driven directed evolution edges
- [x] minimum EP evaluation through real `IExistence`
- [x] player Hobgoblin -> Orc / Ogre pipeline proven
- [ ] inspect all target Tensura race branches and concrete requirement values
- [ ] add project branches where useful
- [ ] add non-EP evolution conditions
- [ ] complete example Goblin -> Hobgoblin -> higher branch content
- [ ] assign final phenotype recipes during the deferred visual mapping pass

## Phase 12 - Creature / subordinate evolution

The architecture is no longer the missing piece. Generic replacement is implemented and already has a runtime proof. The remaining work is gameplay triggering, branch semantics and broader runtime validation.

- [x] data-driven evolution graph foundation
- [x] minimum-EP threshold evaluation through Tensura `IExistence`
- [x] MCA-compatible in-place semantic species + Townstead body path
- [x] optional data-driven external `entity_body` replacement path
- [x] controlled replacement keeps the target species' native Brain/goals/body initialization
- [x] preserve portable Re:Monster state, Tensura existence/ownership, skills/mastery, Better Subordinates progression, tame state, name, equipment and health ratio when replacement is unavoidable
- [x] avoid full old-entity NBT loading into the new species body
- [x] runtime-prove generic physical replacement through Zombie -> Husk while preserving equipment and Voice Cannon
- [x] preserve MobChampions rank through replacement rather than generating a new rarity roll
- [ ] runtime-retest MobChampions preservation
- [ ] runtime-test owner/tame/Better-Subs persistence across a real owned external evolution
- [ ] prefer native Tensura evolution APIs for native Tensura mobs where a safe exposed path exists; otherwise use the controlled adapter
- [ ] implement additional reusable evolution conditions beyond minimum EP
- [ ] automatic natural evolution trigger for unambiguous wild-mob edges
- [ ] owned-subordinate branch choice in normal gameplay/UI
- [ ] deterministic/data-authored wild branching when more than one path is available
- [ ] author/runtime-prove the first canine natural chain (`wolf -> hound_dog -> barghest / direwolf -> TBD higher canine`)
- [ ] remove debug commands from the required player workflow; keep them only as pack-author diagnostics

## Phase 13 - MCA-backed fantasy civilizations

Start only after creature/subordinate progression and evolution are stable.

### Goblin civilization

- [ ] MCA-backed Goblin civilian species prototype
- [ ] project Townstead Goblin identity
- [ ] race-appropriate tribal clothing pools
- [ ] tribal guard equipment/loadouts
- [ ] preserve relevant Tensura Goblin trades
- [ ] Goblin naming + Goblin -> Hobgoblin semantics
- [ ] preserve Tensura EP/skills/race state alongside MCA family/genetics/work/schedule
- [ ] runtime-test inheritance, professions, guards, clothing and evolution together

### Dwarf civilization

Only after Goblin proves the architecture.

- [ ] MCA-backed Dwarf species adapter
- [ ] Townstead/Tensura dwarf visual identity
- [ ] reuse compatible `DwarfProfession` trades
- [ ] preserve Dwarven Reputation / price / hostility / Gazel progression
- [ ] map Tensura dwarf professions and trainers
- [ ] explicitly define MCA-vs-Tensura work/restock authority
- [ ] dwarf-specific clothing/equipment pools

Do not remove native `DwarfEntity` until reputation/profession/Gazel compatibility is complete.

## Phase 14 - Polish and pack-author tools

- [ ] config UX
- [x] entity diagnostics
- [x] FOLLOW_RANGE / ownership / MobChampions diagnostics
- [x] targeted profile reapply command
- [x] guard-decision diagnostics
- [x] MCA subordinate ownership diagnostics
- [x] subordinate progression/energy diagnostics
- [ ] profile validation reports
- [ ] automatic profile reload + loaded-entity reapply
- [ ] pack-maker documentation

## Final Phase - FTB Quests

Deferred until the systems they document are stable.

- [ ] Reincarnation
- [ ] Survival / ecology
- [ ] Skills
- [ ] Naming and subordinates
- [ ] Evolution
- [ ] Civilization / MCA
- [ ] Dungeons
- [ ] Bounties
- [ ] Awakening
- [ ] World bosses
- [ ] Endgame
