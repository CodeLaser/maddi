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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class isolates that <b>parse</b> and do not <b>compile</b>.
 * <p>
 * Every case here was found by isolating a real fernflower type and compiling the result by hand, after
 * {@code debug.isolateClass} was added to the DSL. What made them invisible until then is not that maddi's front
 * end is weaker than javac — it <i>is</i> javac, driven through {@code task.parse()} then {@code task.analyze()},
 * so attribution runs and these are javac's own messages. It is that the existing isolate tests re-parse with
 * <b>default</b> {@link JavaInspector.ParseOptions}, where {@code failFast} is false; javac's errors are then
 * logged at INFO and the parse is allowed to succeed. And they do not show up as
 * {@code parseExceptions()} either, so a test asserting on those sees nothing.
 * <p>
 * Hence {@code setFailFast(true)} below, and nothing more elaborate: it is the switch that turns any javac error
 * into a {@code CompilationProblems}, which names each error with its file, line and column. Running javac
 * separately would only recompute a verdict maddi already has.
 * <p>
 * The fixtures are reductions of the originals, small enough to read:
 * <ul>
 *   <li>{@code SwitchPatternHelper} statically imports {@code ClassNameConstants.JAVA_UTIL_OBJECTS} — an
 *       <b>interface</b> field, hence implicitly static;</li>
 *   <li>{@code ExprProcessor} calls {@code pop()} on an {@code ExpressionStack extends ListStack<Exprent>};</li>
 *   <li>{@code ClassWriter} switches over its own private {@code enum MType}.</li>
 * </ul>
 */
public class TestIsolateClass4Compiles {
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

    /**
     * Isolates {@code fqn} out of {@code sources}, taking each kept member's verbatim text straight out of the
     * fixture: the isolate's whole point is that the bodies are copied, so the test has to supply them the way a
     * real driver does, from the source it parsed.
     */
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

    /**
     * Re-parses the emitted tree with fail-fast on, so that any javac error in it is a thrown
     * {@code CompilationProblems} rather than a log line. The type-set check stays: a unit whose symbols cannot be
     * resolved is DROPPED with a warning, and dropping is not an error, so it would otherwise pass silently.
     */
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

    /** The member's own lines, cut out of the compilation unit by its recorded source span. */
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

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String CONSTANTS = """
            package p.q;
            public interface Constants {
                String OBJECTS = "java/util/Objects";
            }
            """;

    @Language("java")
    private static final String USES_STATIC_IMPORT = """
            package a.b;
            import static p.q.Constants.OBJECTS;
            public class UsesStaticImport {
                public static boolean isObjects(String name) {
                    return OBJECTS.equals(name);
                }
            }
            """;

    @DisplayName("an implicitly-static interface field, statically imported, stays static")
    @Test
    public void staticallyImportedInterfaceField() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.Constants", CONSTANTS,
                "a.b.UsesStaticImport", USES_STATIC_IMPORT), "a.b.UsesStaticImport");
        // An interface field is implicitly static and maddi does not always report it so. Taken at its word, the
        // field is neither recorded as a static import nor stubbed static: it landed as a plain instance field ON
        // THE ISOLATED TYPE ("public String JAVA_UTIL_OBJECTS;"), and the verbatim body then read it from a static
        // method -- "non-static variable JAVA_UTIL_OBJECTS cannot be referenced from a static context".
        //
        // FIXED, and the fix was not in IsolateClass. This case used to be JVM-STATE DEPENDENT -- reproducing on
        // its own, passing in a warm runtime -- because the two inspection paths disagreed about the field:
        // ClassSymbolScanner reads javac's symbol flags, where the implicit static is present, while
        // ScanCompilationUnit read the written JCModifiers, where it is not. Whichever had run for that interface
        // in that JVM decided the answer. ScanCompilationUnit.field now applies JLS 9.3 itself.
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String LIST_STACK = """
            package p.q;
            import java.util.ArrayList;
            public class ListStack<T> extends ArrayList<T> {
                public T pop() {
                    return remove(size() - 1);
                }
            }
            """;

    @Language("java")
    private static final String ITEM = """
            package p.q;
            public class Item {
                public int weight;
            }
            """;

    @Language("java")
    private static final String ITEM_STACK = """
            package p.q;
            public class ItemStack extends ListStack<Item> {
            }
            """;

    @Language("java")
    private static final String USES_GENERIC_SUPERTYPE = """
            package a.b;
            import p.q.Item;
            import p.q.ItemStack;
            public class UsesGenericSupertype {
                public int total(ItemStack stack) {
                    Item first = stack.pop();
                    Item second = stack.pop();
                    return first.weight + second.weight;
                }
            }
            """;

    @DisplayName("a method inherited from a generic supertype keeps its type argument")
    @Test
    public void inheritedGenericMethod() throws IOException {
        Map<String, String> tree = isolate(Map.of("p.q.ListStack", LIST_STACK, "p.q.Item", ITEM,
                "p.q.ItemStack", ITEM_STACK, "a.b.UsesGenericSupertype", USES_GENERIC_SUPERTYPE),
                "a.b.UsesGenericSupertype");
        // 'pop()' is declared on ListStack<T> and returns T; through 'ItemStack extends ListStack<Item>' that is
        // an Item. Stubbing it on the SCOPE type with an erased return gave "public Object pop()" on ItemStack,
        // and the verbatim 'Item first = stack.pop();' then failed with
        // "incompatible types: Object cannot be converted to Item".
        assertCompiles(tree);
    }

    // ---------------------------------------------------------------------------------------------------------

    @Language("java")
    private static final String USES_ENUM = """
            package a.b;
            public class UsesEnum {
                private enum Kind {CLASS, FIELD, METHOD}

                private static String describe(Kind kind) {
                    switch (kind) {
                        case CLASS:
                            return "class";
                        case FIELD:
                            return "field";
                        default:
                            return "method";
                    }
                }

                public String describeClass() {
                    return describe(Kind.CLASS);
                }
            }
            """;

    @DisplayName("an enum is stubbed as an enum, so a switch over it compiles")
    @Test
    public void switchOverEnum() throws IOException {
        Map<String, String> tree = isolate(Map.of("a.b.UsesEnum", USES_ENUM), "a.b.UsesEnum");
        // The stub came out as "public static class Kind { public static Kind CLASS; ... }" -- a class with static
        // fields, which is enough for 'Kind.CLASS' and not enough for 'case CLASS:':
        // "pattern or enum constant required".
        assertCompiles(tree);
    }
}
