# Roadmap

This roadmap is ordered by dependency and risk, not by feature hype. Shared infrastructure comes before content-heavy systems.

## Phase 0 - Foundation

- [x] Create project repository
- [x] Freeze initial runtime: Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21
- [x] Inspect Tensura 2.0.1.2 mod metadata and first internal APIs
- [ ] Add complete local development dependency set
- [ ] Build the addon successfully
- [ ] Launch a test instance and confirm `[Re:Monster Integration] loaded`
- [ ] Add a debug command / diagnostics screen for integration state

## Phase 1 - Shared Mob Profile system

Create one data-driven source of truth for modded mobs.

- [ ] JSON reload listener and validation
- [ ] entity registry resolution
- [ ] optional-mod-safe profiles: missing entity ids are ignored with a warning
- [ ] mob classification: passive / neutral / hostile / elite / boss
- [ ] EP values
- [ ] skills possessed by mob
- [ ] learning multiplier
- [ ] dungeon/spawner weight
- [ ] MCA guard hostility policy
- [ ] ecology / habitat profile
- [ ] tags/themes such as beast, undead, dark_fantasy, aquatic, dragon, construct

Planned profile shape:

```json
{
  "entity": "tensura:horned_rabbit",
  "tier": "normal",
  "ep": 1200,
  "skills": [],
  "learning_multiplier": 1.0,
  "guard_hostile": true,
  "ecology": {
    "habitats": ["plains", "meadow", "forest"],
    "rarity": "common",
    "weight": 70,
    "group_min": 1,
    "group_max": 3
  }
}
```

## Phase 2 - Tensura mob integration

- [ ] Resolve `IExistence` for arbitrary `LivingEntity`
- [ ] assign / normalize EP for supported third-party mobs
- [ ] assign effective Tensura skills to third-party mobs
- [ ] prefer official compatibility behavior when an installed compat already owns a mob
- [ ] avoid double EP / double skill assignment
- [ ] classification and scaling hooks for bosses / MobChampions / Tensura Unique Monsters

Target mob mods include:

- Tensura and installed Tensura addons
- Born in Chaos
- L_Ender's Cataclysm
- Mowzie's Mobs
- Mowzie's Cataclysm
- Mobs of Mythology
- Bosses'Rise
- Ice and Fire Community Edition
- Saint's Dragons when an official 1.21.1 NeoForge build exists
- Opposing Force when an official 1.21.1 NeoForge build exists

## Phase 3 - Skill learning from kills

When a player possesses a Tensura skill but has not learned/mastered it, killing a mob that effectively possesses the skill should grant learning progress.

- [ ] resolve true killer / owner
- [ ] support player direct kills
- [ ] investigate subordinate and clone kills
- [ ] intersect dead mob skills with player's possessed-unlearned skills
- [ ] grant configurable learning progress
- [ ] scale progress by EP / tier / boss / champion state
- [ ] prevent duplicate grants

## Phase 4 - Ecology / Spawn Director

Build a coherent isekai bestiary instead of accepting every mod's independent spawn table.

- [ ] inventory existing natural spawn systems per target mod
- [ ] override Tensura biome tags / NeoForge biome modifiers where possible
- [ ] use InControl for rules that cannot be cleanly data-overridden
- [ ] custom runtime spawn hook only where required
- [ ] habitat groups: plains, forest, taiga, swamp, mountain, desert, coast/ocean, cave, nether, end, cursed, dungeon-only
- [ ] frequency tiers and group sizes
- [ ] minimum player distance where useful
- [ ] curate Born in Chaos so horror/Halloween mobs do not dominate normal fantasy biomes
- [ ] keep elite/boss entities out of ordinary spawn soup unless explicitly intended

Initial design rule:

- generalist fantasy fauna should be visible and reasonably frequent
- biome-characteristic mobs should make regions feel different
- themed / goofy / horror mobs should be restricted to fitting contexts
- bosses and calamity-tier mobs should normally use structures, events or rare conditions

## Phase 5 - MCA Threat Bridge

- [ ] MCA guards attack hostile mobs from supported mods
- [ ] use explicit project hostility classification rather than trusting every mod's broad monster tags
- [ ] detect Tensura subordinate ownership dynamically
- [ ] guards ignore normally-hostile mobs owned by a friendly player
- [ ] active aggression overrides subordinate immunity: a subordinate attacking villagers can still be defended against
- [ ] avoid guards targeting friendly/tamed third-party mobs
- [ ] test archers and melee guards

## Phase 6 - MCA Villager Subordinate Adapter

Goal: make an MCA villager a legitimate Tensura subordinate without replacing the MCA entity or destroying its simulation state.

Preserve:

- [ ] MCA entity identity
- [ ] Brain / AI
- [ ] family and relationships
- [ ] profession and work behavior
- [ ] village / home
- [ ] memories
- [ ] inventory and equipment
- [ ] personality / traits
- [ ] Townstead roots / genetics

Add:

- [ ] Tensura permanent owner
- [ ] EP
- [ ] skills
- [ ] learning / mastery compatibility
- [ ] race state
- [ ] Harvest Festival compatibility
- [ ] evolution compatibility
- [ ] ally/guard checks

Thought Communication must be adapted to MCA semantics instead of replacing MCA scheduling.

## Phase 7 - Dungeon / Spawner Director

- [ ] YUNG Better Dungeons spawner support
- [ ] When Dungeons Arise spawner support
- [ ] randomized modded-mob spawn pools
- [ ] per-structure / structure-tag pools where possible
- [ ] use Mob Profile tier/ecology metadata
- [ ] allow MobChampions / Tensura Unique Monsters modifiers where compatible
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

- [ ] inspect all Tensura race branches and requirements
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

## Phase 13 - Polish and pack authoring tools

- [ ] config UX
- [ ] diagnostics
- [ ] profile validation reports
- [ ] commands for inspecting entity EP / skills / owner / ecology
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
