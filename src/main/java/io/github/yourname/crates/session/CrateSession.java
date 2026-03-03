package io.github.yourname.crates.session;

import io.github.yourname.crates.CratePlugin;
import io.github.yourname.crates.crate.CrateType;
import io.github.yourname.crates.crate.LootItem;
import io.github.yourname.crates.util.ItemUtil;
import io.github.yourname.crates.util.TextUtil;
import io.github.yourname.crates.util.WeightedPicker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class CrateSession {
    private final CratePlugin plugin;
    private final Player player;
    private final CrateType type;
    private final PendingRewardManager pendingRewardManager;
    private final Runnable onFinish;

    private Inventory inv;
    private BukkitTask task;

    private int elapsedTicks = 0;
    private ItemStack selectedReward = null; // Track the reward for disconnect handling

    private final WeightedPicker<LootItem> picker = new WeightedPicker<>();

    public CrateSession(CratePlugin plugin, Player player, CrateType type, PendingRewardManager pendingRewardManager, Runnable onFinish) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
        this.pendingRewardManager = pendingRewardManager;
        this.onFinish = onFinish;
    }

    public Inventory inventory() {
        return inv;
    }

    public CrateType crateType() {
        return type;
    }

    public void start() {
        inv = Bukkit.createInventory(null, type.gui().size(), TextUtil.colorizeToComponent(type.gui().title()));
        decorate(inv);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

        int interval = Math.max(1, type.animation().interval());
        int total = Math.max(interval, type.animation().ticks());

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(total), 0L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void tick(int totalTicks) {
        if (!player.isOnline()) {
            // Player disconnected - save their reward for next login
            if (selectedReward == null) {
                // Select their reward now since animation was interrupted
                LootItem won = picker.pick(toEntries(type.loot()));
                selectedReward = won.item().clone();
            }
            pendingRewardManager.addPendingReward(player.getUniqueId(), selectedReward);
            plugin.getLogger().info("Saved pending reward for " + player.getName() + " due to disconnect");
            stop();
            onFinish.run();
            return;
        }
        if (player.getOpenInventory().getTopInventory() != inv) {
            // Prevent "escape" closing to avoid losing state.
            player.openInventory(inv);
        }

        elapsedTicks += Math.max(1, type.animation().interval());

        // Preview roll (purely visual)
        LootItem preview = randomLoot();
        inv.setItem(type.animation().finalSlot(), preview.item().clone());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.6f);

        if (elapsedTicks >= totalTicks) {
            finish();
        }
    }

    private LootItem randomLoot() {
        // Quick random: pick from list uniformly for preview.
        return type.loot().get((int) (Math.random() * type.loot().size()));
    }

    private void finish() {
        stop();

        // Select reward if not already selected (from disconnect)
        if (selectedReward == null) {
            LootItem won = picker.pick(toEntries(type.loot()));
            selectedReward = won.item().clone();
        }

        giveReward(selectedReward);

        inv.setItem(type.animation().finalSlot(), selectedReward);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == inv) {
                player.closeInventory();
            }
            onFinish.run();
        }, 20L);
    }

    private void giveReward(ItemStack reward) {
        var leftover = player.getInventory().addItem(reward);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }
        player.sendMessage(Component.text("You won: ").append(reward.displayName() != null ? reward.displayName() : Component.text(reward.getType().name())));
    }

    private List<WeightedPicker.Entry<LootItem>> toEntries(List<LootItem> loot) {
        List<WeightedPicker.Entry<LootItem>> entries = new ArrayList<>();
        for (LootItem li : loot) {
            entries.add(new WeightedPicker.Entry<>(li, li.weight()));
        }
        return entries;
    }

    private void decorate(Inventory inv) {
        ItemStack pane = ItemUtil.named(Material.GRAY_STAINED_GLASS_PANE, 1, Component.empty());
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            inv.setItem(i, pane);
        }
        // Make center empty initially
        inv.setItem(type.animation().finalSlot(), new ItemStack(Material.AIR));
    }
}
