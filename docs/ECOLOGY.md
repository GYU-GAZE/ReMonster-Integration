# Ecology / Spawn Director

## Goal

Make the installed mob mods feel like one isekai bestiary. Biomes should have recognizable fauna, generalist fantasy creatures should actually be encountered, and strongly themed mobs should appear only where they make sense.

Current vanilla/mod spawn tables are treated as input, not canon.

## Mechanism priority

Use the least invasive mechanism that can express the desired rule:

1. datapack biome tags / NeoForge biome modifiers
2. mod configs
3. InControl
4. custom runtime spawn interception

Tensura 2.0.1.2 already uses `data/tensura/neoforge/biome_modifier/*_spawn.json` plus `data/tensura/tags/worldgen/biome/*_spawn.json`, which makes it a good target for clean datapack overrides.

## Habitat vocabulary

Project profile habitats currently include:

```text
PLAINS
MEADOW
FOREST
TAIGA
SWAMP
MOUNTAIN
DESERT
COAST
OCEAN
RIVER
CAVE
NETHER
END
CURSED
DUNGEON_ONLY
```

These are semantic habitats. Later mapping code/datapacks will translate them into actual biome ids/tags.

## Encounter philosophy

### Generalists

Creatures that visually make sense across large parts of a fantasy overworld should have broad habitat coverage and meaningful spawn weight.

Examples:

- cattledeer
- horned rabbit
- slimes
- hound dogs / fantasy canids
- kobolds where appropriate
- common insects / minor beasts

### Regional identity

Some mobs should strongly communicate biome change.

Examples:

- lizardmen, leech lizards, aqua frogs -> swamps/wetlands
- deathworms / sandworms / desert monsters -> desert/badlands
- trolls / cyclopes / some bears -> mountains/taiga
- aquatic monsters -> rivers/coasts/ocean
- dragons -> restricted high-tier habitats/regions/structures

### Threat tiers

Ordinary world spawn should not become boss soup.

- normal -> natural fauna and regular monsters
- elite -> uncommon/rare natural conditions or dungeons
- boss -> structures/events/very rare conditions
- calamity -> explicit world event/endgame placement unless a design exception is documented

## Initial biome sketches

These are design starting points, not final balance numbers.

### Plains / meadow

Expected visible population:

```text
very common / common
- cattledeer
- horned rabbit
- slime
- hound dog
- selected kobolds / small fantasy humanoids

uncommon
- direwolf packs
- giant ant
- selected orc scouts

rare
- pegasus/unicorn depending on biome
- elite/champion variant
```

Plains should work as an early-game isekai ecosystem rather than feeling empty until a hostile pack suddenly spawns.

### Forest

```text
- horned rabbit
- horned/giant bear depending on tier
- direwolf
- blade tiger
- black/knight spiders where fitting
- one-eyed owl
- foliaath and similar fantasy flora
- kobolds
```

### Taiga

```text
- bears
- direwolf
- horned rabbit
- cold-region predators
- selected fantasy night encounters where they fit the pack tone
```

### Swamp

```text
- aqua frog
- hover lizard
- leech lizard
- lizardman
- hydra where appropriately rare
- corpse/glutton-style fish from retained compatible mods where applicable
- spore creatures
```

### Mountain

```text
- pegasus
- unicorn
- giant/horned bears
- troll
- cyclops
- feathered serpent
- rare dragon encounters/territories
```

### Desert / badlands

```text
- armorsaurus
- basilisk
- knight spider
- orcs depending on region design
- deathworm
- sandworm
- high-tier Cataclysm desert content as structure/boss encounters
```

### Cave

```text
- giant bat
- black spider
- evil centipede
- underground fantasy fauna
- dungeon-specific undead
```

## Tensura baseline observations

The inspected 2.0.1.2 jar currently has examples such as:

- Horned Rabbit: weight 20, group 1-2, primarily taiga / woodland-mansion-tag / cherry grove selectors
- Horned Bear: weight 20, group 1, taiga/hill/cherry-grove selectors
- Cattledeer: weight 82, group 1-4
- Direwolf: weight 10, group 3-7 and very broad biome coverage

Full extracted modifier summary is stored in `reference/tensura_natural_spawns_2.0.1.2.csv`.

This explains why a player can see many direwolves in bursts while effectively never encountering Horned Rabbits during plains play despite both being iconic early fantasy fauna.

## Future balancing inputs

Ecology balancing should eventually use:

- observed spawn counts during real play
- EP reward per mob
- pack/group size
- combat danger
- day/night availability
- biome area prevalence
- dungeon frequency
- quest/bounty demand

Spawn frequency and EP economy must be balanced together. Making a high-EP mob common is also an indirect progression buff.
