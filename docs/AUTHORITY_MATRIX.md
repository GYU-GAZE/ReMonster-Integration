# Compatibility authority matrix

This document defines which installed system owns each gameplay state in the Re:Monster modpack.

The central rule is simple: **Re:Monster Integration bridges and composes existing systems; it does not create a second source of truth when an installed mod already owns the state.**

The current target runtime was audited from the real 1.21.1 modpack load list. Libraries, rendering/performance mods and ordinary content mods were checked for domain overlap; only systems that can collide with Re:Monster behavior require an authority rule below.

## Mandatory source-of-truth rules

| Domain | Authority | Re:Monster responsibility | Forbidden duplication |
| --- | --- | --- | --- |
| Native race | ManasCore / Tensura | Bind a project semantic/body profile to the real `ManasRace`; synchronize UI/body presentation after native changes | Independent player race state that can disagree with ManasCore |
| Native race evolution | ManasCore / Tensura and the addon that registered the race | Add a branch only when the pack deliberately needs one; let native requirement/progress/evolution execute when a native target race exists | Reimplementing a native race's EP/boss/skill requirements in a second graph |
| Tensura skills | ManasCore / Tensura | Read/write real `SkillStorage`, acquisition/mastery and native activation lifecycle | Parallel skill inventory or fake skill flags |
| EP / existence | Tensura | Read/write the real `IExistence`; preserve it through external body replacement | Parallel XP/EP currency |
| EP distribution and aura/magicule regeneration | Tensura Modifications (`tensura_epfix`) when installed | Stay out of reward splitting and regeneration; maintain only compatibility capacity/current-pool invariants for external bodies | A second kill-EP distributor or independent regen loop |
| EP -> combat power | EPGains (`epgains`) in the target pack | Disable Re:Monster fallback physical stat-growth curve; preserve EPGains state by keeping native existence intact | Two independent EP-to-stat/damage curves |
| Subordinate training/unlocks | Tensura Better Subordinates | Expose its real storage/counters on compatible bodies and preserve them through replacement | A second subordinate training counter system |
| Native Tensura subordinate combat AI | Tensura / Better Subordinates | Leave native `ISubordinate` behavior authoritative | Generic Re:Monster combat AI replacing native subordinate AI |
| External active-skill AI | Re:Monster Integration fallback | For bodies with no native Tensura skill AI, choose an eligible learned active skill, provision the real ability preset and invoke native ManasCore activation/cost/cooldown lifecycle | Reimplementing skill effects or costs |
| Kill-driven acquisition/mastery | Re:Monster Integration | Universal exact-`LivingEntity` kill credit and native acquisition/mastery progress for players, MCA, pets and wild compatible mobs | Direct-grant and acquisition processing for the same kill |
| MCA villager identity | MCA | Preserve entity UUID/type and MCA state in place | Replacing MCA villagers with fantasy mob EntityTypes |
| MCA Brain / family / profession / schedule / village state | MCA | Add narrow compatibility decisions and presentation hooks only | Reimplementing villager simulation |
| MCA inventory | MCA | Select/project items from the real MCA inventory for compatibility combat | Shadow inventory or equipment state invisible to MCA UI |
| MCA phenotype/genetics/body rendering | Townstead | Map native Tensura race to a Townstead body/root recipe and request Townstead recalculation/sync | Independent Re:Monster phenotype/genetics state |
| Generic elite/champion rank | MobChampions | Make eligible mobs universal, add rank-facing Tensura EP/skill/size consequences, preserve rank through physical evolution | A second generic random elite-rank system |
| Bosses'Rise Tensura behavior | Bosses'Rise x Tensura Compatibility | Consume its real EP/skills/ability state; use Mob Profiles only for cross-system metadata such as ecology/guards/dungeons | Reauthoring Bosses'Rise Tensura skills/EP behavior |
| Mysticism / SlimeThroneExtras races and skills | Their addon + ManasCore registry | Discover and bind their real registered races/skills | Project copies of addon races/skills |
| Adventurer guild progression / commissions | Tensura Guild | Feed compatible mob/profile targets or metadata into the guild system where an extension point exists | A third independent generic bounty/commission state |
| MCA villager quests | MCA: Quests | Author datapack quests / custom objective adapters around Mob Profiles where useful | A second villager quest tracker |
| Kingdom/capital politics | MCA Capitals | Bridge fantasy race/civilization identity into MCA Capitals | Independent monarchy/diplomacy/succession state |
| Dedicated Tensura boss structures | Tensura Boss Structure / Tensura Not Enough Bosses where applicable | Preserve these structures and boss ownership; Dungeon Director should only handle generic structure/spawner pools around them | Replacing dedicated boss arenas with generic dungeon logic |
| Natural spawn tables | Each source mod/datapack first; InControl only as fallback | Prefer biome modifiers/tags/data overrides; use InControl for rules data cannot express; runtime hook last | Multiple independent spawn governors altering the same mob without an authority decision |
| Generic dungeon/spawner pools | Re:Monster Dungeon Director (future) | Compose Mob Profile ecology/tier metadata into supported generic structures | Rewriting dedicated boss-structure systems |

