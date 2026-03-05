package me.putindeer.shieldpatterns;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BannerPatternLayers;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BannerManager implements Listener {
    private final Main plugin;
    private final BannerDatabase database;
    private final Map<UUID, List<Pattern>> sessionPatterns = new HashMap<>();
    @Getter private int maxPatterns;
    public boolean shouldHideVanillaPatternsName;
    private boolean shouldOverride;
    public boolean shouldShowResult;

    public BannerManager(Main plugin) {
        this.plugin = plugin;
        this.database = new BannerDatabase(plugin);
        new BannerCommand(plugin);
        refreshConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Called when a player clicks on an inventory slot. This method checks if the clicked item
     * is a shield and if the player has the relevant permissions to apply banner patterns.
     * If valid, the method applies the player's saved banner patterns to the shield.
     *
     * @param event The InventoryClickEvent triggered when a player interacts with an inventory.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onShieldClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("shieldpatterns.shieldbanneruse")) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.SHIELD) return;

        List<Pattern> patterns = loadFromDatabase(player);
        if (patterns.isEmpty()) return;

        applyBanner(player, item, patterns);
    }

    /**
     * Returns the current in-session patterns for this player.
     * If the session is fresh (first time opening GUI), loads existing patterns from DB.
     */
    public List<Pattern> getSessionPatterns(Player player) {
        return sessionPatterns.computeIfAbsent(player.getUniqueId(), id -> loadFromDatabase(player));
    }

    /**
     * Adds a pattern to the session and immediately saves all patterns to the DB.
     */
    public void addPattern(Player player, Pattern pattern) {
        List<Pattern> patterns = getSessionPatterns(player);

        if (patterns.size() >= getMaxPatterns()) {
            plugin.utils.message(player, plugin.getMessages().getString("max-patterns"));
            return;
        }

        patterns.add(pattern);
        saveToDatabase(player, patterns);
    }

    /**
     * Clears the session and deletes the player's DB entry.
     */
    public void reset(Player player) {
        sessionPatterns.remove(player.getUniqueId());
        database.delete(player.getUniqueId());
    }

    /**
     * Applies a set of banner patterns to the specified shield.
     *
     * @param player   The player for whom the shield is being updated.
     * @param shield   The ItemStack representing the shield to which the patterns will be applied.
     * @param patterns The list of patterns to be applied to the shield.
     */
    private void applyBanner(Player player, ItemStack shield, List<Pattern> patterns) {
        BannerPatternLayers previousPatterns = shield.getData(DataComponentTypes.BANNER_PATTERNS);
        if (previousPatterns != null && !previousPatterns.patterns().isEmpty() && !shouldOverride) {
            return;
        }
        shield.setData(DataComponentTypes.BANNER_PATTERNS, BannerPatternLayers.bannerPatternLayers(patterns));
    }

    /**
     * Loads patterns from the DB and deserializes them.
     * Format stored: "COLOR;PATTERN_KEY" per entry, comma-separated.
     */
    private List<Pattern> loadFromDatabase(Player player) {
        List<String> raw = database.load(player.getUniqueId());
        List<Pattern> patterns = new ArrayList<>();

        for (String string : raw) {
            try {
                String[] split = string.split(";");
                DyeColor color = DyeColor.valueOf(split[0]);
                PatternType patternType = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.BANNER_PATTERN)
                        .get(NamespacedKey.minecraft(split[1]));
                if (patternType == null) throw new IllegalArgumentException("Unknown pattern: " + split[1]);
                patterns.add(new Pattern(color, patternType));
            } catch (Exception e) {
                plugin.utils.severe("Failed to deserialize pattern: " + string);
            }
        }

        return patterns;
    }

    /**
     * Serializes and saves all patterns to the DB immediately.
     */
    private void saveToDatabase(Player player, List<Pattern> patterns) {
        List<String> serialized = new ArrayList<>();
        for (Pattern pattern : patterns) {
            NamespacedKey key = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN)
                    .getKey(pattern.getPattern());
            if (key == null) {
                plugin.utils.severe("Failed to serialize pattern, skipping: " + pattern.getPattern());
                continue;
            }
            serialized.add(pattern.getColor() + ";" + key.getKey());
        }
        database.save(player.getUniqueId(), serialized);
    }

    /**
     * Refreshes the configuration settings for the BannerManager.
     * This method retrieves and updates the following settings from the plugin's configuration:
     * - maxPatterns: The maximum number of patterns a player can stack, defaulting to 32 if not set.
     * - shouldHideVanillaPatternsName: Determines if the vanilla pattern name should be hidden, defaulting to false if not set.
     * - shouldOverride: Indicates whether the player's pattern should override existing shield patterns, defaulting to true if not set.
     * This method ensures the BannerManager operates based on the latest configuration values.
     */
    public void refreshConfig() {
        maxPatterns = plugin.getConfig().getInt("max-patterns", 32);
        shouldHideVanillaPatternsName = plugin.getConfig().getBoolean("hide-vanilla-pattern-name", false);
        shouldOverride = plugin.getConfig().getBoolean("should-override", true);
        shouldShowResult = plugin.getConfig().getBoolean("gui-show-result", false);
    }
}