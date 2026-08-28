# Compatibility authority matrix

This document defines which installed system owns each gameplay state in the Re:Monster modpack.

The central rule is simple: **Re:Monster Integration bridges and composes existing systems; it does not create a second source of truth when an installed mod already owns the state.**

The target runtime was audited from the real 1.21.1 load list and exact runtime jars where overlap mattered. See also [`TARGET_PACK_CONFIG.md`](TARGET_PACK_CONFIG.md).

## Mandatory source-of-truth rules

| Domain | Authority | Re:Monster responsibility | Forbidden duplication |
| --- | --- | --- | --- |
| Native player race | ManasCore / Tensura | Bind project semantic/body content to the real `ManasRace`; synchronize presentation after native changes | Independent player race state that can disagree with ManasCore |
| Native player race evolution | ManasCore / Tensura and the addon that registered the race | Discover and compose the real branch/requirements; add project branches only deliberately | Reimplementing the same native evolution in a second hidden graph |
| Tensura skill storage/lifecycle | ManasCore / Tensura | Read/write real `SkillStorage`, acquisition/mastery and native activation lifecycle | Parallel skill inventory or fake skill flags |
| Innate/effective mob skills | Dedicated compatibility addon first; otherwise Re:Monster Mob Profile | Put the skill on the mob's real native storage and describe the mob for cross-system use | A fake loot-only skill declaration pretending the mob possesses the skill |
| Player kill skill drops | Tensura: Skill Obtain | Author EntityType -> skill/chance drop tables; delivery stays native through `SkillHelper.learnSkill` | Re:Monster processing the same player kill through gradual acquisition |
| Non-player kill acquisition/mastery | Re:Monster Integration | Exact living kill credit and gradual native acquisition/mastery for MCA, subordinates, pets and wild compatible mobs | A second direct-grant path for those same non-player kills |
| EP / existence | Tensura | Read/write real `IExistence`; preserve it through external body replacement | Parallel XP/EP currency |
| EP distribution and aura/magicule regeneration | Tensura Modifications (`tensura_epfix`) | Stay out of reward splitting and regeneration; maintain only external-body capacity/current-pool invariants | A second kill-EP distributor or independent regen loop |
| EP / MP / AP -> combat power | EPGains | No competing Re:Monster stat-growth curve; keep native existence intact | Two independent existence-to-power curves |
| Subordinate training/unlocks | Tensura Better Subordinates | Expose/preserve its real storage and counters on compatible bodies | A second subordinate training counter system |
| Native Tensura subordinate combat AI | Tensura / Better Subordinates | Leave native `ISubordinate` behavior authoritative | Generic Re:Monster AI replacing native subordinate AI |
| External active-skill AI | Re:Monster Integration fallback | For bodies without native Tensura skill AI, choose a learned active skill and invoke native activation/cost/cooldown lifecycle | Reimplementing skill effects or costs |
| MCA villager identity | MCA | Preserve MCA entity UUID/type/state in place | Replacing MCA villagers with fantasy mob EntityTypes |
| MCA Brain / family / profession / schedule / village state | MCA | Add narrow compatibility decisions/presentation hooks only | Reimplementing villager simulation |
| MCA inventory | MCA | Select/project real MCA inventory items for compatibility combat | Shadow inventory invisible to MCA |
| MCA phenotype/genetics/body rendering | Townstead | Map native Tensura race to Townstead body/root recipes and request Townstead recalculation/sync | Independent Re:Monster phenotype/genetics state |
| MCA species/personality dialogue voice | Townstead | Preserve Townstead voice after capital-specific dialogue gets first refusal | Silently losing Townstead voice to another overwrite |
| Capital/political dialogue and kingdom politics | MCA Capitals | Capital-specific line gets first refusal; monarchy/court/succession/diplomacy/war stay MCA Capitals-owned | Independent Re:Monster political state |
| Generic elite/champion rank | MobChampions | Universal eligibility, rank-facing Tensura consequences, rank preservation across replacement | Any second random generic elite generator |
| Bosses'Rise Tensura behavior | Bosses'Rise x Tensura Compatibility | Consume its real EP/skills/ability state; use Mob Profiles only for cross-system metadata | Reauthoring the dedicated compatibility layer |
| Mysticism / SlimeThroneExtras races and skills | Their addon + ManasCore registry | Discover/bind their real registered races/skills | Project copies of addon races/skills |
| Adventurer guild progression / commissions | Tensura Guild | Feed compatible profile targets/metadata where extension points exist | A third generic bounty/commission tracker |
| MCA villager quests | MCA: Quests | Author compatible quest data/objectives around Mob Profiles | A second villager quest tracker |
| Dedicated Tensura boss structures | Tensura Boss Structure / TNO where applicable | Preserve dedicated boss content; generic dungeon work integrates around it | Replacing dedicated arenas with generic dungeon logic |
| Natural spawn tables | Source mod/datapack first; InControl fallback | Prefer biome tags/modifiers/data, then InControl only for gaps, runtime hook last | Multiple active spawn governors for the same mob/biome |
| Generic dungeon/spawner pools | Re:Monster Dungeon Director (future) | Compose profile ecology/tier metadata into supported generic structures | Rewriting dedicated boss-structure systems |

