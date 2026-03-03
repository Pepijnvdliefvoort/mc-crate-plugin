package io.github.yourname.crates.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ItemUtil {
    private ItemUtil() {}

    public static ItemStack buildItem(Material mat, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isBlank()) {
                meta.displayName(TextUtil.colorizeToComponent(name));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(TextUtil.colorizeLore(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack named(Material mat, int amount, Component name) {
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
