package me.putindeer.shieldpatterns;

import fr.mrmicky.fastinv.FastInvManager;
import lombok.Getter;
import me.putindeer.api.util.PluginUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {
    public PluginUtils utils;
    public BannerManager manager;
    @Getter private FileConfiguration messages;

    @Override
    public void onEnable() {
        String prefix = getConfig().getString("prefix", "<dark_gray>[<aqua>shield-patterns</aqua>]<reset>");
        utils = new PluginUtils(this, prefix);
        startup();
        manager = new BannerManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Initializes the plugin and prepares the necessary resources for it to function correctly.
     * This method performs the following steps:
     * <br><br>
     * 1. Creates the plugin's data folder if it does not already exist. If the folder is created successfully,
     *    it triggers the {@code printFirstRunGuide()} method to display a guide for first-time users.
     * <br><br>
     * 2. Saves the default configuration file from the plugin's resources into the data folder if a configuration
     *    file does not already exist. This is done to ensure configuration defaults are available.
     * <br><br>
     * 3. Loads the plugin's custom messages from the "messages.yml" file into memory using the {@code loadMessages()} method.
     * <br><br>
     * 4. Initializes the reload functionality by registering the {@code ReloadPlugin} class, which manages the
     *    "/shieldreload" command for reloading the plugin's configurations dynamically.
     * <br><br>
     * 5. Registers the plugin with the {@code FastInvManager}, enabling the use of fast inventory tools.
     */
    public void startup() {
        if (getDataFolder().mkdirs()) {
            printFirstRunGuide();
        }
        saveDefaultConfig();
        loadMessages();
        new ReloadPlugin(this);
        FastInvManager.register(this);
    }

    /**
     * Reloads the plugin's configuration and related resources.
     * This method performs the following tasks:
     * - Reloads the messages from the "messages.yml" file using the {@link #loadMessages()} method.
     * - Reloads the primary plugin configuration.
     * - Updates the utility class's prefix value using the configuration's "prefix" key,
     *   defaulting to "<dark_gray>[<aqua>shield-patterns</aqua>]<reset>" if the key is not found.
     * - Refreshes the BannerManager configuration using the {@code manager.refreshConfig()} method,
     *   ensuring the latest settings are applied.
     */
    public void reloadPlugin() {
        loadMessages();
        reloadConfig();
        utils.setPrefix(getConfig().getString("prefix", "<dark_gray>[<aqua>shield-patterns</aqua>]<reset>"));
        manager.refreshConfig();
    }

    /**
     * Loads the messages from the "messages.yml" configuration file located in the plugin's data folder.
     * If the file does not exist, it copies a default version of "messages.yml" from the plugin's resources.
     * The loaded configuration is stored in the 'messages' field.
     */
    public void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    private void printFirstRunGuide() {
        utils.log(
                "<green>✦ thanks for using shield-patterns!",
                "<green>it seems this is your first time running this plugin",
                "<green>here's a quick guide to get you started:",
                "<green>─────────────────────────────────────────",
                "<green>commands:",
                "<green>  /shieldbanner or /banner  - opens the banner creator gui",
                "<green>  /shieldreset <player>     - reset a player's shield banner",
                "<green>  /shieldreload             - reload config.yml and messages.yml",
                "<green>  ─────────────────────────────────────────",
                "<green>permissions:",
                "<green>  shieldpatterns.shieldbannercreate - use /shieldbanner command",
                "<green>  shieldpatterns.shieldbanneruse    - apply banner to shields on click",
                "<green>  shieldpatterns.admin              - use /shieldreset and /shieldreload",
                "<green>─────────────────────────────────────────",
                "<green>config (plugins/shield-patterns/config.yml):",
                "<green>prefix:                    plugin prefix shown in messages",
                "<green>max-patterns:              max patterns a player can stack (default: 32)",
                "<green>should-override:           if the player's pattern should override existing shield patterns (default: true)",
                "<green>hide-vanilla-pattern-name: if the vanilla pattern name should be hidden (default: true)",
                "<green>gui-show-result:           if the result of the banner should be shown in the banner pattern gui (default: false)",
                "<green>─────────────────────────────────────────",
                "<green>you can also configure every single message at messages.yml",
                "<green>✦ i have more open-source resources available for you at:",
                "<aqua>  https://github.com/putindeer");
    }
}
