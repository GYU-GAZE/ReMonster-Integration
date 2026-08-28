# Roadmap

This roadmap is ordered by dependency and risk. Existing mods remain authoritative for the gameplay state they already own; Re:Monster Integration bridges those systems instead of creating parallel sources of truth.

See [`AUTHORITY_MATRIX.md`](AUTHORITY_MATRIX.md) before adding or changing cross-mod state.

## Status convention

- `[x]` means implementation exists.
- **runtime-proven** means the behavior was observed working in the actual 1.21.1 target modpack.
- **runtime-retest pending** means a concrete fix exists and compiles but that exact corrected path still needs an in-game confirmation.
- **pack cleanup** means a redundant external mod/config/data artifact should be removed from the assembled modpack rather than duplicated in Re:Monster.

# Current milestone - Ecology / spawn / aggro handoff

The progression/subordinate/evolution infrastructure batch is complete enough to leave architecture-first development. The next major gameplay pass is world ecology: deciding what can spawn, where, how often, how far from the player and how aggressively it can acquire targets.

Before starting new ecology content, the assembled pack should remove the two audited legacy overlaps:

- [ ] **pack cleanup:** remove `Tensura: Skill Obtain` (`tensura_skillobtain`); Re:Monster now owns universal kill-driven acquisition/mastery
- [ ] **pack cleanup:** remove `Tensura: Unique Monsters` (`tr_unique_monsters`); MobChampions is the sole generic random elite authority

Compatibility guards remain in the addon so migration does not require both removals in the same launch.

## Completed progression batch - runtime proof summary

- [x] arbitrary living killers use native Tensura EP/existence rather than a parallel XP system
- [x] wild third-party mobs gain individual EP and acquisition state (**runtime-proven: Mobs of Mythology Chupacabra**)
- [x] repeated kill acquisition can reach a fully learned skill (**runtime-proven: Iron Golem learned Voice Cannon**)
- [x] external active-skill AI can activate real learned Tensura skills through native ability preset/cost/cooldown/lifecycle (**runtime-proven: Zombie cast Voice Cannon**)
- [x] external aura/magicule compatibility foundation supports native learning and active-skill costs
- [x] MobChampions eligibility is universal for compatible `Mob` entities with a datapack opt-out tag (**runtime-proven across vanilla/MCA/modded mobs**)
- [x] MobChampions rank increases the individual's real base Max EP, physical scale and birth-skill package
- [x] Legendary champions additionally receive one native Unique Skill
- [x] MCA champions discard MobChampions-generated invisible equipment while preserving MCA inventory authority
- [x] champion rank, EP bonus and rolled skills persist across controlled physical evolution (**runtime-proven**)
- [x] MCA armed civilian self-defence preserves native MCA simulation and panic semantics (**runtime-proven**)
- [x] ordinary MCA civilian bow approach/kite/emergency spacing works (**runtime-proven**)
- [x] commanded/owned MCA bow conflict was fixed so civilian militia cleanup no longer cancels subordinate draw (**runtime-retest pending for this exact post-fix path**)
- [x] MCA subordinate save/reload preserves native existence and learned skills (**runtime-proven with 39,999 EP + Water Current Control**)
- [x] MCA Battlewill manual learning works through the real Tensura learning path (**runtime-proven**)
- [x] generic external manual/tome bridge only consumes an item after real storage confirmation (**runtime-retest pending on non-MCA body**)
- [x] controlled cross-EntityType evolution preserves portable identity/progression while target body keeps native initialization (**runtime-proven**)
- [x] natural single-edge wild evolution, reusable branch conditions and owned evolution choice UI are implemented and were runtime-proven with temporary canine proof data
- [x] evolution is allowed during combat and controlled replacement preserves the current combat target
- [x] common evolution particles/sound/message work for controlled creature evolution and native ManasCore/Tensura race evolution
- [x] player/native race binding uses ManasCore/Tensura as the source of truth; project semantic/body state synchronizes to it (**runtime-proven**)
- [x] proof-only canine and Hobgoblin->Orc authored evolution recipes were removed after validation; the generic engine remains
- [x] compatibility authority audit added; startup reports authoritative systems and legacy overlaps
- [x] Re:Monster EP-to-vanilla-stat fallback automatically disables itself when EPGains is installed

Remaining narrow regression work from the batch is tracked in the relevant phases below and does not block starting the ecology pass.

