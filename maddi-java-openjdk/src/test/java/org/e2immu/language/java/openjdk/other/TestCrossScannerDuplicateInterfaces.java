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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression (originally surfaced by {@code TestCloneBench}): the class scanner's per-type "setup block" (parent,
 * type parameters, interfaces, annotations) must run exactly once per {@link TypeInfo}, ever. It used to be guarded by
 * an {@code IdentityHashMap} field on the {@code ClassSymbolScanner} instance, but a fresh scanner is built for every
 * {@code parse()}, while {@code TypeInfo} instances (and their still-open builders) are shared through the
 * {@code InfoByFqn} registry. A type left LAZILY-loaded-but-uncommitted by one scanner therefore had its whole setup
 * re-run by the next scanner's on-demand COMPLETE load; since {@code addInterfaceImplemented} appends, its interface
 * list was duplicated, tripping {@code "Extending multiple identical interfaces"} in {@code TypeInspectionImpl.commit()}.
 * The fix moves the guard onto the shared {@code InfoByFqn} ({@code markClassScannerSetupDone}).
 * <p>
 * This test reproduces the exact cross-scanner path hermetically: scan #1 commits {@code java.util.LinkedList}, which
 * references its interface {@code java.util.Deque} only as a type (LAZILY loaded, its interfaces added, not committed).
 * scan #2 builds a fresh scanner sharing the same {@code InfoByFqn}; {@code commitType} then forces that fresh scanner
 * to COMPLETE-load {@code Deque} -- the same on-demand load {@code CompiledTypesManager.getOrLoad} performs in
 * production. Before the fix this second load re-ran the setup and duplicated {@code Deque}'s interfaces (throwing in
 * commit under {@code -ea}); after it, the setup runs once across the two scanners.
 */
public class TestCrossScannerDuplicateInterfaces extends CommonTest {

    @Test
    public void test() {
        // scan #1: committing LinkedList pulls in its interface Deque as a mere type reference -> LAZILY, uncommitted
        scan("a.b.X", "package a.b; import java.util.LinkedList; class X { LinkedList<String> f = new LinkedList<>(); }");

        // scan #2: a *fresh* ClassSymbolScanner, sharing the same InfoByFqn (and thus the LAZILY Deque from scan #1)
        scan("a.b.Y", "package a.b; class Y { }");

        TypeInfo deque = classSymbolScanner.getType("java.util.Deque");
        assertNotNull(deque);
        assertTrue(deque != null && !deque.hasBeenInspected(), "Deque should still be LAZILY loaded, not committed");

        // force the fresh scanner to COMPLETE-load Deque -- exactly what getOrLoad's lazy loader does on demand.
        // Pre-fix, this re-ran Deque's setup on a fresh per-scanner guard and appended its interfaces a second time.
        classSymbolScanner.commitType(deque);

        List<ParameterizedType> interfaces = deque.interfacesImplemented();
        long distinct = interfaces.stream().map(p -> p.typeInfo().fullyQualifiedName()).distinct().count();
        assertEquals(distinct, interfaces.size(), "interfaces must not be duplicated across scanners: " + interfaces);
        // java.util.Deque extends Queue (and, on JDK 21+, SequencedCollection); assert on the stable one, not the count
        assertTrue(interfaces.stream().anyMatch(p -> "java.util.Queue".equals(p.typeInfo().fullyQualifiedName())),
                "expected java.util.Queue among Deque's interfaces: " + interfaces);
    }
}
