# Compatibility authority matrix

This document defines which installed system owns each mutable gameplay state in the Re:Monster modpack.

Central rule: **Re:Monster Integration composes existing systems and stores only project-owned content/state. It must not create a second source of truth for state already owned by another installed mod.**

The target runtime was audited from the real 1.21.1 load list and exact runtime jars where overlap mattered. See also [`TARGET_PACK_CONFIG.md`](TARGET_PACK_CONFIG.md).

## Source-of-truth table

| Domain | Authority | Re:Monster responsibility |
| --- | --- | --- |
| Native player race | ManasCore / Tensura | Bind project semantic/body content to the real `ManasRace`; synchronize presentation after native changes |
| Native player race evolution | ManasCore / Tensura or the addon registering the race | Discover/compose native requirements; add only genuinely new project branches |
| Creature evolution branch graph / eligibility | Re:Monster Integration | Own canonical cross-mod lifecycle edges, branch choice and authored requirements for creature evolution in this pack |
| Native same-EntityType form implementation | The source mob mod, especially Tensura | Keep the real variant/state/model/stat/skill machinery; when a native automatic transition is adopted into the Re:Monster graph, suppress only its autonomous trigger and invoke the native form mutation through an explicit graph edge |
| Tensura skills / mastery / activation | ManasCore / Tensura | Use real `SkillStorage`, acquisition/mastery, costs, cooldowns and effects |
| Innate/effective mob skills | Dedicated compatibility addon first, otherwise Re:Monster Mob Profile | Put authored skills on the mob's real native skill storage |
| Kill-driven skill learning | Re:Monster Integration | Exact `LivingEntity` killer, real/profile source skills and native acquisition/mastery for players and non-players |
| EP / existence | Tensura | Read/write real `IExistence`; preserve through external body replacement |
| EP distribution + aura/magicule regeneration | Tensura Modifications | Stay out of reward splitting/regen; maintain only external-body pool coherence |
| EP / MP / AP -> combat power | EPGains | No competing Re:Monster EP-to-stat curve |
| Subordinate training/unlocks | Better Subordinates | Expose/preserve its real storage/counters on compatible bodies |
| Native Tensura subordinate AI | Tensura / Better Subordinates | Leave native AI authoritative |
| External active-skill AI | Re:Monster fallback | Choose learned skills only for bodies lacking native Tensura skill AI; invoke native activation lifecycle |
| MCA identity / UUID | MCA | Keep MCA villagers as MCA entities |
| MCA Brain / family / profession / schedule / village state | MCA | Add narrow compatibility hooks only |
| MCA inventory | MCA | Read/project the real MCA inventory; no shadow inventory |
| MCA phenotype/genetics/body rendering | Townstead | Map native race to Townstead body/root recipes and request recalculation/sync |
| MCA species/personality voice | Townstead | No Re:Monster mediation unless a future Re:Monster feature directly needs this domain |
| Capital dialogue / monarchy / politics | MCA Capitals | No Re:Monster mediation unless a future Re:Monster feature directly needs this domain |
| Generic elite/champion rank | MobChampions | Universal eligibility, Tensura EP/skill/scale consequences and rank preservation |
| Bosses'Rise Tensura behavior | Bosses'Rise x Tensura Compatibility | Use its real EP/skill behavior; Mob Profiles add only cross-system metadata |
| Mysticism / other addon races + skills | Their addon + ManasCore registry | Discover/bind the real registered IDs |
| Adventurer guild progression / commissions | Tensura Guild | Feed compatible targets/metadata only where extension points exist |
| MCA villager quests | MCA: Quests | Author compatible objective data without a second quest tracker |
| Natural spawn tables | Source mod/datapack first; InControl fallback | Prefer source data overrides, then InControl only for gaps |
| Generic dungeon/spawner pools | Re:Monster Dungeon Director (future) | Compose profile ecology/tier metadata without replacing dedicated boss structures |

## Required target-pack cleanup

### Tensura: Skill Obtain (`tensura_skillobtain`)

Remove it.

It is a player-only configurable `EntityType -> skill -> chance` direct-grant table. It does **not** give innate skills to mobs. Re:Monster already has the stronger canonical model for this pack: the defeated creature actually owns its Tensura skills, and any compatible living killer can progress those same real skills through native acquisition/mastery.

Keeping Skill Obtain would force the pack to maintain the same bestiary skill design twice and allow a player-only drop table to disagree with what the creature actually possesses.

The one useful idea worth retaining is independent per-skill chance. If needed, add that later to the canonical Re:Monster profile/learning data instead of restoring a second registry.

### Tensura: Unique Monsters (`tr_unique_monsters`)

Remove it.

Its random larger/stronger monster layer overlaps directly with MobChampions. MobChampions plus Re:Monster rank-facing EP, scale and native-skill integration is the sole generic elite system for this pack.

## Keep and delegate

### EPGains 0.0.18

Keep it as the sole existence-to-combat-power scaler. Re:Monster's former logarithmic vanilla-attribute growth service was removed entirely.

