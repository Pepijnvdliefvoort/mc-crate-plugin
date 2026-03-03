package io.github.yourname.crates.util;

import java.util.List;
import java.util.Random;

public final class WeightedPicker<T> {
    public record Entry<T>(T value, int weight) {}

    private final Random random = new Random();

    public T pick(List<Entry<T>> entries) {
        int total = 0;
        for (Entry<T> e : entries) total += Math.max(0, e.weight());
        if (total <= 0) throw new IllegalArgumentException("Total weight must be > 0");

        int roll = random.nextInt(total) + 1; // 1..total
        int running = 0;
        for (Entry<T> e : entries) {
            running += Math.max(0, e.weight());
            if (roll <= running) return e.value();
        }
        return entries.get(entries.size() - 1).value(); // fallback
    }
}
