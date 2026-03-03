package io.github.yourname.crates.command;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.crate.CrateManager;
import io.github.yourname.crates.crate.CrateType;
import io.github.yourname.crates.util.LocationKey;
import io.github.yourname.crates.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CratesCommand implements CommandExecutor, TabCompleter {
    private final CratePlugin plugin;
    private final CrateManager crates;

    public CratesCommand(CratePlugin plugin, CrateManager crates) {
        this.plugin = plugin;
        this.crates = crates;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /crates <reload|set|remove|givekey>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                crates.reloadAll();
                sender.sendMessage(Component.text("Reloaded config + placed crates."));
                return true;
            }
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Players only."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /crates set <crateId>"));
                    return true;
                }

                String crateId = args[1].toLowerCase(Locale.ROOT);
                CrateType type = crates.getType(crateId).orElse(null);
                if (type == null) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cUnknown crate id."));
                    return true;
                }

                Block target = getTargetBlock(player, 6);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cLook at a block within 6 blocks."));
                    return true;
                }

                if (target.getType() != type.blockMaterial()) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cThat block must be: &f" + type.blockMaterial().name()));
                    return true;
                }

                crates.setPlacedCrate(LocationKey.from(target.getLocation()), crateId);
                sender.sendMessage(TextUtil.colorizeToComponent("&aSet crate '" + crateId + "' at this block."));
                return true;
            }
            case "remove" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Players only."));
                    return true;
                }

                Block target = getTargetBlock(player, 6);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cLook at a block within 6 blocks."));
                    return true;
                }

                boolean removed = crates.removePlacedCrate(LocationKey.from(target.getLocation()));
                sender.sendMessage(removed
                        ? TextUtil.colorizeToComponent("&aRemoved crate at this block.")
                        : TextUtil.colorizeToComponent("&cThat block isn't a placed crate."));
                return true;
            }
            case "givekey" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /crates givekey <player> <crateId> [amount]"));
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cPlayer not found (must be online)."));
                    return true;
                }

                String crateId = args[2].toLowerCase(Locale.ROOT);
                CrateType type = crates.getType(crateId).orElse(null);
                if (type == null) {
                    sender.sendMessage(TextUtil.colorizeToComponent("&cUnknown crate id."));
                    return true;
                }

                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
                }

                ItemStack key = crates.keyFactory().createKey(type, amount);
                var leftover = target.getInventory().addItem(key);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(item ->
                            target.getWorld().dropItemNaturally(target.getLocation(), item)
                    );
                }

                sender.sendMessage(TextUtil.colorizeToComponent("&aGave &f" + amount + " &akey(s) for crate &f" + crateId + "&a to &f" + target.getName()));
                return true;
            }
        }

        sender.sendMessage(Component.text("Unknown subcommand."));
        return true;
    }

    private Block getTargetBlock(Player player, double maxDist) {
        RayTraceResult res = player.rayTraceBlocks(maxDist);
        return res != null ? res.getHitBlock() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "set", "remove", "givekey").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return crates.allTypes().stream().map(CrateType::id)
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givekey")) {
            return crates.allTypes().stream().map(CrateType::id)
                    .filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return new ArrayList<>();
    }
}