# Phase 0 - Foundation

- [x] project repository and CI
- [x] target runtime frozen: Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21
- [x] exact Tensura 2.0.1.2 / ManasCore 4.0.0.2 compile integration
- [x] actual full-modpack runtime validation
- [x] `/remonster inspect <entity>` diagnostics
- [x] `/remonster progression status <entity>` diagnostics
- [x] pack-wide compatibility authority matrix
- [ ] add the complete local runtime dependency set for launching the full pack directly from the development workspace

# Phase 1 - Shared Mob Profile system

- [x] central `MobProfile` model
- [x] JSON parsing/validation/reload registry
- [x] optional-mod-safe entity resolution
- [x] disposition and tier classification
- [x] EP values with `FALLBACK` / `MINIMUM` / `REPLACE` policy
- [x] effective Tensura skill declarations
- [x] learning multiplier
- [x] dungeon/spawner weight
- [x] MCA guard hostility policy
- [x] ecology / habitat profile
- [x] optional aggro/FOLLOW_RANGE override
- [x] semantic themes/tags
- [x] initial Wyrmroost catalog
- [x] initial Legendary Monsters boss profiles
- [ ] automatically reapply changed profiles to already-loaded matching entities after datapack reload
- [ ] author/balance the broader final profile catalog for the installed target roster
- [ ] add profile validation/reporting suitable for pack-author content passes

# Phase 2 - Tensura / cross-mod existence integration

Authority: Tensura owns existence/EP and skills; dedicated compatibility addons own their specific mod pair.

- [x] resolve Tensura existence for arbitrary `LivingEntity`
- [x] safe external EP/existence bridge
- [x] ownership/alliance bridge
- [x] generic profile application on entity join
- [x] safe EP application preserving positive native values by default
- [x] native ManasCore/Tensura skill assignment without duplicates
- [x] consume skills actually present at runtime so dedicated compatibility addons remain authoritative
- [x] Bosses'Rise x Tensura Compatibility designated authoritative for Bosses'Rise Tensura behavior
- [x] Mysticism / SlimeThroneExtras native race+skill registries designated authoritative for their content
- [x] EPGains designated authoritative for EP/MP/AP combat power scaling
- [x] Tensura Modifications designated authoritative for participant EP distribution and regeneration
- [x] explicit authority matrix for current overlapping systems
- [ ] author/balance final EP/profile metadata for remaining third-party mobs
- [ ] author project-owned effective skills only when no dedicated compatibility path exists
- [ ] investigate any remaining roster-specific dedicated Tensura compatibility addons before writing project overrides

Target roster includes Tensura/addons, Cataclysm, Mowzie's Mobs, Mowzie's Cataclysm, Mobs of Mythology, Bosses'Rise, Ice and Fire CE, Wyrmroost, Born in Chaos and future compatible fantasy-mob additions.

# Phase 3 - Universal skill learning from kills

Authority: Re:Monster owns universal acquisition/mastery kill progression; ManasCore/Tensura owns the actual skill instance/storage/lifecycle.

- [x] acquisition state identified as native mastery `< 0`
- [x] exact `LivingEntity` kill credit
- [x] profile-declared + actually-present defeated skills
- [x] profile learning multiplier
- [x] MobChampions learning multiplier
- [x] native `TensuraSkill.addLearnPoint(...)` lifecycle used
- [x] external learning energy handling without permanent artificial cost
- [x] wild third-party acquisition (**runtime-proven**)
- [x] repeated acquisition reaches learned state (**runtime-proven: Iron Golem -> Voice Cannon**)
- [x] legacy `tensura_skillobtain` player path is suppressed while that redundant mod remains installed
- [ ] remove `tensura_skillobtain` from the assembled pack
- [ ] runtime-test unusual skill-specific mode-learning overrides
- [ ] runtime-validate unusual clone/projectile/summon kill-credit implementations
- [ ] expose base learning amount as configurable balance data
- [ ] evaluate EP/profile-tier learning scaling only after ecology/balance playtests

# Phase 4 - Ecology / Spawn / Aggro Director - NEXT MAJOR PASS

Goal: make the world feel like one authored isekai ecosystem instead of independent mod spawn tables stacked together.

## Spawn authority policy

1. source-mod datapack/biome tags/modifiers first;
2. Re:Monster/Paxi datapack override second;
3. InControl only for conditions data cannot cleanly express;
4. custom runtime spawn hook only as a last resort.

