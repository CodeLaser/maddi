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

package io.codelaser.maddi.modification.prepwork.callgraph;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.modification.prepwork.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code new X[n]} is an array creation, not a call to a constructor of {@code X}.
 * <p>
 * The runtime represents it with a constructor it invents on the spot -- {@code Factory.newArrayCreationConstructor},
 * owned by the ELEMENT type, one {@code int} parameter per dimension -- which is not among
 * {@code X.constructors()}. Written into the call graph, that object collides with a declared constructor of the
 * same shape: {@code X(int)} and the invented one print the same fully qualified name
 * {@code a.b.X.&lt;init&gt;(int)}, while being unequal objects with the same hashCode.
 * <p>
 * Measured downstream on closed-core (23k types): {@code GraphService.classLevelDepths} collects the walked
 * methods into a map keyed on that printed name and died with
 * {@code IllegalStateException: Duplicate key com.example.parameter.util.LinkMap.&lt;init&gt;(int)} --
 * one entry standing for {@code new LinkMap[linkInfos.length]}, the other for the declared
 * {@code LinkMap(int initialCapacity)}. The consumer cannot tell them apart, so the graph must not offer
 * the invented one; {@code IsolationCore} already refuses it, for the same reason.
 * <p>
 * Nothing is lost by leaving it out: the dependency on the element type is recorded separately, by
 * {@code addType(info, cc.parameterizedType(), REFERENCES)}.
 */
public class TestCallGraphArrayCreation extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                static class E {
                    final int n;
                    E(int n) {
                        this.n = n;
                    }
                }
                static E[] make(int size) {
                    return new E[size];
                }
                static E one() {
                    return new E(3);
                }
            }
            """;

    private static List<MethodInfo> calledMethods(G<Info> graph, Info from) {
        Map<V<Info>, Long> edges = graph.edges(new V<>(from));
        if (edges == null) return List.of();
        return edges.entrySet().stream()
                .filter(e -> ComputeCallGraph.isReference(e.getValue()))
                .map(Map.Entry::getKey)
                .map(V::t)
                .filter(i -> i instanceof MethodInfo)
                .map(i -> (MethodInfo) i)
                .toList();
    }

    @DisplayName("new E[size] contributes no call edge to a constructor of E")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(ABX, INPUT1);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();

        TypeInfo E = X.findSubType("E");
        MethodInfo declaredConstructor = E.constructors().stream()
                .filter(c -> c.parameters().size() == 1)
                .findFirst().orElseThrow();
        MethodInfo make = X.findUniqueMethod("make", 1);
        MethodInfo one = X.findUniqueMethod("one", 0);

        // the control: a real 'new E(3)' IS a call, and it is the declared constructor
        List<MethodInfo> calledByOne = calledMethods(graph, one);
        assertEquals(List.of(declaredConstructor), calledByOne,
                "new E(3) must call the constructor E declares");

        // the defect: 'new E[size]' hands the graph a constructor E does not declare
        List<MethodInfo> calledByMake = calledMethods(graph, make);
        List<MethodInfo> synthetic = calledByMake.stream()
                .filter(MethodInfo::isSyntheticArrayConstructor).toList();
        assertTrue(synthetic.isEmpty(),
                "the array creation put " + synthetic + " into the call graph; it stands for 'new E[size]',"
                + " it is not among E.constructors(), and it prints exactly like the declared E(int)");
        assertFalse(calledByMake.contains(declaredConstructor),
                "and it must not be attributed to the declared constructor either");

        // the element type dependency survives: that is what the array creation actually means
        Map<V<Info>, Long> fromMake = graph.edges(new V<>(make));
        assertNotNull(fromMake, "make has no outgoing edges at all");
        assertTrue(fromMake.containsKey(new V<>((Info) E)),
                "the dependency of make(int) on E must still be recorded");
    }
}
