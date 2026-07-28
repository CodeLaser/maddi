package org.e2immu.analyzer.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.util.IsolateClass;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The cases that cost {@link org.e2immu.analyzer.modification.common.util.IsolateMethod} its defects, in a class
 * isolate: two types with the same simple name in different packages, a member type named through its enclosing
 * type, an interface with a generic method, and a JDK type kept as itself.
 * <p>
 * None of them is a conflict here. A project has packages, so the simple-name clash resolves through an import
 * exactly as it did in the original, and a member type keeps its real nesting instead of competing for a slot.
 */
public class TestIsolateClass2 {
    protected JavaInspector javaInspector;
    protected IsolateClass isolateClass;

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        javaInspector = javaInspectorFactory().withSources(SourceSetImpl.testProtocolSourceSet());
        isolateClass = new IsolateClass(javaInspector);
    }

    @Language("java")
    public static final String ENTRY_A = """
            package one;
            public class Entry {
                public String name() { return null; }
            }
            """;

    @Language("java")
    public static final String ENTRY_B = """
            package two;
            public class Entry {
                public int size() { return 0; }
            }
            """;

    @Language("java")
    public static final String OUTER = """
            package three;
            public class Outer {
                public static class Inner {
                    public long id() { return 0L; }
                }
                public interface Sink<T> {
                    void accept(T t);
                }
            }
            """;

    @Language("java")
    public static final String X = """
            package a.b;
            import one.Entry;
            import three.Outer;
            import java.util.List;
            import java.util.ArrayList;
            public class X implements Outer.Sink<Entry> {
                private final List<Entry> entries = new ArrayList<>();
                private Outer.Inner inner;
                @Override
                public void accept(Entry t) {
                    entries.add(t);
                }
                public int total(two.Entry other) {
                    return other.size() + entries.size() + (int) inner.id();
                }
            }
            """;

    private Map<String, String> isolate() {
        TypeInfo type = javaInspector.parse(Map.of(
                                "one.Entry", ENTRY_A, "two.Entry", ENTRY_B,
                                "three.Outer", OUTER, "a.b.X", X),
                        new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build())
                .parseResult().findType("a.b.X");
        IsolateClass.Result r = isolateClass.isolate(type);
        Map<MethodInfo, String> sources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            sources.put(original, switch (original.name()) {
                case "accept" -> """
                        @Override
                        public void accept(Entry t) {
                            entries.add(t);
                        }""";
                case "total" -> """
                        public int total(two.Entry other) {
                            return other.size() + entries.size() + (int) inner.id();
                        }""";
                default -> "";
            });
        }
        return isolateClass.print(r, sources);
    }

    @DisplayName("same simple name in two packages, a member type, an interface, and a JDK type")
    @Test
    public void hardCases() {
        Map<String, String> project = isolate();
        project.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));

        assertTrue(project.containsKey("a/b/X.java"), project.keySet().toString());
        // BOTH Entry types are stubbed, each in its own package -- in a single-frame isolate only one of them
        // could have held the simple name
        assertTrue(project.containsKey("one/Entry.java"), project.keySet().toString());
        assertTrue(project.containsKey("two/Entry.java"), project.keySet().toString());
        assertTrue(project.containsKey("three/Outer.java"), project.keySet().toString());

        String x = project.get("a/b/X.java");
        assertTrue(x.contains("import one.Entry;"), x);
        // 'two.Entry' stays written out in the verbatim body, and needs no import to resolve
        assertTrue(x.contains("public int total(two.Entry other)"), x);
        // the JDK types are kept as themselves
        assertTrue(x.contains("import java.util.List;") || x.contains("java.util.List"), x);

        // Inner and Sink keep their real nesting inside Outer, so 'Outer.Inner' and 'Outer.Sink<Entry>' resolve
        String outer = project.get("three/Outer.java");
        assertTrue(outer.contains("class Inner"), outer);
        assertTrue(outer.contains("Sink"), outer);
    }

    @DisplayName("the emitted project parses back on the JDK alone")
    @Test
    public void reparses() {
        Map<String, String> project = isolate();
        javaInspector.invalidateAllSources();
        Map<String, String> byFqn = new LinkedHashMap<>();
        project.forEach((path, source) ->
                byFqn.put(path.substring(0, path.length() - ".java".length()).replace('/', '.'), source));
        var summary = javaInspector.parse(byFqn, new JavaInspector.ParseOptions.Builder().build());
        assertEquals(0, summary.parseExceptions().size(), summary.parseExceptions().toString());
        // set-difference: an unresolvable unit is DROPPED with a warning, never reported as a parse error
        assertEquals(byFqn.keySet().size(), summary.types().size(),
                "expected " + byFqn.keySet() + "\nbut got  " + summary.types()
                + "\n\n" + String.join("\n-----\n", project.values()));
    }
}