One mob/biome should end with one effective spawn policy.

## Existing foundation

- [x] inventory Tensura natural spawn mechanism and 42 natural spawn modifiers
- [x] inventory Wyrmroost spawn mechanism, weights, groups and biome tags
- [x] prove clean Tensura biome override with Horned Rabbit
- [x] confirm Wyrmroost is data-overridable
- [x] semantic habitat groups + frequency/group-size model
- [x] policy keeping bosses/elites out of ordinary spawn soup unless explicitly authored
- [x] initial Horned Rabbit ecology rebalance
- [x] optional profile `aggro_range`
- [x] initial Direwolf/Barghest aggro normalization metadata

## Next implementation order

- [ ] inventory spawn authority for Ice and Fire CE
- [ ] inventory spawn authority for Mowzie's Mobs / Mowzie's Cataclysm
- [ ] inventory spawn authority for Cataclysm
- [ ] inventory spawn authority for Mobs of Mythology
- [ ] inventory spawn authority for Born in Chaos
- [ ] inventory Bosses'Rise / dedicated boss spawn semantics
- [ ] inventory remaining ordinary Tensura fauna and addon mobs
- [ ] define global ordinary-vs-rare-vs-elite/boss spawn budget
- [ ] dramatically reduce vanilla hostile/fauna representation where the fantasy roster should replace it
- [ ] expand curated biome/habitat overrides
- [ ] tune Wyrmroost distribution
- [ ] add minimum player spawn distance for dangerous mobs where needed
- [ ] normalize ordinary hostile Tensura FOLLOW_RANGE
- [ ] preserve intentionally exceptional calamity/boss detection ranges
- [ ] reduce fast/flying off-screen dogpiles
- [ ] validate pack performance and local mob density
- [ ] full-pack ecology playtest

# Phase 5 - MCA threat bridge and civilian combat

Authority: MCA owns villager Brain/simulation; Re:Monster adds narrow cross-mod threat/equipment compatibility.

- [x] hook MCA guard enemy sensing rather than replace guard combat AI
- [x] profile hostile mobs can become guard targets
- [x] owned/Tensura allies normally ignored
- [x] active aggression can override ownership immunity
- [x] ordinary armed MCA civilians defend themselves/nearby villagers (**runtime-proven**)
- [x] strongest real melee weapon selection (**runtime-proven**)
- [x] preserve MCA civilian panic/self-defence package
- [x] hide panic presentation only while healthy/armed/engaged
- [x] restore visible panic below 25% health
- [x] ordinary civilian native-style bow spacing (**runtime-proven**)
- [x] native MCA guards/archers remain untouched
- [x] MCA MobChampions gear-cleanup prevents invisible champion equipment while preserving real MCA inventory
- [ ] runtime-retest owned/commanded MCA bow firing after militia-vs-subordinate cleanup conflict fix
- [ ] runtime-test native MCA guards/archers against a broader modded-hostile sample after ecology profiles expand

# Phase 6 - Universal subordinate / NPC progression adapter

Goal: let non-native bodies participate in real Tensura/Better Subordinates progression without replacing their own simulation systems.

## MCA preservation contract

- [x] MCA entity identity/UUID remains in place
- [x] Brain/AI remains MCA-owned
- [x] family/relationships remain MCA-owned
- [x] profession/work/schedule remains MCA-owned
- [x] village/home/memories remains MCA-owned
- [x] inventory/equipment/personality/Townstead genetics remains MCA/Townstead-owned

## Ownership / progression

- [x] generic permanent Tensura owner bridge
- [x] MCA in-place ownership adapter
- [x] Better Subordinates storage compatibility
- [x] external/MCA real Tensura skill/mastery state
- [x] external individual EP/existence growth
- [x] universal external active-skill AI through native activation (**runtime-proven: Zombie Voice Cannon**)
- [x] MCA subordinate save/reload EP+skill preservation (**runtime-proven**)
- [x] MCA Battlewill manual learning (**runtime-proven**)
- [ ] runtime-retest non-MCA generic manual/tome learning after storage-confirmation fix
- [ ] runtime-verify Better Subordinates counters such as `totalKills` / `damageTaken` on an owned compatible external/MCA subordinate
- [ ] Harvest Festival compatibility

## Command / interaction layer

