# Roadmap

This roadmap is ordered by dependency and risk, not by feature hype. Shared infrastructure comes before content-heavy systems.

## Phase 0 - Foundation

- [x] Create project repository
- [x] Freeze initial runtime: Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21
- [x] Inspect Tensura 2.0.1.2 mod metadata and first internal APIs
- [x] Add CI-safe compile dependencies for exact Tensura 2.0.1.2 + ManasCore 4.0.0.2 through CurseMaven
- [x] Extract ManasCore bundled modules into the compile classpath without committing third-party jars
- [x] Build the addon successfully in CI against real Tensura + ManasCore APIs
- [ ] Add complete local runtime dependency set for launching the full pack from the dev workspace
- [ ] Launch a test instance and confirm `[Re:Monster Integration] loaded`
- [x] Add `/remonster inspect <entity>` diagnostics for profile, EP, ownership layers, skills, MobChampions and FOLLOW_RANGE

## Phase 1 - Shared Mob Profile system

Create one data-driven source of truth for modded mobs.

- [x] Define central `MobProfile` data model
- [x] Define JSON parser + field validation
- [x] Define in-memory profile registry
- [x] JSON reload listener and resource-pack reload registration
- [x] entity registry validation/resolution during reload
- [x] optional-mod-safe profiles: missing entity ids are ignored with a warning
- [x] model mob classification: passive / neutral / hostile + normal / elite / boss / calamity
- [x] model EP values and safe application policy (`FALLBACK`, `MINIMUM`, `REPLACE`)
- [x] model skills possessed by mob
- [x] model learning multiplier
- [x] model dungeon/spawner weight
- [x] model MCA guard hostility policy
- [x] model ecology / habitat profile
- [x] model optional aggro/FOLLOW_RANGE override; `0` preserves native behavior
- [x] model tags/themes such as beast, undead, dark_fantasy, aquatic, dragon, construct
- [x] expand habitat vocabulary with savanna and jungle after source-backed Wyrmroost inventory
- [ ] reapply profiles to every already-loaded matching entity automatically after datapack reload
- [ ] author broader validated profile catalog for supported mob mods
- [x] author initial source-backed Wyrmroost catalog for all 10 living dragon/creature entity types
- [x] author initial Legendary Monsters boss profiles for Cloud Golem, Possessed Paladin and The Obliterator

Current profile shape:

```json
{
  "entity": "tensura:horned_rabbit",
  "disposition": "neutral",
  "tier": "normal",
  "ep": 0,
  "ep_mode": "fallback",
  "skills": [],
  "learning_multiplier": 1.0,
  "guard_hostile": false,
  "aggro_range": 0,
  "dungeon_weight": 1,
  "themes": ["beast", "early_game"],
  "ecology": {
    "natural_spawn": true,
    "habitats": ["plains", "meadow", "forest"],
    "rarity": "common",
    "weight": 70,
    "group_min": 1,
    "group_max": 3
  }
}
```

## Phase 2 - Tensura mob integration

- [x] Resolve `IExistence` for arbitrary `LivingEntity`
- [x] Add a narrow `TensuraExistenceBridge` for EP access
- [x] Add a narrow `TensuraSubordinateBridge` for resolved and raw ownership/alliance access
- [x] Add generic runtime Mob Profile application on entity join
- [x] Add safe EP application that preserves existing positive EP by default
- [x] Add generic ManasCore/Tensura skill assignment without duplicates
- [x] run project application at `EventPriority.LOWEST` so existing compatibility can act first
- [x] consume skills actually present on a mob at runtime so official compatibility addons remain authoritative
- [ ] author / balance EP values for supported third-party mobs
- [ ] author / validate project-owned effective Tensura skills where no good compatibility addon exists
- [x] define explicit Legendary Monsters ownership rule: dedicated Tensura compatibility addon owns EP/skills/casting if runtime validation passes
- [ ] explicit per-mod rules for the remaining official compatibility addons
- [x] detect MobChampions state without recreating its stat system
- [x] use MobChampions rank for kill-learning scaling
- [ ] use MobChampions rank for EP/quest/dungeon reward scaling

Target mob mods include:

- Tensura and installed Tensura addons
- L_Ender's Cataclysm
- Mowzie's Mobs
- Mowzie's Cataclysm
- Mobs of Mythology
- Bosses'Rise
- Ice and Fire Community Edition
- MobChampions as the sole generic random elite/champion variation layer
- Legendary Monsters 2.2.2 on 1.21.1 NeoForge
- Wyrmroost 1.4.1 on 1.21.1 NeoForge
- Saint's Dragons when an official 1.21.1 NeoForge build exists
- Opposing Force when an official 1.21.1 NeoForge build exists

