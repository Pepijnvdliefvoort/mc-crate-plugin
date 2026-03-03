package io.github.yourname.crates.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {}

    public static Component colorizeToComponent(String input) {
        if (input == null) return Component.empty();
        // Accept &-color codes and convert to Component
        return LEGACY.deserialize(input);
    }

    public static String colorizeToLegacyString(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<Component> colorizeLore(List<String> lore) {
        if (lore == null) return List.of();
        return lore.stream().map(TextUtil::colorizeToComponent).collect(Collectors.toList());
    }
}
