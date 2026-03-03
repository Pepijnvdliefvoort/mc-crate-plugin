package io.github.yourname.crates;

import io.github.yourname.crates.command.CratesCommand;
import io.github.yourname.crates.crate.CrateManager;
import io.github.yourname.crates.listener.CrateInteractListener;
import io.github.yourname.crates.listener.CrateProtectionListener;
import io.github.yourname.crates.listener.GuiListener;
import io.github.yourname.crates.session.PendingRewardManager;
import io.github.yourname.crates.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CratePlugin extends JavaPlugin {

    private CrateManager crateManager;
    private SessionManager sessionManager;
    private PendingRewardManager pendingRewardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pendingRewardManager = new PendingRewardManager(this);
        this.sessionManager = new SessionManager(this, pendingRewardManager);
        this.crateManager = new CrateManager(this);

        crateManager.reloadAll();

        getCommand("crates").setExecutor(new CratesCommand(this, crateManager));
        getCommand("crates").setTabCompleter(new CratesCommand(this, crateManager));

        Bukkit.getPluginManager().registerEvents(new CrateInteractListener(this, crateManager, sessionManager), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(sessionManager), this);
        Bukkit.getPluginManager().registerEvents(new CrateProtectionListener(crateManager, sessionManager, pendingRewardManager), this);
    }

    @Override
    public void onDisable() {
        if (crateManager != null) {
            crateManager.savePlacedCrates();
        }
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
    }

    public CrateManager crateManager() {
        return crateManager;
    }

    public SessionManager sessionManager() {
        return sessionManager;
    }

    public PendingRewardManager pendingRewardManager() {
        return pendingRewardManager;
    }
}
