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

package io.codelaser.maddi.callgraph;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;

/**
 * A class literal in a TYPE-USE annotation's arguments is a dependency: {@code List<@WithConverter(Trimmed.class)
 * String>} depends on Trimmed. addType recorded the annotation type and not its arguments, which doAnnotations does
 * visit for a declaration annotation; so seam and split analyses missed the edge (Quarkus vertx-http config
 * interfaces, the modularization campaign's QE-31: a carve called acyclic did not compile).
 */
public class TestCallGraphTypeUseAnnotationArgument extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.util.List;
            import java.util.Optional;
            class X {
                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.METHOD, ElementType.TYPE_USE})
                @interface WithConverter {
                    Class<?> value();
                }
                static final class Trimmed {
                }
                static final class PartConverter {
                }
                interface Config {
                    List<@WithConverter(Trimmed.class) String> rolesAllowed();
                    Optional<List<@WithConverter(PartConverter.class) String>> parts();
                }
            }
            """;

    @DisplayName("a type-use annotation's class-literal argument is an edge, one and two type arguments deep")
    @Test
    public void test() throws Exception {
        // the javac front end, as the engines parse: type-use annotations on type arguments are its CST's
        openJdkParser();
        runtime = javaInspector.runtime();
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        G<Info> graph = new ComputeCallGraph(runtime, X).go().graph();
        String printed = ComputeCallGraph.print(graph);
        // before the fix only `rolesAllowed()->D->a.b.X.WithConverter` was there
        assertTrue(printed.matches("(?s).*a\\.b\\.X\\.Config\\.rolesAllowed\\(\\)->\\w+->a\\.b\\.X\\.Trimmed.*"),
                printed);
        assertTrue(printed.matches("(?s).*a\\.b\\.X\\.Config\\.parts\\(\\)->\\w+->a\\.b\\.X\\.PartConverter.*"),
                printed);
    }
}
