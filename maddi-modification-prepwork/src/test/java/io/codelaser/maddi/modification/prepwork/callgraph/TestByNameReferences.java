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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.modification.prepwork.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static io.codelaser.maddi.inspection.integration.JavaInspectorImpl.TEST_PROTOCOL_PREFIX;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A string literal that a declared {@link ByNameSink} turns into a type: the reference the compiler never sees and
 * an editor must still update.
 * <p>
 * The fixture is Cassandra's shape in miniature. {@code Registry} is a resolver a refactoring wrote —
 * {@code field(binaryName, member)} — and {@code Holder} is the table that calls it, in every spelling that
 * matters: a literal, a literal naming a NESTED type, a name held by a {@code static final} constant, and three
 * shapes no recogniser can read (a concatenation, a parameter, a name that is not in the parse).
 */
public class TestByNameReferences extends CommonTest {

    @Language("java")
    private static final String REGISTRY = """
            package a.b;
            public class Registry {
                public static Object field(String className, String member) { return null; }
                public static Object type(String className) { return null; }
            }
            """;

    @Language("java")
    private static final String TARGET = """
            package a.b;
            public class Target {
                public static final Object instance = new Object();
                public static class Nested {
                    public static final Object deep = new Object();
                }
                public static void overloaded() { }
                public static void overloaded(int i) { }
            }
            """;

    @Language("java")
    private static final String HOLDER = """
            package a.b;
            public class Holder {
                private static final String TARGET_NAME = "a.b.Target";
                private final String fromOutside;

                public Holder(String fromOutside) {
                    this.fromOutside = fromOutside;
                }

                public Object literal() {
                    return Registry.field("a.b.Target", "instance");
                }
                public Object nested() {
                    return Registry.field("a.b.Target$Nested", "deep");
                }
                public Object viaConstant() {
                    return Registry.type(TARGET_NAME);
                }
                public Object ambiguousMember() {
                    return Registry.field("a.b.Target", "overloaded");
                }
                public Object concatenated(String suffix) {
                    return Registry.type("a.b." + suffix);
                }
                public Object fromAParameter() {
                    return Registry.type(fromOutside);
                }
                public Object notInTheParse() {
                    return Registry.type("com.elsewhere.Absent");
                }
            }
            """;

    private static final List<ByNameSink> SINKS = List.of(
            new ByNameSink("a.b.Registry", "field", 2, 0, 1, ByNameSink.Kind.FIELD),
            new ByNameSink("a.b.Registry", "type", 1, 0, -1, ByNameSink.Kind.TYPE));

