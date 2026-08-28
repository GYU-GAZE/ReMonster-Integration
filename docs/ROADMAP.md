# Roadmap

This roadmap is ordered by dependency and content-authoring risk. Existing mods remain authoritative for gameplay state they already own; Re:Monster Integration composes those systems rather than creating competing copies.

Read [`AUTHORITY_MATRIX.md`](AUTHORITY_MATRIX.md) before changing cross-mod state and [`TARGET_PACK_CONFIG.md`](TARGET_PACK_CONFIG.md) before changing target-pack progression balance.

## Status convention

- `[x]` implementation exists
- **runtime-proven** behavior was observed in the real 1.21.1 target modpack
- **runtime-retest pending** a concrete fix exists and compiles but that exact corrected path still needs in-game confirmation
- **content-authoring** infrastructure exists and remaining work is canonical data/design rather than architecture

# Current milestone - final authority handoff -> canonical content

The architecture/debug era is complete except for one narrow runtime regression: the new Townstead + MCA Capitals dialogue compositor must be confirmed in the full pack before its cleanup PR merges.

- [x] Re:Monster is the single universal kill-learning authority for **players and non-players**
- [x] `tensura_skillobtain` designated for target-pack removal to avoid a second player-only EntityType -> skill table
- [x] Mob Profile / dedicated compat skills remain the canonical statement of what a creature actually possesses
- [x] keep **EPGains 0.0.18** as the sole EP/MP/AP -> combat-power scaler
- [x] remove Re:Monster's redundant EP -> vanilla-stat growth implementation entirely
- [x] pin corrected/subtle continuous EPGains rates in `TARGET_PACK_CONFIG.md`
- [x] designate **Tensura: Unique Monsters** for removal; MobChampions is the sole generic elite system
- [x] inspect exact Townstead / MCA Capitals `getTranslatable` overwrite collision
- [x] implement composition order: MCA Capitals political line -> Townstead species/personality voice -> ordinary MCA fallback
- [x] proof/test evolution content removed from production
- [ ] **runtime-retest pending:** verify Townstead species dialogue and MCA Capitals political dialogue both work in the same full-pack launch
- [ ] merge final authority-cleanup PR after the dialogue proof
- [ ] remove `tensura_skillobtain` and `tr_unique_monsters` from the assembled pack
- [ ] write target EPGains values into `config/epgains/config.toml`

After this narrow operational cleanup, new work is canonical content authoring rather than architecture proof-of-concept work.

## Completed infrastructure proof summary

- [x] arbitrary compatible living entities use native Tensura existence/EP rather than parallel XP
- [x] wild third-party mobs gain individual EP and acquisition state (**runtime-proven: Mobs of Mythology Chupacabra**)
- [x] repeated acquisition reaches a learned native skill (**runtime-proven: Iron Golem learned Voice Cannon**)
- [x] external active-skill AI invokes real ManasCore/Tensura activation/cost/cooldown lifecycle (**runtime-proven: Zombie cast Voice Cannon**)
- [x] external aura/magicule compatibility foundation supports native learning and active-skill costs
- [x] MobChampions eligibility is universal for compatible `Mob` entities with datapack opt-out
- [x] MobChampions rank projects into real Max EP, physical scale and native birth skills
- [x] champion rank/skills survive controlled physical evolution
- [x] MCA civilian melee/bow/panic compatibility preserves MCA simulation
- [x] MCA subordinate save/reload preserved 39,999 EP + Water Current Control
- [x] MCA Battlewill manual learning uses native Tensura storage
- [x] controlled cross-EntityType evolution preserves portable progression while target body keeps native initialization
- [x] natural single-edge evolution, reusable conditions and owned branch-choice UI were runtime-proven with temporary proof content
- [x] player/native race binding uses ManasCore/Tensura as source of truth and synchronizes project/Townstead presentation

# Phase 0 - Foundation / authority - COMPLETE

- [x] repository + CI + Java 21 / NeoForge 21.1.248 target
- [x] exact Tensura 2.0.1.2 / ManasCore 4.0.0.2 integration
- [x] actual full-pack runtime validation
- [x] entity/profile/existence/skill/ownership/MobChampions diagnostics
- [x] Mob Profile model + JSON reload registry
- [x] optional-mod-safe entity resolution
- [x] real Tensura existence/skill bridges for arbitrary compatible `LivingEntity`
- [x] Better Subordinates external/MCA storage preservation
- [x] MCA identity/Brain/family/profession/schedule/inventory preservation contract
- [x] native player race <-> project body synchronization
- [x] generic controlled physical evolution adapter
- [x] reusable evolution conditions + owned choice UI + natural single-edge trigger
- [x] explicit cross-mod authority matrix
- [x] no production proof/test evolution data
- [ ] add the complete full-pack runtime dependency set for direct local dev launches when useful

