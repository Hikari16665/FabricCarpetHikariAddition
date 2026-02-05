package me.eventually.carpethikariaddition;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.Validator;

import net.minecraft.server.command.ServerCommandSource;

import org.jetbrains.annotations.Nullable;

/**
 * Here is your example Settings class that will be a part of /{@link ExtensionServer} settings
 * command,
 *
 * <p>Descriptions, Extra and any other form of translation are handled by {@link
 * ExtensionServer#canHasTranslations(String)} and stored in assets/carpethikariaddition/lang/
 */
public class ExtensionOwnSettings {

    static class DayTimeValidator extends Validator<Long> {
        @Override
        public Long validate(
                @Nullable ServerCommandSource source,
                CarpetRule<Long> changingRule,
                Long newValue,
                String userInput) {
            return Math.max(0, Math.min(24000, newValue));
        }
    }

    public static final String MODID = "carpethikariaddition";
    public static final String MAINTENANCE = "maintenance";
    public static final String LEVEL = "level";
    public static final String ITEM = "item";
    public static final String ENTITY = "entity";
    public static final String EXPERIMENTAL = "experimental";

    @Rule(categories = {MODID, MAINTENANCE})
    public static boolean kelpBulkBreaking = false;

    @Rule(categories = {MODID, MAINTENANCE})
    public static boolean kelpBulkBreakingDrop = true;

    @Rule(categories = {MODID, MAINTENANCE})
    public static boolean kelpPreventGrowth = false;

    @Rule(categories = {MODID, LEVEL})
    public static boolean enableDayTimeOverride = false;

    @Rule(
            categories = {MODID, LEVEL},
            validators = DayTimeValidator.class)
    public static long dayTimeOverride = 0L;

    @Rule(categories = {MODID, ITEM})
    public static boolean fireChargeNoFireBlock = false;

    @Rule(categories = {MODID, ENTITY})
    public static boolean fastFallingAnvil = false;

    @Rule(categories = {MODID, ENTITY})
    public static boolean villagerFastGrow = false;

    @Rule(categories = {MODID, EXPERIMENTAL})
    public static boolean shulkerBoxBulkCrafting = false;

    @Rule(categories = {MODID, ITEM})
    public static boolean shulkerBoxBlockPlace = false;
}
