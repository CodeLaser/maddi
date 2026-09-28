package io.codelaser.maddi.modification.analyzer.kotlin;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Defensive copies. A Kotlin class that keeps `xs.toList()` has the same meaning as a Java class that keeps
 `List.copyOf(xs)`; the analyzer recognises only the Java spelling. Both Kotlin classes initialize the field in a
 property initializer, so #85 applies to their constructors alike; the type and field verdicts isolate the copy.
 */
public class TestKotlinAnalyzerCollections extends CommonKotlinAnalyzerTest {

    @Language("kotlin")
    private static final String KOTLIN = """
            package k
            class Snap(xs: List<StringBuilder>) { private val items = xs.toList(); fun items(): List<StringBuilder> = items }
            class SnapJ(xs: List<StringBuilder>) { private val items = java.util.List.copyOf(xs); fun items(): List<StringBuilder> = items }
            """;

    @Language("java")
    private static final String JAVA = """
            package j;
            import java.util.List;
            final class Snap { private final List<StringBuilder> items; Snap(List<StringBuilder> xs) { this.items = List.copyOf(xs); } List<StringBuilder> items() { return items; } }
            final class SnapJ { private final List<StringBuilder> items; SnapJ(List<StringBuilder> xs) { this.items = List.copyOf(xs); } List<StringBuilder> items() { return items; } }
            """;

    private static Analyzed a;

    @BeforeAll
    public static void beforeAll() {
        a = analyze(KOTLIN, JAVA);
    }

    /* the Java spelling in Kotlin: same type, field and accessor verdicts as Java */
    @Test
    public void listCopyOf() {
        a.assertSameAsJava("SnapJ", "items");
        assertEquals("type SnapJ: @Immutable(hc=true) @Independent(hc=true)", a.kotlin("SnapJ").lines().findFirst().orElseThrow());
    }

    /* ⛔ maddi#87: `toList()` is not a recognised copy; the field reads as the caller's list */
    @Test
    public void defensiveCopy() {
        assertEquals("""
                type Snap: @Immutable(hc=true) @Independent(hc=true)
                field items: final=true unmodified=true @Independent(hc=true)
                method items: nonModifying=true @Independent(hc=true)""", a.java("Snap", "items"));
        assertEquals("""
                type Snap: @FinalFields @Dependent
                field items: final=true unmodified=true @Dependent
                method items: nonModifying=true @Dependent""", a.kotlin("Snap", "items"));
    }
}
