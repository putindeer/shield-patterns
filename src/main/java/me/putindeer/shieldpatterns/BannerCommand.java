package me.putindeer.shieldpatterns;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BannerCommand implements CommandExecutor {
    private final Main plugin;

    public BannerCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("shieldbanner")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("shieldreset")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shieldreset")) {
            handleShieldReset(sender, args);
        }

        handleShieldGUI(sender);
        return true;
    }

    /**
     * Handles the reset of a player's shield banner. This command can only be executed
     * by a sender with the appropriate permissions. It resets the target player's shield
     * banner patterns, both in the session and in the database, and notifies both the
     * sender and the target player about the reset.
     *
     * @param sender The entity that executed the command. Must have the "shieldpatterns.admin" permission.
     * @param args   The arguments provided with the command. The first argument should be the target player's name.
     *               If no arguments are provided or the target player is not found, appropriate error messages are sent to the sender.
     */
    private void handleShieldReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shieldpatterns.admin")) {
            plugin.utils.message(sender, plugin.getMessages().getString("no-permission"));
            return;
        }

        if (args.length == 0) {
            plugin.utils.message(sender, plugin.getMessages().getString("shieldreset.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            String playerNotFound = plugin.getMessages().getString("shieldreset.not-found");
            assert playerNotFound != null;
            plugin.utils.message(sender, playerNotFound.replace("%player%", args[0]));
            return;
        }

        plugin.manager.reset(target);

        String bannerResetMessage = plugin.getMessages().getString("shieldreset.success");
        assert bannerResetMessage != null;
        plugin.utils.message(sender, bannerResetMessage.replace("%player%", target.getName()));

        String bannerResetTargetMessage = plugin.getMessages().getString("shieldreset.resetted");
        assert bannerResetTargetMessage != null;
        plugin.utils.message(target, bannerResetTargetMessage.replace("%player%", sender.getName()));
    }

    /**
     * Handles the display of the shield customization GUI for a player.
     * This method checks if the sender is a player and whether they have
     * the required permission to access the GUI. If the conditions are met,
     * it opens the BannerGUI for the player.
     *
     * @param sender The entity that executed the command. Must be a player instance
     *               and have the "shieldpatterns.shieldbannercreate" permission to access the GUI.
     */
    private void handleShieldGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) return;

        if (!player.hasPermission("shieldpatterns.shieldbannercreate")) {
            plugin.utils.message(player, plugin.getMessages().getString("no-permission"));
        }

        new BannerGUI(player, plugin);
    }
}