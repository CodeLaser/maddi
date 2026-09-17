package io.codelaser.maddi.modification.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import io.codelaser.maddi.modification.common.util.IsolateClass;
import io.codelaser.maddi.modification.common.util.ProgramHierarchy;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    /** The other half of "can this factory be trusted": whether another body can run in its place. */
    @Language("java")
    private static final String MAKER = """
            package p.q;
            public final class Maker {
                public Item make() { return new Item(); }
            }
            """;

    @Language("java")
    private static final String OPEN = """
            package p.q;
            public class Open {
                public final Item sealedMake() { return new Item(); }
                public Item openMake() { return new Item(); }
            }
            """;

    @Language("java")
    private static final String USE = """
            package a.b;
            import p.q.Item;
            import p.q.Items;
            import p.q.Maker;
            import p.q.Open;
            import p.q.Shape;
            public class Use {
                public int made(Maker maker, Open open) {
                    return maker.make().weight + open.sealedMake().weight + open.openMake().weight;
                }
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
                "p.q.Maker", MAKER, "p.q.Open", OPEN, "a.b.Use", USE), "a.b.Use");
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
        // 'final' is what "no other body can run in its place" is decided from, so a stub TYPE keeps it.
        // ⚠ A final METHOD does not, yet: the isolator copies inherited implementations onto sub-stubs, and a
        // copy below a final declaration does not compile (see IsolationCore.ensureMethodInfo). Pinned here so
        // that whoever adds it meets the reason first.
        String maker = tree.get("p/q/Maker.java");
        assertTrue(maker.contains("public final class Maker"), maker);
        String open = tree.get("p/q/Open.java");
        assertTrue(open.contains("public class Open"), open);
        assertTrue(open.matches("(?s).*public Item sealedMake\\(\\).*"), open);
        assertTrue(open.matches("(?s).*public Item openMake\\(\\).*"), open);
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String HIERARCHY = """
            package p.q;
            public class Hierarchy {
                public static class Leaf { public Item make() { return new Item(); } }
                public static class Base { public Item make() { return new Item(); } }
                public static class Derived extends Base { }
                public static class AnonymousBase { public Item make() { return new Item(); } }
                public static class LocalBase { public Item make() { return new Item(); } }
                public static abstract class Abstract { }
                public static final class Final { }
                public interface Face { }
                static Object anonymous() {
                    Runnable r = () -> { Object o = new AnonymousBase() { }; };
                    return r;
                }
                static Object local() {
                    class Local extends LocalBase { }
                    return new Local();
                }
            }
            """;

    @Language("java")
    private static final String USE_HIERARCHY = """
            package a.b;
            import p.q.Hierarchy;
            public class UseHierarchy {
                public int run(Hierarchy.Leaf leaf, Hierarchy.Base base, Hierarchy.AnonymousBase anonymous) {
                    return leaf.make().weight + base.make().weight + anonymous.make().weight;
                }
            }
            """;

    @DisplayName("visible finality: a class the whole program never extends is final in its stub, on request")
    @Test
    public void classesNeverExtended() throws IOException {
        Map<String, String> sources = Map.of("p.q.Shape", SHAPE, "p.q.Item", ITEM, "p.q.Hierarchy", HIERARCHY,
                "a.b.UseHierarchy", USE_HIERARCHY);
        var parsed = javaInspector.parse(sources,
                new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build());
        Set<TypeInfo> never = ProgramHierarchy.classesNeverExtended(parsed.parseResult().primaryTypes());
        // Derived and the kept type are leaves too; Base has a named subclass, AnonymousBase an anonymous one
        // inside a lambda, LocalBase a local one; abstract, final, and interface types are not candidates
        assertEquals("Derived, Hierarchy, Item, Leaf, UseHierarchy",
                never.stream().map(TypeInfo::simpleName).sorted().collect(Collectors.joining(", ")));

        TypeInfo type = parsed.parseResult().findType("a.b.UseHierarchy");
        isolateClass.withClassesNeverExtended(never);
        IsolateClass.Result r = isolateClass.isolate(type);
        Map<MethodInfo, String> memberSources = new LinkedHashMap<>();
        for (MethodInfo original : r.markers().values()) {
            memberSources.put(original, verbatim(sources.get(original.primaryType().fullyQualifiedName()), original));
        }
        Map<String, String> tree = isolateClass.print(r, memberSources);
        String hierarchy = tree.get("p/q/Hierarchy.java");
        System.out.println(hierarchy);
        assertTrue(hierarchy.contains("static final class Leaf"), hierarchy);
        assertTrue(hierarchy.matches("(?s).*static class Base\\b.*"), hierarchy);
        assertTrue(hierarchy.matches("(?s).*static class AnonymousBase\\b.*"), hierarchy);
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
