# First integrated runtime playtest

This checklist is for the first real Re:Monster Integration jar placed inside the full Minecraft 1.21.1 NeoForge pack.

Do the checks in order. A failure early in the list can make later results meaningless.

## 0. Pack cleanup before the authoritative test

The current project roster intentionally excludes:

- Born in Chaos
- Tensura Unique Monsters / Tensura: Unique Monster

The addon itself is fail-soft and should not require either one to be removed just to load, but the authoritative pack test should use the intended roster.

Wyrmroost and Legendary Monsters checks can be skipped until those mods are actually installed.

## 1. Addon load

Put the built `remonster_integration-*.jar` in the instance `mods` folder and launch the same Minecraft 1.21.1 / NeoForge 21.1.248 runtime used by the project.

Expected:

- game reaches the title screen/world normally
- no Mixin apply error mentioning `remonster_integration`
- server/world startup logs contain the Re:Monster Integration initialization message

If this fails, save `latest.log` before doing anything else.

## 2. Mob Profile reload

Enter a world with commands enabled and target a Tensura Horned Rabbit, Direwolf or Barghest.

Run:

```text
/remonster inspect <target>
```

Expected for a profiled entity:

- profile is present
- runtime EP is readable
- runtime skills are readable
- runtime FOLLOW_RANGE is readable
- ownership layers are readable
- MobChampions state is displayed

For Direwolf, current project expectation is:

```text
aggro_range = 20
runtime FOLLOW_RANGE ~= 20
```

Use:

```text
/remonster profile reapply <target>
```

Expected: command reports the before/after EP and FOLLOW_RANGE without duplicating skills.

## 3. Horned Rabbit ecology override

Travel/generate fresh terrain in plains/meadow/forest habitat.

Expected:

- Horned Rabbits are now valid natural fauna in the project-selected early-game habitats
- expected configured weight: 70
- expected group: 1-3

This is a qualitative spawn test. Do not infer balance from one minute of observation.

## 4. Tensura aggro normalization

Test a wild Direwolf from outside melee distance.

Expected:

- it should not acquire the player from the original broader Tensura range once the project profile is applied
- `/remonster inspect` should show FOLLOW_RANGE around 20
- ally target propagation can still occur after one member legitimately acquires the player; this is a separate behavior we are intentionally testing after range normalization

Record whether the pack still feels like an off-screen dogpile despite the lower range.

## 5. MCA guard vs wild hostile

Put an MCA guard near a wild profiled hostile such as a Direwolf.

Run:

```text
/remonster guard_priority <guard> <target>
```

Expected for a normal hostile profile:

```text
guard priority override: 3
playerOwnedCompanion=false
```

Expected gameplay: guard treats the mob as a threat.

## 6. MCA villager as Tensura + Better Subordinates subordinate in place

Target a normal MCA villager and run:

```text
/remonster mca_subordinate assign <villager> <player>
/remonster mca_subordinate status <villager>
```

Expected immediately:

- entity remains an MCA villager
- permanent owner equals the player UUID
- resolved owner equals the player UUID unless a higher-priority temporary owner/summoner exists
- MCA profession/family/home/personality/schedule are unchanged
- with Better Subordinates 1.2.4 installed, diagnostics show `Better Subs progression storage: present`

The Better Subordinates storage check is decisive. `missing` means its native progression storage did not attach to the MCA villager and the compatibility mixin needs investigation before testing kill/damage/EP training.

Then interact with the villager normally and verify MCA UI still works.

## 7. Save/reload ownership and progression-storage persistence

After step 6:

1. save and quit the world
2. reopen it
3. run `/remonster mca_subordinate status <villager>` again

Expected:

- permanent owner still equals the player UUID
- resolved owner still resolves correctly
- Better Subs progression storage is still `present`
- MCA profession/family/home data remains intact

This is the first decisive runtime proof that the in-place MCA subordinate architecture works.

## 8. MCA guard vs player-owned Tensura subordinate

Place an MCA guard near a normally hostile Tensura mob that is genuinely owned by the player.

Expected diagnostics:

```text
/remonster guard_priority <guard> <subordinate>
```

returns:

```text
guard priority override: -1
playerOwnedCompanion=true
```

Expected gameplay: the guard does not passively attack it.

Then make that subordinate actively target/attack an MCA villager or the guard.

Expected diagnostics/gameplay:

```text
guard priority override: 9
activeThreat=true
```

Active village defence must override ownership immunity.

## 9. MobChampions

Find/spawn a MobChampion and run:

```text
/remonster inspect <champion>
```

Expected:

- rank appears as UNCOMMON / RARE / EPIC / LEGENDARY
- normal non-champion shows `normal/not active`

Re:Monster Integration does not own the champion combat stat modifications. MobChampions remains authoritative for health/armor/speed/damage/effects.

Learning reward multiplier currently used by Re:Monster Integration:

```text
UNCOMMON   1.25x
RARE       1.5x
EPIC       2.0x
LEGENDARY  3.0x
```

## 10. Runtime skill learning from compatibility addons

This test becomes especially important after Legendary Monsters + its Tensura compatibility addon are installed.

Requirement:

- defeated mob has a real Tensura skill in its runtime ManasCore SkillStorage
- player already possesses the same skill in Tensura's acquisition state (`mastery < 0`)

Expected:

- killing the mob grants native Tensura acquisition progress through `TensuraSkill.addLearnPoint`
- the skill can come either from the Re:Monster profile or from an external compatibility addon at runtime

This verifies that Re:Monster consumes existing compatibility instead of duplicating it.

## 11. Wyrmroost companion guard test

Once Wyrmroost is installed:

1. inspect a wild Overworld Drake
2. verify it is treated as a wild hostile profile
3. tame it using Wyrmroost's own mechanics
4. inspect it again
5. put it next to an MCA guard

Expected:

- tamed dragon exposes a player owner through Minecraft's `OwnableEntity` contract
- `/remonster inspect` reports `player-owned companion: true`
- `/remonster guard_priority <guard> <dragon>` reports `-1` while the dragon is peaceful
- if the dragon actively attacks the village, priority becomes `9`

This behavior is generic and should also protect other modded companions that correctly implement `OwnableEntity`.

## 12. What to send back after testing

For a clean debugging round, return:

- `latest.log`
- the exact command output for any failed check
- what entity was used
- whether failure happened before or after save/reload
- crash report only if one was generated

Do not change several unrelated mods between a failing test and its log, otherwise the runtime state becomes harder to attribute.
