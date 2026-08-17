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
 * A class isolate keeps ALL of the type's members, pasted verbatim, on a type that IS the original. So anything
 * one of those bodies names on itself is already declared, and stubbing it as well declares it twice.
 * <p>
 * Neither defect below is visible to a re-parse: maddi accepts a duplicate declaration, and accepts a constructor
 * inside an interface, so {@code TestIsolateClass1.reparses} passes on a tree javac rejects outright. They were
 * found by compiling the hundred-class closed-core isolate corpus: 76 of the 100 trees had duplicate members (3727 in
 * total), and 24 had {@code "<identifier> expected"} from an interface stub that had been given a constructor.
 * Not one of the 100 compiled.
 */
public class TestIsolateClass3SelfReference {
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
    public static final String ITEM = """
            package p.q;
            public interface Item {
                String name();
            }
            """;

    /*
    'helper()' unqualified, 'C.helper()' written out, and 'this.size()' all name a member of C itself, and the
    called member is declared BELOW its caller -- which is the ordinary case, and the one where a fix applied
    during the walk rather than before it would still stub.
     */
    @Language("java")
    public static final String C = """
            package a.b;
            import p.q.Item;
            public class C {
                public int render() {
                    return helper() + C.helper() + this.size();
                }
                public static int helper() { return 1; }
                public int size() { return 2; }
                public Item[] items(int n) {
                    return new Item[n];
                }
            }
            """;

    private Map<String, String> isolate() {
        TypeInfo type = javaInspector.parse(Map.of("p.q.Item", ITEM, "a.b.C", C),
                        new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build())
                .parseResult().findType("a.b.C");
        IsolateClass.Result r = isolateClass.isolate(type);
        Map<MethodInfo, String> sources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            sources.put(original, switch (original.name()) {
                case "render" -> """
                        public int render() {
                            return helper() + C.helper() + this.size();
                        }""";
                case "helper" -> "public static int helper() { return 1; }";
                case "size" -> "public int size() { return 2; }";
                case "items" -> """
                        public Item[] items(int n) {
                            return new Item[n];
                        }""";
                default -> "";
            });
        }
        return isolateClass.print(r, sources);
    }

    @DisplayName("a member the isolated type calls on itself is declared once, not twice")
    @Test
    public void selfCallIsNotStubbed() {
        String c = isolate().get("a/b/C.java");
        System.out.println(c);
        assertEquals(1, count(c, "int helper("), "'helper' must be declared exactly once:\n" + c);
        assertEquals(1, count(c, "int size("), "'size' must be declared exactly once:\n" + c);
        // the verbatim text, not a stub body: a stub would read '{ return 0; }'
        assertTrue(c.contains("public static int helper() { return 1; }"), c);
        assertTrue(c.contains("public int size() { return 2; }"), c);
    }

    @DisplayName("'new Item[n]' does not give the Item stub a constructor")
    @Test
    public void arrayCreationIsNotAConstructorCall() {
        Map<String, String> project = isolate();
        String item = project.get("p/q/Item.java");
        assertNotNull(item, project.keySet().toString());
        System.out.println(item);
        // 'interface Item { Item(int dim0) { } }' is not Java at all -- javac stops at "<identifier> expected"
        assertEquals(0, count(item, "Item("), "an interface stub has no constructor:\n" + item);
        assertTrue(item.contains("name"), item);
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) n++;
        return n;
    }
}
