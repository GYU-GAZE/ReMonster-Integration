# Universal Subordinate System

This document is the focused hand-off for Re:Monster's cross-mod subordinate system. It records the runtime contract, the native-screen policy, the progression decisions that were previously only present in development chat, and the immediate validation queue.

## Target runtime

- Minecraft 1.21.1
- NeoForge 21.1.248
- Tensura: Reincarnated 2.0.1.2
- Tensura: Better Subordinates 1.2.4
- MCA Reborn 7.7.36-beta.3

## Core contract

A subordinate keeps its original entity and original simulation whenever possible.

- Tensura ownership is the canonical cross-mod ownership layer.
- Native Tensura subordinates keep native Tensura command state and AI.
- MCA villagers remain MCA entities. Do not replace their Brain, schedule, family, profession, home, memories, inventory, personality or traits.
- Vanilla/modded pets and mounts keep their native entity and inventory behavior; Re:Monster only supplies the missing command bridge.
- Better Subordinates is used for its progression/status infrastructure where the target can participate safely.

The universal command layer currently normalizes these movement states:

- `FOLLOW`
- `WANDER`
- `STAY`

Routing is native-first:

1. real Tensura subordinate -> Tensura `SubordinateHelper`
2. MCA subordinate -> MCA movement adapter without replacing its Brain
3. supported vanilla/modded pet or mount -> small Re:Monster compatibility state
4. another complete `ISubordinate` implementation -> native Tensura helper fallback

## Native-screen policy

Do not replace a useful native GUI merely to make every subordinate look identical.

Known native screens are decorated with Re:Monster companion chrome containing:

- target name
- HP / max HP
- EP
- SHP
- current movement command
- `Status`
- `Follow`
- `Wander`
- `Stay`

The native contents remain underneath and continue to own inventory, MCA interactions and mod-specific controls.

### Status

`Status` routes through the Better Subordinates status bridge when available.

Tensura humanoids such as goblins also have a native Status control, but the Re:Monster header can visually cover the native control. The project therefore keeps the Re:Monster `Status` button available on those screens instead of relying on the hidden native button.

### MCA screen rebuilds

MCA `AbstractDynamicScreen#setLayout()` calls `clearWidgets()` when interaction data/layout changes. This can happen after NeoForge `ScreenEvent.Init.Post`, which previously produced this exact failure mode:

1. Re:Monster adds `Status` / `Follow` / `Wander` / `Stay`.
2. MCA receives interaction data.
3. MCA rebuilds its layout and clears all widgets.
4. The Re:Monster info panel remains because it is rendered independently in `ScreenEvent.Render.Post`.
5. All four buttons disappear.

Current fix: during `ScreenEvent.Render.Pre`, Re:Monster checks whether each chrome button is still attached and re-adds only buttons discarded by the native screen. This is intentionally generic so other native screens that rebuild widgets receive the same protection.

Implemented in commit `bd27b854` (`Fix native companion chrome persistence`). CI build 169 passed.

## Progression design

The previously agreed gameplay progression for subordinate orders is:

1. Naming / valid ownership establishes the subordinate relationship.
2. At the Telepathy / 2,000 EP progression milestone, movement orders become the meaningful command layer:
   - `Stay`
   - `Follow`
   - `Wander`
3. After Thought Communication is mastered, targeting/combat-behavior orders expand to:
   - `Neutral`
   - `Passive`
   - `Aggressive`
   - `Protect`

The movement bridge exists today. The progression gate and the four targeting/combat-behavior modes still need to be integrated into the universal UI/authorization layer.

Tensura already exposes the relevant native helpers:

- `SubordinateHelper.setNeutral`
- `SubordinateHelper.setPassive`
- `SubordinateHelper.setAggressive`
- `SubordinateHelper.setProtect`

Thought Communication also has separate movement and targeting command paths. Re:Monster should reuse those semantics instead of inventing unrelated combat stances.

For MCA, targeting commands must be adapted deliberately. Do not transplant TensuraTamableEntity combat automation or destroy MCA scheduling just to make the four modes work.

## Current implementation status

Already implemented:

- canonical Tensura ownership for generic living entities
- MCA in-place subordinate ownership adapter
- Better Subordinates progression storage attachment for MCA without entity replacement
- native Tensura / MCA / vanilla pet / horse movement routing
- `FOLLOW`, `WANDER`, `STAY` state and UI controls
- target HP / EP / SHP / command snapshot
- Better Subordinates Status bridge
- native-screen context hand-off, including server-authoritative context for MCA
- native inventory/menu preservation policy
- fallback subordinate menu for entities without a useful native GUI
- recovery of companion chrome after native screens clear/rebuild widgets

A full runtime battery for the universal subordinate system was reported passing before the latest native-screen chrome/context changes. Those latest GUI changes must therefore be revalidated rather than treating the older pass as proof of the new path.

## Immediate runtime validation queue

### Tensura humanoid / goblin

- [ ] Re:Monster `Status` is visible and clickable.
- [ ] HP / EP / SHP / command info is visible.
- [ ] `Follow`, `Wander`, `Stay` remain visible and clickable.
- [ ] native inventory/menu still works.
- [ ] opening Status reaches the intended Better Subordinates/native status path.

### MCA subordinate

- [ ] HP / EP / SHP / command info is visible.
- [ ] `Status`, `Follow`, `Wander`, `Stay` are visible after MCA interaction data arrives.
- [ ] buttons remain present after MCA calls `setLayout()` / `clearWidgets()`.
- [ ] each movement command actually updates the villager and reflected command snapshot.
- [ ] MCA interaction/family/relationship UI remains usable.
- [ ] closing/reopening or moving between MCA screens does not attach chrome to the wrong villager.

### Generic subordinate families

- [ ] vanilla tameable: movement states still route correctly.
- [ ] horse/native mount: inventory remains native and movement bridge still works.
- [ ] Tensura mount: native mount screen remains usable with chrome.
- [ ] fallback living entity with no useful native menu still reaches the generic subordinate menu.

## Next implementation slice

After the native-screen regression tests are green:

1. enforce/represent the agreed Telepathy / 2,000 EP movement-command progression gate in the universal command layer
2. add universal targeting mode model: `NEUTRAL`, `PASSIVE`, `AGGRESSIVE`, `PROTECT`
3. delegate native Tensura targeting modes to `SubordinateHelper`
4. design the MCA targeting adapter so it cooperates with MCA Brain/schedule rather than replacing them
5. expose targeting controls only after the agreed Thought Communication mastery condition
6. add targeting mode to the subordinate snapshot/status UI
7. runtime-test targeting behavior for Tensura mobs, MCA villagers and generic compatible subordinates

After that, return to the larger project roadmap: MCA subordinate EP/skills/learning/race/evolution work, ecology/profile authoring, dungeon/spawner integration and the other core systems. FTB Quests remains intentionally last.