EPGains is continuously linear, not a 50k-step threshold. With the target-pack rate documented in `TARGET_PACK_CONFIG.md`, 2,000 Max EP already produces about +0.04% player outgoing damage, while non-player entities receive half by default.

### Tensura Modifications

Keep it for participant EP distribution and aura/magicule regeneration. Re:Monster only keeps external bodies compatible with the native existence pools used by learning/skills.

### Better Subordinates

Keep it as training/unlock authority. Re:Monster only extends compatible bodies into its real state.

### Mysticism / TNO / other Tensura content addons

Keep them as content authorities. Any race/skill they register is consumed from ManasCore/Tensura rather than recreated as project state.

### Bosses'Rise x Tensura Compatibility

Keep it as the dedicated Bosses'Rise adapter. Re:Monster does not reauthor its existing Tensura EP/skill behavior.

### Tensura Guild + MCA: Quests

Keep both because their scopes differ: Tensura Guild owns adventurer progression/commissions; MCA: Quests owns villager-local quest state.

### MCA Capitals + Townstead

Keep both as independent MCA extensions.

The exact target runtime logs an overwrite conflict on `VillagerEntityMCA#getTranslatable`, but that conflict is between two third-party dialogue systems and is outside Re:Monster Integration's gameplay responsibility. Re:Monster intentionally does **not** add a compositor or third-party compatibility patch for it. If one mod wins the overwrite, that is left to those mods/the assembled pack unless a future Re:Monster feature directly depends on the dialogue path.

### Tensura native creature forms under the Re:Monster evolution graph

Keep the native form machinery, but do not let an upstream autonomous evolution bypass the authored Re:Monster lifecycle when that creature has been adopted into the universal graph.

For a same-EntityType creature such as `tensura:direwolf`:

1. Re:Monster owns which evolution edges exist, which target form is selected and whether its conditions are satisfied.
2. Native Tensura variant/evolution state remains the real physical form state.
3. Native automatic triggering is suppressed for graph-owned transitions.
4. Re:Monster reproduces/adopts the meaningful native requirements as graph conditions, then explicitly invokes the native form mutation for the selected target.
5. The entity is not destroyed/recreated merely to represent another Direwolf form.

This lets owned creatures choose among eligible branches while wild creatures remain deterministic from authored conditions, without inventing fake EntityTypes or a second form state.

## Non-conflicting/complementary groups

Creature/boss mods such as Ice and Fire CE, Wyrmroost, Mowzie's Mobs, Mowzie's Cataclysm, L_Ender's Cataclysm, Mobs of Mythology, Born in Chaos, Bosses'Rise and Legendary Monsters retain their native entity bodies/AI. Re:Monster integrates them through real Tensura skills/existence, Mob Profiles, evolution, ecology, MobChampions and quests.

Structure mods retain worldgen authority. Loot/equipment mods retain item/loot authority. AliveCombat/Combat Roll remain player-combat/client systems. InControl remains a fallback spawn-rule engine. MCA Polygamy changes relationship rules while MCA remains family/identity authority. Libraries, UI and performance mods do not own Re:Monster gameplay state.

## End-to-end invariants

1. Identify the upstream owner before mutating state.
2. Prefer upstream public API/storage/event over copied state.
3. Any Re:Monster cache/projection must be reconstructible from or synchronized with upstream authority.
4. A bound player race must never allow `Re:Monster species != ManasCore race`.
5. External physical evolution transfers only portable identity/progression; the destination keeps native AI/Brain/goals/base initialization.
6. MCA villagers never lose MCA entity identity for fantasy-race presentation/evolution.
7. Native Tensura mobs/subordinates keep native skill/subordinate AI unless a researched adapter explicitly composes that native mechanism.
8. Dedicated compatibility addons remain authoritative for the mod pair they already integrate.
9. Generic elite behavior belongs to MobChampions only.
10. Generic spawn control ends with one effective rule path per mob/biome.
11. Debug commands may inspect/force pack-author state, but ordinary gameplay must not require them.
12. Proof/test datapack content never ships in production.
13. Native same-EntityType forms/variants are modeled as native form transitions, not blindly converted into replacement EntityTypes.
14. When a native automatic creature evolution is adopted into the Re:Monster graph, suppress the upstream trigger rather than replacing the upstream form state/mechanics.
15. Re:Monster does not accumulate compatibility patches for unrelated third-party conflicts that do not intersect its own systems.

## Runtime-proven infrastructure checkpoints

- Zombie used Voice Cannon through native ManasCore/Tensura activation
- Iron Golem progressed acquisition and learned Voice Cannon from kills
- Mobs of Mythology Chupacabra gained native acquisition state
- MobChampions eligibility and rank-facing EP/size/skill behavior worked across external mobs
- champion rank/birth skills survived controlled physical evolution
- MCA civilian melee/bow/panic compatibility retained MCA simulation
- native Tensura race change synchronized project species/body presentation
- owned external evolution choice worked through subordinate UI
- MCA subordinate save/reload preserved 39,999 EP and Water Current Control

Proof-only evolution recipes/species mappings were removed after validation. Canonical race/bestiary/evolution authoring can now proceed from the native evolution atlas without any Townstead/MCA Capitals dialogue regression gate.
