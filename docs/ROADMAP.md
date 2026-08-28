# Roadmap

This roadmap is ordered by dependency and content-authoring risk. Existing mods remain authoritative for gameplay state they already own; Re:Monster Integration composes those systems rather than creating competing copies.

Read [`AUTHORITY_MATRIX.md`](AUTHORITY_MATRIX.md) before changing cross-mod state and [`TARGET_PACK_CONFIG.md`](TARGET_PACK_CONFIG.md) before changing target-pack progression balance.

## Status convention

- `[x]` implementation exists
- **runtime-proven** behavior was observed in the real 1.21.1 target modpack
- **runtime-retest pending** a concrete fix exists and compiles but that exact corrected path still needs in-game confirmation
- **content-authoring** infrastructure exists and the remaining work is canonical data/design rather than architecture

# Current milestone - final authority handoff, then canonical content

The architecture/debug era is effectively complete. One final compatibility build remains intentionally unmerged until the combined Townstead + MCA Capitals dialogue path is confirmed in-game.

- [x] keep **Tensura: Skill Obtain 1.1** as the player-facing authored kill-skill drop system
- [x] make Re:Monster kill acquisition permanently **non-player only**
- [x] keep **EPGains 0.0.18** as the sole existence-to-combat-power scaler
- [x] remove Re:Monster's redundant EP-to-vanilla-stat growth implementation
- [x] pin corrected EPGains target-pack rates in `TARGET_PACK_CONFIG.md`
- [x] designate **Tensura: Unique Monsters** for removal; MobChampions is the sole generic elite system
- [x] inspect the real Townstead / MCA Capitals `getTranslatable` overwrite collision
- [x] implement composition order: MCA Capitals political line -> Townstead species/personality voice -> ordinary MCA fallback
- [x] final authority-cleanup branch compiles successfully in CI
- [ ] **runtime-retest pending:** verify the Townstead + MCA Capitals dialogue composition in the full pack
- [ ] merge final authority-cleanup PR after that runtime proof
- [ ] remove `tr_unique_monsters` from the assembled modpack
- [ ] write corrected EPGains values into the assembled pack config

After those operational cleanup items, new work is canonical content authoring rather than proof-of-concept architecture.

## Completed infrastructure batch - runtime proof summary

- [x] arbitrary compatible living entities use native Tensura existence/EP rather than parallel XP
- [x] wild third-party mobs gain individual EP and acquisition state (**runtime-proven: Mobs of Mythology Chupacabra**)
- [x] repeated non-player acquisition can reach a learned skill (**runtime-proven: Iron Golem learned Voice Cannon**)
- [x] external active-skill AI invokes real ManasCore/Tensura activation/cost/cooldown lifecycle (**runtime-proven: Zombie cast Voice Cannon**)
- [x] external aura/magicule compatibility foundation supports native learning and active-skill costs
- [x] Tensura: Skill Obtain and Re:Monster now have deliberately separate player-vs-non-player kill-learning authority
- [x] MobChampions eligibility is universal for compatible `Mob` entities with datapack opt-out
- [x] MobChampions rank projects into real base Max EP, physical scale and native birth skills
- [x] Legendary champions can receive a native Unique Skill
- [x] MCA champions preserve MCA inventory authority rather than invisible MobChampions gear
- [x] champion rank/EP bonus/skills survive controlled physical evolution
- [x] MCA civilian melee/bow/panic compatibility preserves MCA simulation
- [x] MCA subordinate save/reload preserved 39,999 EP + Water Current Control
- [x] MCA Battlewill manual learning uses native Tensura storage
- [x] controlled cross-EntityType evolution preserves portable progression while target body keeps native initialization
- [x] natural single-edge evolution, reusable conditions and owned branch-choice UI were runtime-proven using temporary proof content
- [x] proof-only evolution recipes/species mappings were removed after validation
- [x] player/native race binding uses ManasCore/Tensura as source of truth and synchronizes project/Townstead presentation
- [x] compatibility authority matrix covers the current target modpack

# Phase 0 - Foundation / authority - COMPLETE

