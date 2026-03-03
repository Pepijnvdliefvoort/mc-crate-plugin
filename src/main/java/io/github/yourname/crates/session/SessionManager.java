package io.github.yourname.crates.session;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.crate.CrateType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private final CratePlugin plugin;
    private final PendingRewardManager pendingRewardManager;
    private final Map<UUID, CrateSession> sessions = new ConcurrentHashMap<>();

    public SessionManager(CratePlugin plugin, PendingRewardManager pendingRewardManager) {
        this.plugin = plugin;
        this.pendingRewardManager = pendingRewardManager;
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void startSession(Player player, CrateType type) {
        endSession(player);
        CrateSession session = new CrateSession(plugin, player, type, pendingRewardManager, () -> sessions.remove(player.getUniqueId()));
        sessions.put(player.getUniqueId(), session);
        session.start();
    }

    public CrateSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void endSession(Player player) {
        CrateSession session = sessions.remove(player.getUniqueId());
        if (session != null) session.stop();
    }

    public void shutdown() {
        for (CrateSession s : sessions.values()) {
            s.stop();
        }
        sessions.clear();
    }
}
