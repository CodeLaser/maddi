package org.e2immu.analyzer.modification.link.impl.graph;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Timer {
    private static class T implements Comparable<T> {
        final String label;
        long hits;
        long cumulative;
        Instant start;

        private T(String label) {
            this.label = label;
        }

        @Override
        public int compareTo(@NotNull Timer.T o) {
            return label.compareTo(o.label);
        }

        @Override
        public String toString() {
            return label + ": " + hits + ", " + String.format("%,d", cumulative / 1000000L) + " ms";
        }
    }

    private final Map<String, T> map = new HashMap<>();

    public void start(String label) {
        T t = map.computeIfAbsent(label, _ -> new T(label));
        assert t.start == null;
        t.start = Instant.now();
    }

    public void end(String label) {
        T t = map.get(label);
        assert t != null && t.start != null;
        Instant now = Instant.now();
        Duration d = t.start.until(now);
        long duration = d.toNanos();
        t.cumulative += duration;
        t.hits++;
        t.start = null;
    }

    @Override
    public String toString() {
        return map.values().stream().sorted().map(Object::toString).collect(Collectors.joining("; "));
    }
}
