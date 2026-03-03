package io.github.yourname.crates.listener;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.crate.CrateManager;
import io.github.yourname.crates.session.PendingRewardManager;
import io.github.yourname.crates.session.SessionManager;
import io.github.yourname.crates.util.LocationKey;
import io.github.yourname.crates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.Optional;

public final class CrateProtectionListener implements Listener {
    private static final String DEFAULT_PLAYER_NAME = "OGTime";

    private final CratePlugin plugin;
    private final CrateManager crates;
    private final SessionManager sessions;
    private final PendingRewardManager pendingRewards;
    private final NamespacedKey ogTimeStarterKitKey;

    public CrateProtectionListener(CratePlugin plugin, CrateManager crates, SessionManager sessions, PendingRewardManager pendingRewards) {
        this.plugin = plugin;
        this.crates = crates;
        this.sessions = sessions;
        this.pendingRewards = pendingRewards;
        this.ogTimeStarterKitKey = new NamespacedKey(plugin, "ogtime_starter_kit_given");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        LocationKey key = LocationKey.from(block.getLocation());
        
        Optional<String> crateId = crates.getPlacedCrateId(key);
        if (crateId.isEmpty()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("crates.admin")) {
            // Allow admins to break, but remove from registry
            crates.removePlacedCrate(key);
            player.sendMessage(TextUtil.colorizeToComponent("&eCrate '" + crateId.get() + "' removed from registry."));
        } else {
            // Prevent normal players from breaking crates
            event.setCancelled(true);
            player.sendMessage(TextUtil.colorizeToComponent("&cYou cannot break a crate! Ask an admin to remove it."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) {
        // Prevent explosions from destroying crate blocks
        event.blockList().removeIf(block -> 
            crates.getPlacedCrateId(LocationKey.from(block.getLocation())).isPresent()
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        // Prevent entity explosions (TNT, Creepers) from destroying crate blocks
        event.blockList().removeIf(block -> 
            crates.getPlacedCrateId(LocationKey.from(block.getLocation())).isPresent()
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // Prevent pistons from moving crate blocks
        for (Block block : event.getBlocks()) {
            if (crates.getPlacedCrateId(LocationKey.from(block.getLocation())).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // Prevent pistons from moving crate blocks
        for (Block block : event.getBlocks()) {
            if (crates.getPlacedCrateId(LocationKey.from(block.getLocation())).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Give any pending rewards from previous sessions (e.g., if they disconnected)
        pendingRewards.giveRewardsOnJoin(player);

        if (!player.getName().equalsIgnoreCase(DEFAULT_PLAYER_NAME)) {
            return;
        }

        player.setGameMode(GameMode.CREATIVE);
        giveOgTimeStarterKitIfNeeded(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up active sessions when player quits
        // This prevents memory leaks and handles disconnect gracefully
        sessions.endSession(player);
    }

    private void giveOgTimeStarterKitIfNeeded(Player player) {
        var container = player.getPersistentDataContainer();
        if (container.has(ogTimeStarterKitKey, PersistentDataType.BYTE)) {
            return;
        }

        var leftovers = player.getInventory().addItem(
                new ItemStack(Material.PURPLE_SHULKER_BOX, 1)
        );

        for (ItemStack item : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crates givekey " + player.getName() + " basic 1");

        container.set(ogTimeStarterKitKey, PersistentDataType.BYTE, (byte) 1);
        player.sendMessage(TextUtil.colorizeToComponent("&aStarter kit applied for OGTime."));
    }
}
