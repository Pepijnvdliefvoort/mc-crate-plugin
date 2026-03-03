package io.github.yourname.crates.listener;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.crate.CrateManager;
import io.github.yourname.crates.crate.CrateType;
import io.github.yourname.crates.session.SessionManager;
import io.github.yourname.crates.util.LocationKey;
import io.github.yourname.crates.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class CrateInteractListener implements Listener {
    private final CratePlugin plugin;
    private final CrateManager crates;
    private final SessionManager sessions;

    public CrateInteractListener(CratePlugin plugin, CrateManager crates, SessionManager sessions) {
        this.plugin = plugin;
        this.crates = crates;
        this.sessions = sessions;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Optional<String> crateIdOpt = crates.getPlacedCrateId(LocationKey.from(clicked.getLocation()));
        if (crateIdOpt.isEmpty()) return;

        String crateId = crateIdOpt.get();
        CrateType type = crates.getType(crateId).orElse(null);
        if (type == null) return;

        // Prevent opening the block inventory (e.g., shulker)
        event.setCancelled(true);

        Player player = event.getPlayer();
        
        // Check session first to prevent race condition
        if (sessions.hasSession(player)) {
            player.sendMessage(Component.text("You're already opening a crate."));
            return;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        String keyId = crates.keyFactory().readKeyId(inHand).orElse(null);
        if (keyId == null || !keyId.equalsIgnoreCase(crateId)) {
            player.sendMessage(TextUtil.colorizeToComponent("&cThat key doesn't fit this crate."));
            return;
        }

        if (clicked.getType() != type.blockMaterial()) {
            player.sendMessage(TextUtil.colorizeToComponent("&cThis crate block is not the configured material for '" + crateId + "'."));
            return;
        }

        // Check inventory space before consuming key
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(TextUtil.colorizeToComponent("&cYour inventory is full! Make space before opening crates."));
            return;
        }

        // Consume key BEFORE creating session to prevent race condition
        if (!consumeOneKey(player)) {
            player.sendMessage(TextUtil.colorizeToComponent("&cFailed to consume key."));
            return;
        }
        
        // Now start session (key is already consumed, preventing duplication)
        sessions.startSession(player, type);
    }

    private boolean consumeOneKey(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player.getGameMode() == GameMode.CREATIVE) return true;

        if (item == null || item.getType().isAir()) return false;
        
        int amt = item.getAmount();
        if (amt <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amt - 1);
        }
        return true;
    }
}