Explicit roster policy:

- Born in Chaos is removed from the pack plan
- Tensura Unique Monsters / Tensura: Unique Monster is removed from the pack plan
- MobChampions owns generic random elite/champion variation
- Legendary Monsters' dedicated Tensura compatibility addon should be preferred for EP/skills if runtime testing confirms it behaves correctly

## Phase 3 - Skill learning from kills

When a player possesses a Tensura skill but is still in Tensura's acquisition-learning state, killing a mob that effectively possesses the skill should grant learning progress.

- [x] Confirm native representation: acquisition-learning skill instance has mastery < 0
- [x] Confirm native primitive: `TensuraSkill.addLearnPoint(...)`
- [x] Add `TensuraSkillBridge` that feeds native learning instead of inventing parallel points
- [x] Add profile-driven `KillLearningService`
- [x] hook `LivingDeathEvent`
- [x] support direct player kills
- [x] resolve generic Tensura subordinate/summon ownership chains back to a `ServerPlayer`
- [x] depth/cycle guard for ownership resolution
- [x] intersect profile skill ids with player's possessed acquisition-learning instances
- [x] union profile-declared skills with skills actually present on the defeated mob at runtime
- [ ] runtime-validate specific clone/summon implementations whose damage source may differ
- [ ] add common/configurable base learning amount
- [x] scale core service by profile learning multiplier
- [x] scale learning by MobChampions rank: uncommon 1.25x, rare 1.5x, epic 2x, legendary 3x
- [ ] additionally scale by EP / profile tier where balance testing supports it
- [ ] runtime-test skills with unusual mode-learning overrides

## Phase 4 - Ecology / Spawn / Aggro Director

Build a coherent isekai bestiary instead of accepting every mod's independent spawn table and aggro rules.

- [x] inventory Tensura 2.0.1.2 natural spawn mechanism and extract all 42 natural spawn modifiers
- [x] inventory Wyrmroost 1.4.1 natural spawn mechanism, weights, group sizes and biome tags
- [ ] inventory existing natural spawn systems for the remaining target mods
- [x] prove clean Tensura biome-tag / NeoForge biome-modifier overrides with Horned Rabbit
- [x] confirm Wyrmroost also uses clean NeoForge biome modifiers/tags suitable for datapack overrides
- [ ] expand biome overrides to the curated Tensura fauna roster
- [ ] tune Wyrmroost distribution only where source defaults conflict with project ecology
- [ ] use InControl for rules that cannot be cleanly data-overridden
- [ ] custom runtime spawn hook only where required
- [x] define semantic habitat groups: plains, meadow, savanna, forest, jungle, taiga, swamp, mountain, desert, coast/ocean, river, cave, nether, end, cursed, dungeon-only
- [x] model frequency tiers and group sizes
- [ ] minimum player distance where useful
- [x] define policy keeping elite/boss entities out of ordinary spawn soup unless explicitly intended
- [x] author first active biome rebalance: Horned Rabbit in plains/meadow/forest, weight 70, groups 1-3
- [x] classify all 10 Wyrmroost living species from their real target goals/taming/spawn source before assigning profiles
- [ ] playtest first active ecology override in the full pack

### Aggro normalization

- [x] inspect Tensura 2.0.1.2 hostile target acquisition instead of assuming vanilla AI
- [x] confirm common Tensura mobs use SmartBrainLib nearby-entity sensing based on `Attributes.FOLLOW_RANGE`
- [x] confirm base Tensura tamable entity FOLLOW_RANGE is 32 blocks
- [x] identify Tensura mobs with larger native ranges, including multiple 64-block entities, Megalodon at 48 and Charybdis at 256
- [x] confirm wild prey targeting treats players as directly valid prey and can propagate a newly acquired target to nearby allies
- [x] add optional Mob Profile `aggro_range` and apply it through the vanilla FOLLOW_RANGE attribute
- [x] start role-based normalization with Direwolf and Barghest instead of blanket-changing all Tensura entities
- [ ] author normalized ranges for the remaining ordinary hostile Tensura fauna
- [ ] keep intentionally exceptional boss/calamity detection where it improves encounters
- [ ] specifically reduce ordinary fast/flying hostile acquisition ranges where they create off-screen dogpiles
- [ ] playtest whether ally target propagation still feels too aggressive after range normalization

Initial design rule:

- generalist fantasy fauna should be visible and reasonably frequent
- biome-characteristic mobs should make regions feel different
- strongly themed mobs should be restricted to fitting contexts
- bosses and calamity-tier mobs should normally use structures, events or rare conditions
- ordinary hostile fauna should usually give the player a reasonable chance to notice or avoid it before pursuit begins

