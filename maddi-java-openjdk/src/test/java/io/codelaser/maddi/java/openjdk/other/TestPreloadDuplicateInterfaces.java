/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.java.openjdk.CommonTest;
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