- [x] repository + CI + Java 21 / NeoForge 21.1.248 target
- [x] exact Tensura 2.0.1.2 / ManasCore 4.0.0.2 integration
- [x] actual full-pack runtime validation
- [x] entity/profile/existence/skill/ownership/MobChampions diagnostics
- [x] Mob Profile model + JSON reload registry
- [x] optional-mod-safe entity resolution
- [x] real Tensura existence/skill bridges for arbitrary compatible LivingEntity
- [x] Better Subordinates external/MCA storage preservation
- [x] MCA identity/Brain/family/profession/schedule/inventory preservation contract
- [x] native race <-> project body synchronization
- [x] generic controlled physical evolution adapter
- [x] reusable evolution conditions + owned choice UI + natural single-edge trigger
- [x] explicit cross-mod authority matrix
- [x] no production proof/test evolution content
- [ ] add the complete full-pack runtime dependency set for direct local dev launches when useful

# Phase 1 - Native evolution atlas and form adapter - NEXT

Before authoring thousands of evolution edges, inventory how the installed Tensura ecosystem already represents evolution. A Re:Monster edge must delegate to the native mechanism whenever that mechanism already exists.

## Evolution transition taxonomy

Every canonical evolution transition must be classified as one of these forms:

1. **native player race transition** - real ManasCore `ManasRace` evolution
2. **native same-EntityType form transition** - variant/state/name evolution on the existing creature body
3. **physical EntityType transition** - source body becomes a genuinely different registered creature body

Re:Monster must never manufacture a new EntityType merely because a stronger native variant has a different name or texture.

## Exact Tensura findings already confirmed

- [x] Tensura has both same-body `INameEvolution` and true EntityType evolution
- [x] true base-Tensura EntityType chain confirmed: `lesser_daemon -> greater_daemon -> arch_daemon -> daemon_peer`
- [x] base `tensura:direwolf` is one EntityType with a native variant/state evolution machine
- [x] Direwolf native forms include base, Fang, Mystic, Tempest/Star-family variants rather than separate mob EntityTypes
- [x] Direwolf native Fang selection uses biome/environment semantics
- [x] native Direwolf evolution also changes stats/skills/state on the same individual
- [x] other base `INameEvolution` users identified: Arch Daemon, Direwolf, Goblin, Greater Daemon, Hell Moth, Landfish, Lesser Daemon, Lizardman, Orc, Salamander, Slime, Evil Centipede and Tempest Serpent
- [x] Mysticism 2.1.2 playable `DirewolfRace` / Fang / special wolf races contain no hooks into base Tensura `DirewolfEntity`, `DirewolfVariant` or `INameEvolution`
- [x] therefore **Mysticism playable wolf races and base Tensura Direwolf mob forms are separate authorities despite similar names**

## Atlas work

- [ ] inventory every native Tensura `INameEvolution` state machine and its requirements/results
- [ ] inventory every Tensura existence-data EntityType evolution mapping
- [ ] inventory every playable Tensura ManasRace branch and requirement
- [ ] inventory every Mysticism playable race branch/requirement
- [ ] inventory every SlimeThroneExtras/TNO/addon race branch/requirement
- [ ] document which native forms change model/texture only versus stats/skills/AI/body semantics
- [ ] identify native evolution calls that dangerously copy full old-body NBT and decide per route whether direct native execution or the controlled Re:Monster adapter is safer
- [ ] implement a **native-form evolution adapter** so a Re:Monster graph edge can invoke/target an upstream form/variant/state without replacing EntityType
- [ ] expose native-form identity in diagnostics and evolution choice UI
- [ ] runtime-prove at least one same-body form edge through the unified evolution UI/graph
- [ ] runtime-prove one native true EntityType edge through the appropriate authority path

# Phase 2 - Player race revamp and Re:Monster branches

Goal: make the playable race ecosystem feel authored for this pack rather than a loose union of Tensura addons.

Authority: ManasCore/Tensura/addon `ManasRace` remains the real player-race state. Re:Monster supplies visual mapping, cross-mod traits and genuinely new branches where native content has a gap.

## Full race catalog

- [ ] build a machine-readable atlas of every playable race in Tensura + Mysticism + SlimeThroneExtras/TNO/other installed race addons
- [ ] record native parents, evolution requirements, EP bands, skills, attributes and special mechanics
- [ ] classify overlapping/near-duplicate races before authoring new ones
- [ ] identify lore/gameplay dead ends that deserve new Re:Monster-inspired branches

