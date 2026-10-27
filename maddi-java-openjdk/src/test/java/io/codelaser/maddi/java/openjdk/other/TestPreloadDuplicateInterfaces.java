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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression: a <em>preloaded</em> type was loaded LAZILY (interfaces added, but not committed) and then again with
 * LOAD_MEMBERS (interfaces added a second time), because {@code ClassSymbolScanner.loadType} re-runs the parent/
 * interfaces block whenever {@code loadMode != COMPLETE} and {@code hasBeenInspected()} is still false. Since
 * {@code addInterfaceImplemented} appends, the type's {@code interfacesImplemented()} ended up duplicated
 * (e.g. {@code java.util.LinkedList} reported {@code [List, Deque, Cloneable, Serializable]} twice).
 */
public class TestPreloadDuplicateInterfaces extends CommonTest {

    public TestPreloadDuplicateInterfaces() {
        super(List.of("java.base::java.util")); // preload triggers the LAZILY-then-LOAD_MEMBERS path
    }

    @Test
    public void test() {
        scan("a.b.X", "package a.b; class X { }");
        TypeInfo linkedList = classSymbolScanner.getType("java.util.LinkedList");
        assertNotNull(linkedList);
        List<ParameterizedType> interfaces = linkedList.interfacesImplemented();
        long distinct = interfaces.stream().map(p -> p.typeInfo().fullyQualifiedName()).distinct().count();
        assertEquals(distinct, interfaces.size(), "interfaces must not be duplicated: " + interfaces);
        // java.util.LinkedList implements List, Deque, Cloneable, Serializable -- four, each exactly once
        assertEquals(4, interfaces.size());
    }
}
