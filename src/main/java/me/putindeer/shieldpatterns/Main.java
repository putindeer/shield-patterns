package me.putindeer.shieldbanners;

import lombok.Getter;
import me.putindeer.api.util.PluginUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {
    public final PluginUtils utils = new PluginUtils(this, "ShieldBanners");
    public final BannerManager manager = new BannerManager(this);
    @Getter private FileConfiguration messages;

    @Override
    public void onEnable() {
        loadMessages();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
    }
}