## Visual revamp

- [ ] bind every meaningful native race to a Townstead/project visual body recipe
- [ ] define body scale and proportions
- [ ] define palettes/skin/fur/scales
- [ ] define horns/ears/tusks/tails/wings/markings and other attachments
- [ ] define preserved/gained/lost phenotype traits across evolution
- [ ] add project-owned Townstead roots/genes/attachments only where stock content cannot represent the race
- [ ] validate every final visual mapping in-game

## Trait / branch revamp

- [ ] audit each native race's actual skills/traits before adding anything
- [ ] add only complementary Re:Monster traits where native/addon behavior leaves a real gap
- [ ] design new branches inspired by Re:Monster logic while fitting Tensura's existence/skill model
- [ ] keep native requirement authority when a native target already exists
- [ ] use project conditions for genuinely new branches or deliberate additional restrictions
- [ ] build final Goblin -> Hobgoblin -> higher branch family first as the proving catalog
- [ ] expand race families iteratively until every relevant playable lineage has coherent progression

# Phase 3 - Bestiary Tensura skill integration

Goal: every creature should participate coherently in Tensura's real skill ecosystem.

## Authority order

1. dedicated Tensura compatibility addon for that creature mod
2. skills actually present on the entity at runtime
3. Re:Monster Mob Profile authored effective/innate skills only for uncovered gaps

## Content pass

- [ ] inventory every living creature EntityType in the final target mod roster
- [ ] record native AI, attacks, elements, resistances, habitats, boss status and existing Tensura compatibility
- [ ] assign sensible real Tensura skills to uncovered mobs
- [ ] prefer skills that describe behavior the mob visibly performs rather than arbitrary power inflation
- [ ] avoid assigning skills that dedicated compatibility addons already provide
- [ ] balance kill-acquisition value around the rarity/difficulty of the creature
- [ ] feed final skill identities into Mob Profile themes, learning multipliers, dungeons and future quest metadata

Target creature sources include Tensura/addons, Ice and Fire CE, Wyrmroost, Mowzie's Mobs, Mowzie's Cataclysm, L_Ender's Cataclysm, Mobs of Mythology, Bosses'Rise, Born in Chaos, Legendary Monsters and any later approved fantasy additions.

# Phase 4 - Universal creature evolution atlas

Goal: author coherent evolutionary lifecycles across the entire playable bestiary rather than treating every mod's creatures as isolated content islands.

## Coverage target

- [ ] every relevant living mob in the final game receives an evolution classification
- [ ] creatures that semantically support evolution should generally aim for **at least four meaningful progression tiers** across their full lifecycle
- [ ] the four-tier target is not a mechanical obligation for bosses, constructs, unique beings or creatures where invented filler forms would damage the fiction
- [ ] every dead-end or exception must be intentional and documented

A tier can be represented by:

- another EntityType/body
- a native same-body variant/form/state
- a native player-race transition when working on player lineages
- a deliberately authored new project form/body only when existing content cannot express the lifecycle

## Authoring rules

- [ ] reuse native Tensura form transitions before inventing substitutes
- [ ] reuse compatible creatures from different installed mods when they form a believable stronger/weaker lineage
- [ ] create new Re:Monster-inspired forms only where the roster has a genuine missing link
- [ ] define EP, biome, dimension, skill, light, ownership and other branch conditions where meaningful
- [ ] do not use unexplained random branching when a semantic/environmental condition can distinguish paths
- [ ] keep owned multi-branch evolution player-chosen
- [ ] keep wild branching deterministic from authored conditions
- [ ] preserve portable identity/progression across unavoidable body replacement
- [ ] preserve native AI/body initialization of the destination species
- [ ] preserve MobChampions individual rank/bonuses

## Batch order

