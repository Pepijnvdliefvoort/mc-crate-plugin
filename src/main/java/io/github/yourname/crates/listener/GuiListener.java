package io.github.yourname.crates.listener;

import io.github.yourname.crates.session.CrateSession;
import io.github.yourname.crates.session.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GuiListener implements Listener {
    private final SessionManager sessions;

    public GuiListener(SessionManager sessions) {
        this.sessions = sessions;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        CrateSession session = sessions.getSession(player);
        if (session == null) return;

        if (event.getView().getTopInventory() == session.inventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        CrateSession session = sessions.getSession(player);
        if (session == null) return;

        if (event.getView().getTopInventory() == session.inventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Session itself will reopen inventory while running; after finish, session ends.
        // If they closed it after it finished (rare), clean up.
        CrateSession session = sessions.getSession(player);
        if (session == null) return;

        // If they somehow close, keep session alive; it will reopen on next tick.
    }
}
