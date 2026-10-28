/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CodeLaser/maddi-mod#76: records, for one computation on one thread, every analysis value it read and every
 * value it left behind, so that a later run of the same computation can be skipped when nothing it read has
 * changed. Hooked into {@link PropertyValueMapImpl}, and into any other {@link Owner} of state a computation reads
 * and writes outside the analysis maps; near-zero cost when no recording is active on the thread.
 *
 * <p>A read is recorded with the value it saw the first time, unless the computation itself wrote that slot
 * earlier (then the read sees its own output, not an input). A written slot is recorded with its final value at
 * {@link #stop(Recording)}. The recording is reusable ({@link Recording#firstChange(Object)} is null) when every read slot holds an equal
 * value and every written slot still holds what the computation left there: a deterministic computation then
 * reads the same inputs and writes the same values, so skipping it changes nothing.
 *
 * <p>A slot that was first read and then written is an input that the computation's own output replaces; it is
 * only reusable when the two are equal (the computation reproduced its input). Reading a whole map
 * ({@link PropertyValueMapImpl#propertyValueStream()}) makes the recording unusable.
 */
public final class AnalysisAccessRecorder {
    // volatile not needed: armed once, before the analysis threads start
    private static boolean ENABLED;

    private static final ThreadLocal<Recording> CURRENT = new ThreadLocal<>();

    private AnalysisAccessRecorder() {
    }

    public static void arm() {
        ENABLED = true;
    }

    public static boolean isArmed() {
        return ENABLED;
    }

    /** marks the absence of a value in a recorded slot */
    private static final Object ABSENT = new Object() {
        @Override
        public String toString() {
            return "<absent>";
        }
    };

    /**
     * Holds recordable state: one value per key, which the recorder can look at again later without recording that
     * access.
     */
    public interface Owner {
        Object peek(Object key);

        /*
        equals() alone is not enough: some values compare by key only (methodLinks), and a computation reading an
        equal value with other content may compute something else (Value.sameContent)
         */
        default boolean sameValue(Object key, Object a, Object b) {
            return a.equals(b) && (!(a instanceof Value va) || va.sameContent((Value) b));
        }
    }

    private static boolean same(Slot slot, Object a, Object b) {
        if (a == b) return true;
        if (a == ABSENT || b == ABSENT) return false;
        return slot.owner.sameValue(slot.key, a, b);
    }

    private static String keyName(Object key) {
        return key instanceof Property p ? p.key() : String.valueOf(key);
    }

    private record Slot(Owner owner, Object key) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Slot s && s.owner == owner && s.key == key;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(owner) * 31 + System.identityHashCode(key);
        }

        Object peek() {
            Object now = owner.peek(key);
            return now == null ? ABSENT : now;
        }
    }

    public static final class Recording {
        private final Recording enclosing;
        // in order of first access: LINKCACHECHECK reports the first read where two computations diverge
        private final Map<Slot, Object> reads = new LinkedHashMap<>();
        private final Map<Slot, Object> writes = new HashMap<>();
        private String unusable;
        private Object context;
        // filled by stop()
        private Slot[] slots;
        private Object[] values;
        private boolean[] written;

        private Recording(Recording enclosing) {
            this.enclosing = enclosing;
        }

        /** process-wide state the computation depends on outside the analysis maps; compared with equals() */
        public void setContext(Object context) {
            this.context = context;
        }

        /** null when the recording can be reused; otherwise why not */
        public String unusable() {
            return unusable;
        }

        public int size() {
            return slots == null ? 0 : slots.length;
        }

        /**
         * @return null when the context is equal and every recorded slot holds the value of the recording;
         * otherwise the key of the first property that differs
         */
        public String firstChange(Object context) {
            if (unusable != null) return unusable;
            if (!Objects.equals(this.context, context)) return "context";
            for (int i = 0; i < slots.length; i++) {
                if (!same(slots[i], slots[i].peek(), values[i])) return keyName(slots[i].key);
            }
            return null;
        }

        /**
         * LINKCACHECHECK, on the recording of a hit ({@code this}) and that of the computation it replaced: null
         * when skipping the computation leaves the same state, i.e. every slot the computation wrote already held
         * that value in the hit's recording; otherwise the first difference. A computation that reads differently
         * but writes the same is skipped safely. Slots of the {@code ignore} properties are left out.
         */
        public String differenceFrom(Recording computation, Set<String> ignore) {
            if (unusable != null || computation.unusable != null) {
                return "unusable: " + unusable + " / " + computation.unusable + "; " + (unusable == null && slots != null
                        && computation.slots != null ? firstDivergence(computation) : "");
            }
            Map<Slot, Object> mine = new HashMap<>();
            for (int i = 0; i < slots.length; i++) mine.put(slots[i], values[i]);
            for (int i = 0; i < computation.slots.length; i++) {
                Slot slot = computation.slots[i];
                if (!computation.written[i] || ignore.contains(keyName(slot.key))) continue;
                if (!mine.containsKey(slot)) return "new output " + keyName(slot.key) + " = " + computation.values[i];
                Object v = mine.get(slot);
                if (!same(slot, v, computation.values[i])) {
                    return keyName(slot.key) + ": " + v + " -> " + computation.values[i] + "; " + firstDivergence(computation);
                }
            }
            return null;
        }

        private String firstDivergence(Recording computation) {
            Map<Slot, Object> mine = new HashMap<>();
            for (int i = 0; i < slots.length; i++) {
                if (!written[i]) mine.put(slots[i], values[i]);
            }
            for (int i = 0; i < computation.slots.length; i++) {
                Slot slot = computation.slots[i];
                if (computation.written[i]) continue;
                String where = keyName(slot.key) + " of a " + slot.owner.getClass().getSimpleName() + " (read #" + i + ")";
                if (!mine.containsKey(slot)) return "first new read: " + where + " = " + abbreviate(computation.values[i]);
                if (!same(slot, mine.get(slot), computation.values[i])) {
                    return "first changed read: " + where + ": " + abbreviate(mine.get(slot)) + " -> "
                           + abbreviate(computation.values[i]);
                }
            }
            return "same reads (" + computation.slots.length + " slots)";
        }

        private static String abbreviate(Object o) {
            String s = String.valueOf(o);
            return s.length() > 200 ? s.substring(0, 200) + "..." : s;
        }

        private void read(Slot slot, Object value) {
            if (!writes.containsKey(slot)) reads.putIfAbsent(slot, value == null ? ABSENT : value);
        }

        private void freeze() {
            List<Slot> list = new ArrayList<>(reads.size() + writes.size());
            List<Object> vals = new ArrayList<>(reads.size() + writes.size());
            List<Boolean> w = new ArrayList<>(reads.size() + writes.size());
            reads.forEach((slot, then) -> {
                boolean alsoWritten = writes.containsKey(slot);
                if (alsoWritten && !same(slot, slot.peek(), then) && unusable == null) {
                    unusable = "reproduced:" + keyName(slot.key);
                }
                list.add(slot);
                vals.add(then);
                w.add(alsoWritten);
            });
            writes.keySet().forEach(slot -> {
                if (!reads.containsKey(slot)) {
                    list.add(slot);
                    vals.add(slot.peek());
                    w.add(true);
                }
            });
            slots = list.toArray(new Slot[0]);
            values = vals.toArray();
            written = new boolean[w.size()];
            for (int i = 0; i < written.length; i++) written[i] = w.get(i);
            reads.clear();
            writes.clear();
        }
    }

    /**
     * Starts a recording on this thread. Recordings nest: a nested computation's accesses belong to it and to
     * every enclosing recording, since the enclosing computation's result depends on them as well.
     */
    public static Recording start() {
        Recording r = new Recording(CURRENT.get());
        CURRENT.set(r);
        return r;
    }

    /** ends the innermost recording on this thread, which must be {@code recording} */
    public static void stop(Recording recording) {
        Recording current = CURRENT.get();
        assert current == recording : "recordings must nest";
        CURRENT.set(recording.enclosing);
        recording.freeze();
    }

    /** ends a recording without keeping it, e.g. after an exception */
    public static void abandon(Recording recording) {
        CURRENT.set(recording.enclosing);
        recording.reads.clear();
        recording.writes.clear();
        recording.unusable = "abandoned";
    }

    /** {@code key} is compared by identity: a constant, such as a {@link Property} */
    public static void read(Owner owner, Object key, Object value) {
        Recording r = CURRENT.get();
        if (r == null) return;
        Slot slot = new Slot(owner, key);
        for (; r != null; r = r.enclosing) r.read(slot, value);
    }

    public static void write(Owner owner, Object key) {
        Recording r = CURRENT.get();
        if (r == null) return;
        Slot slot = new Slot(owner, key);
        for (; r != null; r = r.enclosing) r.writes.putIfAbsent(slot, Boolean.TRUE);
    }

    static void readAll() {
        for (Recording r = CURRENT.get(); r != null; r = r.enclosing) {
            if (r.unusable == null) r.unusable = "readAll";
        }
    }

    public static boolean active() {
        return ENABLED && CURRENT.get() != null;
    }
}
