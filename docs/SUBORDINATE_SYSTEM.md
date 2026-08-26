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

Movement and combat posture are independent command axes.

Movement:

- `FOLLOW`
- `WANDER`
- `STAY`

Targeting / combat posture:

- `NEUTRAL`
- `PASSIVE`
- `AGGRESSIVE`
- `PROTECT`

This separation is intentional. Examples such as `FOLLOW + PROTECT`, `STAY + PASSIVE` and `WANDER + AGGRESSIVE` are valid state combinations.

Movement routing is native-first:

1. real Tensura subordinate -> Tensura `SubordinateHelper`
2. MCA subordinate -> MCA movement adapter without replacing its Brain
3. supported vanilla/modded pet or mount -> small Re:Monster compatibility state
4. another complete `ISubordinate` implementation -> native Tensura helper fallback

Targeting routing is also native-first:

1. real Tensura `ISubordinate` -> Tensura's native behaviour integer + `SubordinateHelper`
2. MCA subordinate -> Re:Monster selects the posture/target but feeds MCA Brain memories so MCA's own combat tasks remain responsible for execution
3. another named/owned `Mob` -> small persistent compatibility state; the original mob AI remains responsible for whether/how it can physically attack

## Native-screen policy

Do not replace a useful native GUI merely to make every subordinate look identical.

Known native screens are decorated with Re:Monster companion chrome containing:

- target name
- HP / max HP
- EP
- SHP
- current movement command
- current targeting mode or `LOCKED`
- `Status`
- `Follow`
- `Wander`
- `Stay`
- `Neutral`
- `Passive`
- `Aggressive`
- `Protect`

The targeting row remains disabled until the owner has mastered Thought Communication. The native contents remain underneath and continue to own inventory, MCA interactions and mod-specific controls.

### Native input ownership

Re:Monster must not steal an entity's primary interaction merely to expose subordinate controls.

- ordinary right-click belongs to the owning mod's normal interaction path
- if a mount uses ordinary right-click to ride, riding remains ordinary right-click
- when Re:Monster intentionally normalizes a rideable inventory entity to horse-style controls, secondary-use / shift-right-click owns the native inventory
- Re:Monster decorates the native screen after it opens rather than opening a replacement screen first
- the lightweight fallback subordinate screen is allowed only when the complete native/item interaction chain returned `PASS`

An older implementation deliberately opened the Re:Monster overview on empty-hand normal right-click for non-humanoid `TensuraTamableEntity` instances. That behavior could steal the Basilisk's riding interaction and has been removed. The current `SubordinateMenuEvents` path only sends an ownership-authorized screen context and never cancels the native interaction.

Tensura 2.0.1.2 `TensuraMountEntity` implements `HasCustomInventoryScreen` and owns its `MountScreen` / `MountMenu`, but exact-jar inspection exposed a bad input split for this pack: for an owned rideable mount, `getRidingInteraction` mounts before reaching its secondary-use branch; when the branch is reached, secondary-use returns `PASS`, which then falls through to `cycleCommands`, while non-secondary-use opens the inventory. Re:Monster therefore patches the class base rather than Basilisk specifically: secondary-use by the owner opens `openCustomInventoryScreen`, while every normal-click chest/shears/saddle/riding path remains native Tensura behavior. The redundant mount command-cycle gesture is replaced by the Follow/Wander/Stay controls already present in the companion chrome.

Implemented by `TensuraMountInventoryInteractionMixin` and registered in the main mixin set. The compile/build step passed against exact Tensura 2.0.1.2; runtime input behavior still needs the pack regression test below.

### Generic native inventory coverage

Native inventory support must be capability/data driven rather than a species whitelist.

`SubordinateMenuPolicy` currently treats all of these as native-menu entities:

- Tensura humanoids
- vanilla `AbstractHorse`
- any `HasCustomInventoryScreen`
- MCA villagers
- any entity type in `#remonster_integration:native_subordinate_menu`

The generic client matcher also accepts `HasCustomInventoryScreen` entities whose owning mod opens an `AbstractContainerScreen`.

This means Basilisk is not special-cased as the only modded inventory creature. Any third-party mob that follows the standard vanilla custom-inventory contract is automatically eligible for native-screen decoration. The entity-type tag is the escape hatch for mods with useful native GUIs that do not implement that contract.

Input semantics are intentionally not overwritten for every third-party `HasCustomInventoryScreen` entity. A non-rideable inventory creature may legitimately use normal right-click for its container, while a rideable creature may have additional item or command gestures. Re:Monster preserves the owning mod's interaction until runtime testing proves a collision. If another rideable mod has the same inventory-versus-riding conflict as Tensura mounts, add a capability/tag-driven compatibility adapter rather than a Java species whitelist.

