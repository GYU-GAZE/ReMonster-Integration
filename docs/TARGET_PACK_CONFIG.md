# Target pack authority configuration

These values belong to the assembled Re:Monster modpack rather than to Re:Monster Integration. The addon deliberately does not reimplement or silently override them.

## EPGains 0.0.18

Authority: EPGains owns existence-based combat-power scaling.

ManasCore stores this config at:

`config/epgains/config.toml`

The exact NeoForge 0.0.18 runtime jar initializes the three linear rates 100x higher than the scaling described on the public mod page. The implementation multiplies the raw maximum pool directly by the configured rate, so the documented `+1% per 50,000 Max EP` requires `0.01 / 50000 = 2e-7`, not the jar constructor default `2e-5`.

Recommended Re:Monster target-pack values:

```toml
enableOffensiveEP = true
offensiveEPRate = 2.0E-7

enableMagicMP = true
magicMPRate = 4.0E-7

enableBattlewillAP = true
battlewillAPRate = 6.666666666666667E-7

enableResistEP = true
resistEPCap = 0.25
resistEPScale = 2000000.0

mobRate = 0.5
```

These rates are continuous, not thresholds. At the recommended EP rate a player receives approximately:

- 2,000 Max EP -> +0.04% outgoing damage from EP
- 10,000 Max EP -> +0.2%
- 50,000 Max EP -> +1%
- 1,000,000 Max EP -> +20%

Non-player entities receive half of those linear bonuses with `mobRate = 0.5`.

The magic and battlewill bonuses stack multiplicatively with the EP bonus exactly as EPGains implements them. Re:Monster Integration contains no second EP-to-stat scaling system.

## Tensura: Skill Obtain 1.1

Remove `tensura_skillobtain` from the target pack.

Its useful feature is an authored `EntityType -> skill -> independent chance` table for direct player grants. It does **not** assign innate skills to mobs. Keeping it would require maintaining a second player-only skill-source table beside the canonical skills already authored on mobs through dedicated compatibility addons and Re:Monster Mob Profiles.

Re:Monster remains the single kill-learning authority for both players and non-player living combatants. The defeated mob's real ManasCore/Tensura skill storage plus Mob Profile declarations define what can be learned, and progression uses native acquisition/mastery rather than a parallel fake skill flag.

If per-skill kill chance is desirable later, add it as data on the canonical Re:Monster skill/profile source rather than reintroducing a second EntityType skill-drop registry.

## Tensura: Unique Monsters

Remove `tr_unique_monsters` from the target pack.

MobChampions plus Re:Monster's champion EP/scale/skill integration is the sole generic random elite layer. Appraisal Eye is not considered sufficient reason to retain a second elite generator in this pack.
