package com.gyugaze.remonsterintegration.compat;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.neoforged.fml.ModList;

/**
 * Documents and reports which installed mod owns each overlapping gameplay domain in the target pack.
 *
 * The integration addon should bridge foreign state, not silently create competing sources of truth.
 * This startup audit makes intentional authority decisions visible in logs and flags legacy overlaps
 * that should be disabled or removed from the assembled modpack.
 */
public final class CompatibilityAuthorityPolicy {
    private CompatibilityAuthorityPolicy() {
    }

    public static void logInstalledAuthorities() {
        ModList mods = ModList.get();

        infoIfLoaded(mods, "manascore", "ManasCore/Tensura owns native race and skill storage/lifecycle");
        infoIfLoaded(mods, "tensura", "Tensura owns existence/EP, native race evolution and native subordinate behavior");
        infoIfLoaded(mods, "tensura_epfix", "Tensura Modifications owns participant EP distribution and aura/magicule regeneration");
        infoIfLoaded(mods, "epgains", "EPGains owns EP/MP/AP combat power scaling; Re:Monster fallback stat scaling is disabled");
        infoIfLoaded(mods, "tensura_better_subs", "Better Subordinates owns subordinate training counters and unlock progression");
        infoIfLoaded(mods, "mysticism", "Mysticism owns its races/skills and copy/paste skill mechanics");
        infoIfLoaded(mods, "mca", "MCA owns villager identity, Brain, family, profession, schedule and inventory");
        infoIfLoaded(mods, "townstead", "Townstead owns MCA phenotype/genetics/body rendering; Re:Monster only maps native race to a body recipe");
        infoIfLoaded(mods, "mobchampions", "MobChampions owns random elite rank generation and champion effects/stats");
        infoIfLoaded(mods, "bossesrisetensuracompat", "Bosses'Rise Tensura compatibility owns Bosses'Rise EP/skills/ability behavior");
        infoIfLoaded(mods, "tensura_guild", "Tensura Guild owns adventurer guild ranks, parties and guild commissions");
        infoIfLoaded(mods, "mcaquests", "MCA: Quests owns villager-local quest state and turn-in flow");

        if (mods.isLoaded("tensura_skillobtain")) {
            ReMonsterIntegration.LOGGER.warn(
                    "[Re:Monster Authority] tensura_skillobtain is a legacy overlap: it directly grants player skills on kill, "
                            + "while Re:Monster owns universal acquisition/mastery learning. Remove tensura_skillobtain from the target pack. "
                            + "Until removal, Re:Monster suppresses its player kill-learning path to avoid double processing."
            );
        }

        if (mods.isLoaded("tr_unique_monsters")) {
            ReMonsterIntegration.LOGGER.warn(
                    "[Re:Monster Authority] tr_unique_monsters overlaps MobChampions with its own random larger/stronger EP-scaled monsters. "
                            + "MobChampions must remain the sole generic elite generator. Disable Unique Monsters spawning/chance in that mod's "
                            + "server config if keeping it for Appraisal Eye, or remove the mod entirely if Appraisal Eye is not desired."
            );
        }
    }

    private static void infoIfLoaded(ModList mods, String modId, String authority) {
        if (mods.isLoaded(modId)) {
            ReMonsterIntegration.LOGGER.info("[Re:Monster Authority] {}: {}", modId, authority);
        }
    }
}
