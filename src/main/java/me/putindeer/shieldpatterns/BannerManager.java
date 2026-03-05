package me.putindeer.shieldbanners;

import me.putindeer.shieldbanners.BannerDatabase;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.*;

public class BannerManager implements Listener {

    private final Main plugin;
    private final BannerDatabase database;
    private final Map<UUID, BannerMeta> cache = new HashMap<>();

    public BannerManager(Main plugin) {
        this.plugin = plugin;
        this.database = new BannerDatabase(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        BannerMeta meta = (BannerMeta) new ItemStack(Material.WHITE_BANNER).getItemMeta();
        cache.put(p.getUniqueId(), meta);

        for (String s : database.load(p.getUniqueId())) {
            String[] split = s.split(";");
            meta.addPattern(new Pattern(
                    DyeColor.valueOf(split[0]),
                    PatternType.valueOf(split[1])
            ));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        List<String> patterns = new ArrayList<>();
        getBanner(p).getPatterns().forEach(pattern ->
                patterns.add(pattern.getColor().name() + ";" + pattern.getPattern().name())
        );

        database.save(p.getUniqueId(), patterns);
        cache.remove(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onShieldClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("shieldbanners.applybanner")) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.SHIELD) return;

        applyBanner(player, item);
    }

    public void applyBanner(Player player, ItemStack shield) {
        BlockStateMeta meta = (BlockStateMeta) shield.getItemMeta();
        if (meta == null) return;

        if (meta.getBlockState() instanceof org.bukkit.block.Banner banner) {
            banner.setPatterns(getBanner(player).getPatterns());
            banner.update();
            meta.setBlockState(banner);
            shield.setItemMeta(meta);
        }
    }

    public BannerMeta getBanner(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(),
                id -> (BannerMeta) new ItemStack(Material.WHITE_BANNER).getItemMeta());
    }

    public void addPattern(Player player, Pattern pattern) {
        if (getBanner(player).getPatterns().size() >= 16) {
            plugin.utils.message(player, plugin.getMessages().getString("max-patterns"));
            return;
        }

        getBanner(player).addPattern(pattern);
    }

    public void reset(Player player) {
        cache.put(player.getUniqueId(),
                (BannerMeta) new ItemStack(Material.WHITE_BANNER).getItemMeta());
    }

    public static String capitalizeWord(String str) {
        String newString = str.toUpperCase();
        return newString.charAt(0) + newString.substring(1).toLowerCase();
    }
}