## Required pack cleanup

### Tensura: Unique Monsters (`tr_unique_monsters`)

Remove it from the target pack.

Its random larger/stronger monster layer overlaps directly with MobChampions. The target design explicitly prefers MobChampions plus Re:Monster's rank-facing EP/scale/native-skill integration. Appraisal Eye is not considered enough reason to retain a second generic elite generator.

MobChampions is the sole generic random elite/champion authority.

## Keep and delegate to them

### Tensura: Skill Obtain 1.1

Keep it as the **player-facing configured kill-skill drop system**.

Exact runtime-bytecode audit shows that it:

- only reacts when `DamageSource.getEntity()` is a `ServerPlayer`;
- matches the defeated EntityType against authored config entries;
- supports multiple skills with independent per-skill chances;
- skips a skill the player already has;
- grants successful drops directly through native `SkillHelper.learnSkill`.

It does **not** assign innate skills to mobs. Re:Monster Mob Profiles or dedicated compatibility addons do that by putting skills on the mob's real native skill storage.

Re:Monster therefore never runs its gradual kill-acquisition handler for `ServerPlayer`. Its complementary kill-learning path is non-player only.

### EPGains 0.0.18

Keep it as the sole existence-to-combat-power scaler and remove Re:Monster's old logarithmic vanilla-attribute curve entirely.

The exact NeoForge 0.0.18 jar is continuously linear, but its constructor defaults are 100x larger than the rates described by the public mod documentation. The target pack must explicitly set the corrected rates in `config/epgains/config.toml`; see [`TARGET_PACK_CONFIG.md`](TARGET_PACK_CONFIG.md).

### Tensura Modifications

Keep it. Dynamic participant EP distribution and aura/magicule regeneration remain upstream behavior. Re:Monster's external energy bridge only keeps non-native bodies coherent with native learning/activation costs.

### Better Subordinates

Keep it as subordinate training/unlock authority. Re:Monster only makes compatible external/MCA bodies carry and preserve its real storage/counters.

### Mysticism and SlimeThroneExtras

Keep them as content authorities. Their races/skills must be consumed from ManasCore registries. Future Re:Monster race/evolution content binds to the real native IDs rather than creating competing player-race state.

### Neo Otherworld Compatibility Fix / Tensura TNO

Keep it as a compatibility/content layer. Any race it contributes must ultimately exist as real ManasCore race state that Re:Monster observes and binds.

### Bosses'Rise x Tensura Compatibility

Keep it as the Bosses'Rise Tensura adapter. Mob Profiles may classify those mobs for guards/ecology/dungeons/rewards but do not overwrite EP/skill behavior already supplied by the dedicated compatibility addon.

### Tensura Guild and MCA: Quests

Keep both because their scopes differ cleanly:

- Tensura Guild owns adventurer ranks, parties and world/adventure commissions.
- MCA: Quests owns per-villager/local quest offers, favor and village projects.

A third generic Bountiful-style bounty layer is not planned without a concrete gap.

### MCA Capitals + Townstead dialogue composition

Keep both.

The exact target runtime logged an overwrite conflict on `VillagerEntityMCA#getTranslatable`: MCA Capitals won and Townstead's `MessengerVoiceMixin` was skipped. Exact MCA Capitals 1.2.1 bytecode and Townstead source show both systems have legitimate non-overlapping intent.

The composed authority is:

1. MCA Capitals capital/political line, when one exists;
2. Townstead personality/origin/lineage/ancestry/species voice, when one exists;
3. normal MCA Capitals/MCA translation fallback.

