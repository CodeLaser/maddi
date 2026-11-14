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

package io.codelaser.maddi.inspection.openjdk;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/*
 CodeLaser/maddi#193: a JDK functional interface that the scan only loads lazily (Supplier, Function: reached from a
 lambda or a method reference, never named in a member signature that completes it) has its single abstract method
 once the parse is done. Supplier is first touched while ScanCompilationUnits.scan() preloads java.lang (one of
 its types names it), and that path cleared the loaded-types accounting without handing the touched types on as
 preloads, so nothing committed it (preloadOnly did; maddi#119).
 */
public class TestLibraryFunctionalInterfaceSam {
    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
                static class Point { Point(Integer x) { } }
                Object inLambda() { java.util.function.Supplier<Integer> s = () -> 4; return s; }
                Object creating() { java.util.function.Function<Integer, Point> f = Point::new; return f; }
            }
            """;

    @Test
    public void test() {
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("a.b.C", INPUT)), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        TypeInfo c = parseResult.findType("a.b.C");
        List<String> seen = new ArrayList<>();
        c.methodStream().forEach(m -> m.methodBody().visit(e -> {
            TypeInfo t = e instanceof Lambda l ? l.abstractFunctionalTypeInfo()
                    : e instanceof MethodReference mr ? mr.parameterizedType().typeInfo() : null;
            if (t != null) {
                seen.add(t.simpleName() + " inspected=" + t.hasBeenInspected() + " fi=" + t.isFunctionalInterface()
                         + " sam=" + (t.singleAbstractMethod() == null ? null : t.singleAbstractMethod().name()));
            }
            return true;
        }));
        assertEquals("[Supplier inspected=true fi=true sam=get, Function inspected=true fi=true sam=apply]",
                seen.toString());
    }
}
