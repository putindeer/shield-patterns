package me.putindeer.shieldpatterns;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ReloadPlugin implements CommandExecutor {
    private final Main plugin;
    public ReloadPlugin(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("shieldreload")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("shieldpatterns.admin")) {
            plugin.utils.message(sender, plugin.getMessages().getString("no-permission"));
            return true;
        }

        plugin.reloadPlugin();
        plugin.utils.message(sender, plugin.getMessages().getString("reload-success"));
        return true;
    }
}