# Phase 1 - Native evolution atlas + form adapter - NEXT

Before authoring thousands of evolution edges, inventory how the installed Tensura ecosystem already represents evolution. A Re:Monster edge delegates to the native mechanism whenever that mechanism already exists.

## Evolution transition taxonomy

Every canonical transition must be classified as one of:

1. **native player race transition** - real ManasCore `ManasRace` evolution
2. **native same-EntityType form transition** - variant/state/name evolution on the existing creature body
3. **physical EntityType transition** - source becomes a genuinely different registered creature body

A different texture/name is not sufficient reason to manufacture a new EntityType.

## Exact Tensura findings already confirmed

- [x] Tensura uses both same-body `INameEvolution` and true EntityType evolution
- [x] true base-Tensura EntityType chain confirmed: `lesser_daemon -> greater_daemon -> arch_daemon -> daemon_peer`
- [x] `tensura:direwolf` is one EntityType with a native variant/state evolution machine
- [x] Direwolf variants include base Direwolf, Fang, Mystic and Tempest/Star-family forms
- [x] Direwolf Fang selection uses biome/environment semantics
- [x] native Direwolf `evolve()` increments an evolution state, buffs the same entity and mutates `DirewolfVariant`
- [x] base Direwolf `getMaxEvolutionState()` is 3
- [x] the same entity persists; Fang/Mystic/Tempest/Star are **not separate mob EntityTypes**
- [x] Mysticism playable Direwolf/Fang/special-wolf **player races** are a separate ManasRace system despite similar names
- [x] other base `INameEvolution` users include Arch Daemon, Direwolf, Goblin, Greater Daemon, Hell Moth, Landfish, Lesser Daemon, Lizardman, Orc, Salamander, Slime, Evil Centipede and Tempest Serpent

## Atlas / adapter work

- [ ] inventory every native Tensura `INameEvolution` state machine and exact requirements/results
- [ ] inventory every Tensura EntityType evolution mapping
- [ ] inventory every playable Tensura race branch + requirement
- [ ] inventory Mysticism / TNO / other installed addon race branches
- [ ] document which native forms change texture/model only versus stats/skills/AI/body semantics
- [ ] identify unsafe native replacement paths that copy full NBT and choose native vs controlled adapter per route
- [ ] add a **native-form evolution adapter** so graph edges can target upstream form/variant/state without replacing EntityType
- [ ] expose native form identity in diagnostics and owned evolution UI
- [ ] runtime-prove one same-body native form edge through the unified graph/UI
- [ ] runtime-prove one native true EntityType edge through the appropriate authority path

# Phase 2 - Player race revamp + Re:Monster branches

Goal: turn the current loose union of Tensura/addon races into a coherent Re:Monster-style playable race ecosystem without duplicating ManasCore race state.

## Full race catalog

- [ ] machine-readable atlas of every playable race in Tensura + Mysticism + TNO/other installed race addons
- [ ] record native parent, evolution requirements, EP bands, skills, attributes and special mechanics
- [ ] identify near-duplicates/overlaps before creating project races
- [ ] identify dead ends and missing branches that fit Re:Monster logic

## Visual revamp

- [ ] bind every meaningful native race to a Townstead/project body recipe
- [ ] define scale/proportions
- [ ] define palettes/skin/fur/scales
- [ ] define horns/ears/tusks/tails/wings/markings/attachments
- [ ] define phenotype traits gained/lost/preserved across evolution
- [ ] create project Townstead roots/genes/attachments only where stock content is insufficient
- [ ] validate every final race body in-game

## Traits + new branches

- [ ] audit each native race's actual traits/skills before adding project behavior
- [ ] preserve native/addon mechanics where they already express the race well
- [ ] add complementary Re:Monster-style traits only for real gaps
- [ ] author genuinely new branches that fit Tensura existence/skill semantics
- [ ] keep native requirement authority for existing native targets
- [ ] use Re:Monster conditions for genuinely new branches
- [ ] build Goblin -> Hobgoblin -> higher family first as the canonical proving lineage
- [ ] expand until every meaningful playable lineage has coherent progression

