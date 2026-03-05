package me.putindeer.shieldbanners;

import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.InventoryScheme;
import fr.mrmicky.fastinv.PaginatedFastInv;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.List;
import java.util.Objects;

public class BannerGUI {
    private final Main plugin;
    private final Player player;

    private DyeColor selectedColor = DyeColor.BLACK;
    private final List<DyeColor> dyeColors = List.of(DyeColor.values());
    private final List<PatternType> patternTypes = RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).stream().toList();

    public BannerGUI(Player player, Main plugin) {
        this.plugin = plugin;
        this.player = player;

        openColorMenu();
    }

    private void openColorMenu() {
        FastInv inv = new FastInv(owner -> Bukkit.createInventory(owner, 27, plugin.utils.chat(plugin.getMessages().getString("gui.color-title"))));

        int[] border = {18, 19, 20, 21, 23, 24, 25, 26};
        inv.setItems(border, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());

        inv.setItem(22, buildPreviewBanner(), e -> {
            plugin.manager.reset(player);
            openColorMenu();
        });

        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < 16; i++) {
            DyeColor color = dyeColors.get(i);
            int slot = slots[i];

            inv.setItem(slot,
                    new ItemStack(Objects.requireNonNull(Material.matchMaterial(color.name() + "_WOOL"))),
                    e -> {
                        selectedColor = color;
                        openPatternMenu(1);
                    });
        }

        inv.open(player);
    }

    private void openPatternMenu(int page) {
        PaginatedFastInv inv = new PaginatedFastInv(owner -> Bukkit.createInventory(owner, 54, plugin.utils.chat(plugin.getMessages().getString("gui.pattern-title"))));

        new InventoryScheme()
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("mmmmmmmmm")
                .mask("000000000")
                .bindPagination('m').apply(inv);

        int[] border = {45, 46, 47, 48, 50, 51, 52, 53};
        inv.setItems(border, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());

        inv.setItem(49, buildPreviewBanner(), e -> {
            plugin.manager.reset(player);
            openPatternMenu(inv.currentPage());
        });

        String goBackName = plugin.getMessages().getString("gui.back.name");
        List<String> goBackLore = plugin.getMessages().getStringList("gui.back.lore");
        inv.setItem(53, plugin.utils.ib(Material.BARRIER).name(goBackName).lore(goBackLore).build(), e -> openColorMenu());

        for (PatternType pattern : patternTypes) {

            ItemStack bannerItem = new ItemStack(Material.WHITE_BANNER);
            BannerMeta meta = (BannerMeta) bannerItem.getItemMeta();

            meta.addPattern(new Pattern(
                    selectedColor == DyeColor.WHITE ? DyeColor.BLACK : DyeColor.WHITE,
                    PatternType.BASE
            ));

            meta.addPattern(new Pattern(selectedColor, pattern));
            bannerItem.setItemMeta(meta);

            inv.addContent(bannerItem, e -> {
                if (plugin.manager.getBanner(player).getPatterns().size() >= 16) {
                    plugin.utils.message(player, plugin.getMessages().getString("max-patterns"));
                    return;
                }

                plugin.manager.addPattern(player, new Pattern(selectedColor, pattern));
                openPatternMenu(inv.currentPage());
            });
        }

        inv.openPage(page);
        inv.open(player);
    }

    private void setupNavigationButtons(PaginatedFastInv inv) {
        if (!inv.isLastPage() && inv.lastPage() != 1) {
            inv.setItem(53,
                    plugin.utils.ib(Material.ARROW)
                            .name(plugin.getMessages().getString("gui.next-page.name"))
                            .lore(plugin.getMessages().getStringList("gui.next-page.lore"))
                            .build(),
                    event -> inv.openNext()
            );
        } else {
            inv.setItem(53, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());
        }

        if (!inv.isFirstPage()) {
            inv.setItem(45,
                    plugin.utils.ib(Material.ARROW)
                            .name(plugin.getMessages().getString("gui.previous-page.name"))
                            .lore(plugin.getMessages().getStringList("gui.previous-page.lore"))
                            .build(),
                    event -> inv.openPrevious()
            );
        } else {
            inv.setItem(45, plugin.utils.ib(Material.GRAY_STAINED_GLASS_PANE).hideTooltip().build());
        }
    }

    private ItemStack buildPreviewBanner() {
        return plugin.utils.ib(Material.WHITE_BANNER).pattern(plugin.manager.getBanner(player).getPatterns().toArray(Pattern[]::new)).build();
    }
}