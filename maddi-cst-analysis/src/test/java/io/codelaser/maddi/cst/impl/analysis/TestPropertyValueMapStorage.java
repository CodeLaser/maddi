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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.analysis.Value;

/** The packed-array storage of PropertyValueMapImpl behaves as a map, across the switch to a HashMap. */
public class TestPropertyValueMapStorage {

    private static final List<Property> PROPERTIES = new ArrayList<>();

    static {
        for (int i = 0; i < 14; i++) PROPERTIES.add(new PropertyImpl("p" + i));
    }

    private static Value bool(boolean b) {
        return ValueImpl.BoolImpl.from(b);
    }

    @Test
    public void emptyUntilWritten() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        assertTrue(map.isEmpty());
        assertNull(map.peek(PROPERTIES.get(0)));
        map.removeIf(p -> true);
        assertTrue(map.isEmpty());
        assertEquals(0, map.propertyValueStream().count());
    }

    @Test
    public void insertionOrderWhilePacked() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        for (int i = 7; i >= 0; i--) map.set(PROPERTIES.get(i), bool(i % 2 == 0));
        assertEquals(List.of("p7", "p6", "p5", "p4", "p3", "p2", "p1", "p0"),
                map.propertyValueStream().map(pv -> pv.property().key()).toList());
        map.removeIf(p -> Set.of("p6", "p3").contains(p.key()));
        assertEquals(List.of("p7", "p5", "p4", "p2", "p1", "p0"),
                map.propertyValueStream().map(pv -> pv.property().key()).toList());
        map.removeIf(p -> true);
        assertTrue(map.isEmpty());
    }

    @Test
    public void setRefusesOverwrite() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        map.set(PROPERTIES.get(0), bool(false));
        assertThrows(IllegalArgumentException.class, () -> map.set(PROPERTIES.get(0), bool(true)));
        assertTrue(map.setAllowControlledOverwrite(PROPERTIES.get(1), bool(false)));
        assertTrue(map.setAllowControlledOverwrite(PROPERTIES.get(1), bool(true)));
        assertFalse(map.setAllowControlledOverwrite(PROPERTIES.get(1), bool(true)));
        assertEquals(bool(true), map.peek(PROPERTIES.get(1)));
    }

    @Test
    public void randomOperationsMatchAMap() {
        Random random = new Random(20261120L);
        for (int round = 0; round < 500; round++) {
            PropertyValueMapImpl map = new PropertyValueMapImpl();
            Map<Property, Value> reference = new LinkedHashMap<>();
            for (int op = 0; op < 40; op++) {
                Property p = PROPERTIES.get(random.nextInt(PROPERTIES.size()));
                switch (random.nextInt(4)) {
                    case 0, 1 -> {
                        Value v = bool(random.nextBoolean());
                        boolean changed = map.overwrite(p, v);
                        Value prev = reference.put(p, v);
                        assertEquals(!v.equals(prev), changed);
                    }
                    case 2 -> {
                        int mod = 2 + random.nextInt(3);
                        map.removeIf(q -> q.key().hashCode() % mod == 0);
                        reference.keySet().removeIf(q -> q.key().hashCode() % mod == 0);
                    }
                    default -> assertEquals(reference.get(p), map.peek(p));
                }
                assertEquals(reference.isEmpty(), map.isEmpty());
                assertEquals(reference, toMap(map));
                assertEquals(reference.containsKey(p), map.haveAnalyzedValueFor(p));
            }
        }
    }

    private static Map<Property, Value> toMap(PropertyValueMap map) {
        Map<Property, Value> result = new LinkedHashMap<>();
        map.propertyValueStream().forEach(pv -> assertNull(result.put(pv.property(), pv.value())));
        return result;
    }
}