## Phase 5 - MCA Threat Bridge

- [x] inspect MCA 1.21.1 guard target acquisition and current hostile-mob configuration
- [x] hook the central MCA `GuardEnemiesSensor` priority decision instead of rewriting individual combat tasks
- [x] MCA guards can target profile-declared hostile mobs from supported mods
- [x] use explicit project hostility classification rather than trusting every mod's broad monster tags
- [x] expose Tensura subordinate ownership/alliance predicate for the MCA bridge
- [x] guards ignore normally-hostile mobs owned through Tensura by a player
- [x] guards also ignore vanilla/modded `OwnableEntity` companions owned by a player, including tamed Wyrmroost dragons
- [x] active aggression overrides ownership immunity when the mob is directly attacking the guard or another MCA villager
- [x] profile-declared friendly/neutral entities can be excluded from guard targeting
- [x] keep the bridge optional/fail-soft when MCA and/or a companion mod is absent
- [x] add `/remonster guard_priority <guard> <target>` diagnostics
- [ ] runtime-test melee guards and archers in the full pack

## Phase 6 - MCA Villager Subordinate Adapter

Goal: make an MCA villager a legitimate Tensura subordinate without replacing the MCA entity or destroying its simulation state.

Preserve:

- [x] adapter architecture keeps MCA entity identity instead of entity replacement
- [x] adapter architecture leaves Brain / AI untouched
- [x] adapter architecture leaves family and relationships untouched
- [x] adapter architecture leaves profession and work behavior untouched
- [x] adapter architecture leaves village / home untouched
- [x] adapter architecture leaves memories untouched
- [x] adapter architecture leaves inventory and equipment untouched
- [x] adapter architecture leaves personality / traits untouched
- [x] adapter architecture leaves Townstead roots / genetics untouched

Add:

- [x] low-level Tensura permanent-owner write exists without entity replacement
- [x] confirmed Tensura recognizes generic `LivingEntity` ownership before checking native `ISubordinate`
- [x] expose resolved, permanent, temporary and summoner ownership separately for debugging
- [x] add MCA-specific in-place ownership adapter
- [x] add pack-author commands to assign/clear Tensura permanent ownership on an MCA villager
- [x] add `/remonster mca_subordinate status <target>` ownership diagnostics
- [ ] runtime-verify ownership survives save/reload and MCA schedule changes
- [ ] EP progression
- [ ] skills
- [ ] learning / mastery compatibility
- [ ] race state
- [ ] Harvest Festival compatibility
- [ ] evolution compatibility
- [ ] ally/guard runtime checks

Thought Communication must be adapted to MCA semantics instead of replacing MCA scheduling. Initial bytecode inspection indicates native stay/follow/wander mutation is gated behind Tensura's `ISubordinate`, which is promising for an ownership-only MCA adapter, but runtime verification is mandatory.

### Universal subordinate interaction / native-screen slice

- [x] preserve native screens instead of replacing useful inventories/dialogue UIs
- [x] decorate known native screens with HP / EP / SHP / current command + Status / Follow / Wander / Stay
- [x] recognize generic `HasCustomInventoryScreen` entities without species whitelists
- [x] data-driven native-menu tag escape hatch for nonstandard mod GUIs
- [x] remove old empty-hand normal-click Tensura-creature overview path that could steal mount interaction
- [x] recover Re:Monster buttons after MCA `clearWidgets()` layout rebuilds
- [ ] regression-test Tensura humanoid / goblin chrome
- [ ] regression-test MCA chrome after asynchronous layout rebuild
- [ ] regression-test horse normal-click ride + shift-click inventory
- [ ] regression-test Basilisk/Tensura mount normal-click ride + shift-click native MountScreen
- [ ] regression-test one third-party `HasCustomInventoryScreen` mob from the pack
- [ ] regression-test one generic fallback subordinate with no useful native menu
- [ ] gate movement commands behind the agreed Telepathy / 2,000 EP milestone
- [x] add separate targeting modes: Neutral / Passive / Aggressive / Protect
- [x] gate targeting controls through Tensura's native Thought Communication `isMastered()` state
- [x] route native Tensura targeting through `SubordinateHelper`
- [x] add MCA/generic targeting compatibility state without replacing MCA Brain/schedule
- [ ] runtime-test Neutral / Passive / Aggressive / Protect on Tensura, MCA and generic compatible subordinates

See `docs/SUBORDINATE_SYSTEM.md` for the focused contract and test matrix.

## Phase 7 - Dungeon / Spawner Director

