package me.eventually.carpethikariaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import me.eventually.carpethikariaddition.listeners.BlockUseListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Check CarpetExtension interface for more events. */
public class ExtensionServer implements ModInitializer, CarpetExtension {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.

    // !Replace this with your mods carpethikariaddition!
    public static final String MODID = "carpethikariaddition";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final SettingsManager mySettingManager =
            new SettingsManager("1.0.0", MODID, "CarpetHikariAddition");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        // This is also where we will initialize out carpet extension and Settings Manager!

        // we can't access carpet's settings manager here (yet)!
        UseBlockCallback.EVENT.register(BlockUseListener::onBlockUse);

        CarpetServer.manageExtension(new ExtensionServer());

        // Let's have our own settings class independent of carpet.conf
        mySettingManager.parseSettingsClass(ExtensionOwnSettings.class);




        LOGGER.info("Hikari Carpet Extension Loaded!");
        LOGGER.info("Love U! :3");
    }

    /**
     * Currently anything needing {@link CarpetServer#settingsManager} needs to be here because this
     * is right after it is initialized.
     */
    @Override
    public void onGameStarted() {
    }

    @Override
    public void registerCommands(
            CommandDispatcher<ServerCommandSource> dispatcher,
            final CommandRegistryAccess commandBuildContext) {}

    @Override
    public SettingsManager extensionSettingsManager() {
        // this will ensure that our settings are loaded properly when world loads
        return mySettingManager;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translations.getTranslationFromResourcePath(
                "assets/carpethikariaddition/lang/%s.json".formatted(lang));
    }
}
