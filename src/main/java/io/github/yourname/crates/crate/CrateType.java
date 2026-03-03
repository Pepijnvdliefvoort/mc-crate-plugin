package io.github.yourname.crates.crate;

import org.bukkit.Material;

import java.util.List;

public record CrateType(
        String id,
        Material blockMaterial,
        GuiSettings gui,
        AnimationSettings animation,
        KeySettings key,
        List<LootItem> loot
) {
    public record GuiSettings(String title, int size) {}
    public record AnimationSettings(int ticks, int interval, int finalSlot) {}
    public record KeySettings(Material material, String name, List<String> lore) {}
}
