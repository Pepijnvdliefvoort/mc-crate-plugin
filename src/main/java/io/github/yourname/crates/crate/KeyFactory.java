package io.github.yourname.crates.crate;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.util.ItemUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class KeyFactory {
    public static final String PDC_KEY = "crate_key_id";

    private final NamespacedKey keyId;

    public KeyFactory(CratePlugin plugin) {
        this.keyId = new NamespacedKey(plugin, PDC_KEY);
    }

    public ItemStack createKey(CrateType type, int amount) {
        ItemStack keyItem = ItemUtil.buildItem(
                type.key().material(),
                amount,
                type.key().name(),
                type.key().lore()
        );

        ItemMeta meta = keyItem.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(keyId, PersistentDataType.STRING, type.id());
            keyItem.setItemMeta(meta);
        }
        return keyItem;
    }

    public Optional<String> readKeyId(ItemStack item) {
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(keyId, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }
}
