package io.github.yourname.crates.session;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PendingRewardManager {
    private final CratePlugin plugin;
    private final Map<UUID, List<ItemStack>> pendingRewards = new HashMap<>();
    private File rewardsFile;
    private YamlConfiguration rewardsConfig;

    public PendingRewardManager(CratePlugin plugin) {
        this.plugin = plugin;
        loadPendingRewards();
    }

    public void addPendingReward(UUID playerId, ItemStack reward) {
        pendingRewards.computeIfAbsent(playerId, k -> new ArrayList<>()).add(reward);
        savePendingRewards();
    }

    public void giveRewardsOnJoin(Player player) {
        UUID playerId = player.getUniqueId();
        List<ItemStack> rewards = pendingRewards.remove(playerId);
        
        if (rewards == null || rewards.isEmpty()) return;

        player.sendMessage(TextUtil.colorizeToComponent("&aYou have &e" + rewards.size() + " &apending crate reward(s)!"));
        
        for (ItemStack reward : rewards) {
            var leftover = player.getInventory().addItem(reward);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item)
                );
            }
            player.sendMessage(Component.text("Received: ").append(
                reward.displayName() != null ? reward.displayName() : Component.text(reward.getType().name())
            ));
        }
        
        savePendingRewards();
    }

    public boolean hasPendingRewards(UUID playerId) {
        return pendingRewards.containsKey(playerId);
    }

    private void loadPendingRewards() {
        rewardsFile = new File(plugin.getDataFolder(), "pending_rewards.yml");
        if (!rewardsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                rewardsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create pending_rewards.yml: " + e.getMessage());
            }
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        pendingRewards.clear();
        for (String uuidStr : rewardsConfig.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) rewardsConfig.getList(uuidStr);
                if (items != null && !items.isEmpty()) {
                    pendingRewards.put(playerId, new ArrayList<>(items));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in pending_rewards.yml: " + uuidStr);
            }
        }
    }

    private void savePendingRewards() {
        if (rewardsConfig == null || rewardsFile == null) return;

        // Clear existing data
        for (String key : rewardsConfig.getKeys(false)) {
            rewardsConfig.set(key, null);
        }

        // Save pending rewards
        for (Map.Entry<UUID, List<ItemStack>> entry : pendingRewards.entrySet()) {
            rewardsConfig.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            rewardsConfig.save(rewardsFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save pending_rewards.yml: " + ex.getMessage());
        }
    }
}
