# Re:Monster Species Evolution Foundation

Status: **foundation complete and runtime-proven for the player path**.

The remaining race-by-race visual work is intentionally deferred to the later **Playable Race Visual Mapping** content pass in `docs/ROADMAP.md`.

## Identity model

Evolution keeps two concepts separate:

- **Semantic species** — persistent Re:Monster gameplay identity (`remonster_integration:ogre`, etc.).
- **Physical body** — adapter-specific representation used to render/materialize that species.

An MCA villager can therefore remain the exact same MCA entity and become semantically an Ogre while Townstead renders it with a Tiefling-derived body. Native Tensura mobs and future external-mob adapters can materialize that same semantic species differently.

## Persistent species state

`SpeciesStateService` stores:

- `remonster_species`
- `remonster_phenotype_seed`

The phenotype seed is stable across save/reload and is copied when the player entity is cloned. Future replacement-based adapters must copy it too.

## Data directories

### `data/*/remonster_evolutions/*.json`

A directed evolution edge:

```json
{
  "from": "remonster_integration:hobgoblin",
  "to": "remonster_integration:orc",
  "minimum_ep": 0
}
```

`minimum_ep` is enforced through Tensura's real `IExistence` EP storage. The initial Hobgoblin test edges deliberately use zero until progression balance is defined.

### `data/*/remonster_species/*.json`

Physical representation for a semantic species:

```json
{
  "species": "remonster_integration:orc",
  "townstead": {
    "root_candidates": [
      "townstead_classic:wild_orc",
      "townstead_classic:blood_orc"
    ],
    "root_search_terms": ["orc"],
    "preserve_shared_loci": true,
    "clamp_body_metrics": true,
    "scale": 1.15
  }
}
```

Exact root candidates are tried first. Search terms are a compatibility fallback so a Classic Fantasy Races filename/ID change does not hard-crash the graph.

Runtime-confirmed Classic Fantasy roots currently used by the prototype profiles:

- Hobgoblin body approximation: `townstead_classic:goblin`
- Orc: `townstead_classic:wild_orc`, with `townstead_classic:blood_orc` fallback
- Ogre body approximation: `townstead_classic:tiefling`

Current prototype Townstead body scales:

- Hobgoblin: `1.00`
- Orc: `1.15`
- Ogre: `1.25`

Scale is applied as a dedicated Re:Monster modifier on Minecraft's native `SCALE` attribute, so the bridge does not overwrite scale state owned by Tensura or another mod.

## Townstead transformation rules

Players and MCA villagers use Townstead's persisted Roots/genotype system.

For an evolution:

1. Seed the target root's genotype using a deterministic seed derived from the individual's persisted phenotype seed plus the target root.
2. Preserve an old allele pair only when the exact old gene also belongs to the target genome at the same locus.
3. Race-specific incompatible loci are replaced by the deterministic target roll.
4. Recalculate Townstead phenotype/powers from the transformed genetics, so physical traits and racial abilities can change naturally with the new body/genome.
5. MCA float body metrics are clamped into the target root's allowed ranges instead of being randomly rerolled.
6. MCA villagers mutate in-place. Family, relationships, memories, inventory, profession, personality, ownership and entity UUID are not replaced.
7. Townstead root and expressed-gene packets are sent immediately so the visual transformation does not require relogging.
8. A Townstead-specific scale modifier is applied without touching other mods' scale modifiers.

This makes shared features persist where the target genetics support them while allowing incompatible race-specific traits to disappear. Runtime behavior already confirmed this pattern: changing from a Goblin-derived body to Orc can drop Goblin night vision, while a Tiefling-derived target can still express night vision when that target genetics provides it.

Townstead's genetics/attachment model also supports later hybrid phenotype recipes, including mixing compatible loci/attachments from different fantasy races. That capability is intentionally not being exhaustively mapped during this foundation phase.

More explicit transformations such as exact palette-relative mapping (`dark green -> dark red`) or hand-authored combinations of horns/ears/tusks are deferred to the Playable Race Visual Mapping pass.

## Runtime proof

Player runtime testing has confirmed the complete pipeline:

- semantic Hobgoblin state can evolve through a loaded data-driven edge
- Hobgoblin -> Orc changes the visible Townstead body to `townstead_classic:wild_orc`
- Hobgoblin -> Ogre changes to the Tiefling-derived body adapter
- the target body's Townstead traits/powers are recalculated
- Townstead-specific body scale applies correctly
- the semantic species remains separate from the borrowed Townstead body identity

`/remonster species set` remains a semantic/debug state command and deliberately does not materialize the physical body by itself. Physical transformation belongs to the evolution/body-adapter path.

## Diagnostic commands

Permission level 2:

```text
/remonster species get <target>
/remonster species set <target> <species>
/remonster species clear <target>
/remonster evolutions <target>
/remonster evolve <target> <target_species>
/remonster townstead_roots
```

Basic manual smoke test:

```text
/remonster townstead_roots
/remonster species set @s remonster_integration:hobgoblin
/remonster evolutions @s
/remonster evolve @s remonster_integration:orc
```

Repeat with `remonster_integration:ogre` after resetting semantic species to Hobgoblin.
