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

package io.codelaser.maddi.graph.analyzer;

import io.codelaser.maddi.graph.op.BreakCycles;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeDependencies {

    // cp analyzer/build/e2immuGraph/typeDependencies.gml analyzer/build/e2immuGraph/packageDependenciesBasedOnTypeGraph.gml  graph/src/test/resources/io/codelaser/maddi/graph

    @Tag("slow")
    @Test
    public void test() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                                                                               + "io/codelaser/maddi/graph/typeDependencies.gml", Main.SEQUENTIAL});
        assertEquals(31, lin.maxCycleSize());
        assertEquals(101, lin.actionLog().size());
        assertEquals(100, lin.list().size());
    }

    @Test
    public void testParallel() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "io/codelaser/maddi/graph/typeDependencies.gml", Main.PARALLEL});
        assertEquals(31, lin.maxCycleSize());
        assertEquals(101, lin.actionLog().size());
        assertEquals(100, lin.list().size());
    }

    @Test
    public void testVertexWeight() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "io/codelaser/maddi/graph/typeDependencies.gml", Main.VERTEX_WEIGHT});
        assertEquals(1, lin.maxCycleSize());
        assertEquals(143, lin.actionLog().size());
        assertEquals(168, lin.list().size()); // must be <= 1042, the number of nodes
    }

    @Test
    public void test2() throws IOException {
        Main main = new Main();
        BreakCycles.Linearization<TypeGraphIO.Node> lin = main.go(new String[]{Main.CLASSPATH
                + "io/codelaser/maddi/graph/packageDependenciesBasedOnTypeGraph.gml"});
        assertEquals(9, lin.list().size());
        assertEquals(5, lin.actionLog().size());
        assertEquals(36, lin.maxCycleSize());
    }
}