- [x] preserve useful native screens
- [x] decorate known subordinate screens
- [x] generic fallback overview UI
- [x] owned evolution choice entry point in fallback UI
- [x] Follow / Wander / Stay
- [x] movement gating through Telepathy / EP milestone
- [x] Neutral / Passive / Aggressive / Protect
- [x] targeting gating through Thought Communication mastery
- [x] native Tensura targeting through `SubordinateHelper`
- [x] external/MCA targeting compatibility without replacing MCA Brain
- [ ] consolidated movement/targeting regression on one native Tensura subordinate, one MCA subordinate and one generic external subordinate
- [ ] regression-test horse / Tensura mount native interaction paths
- [ ] regression-test a third-party custom-inventory subordinate

## Skill teaching authority

Better Subordinates owns permanent subordinate training/unlock progression. Mysticism owns its copy/paste mechanics. Re:Monster only bridges real manuals/tomes where a player-only interaction would otherwise block compatible NPC bodies.

Do not add a second arbitrary permanent Unique-Skill teaching framework without a deliberate new game design.

# Phase 7 - Dungeon / Spawner Director

Start after the first ecology pass establishes final habitat/tier pools.

- [ ] YUNG Better Dungeons spawner integration
- [ ] When Dungeons Arise spawner integration
- [ ] inspect YUNG Mineshaft/Stronghold spawner opportunities only where useful
- [ ] randomized modded-mob spawn pools
- [ ] per-structure / structure-tag pools
- [ ] use Mob Profile tier/ecology metadata
- [ ] allow MobChampions variants naturally
- [ ] investigate `SpawnPotentials` rewriting for correct cage preview
- [ ] explicitly preserve Tensura Boss Structure and other dedicated boss arenas

# Phase 8 - Quest-system integration

The old plan for a separate Bountiful generic bounty layer is retired unless a future gap justifies it.

## Tensura Guild - primary adventurer commission authority

- [ ] inspect extension/data points for custom modded subjugation targets
- [ ] feed appropriate Mob Profile targets/tiers into commissions where supported
- [ ] preserve native guild rank/party/commission state
- [ ] use dedicated boss/structure content rather than parallel quest state where Tensura Guild already supports it

## MCA: Quests - villager/local quest authority

- [ ] author monster-hunt quest templates through MCA: Quests datapacks
- [ ] connect Mob Profile targets/tiers through data or custom objective adapters where practical
- [ ] preserve MCA quest offer/progress/turn-in/favor authority
- [ ] evaluate village projects for fantasy-civilization objectives later

# Phase 9 - Starting Unique Skill selector

- [ ] inspect all installed addons that alter reincarnation/starting skills before implementation
- [ ] replace/intercept starting skill selection only through native ManasCore/Tensura storage
- [ ] searchable direct Unique Skill selection
- [ ] configurable selected-skill count
- [ ] random mode + configurable random count
- [ ] blacklist/pool controls across installed skill registries
- [ ] server-side validation
- [ ] first-login/reset/reincarnation compatibility

# Phase 10 - Native race <-> Townstead body bridge

Authority correction: **ManasCore/Tensura native race is the source of truth.** Re:Monster semantic species is a bound projection/body/content key for native player races, not an independent competing player race.

- [x] data-driven species -> native race binding
- [x] setting a bound Re:Monster player species updates the real native race
- [x] native race changes update the bound project species/body state
- [x] native evolution emits common Re:Monster evolution feedback
- [x] Townstead body/root mapping
- [x] deterministic phenotype seed
- [x] preserve compatible visual loci
- [x] Townstead recalculation/sync
- [x] scale adapter
- [x] Hobgoblin / Orc / Ogre native bindings retained as body-mapping foundation

## Deferred visual content pass

- [ ] inventory every meaningful playable native Tensura/Mysticism/SlimeThroneExtras race used by the pack
- [ ] bind each to a Townstead/project-owned visual body recipe
- [ ] scales/palettes/horns/ears/tusks/markings/proportions
- [ ] preserved/gained/lost phenotype traits across native evolution
- [ ] custom roots/attachments only where stock Townstead assets are insufficient
- [ ] validate all final mapped player races in game

# Phase 11 - Expanded player race evolution

Authority: native ManasCore/Tensura race evolution requirements remain authoritative whenever the target race already exists.

