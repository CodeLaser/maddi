package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>{@code .length} on a field whose array-ness arrives through a type variable.</b>
 * <p>
 * {@code visitMemberSelect} recognises an array length by asking the SCOPE's declared type:
 * {@code "length".equals(fieldName) && scope.parameterizedType().arrays() > 0}. For a field inherited from a
 * generic supertype that is the DECLARED type variable, not its substitution — guava's
 * {@code ByteSourceTester extends SourceSinkTester<ByteSource, byte[], ByteSourceFactory>} reads the inherited
 * {@code protected final T expected}, so the scope's declared type is {@code T} and {@code arrays()} is 0.
 * <p>
 * ⛔ The guard then falls through to {@code getOrLoadField}, and javac models {@code .length} as a field of a
 * SYNTHETIC pseudo-class named {@code Array} with no owner — so the loader trips on {@code owner.kind == NIL} and
 * throws {@code UnresolvedSymbolException("Type Array not found")}. A symbol name that appears nowhere in the
 * source, for an expression that is plain Java.
 * <p>
 * ⚠ The same expression on a directly-typed local parses fine, which is why only files reading an INHERITED
 * array-typed generic field fail. Both are below, so the control is in the same test.
 */
public class TestArrayLengthOnInheritedTypeVariable extends CommonTest {

    @Language("java")
    private static final String SRC = """
            package a.b;
            public class Holder {
                static class Base<S, T> {
                    protected final T expected;
                    Base(T expected) { this.expected = expected; }
                }
                static class Sub extends Base<String, byte[]> {
                    Sub(byte[] e) { super(e); }
                    int viaTypeVariable() {
                        return expected.length;
                    }
                }
                int directly(byte[] local) {
                    return local.length;
                }
            }
            """;

    @DisplayName("an inherited array-typed generic field still yields an array length")
    @Test
    public void inheritedTypeVariableBoundToAnArray() {
        TypeInfo holder = scan("a.b.Holder", SRC);
        assertNotNull(holder);
        TypeInfo sub = holder.subTypes().stream().filter(t -> "Sub".equals(t.simpleName())).findFirst()
                .orElseThrow();
        String body = sub.findUniqueMethod("viaTypeVariable", 0).methodBody().toString();
        assertTrue(body.contains("expected.length"), body);
    }
}
