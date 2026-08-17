package io.codelaser.maddi.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.modification.common.util.IsolateClass;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A class isolate emits a small source tree rather than one file: the isolated type in its own package, and one
 * compilation unit of stubs per package the dependencies came from.
 */
public class TestIsolateClass1 {
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
    public static final String VALUE = """
            package p.q;
            public class Value {
                public String text;
                public String describe() { return text; }
            }
            """;

    @Language("java")
    public static final String HELPER = """
            package r.s;
            public class Helper {
                public static p.q.Value convert(p.q.Value v) { return null; }
            }
            """;

    @Language("java")
    public static final String X = """
            package a.b;
            import p.q.Value;
            import r.s.Helper;
            public class X {
                private Value value;
                public String render(Value v) {
                    return v.describe();
                }
                public void store(Object o) {
                    this.value = Helper.convert((p.q.Value) o);
                }
            }
            """;

    private Map<String, String> isolate(String fqn) {
        TypeInfo type = javaInspector.parse(Map.of("p.q.Value", VALUE, "r.s.Helper", HELPER, "a.b.X", X),
                        new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build())
                .parseResult().findType(fqn);
        IsolateClass.Result r = isolateClass.isolate(type);
        // verbatim member sources, as a real driver would take them from CachingSourceExtractor
        Map<MethodInfo, String> sources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            sources.put(original, switch (original.name()) {
                case "render" -> """
                        public String render(Value v) {
                            return v.describe();
                        }""";
                case "store" -> """
                        public void store(Object o) {
                            this.value = Helper.convert((p.q.Value) o);
                        }""";
                default -> "";
            });
        }
        return isolateClass.print(r, sources);
    }

    @DisplayName("one unit for the isolated type, one per package of its dependencies")
    @Test
    public void projectLayout() {
        Map<String, String> project = isolate("a.b.X");
        project.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));

        assertTrue(project.containsKey("a/b/X.java"), project.keySet().toString());
        assertTrue(project.containsKey("p/q/Value.java"), project.keySet().toString());

        // the isolated type keeps its own package and name, so 'p.q.Value' and a bare 'Value' both resolve the
        // way they did in the original -- through the package and an import, not through a namespace stub
        String x = project.get("a/b/X.java");
        assertTrue(x.contains("package a.b;"), x);
        assertTrue(x.contains("import p.q.Value;"), x);
        assertFalse(x.contains("class p {"), "no namespace stubs in a class isolate:\n" + x);
        assertTrue(x.contains("return v.describe();"), x);
        assertTrue(x.contains("this.value = Helper.convert((p.q.Value) o);"), x);
        assertTrue(project.containsKey("r/s/Helper.java"), project.keySet().toString());

        String value = project.get("p/q/Value.java");
        assertTrue(value.contains("package p.q;"), value);
        assertTrue(value.contains("describe"), value);
    }

    @DisplayName("the emitted project parses back on the JDK alone")
    @Test
    public void reparses() throws IOException {
        Map<String, String> project = isolate("a.b.X");
        javaInspector.invalidateAllSources();
        Map<String, String> byFqn = new LinkedHashMap<>();
        project.forEach((path, source) ->
                byFqn.put(path.substring(0, path.length() - ".java".length()).replace('/', '.'), source));
        var summary = javaInspector.parse(byFqn, new JavaInspector.ParseOptions.Builder().build());
        assertEquals(0, summary.parseExceptions().size(), summary.parseExceptions().toString());
        // set-difference, not the exception count: an unresolvable unit is DROPPED with a warning
        assertEquals(byFqn.keySet().size(), summary.types().size(),
                "expected " + byFqn.keySet() + " but got " + summary.types());
    }
}
