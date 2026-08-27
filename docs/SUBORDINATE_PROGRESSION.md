# Subordinate progression

## Authority split

Re:Monster Integration does not recreate Better Subordinates progression.

For entities supported by Better Subordinates, the upstream mod remains authoritative for:

- kill counters
- damage-taken counters
- special kill/eating counters
- EP-threshold skill unlock checks
- subordinate skill acquisition
- skill learning/mastery state
- persistence of `ExpandedSkillStorage`

Re:Monster Integration owns only the cross-mod compatibility pieces required to make that progression work for non-native subordinate bodies such as MCA villagers:

- Tensura permanent ownership on the existing entity
- eligibility for Better Subordinates `ExpandedSkillStorage`
- diagnostics spanning Tensura + Better Subordinates + Re:Monster semantic species
- Re:Monster evolution graph and physical-body adapters

The MCA entity itself is never replaced for progression. MCA Brain, family, relationships, profession, schedule, inventory and Townstead state remain on the same entity.

## MCA storage bridge

Better Subordinates normally attaches `ExpandedSkillStorage` to living `OwnableEntity` instances. MCA villagers deliberately do not implement the vanilla pet ownership contract.

`ExpandedSkillStorageMixin` widens only the Better Subordinates storage-registration predicate so an MCA villager receives the native progression storage. Ownership resolution after that point is already generic because Better Subordinates uses Tensura `SubordinateHelper` and ordinary `LivingEntity` storage APIs.

No Better Subordinates `TensuraTamableEntity` combat-AI mixins are copied onto MCA villagers.

## Progression diagnostics

Pack-author commands require permission level 2.

### Full status

```text
/remonster progression status <target>
```

Reports:

- entity id / UUID
- whether the target is MCA
- resolved Tensura owner
- Re:Monster semantic species
- current Tensura EP
- Tensura gained-EP counter
- Better Subordinates storage status and runtime storage class
- known Better Subordinates progression counters
- current non-resistance Tensura skills
- raw mastery value and mastered state per skill
- outgoing Re:Monster evolution edges and whether their EP requirement is ready

Known Better Subordinates counters currently inspected:

```text
spiderEyesEaten
spiderKills
slimeKills
centipedeKills
totalKills
damageTaken
existenceUpdated
```

These values are read reflectively for diagnostics only. Re:Monster never increments or rewrites them.

### EP threshold test helpers

```text
/remonster progression set_ep <target> <ep>
/remonster progression add_ep <target> <amount>
```

These commands write only Tensura's real `IExistence` EP value and mark that storage dirty. Better Subordinates remains responsible for detecting the changed EP and applying any native unlock logic.

They are debug helpers, not intended player-facing progression mechanics.

## Runtime validation slice

Use one existing MCA villager and keep the same UUID throughout the test.

1. Assign the villager as a Tensura subordinate through the existing MCA adapter.
2. Run `/remonster progression status <target>`.
3. Confirm `Better Subs storage: present`.
4. Record `totalKills`, `damageTaken`, current EP and current skills.
5. Let the villager take ordinary combat damage.
6. Run progression status again and verify `damageTaken` increased.
7. Let the villager kill an ordinary valid target.
8. Run progression status again and verify `totalKills` increased.
9. Use `set_ep` or `add_ep` to cross a Better Subordinates EP-only unlock threshold.
10. Allow at least one server tick, then run progression status again and verify the expected skill/storage state changed through Better Subordinates itself.
11. Save and reload the world.
12. Verify the same MCA UUID still has ownership, EP, Better Subordinates counters and learned skills.

Only after this runtime slice passes should the roadmap mark MCA Better Subordinates kill/damage/EP skill progression as runtime-proven.

## Evolution connection

Re:Monster evolution readiness is intentionally reported beside Better Subordinates progression but remains a separate system.

Current evolution definitions evaluate real Tensura EP. A later gameplay pass will define how a subordinate actually chooses or triggers an unlocked evolution when several branches are available. The project must not auto-pick one branch merely because multiple EP requirements are simultaneously satisfied.