### Status

`Status` routes through the Better Subordinates status bridge when available.

Tensura humanoids such as goblins also have a native Status control, but the Re:Monster header can visually cover the native control. The project therefore keeps the Re:Monster `Status` button available on those screens instead of relying on the hidden native button.

### MCA screen rebuilds

MCA `AbstractDynamicScreen#setLayout()` calls `clearWidgets()` when interaction data/layout changes. This can happen after NeoForge `ScreenEvent.Init.Post`, which previously produced this exact failure mode:

1. Re:Monster adds its companion buttons.
2. MCA receives interaction data.
3. MCA rebuilds its layout and clears all widgets.
4. The Re:Monster info panel remains because it is rendered independently in `ScreenEvent.Render.Post`.
5. The companion buttons disappear.

Current fix: during `ScreenEvent.Render.Pre`, Re:Monster checks whether each chrome button is still attached and re-adds only buttons discarded by the native screen. The recovery path now covers Status, all three movement controls and all four targeting controls. This is intentionally generic so other native screens that rebuild widgets receive the same protection.

The original chrome persistence fix was implemented in commit `bd27b854` (`Fix native companion chrome persistence`).

## Progression design

The agreed gameplay progression for subordinate orders is:

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

The movement bridge exists. Its Telepathy / 2,000 EP authorization gate is still pending.

The targeting layer is now implemented and uses Tensura's exact native Thought Communication mastery rule rather than a project-owned threshold. Exact Tensura 2.0.1.2 bytecode inspection confirmed `ThoughtCommunicationSkill#nextMode` gates targeting through `ManasSkillInstance.isMastered(LivingEntity)`. Re:Monster resolves the owner's `CommonSkills.THOUGHT_COMMUNICATION` instance and calls the same `isMastered` predicate.

Exact Tensura targeting behaviour ids are preserved:

- `0` -> `NEUTRAL`
- `1` -> `PASSIVE`
- `2` -> `AGGRESSIVE`
- `3` -> `PROTECT`

Native Tensura routing uses:

- `SubordinateHelper.setNeutral`
- `SubordinateHelper.setPassive`
- `SubordinateHelper.setAggressive`
- `SubordinateHelper.setProtect`

Exact-jar inspection also confirmed `SubordinateHelper.removeTarget` clears target/anger and relevant Brain combat memories when changing away from an active target state. The external compatibility layer mirrors that cleanup where applicable.

### Targeting semantics

The project follows Tensura's native meaning rather than inventing unrelated stances.

- `NEUTRAL`: retaliates / reacts to threats and can follow the owner's attacker, last attacked target or active Mob target.
- `PASSIVE`: does not choose or retain an attack target.
- `AGGRESSIVE`: may acquire the nearest valid non-allied living target.
- `PROTECT`: includes Neutral behavior and additionally seeks mobs currently attacking a protected ally.
- `STAY` / ordered-to-sit suppresses external targeting, matching the native Tensura invariant.

### MCA targeting adapter

MCA targeting must cooperate with MCA Brain/schedule rather than replace them.

For MCA villagers, Re:Monster owns only the universal posture decision and target selection. When a target is chosen it supplies the existing MCA/vanilla Brain memories used by MCA combat packages, including `ATTACK_TARGET`, `NEAREST_HOSTILE`, `LOOK_TARGET` and `WALK_TARGET`. MCA remains responsible for its own combat tasks, movement execution, schedule, family simulation and profession behavior.

Changing to a posture that should drop combat clears the relevant target/Brain state instead of leaving a Passive villager chasing a stale enemy.

Do not transplant TensuraTamableEntity Haki/combat automation into MCA villagers.

## MCA-backed fantasy humanoids

This work is intentionally deferred to the late `MCA-backed fantasy civilizations` roadmap phase. It depends on Townstead race visuals/genetics, expanded Tensura race evolution and stable subordinate progression. It is not part of the current universal subordinate slice.

The eventual Goblin integration includes more than replacing the entity: MCA-backed Goblins must use the project Townstead Goblin race/species identity, use restricted tribal clothing pools, use tribal guard equipment/loadouts, preserve Tensura Goblin trades/naming/evolution/EP/skills/race state and still retain MCA family/work/schedule behavior.

Dwarves are even more coupled to Tensura systems. A future MCA-backed Dwarf adapter must preserve Dwarven Reputation, price/hostility effects, Gazel Dwargo progression, Tensura professions and trades before native `DwarfEntity` can be removed.

