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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
        Edited edited = edit(Map.of("Circle", CIRCLE, "Holder", HOLDER, "Other", OTHER), Map.of("Circle", newCircle));
        assertEquals(Set.of(edited.before("Holder")), edited.dependents());
        assertNotSame(edited.before("Holder"), edited.after("Holder"));
        assertSame(edited.before("Other"), edited.after("Other"), "not a dependent: kept in every parse");
        return new Warm(edited.outcome(), edited.before("Holder"), edited.before("Other"));
    }

    private record Edited(ReparseWithDependents.Outcome outcome, ParseResult before, Set<TypeInfo> dependents) {
        TypeInfo before(String simpleName) {
            return before.findType("s." + simpleName);
        }

        TypeInfo after(String simpleName) {
            return outcome.parseResult().findType("s." + simpleName);
        }

        Set<String> reparsedDependents() {
            Set<String> names = new TreeSet<>();
            outcome.reparsedDependents().forEach(t -> names.add(t.simpleName()));
            return names;
        }
    }

    /*
     The files of package s by simple name, then the edits: the edited types are the changed ones, their dependents
     come from the call graph. Ends by comparing the warm call graph with a cold parse's.
     */
    private Edited edit(Map<String, String> files, Map<String, String> edits) throws IOException {
        Path src = root.resolve("src");
        for (Map.Entry<String, String> e : files.entrySet()) write(src, "s/" + e.getKey() + ".java", e.getValue());
        SourceSet set = new SourceSetImpl.Builder().setName("main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).build();
        JavaInspector inspector = inspector(set);
        ParseResult pr = inspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        ComputeCallGraph ccg = new ComputeCallGraph(inspector.runtime(), pr, _ -> false).go();
        Set<TypeInfo> changed = new HashSet<>();
        for (String name : edits.keySet()) changed.add(pr.findType("s." + name));
        Set<TypeInfo> dependents = new PrimaryTypeUseGraph(ccg.graph()).dependentsOf(changed);

        for (Map.Entry<String, String> e : edits.entrySet()) write(src, "s/" + e.getKey() + ".java", e.getValue());
        ReparseWithDependents.Outcome outcome = ReparseWithDependents.go(inspector, changed, dependents,
                invalidated -> new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true)
                        .setInvalidated(invalidated).build());

        ParseResult warm = outcome.parseResult();
        for (TypeInfo type : changed) assertNotSame(type, warm.findType(type.fullyQualifiedName()));
        JavaInspector cold = inspector(set);
        ParseResult coldResult = cold.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();
        assertEquals(ComputeCallGraph.print(new ComputeCallGraph(cold.runtime(), coldResult, _ -> false).go().graph()),
                ComputeCallGraph.print(new ComputeCallGraph(inspector.runtime(), warm, _ -> false).go().graph()),
                "the warm call graph as a cold parse's");
        // a rewired dependent keeps its compilation unit, a re-parsed one has a new one
        for (TypeInfo type : dependents) {
            boolean reparsed = outcome.reparsedDependents().contains(type);
            TypeInfo now = warm.findType(type.fullyQualifiedName());
            assertEquals(!reparsed, type.compilationUnit().fingerPrintOrNull() == now.compilationUnit()
                    .fingerPrintOrNull(), type + (reparsed ? " re-parsed" : " rewired"));
        }
        return new Edited(outcome, pr, dependents);
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

    // test-manager, 2026-11-10: a declaration change in one changed type made every dependent re-parse
    @DisplayName("two changed types, one with a declaration change: only its dependents are parsed again")
    @Test
    public void unrelatedDependent() throws IOException {
        String square = """
                package s;
                public class Square {
                    public double side() { return 1; }
                }
                """;
        String squareUse = """
                package s;
                public class SquareUse {
                    public double twice(Square s) { return 2 * s.side(); }
                }
                """;
        Edited edited = edit(Map.of("Circle", CIRCLE, "Holder", HOLDER, "Square", square, "SquareUse", squareUse),
                Map.of("Circle", CIRCLE.replace("SIDES = 0", "SIDES = 1"),
                        "Square", square.replace("return 1;", "return 2;")));
        assertEquals(Set.of("s.Circle"), edited.outcome().declarationsChanged());
        assertEquals(Set.of("Holder"), edited.reparsedDependents(), "SquareUse does not rely on Circle");
    }

    @DisplayName("a call through a chain: the type a call resolves in, never named in the dependent")
    @Test
    public void chain() throws IOException {
        String shape = """
                package s;
                public class Shape {
                    public double scale(Object o) { return 1; }
                }
                """;
        String disk = """
                package s;
                public class Disk extends Shape {
                    public double r() { return 1; }
                }
                """;
        String factory = """
                package s;
                public class Factory {
                    public Disk make() { return new Disk(); }
                }
                """;
        // Holder never writes Disk: factory.make() is a Disk, and scale resolves in it
        String holder = """
                package s;
                public class Holder {
                    private final Factory factory = new Factory();
                    public double area() { return factory.make().scale("x"); }
                }
                """;
        Edited edited = edit(Map.of("Shape", shape, "Disk", disk, "Factory", factory, "Holder", holder),
                Map.of("Disk", disk.replace("public double r() { return 1; }",
                        "public double r() { return 1; }\n    public double scale(String s) { return 2; }")));
        assertEquals(Set.of("s.Disk"), edited.outcome().declarationsChanged());
        assertTrue(edited.reparsedDependents().contains("Holder"), "Holder relies on Disk's members");
    }

    /*
     The signature holds a constant's initializer as written, not its value: Holder's 'Circle.SIDES + 1' reads the
     same after the edit, and User, which reads Holder.MORE through the field, is rewired onto the new Holder.
     */
    @DisplayName("a constant computed from a changed one: the type computing it is parsed again, its users rewired")
    @Test
    public void cascade() throws IOException {
        String holder = """
                package s;
                public class Holder {
                    public static final int MORE = Circle.SIDES + 1;
                }
                """;
        // relies on Holder's constant, never on Circle
        String user = """
                package s;
                public class User {
                    public int v() { return Holder.MORE; }
                }
                """;
        Edited edited = edit(Map.of("Circle", CIRCLE, "Holder", holder, "User", user, "Other", OTHER),
                Map.of("Circle", CIRCLE.replace("SIDES = 0", "SIDES = 1")));
        assertEquals(Set.of("s.Circle"), edited.outcome().declarationsChanged());
        assertEquals(Set.of("Holder"), edited.reparsedDependents());
        assertSame(edited.before("Other"), edited.after("Other"));
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
