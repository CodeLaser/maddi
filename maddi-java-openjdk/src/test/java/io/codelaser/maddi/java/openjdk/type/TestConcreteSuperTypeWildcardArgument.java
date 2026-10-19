package io.codelaser.maddi.java.openjdk.type;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * maddi#159: {@code concreteSuperType} puts a WILDCARD argument into the type arguments nested in a supertype's
 * argument, as it does a plain one. {@code Stream<T> extends BaseStream<T, Stream<T>>}, seen from {@code Stream<?>},
 * was {@code BaseStream<?, Stream<T>>}: Stream's own {@code T} where it means nothing. Each field's type below is a
 * receiver; the supertype it is seen as is {@code B}.
 */
public class TestConcreteSuperTypeWildcardArgument extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Comparator;
            import java.util.List;
            class X {
                static class B<P, Q> { }
                static class S<T> extends B<T, S<T>> { }
                static class Ext<T> extends B<T, List<? extends T>> { }
                static class Sup<T> extends B<T, Comparator<? super T>> { }

                S<String> plain;
                S<?> unbound;
                S<? extends CharSequence> extendsArg;
                Ext<?> extUnbound;
                Ext<? extends Number> extExtends;
                Ext<? super Number> extSuper;
                Sup<? super Number> supSuper;
                Sup<? extends Number> supExtends;
            }
            """;

    private static String seenAsB(TypeInfo x, String field) {
        ParameterizedType receiver = x.getFieldByName(field, true).type();
        ParameterizedType b = x.findSubType("B").asParameterizedType();
        return receiver.concreteSuperType(b).detailedString();
    }

    @Test
    public void test() {
        TypeInfo x = scan("a.b.X", INPUT);
        assertEquals("a.b.X.B<String,a.b.X.S<String>>", seenAsB(x, "plain"));
        assertEquals("a.b.X.B<?,a.b.X.S<?>>", seenAsB(x, "unbound"));
        assertEquals("a.b.X.B<? extends CharSequence,a.b.X.S<? extends CharSequence>>", seenAsB(x, "extendsArg"));
        assertEquals("a.b.X.B<?,java.util.List<?>>", seenAsB(x, "extUnbound"));
        assertEquals("a.b.X.B<? extends Number,java.util.List<? extends Number>>", seenAsB(x, "extExtends"));
        // '? extends' of '? super Number': Java has no such type argument, only '?'
        assertEquals("a.b.X.B<? super Number,java.util.List<?>>", seenAsB(x, "extSuper"));
        assertEquals("a.b.X.B<? super Number,java.util.Comparator<? super Number>>", seenAsB(x, "supSuper"));
        assertEquals("a.b.X.B<? extends Number,java.util.Comparator<?>>", seenAsB(x, "supExtends"));
    }
}