    private ParseResult parse() throws IOException {
        Map<String, String> sourcesByURIString = Map.of("a.b.Registry", REGISTRY, "a.b.Target", TARGET,
                        "a.b.Holder", HOLDER).entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> TEST_PROTOCOL_PREFIX + e.getKey(), Map.Entry::getValue));
        javaInspector = new JavaInspectorImpl();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .addClassPath(JavaInspectorImpl.MADDI_SUPPORT)
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/platform/commons")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/opentest4j");
        sourcesByURIString.keySet().forEach(builder::addSources);
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).build();
        Summary summary = javaInspector.parse(sourcesByURIString, parseOptions);
        return summary.parseResult();
    }

    private ComputeCallGraph compute(ParseResult parseResult, List<ByNameSink> sinks) {
        return new ComputeCallGraph(runtime, parseResult, t -> false)
                .withByNameSinks(sinks, parseResult).go();
    }

    @DisplayName("every spelling that can be read, and every one that cannot")
    @Test
    public void test() throws IOException {
        ParseResult parseResult = parse();
        ComputeCallGraph ccg = compute(parseResult, SINKS);

        assertEquals("""
                a.b.Holder.literal() -> a.b.Target#instance (FIELD)
                a.b.Holder.nested() -> a.b.Target$Nested#deep (FIELD)
                a.b.Holder.viaConstant() -> a.b.Target (TYPE, via constant)
                a.b.Holder.ambiguousMember() -> a.b.Target#overloaded (FIELD)""",
                ccg.byNameReferences().stream().map(Object::toString).sorted(
                        // source order is the member order of the fixture, which is what a reader expects
                        java.util.Comparator.comparingInt(s -> HOLDER.indexOf(s.substring(9, s.indexOf('(')))))
                        .reduce((a, b) -> a + "\n" + b).orElse(""));

        // ⛔ the blind spot is a NUMBER: a concatenation and a parameter, both calls to a declared sink whose name
        // this cannot read. "com.elsewhere.Absent" is NOT among them -- it was read perfectly and simply is not
        // ours, which is a different thing and must not inflate the count.
        assertEquals(2, ccg.unresolvedSinkCalls());
    }

    @DisplayName("the resolved member, where it is unambiguous; null where the sink's own rule would decide")
    @Test
    public void members() throws IOException {
        ParseResult parseResult = parse();
        Map<String, ByNameReference> byMember = compute(parseResult, SINKS).byNameReferences().stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.from().simpleName(), r -> r));

        TypeInfo target = parseResult.findType("a.b.Target");
        assertSame(target.getFieldByName("instance", true), byMember.get("literal").targetMember());
        assertSame(parseResult.findType("a.b.Target.Nested").getFieldByName("deep", true),
                byMember.get("nested").targetMember());
        assertNull(byMember.get("ambiguousMember").targetMember(),
                "two methods called 'overloaded': which one a binding picks is the SINK's rule, not the graph's");
        assertNull(byMember.get("viaConstant").targetMember(), "that sink names no member");
        assertSame(target, byMember.get("viaConstant").targetType());
    }

    @DisplayName("⭐ the site is the literal's range, and for a constant it is in ANOTHER member than the caller")
    @Test
    public void theSiteIsWhereTheLiteralIsWritten() throws IOException {
        ParseResult parseResult = parse();
        Map<String, ByNameReference> byMember = compute(parseResult, SINKS).byNameReferences().stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.from().simpleName(), r -> r));

        ByNameReference literal = byMember.get("literal");
        assertEquals(false, literal.viaConstant());
        assertNotNull(literal.siteSource());
        assertEquals(11, literal.siteSource().beginLine(), "the line of the call in the fixture");
        assertNotNull(literal.memberSource(), "the member literal has its own range: a rename edits only that");

        // the constant hop: the CALL is in viaConstant(), the LITERAL is up in the field's initialiser
        ByNameReference viaConstant = byMember.get("viaConstant");
        assertEquals(true, viaConstant.viaConstant());
        assertEquals(3, viaConstant.siteSource().beginLine(),
                "a verb that rewrites this must follow siteSource, not from()");
        assertEquals("viaConstant", viaConstant.from().simpleName());
    }

    @DisplayName("the edge is SOFT: below every threshold, so no cycle, giant or analysis order changes")
    @Test
    public void theEdgeIsSoft() throws IOException {
        ParseResult parseResult = parse();
        G<Info> graph = compute(parseResult, SINKS).graph();

        TypeInfo holder = parseResult.findType("a.b.Holder");
        TypeInfo target = parseResult.findType("a.b.Target");
        Long weight = graph.edges(new V<>((Info) holder.findUniqueMethod("literal", 0))).get(new V<>((Info) target));
        assertNotNull(weight, "Holder.literal names a.b.Target by name");
        assertEquals("n", ComputeCallGraph.edgeValuePrinter(weight));
        assertTrue(ComputeCallGraph.isByName(weight));
        assertEquals(false, ComputeCallGraph.isAtLeastReference(weight), "a string is not a compile dependency");
        assertEquals(1, ComputeCallGraph.byNameReferenceCount(weight));
    }

    /**
     * ⛔ <b>A resolver written per PACKAGE has no fully-qualified name to declare.</b> {@code registry.stringify}
     * writes {@code LazyBinding} once per package, package-private — Cassandra carries two copies, and a third
     * corpus will have its own. A sink whose {@code typeFqn} has no dot is therefore matched by SIMPLE name, in
     * any package; this is the shape the whole feature exists for.
     */
    @DisplayName("a sink named by its simple name matches the resolver in every package it was written into")
    @Test
    public void aSimpleNameMatchesEveryPackage() throws IOException {
        ParseResult parseResult = parse();
        List<ByNameSink> bySimpleName = List.of(
                new ByNameSink("Registry", "field", 2, 0, 1, ByNameSink.Kind.FIELD),
                new ByNameSink("Registry", "type", 1, 0, -1, ByNameSink.Kind.TYPE));
        assertEquals(compute(parseResult, SINKS).byNameReferences().size(),
                compute(parseResult, bySimpleName).byNameReferences().size(),
                "a.b.Registry by simple name must find exactly what the fully-qualified sink finds");

        // and a dotted name still means exactly that type: a sink for another package's Registry finds nothing
        assertEquals(List.of(), compute(parseResult,
                List.of(new ByNameSink("x.y.Registry", "field", 2, 0, 1, ByNameSink.Kind.FIELD)))
                .byNameReferences());
    }

    /**
     * ⭐ <b>The off switch, which is the most important test here.</b> maddi recognises nothing until a caller
     * declares sinks, so every existing user must see the graph it has always seen — not "almost", but bit for bit.
     */
    @DisplayName("with no sinks declared: no rows, and a graph identical weight for weight")
    @Test
    public void offByDefault() throws IOException {
        ParseResult parseResult = parse();
        ComputeCallGraph off = new ComputeCallGraph(runtime, parseResult, t -> false).go();

        assertEquals(List.of(), off.byNameReferences());
        assertEquals(0, off.unresolvedSinkCalls());

        // the same parse, computed twice: with sinks, and without. Every weight of the OFF graph must appear
        // unchanged in the ON graph -- the new lane adds bits, it never moves one.
        ComputeCallGraph on = compute(parseResult, SINKS);
        int compared = 0;
        for (V<Info> v : off.graph().vertices()) {
            Map<V<Info>, Long> offEdges = off.graph().edges(v);
            if (offEdges == null) continue;
            Map<V<Info>, Long> onEdges = on.graph().edges(v);
            assertNotNull(onEdges, v + " lost all its edges");
            for (Map.Entry<V<Info>, Long> e : offEdges.entrySet()) {
                Long onWeight = onEdges.get(e.getKey());
                assertNotNull(onWeight, v + " -> " + e.getKey() + " disappeared");
                long withoutByName = onWeight - ComputeCallGraph.byNameReferenceCount(onWeight)
                                                * ComputeCallGraph.BY_NAME_REFERENCES;
                assertEquals(e.getValue(), withoutByName,
                        v + " -> " + e.getKey() + ": " + ComputeCallGraph.edgeValuePrinter(e.getValue())
                        + " became " + ComputeCallGraph.edgeValuePrinter(onWeight));
                ++compared;
            }
        }
        assertTrue(compared > 20, "expected a graph worth comparing, compared " + compared + " edges");
    }
}
