package org.e2immu.analyzer.modification.common;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Known maddi-parser limitation, surfaced by {@code IsolateMethod}'s {@code @Override} supertype.
 * <p>
 * When a top-level type refers to a <em>sibling</em> top-level type's nested members by qualified name (here
 * {@code X.A} and {@code X.B} inside {@code X_super}), and the compilation unit is in the <em>unnamed</em>
 * package, the parse stubs the {@code X} prefix and then throws {@code Duplicating type X} once the real
 * {@code X} is parsed from the same unit. It needs more than one such reference and the unnamed package;
 * {@code package a.b}, a single reference, or javac all handle it. {@code javac} compiles this source.
 * <p>
 * {@code IsolateMethod} hits exactly this: the abstract supertype it generates for an {@code @Override} method
 * refers to the frame's nested types by qualified name, and the frame is in the unnamed package
 * ({@code targetPackage ""}). The output compiles with javac but cannot be reparsed here.
 * <p>
 * Remove {@link Disabled} once the parser resolves a forward, same-unit qualified reference to the type being
 * defined instead of stubbing the prefix.
 */
@Disabled("maddi parser limitation: sibling-qualified references to a nested type duplicate the prefix type")
public class TestSiblingQualifiedNestedReference extends CommonIsolateMethodTest {

    @Language("java")
    public static final String INPUT = """
            abstract class X_super {
                abstract X.A get(X.B b);
            }
            public class X extends X_super {
                class A { }
                class B { }
                @Override
                public A get(B b) { return null; }
            }
            """;

    @DisplayName("sibling type references another's nested members by qualified name (unnamed package)")
    @Test
    public void test() {
        // currently throws 'Duplicating type X' during the parse
        TypeInfo xSuper = javaInspector.parse("X_super", INPUT);
        assertNotNull(xSuper);
        MethodInfo get = xSuper.findUniqueMethod("get", 1);
        assertEquals("A", get.returnType().typeInfo().simpleName());
    }
}