- [x] native race/evolution framework located
- [x] project branch injection can expose an additional existing native target race
- [x] native race progress/requirements remain native instead of being overwritten by duplicate Re:Monster percentages
- [x] native evolution -> project species/body/feedback synchronization
- [x] proof branch infrastructure runtime-tested
- [x] proof-only authored Hobgoblin->Orc edge removed after validation
- [ ] inspect final desired race tree across Tensura + Mysticism + SlimeThroneExtras
- [ ] add only genuinely missing project branches
- [ ] use project conditions only as additional restrictions where native target behavior cannot express the design
- [ ] build final Goblin/Hobgoblin/higher content tree after race audit
- [ ] assign final phenotype recipes during visual mapping pass

# Phase 12 - Creature / subordinate evolution

Architecture is implemented and runtime-proven. Current bundled proof recipes were deliberately removed so temporary test balance does not silently become production canon.

- [x] data-driven directed evolution graph
- [x] EP + skill + biome + dimension + ownership + block-light conditions
- [x] external EntityType replacement adapter
- [x] keep target body native AI/Brain/goals/base initialization
- [x] transfer portable Re:Monster/Tensura/Better Subs/tame/name/equipment/health state
- [x] avoid loading full old-body NBT into the new body
- [x] preserve MobChampions rank/individual champion bonuses
- [x] automatic wild evolution for a single unambiguous eligible edge
- [x] deterministic data-authored branch selection rules
- [x] owned evolution choice UI
- [x] evolution can occur in combat and preserve target
- [x] common evolution audiovisual feedback
- [x] temporary canine proof chain runtime-validated then removed from production data
- [ ] inspect native evolution capability of each final candidate before using controlled replacement
- [ ] author final canonical creature evolution catalog from the final mod roster
- [ ] runtime-test owner/tame/Better-Subs persistence across one final canonical owned evolution after content is authored
- [ ] keep debug commands as pack-author diagnostics only

# Phase 13 - MCA-backed fantasy civilizations

Start after ecology establishes where civilizations and threats belong.

MCA remains the citizen simulation authority; Townstead owns phenotype; Tensura/ManasCore owns race/EP/skills; MCA Capitals owns monarchy/diplomacy; MCA: Quests owns villager-local quest state.

## Goblin civilization first

- [ ] MCA-backed Goblin civilian prototype bound to a real native Tensura race
- [ ] Townstead Goblin visual identity
- [ ] tribal clothing pools
- [ ] tribal guard equipment/loadouts
- [ ] preserve relevant native Tensura Goblin trade/content where useful
- [ ] naming + native Goblin -> Hobgoblin semantics
- [ ] bridge eligible settlements into MCA Capitals rather than build separate kingdom state
- [ ] use MCA: Quests for local jobs/projects
- [ ] runtime-test inheritance/professions/guards/clothing/race progression together

## Dwarf civilization after Goblin proves the pattern

- [ ] MCA-backed Dwarf adapter bound to real native race
- [ ] Townstead/Tensura dwarf visual identity
- [ ] preserve compatible native Dwarf profession/trade/reputation/Gazel systems
- [ ] explicitly define MCA-vs-Tensura work/restock authority
- [ ] integrate eligible Dwarf settlements with MCA Capitals
- [ ] dwarf clothing/equipment pools

Do not remove or replace native Tensura entities that still own progression/reputation/content not yet bridged.

# Phase 14 - Polish / pack-author tools

- [ ] config UX
- [x] entity/profile/existence/skill diagnostics
- [x] ownership/MobChampions/FOLLOW_RANGE diagnostics
- [x] targeted profile reapply command
- [x] guard decision diagnostics
- [x] subordinate progression/energy diagnostics
- [x] authority startup audit/warnings
- [ ] profile validation reports
- [ ] automatic profile reload + loaded-entity reapply
- [ ] pack-maker documentation
- [ ] migration checker that reports redundant/unsupported mods and conflicting authority settings

# Final Phase - FTB Quests / progression guide

FTB Quests remains deferred until the gameplay systems it documents are stable. It should act as the pack's guided progression/book layer, not replace runtime systems such as Tensura Guild or MCA: Quests.

- [ ] reincarnation/onboarding
- [ ] survival/ecology
- [ ] skills/acquisition
- [ ] naming/subordinates
- [ ] evolution
- [ ] civilization/MCA
- [ ] dungeons
- [ ] guild/commission guidance
- [ ] awakening
- [ ] world bosses
- [ ] endgame
