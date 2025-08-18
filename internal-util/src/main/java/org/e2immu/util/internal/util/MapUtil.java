package org.e2immu.util.internal.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

// keep the 'final' here, there is a test that checks that MapUtil is not extensible
public final class MapUtil {

    public static <T extends Comparable<? super T>, D extends Comparable<? super D>> int compareMaps(Map<T, D> map1, Map<T, D> map2) {
        int c = map1.size() - map2.size();
        if (c != 0) return c;
        // same size
        int differentValue = 0;
        for (Map.Entry<T, D> e : map1.entrySet()) {
            D dv = map2.get(e.getKey());
            if (dv != null && differentValue == 0) {
                // are there different values?
                differentValue = e.getValue().compareTo(dv);
            }
            if (dv == null) {
                // different keys
                return compareKeys(map1.keySet(), map2.keySet());
            }
        }
        return differentValue;
    }

    private static <T extends Comparable<? super T>> int compareKeys(Set<T> set1, Set<T> set2) {
        TreeSet<T> treeSet1 = new TreeSet<>(set1);
        TreeSet<T> treeSet2 = new TreeSet<>(set2);
        Iterator<T> it1 = treeSet1.iterator();
        Iterator<T> it2 = treeSet2.iterator();
        while (it1.hasNext()) {
            assert it2.hasNext();
            int d = it1.next().compareTo(it2.next());
            if (d != 0) return d;
        }
        return 0;
    }

    public static <K, V> String nice(Map<K, V> map) {
        return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).sorted().collect(Collectors.joining(", "));
    }
}