## Required modpack removals

### Tensura: Skill Obtain (`tensura_skillobtain`)

Remove it from the target pack.

It directly grants configured skills to a **player** after a configured entity kill. Re:Monster now owns universal kill-driven acquisition/mastery and supports the exact living killer, including external mobs and NPCs. Keeping both would make the player path semantically inconsistent: one system direct-grants while the other progresses native acquisition.

Migration safety: while `tensura_skillobtain` is still installed, Re:Monster suppresses its own **player** kill-learning handler. Non-player acquisition remains active. This guard exists only so removing the legacy mod does not have to happen in the same launch as the addon update.

### Tensura: Unique Monsters (`tr_unique_monsters`)

Remove it from the target pack.

Its purpose is random larger/stronger Tensura monsters with configurable spawn chance and EP multiplier. MobChampions is now the pack-wide generic elite authority and Re:Monster already projects champion rank into individual base EP, physical scale and Tensura birth skills. Running both creates two independent random-elite layers that can stack size/power/EP semantics and make balancing opaque.

MobChampions remains the sole generic random elite/champion system.

## Keep, but delegate to them

### EPGains

Keep it. The current pack version already gives native existence values direct combat relevance, including non-player entities. Re:Monster's old logarithmic EP-to-vanilla-stat service is now a fallback only and automatically removes/disables itself when `epgains` is loaded.

### Tensura Modifications

Keep it. Dynamic participant EP distribution and scalable aura/magicule regeneration are useful base-system improvements. Re:Monster does not duplicate either. The external energy bridge exists only so non-native bodies have coherent current/max pools for native Tensura learning and skill activation.

### Better Subordinates

Keep it as the training authority. Re:Monster only makes compatible external/MCA bodies capable of carrying its real storage/counters and preserving them across evolution.

### Mysticism and SlimeThroneExtras

Keep them as content authorities. Their races and skills must be consumed from the ManasCore registries. Any future Re:Monster race body/evolution mapping must explicitly bind to their native race IDs rather than create a project-only player race.

### Neo Otherworld Compatibility Fix / Tensura TNO

Keep it as a compatibility layer. It should not become a second race authority. The audited runtime currently reports its race-injection refresh as disabled. If that feature is enabled later, it must still result in real ManasCore race registrations/state that Re:Monster observes and binds, never a parallel race value.

### Bosses'Rise x Tensura Compatibility

Keep it as the Bosses'Rise Tensura adapter. Re:Monster Mob Profiles may classify those mobs for guard hostility, ecology, dungeon weighting and cross-mod rewards, but do not overwrite skill/EP behavior already supplied by the dedicated compatibility addon.

### Tensura Guild and MCA: Quests

Keep both because their scopes differ cleanly:

- Tensura Guild: player adventurer progression, ranks, parties and world/adventure commissions.
- MCA: Quests: per-villager/local MCA quest offers, favor and village projects.

The old roadmap idea of adding a third Bountiful-style generic bounty layer is retired unless a concrete gap appears.

### MCA Capitals

Keep it as future civilization governance authority. Re:Monster fantasy civilizations should be MCA citizens with Tensura race/EP/skill identity and Townstead phenotype, while monarchy, courts, succession, diplomacy and war remain MCA Capitals state.

## Mods audited as non-conflicting or complementary

