package me.eventually.carpethikariaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Translations;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    //
    //		// let's /carpet handle our few simple settings
    //		CarpetServer.settingsManager.parseSettingsClass(ExampleSimpleSettings.class);
    //
    //		// set up a ruleObserver to observe when rules change in carpet.
    //		//only observes rules from the settings managers it has been registered to.
    //		// use registerGlobalObserver to observe rules from all settings managers (that allow for
    // this feature)
    //		CarpetServer.settingsManager.registerRuleObserver(ExtensionServer::exampleRuleObserver);
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