- [ ] YUNG Better Dungeons spawner support
- [ ] When Dungeons Arise spawner support
- [ ] randomized modded-mob spawn pools
- [ ] per-structure / structure-tag pools where possible
- [ ] use Mob Profile tier/ecology metadata
- [ ] allow MobChampions variants where compatible
- [ ] investigate rewriting `SpawnPotentials` for correct cage preview

## Phase 8 - Hunt quest integrations

### Bountiful

- [ ] high-frequency kill bounty pool for Tensura / project mobs
- [ ] reward and rarity scaling from Mob Profiles

### MCA: Quests

- [ ] monster-hunt quest templates
- [ ] generate / maintain targets from Mob Profiles where practical

## Phase 9 - Starting Unique Skill selector

- [ ] replace/intercept reincarnation skill selection
- [ ] direct searchable Unique Skill selection
- [ ] configurable number of selected skills
- [ ] random mode
- [ ] configurable random skill count
- [ ] blacklist / pool controls
- [ ] server-side validation
- [ ] compatibility with first login, reset and reincarnation flows

## Phase 10 - Townstead <-> Tensura race visual bridge

Player gameplay remains Tensura-driven. Townstead primarily supplies visual/genetic representation for the player while its normal gameplay traits remain relevant to villagers.

- [ ] race mapping model
- [ ] project-specific Townstead roots/species/lineages
- [ ] Tensura race -> Townstead visual state sync
- [ ] preserve visual genes where evolution permits
- [ ] skin/hair/eyes/horns/ears/markings/proportions palettes
- [ ] multiplayer sync

## Phase 11 - Expanded race evolution

- [x] locate Tensura native race/evolution framework and requirement classes
- [ ] inspect all target Tensura race branches and concrete requirement values
- [ ] add project branches where useful
- [ ] assign visual profile per stage
- [ ] example: Goblin -> Hobgoblin -> Enlightened Hobgoblin / Ogre branches
- [ ] avoid replacing semantic race identity merely to borrow Townstead assets

## Phase 12 - Subordinate EP evolution

- [ ] data-driven evolution graph
- [ ] threshold / condition evaluation
- [ ] normal Tensura mobs: prefer native evolution path if exposed
- [ ] preserve owner, EP, skills, name, equipment and state when entity replacement is unavoidable
- [ ] MCA villagers: never replace with a Tensura entity; update Tensura race + visual state in place

## Phase 13 - MCA-backed fantasy civilizations

This is intentionally late. It depends on the Townstead race/visual bridge, expanded Tensura race evolution and stable MCA subordinate progression. Do not treat it as a simple entity swap.

### Goblin civilization

- [ ] prototype MCA-backed Goblin civilian species only after the race/evolution bridges are stable
- [ ] make every MCA-backed Goblin use the project Townstead Goblin race/species identity
- [ ] restrict Goblin clothing pools to race-appropriate / tribal clothing instead of the full MCA wardrobe
- [ ] replace MCA Goblin guard equipment/loadouts with tribal equipment
- [ ] preserve Tensura Goblin trade catalog on the MCA-backed Goblin
- [ ] preserve Goblin naming and Goblin -> Hobgoblin evolution semantics
- [ ] preserve relevant Tensura EP / skills / race state on the MCA-backed Goblin
- [ ] preserve MCA family, genetics, relationships, work, memories and schedule
- [ ] runtime-test family inheritance, clothing, professions, guards and evolution together

### Dwarf civilization

Only start after Goblin proves the architecture.

- [ ] design MCA-backed Dwarf species adapter
- [ ] map the Townstead/Tensura dwarf race identity and visual genetics
- [ ] reuse `DwarfProfession` trade listings where compatible instead of manually cloning tables
- [ ] preserve Dwarven Reputation changes, price effects, trade refusal/hostility and Gazel Dwargo progression
- [ ] map Tensura dwarf professions including trainers / merchant / miner / lumberjack / guard semantics
- [ ] decide explicitly which native dwarf worktime/restock/trader-level behaviors MCA supersedes and which remain bridged
- [ ] define dwarf clothing and guard/equipment pools separately from Goblin civilization rules

Do not remove native `DwarfEntity` from the pack until the reputation/profession/Gazel compatibility contract is complete.

## Phase 14 - Polish and pack authoring tools

- [ ] config UX
- [x] initial entity diagnostics command
- [x] expose runtime FOLLOW_RANGE, ownership layers and MobChampions in entity diagnostics
- [x] add targeted profile reapply command
- [x] add guard-decision diagnostics
- [x] add MCA subordinate ownership-status diagnostics
- [ ] profile validation reports
- [ ] automatic profile reload/reapply command for loaded entities
- [ ] pack maker documentation

## Final Phase - FTB Quests

Deferred intentionally until the systems it documents are stable.

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
