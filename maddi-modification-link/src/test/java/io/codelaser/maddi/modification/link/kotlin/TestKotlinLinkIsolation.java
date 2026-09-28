package io.codelaser.maddi.modification.link.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/*
 ⛔ maddi#83: state from one parse leaks into the link results of a later, independent parse in the same JVM. Linking
 a static call (`listOf(a)`, i.e. CollectionsKt__CollectionsJVMKt.listOf) is enough; afterwards a singleton's field
 `Registry.INSTANCE.all` prints with the earlier call's scope as its owner, on the Kotlin and the Java side. Which
 wrong name it gets depends on what ran first in the JVM, so this asserts only that the right one is absent; it
 fails when #83 is fixed, and should then assert `Registry.INSTANCE.all`.
 */
public class TestKotlinLinkIsolation extends CommonKotlinLinkTest {

    @Language("kotlin")
    private static final String FIRST = """
            package k
            class X { fun listOfIt(a: StringBuilder): List<StringBuilder> = listOf(a) }
            """;

    @Language("kotlin")
    private static final String SECOND = """
            package k
            object Registry { val all = ArrayList<StringBuilder>(); fun register(s: StringBuilder) { all.add(s) } }
            class X { fun register(s: StringBuilder) { Registry.register(s) } }
            """;

    @Language("java")
    private static final String SECOND_JAVA = """
            package j;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                static final class Registry {
                    static final Registry INSTANCE = new Registry();
                    final List<StringBuilder> all = new ArrayList<>();
                    void register(StringBuilder s) { all.add(s); }
                }
                void register(StringBuilder s) { Registry.INSTANCE.register(s); }
            }
            """;

    @Test
    public void staticScopeLeaksAcrossParses() {
        link(FIRST, "package j;\nclass X { }\n").kotlinLinks("listOfIt");
        Parsed second = link(SECOND, SECOND_JAVA);
        String kotlin = second.kotlinLinks("register");
        String java = second.javaLinks("register");
        assertFalse(kotlin.contains("Registry.INSTANCE"), kotlin);
        assertFalse(java.contains("Registry.INSTANCE"), java);
    }
}
