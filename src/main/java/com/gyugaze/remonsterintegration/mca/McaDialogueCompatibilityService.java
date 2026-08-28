package com.gyugaze.remonsterintegration.mca;

import com.gyugaze.remonsterintegration.ReMonsterIntegration;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Composes the two installed MCA dialogue extensions that otherwise overwrite the same method.
 *
 * MCA Capitals gets first refusal for a capital/political phrase. If it has no specialized line,
 * Townstead may provide the villager's species/personality voice. Returning {@code null} means the
 * normal MCA Capitals/MCA implementation should continue and build its ordinary translation key.
 */
public final class McaDialogueCompatibilityService {
    private static final String MCA_CAPITALS_MOD_ID = "mcacapitals";
    private static final String TOWNSTEAD_MOD_ID = "townstead";
    private static final String CAPITAL_DIALOGUE_CLASS = "com.majesttyx.mcacapitals.dialogue.CapitalDialogueService";
    private static final String CAPITAL_DIALOGUE_METHOD = "maybeFormatMcaPhraseLine";

    private static volatile Method capitalDialogueMethod;
    private static volatile boolean capitalLookupAttempted;

    private McaDialogueCompatibilityService() {
    }

    public static MutableComponent resolve(
            VillagerEntityMCA villager,
            Player target,
            String phraseId,
            Object[] params
    ) {
        ModList mods = ModList.get();
        if (!mods.isLoaded(MCA_CAPITALS_MOD_ID) || !mods.isLoaded(TOWNSTEAD_MOD_ID)) {
            return null;
        }

        MutableComponent capitalLine = resolveCapitalLine(villager, target, phraseId);
        if (capitalLine != null) {
            return capitalLine;
        }

        return TownsteadVoiceBridge.resolve(villager, target, phraseId, params);
    }

    private static MutableComponent resolveCapitalLine(VillagerEntityMCA villager, Player target, String phraseId) {
        if (!(target instanceof ServerPlayer serverPlayer) || phraseId == null || phraseId.isEmpty()) {
            return null;
        }

        Method method = getCapitalDialogueMethod(villager.getClass().getClassLoader());
        if (method == null) {
            return null;
        }

        try {
            Object result = method.invoke(null, serverPlayer, (Entity) villager, phraseId);
            if (result instanceof String line && !line.isBlank()) {
                return Component.literal(line);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            ReMonsterIntegration.LOGGER.debug(
                    "MCA Capitals dialogue bridge failed for villager {} and phrase {}",
                    villager.getUUID(),
                    phraseId,
                    exception
            );
        }
        return null;
    }

    private static Method getCapitalDialogueMethod(ClassLoader loader) {
        if (capitalLookupAttempted) {
            return capitalDialogueMethod;
        }
        synchronized (McaDialogueCompatibilityService.class) {
            if (capitalLookupAttempted) {
                return capitalDialogueMethod;
            }
            try {
                Class<?> service = Class.forName(CAPITAL_DIALOGUE_CLASS, false, loader);
                capitalDialogueMethod = service.getMethod(
                        CAPITAL_DIALOGUE_METHOD,
                        ServerPlayer.class,
                        Entity.class,
                        String.class
                );
            } catch (ReflectiveOperationException | LinkageError exception) {
                ReMonsterIntegration.LOGGER.warn(
                        "MCA Capitals is loaded but its dialogue API could not be resolved; leaving its native overwrite authoritative",
                        exception
                );
            } finally {
                capitalLookupAttempted = true;
            }
            return capitalDialogueMethod;
        }
    }

    /** Kept in a nested class so Townstead classes are never resolved when Townstead is absent. */
    private static final class TownsteadVoiceBridge {
        private TownsteadVoiceBridge() {
        }

        private static MutableComponent resolve(
                VillagerEntityMCA villager,
                Player target,
                String phraseId,
                Object[] params
        ) {
            return com.aetherianartificer.townstead.dialogue.SpeciesVoice.line(villager, target, phraseId, params);
        }
    }
}