- [ ] Tensura native fauna/forms first, because they define the canonical adapter semantics
- [ ] vanilla Minecraft creatures as roots/bridges where useful
- [ ] Ice and Fire CE lifecycle families
- [ ] Wyrmroost lifecycle families
- [ ] Mowzie's Mobs / Mowzie's Cataclysm
- [ ] L_Ender's Cataclysm
- [ ] Mobs of Mythology
- [ ] Born in Chaos
- [ ] Bosses'Rise / Legendary Monsters and other boss-heavy packs with explicit unique/terminal classification
- [ ] cross-mod gap analysis and project-owned missing forms
- [ ] full evolution graph validation for cycles, unreachable nodes, ambiguous wild branches and nonsensical tier regressions

# Phase 5 - Ecology / spawn / aggro director

Ecology now follows the race/bestiary taxonomy rather than preceding it. Final lifecycle tiers, habitats and rarity semantics should inform spawn policy.

## Spawn authority

1. source-mod datapack/biome tags/modifiers first
2. Re:Monster/Paxi data override second
3. InControl only for conditions data cannot express
4. custom runtime spawn hook last

One mob/biome ends with one effective spawn policy.

## Existing foundation

- [x] Tensura natural spawn mechanism + 42 natural spawn modifiers inventoried
- [x] Wyrmroost spawn mechanism/weights/groups/tags inventoried
- [x] Horned Rabbit data override proven
- [x] Wyrmroost data override proven
- [x] semantic habitat/frequency/group-size model
- [x] optional `aggro_range`
- [x] initial Direwolf/Barghest aggro metadata

## Content pass

- [ ] inventory spawn authority for every final creature source mod
- [ ] define ordinary/rare/apex/boss spawn budgets using final evolution tiers
- [ ] dramatically reduce vanilla representation where fantasy fauna should replace it
- [ ] expand curated habitat/biome overrides
- [ ] prevent advanced evolution stages from casually spawning where they should instead be earned/evolved
- [ ] tune Wyrmroost distribution
- [ ] minimum player spawn distance for dangerous mobs where useful
- [ ] normalize ordinary hostile Tensura FOLLOW_RANGE
- [ ] preserve exceptional calamity/boss sensing intentionally
- [ ] reduce fast/flying off-screen dogpiles
- [ ] validate density/performance and run full-pack ecology playtest

# Phase 6 - MCA threat / civilian combat - FOUNDATION COMPLETE

Authority: MCA owns villager Brain/simulation; Re:Monster adds narrow cross-mod threat/equipment compatibility.

- [x] guard enemy sensing bridge for profile-hostile modded mobs
- [x] owned/Tensura allies normally ignored unless actively hostile
- [x] ordinary armed civilians defend themselves/nearby villagers
- [x] strongest real melee weapon selection
- [x] preserve MCA civilian self-defence/panic semantics
- [x] healthy armed engaged civilians suppress only the inappropriate panic presentation
- [x] ordinary civilian native-style bow spacing
- [x] native MCA guards/archers untouched
- [x] MCA MobChampions gear cleanup preserves MCA inventory authority
- [ ] runtime-retest owned/commanded MCA bow firing after the final militia/subordinate cleanup conflict fix
- [ ] broader guard/archer hostile-roster test after bestiary profiles expand

# Phase 7 - Universal subordinate / NPC progression - FOUNDATION COMPLETE

Authority: Tensura/Better Subordinates owns native progression; MCA owns MCA simulation; Re:Monster bridges compatible external bodies.

- [x] generic permanent Tensura owner bridge
- [x] MCA in-place ownership adapter
- [x] Better Subordinates storage compatibility
- [x] external/MCA real Tensura skill/mastery state
- [x] external individual EP/existence growth
- [x] external active-skill fallback through native activation
- [x] MCA subordinate save/reload EP+skill persistence
- [x] MCA Battlewill manual learning
- [x] Follow / Wander / Stay and Neutral / Passive / Aggressive / Protect compatibility controls
- [x] movement/targeting skill gates use native Tensura milestones
- [ ] runtime-retest non-MCA generic manual/tome learning after storage-confirmation fix
- [ ] runtime-verify Better Subordinates counters such as `totalKills` / `damageTaken` on an owned compatible external/MCA subordinate
- [ ] Harvest Festival compatibility
- [ ] consolidated movement/targeting regression on native Tensura + MCA + generic external subordinate
- [ ] horse/Tensura mount interaction regression
- [ ] one third-party custom-inventory subordinate regression

