package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CstSupertypes}, the printers' shared supertype search: here, because this module's tests have the front end.
 * maddi#158: a supertype's type argument written {@code ? extends B} keeps its wildcard when {@code B} is substituted;
 * guava's {@code ClassToInstanceMap<B> extends Map<Class<? extends B>, B>}.
 */
public class TestCstSupertypes extends CommonJavaToCSharp {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Map;
            class X {
                interface CI<B> extends Map<Class<? extends B>, B> { }
                interface Holder<N> extends CI<N> { }
                interface Cmp<T> extends Map<T, java.util.Comparator<? super T>> { }

                CI<Number> number;
                CI<? super Number> superNumber;
                Holder<String> holder;
                Cmp<? super Integer> cmp;
            }
            """;

    private TypeInfo x() {
        return javaInspector.parse(Map.of("a.b.X", INPUT), new JavaInspector.ParseOptions.Builder().build())
                .parseResult().primaryTypes().stream().filter(t -> "a.b.X".equals(t.fullyQualifiedName()))
                .findFirst().orElseThrow();
    }

    private static String asMap(TypeInfo x, String field) {
        ParameterizedType type = x.getFieldByName(field, true).type();
        TypeInfo mapType = CstSupertypes.firstAsSeen(type.typeInfo(), t -> "java.util.Map".equals(t.fullyQualifiedName()))
                .typeInfo();
        return CstSupertypes.asSeen(type, mapType).detailedString();
    }

    @Test
    public void wildcardOfASubstitutedTypeParameterIsKept() {
        TypeInfo x = x();
        assertEquals("java.util.Map<Class<? extends Number>,Number>", asMap(x, "number"));
        assertEquals("java.util.Map<Class<? extends String>,String>", asMap(x, "holder"),
                "two levels: Holder<N> as CI<N>, then CI's Map");
        // '? extends' of '? super Number': only '?' is left
        assertEquals("java.util.Map<Class<?>,? super Number>", asMap(x, "superNumber"));
        assertEquals("java.util.Map<? super Integer,java.util.Comparator<? super Integer>>", asMap(x, "cmp"));
    }

    @Test
    public void typeItselfInItsOwnTypeParameters() {
        TypeInfo x = x();
        TypeInfo holder = x.findSubType("Holder");
        TypeInfo map = CstSupertypes.firstAsSeen(holder, t -> "java.util.Map".equals(t.fullyQualifiedName())).typeInfo();
        assertEquals("java.util.Map<Class<? extends N>,N>", CstSupertypes.asSeen(holder, map).detailedString());
    }
}
