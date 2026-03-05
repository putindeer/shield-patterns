package me.putindeer.shieldbanners;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BannerCommand implements CommandExecutor {
    private final Main plugin;

    public BannerCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("banner")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("uhc.banner")) {
            plugin.utils.message(player, plugin.getMessages().getString("no-permission"));
            return true;
        }

        new BannerGUI(player, plugin);
        return true;
    }
}