The remaining loaded mods fall into categories that do not duplicate current Re:Monster state ownership:

- **Creature/boss content:** Ice and Fire CE, Mowzie's Mobs, Mowzie's Cataclysm, L_Ender's Cataclysm, Mobs of Mythology, Bosses'Rise, Born in Chaos and future roster mods. Re:Monster integrates them through Mob Profiles, native existence/skills, MobChampions eligibility, ecology and evolution adapters rather than replacing their native entity AI.
- **Structures/worldgen:** YUNG's Better Dungeons/Mineshafts/Strongholds, When Dungeons Arise, Better Village, Improved Village Placement, Tectonic, More Tree Sizes, ZV-Houses. Future directors should operate through data/structure/spawner extension points and not clone their world-generation state.
- **Loot/items/equipment:** Loot Integrations, Ice and Fire Loot Integration, Artifacts, Relics, Ancient Artifacts, Malum, Farmer's Delight, Rustic Delight, Butchery, Sophisticated Backpacks, Supplementaries and construction/storage utilities. These do not own Re:Monster progression domains.
- **Player combat/client behavior:** AliveCombat and Combat Roll affect player controls/combat feel. AliveCombat is client-side and does not own NPC/subordinate AI. They are not substitutes for Re:Monster NPC compatibility AI.
- **Spawn tooling:** InControl is retained as a fallback rule engine. The ecology roadmap should prefer source-mod datapack/biome overrides first and use InControl only for conditions those systems cannot express.
- **MCA add-ons:** MCA Polygamy changes relationship rules but does not replace MCA identity/family storage; future adapters must continue to treat MCA itself as authoritative.
- **Libraries/performance/UI:** ManasCore modules, Architectury, SmartBrainLib, GeckoLib, AzureLib, Curios, Placebo, Balm, Moonlight, OctoLib, Lodestone, KotlinForForge, Sodium, Iris, ModernFix, FerriteCore, ServerCore, EntityCulling, More Culling, ImmediatelyFast, ScalableLux, Spark, JEI, Jade, AppleSkin, Controlling, Inventory Profiles Next, Mouse Tweaks, Xaero, sound/animation libraries and bundled helper libraries do not overlap the gameplay state owned by Re:Monster.

## End-to-end integration invariants

Every new compatibility feature must satisfy these rules before merge:

1. Identify the upstream owner of every mutable state it touches.
2. Prefer the upstream public API/storage/event over copied state.
3. If Re:Monster stores a projection/cache, it must be reconstructible from or synchronized with the upstream owner.
4. A player/native race binding must never allow `Re:Monster species != ManasCore race` when a native binding exists.
5. External physical evolution transfers only portable identity/progression state. The new body keeps its native AI/Brain/goals/base initialization.
6. MCA villagers never lose MCA entity identity for fantasy-race presentation/evolution.
7. Native Tensura mobs/subordinates keep native skill and subordinate AI.
8. Dedicated compatibility addons remain authoritative for the mod pair they explicitly integrate.
9. Generic elite behavior belongs to MobChampions only.
10. Generic spawn control has one effective rule path per mob/biome after the ecology pass.
11. Debug commands may inspect or force state for pack authors, but ordinary gameplay must not require them.
12. Proof/test datapack content must not ship in the production addon.

## Runtime-proven integration checkpoints

The completed progression batch has real full-pack proofs for the critical contracts:

- arbitrary external active skill casting: Zombie used Voice Cannon through native ManasCore/Tensura activation;
- repeated acquisition completion: Iron Golem progressed and learned Voice Cannon from kills;
- wild third-party learning: Mobs of Mythology Chupacabra gained native acquisition state;
- universal MobChampions eligibility and rank-facing EP/size/skill behavior;
- champion rank and birth skills preserved through controlled physical evolution;
- MCA civilian melee/bow/panic compatibility while keeping MCA simulation;
- native Tensura race change synchronizes project species/body presentation and common evolution feedback;
- owned external evolution choice works through the subordinate evolution UI;
- MCA subordinate save/reload preserved 39,999 EP and Water Current Control.

The proof-only evolution recipes/species mappings used to validate the generic engine were removed after these tests. Canonical evolution content will be authored later against the final roster and native-race authority rules above.
