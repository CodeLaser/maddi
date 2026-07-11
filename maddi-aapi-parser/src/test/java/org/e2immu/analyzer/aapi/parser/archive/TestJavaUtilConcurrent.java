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

package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT_HC;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaUtilConcurrent extends CommonTest {

    // Every type here is a @Container (none modifies a parameter's content).
    @Test
    public void testConcurrentContainers() {
        for (Class<?> c : new Class<?>[]{
                ConcurrentHashMap.class, ConcurrentMap.class, CompletableFuture.class, CompletionStage.class,
                Future.class, Executor.class, ExecutorService.class, ThreadFactory.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE),
                    () -> c.getSimpleName() + " should be a @Container");
        }
    }

    // Future/CompletableFuture/CompletionStage hold a result of hidden-content type -> @Independent(hc=true).
    @Test
    public void testFuturesIndependentHc() {
        for (Class<?> c : new Class<?>[]{Future.class, CompletableFuture.class, CompletionStage.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(INDEPENDENT_HC, typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT),
                    () -> c.getSimpleName() + " should be @Independent(hc=true)");
        }
    }

    // Query methods on the futures read state and must be non-modifying (isDone/get); the mutating
    // methods (cancel/complete) stay @Modified.
    @Test
    public void testFutureQueriesNonModifying() {
        for (Class<?> c : new Class<?>[]{Future.class, CompletableFuture.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertFalse(typeInfo.findUniqueMethod("isDone", 0).isModifying(),
                    () -> c.getSimpleName() + ".isDone() must be non-modifying");
            assertFalse(typeInfo.findUniqueMethod("get", 0).isModifying(),
                    () -> c.getSimpleName() + ".get() must be non-modifying");
        }
        TypeInfo cf = compiledTypesManager().get(CompletableFuture.class);
        assertFalse(cf.findUniqueMethod("join", 0).isModifying(), "CompletableFuture.join() must be non-modifying");
        assertTrue(cf.findUniqueMethod("complete", 1).isModifying(), "CompletableFuture.complete() modifies");

        // ConcurrentHashMap's own read methods (not inherited from Map) must also be non-modifying.
        TypeInfo chm = compiledTypesManager().get(ConcurrentHashMap.class);
        assertFalse(chm.findUniqueMethod("mappingCount", 0).isModifying(), "mappingCount() must be non-modifying");
    }

    // ConcurrentHashMap(Map) copies entries, so the source-map parameter is @Independent(hc=true).
    @Test
    public void testConcurrentHashMapCopyConstructor() {
        TypeInfo typeInfo = compiledTypesManager().get(ConcurrentHashMap.class);
        MethodInfo constructor = typeInfo.findConstructor(compiledTypesManager().get(Map.class));
        ParameterInfo p0 = constructor.parameters().getFirst();
        assertFalse(p0.isModified());
        assertSame(INDEPENDENT_HC, p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT));
    }
}
