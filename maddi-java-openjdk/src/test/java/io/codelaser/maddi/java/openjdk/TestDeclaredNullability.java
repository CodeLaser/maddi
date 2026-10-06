package io.codelaser.maddi.java.openjdk;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.type.DeclaredNullability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DeclaredNullability}: what the source declares, per position, read against the {@code @NullMarked} scope.
 * Types render in Kotlin's notation: {@code String?} nullable, {@code String} non-null, {@code String!}
 * unspecified (Kotlin's platform type). The annotation types are declared here, so no annotation jar is needed.
 * <p>
 * Two front-end facts these tests pin: javac normalizes {@code ? extends @Nullable Object} to {@code ?} (the
 * annotation is gone; an unbounded wildcard is parametric, so {@code ?!}); and {@code @Nullable String[] a} speaks
 * about the ELEMENTS, which one {@code ParameterizedType} cannot hold, so the array keeps its scope's state.
 */
public class TestDeclaredNullability extends CommonTest {

    private static final String JS_NULLABLE = """
            package org.jspecify.annotations;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME)
            public @interface Nullable {}
            """;
    private static final String JS_NON_NULL = """
            package org.jspecify.annotations;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME)
            public @interface NonNull {}
            """;
    private static final String JS_NULL_MARKED = """
            package org.jspecify.annotations;
            import java.lang.annotation.*;
            @Target({ElementType.MODULE, ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD,
                     ElementType.CONSTRUCTOR})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NullMarked {}
            """;
    private static final String JS_NULL_UNMARKED = """
            package org.jspecify.annotations;
            import java.lang.annotation.*;
            @Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NullUnmarked {}
            """;
    // a DECLARATION annotation, JSR-305 style: lands on the field/parameter/method, not on the type
    private static final String JX_NULLABLE = """
            package javax.annotation;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Nullable {}
            """;
    private static final String JX_PARAMS = """
            package javax.annotation;
            import java.lang.annotation.*;
            @Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface ParametersAreNonnullByDefault {}
            """;

    // maddi's own, with the attributes that change the meaning (io.codelaser.maddi.annotation, trimmed)
    private static final String MADDI_NOT_NULL = """
            package io.codelaser.maddi.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.CLASS)
            @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
            public @interface NotNull {
                boolean absent() default false;
                boolean contract() default false;
                boolean content() default false;
                boolean implied() default false;
            }
            """;
    private static final String MADDI_NULLABLE = """
            package io.codelaser.maddi.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.CLASS)
            @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
            public @interface Nullable {
                boolean absent() default false;
                boolean contract() default false;
                boolean implied() default true;
            }
            """;

    private Map<String, TypeInfo> parse(Map<String, String> subjects) {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("org.jspecify.annotations.Nullable", JS_NULLABLE);
        src.put("org.jspecify.annotations.NonNull", JS_NON_NULL);
        src.put("org.jspecify.annotations.NullMarked", JS_NULL_MARKED);
        src.put("org.jspecify.annotations.NullUnmarked", JS_NULL_UNMARKED);
        src.put("javax.annotation.Nullable", JX_NULLABLE);
        src.put("javax.annotation.ParametersAreNonnullByDefault", JX_PARAMS);
        src.put("io.codelaser.maddi.annotation.NotNull", MADDI_NOT_NULL);
        src.put("io.codelaser.maddi.annotation.Nullable", MADDI_NULLABLE);
        src.putAll(subjects);
        return scan(false, src).primaryTypes().stream()
                .collect(Collectors.toMap(TypeInfo::fullyQualifiedName, t -> t, (a, b) -> a));
    }

    private static DeclaredNullability declared(Map<String, TypeInfo> types) {
        Map<String, List<AnnotationExpression>> byPackage = types.values().stream()
                .filter(t -> t.typeNature().isPackageInfo())
                .collect(Collectors.toMap(TypeInfo::packageName, TypeInfo::annotations));
        return new DeclaredNullability(p -> byPackage.getOrDefault(p, List.of()));
    }

    // Kotlin notation: '?' nullable, '' non-null, '!' unspecified
    static String k(ParameterizedType pt) {
        if (pt == null) return "-";
        String base;
        if (pt.wildcard() != null && pt.typeInfo() == null && pt.typeParameter() == null) {
            base = "?";
        } else {
            base = pt.typeParameter() != null ? pt.typeParameter().simpleName() : pt.typeInfo().simpleName();
            if (pt.wildcard() != null && pt.wildcard().isExtends()) base = "? extends " + base;
            if (pt.wildcard() != null && pt.wildcard().isSuper()) base = "? super " + base;
        }
        String args = pt.parameters().isEmpty() ? ""
                : pt.parameters().stream().map(TestDeclaredNullability::k).collect(Collectors.joining(", ", "<", ">"));
        String suffix = switch (pt.nullable()) {
            case NULLABLE -> "?";
            case NONNULL -> "";
            case UNSPECIFIED -> "!";
        };
        return base + args + "[]".repeat(pt.arrays()) + suffix;
    }

    private static String fields(TypeInfo t, DeclaredNullability dn) {
        return t.fields().stream().map(f -> f.name() + ": " + k(dn.field(f)))
                .collect(Collectors.joining("\n"));
    }

    private static String method(TypeInfo t, String name, DeclaredNullability dn) {
        MethodInfo m = t.methodStream().filter(mi -> name.equals(mi.name())).findFirst().orElseThrow();
        return m.parameters().stream().map(p -> p.name() + ": " + k(dn.parameter(p)))
                .collect(Collectors.joining(", ", name + "(", "): " + k(dn.returnType(m))));
    }

    private static final String POSITIONS = """
            package a.b;
            import org.jspecify.annotations.NonNull;
            import org.jspecify.annotations.Nullable;
            import java.util.List;
            import java.util.Map;
            public class X<T> {
                String plain;
                @Nullable String f;
                @NonNull String g;
                int i;
                List<@Nullable String> l;
                Map<String, @Nullable List<@Nullable String>> nested;
                List<? extends @Nullable Object> wild;
                List<? extends @Nullable CharSequence> wildCs;
                @Nullable String[] arrElem;
                String @Nullable [] arrItself;
                @javax.annotation.Nullable String[] declArr;
                T tv;
                @Nullable T ntv;
                java.lang.@Nullable String qualified;
                @javax.annotation.Nullable String declField;
                @Nullable String m(@Nullable String p, @javax.annotation.Nullable String q, String r) { return null; }
                @javax.annotation.Nullable String declM() { return null; }
                void v() { }
                String[] arrs(@Nullable String[] a, String @Nullable [] b) { return null; }
            }
            """;

    @DisplayName("unmarked: only what is written; unannotated uses are unspecified, primitives non-null")
    @Test
    public void unmarked() {
        Map<String, TypeInfo> types = parse(Map.of("a.b.X", POSITIONS));
        DeclaredNullability dn = declared(types);
        TypeInfo x = types.get("a.b.X");
        assertEquals("""
                plain: String!
                f: String?
                g: String
                i: int
                l: List<String?>!
                nested: Map<String!, List<String?>?>!
                wild: List<?!>!
                wildCs: List<? extends CharSequence?>!
                arrElem: String[]!
                arrItself: String[]?
                declArr: String[]?
                tv: T!
                ntv: T?
                qualified: String?
                declField: String?""", fields(x, dn));
        assertEquals("m(p: String?, q: String?, r: String!): String?", method(x, "m", dn));
        assertEquals("declM(): String?", method(x, "declM", dn));
        assertEquals("v(): -", method(x, "v", dn));
        // GAP, pinned: on a PARAMETER (and a return), javac's modifier-position type-use annotation is routed onto
        // the type (ScanCompilationUnit.typeOnlyModifierAnnotations), so '@Nullable String[] a' -- elements
        // nullable, JLS 9.7.4 -- arrives exactly like 'String @Nullable [] b' and reads as a nullable ARRAY. A
        // field keeps it among its declaration annotations instead, where it can be told apart (arrElem above).
        assertEquals("arrs(a: String[]?, b: String[]?): String[]!", method(x, "arrs", dn));
    }

    @DisplayName("@NullMarked package: unannotated uses become non-null, type variables and wildcards stay parametric")
    @Test
    public void markedPackage() {
        Map<String, TypeInfo> types = parse(Map.of("a.b.package-info", """
                @org.jspecify.annotations.NullMarked
                package a.b;
                """, "a.b.X", POSITIONS));
        DeclaredNullability dn = declared(types);
        TypeInfo x = types.get("a.b.X");
        assertEquals("""
                plain: String
                f: String?
                g: String
                i: int
                l: List<String?>
                nested: Map<String, List<String?>?>
                wild: List<?!>
                wildCs: List<? extends CharSequence?>
                arrElem: String[]
                arrItself: String[]?
                declArr: String[]?
                tv: T!
                ntv: T?
                qualified: String?
                declField: String?""", fields(x, dn));
        assertEquals("m(p: String?, q: String?, r: String): String?", method(x, "m", dn));
    }

    @DisplayName("the nearest @NullMarked / @NullUnmarked wins: method over type over enclosing type over package")
    @Test
    public void nearestScopeWins() {
        Map<String, TypeInfo> types = parse(Map.of("a.b.package-info", """
                @org.jspecify.annotations.NullMarked
                package a.b;
                """, "a.b.Y", """
                package a.b;
                import org.jspecify.annotations.NullMarked;
                import org.jspecify.annotations.NullUnmarked;
                @NullUnmarked
                public class Y {
                    String s(String p) { return p; }
                    @NullMarked String t(String p) { return p; }
                    @NullMarked
                    static class Inner {
                        String u(String p) { return p; }
                    }
                }
                """));
        DeclaredNullability dn = declared(types);
        TypeInfo y = types.get("a.b.Y");
        assertEquals("s(p: String!): String!", method(y, "s", dn));
        assertEquals("t(p: String): String", method(y, "t", dn));
        TypeInfo inner = y.subTypes().stream().filter(st -> "Inner".equals(st.simpleName())).findFirst().orElseThrow();
        assertEquals("u(p: String): String", method(inner, "u", dn));
    }

    @DisplayName("JSR-305 @ParametersAreNonnullByDefault marks parameters only")
    @Test
    public void parametersNonNullByDefault() {
        Map<String, TypeInfo> types = parse(Map.of("a.b.Z", """
                package a.b;
                @javax.annotation.ParametersAreNonnullByDefault
                public class Z {
                    String f;
                    String m(String p, @javax.annotation.Nullable String q) { return p; }
                }
                """));
        DeclaredNullability dn = declared(types);
        TypeInfo z = types.get("a.b.Z");
        assertEquals("f: String!", fields(z, dn));
        assertEquals("m(p: String, q: String?): String!", method(z, "m", dn));
    }

    @DisplayName("maddi's own: 'absent = true' denies the annotation; '@NotNull(content = true)' is about the content")
    @Test
    public void maddiAnnotations() {
        Map<String, TypeInfo> types = parse(Map.of("a.b.M", """
                package a.b;
                import io.codelaser.maddi.annotation.NotNull;
                import io.codelaser.maddi.annotation.Nullable;
                import java.util.List;
                import java.util.Map;
                public class M {
                    @NotNull String nn;
                    @Nullable String n;
                    @NotNull(absent = true) String notNn;
                    @Nullable(absent = true) String notN;
                    @NotNull(contract = true) String contract;
                    @NotNull(content = true) List<String> elems;
                    @NotNull(content = true) Map<String, @org.jspecify.annotations.Nullable String> mixed;
                    @NotNull(content = true) String[] arr;
                    @Nullable String m(@NotNull String p, @NotNull(content = true) List<String> q) { return null; }
                }
                """));
        DeclaredNullability dn = declared(types);
        TypeInfo m = types.get("a.b.M");
        // content: the reference is the scope's (unmarked: unspecified); an array's content has no slot
        assertEquals("""
                nn: String
                n: String?
                notNn: String!
                notN: String!
                contract: String
                elems: List<String>!
                mixed: Map<String, String?>!
                arr: String[]!""", fields(m, dn));
        assertEquals("m(p: String, q: List<String>!): String?", method(m, "m", dn));
    }
}
