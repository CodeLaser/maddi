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

package org.e2immu.util.internal.graph.analyser;

import org.e2immu.util.internal.graph.analyser.Main;
import org.e2immu.util.internal.graph.analyser.TypeGraphIO;
import org.e2immu.util.internal.graph.op.BreakCycles;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeDependencies {

    // cp analyser/build/e2immuGraph/typeDependencies.gml analyser/build/e2immuGraph/packageDependenciesBasedOnTypeGraph.gml  graph/src/test/resources/org/e2immu/graph

    @Test
    public void test() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                                                                               + "org/e2immu/graph/typeDependencies.gml", Main.SEQUENTIAL});
        assertEquals(31, lin.maxCycleSize());
        assertEquals(101, lin.actionLog().size());
        assertEquals(100, lin.list().size());
    }

    @Test
    public void testParallel() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "org/e2immu/graph/typeDependencies.gml", Main.PARALLEL});
        assertEquals(31, lin.maxCycleSize());
        assertEquals(101, lin.actionLog().size());
        assertEquals(100, lin.list().size());
    }

    @Test
    public void testVertexWeight() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "org/e2immu/graph/typeDependencies.gml", Main.VERTEX_WEIGHT});
        assertEquals(1, lin.maxCycleSize());
        assertEquals(143, lin.actionLog().size());
        assertEquals(168, lin.list().size()); // must be <= 1042, the number of nodes
    }

    @Test
    public void test2() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "org/e2immu/graph/packageDependenciesBasedOnTypeGraph.gml"});
        assertEquals(9, lin.list().size());
        assertEquals(5, lin.actionLog().size());
        assertEquals(36, lin.maxCycleSize());
    }
}
