package com.gyugaze.remonsterintegration.evolution;

import com.aetherianartificer.townstead.pheno.power.Power;
import com.aetherianartificer.townstead.pheno.power.Powers;
import com.aetherianartificer.townstead.root.ExpressedGenesS2CPayload;
import com.aetherianartificer.townstead.root.Heredity;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.Root;
import com.aetherianartificer.townstead.root.RootGenes;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.aetherianartificer.townstead.root.RootSetC2SPayload;
import com.aetherianartificer.townstead.root.RootSyncS2CPayload;
import com.aetherianartificer.townstead.root.ability.GeneAbilityTicker;
import com.aetherianartificer.townstead.root.attribute.GeneAttributeApplier;
import com.aetherianartificer.townstead.root.gene.Allele;
import com.aetherianartificer.townstead.root.gene.Genotype;
import com.aetherianartificer.townstead.root.reproduction.Fertility;
import com.aetherianartificer.townstead.villager.TownsteadVillager;
import com.aetherianartificer.townstead.villager.TownsteadVillagers;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Optional Townstead 0.7.6 body adapter.
 *
 * <p>The MCA path mutates the existing villager in-place. It deliberately does not call
 * RootServerLogic.applyOrRequest because that editor-oriented path rerolls personality and the
 * whole founder genome. Evolution instead preserves the existing person and only transforms
 * traits that are incompatible with the target root.</p>
 */
public final class TownsteadEvolutionCompat {
    private TownsteadEvolutionCompat() {
    }

