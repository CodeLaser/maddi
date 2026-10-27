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
package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The order in which compilation units are scanned must not change whether a field's type has a source position.
 * <p>
 * It did. When a unit referencing {@code Item} is scanned before {@code Item.java} itself, {@code Item} is
 * built from javac's symbol first, fields included; the later scan of {@code Item.java} then finds each field
 * already present, converts its declared type a second time from the tree, and records the type's source under
 * that second instance. {@code DetailedSources} is identity-keyed and callers look up {@code field.type()},
 * the first instance: the position was there and unreachable. Primitive types are runtime singletons and were
 * never affected, which is how {@code int} and {@code long} components of one class had a type position and
 * its {@code String} component did not.
 * <p>
 * Two tests rather than two scans in one: the harness's state is per instance (see
 * {@code TestOverridesUnitOrder}).
 */
public class TestFieldTypeDetailUnitOrder extends CommonTest {

    @Language("java")
    private static final String ITEM = """
            package a.b;
            public class Item {
                public int columnNr;
                public String value;
            }
            """;

    @Language("java")
    private static final String A_USER = """
            package a.b;
            public class AUser {
                public Item make(String s) {
                    Item item = new Item();
                    item.columnNr = 1;
                    item.value = s;
                    return item;
                }
            }
            """;

    private void assertTypePositions(String... order) {
        Map<String, String> ordered = new LinkedHashMap<>();
        Map<String, String> sources = Map.of("a.b.Item", ITEM, "a.b.AUser", A_USER);
        for (String fqn : order) ordered.put(fqn, sources.get(fqn));
        TypeInfo item = scan(false, ordered).primaryTypes().stream()
                .filter(t -> "a.b.Item".equals(t.fullyQualifiedName())).findFirst().orElseThrow();

        FieldInfo columnNr = item.getFieldByName("columnNr", true);
        DetailedSources columnNrDs = columnNr.source().detailedSources();
        assertEquals("3-12:3-14", columnNrDs.detail(columnNr.type()).compact2());

        FieldInfo value = item.getFieldByName("value", true);
        DetailedSources valueDs = value.source().detailedSources();
        Source typeSource = valueDs.detail(value.type());
        assertNotNull(typeSource, "no source position for the type of 'value', looked up as value.type()");
        assertEquals("4-12:4-17", typeSource.compact2());
    }

    @DisplayName("Item scanned first: the field's type is converted once")
    @Test
    public void itemFirst() {
        assertTypePositions("a.b.Item", "a.b.AUser");
    }

    @DisplayName("the user scanned first: Item is built from the symbol, then re-visited from its tree")
    @Test
    public void userFirst() {
        assertTypePositions("a.b.AUser", "a.b.Item");
    }
}
