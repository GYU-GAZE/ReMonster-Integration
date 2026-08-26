# Re:Monster Species Evolution Foundation

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

`minimum_ep` is already enforced through Tensura's real `IExistence` EP storage. The initial Hobgoblin test edges deliberately use zero until progression balance is defined.

### `data/*/remonster_species/*.json`

Physical representation for a semantic species:

```json
{
  "species": "remonster_integration:orc",
  "townstead": {
    "root_candidates": ["townstead_classic:orc"],
    "root_search_terms": ["orc"],
    "preserve_shared_loci": true,
    "clamp_body_metrics": true
  }
}
```

Exact root candidates are tried first. Search terms are a compatibility fallback so a Classic Fantasy Races filename/ID change does not hard-crash the graph.

## Townstead transformation rules

Players and MCA villagers use Townstead's persisted Roots/genotype system.

For an evolution:

1. Seed the target root's genotype using a deterministic seed derived from the individual's persisted phenotype seed plus the target root.
2. Preserve an old allele pair only when the exact old gene also belongs to the target genome at the same locus.
3. Race-specific incompatible loci are replaced by the deterministic target roll.
4. MCA float body metrics are clamped into the target root's allowed ranges instead of being randomly rerolled.
5. MCA villagers mutate in-place. Family, relationships, memories, inventory, profession, personality, ownership and entity UUID are not replaced.
6. Townstead root and expressed-gene packets are sent immediately so the visual transformation does not require relogging.

This makes shared features persist while allowing transformations such as Goblin-specific skin genes to be replaced by Ogre/Tiefling-specific genes. More explicit palette mapping (for example dark green -> dark red rather than only deterministic red-family selection) can be layered on top as per-locus data once the exact Classic Fantasy gene IDs are pinned.

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

Initial manual runtime test:

```text
/remonster townstead_roots
/remonster species set @s remonster_integration:hobgoblin
/remonster evolutions @s
/remonster evolve @s remonster_integration:orc
```

Repeat with `remonster_integration:ogre` after resetting semantic species to Hobgoblin.
