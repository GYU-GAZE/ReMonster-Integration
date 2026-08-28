# Tensura 2.0.1.2 Direwolf native evolution atlas

Status: **bytecode-audited against the exact target-pack artifact** `curse.maven:tensura-reincarnated-643695:8665599`.

Relevant upstream classes:

- `io.github.manasmods.tensura.entity.monster.DirewolfEntity`
- `io.github.manasmods.tensura.entity.variant.DirewolfVariant`

This document records upstream facts. Re:Monster balance requirements such as canonical EP thresholds are intentionally not invented here.

## Physical identity

There is one physical entity type: `tensura:direwolf`.

Fang, Mystic, Tempest and Star-family forms are native state on that same entity. Re:Monster must therefore preserve the existing entity and use `native_form` targets rather than manufacture replacement EntityTypes.

## Variant IDs

| ID | Variant |
|---:|---|
| 0 | `DIREWOLF` |
| 1 | `RED_FANG` |
| 2 | `MYSTIC_FIRE_WOLF` |
| 3 | `GREEN_FANG` |
| 4 | `MYSTIC_WIND_WOLF` |
| 5 | `BLUE_FANG` |
| 6 | `MYSTIC_WATER_WOLF` |
| 7 | `BROWN_FANG` |
| 8 | `MYSTIC_EARTH_WOLF` |
| 9 | `PURPLE_FANG` |
| 10 | `MYSTIC_SPACE_WOLF` |
| 11 | `BLACK_FANG` |
| 12 | `MYSTIC_WOLF` |
| 13 | `TEMPEST_WOLF` |
| 14 | `STAR_WOLF` |
| 15 | `TEMPEST_STAR_WOLF` |

`STAR_WOLF` and `TEMPEST_STAR_WOLF` use the Tempest Wolf model family with distinct birthmark state.

## Native environmental selector

For a base `DIREWOLF`, `getEvolutionByBiomes()` evaluates in this exact priority order:

1. `isStar()` -> `STAR_WOLF`
2. Nether, ruined-portal-desert, badlands or savanna -> `RED_FANG`
3. ocean, beach, river or ruined-portal-swamp -> `BLUE_FANG`
4. forest or jungle -> `GREEN_FANG`
5. End-tagged biome or mountain -> `PURPLE_FANG`
6. hill or block Y <= 64 -> `BROWN_FANG`
7. fallback -> `BLACK_FANG`

The order matters. The same location may satisfy lower-priority predicates, but upstream returns the first match.

### Re:Monster authority policy

Do not duplicate this selector as a second independently maintained table of biome conditions.

The Re:Monster graph owns:

- whether an evolution edge exists
- when the individual satisfies project progression requirements
- the evolution UI / owned-character choice surface
- suppression of upstream autonomous `evolve()` calls

The `tensura:direwolf` native-form adapter owns validation of native branch semantics. For a base Direwolf target it invokes the exact private upstream `getEvolutionByBiomes()` through a Mixin `@Invoker` and exposes only the graph target that matches the native result.

This gives one source of truth for the native environment rules while keeping transition timing under Re:Monster authority.

## Native `evolve()` state machine

`getMaxEvolutionState()` returns `3`.

A successful native evolution step mutates the existing entity in-place. The audited implementation increments evolution state while below max and applies native stat/scale changes rather than replacing the entity.

Observed ordinary evolution buff behavior includes:

- scale `+0.5`
- max health `+10`
- attack `+12`
- movement speed `+0.05`
- swim speed `+1`
- jump strength `+0.2`

Re:Monster must call upstream `evolve()` for supported native transitions rather than reproduce these changes manually.

### Variant transition rules

For base `DIREWOLF`:

- variant becomes the result of `getEvolutionByBiomes()`

For a non-base variant:

- variant ID >= 14 -> `TEMPEST_STAR_WOLF`
- otherwise an odd variant ID -> `DirewolfVariant.byId(id + 1)`
- otherwise the variant does not change during that step

This produces the elemental chains:

