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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * {@link ReparseWithDependents}: an edit that leaves the changed type's declarations alone rewires the dependents; one
 * that moves them parses the dependents again, because javac resolves their names differently. Every case ends with
 * the call graph of the warm result against a cold parse's: the graph names each call's target.
 */
public class TestReparseWithDependents {

    @Language("java")
    private static final String CIRCLE = """
            package s;
            public class Circle {
                public static final int SIDES = 0;
                public static final String NAME = "c" + "ircle"; // a comment above moves its position, not its value
                private double r;
                public Circle(double r) { this.r = r; }
                public double scale(Object o) { return r; }
            }
            """;

    @Language("java")
    private static final String HOLDER = """
            package s;
            public class Holder {
                private final Circle circle = new Circle(1);
                public double area() { return circle.scale("x"); }
                public String text() { return circle.toString(); }
                public int sides() { return Circle.SIDES; }
            }
            """;

    // uses neither
    @Language("java")
    private static final String OTHER = """
            package s;
            public class Other {
                public int v() { return 1; }
            }
            """;

    @TempDir
    Path root;

    private record Warm(ReparseWithDependents.Outcome outcome, TypeInfo holderBefore, TypeInfo otherBefore) {
        TypeInfo holder() {
            return outcome.parseResult().findType("s.Holder");
        }
    }

    private Warm edit(String newCircle) throws IOException {
        Path src = root.resolve("src");
        write(src, "s/Circle.java", CIRCLE);
        write(src, "s/Holder.java", HOLDER);
        write(src, "s/Other.java", OTHER);
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).build();
        JavaInspector inspector = inspector(set);
        ParseResult pr = inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph ccg = new ComputeCallGraph(inspector.runtime(), pr, _ -> false).go();
        TypeInfo circle = pr.findType("s.Circle");
        TypeInfo holder = pr.findType("s.Holder");
        TypeInfo other = pr.findType("s.Other");
        Set<TypeInfo> dependents = new PrimaryTypeUseGraph(ccg.graph()).dependentsOf(Set.of(circle));
        assertEquals(Set.of(holder), dependents);

        write(src, "s/Circle.java", newCircle);
        ReparseWithDependents.Outcome outcome = ReparseWithDependents.go(inspector, Set.of(circle), dependents,
                invalidated -> new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true)
                        .setInvalidated(invalidated).build());

        ParseResult warm = outcome.parseResult();
        assertNotSame(circle, warm.findType("s.Circle"));
        assertNotSame(holder, warm.findType("s.Holder"));
        assertSame(other, warm.findType("s.Other"), "not a dependent: kept in both parses");
        JavaInspector cold = inspector(set);
        ParseResult coldResult = cold.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        assertEquals(ComputeCallGraph.print(new ComputeCallGraph(cold.runtime(), coldResult, _ -> false).go().graph()),
                ComputeCallGraph.print(new ComputeCallGraph(inspector.runtime(), warm, _ -> false).go().graph()),
                "the warm call graph as a cold parse's");
        return new Warm(outcome, holder, other);
    }

    private static void assertRewired(Warm warm) {
        assertEquals(Set.of(), warm.outcome().declarationsChanged());
        assertFalse(warm.outcome().dependentsReparsed());
        assertSame(warm.holderBefore().compilationUnit().fingerPrintOrNull(),
                warm.holder().compilationUnit().fingerPrintOrNull(), "rewired, not re-parsed");
    }

    private static void assertReparsed(Warm warm) {
        assertEquals(Set.of("s.Circle"), warm.outcome().declarationsChanged());
        assertTrue(warm.outcome().dependentsReparsed());
    }

    @DisplayName("a comment and a body: the dependents are rewired")
    @Test
    public void body() throws IOException {
        assertRewired(edit("// a comment\n" + CIRCLE.replace("return r;", "double q = r * 2; return q / 2;")));
    }

    @DisplayName("a private method: no other primary type can name it, the dependents are rewired")
    @Test
    public void privateMember() throws IOException {
        assertRewired(edit(CIRCLE.replace("private double r;", "private double r;\n    private void help() { r++; }")));
    }

    @DisplayName("an overload: Holder's scale(\"x\") now calls scale(String)")
    @Test
    public void overload() throws IOException {
        Warm warm = edit(CIRCLE.replace("public double scale(Object o) { return r; }",
                "public double scale(Object o) { return r; }\n    public double scale(String s) { r++; return r; }"));
        assertReparsed(warm);
        assertTrue(warm.holder().findUniqueMethod("area", 0).methodBody().toString().contains("scale"));
    }

    @DisplayName("an inherited method overridden: Holder's toString() now calls Circle's")
    @Test
    public void override() throws IOException {
        assertReparsed(edit(CIRCLE.replace("public double scale(Object o) { return r; }",
                "public double scale(Object o) { return r; }\n    @Override public String toString() { r++; return \"c\"; }")));
    }

    @DisplayName("a constant's value, which javac may inline in a dependent")
    @Test
    public void constant() throws IOException {
        assertReparsed(edit(CIRCLE.replace("SIDES = 0", "SIDES = 1")));
    }

    @DisplayName("a supertype")
    @Test
    public void supertype() throws IOException {
        assertReparsed(edit(CIRCLE.replace("public class Circle {", "public class Circle implements Cloneable {")));
    }

    @DisplayName("a return type")
    @Test
    public void returnType() throws IOException {
        assertReparsed(edit(CIRCLE.replace("public double scale(Object o) { return r; }",
                "public float scale(Object o) { return (float) r; }")));
    }

    private static JavaInspector inspector(SourceSet set) {
        try {
            JavaInspector inspector = new JavaInspectorImpl(true, false);
            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(set)
                    .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                    .build();
            inspector.initialize(inputConfiguration);
            return inspector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void write(Path sourceRoot, String relative, String content) throws IOException {
        Path file = sourceRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
