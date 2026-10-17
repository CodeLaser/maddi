package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeLaser/maddi#144: the type-use annotations written on an anonymous class creation's type are kept, on the
 * anonymous type's supertype and on the constructor call's type, as they are on a named class's
 * {@code extends}/{@code implements} clause. guava's {@code Iterators.partitionImpl} is the shape.
 */
public class TestTypeUseAnnotationOnAnonymousSupertype extends CommonTest {

    private static final String NULLABLE = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE_USE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Nullable {}
            """;

    private static final String SUBJECT = """
            package a.b;
            import ann.Nullable;
            import java.util.Iterator;
            import java.util.List;

            public class X {
                public abstract static class It<E> implements Iterator<E> {
                    @Override public boolean hasNext() { return false; }
                }

                public static class Named<T> extends It<List<@Nullable T>> {
                    @Override public List<@Nullable T> next() { return null; }
                }

                // a class supertype, as guava's UnmodifiableIterator
                public static <T> It<List<@Nullable T>> ofClass() {
                    return new It<List<@Nullable T>>() {
                        @Override public List<@Nullable T> next() { return null; }
                    };
                }

                // an interface supertype
                public static <T> Iterator<List<@Nullable T>> ofInterface() {
                    return new Iterator<List<@Nullable T>>() {
                        @Override public boolean hasNext() { return false; }
                        @Override public List<@Nullable T> next() { return null; }
                    };
                }

                // a diamond: nothing written, the inferred type stays
                public static It<List<String>> diamond() {
                    return new It<>() {
                        @Override public List<String> next() { return null; }
                    };
                }
            }
            """;

    private TypeInfo parse() {
        return scan(false, Map.of("ann.Nullable", NULLABLE, "a.b.X", SUBJECT))
                .primaryTypes().stream()
                .filter(t -> "a.b.X".equals(t.fullyQualifiedName())).findFirst().orElseThrow();
    }

    private static ConstructorCall returned(TypeInfo x, String methodName) {
        MethodInfo method = x.findUniqueMethod(methodName, 0);
        ReturnStatement rs = (ReturnStatement) method.methodBody().statements().getFirst();
        return (ConstructorCall) rs.expression();
    }

    /** The T inside {@code Super<List<T>>}. */
    private static ParameterizedType innermost(ParameterizedType superType) {
        return superType.parameters().getFirst().parameters().getFirst();
    }

    private static void assertNullableT(ParameterizedType superType, String what) {
        ParameterizedType t = innermost(superType);
        assertEquals(1, t.annotations().size(), what + ": " + superType);
        assertEquals("@Nullable", t.annotations().getFirst().toString(), what);
    }

    @DisplayName("baseline: a named class's extends clause keeps the type-use annotation")
    @Test
    public void named() {
        assertNullableT(parse().findSubType("Named").parentClass(), "Named's parent");
    }

    @DisplayName("an anonymous class extending a class keeps the type-use annotation")
    @Test
    public void anonymousClassSupertype() {
        ConstructorCall cc = returned(parse(), "ofClass");
        assertNotNull(cc.anonymousClass());
        assertNullableT(cc.anonymousClass().parentClass(), "anonymous parent");
        assertNullableT(cc.parameterizedType(), "constructor call type");
    }

    @DisplayName("an anonymous class implementing an interface keeps the type-use annotation")
    @Test
    public void anonymousInterfaceSupertype() {
        ConstructorCall cc = returned(parse(), "ofInterface");
        TypeInfo anonymous = cc.anonymousClass();
        assertTrue(anonymous.parentClass().isJavaLangObject());
        assertEquals(1, anonymous.interfacesImplemented().size());
        assertNullableT(anonymous.interfacesImplemented().getFirst(), "anonymous interface");
        assertNullableT(cc.parameterizedType(), "constructor call type");
    }

    @DisplayName("a diamond anonymous class keeps the inferred type arguments")
    @Test
    public void anonymousDiamond() {
        ConstructorCall cc = returned(parse(), "diamond");
        ParameterizedType parent = cc.anonymousClass().parentClass();
        assertEquals(1, parent.parameters().size(), "inferred, not raw: " + parent);
        assertEquals("java.util.List<String>",
                parent.parameters().getFirst().fullyQualifiedName().replace("java.lang.", ""));
    }
}