## Current implementation status

Already implemented:

- canonical Tensura ownership for generic living entities
- MCA in-place subordinate ownership adapter
- Better Subordinates progression storage attachment for MCA without entity replacement
- native Tensura / MCA / vanilla pet / horse movement routing
- `FOLLOW`, `WANDER`, `STAY` state and UI controls
- separate `NEUTRAL`, `PASSIVE`, `AGGRESSIVE`, `PROTECT` targeting model
- native Tensura targeting routing through `SubordinateHelper`
- external/generic targeting compatibility state
- MCA targeting adapter using existing Brain memories instead of Brain replacement
- server-authoritative targeting command validation
- native Thought Communication `isMastered()` authorization gate
- target HP / EP / SHP / movement / targeting snapshot
- targeting controls in both native-screen chrome and fallback screen
- Better Subordinates Status bridge
- native-screen context hand-off, including server-authoritative context for MCA
- native inventory/menu preservation policy
- capability-driven native inventory recognition through `HasCustomInventoryScreen`
- data-driven native-menu escape hatch for nonstandard mod GUIs
- horse-style secondary-use inventory normalization for all Tensura `TensuraMountEntity` subclasses
- fallback subordinate menu for entities without a useful native GUI
- recovery of all companion controls after native screens clear/rebuild widgets

The targeting implementation compiles/builds against the exact target runtime. Runtime AI behavior still needs the full-pack matrix below before the feature is treated as complete in play.

## Immediate runtime validation queue

### Tensura humanoid / goblin

- [ ] Re:Monster `Status` is visible and clickable.
- [ ] HP / EP / SHP / movement / targeting info is visible.
- [ ] `Follow`, `Wander`, `Stay` remain visible and clickable.
- [ ] `Neutral`, `Passive`, `Aggressive`, `Protect` are disabled before Thought Communication mastery.
- [ ] the four targeting controls enable after Thought Communication is actually mastered.
- [ ] native inventory/menu still works.
- [ ] opening Status reaches the intended Better Subordinates/native status path.
- [ ] each targeting mode changes the native Tensura behaviour correctly.

### MCA subordinate

- [ ] HP / EP / SHP / movement / targeting info is visible.
- [ ] all movement/targeting buttons remain visible after MCA calls `setLayout()` / `clearWidgets()`.
- [ ] targeting row is locked before Thought Communication mastery and unlocks after mastery.
- [ ] `Passive` clears/stops stale combat targets.
- [ ] `Neutral` responds to self/owner aggression without indiscriminate hunting.
- [ ] `Aggressive` acquires valid nearby non-allies.
- [ ] `Protect` responds when an ally is being attacked.
- [ ] MCA interaction/family/relationship UI remains usable.
- [ ] MCA schedule/profession behavior resumes normally outside combat.
- [ ] closing/reopening or moving between MCA screens does not attach chrome to the wrong villager.

### Generic subordinate families

- [ ] vanilla tameable: movement states still route correctly.
- [ ] horse/native mount: ordinary right-click still rides, shift-right-click still opens native inventory, chrome decorates inventory, movement bridge still works.
- [ ] Tensura mount/Basilisk: ordinary right-click still reaches native riding, shift-right-click opens native `MountScreen`, chrome decorates it.
- [ ] Tensura mount special items: ordinary right-click chest/shears/saddle behavior remains unchanged.
- [ ] one third-party `HasCustomInventoryScreen` entity: native interaction gesture remains unchanged and its native container receives chrome.
- [ ] one explicitly tagged nonstandard native GUI, when such a target is identified.
- [ ] fallback living entity with no useful native menu still reaches the generic subordinate menu only after native interaction returned `PASS`.
- [ ] one generic combat-capable non-Tensura subordinate validates all four targeting modes.
- [ ] a generic mob without useful attack AI does not receive fake Tensura combat automation merely because Aggressive was selected.

## Next implementation slice

After the current native-screen + targeting runtime matrix is green:

1. enforce/represent the agreed Telepathy / 2,000 EP movement-command progression gate in the universal authorization/UI layer
2. validate whether direct use of Thought Communication itself needs an additional external-subordinate interception path beyond the universal UI controls
3. finish MCA subordinate save/reload/EP/skills/learning/mastery/race/evolution checks in dependency order
4. return to ecology/profile authoring and dungeon/spawner integration after the subordinate layer is stable

MCA-backed Goblin/Dwarf civilization replacement remains intentionally late, after Townstead race visuals/genetics and race/evolution infrastructure. FTB Quests remains intentionally last.