Better Subordinates owns permanent subordinate training/unlocks. Mysticism owns its copy/paste mechanics. Re:Monster does not add a second arbitrary permanent Unique-Skill teaching framework without a deliberate future design.

# Phase 8 - Dungeon / spawner director

Start after canonical bestiary tiers and the first ecology pass exist.

- [ ] YUNG Better Dungeons integration
- [ ] When Dungeons Arise integration
- [ ] useful YUNG Mineshaft/Stronghold spawner opportunities
- [ ] randomized modded-mob pools
- [ ] per-structure / structure-tag pools
- [ ] use final Mob Profile tier/ecology metadata
- [ ] allow MobChampions variants naturally
- [ ] investigate `SpawnPotentials` rewriting for correct cage preview
- [ ] preserve dedicated Tensura boss arenas/structures

# Phase 9 - Quest-system integration

## Tensura Guild - adventurer commission authority

- [ ] inspect extension/data points for modded subjugation targets
- [ ] feed appropriate final Mob Profile targets/tiers into commissions where supported
- [ ] preserve native guild rank/party/commission state

## MCA: Quests - villager/local quest authority

- [ ] author monster-hunt quest templates through MCA: Quests data
- [ ] bridge profile targets/tiers through data/custom objectives where practical
- [ ] preserve MCA quest offer/progress/turn-in/favor authority
- [ ] village projects for fantasy-civilization objectives later

A third generic Bountiful-style bounty system remains intentionally out unless a concrete gap appears.

# Phase 10 - MCA-backed fantasy civilizations

Start after race visuals/traits and ecology provide stable species identities and settlements.

MCA owns citizens; Townstead owns phenotype; Tensura/ManasCore owns race/EP/skills; MCA Capitals owns monarchy/diplomacy; MCA: Quests owns local quest state.

## Goblin civilization first

- [ ] MCA-backed Goblin civilian prototype bound to real native Tensura race
- [ ] final Townstead Goblin identity from Phase 2
- [ ] tribal clothing and guard loadouts
- [ ] preserve useful native Tensura Goblin trades/content
- [ ] native Goblin -> Hobgoblin progression semantics
- [ ] eligible settlements join MCA Capitals rather than separate kingdom state
- [ ] local work/projects through MCA: Quests
- [ ] inheritance/profession/guard/clothing/race-progression runtime test

## Dwarf civilization after Goblin

- [ ] MCA-backed Dwarf adapter bound to real native race
- [ ] final Townstead/Tensura dwarf identity
- [ ] preserve native Dwarf profession/trade/reputation/Gazel mechanics
- [ ] explicit MCA-vs-Tensura work/restock authority
- [ ] MCA Capitals integration
- [ ] dwarf clothing/equipment pools

# Phase 11 - Starting Unique Skill selector

- [ ] inspect every installed addon that changes reincarnation/starting skills
- [ ] intercept only through native ManasCore/Tensura storage
- [ ] searchable direct Unique Skill selection
- [ ] configurable selected-skill count
- [ ] random mode/count
- [ ] blacklist/pool controls across installed registries
- [ ] server-side validation
- [ ] first-login/reset/reincarnation compatibility

# Phase 12 - Polish / pack-author tools

- [ ] config UX
- [x] entity/profile/existence/skill diagnostics
- [x] ownership/MobChampions/FOLLOW_RANGE diagnostics
- [x] targeted profile reapply
- [x] guard/subordinate diagnostics
- [x] authority startup audit
- [ ] profile/evolution graph validation reports
- [ ] automatic profile reload + loaded-entity reapply
- [ ] migration/authority checker for target-pack config and redundant mods
- [ ] pack-maker documentation

# Final Phase - FTB Quests / progression guide

FTB Quests remains a guided progression/book layer after runtime systems stabilize. It must not replace Tensura Guild, MCA: Quests or other authoritative gameplay systems.

- [ ] reincarnation/onboarding
- [ ] race/evolution atlas guidance
- [ ] survival/ecology
- [ ] skills/acquisition
- [ ] naming/subordinates
- [ ] civilizations/MCA
- [ ] dungeons
- [ ] guild/commission guidance
- [ ] awakening
- [ ] world bosses
- [ ] endgame
