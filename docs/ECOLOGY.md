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

Wyrmroost 1.4.1 also uses NeoForge biome modifiers plus biome tags for its natural dragon population, so its ecology can normally be tuned without invasive AI/spawn mixins.

## Habitat vocabulary

Project profile habitats currently include:

```text
PLAINS
MEADOW
SAVANNA
FOREST
JUNGLE
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
- dragons -> species-specific habitats, restricted high-tier regions or structures

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
- overworld drake where its species role remains appropriate

rare
- pegasus/unicorn depending on biome
- alpine in meadow/mountain transition
- elite/champion variant
```

Plains should work as an early-game isekai ecosystem rather than feeling empty until a hostile pack suddenly spawns.

### Savanna

```text
- overworld drake
- selected generalist beasts
- dry-region predators that do not require true desert
```

Savanna is kept distinct from plains so dragon/fauna distribution does not need to be flattened into one giant overworld bucket.

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
- roost stalker
```

### Jungle

```text
- dragon fruit drake
- dense tropical fantasy fauna
- selected reptiles/insects where their ecology fits
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
- canari wyvern as territorial swamp dragon
- hydra where appropriately rare
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
- alpine
- royal red at rare mountain-peak territory
```

### Desert / badlands

```text
- armorsaurus
- basilisk
- knight spider
- orcs depending on region design
- lesser desertwyrm
- deathworm
- sandworm
- high-tier Cataclysm desert content as structure/boss encounters
```

### Coast / ocean

```text
- silver glider around ocean/beach/stony shore
- butterfly leviathan as rare large aquatic dragon
- other aquatic fantasy fauna by depth/region where practical
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

## Wyrmroost baseline observations

Source-backed 1.4.1 natural spawn data currently gives:

| Species | Native habitat | Weight | Group | Project behavior class |
|---|---|---:|---:|---|
| Lesser Desertwyrm | desert | 13 | 1-3 | neutral/passive burrowing fauna |
| Overworld Drake | plains + savanna | 8 | 1-3 | wild hostile predator; tameable |
| Silver Glider | ocean + beach + stony shore | 10 | 1-4 | passive/avoidant; tameable |
| Roost Stalker | plains + mountain slope + forest | 7 | 2-9 | neutral predator/scavenger; tameable |
| Dragon Fruit Drake | jungle | 23 | 4-5 | adult wild hostile; babies tameable |
| Canari Wyvern | swamp | 9 | 2-5 | territorial neutral; attacks player only after close threat escalation |
| Royal Red | mountain peak | 1 | 1 | rare elite wild predator; tameable through its own knockout flow |
| Alpine | mountain peak + meadow | 2 | 1-4 | neutral; naturally targets nectar-carrying bees rather than players |
| Butterfly Leviathan | ocean | 1 | 1 | rare aquatic elite; wild targets player/guardian/squid in water; tameable |
| Coin Dragon | no natural biome modifier | - | - | passive collectible/special entity |

Important integration rule:

- wild species behavior comes from the Wyrmroost entity's actual goals
- tame state stays authoritative in Wyrmroost
- MCA guard logic separately recognizes player-owned `OwnableEntity` instances, so a species may be hostile while wild and protected as a companion after taming

The first Mob Profiles preserve Wyrmroost's source-backed spawn weights/group sizes rather than immediately rebalance everything. Playtest data should decide which native distributions actually need intervention.

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
