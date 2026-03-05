package me.putindeer.shieldpatterns;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.InventoryScheme;
import fr.mrmicky.fastinv.PaginatedFastInv;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import me.putindeer.api.util.builder.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class BannerGUI {
    private final Main plugin;
    private final Player player;

    private DyeColor selectedColor = DyeColor.BLACK;
    private final List<DyeColor> dyeColors = List.of(DyeColor.values());
    private final List<PatternType> patternTypes = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BANNER_PATTERN).stream().toList();

    public BannerGUI(Player player, Main plugin) {
        this.plugin = plugin;
        this.player = player;

        openColorMenu();
    }

    /**
     * Opens the color selection menu in the player's inventory GUI. This menu allows
     * the player to select a color for use in banner or shield customization. Each color
     * is represented by an item in the inventory, accompanied by a name and description
     * retrieved from the plugin's messages configuration. Available colors are automatically
     * populated from the server's supported dye colors.
     * <br><br>
     * The menu also includes navigation items for selecting and previewing banners and
     * shields, as well as a reset function to clear the player's current customization state.
     * <br><br>
     * The inventory is non-interactive for non-designated slots to prevent unintended
     * actions, and the menu automatically refreshes based on the player's interactions.
     */
    private void openColorMenu() {
        FastInv inv = new FastInv(owner -> Bukkit.createInventory(owner, 27, plugin.utils.chat(plugin.getMessages().getString("gui.color-title"))));

        int[] border = {18, 19, 20, 22, 24, 25, 26};
        inv.setItems(border, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());

        inv.setItem(21, buildPreviewBanner(), e -> {
            plugin.manager.reset(player);
            openColorMenu();
        });
        inv.setItem(23, buildPreviewShield(), e -> {
            plugin.manager.reset(player);
            openColorMenu();
        });

        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < dyeColors.size() && i < slots.length; i++) {
            DyeColor color = dyeColors.get(i);
            int slot = slots[i];

            String colorName = plugin.getMessages().getString("gui.colors." + color.name().toLowerCase(), color.name());
            List<String> colorLore = plugin.getMessages().getStringList("gui.color-description")
                    .stream().map(line -> line.replace("%color%", colorName)).toList();
            inv.setItem(slot, plugin.utils.ib(Objects.requireNonNull(Material.matchMaterial(color.name() + "_WOOL"))).name(colorName).lore(colorLore).build(),
                    e -> {
                        selectedColor = color;
                        openPatternMenu(1);
                    });
        }

        inv.addClickHandler(event -> event.setCancelled(true));
        inv.open(player);
    }

    /**
     * Opens the pattern selection menu for the player. The menu allows the player
     * to view and choose patterns for banners or shields, based on the selected
     * color. The inventory includes navigation buttons, pattern items, preview
     * items, and a back button to return to the previous menu.
     *
     * @param page The page number of the pattern menu to display.
     */
    private void openPatternMenu(int page) {
        String colorString = plugin.getMessages().getString("gui.colors." + selectedColor.name().toLowerCase(), "");
        String hex = colorString.replaceAll(".*<color:(#[0-9A-Fa-f]{6})>.*", "$1");
        String hexString = "<color:" + hex + ">";
        PaginatedFastInv inv = new PaginatedFastInv(owner -> Bukkit.createInventory(owner, 54,
                plugin.utils.chat(hexString + plugin.getMessages().getString("gui.pattern-title"))));

        new InventoryScheme()
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("000000000")
                .bindPagination('m')
                .apply(inv);

        int[] border = {46, 47, 51, 52};
        inv.setItems(border, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());

        inv.setItem(48, buildPreviewBanner(), e -> {
            plugin.manager.reset(player);
            openPatternMenu(inv.currentPage());
        });
        inv.setItem(50, buildPreviewShield(), e -> {
            plugin.manager.reset(player);
            openPatternMenu(inv.currentPage());
        });

        inv.setItem(49,
                plugin.utils.ib(Material.BARRIER)
                        .name(plugin.getMessages().getString("gui.back.name"))
                        .lore(plugin.getMessages().getStringList("gui.back.description"))
                        .build(),
                e -> openColorMenu());

        Material bannerMaterial = selectedColor == DyeColor.WHITE ? Material.BLACK_BANNER : Material.WHITE_BANNER;
        for (PatternType patternType : patternTypes) {
            NamespacedKey patternNSKey = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BANNER_PATTERN).getKey(patternType);
            String patternKey = patternNSKey != null ? patternNSKey.getKey() : patternType.toString().toLowerCase();
            String patternName = hexString + plugin.getMessages().getString("gui.patterns." + patternKey, patternKey);
            List<String> patterndescription = plugin.getMessages().getStringList("gui.pattern-description")
                    .stream().map(line -> line.replace("%pattern%", patternName)).toList();


            ItemBuilder bannerBuilder = plugin.utils.ib(Material.WHITE_BANNER);

            if (plugin.manager.shouldShowResult) {
                List<Pattern> patterns = plugin.manager.getSessionPatterns(player);
                bannerBuilder = bannerBuilder.pattern(patterns.toArray(Pattern[]::new));
            }

            bannerBuilder = bannerBuilder.name(patternName)
                    .lore(patterndescription)
                    .pattern(new Pattern(selectedColor, patternType));

            if (plugin.manager.shouldHideVanillaPatternsName) {
                bannerBuilder.hideBannerPatterns();
            }

            ItemStack bannerItem = bannerBuilder.build();

            inv.addContent(bannerItem, e -> {
                List<Pattern> current = plugin.manager.getSessionPatterns(player);

                if (current.size() >= plugin.manager.getMaxPatterns()) {
                    plugin.utils.message(player, plugin.getMessages().getString("max-patterns"));
                    return;
                }

                plugin.manager.addPattern(player, new Pattern(selectedColor, patternType));
                openPatternMenu(inv.currentPage());
            });
        }

        setupNavigationButtons(inv);

        inv.addClickHandler(event -> event.setCancelled(true));
        inv.openPage(page);
        inv.open(player);
    }

    /**
     * Configures the navigation buttons for a paginated inventory interface.
     * This method sets the navigation items (Next Page and Previous Page)
     * based on the current page and total pages of the inventory. If navigation
     * is not possible in a specific direction (e.g., first page for previous
     * or last page for next), a disabled button is shown instead.
     *
     * @param inv The paginated inventory object for which the navigation buttons
     *            are being set up.
     */
    private void setupNavigationButtons(PaginatedFastInv inv) {
        if (!inv.isLastPage() && inv.lastPage() != 1) {
            inv.setItem(53,
                    plugin.utils.ib(Material.ARROW)
                            .name(plugin.getMessages().getString("gui.next-page.name"))
                            .lore(plugin.getMessages().getStringList("gui.next-page.description"))
                            .build(),
                    event -> {
                        inv.openNext();
                        setupNavigationButtons(inv);
                    });
        } else {
            inv.setItem(53, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());
        }

        if (!inv.isFirstPage()) {
            inv.setItem(45,
                    plugin.utils.ib(Material.ARROW)
                            .name(plugin.getMessages().getString("gui.previous-page.name"))
                            .lore(plugin.getMessages().getStringList("gui.previous-page.description"))
                            .build(),
                    event -> {
                        inv.openPrevious();
                        setupNavigationButtons(inv);
                    });
        } else {
            inv.setItem(45, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());
        }
    }

    /**
     * Builds and returns a preview banner ItemStack for the player.
     * The banner includes the current session patterns, as well as a name
     * and lore retrieved from the plugin's configuration and session data.
     *
     * @return An ItemStack representing the preview banner with applied patterns,
     *     customized name, and lore.
     */
    private ItemStack buildPreviewBanner() {
        List<Pattern> patterns = plugin.manager.getSessionPatterns(player);
        return plugin.utils.ib(Material.WHITE_BANNER)
                .name(plugin.getMessages().getString("gui.your-banner.name"))
                .lore(plugin.getMessages().getStringList("gui.your-banner.description"))
                .pattern(patterns.toArray(Pattern[]::new))
                .build();
    }

    /**
     * Builds and returns a preview shield ItemStack for the player.
     * The shield includes the current session patterns, name, and lore as specified
     * in the plugin's configuration and session data.
     *
     * @return An ItemStack representing the preview shield with the applied patterns, name, and lore.
     */
    private ItemStack buildPreviewShield() {
        List<Pattern> patterns = plugin.manager.getSessionPatterns(player);
        return plugin.utils.ib(Material.SHIELD)
                .name(plugin.getMessages().getString("gui.your-shield.name"))
                .lore(plugin.getMessages().getStringList("gui.your-shield.description"))
                .pattern(patterns.toArray(Pattern[]::new))
                .build();
    }
}