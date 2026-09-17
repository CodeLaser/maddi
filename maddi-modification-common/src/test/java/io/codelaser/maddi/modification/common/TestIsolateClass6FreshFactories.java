package io.codelaser.maddi.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.common.util.IsolateClass;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.codelaser.maddi.modification.common.CommonTest.javaInspectorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A stub of a factory says {@code return new T();} when the original <b>constructs what it returns</b>, and
 * {@code return null;} otherwise — {@code ConstructedReturn}, wired into {@code IsolationCore.ensureMethodInfo}.
 * <p>
 * Why: a stub's body is read by the analyses that consume the isolate. With {@code return null;} a local bound
 * to a factory has no creation, and the question the modernization lane asks of every factory-anchored fill
 * site ("is the object fresh?") has nothing to answer from. On the closed-core class isolates the flagship type
 * is obtained almost only through such factories — three families, all of them constructing what they return —
 * and every one of those sites was refused on the strength of a line the isolator invented.
 * <p>
 * The nine shapes below are the decision table. Four say {@code new}: a direct construction, a local filled
 * through itself and returned, a delegation to such a method, and a delegation through an instance receiver.
 * Five stay {@code null}: a shared field, an object registered before it is returned (created is not fresh),
 * a path that may return {@code null}, an array, and a fresh object returned as its interface. The fixture also
 * makes the instantiated stub declare another constructor, so that the no-arg one the {@code new} needs has to
 * be written explicitly — the same pass that supplies it to an extended stub.
 */
public class TestIsolateClass6FreshFactories {
    private JavaInspector javaInspector;
    private IsolateClass isolateClass;

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
    private static final String SHAPE = """
            package p.q;
            public interface Shape {
            }
            """;

    @Language("java")
    private static final String ITEM = """
            package p.q;
            public class Item implements Shape {
                public int weight;
                public String name;
                public Item() { }
                public Item(int weight) { this.weight = weight; }
                public void setName(String name) { this.name = name; }
            }
            """;

    @Language("java")
    private static final String ITEMS = """
            package p.q;
            import java.util.ArrayList;
            import java.util.List;
            public class Items {
                private static final Item SHARED = new Item();
                private static final List<Item> ALL = new ArrayList<>();

                public static Item direct() { return new Item(); }
                public static Item filled() {
                    Item i = new Item();
                    i.weight = 1;
                    i.setName("x");
                    return i;
                }
                public static Item delegating(int w) {
                    Item i = filled();
                    i.weight = w;
                    return i;
                }
                public Item instanceFactory() {
                    Item i = new Item(2);
                    return i;
                }
                public static Item throughInstance(Items items) { return items.instanceFactory(); }

                public static Item shared() { return SHARED; }
                public static Item registered() {
                    Item i = new Item();
                    ALL.add(i);
                    return i;
                }
                public static Item maybe(boolean b) { return b ? new Item() : null; }
                public static Item[] many() { return new Item[1]; }
                public static Shape shape() { return new Item(); }
            }
            """;

    @Language("java")
    private static final String USE = """
            package a.b;
            import p.q.Item;
            import p.q.Items;
            import p.q.Shape;
            public class Use {
                public int run(Items items, boolean b) {
                    Item a = Items.direct();
                    Item c = Items.filled();
                    Item d = Items.delegating(3);
                    Item e = Items.throughInstance(items);
                    Item f = Items.shared();
                    Item g = Items.registered();
                    Item h = Items.maybe(b);
                    Item[] k = Items.many();
                    Shape s = Items.shape();
                    Item own = new Item(5);
                    return a.weight + c.weight + d.weight + e.weight + f.weight + g.weight + h.weight + k.length
                           + s.hashCode() + own.weight;
                }
            }
            """;

    @DisplayName("a factory stub constructs what the original constructs, and says null otherwise")
    @Test
    public void freshFactories() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Shape", SHAPE, "p.q.Item", ITEM, "p.q.Items", ITEMS,
                "a.b.Use", USE), "a.b.Use");
        String items = tree.get("p/q/Items.java");
        assertEquals("new Item()", returned(items, "direct"));
        assertEquals("new Item()", returned(items, "filled"));
        assertEquals("new Item()", returned(items, "delegating"));
        assertEquals("new Item()", returned(items, "throughInstance"));
        assertEquals("null", returned(items, "shared"));
        assertEquals("null", returned(items, "registered"));   // created is not fresh
        assertEquals("null", returned(items, "maybe"));
        assertEquals("null", returned(items, "many"));
        assertEquals("null", returned(items, "shape"));        // constructed as Item, returned as Shape
        // 'new Item(5)' in the verbatim text declares Item(int) on the stub, so the no-arg constructor the
        // factories' 'new Item()' resolves against has to be written out
        String item = tree.get("p/q/Item.java");
        assertTrue(item.contains("Item(int weight)"), item);
        assertTrue(item.matches("(?s).*\\bItem\\(\\)\\s*\\{.*"), item);
        assertCompiles(tree);
    }

    /** The expression the stub of {@code name} returns. */
    private static String returned(String unit, String name) {
        Matcher m = Pattern.compile("\\b" + name + "\\([^)]*\\)\\s*\\{\\s*return\\s+([^;]+);").matcher(unit);
        assertTrue(m.find(), () -> "no stub of " + name + " in\n" + unit);
        return m.group(1).trim();
    }

    // -- the same harness as TestIsolateClass4Compiles ------------------------------------------------------

    private Map<String, String> isolate(Map<String, String> sources, String fqn) {
        var parsed = javaInspector.parse(sources,
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        TypeInfo type = parsed.parseResult().findType(fqn);
        IsolateClass.Result r = isolateClass.isolate(type);
        Map<MethodInfo, String> memberSources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            memberSources.put(original, verbatim(sources.get(original.primaryType().fullyQualifiedName()), original));
        }
        Map<String, String> tree = isolateClass.print(r, memberSources);
        tree.forEach((path, source) -> System.out.println("===== " + path + "\n" + source));
        return tree;
    }

    private void assertCompiles(Map<String, String> tree) throws IOException {
        Map<String, String> byFqn = new LinkedHashMap<>();
        tree.forEach((path, source) ->
                byFqn.put(path.substring(0, path.length() - ".java".length()).replace('/', '.'), source));
        javaInspector.invalidateAllSources();
        var summary = javaInspector.parse(byFqn, new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).build());
        assertEquals(byFqn.keySet().size(), summary.types().size(),
                "expected " + byFqn.keySet() + " but got " + summary.types());
    }

    private static String verbatim(String unitSource, MethodInfo methodInfo) {
        String[] lines = unitSource.split("\n", -1);
        var source = methodInfo.source();
        StringBuilder sb = new StringBuilder();
        for (int line = source.beginLine(); line <= source.endLine(); line++) {
            if (line > source.beginLine()) sb.append('\n');
            sb.append(lines[line - 1]);
        }
        return sb.toString();
    }
}