# Phase 3 - Bestiary Tensura skill integration

Goal: every final creature participates coherently in Tensura's real skill ecosystem.

## Authority order

1. dedicated Tensura compatibility addon for that creature mod
2. skills actually present on the entity at runtime
3. Re:Monster Mob Profile innate/effective skills for uncovered gaps

## Content pass

- [ ] inventory every living EntityType in the final target roster
- [ ] record attacks/elements/resistances/habitat/boss status/native AI/existing Tensura compat
- [ ] assign sensible real Tensura skills to uncovered mobs
- [ ] put authored skills on the creature's real native skill storage
- [ ] prefer skills that describe abilities the mob visibly performs
- [ ] avoid duplicating skills already supplied by dedicated compatibility addons
- [ ] balance learning value around rarity/difficulty
- [ ] optionally add **per-skill acquisition chance** to canonical profile data if playtesting wants Skill Obtain-style rarity without a second registry
- [ ] feed final identities into Mob Profile themes, learning multipliers, dungeons and quests

Primary creature sources: Tensura/addons, vanilla roots where useful, Ice and Fire CE, Wyrmroost, Mowzie's Mobs, Mowzie's Cataclysm, L_Ender's Cataclysm, Mobs of Mythology, Bosses'Rise, Born in Chaos, Legendary Monsters and later approved fantasy additions.

# Phase 4 - Universal creature evolution atlas

Goal: author believable evolutionary lifecycles across the entire final bestiary rather than leaving each mod as an isolated island.

## Coverage target

- [ ] every relevant living mob receives an evolution classification
- [ ] creatures that semantically support evolution should generally target **at least four meaningful lifecycle tiers**
- [ ] bosses, constructs, unique beings and intentionally terminal organisms may have fewer tiers when filler forms would damage the fiction
- [ ] every exception/dead end is intentional and documented

A tier may be represented by:

- another real EntityType
- a native same-body variant/form/state
- a native player-race transition for playable lineages
- a new project-owned form/body when the installed roster has a genuine missing link

## Authoring rules

- [ ] reuse native Tensura forms before inventing substitutes
- [ ] reuse believable cross-mod weak/strong relatives when appropriate
- [ ] create new Re:Monster-inspired forms only for genuine roster gaps
- [ ] author EP/biome/dimension/skill/light/ownership/other semantic conditions
- [ ] deterministic semantic/environmental branching preferred over unexplained RNG
- [ ] owned multi-branch evolution remains player-chosen
- [ ] wild branching remains deterministic from authored conditions
- [ ] preserve portable identity/progression across physical replacement
- [ ] destination retains its native AI/body initialization
- [ ] MobChampions individual rank/bonuses survive every supported transition

## Batch order

- [ ] Tensura native fauna/forms first because they define native-form adapter semantics
- [ ] vanilla creatures as roots/bridges where useful
- [ ] Ice and Fire CE families
- [ ] Wyrmroost families
- [ ] Mowzie's Mobs / Mowzie's Cataclysm
- [ ] L_Ender's Cataclysm
- [ ] Mobs of Mythology
- [ ] Born in Chaos
- [ ] Bosses'Rise / Legendary Monsters / other boss-heavy packs with explicit terminal/unique classification
- [ ] cross-mod gap analysis + new project forms
- [ ] validate graph for cycles, unreachable nodes, ambiguous wild branches and tier regressions

# Phase 5 - Ecology / spawn / aggro director

Ecology follows the final race/bestiary taxonomy so lifecycle tier, habitat and rarity semantics drive spawn policy.

## Spawn authority

1. source-mod datapack/biome tags/modifiers
2. Re:Monster/Paxi data overrides
3. InControl for conditions data cannot express
4. custom runtime hook last

One mob/biome ends with one effective spawn policy.

