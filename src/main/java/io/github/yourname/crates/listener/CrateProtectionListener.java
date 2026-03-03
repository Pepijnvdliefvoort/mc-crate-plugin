package io.github.yourname.crates.listener;

import io.github.yourname.crates.crate.CrateManager;
import io.github.yourname.crates.session.PendingRewardManager;
import io.github.yourname.crates.session.SessionManager;
import io.github.yourname.crates.util.LocationKey;
import io.github.yourname.crates.util.TextUtil;
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

import java.util.Iterator;
import java.util.Optional;

public final class CrateProtectionListener implements Listener {
    private final CrateManager crates;
    private final SessionManager sessions;
    private final PendingRewardManager pendingRewards;

    public CrateProtectionListener(CrateManager crates, SessionManager sessions, PendingRewardManager pendingRewards) {
        this.crates = crates;
        this.sessions = sessions;
        this.pendingRewards = pendingRewards;
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
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up active sessions when player quits
        // This prevents memory leaks and handles disconnect gracefully
        sessions.endSession(player);
    }
}