Re:Monster implements only this composition bridge; it does not own either dialogue dataset.

## Mods audited as non-conflicting or complementary

The remaining loaded mods do not duplicate current Re:Monster state ownership in a way that requires another authority layer:

- **Creature/boss content:** Ice and Fire CE, Mowzie's Mobs, Mowzie's Cataclysm, L_Ender's Cataclysm, Mobs of Mythology, Bosses'Rise, Born in Chaos and future roster mods. Their native entity AI/body remains theirs; Re:Monster supplies cross-system skill/profile/evolution/ecology integration.
- **Structures/worldgen:** YUNG Better Dungeons/Mineshafts/Strongholds, When Dungeons Arise, Better Village, Improved Village Placement, Tectonic, More Tree Sizes and ZV-Houses. Future integration uses data/structure/spawner extension points.
- **Loot/items/equipment:** Loot Integrations, Ice and Fire Loot Integration, Artifacts, Relics, Ancient Artifacts, Malum, Farmer's Delight, Rustic Delight, Butchery, Sophisticated Backpacks, Supplementaries and utilities do not own Re:Monster progression domains.
- **Player combat/client behavior:** AliveCombat and Combat Roll affect player combat/control feel and are not substitutes for NPC/subordinate compatibility AI.
- **Spawn tooling:** InControl remains a fallback rule engine after source-mod data overrides.
- **MCA add-ons:** MCA Polygamy changes relationship rules while MCA remains identity/family authority.
- **Libraries/performance/UI:** ManasCore modules, Architectury, SmartBrainLib, GeckoLib, AzureLib, Curios, Placebo, Balm, Moonlight, OctoLib, Lodestone, KotlinForForge, Sodium, Iris, ModernFix, FerriteCore, ServerCore, EntityCulling, More Culling, ImmediatelyFast, ScalableLux, Spark, JEI, Jade, AppleSkin, Controlling, Inventory Profiles Next, Mouse Tweaks, Xaero and helper libraries do not overlap the gameplay state owned by Re:Monster.

The August 25 runtime log contained only one direct method-overwrite collision in the audited Re:Monster domains: Townstead vs MCA Capitals `getTranslatable`. That conflict is explicitly composed by the compatibility bridge above. Other resource/animation/config warnings in that log are pack-hygiene issues rather than competing Re:Monster state authorities.

## End-to-end integration invariants

Every new compatibility/content feature must satisfy these rules before merge:

1. Identify the upstream owner of every mutable state it touches.
2. Prefer upstream public API/storage/event over copied state.
3. Any Re:Monster projection/cache must be reconstructible from or synchronized with upstream authority.
4. A bound player race must never allow `Re:Monster species != ManasCore race`.
5. External physical evolution transfers only portable identity/progression; the new body keeps native AI/Brain/goals/base initialization.
6. MCA villagers never lose MCA entity identity for fantasy-race presentation/evolution.
7. Native Tensura mobs/subordinates keep native skill/subordinate AI unless an explicitly researched adapter composes their native mechanism.
8. Dedicated compatibility addons remain authoritative for the pair they explicitly integrate.
9. Generic elite behavior belongs to MobChampions only.
10. Generic spawn control ends with one effective rule path per mob/biome.
11. Debug commands may inspect/force pack-author state, but ordinary gameplay must not require them.
12. Proof/test datapack content must never ship in production.
13. Native same-EntityType forms/variants must be modeled as native form transitions rather than blindly converted into replacement EntityTypes.

## Runtime-proven integration checkpoints

The completed infrastructure batch has full-pack proofs for the critical contracts:

- Zombie used Voice Cannon through native ManasCore/Tensura activation;
- Iron Golem progressed acquisition and learned Voice Cannon from kills;
- Mobs of Mythology Chupacabra gained native acquisition state;
- MobChampions eligibility and rank-facing EP/size/skill behavior worked across external mobs;
- champion rank/birth skills survived controlled physical evolution;
- MCA civilian melee/bow/panic compatibility retained MCA simulation;
- native Tensura race change synchronized project species/body presentation and common evolution feedback;
- owned external evolution choice worked through subordinate UI;
- MCA subordinate save/reload preserved 39,999 EP and Water Current Control.

Proof-only evolution recipes/species mappings were removed after validation. Canonical race and bestiary evolution content is the next authoring stage after the final authority cleanup passes CI/runtime validation.