    public static ApplyResult apply(
            LivingEntity entity,
            ResourceLocation semanticSpecies,
            SpeciesBodyProfile.TownsteadBody body
    ) {
        Optional<ResourceLocation> rootOptional = resolveRoot(body);
        if (rootOptional.isEmpty()) {
            return ApplyResult.failure(
                    "None of the configured Townstead roots exist for " + semanticSpecies
                            + " (candidates=" + body.rootCandidates()
                            + ", searchTerms=" + body.rootSearchTerms() + ")"
            );
        }

        ResourceLocation targetRoot = rootOptional.get();
        try {
            if (entity instanceof ServerPlayer player) {
                applyPlayer(player, targetRoot, body);
                return ApplyResult.success("townstead player root=" + targetRoot);
            }
            if (entity instanceof VillagerEntityMCA villager) {
                applyVillager(villager, targetRoot, body);
                return ApplyResult.success("townstead MCA root=" + targetRoot);
            }
            return ApplyResult.failure("Townstead body adapter only supports players and MCA villagers");
        } catch (RuntimeException ex) {
            return ApplyResult.failure(
                    "Townstead physical transformation failed for root " + targetRoot
                            + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    public static List<ResourceLocation> loadedRoots() {
        return RootRegistry.all().stream()
                .map(Root::id)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private static void applyPlayer(
            ServerPlayer player,
            ResourceLocation targetRoot,
            SpeciesBodyProfile.TownsteadBody body
    ) {
        Genotype previous = PlayerRoot.getGenotype(player).copy();
        List<Power> oldPowers = new ArrayList<>(Powers.active(player));

        PlayerRoot.setRootId(player, targetRoot.toString());
        PlayerRoot.setGenotype(
                player,
                transformedGenotype(player, previous, targetRoot, body.preserveSharedLoci())
        );
        resetPassives(player, oldPowers);
        syncPlayer(player, targetRoot);
    }

    private static void applyVillager(
            VillagerEntityMCA villager,
            ResourceLocation targetRoot,
            SpeciesBodyProfile.TownsteadBody body
    ) {
        TownsteadVillager state = TownsteadVillagers.get(villager);
        TownsteadVillager.Life life = state.life();
        Genotype previous = life.genotype().copy();
        List<Power> oldPowers = new ArrayList<>(Powers.active(villager));

        life.setRoot(targetRoot.toString());
        life.setGenotype(
                transformedGenotype(villager, previous, targetRoot, body.preserveSharedLoci())
        );
        life.setHeritage(RootRegistry.seedHeritage(targetRoot));
        Heredity.recomputeExpressed(life);

        if (body.clampBodyMetrics()) {
            RootGenes.clamp(
                    villager,
                    RootGenes.resolveBodyMetrics(RootRegistry.effectiveInheritedGenes(targetRoot))
            );
        }

        Fertility.syncMcaInfertileTrait(villager);
        villager.refreshDimensions();
        TownsteadVillagers.flush(villager);
        resetPassives(villager, oldPowers);
        syncVillager(villager, targetRoot);
    }

    /**
     * Roll target-only genes deterministically, then carry forward old alleles only when their
     * exact gene still exists in the target genome at the same locus.
     *
     * <p>This is the important phenotype rule: shared traits stay recognisably the same person,
     * while race-specific skin/ears/horns are replaced by the target race instead of leaking a
     * Goblin allele into an Ogre body. The roll uses Re:Monster's persisted phenotype seed, so
     * save/reload never reshuffles the result.</p>
     */
    private static Genotype transformedGenotype(
            LivingEntity entity,
            Genotype previous,
            ResourceLocation targetRoot,
            boolean preserveSharedLoci
    ) {
        long seed = SpeciesStateService.seedForSpecies(entity, targetRoot);
        Genotype target = Heredity.seedGenotype(targetRoot, RandomSource.create(seed));
        if (!preserveSharedLoci || previous.isEmpty()) {
            return target;
        }

        Set<ResourceLocation> targetGenes = new HashSet<>();
        RootRegistry.effectiveInheritedGenes(targetRoot).forEach(ref -> targetGenes.add(ref.geneId()));

        for (ResourceLocation locus : target.loci()) {
            Allele[] oldPair = previous.at(locus);
            if (oldPair == null || !compatible(oldPair[0], targetGenes) || !compatible(oldPair[1], targetGenes)) {
                continue;
            }
            target.set(locus, oldPair[0], oldPair[1]);
        }
        return target;
    }

    private static boolean compatible(Allele allele, Set<ResourceLocation> targetGenes) {
        return allele == null || allele.isWild() || targetGenes.contains(allele.geneId());
    }

    private static Optional<ResourceLocation> resolveRoot(SpeciesBodyProfile.TownsteadBody body) {
        for (ResourceLocation candidate : body.rootCandidates()) {
            if (RootRegistry.byId(candidate) != null) {
                return Optional.of(candidate);
            }
        }

        List<Root> roots = RootRegistry.all().stream()
                .sorted(Comparator.comparing(root -> root.id().toString()))
                .toList();
        for (String searchTerm : body.rootSearchTerms()) {
            String needle = searchTerm.toLowerCase(Locale.ROOT);
            for (Root root : roots) {
                if (root.id().getPath().toLowerCase(Locale.ROOT).contains(needle)) {
                    return Optional.of(root.id());
                }
            }
        }
        return Optional.empty();
    }

    private static void resetPassives(LivingEntity entity, List<Power> oldPowers) {
        GeneAbilityTicker.resetPassives(entity);
        GeneAttributeApplier.removeFor(entity, oldPowers);
    }

    private static void syncPlayer(ServerPlayer player, ResourceLocation targetRoot) {
        RootSyncS2CPayload selfSync =
                new RootSyncS2CPayload(RootSetC2SPayload.SELF, targetRoot.toString());
        RootSyncS2CPayload entitySync =
                new RootSyncS2CPayload(player.getId(), targetRoot.toString());
        ExpressedGenesS2CPayload genes =
                ExpressedGenesS2CPayload.forEntity(player.getId(), player);

        PacketDistributor.sendToPlayer(player, selfSync);
        PacketDistributor.sendToPlayer(player, entitySync);
        PacketDistributor.sendToPlayer(player, genes);
        PacketDistributor.sendToPlayersTrackingEntity(player, entitySync);
        PacketDistributor.sendToPlayersTrackingEntity(player, genes);
    }

    private static void syncVillager(VillagerEntityMCA villager, ResourceLocation targetRoot) {
        RootSyncS2CPayload rootSync =
                new RootSyncS2CPayload(villager.getId(), targetRoot.toString());
        ExpressedGenesS2CPayload genes =
                ExpressedGenesS2CPayload.forEntity(villager.getId(), villager);
        PacketDistributor.sendToPlayersTrackingEntity(villager, rootSync);
        PacketDistributor.sendToPlayersTrackingEntity(villager, genes);
    }

    public record ApplyResult(boolean success, String description, String failure) {
        public static ApplyResult success(String description) {
            return new ApplyResult(true, description, "");
        }

        public static ApplyResult failure(String failure) {
            return new ApplyResult(false, "", failure);
        }
    }
}
