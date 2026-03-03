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
    private static final int REEL_SIZE = 7;
    private static final int MIN_STEP_DELAY_TICKS = 1;

    private final CratePlugin plugin;
    private final Player player;
    private final CrateType type;
    private final PendingRewardManager pendingRewardManager;
    private final Runnable onFinish;
    private final List<WeightedPicker.Entry<LootItem>> lootEntries;

    private Inventory inv;
    private BukkitTask task;
    private int[] reelSlots;
    private final List<ItemStack> reelItems = new ArrayList<>(REEL_SIZE);

    private int elapsedTicks = 0;
    private ItemStack selectedReward = null;

    private final WeightedPicker<LootItem> picker = new WeightedPicker<>();

    public CrateSession(CratePlugin plugin, Player player, CrateType type, PendingRewardManager pendingRewardManager, Runnable onFinish) {
        this.plugin = plugin;
        this.player = player;
        this.type = type;
        this.pendingRewardManager = pendingRewardManager;
        this.onFinish = onFinish;
        this.lootEntries = toEntries(type.loot());
    }

    public Inventory inventory() {
        return inv;
    }

    public CrateType crateType() {
        return type;
    }

    public void start() {
        inv = Bukkit.createInventory(null, type.gui().size(), TextUtil.colorizeToComponent(type.gui().title()));
        reelSlots = computeReelSlots(inv.getSize());
        fillInitialReel();
        decorate(inv);
        renderReel();

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

        int total = Math.max(20, type.animation().ticks());
        elapsedTicks = 0;
        scheduleNextTick(total);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    private void tick(int totalTicks) {
        if (!player.isOnline()) {
            if (selectedReward == null) selectedReward = currentCenterItem();
            pendingRewardManager.addPendingReward(player.getUniqueId(), selectedReward);
            plugin.getLogger().info("Saved pending reward for " + player.getName() + " due to disconnect");
            stop();
            onFinish.run();
            return;
        }

        if (player.getOpenInventory().getTopInventory() != inv) {
            player.openInventory(inv);
        }

        rotateReelLeft();
        renderReel();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.6f);

        elapsedTicks += computeStepDelay(totalTicks);

        if (elapsedTicks >= totalTicks) {
            finish();
            return;
        }

        scheduleNextTick(totalTicks);
    }

    private void scheduleNextTick(int totalTicks) {
        long delay = computeStepDelay(totalTicks);
        task = Bukkit.getScheduler().runTaskLater(plugin, () -> tick(totalTicks), delay);
    }

    private int computeStepDelay(int totalTicks) {
        int baseDelay = Math.max(MIN_STEP_DELAY_TICKS, type.animation().interval());
        int maxDelay = Math.max(baseDelay + 2, baseDelay * 4);
        double progress = Math.min(1.0d, (double) elapsedTicks / Math.max(1, totalTicks));
        double eased = progress * progress;
        double delay = baseDelay + ((maxDelay - baseDelay) * eased);
        return Math.max(MIN_STEP_DELAY_TICKS, (int) Math.round(delay));
    }

    private void rotateReelLeft() {
        if (reelItems.isEmpty()) {
            fillInitialReel();
        }
        reelItems.remove(0);
        reelItems.add(pickWeightedLoot().item().clone());
    }

    private void fillInitialReel() {
        reelItems.clear();
        for (int i = 0; i < REEL_SIZE; i++) {
            reelItems.add(pickWeightedLoot().item().clone());
        }
    }

    private void renderReel() {
        for (int i = 0; i < REEL_SIZE; i++) {
            inv.setItem(reelSlots[i], reelItems.get(i));
        }
        renderCenterIndicators();
    }

    private int[] computeReelSlots(int inventorySize) {
        int[] slots = new int[REEL_SIZE];
        int rows = Math.max(1, inventorySize / 9);
        int middleRow = (rows - 1) / 2;
        int rowStart = middleRow * 9;
        int startColumn = 1;
        for (int i = 0; i < REEL_SIZE; i++) {
            slots[i] = rowStart + startColumn + i;
        }
        return slots;
    }

    private LootItem pickWeightedLoot() {
        return picker.pick(lootEntries);
    }

    private ItemStack currentCenterItem() {
        if (reelItems.size() >= REEL_SIZE) {
            return reelItems.get(REEL_SIZE / 2).clone();
        }
        return pickWeightedLoot().item().clone();
    }

    private void finish() {
        stop();

        if (selectedReward == null) {
            selectedReward = currentCenterItem();
        }

        giveReward(selectedReward);

        inv.setItem(reelSlots[REEL_SIZE / 2], selectedReward);
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
        for (int slot : reelSlots) {
            inv.setItem(slot, new ItemStack(Material.AIR));
        }
        renderCenterIndicators();
    }

    private void renderCenterIndicators() {
        int centerSlot = reelSlots[REEL_SIZE / 2];
        ItemStack indicator = ItemUtil.named(Material.RED_STAINED_GLASS_PANE, 1, TextUtil.colorizeToComponent("&c▼ Prize ▼"));

        int top = centerSlot - 9;
        int bottom = centerSlot + 9;

        if (top >= 0) {
            inv.setItem(top, indicator.clone());
        }
        if (bottom < inv.getSize()) {
            inv.setItem(bottom, indicator.clone());
        }
    }
}
