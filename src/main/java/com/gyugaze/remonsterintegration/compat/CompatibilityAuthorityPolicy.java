package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.neoforged.fml.ModList;

/**
 * Documents and reports which installed mod owns each overlapping gameplay domain in the target pack.
 *
 * The integration addon should bridge foreign state, not silently create competing sources of truth.
 * This startup audit makes intentional authority decisions visible in logs and flags actual pack
 * cleanup requirements.
 */
public final class CompatibilityAuthorityPolicy {
    private CompatibilityAuthorityPolicy() {
    }

    public static void logInstalledAuthorities() {
        ModList mods = ModList.get();

        infoIfLoaded(mods, "manascore", "ManasCore/Tensura owns native race and skill storage/lifecycle");
        infoIfLoaded(mods, "tensura", "Tensura owns existence/EP, native race evolution and native subordinate behavior");
        infoIfLoaded(mods, "tensura_epfix", "Tensura Modifications owns participant EP distribution and aura/magicule regeneration");
        infoIfLoaded(mods, "epgains", "EPGains owns EP/MP/AP combat-power scaling; Re:Monster has no parallel EP-to-stat curve");
        infoIfLoaded(mods, "tensura_better_subs", "Better Subordinates owns subordinate training counters and unlock progression");
        infoIfLoaded(mods, "mysticism", "Mysticism owns its races/skills and copy/paste skill mechanics");
        infoIfLoaded(mods, "mca", "MCA owns villager identity, Brain, family, profession, schedule and inventory");
        infoIfLoaded(mods, "townstead", "Townstead owns MCA phenotype/genetics/body rendering and species/personality voice");
        infoIfLoaded(mods, "mcacapitals", "MCA Capitals owns capital politics and capital-specific dialogue; Re:Monster composes it ahead of Townstead species voice");
        infoIfLoaded(mods, "mobchampions", "MobChampions owns random elite rank generation and champion effects/stats");
        infoIfLoaded(mods, "bossesrisetensuracompat", "Bosses'Rise Tensura compatibility owns Bosses'Rise EP/skills/ability behavior");
        infoIfLoaded(mods, "tensura_guild", "Tensura Guild owns adventurer guild ranks, parties and guild commissions");
        infoIfLoaded(mods, "mcaquests", "MCA: Quests owns villager-local quest state and turn-in flow");

        if (mods.isLoaded("tensura_skillobtain")) {
            ReMonsterIntegration.LOGGER.warn(
                    "[Re:Monster Authority] tensura_skillobtain should be removed from the target pack: it maintains a second authored "
                            + "EntityType -> direct skill-drop table for players, while Re:Monster already owns universal native "
                            + "acquisition/mastery from the defeated mob's real/profile skills."
            );
        }

        if (mods.isLoaded("tr_unique_monsters")) {
            ReMonsterIntegration.LOGGER.warn(
                    "[Re:Monster Authority] tr_unique_monsters must be removed from the target pack: it duplicates MobChampions with a second "
                            + "random larger/stronger elite layer. MobChampions + Re:Monster rank integration is the sole generic elite system."
            );
        }
    }

    private static void infoIfLoaded(ModList mods, String modId, String authority) {
        if (mods.isLoaded(modId)) {
            ReMonsterIntegration.LOGGER.info("[Re:Monster Authority] {}: {}", modId, authority);
        }
    }
}
