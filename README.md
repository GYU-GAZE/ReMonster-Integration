# Re:Monster Integration

Compatibility and progression layer for the Re:Monster-style Minecraft 1.21.1 NeoForge modpack.

The project treats Tensura: Reincarnated as the gameplay spine and integrates MCA Reborn, Townstead, dungeon mods and external mob mods into one coherent isekai ecosystem.

## Target runtime

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Tensura: Reincarnated 2.0.1.2
- MCA Reborn 7.7.36-beta.3
- Townstead 0.7.6+1.21.1

Exact compatibility versions live in `docs/COMPATIBILITY.md`.

## Core systems

- data-driven Mob Profiles
- cross-mod EP and skill assignment
- kill -> skill learning progress
- ecology / biome spawn director
- MCA guard threat bridge
- MCA villager subordinate adapter
- subordinate EP evolution
- Tensura <-> Townstead race/visual bridge
- expanded Tensura race evolution paths
- dungeon spawner integration
- Bountiful and MCA: Quests hunting content
- configurable starting Unique Skill selector
- FTB Quests book after the core systems are stable

See `docs/ROADMAP.md` and `docs/ARCHITECTURE.md`.

## Third-party jars

Third-party mod jars are intentionally not committed to this repository. Tensura 2.0.1.2 declares `All Rights Reserved` in its mod metadata.

For local development, place required development jars under `libs/`. The directory is ignored by Git. The Gradle project is set up to consume local jars from that directory while we replace them with Maven dependencies where practical.

## Status

Foundation / API reconnaissance. The first implementation milestone is a loadable NeoForge addon plus the shared Mob Profile and Tensura subordinate detection infrastructure.
