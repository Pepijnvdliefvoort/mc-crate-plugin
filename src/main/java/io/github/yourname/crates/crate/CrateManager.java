package io.github.yourname.crates.crate;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.util.ItemUtil;
import io.github.yourname.crates.util.LocationKey;
import io.github.yourname.crates.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class CrateManager {
    private final CratePlugin plugin;

    private final Map<String, CrateType> crateTypes = new HashMap<>();
    private final Map<LocationKey, String> placedCrates = new HashMap<>();

    private File placedFile;
    private YamlConfiguration placedConfig;

    private KeyFactory keyFactory;

    public CrateManager(CratePlugin plugin) {
        this.plugin = plugin;
        this.keyFactory = new KeyFactory(plugin);
    }

    public void reloadAll() {
        plugin.reloadConfig();
        loadCrateTypes();
        loadPlacedCrates();
    }

    public Collection<CrateType> allTypes() {
        return Collections.unmodifiableCollection(crateTypes.values());
    }

    public Optional<CrateType> getType(String id) {
        return Optional.ofNullable(crateTypes.get(id.toLowerCase(Locale.ROOT)));
    }

    public KeyFactory keyFactory() {
        return keyFactory;
    }

    public Optional<String> getPlacedCrateId(LocationKey key) {
        return Optional.ofNullable(placedCrates.get(key));
    }

    public void setPlacedCrate(LocationKey loc, String crateId) {
        placedCrates.put(loc, crateId.toLowerCase(Locale.ROOT));
        savePlacedCrates();
    }

    public boolean removePlacedCrate(LocationKey loc) {
        boolean removed = placedCrates.remove(loc) != null;
        if (removed) savePlacedCrates();
        return removed;
    }

    public void savePlacedCrates() {
        if (placedConfig == null || placedFile == null) return;

        placedConfig.set("placed", null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<LocationKey, String> e : placedCrates.entrySet()) {
            LocationKey lk = e.getKey();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("world", lk.world());
            row.put("x", lk.x());
            row.put("y", lk.y());
            row.put("z", lk.z());
            row.put("crate", e.getValue());
            list.add(row);
        }
        placedConfig.set("placed", list);

        try {
            placedConfig.save(placedFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save placed.yml: " + ex.getMessage());
        }
    }

    private void loadPlacedCrates() {
        placedFile = new File(plugin.getDataFolder(), "placed.yml");
        if (!placedFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                placedFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create placed.yml: " + e.getMessage());
            }
        }
        placedConfig = YamlConfiguration.loadConfiguration(placedFile);

        placedCrates.clear();
        List<?> list = placedConfig.getList("placed");
        if (list == null) return;

        for (Object o : list) {
            if (!(o instanceof Map<?, ?> map)) continue;
            String world = Objects.toString(map.get("world"), null);
            Integer x = asInt(map.get("x"));
            Integer y = asInt(map.get("y"));
            Integer z = asInt(map.get("z"));
            String crate = Objects.toString(map.get("crate"), null);

            if (world == null || x == null || y == null || z == null || crate == null) continue;
            placedCrates.put(new LocationKey(world, x, y, z), crate.toLowerCase(Locale.ROOT));
        }
    }

    private Integer asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void loadCrateTypes() {
        crateTypes.clear();

        ConfigurationSection crates = plugin.getConfig().getConfigurationSection("crates");
        if (crates == null) {
            plugin.getLogger().warning("No 'crates' section in config.yml");
            return;
        }

        for (String id : crates.getKeys(false)) {
            ConfigurationSection sec = crates.getConfigurationSection(id);
            if (sec == null) continue;

            Material block = Material.matchMaterial(sec.getString("block", "SHULKER_BOX"));
            if (block == null) block = Material.SHULKER_BOX;

            // gui
            ConfigurationSection guiSec = sec.getConfigurationSection("gui");
            String title = guiSec != null ? guiSec.getString("title", id) : id;
            int size = guiSec != null ? guiSec.getInt("size", 27) : 27;
            size = normalizeInventorySize(size);

            // animation
            ConfigurationSection animSec = sec.getConfigurationSection("animation");
            int ticks = animSec != null ? animSec.getInt("ticks", 60) : 60;
            int interval = animSec != null ? animSec.getInt("interval", 2) : 2;
            int finalSlot = animSec != null ? animSec.getInt("finalSlot", 13) : 13;
            if (finalSlot < 0 || finalSlot >= size) finalSlot = Math.min(13, size - 1);

            // key
            ConfigurationSection keySec = sec.getConfigurationSection("key");
            Material keyMat = keySec != null ? Material.matchMaterial(keySec.getString("material", "TRIPWIRE_HOOK")) : Material.TRIPWIRE_HOOK;
            if (keyMat == null) keyMat = Material.TRIPWIRE_HOOK;
            String keyName = keySec != null ? keySec.getString("name", id + " Key") : (id + " Key");
            List<String> keyLore = keySec != null ? keySec.getStringList("lore") : List.of();

            // loot (items only)
            List<LootItem> lootItems = new ArrayList<>();
            List<?> lootList = sec.getList("loot");
            if (lootList != null) {
                for (Object obj : lootList) {
                    if (!(obj instanceof Map<?, ?> map)) continue;
                    int weight = asInt(map.get("weight")) != null ? asInt(map.get("weight")) : 1;

                    Object itemObj = map.get("item");
                    if (!(itemObj instanceof Map<?, ?> itemMap)) continue;

                    Material mat = Material.matchMaterial(Objects.toString(itemMap.get("material"), "STONE"));
                    if (mat == null) mat = Material.STONE;
                    int amt = asInt(itemMap.get("amount")) != null ? asInt(itemMap.get("amount")) : 1;
                    String nameStr = itemMap.get("name") != null ? Objects.toString(itemMap.get("name")) : null;

                    @SuppressWarnings("unchecked")
                    List<String> lore = itemMap.get("lore") instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();

                    ItemStack stack = ItemUtil.buildItem(mat, amt, nameStr, lore);
                    lootItems.add(new LootItem(stack, weight));
                }
            }

            if (lootItems.isEmpty()) {
                plugin.getLogger().warning("Crate '" + id + "' has no loot entries. Skipping.");
                continue;
            }

            CrateType type = new CrateType(
                    id.toLowerCase(Locale.ROOT),
                    block,
                    new CrateType.GuiSettings(title, size),
                    new CrateType.AnimationSettings(ticks, interval, finalSlot),
                    new CrateType.KeySettings(keyMat, keyName, keyLore),
                    lootItems
            );

            crateTypes.put(type.id(), type);
        }
    }

    private int normalizeInventorySize(int size) {
        int s = Math.max(9, Math.min(54, size));
        return ((s + 8) / 9) * 9;
    }
}
