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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type arguments written in a constructor call, {@code new Box<Sol>(4)}, are printed, so they need their imports
 * (GitHub CodeLaser/maddi#178). With {@code var}, nothing else in the unit names them.
 */
public class TestImportConstructorTypeArgument extends CommonTest2 {

    @Language("java")
    private static final String SOL = """
            package a.c;
            public class Sol { }
            """;

    @Language("java")
    private static final String BOX = """
            package a.b;
            public class Box<S> { public Box(int i) { } }
            """;

    @Language("java")
    private static final String V = """
            package a.b;
            import a.c.Sol;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Set;
            public class V {
                Object m() {
                    var box = new Box<Sol>(4);
                    var map = new HashMap<Class<?>, Set<List<Sol>>>();
                    return box.toString() + map;
                }
            }
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.c.Sol", SOL, "a.b.Box", BOX, "a.b.V", V));
        TypeInfo typeInfo = parseResult.findType("a.b.V");
        ImportComputer ic = javaInspector.importComputer(4, typeInfo.compilationUnit().sourceSet());
        String printed = javaInspector.print2(typeInfo.compilationUnit(), (Qualification.Decorator) null, ic);
        assertTrue(printed.contains("new Box<Sol>(4)"), printed);
        for (String imp : new String[]{"a.c.Sol", "java.util.HashMap", "java.util.List", "java.util.Set"}) {
            assertTrue(printed.contains("import " + imp + ";"), imp + "\n" + printed);
        }
    }
}