- [x] Tensura natural spawn mechanism + 42 modifiers inventoried
- [x] Wyrmroost spawn mechanism/weights/groups/tags inventoried
- [x] Horned Rabbit and Wyrmroost data overrides proven
- [x] semantic habitat/frequency/group-size model
- [x] optional `aggro_range`
- [ ] inventory spawn authority for every final creature mod
- [ ] define ordinary/rare/apex/boss budgets using evolution tiers
- [ ] reduce vanilla representation where fantasy fauna should replace it
- [ ] prevent high evolution stages from casually spawning where they should be earned
- [ ] expand curated habitat/biome overrides
- [ ] minimum player spawn distance for dangerous mobs where useful
- [ ] normalize ordinary hostile FOLLOW_RANGE while preserving intentional calamity/boss sensing
- [ ] reduce fast/flying off-screen dogpiles
- [ ] full-pack density/performance/ecology playtest

# Phase 6 - MCA threat / civilian combat - FOUNDATION COMPLETE

- [x] profile-hostile modded mobs bridge into MCA guard sensing
- [x] ownership/alliance handling
- [x] ordinary armed civilian melee + bow self-defence
- [x] native MCA panic semantics preserved
- [x] native guards/archers remain MCA-owned
- [x] MCA inventory remains authoritative
- [ ] runtime-retest owned/commanded MCA bow after final cleanup conflict fix
- [ ] broader guard/archer hostile-roster test after bestiary expansion

# Phase 7 - Universal subordinate / NPC progression - FOUNDATION COMPLETE

- [x] generic permanent Tensura owner bridge
- [x] MCA in-place ownership adapter
- [x] Better Subordinates storage compatibility
- [x] external/MCA real Tensura skills/mastery/existence
- [x] external active-skill fallback through native activation
- [x] MCA subordinate save/reload EP+skill persistence
- [x] MCA Battlewill manual learning
- [x] Follow/Wander/Stay + Neutral/Passive/Aggressive/Protect controls
- [ ] non-MCA generic manual/tome regression
- [ ] Better Subordinates `totalKills` / `damageTaken` runtime verification on external/MCA subordinate
- [ ] Harvest Festival compatibility
- [ ] consolidated movement/targeting regression on Tensura + MCA + generic external subordinate
- [ ] mount/custom-inventory regressions

# Phase 8 - Dungeon / spawner director

Start after evolution/ecology establish final tier/habitat pools.

- [ ] YUNG Better Dungeons support
- [ ] When Dungeons Arise support
- [ ] randomized modded-mob pools
- [ ] per-structure / structure-tag pools
- [ ] use Mob Profile tier/ecology metadata
- [ ] allow MobChampions variants naturally
- [ ] preserve dedicated Tensura boss structures/arenas
- [ ] investigate `SpawnPotentials` rewriting for cage preview

# Phase 9 - Quest integration

## Tensura Guild

- [ ] inspect extension/data points for custom modded subjugation targets
- [ ] feed appropriate Mob Profile targets/tiers into commissions
- [ ] preserve native guild rank/party/commission state

## MCA: Quests

- [ ] monster-hunt/local fantasy-civilization quest templates
- [ ] connect Mob Profile targets/tiers where practical
- [ ] preserve MCA quest offer/progress/turn-in/favor authority

# Phase 10 - Starting Unique Skill selector

- [ ] inspect installed starting-skill/reincarnation addons first
- [ ] searchable direct Unique Skill selection through native ManasCore/Tensura storage
- [ ] configurable selected-skill count
- [ ] random mode + configurable count
- [ ] blacklist/pool controls across installed skill registries
- [ ] server-side validation
- [ ] first-login/reset/reincarnation compatibility

# Phase 11 - MCA-backed fantasy civilizations

Start after race/body and creature progression catalogs are stable.

- [ ] Goblin MCA civilization prototype
- [ ] fantasy race/Townstead identity while retaining MCA family/work/schedule
- [ ] race-appropriate clothing/guard equipment/trades/names
- [ ] integrate with MCA Capitals rather than creating parallel kingdom state
- [ ] Dwarf civilization after Goblin proves the architecture

# Phase 12 - Polish / pack-author tools

- [ ] profile/evolution validation reports
- [ ] loaded-entity profile reapply after reload
- [ ] graph validation tooling
- [ ] content-author documentation
- [ ] pack-hygiene pass for stale optional recipes/tags/mixins in the assembled pack

# Final Phase - FTB Quests

Deferred until the systems it documents are stable.

- [ ] Reincarnation
- [ ] Survival/ecology
- [ ] Skills
- [ ] Naming/subordinates
- [ ] Evolution
- [ ] Civilization/MCA
- [ ] Dungeons
- [ ] Guild/quests
- [ ] Awakening/world bosses/endgame