- `RED_FANG -> MYSTIC_FIRE_WOLF`
- `GREEN_FANG -> MYSTIC_WIND_WOLF`
- `BLUE_FANG -> MYSTIC_WATER_WOLF`
- `BROWN_FANG -> MYSTIC_EARTH_WOLF`
- `PURPLE_FANG -> MYSTIC_SPACE_WOLF`
- `BLACK_FANG -> MYSTIC_WOLF`

and the special chain:

- `TEMPEST_WOLF -> STAR_WOLF`
- `STAR_WOLF -> TEMPEST_STAR_WOLF`

Because Mystic variants have even IDs below 14, an additional native evolution-state step can occur without changing the visible variant. Example:

`DIREWOLF state 0 -> FANG state 1 -> MYSTIC state 2 -> same MYSTIC state 3`

That final state-3 step is a real native stat/state progression but **not a new form**. Do not invent `Mystic Wolf II`, `Super Mystic Wolf`, etc. merely to expose it as another species node. If the project later wants to preserve that final native step, model it as non-form native-state progression rather than a fake species.

## Native skill grants in high forms

When the resulting/current variant ID is >= 14, upstream grants `Shadow Motion`.

During the high-form transition, a Star-marked individual can additionally receive:

- `Black Lightning`
- `Ultra Instinct`

These grants belong to upstream `evolve()` and must not be duplicated by Re:Monster.

## Tempest / Star origin semantics

`TEMPEST_WOLF` is primarily a special pack/spawn variant in upstream Tensura, not an ordinary result of the base biome selector.

During Direwolf pack spawn initialization, upstream can roll an evolved pack. An evolved pack can then roll a Tempest Wolf pack; otherwise it may use an elemental Fang result or Black Fang. Tempest Wolf packs can roll a Star birthmark on an individual.

The entity carries explicit synchronized booleans including `STAR` and `ALPHA` plus its `EVOLUTION_STATE` and variant ID.

Consequences for the Re:Monster graph:

- elemental Fang selection can be delegated exactly to `getEvolutionByBiomes()`
- naturally existing `TEMPEST_WOLF`, `STAR_WOLF` and `TEMPEST_STAR_WOLF` remain resolvable as distinct semantic native-form species
- `TEMPEST_WOLF -> STAR_WOLF -> TEMPEST_STAR_WOLF` is a real native form chain
- `DIREWOLF -> TEMPEST_WOLF` is **not** an upstream biome-evolution edge and therefore needs an explicit project-authored rule if Re:Monster wants normal Direwolves to earn that branch
- a Star-marked base Direwolf is selected as `STAR_WOLF` before all biome checks

Do not silently turn the upstream special-spawn chance into a normal zero-cost evolution edge.

## Element Core form switching

Upstream also contains `getVariantFromCore(ItemStack)` behavior for `BLACK_FANG`:

- Earth core -> `BROWN_FANG`
- Fire core -> `RED_FANG`
- Wind core -> `GREEN_FANG`
- Water core -> `BLUE_FANG`
- Space core -> `PURPLE_FANG`

This is a native manual form-change mechanic, not the same path as `evolve()`. It should be audited separately before Re:Monster decides whether to leave it native or route it through graph authority.

## Adapter safety rule

Do not call native `evolve()` and then forcibly replace the selected Fang with another `setVariant()` result.

`setVariant()` has native variant-specific side effects. A transient environment-selected form followed by a forced graph form risks applying the wrong form effects before the correction.

Instead:

1. graph edge satisfies project requirements
2. native-form eligibility asks upstream which base form is valid
3. only the matching target is considered eligible
4. adapter authorizes one upstream `evolve()` call
5. adapter asserts that the actual resulting form equals the graph target
6. semantic species is synchronized to that already-real native form

## Canine spine status

Canonical physical/semantic spine:

`minecraft:wolf -> tensura:hound_dog -> tensura:direwolf -> native Direwolf forms`

The body identities are registered. Runtime edges must not receive placeholder EP thresholds merely to make the atlas look complete. Canonical timing/EP requirements are a separate content/balance decision.
