/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.openjdk.other;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotations extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class MoveDown_0<K, V> {
            
                private TestMap<K, V> map = null;
            
                interface TestMap<K, V> {
                    @SuppressWarnings("g")
                    V get(K k);
            
                    @SuppressWarnings("h")
                    void put(K k, V v);
                }
            
                interface Remap<V> {
                    @SuppressWarnings("i")
                    V apply(@SuppressWarnings("v") V v1, @SuppressWarnings("w")  V v2);
                }
            
                void same1(K key, V value, Remap<V> remap) {
                    V oldValue = map.get(key);
                    if (oldValue == null) {
                        map.put(key, value);
                    } else {
                        V newValue = remap.apply(oldValue, value);
                        map.put(key, newValue);
                    }
                }
            
                void same2(K key, V value, Remap<V> remap) {
                    V oldValue = map.get(key);
                    V newValue;
                    if (oldValue == null) {
                        newValue = value;
                    } else {
                        newValue = remap.apply(oldValue, value);
                    }
                    map.put(key, newValue);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = scan(Map.of("a.b.MoveDown_0", INPUT1), List.of()).getFirst();
        assertEquals(2, typeInfo.typeParameters().size());
        TypeInfo remap = typeInfo.findSubType("Remap");
        MethodInfo apply = remap.findUniqueMethod("apply", 2);
        assertEquals("@SuppressWarnings(\"i\")", apply.annotations().getFirst().toString());
        ParameterInfo pi0 = apply.parameters().getFirst();
        assertEquals("@SuppressWarnings(\"v\")", pi0.annotations().getFirst().toString());
    }

    @Language("java")
    private static final String RESOURCES = """
            package a;
            import java.lang.annotation.*;
            @Documented
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.PACKAGE})
            public @interface Resources {
                Resource[] value();
            }
            """;

    @Language("java")
    private static final String RESOURCE = """
            package a;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Resource {
                String name() default "";
            
                String lookup() default "";
            
                Class<?> type() default Object.class;
            
                AuthenticationType authenticationType() default AuthenticationType.CONTAINER;
            
                boolean shareable() default true;
            
                String mappedName() default "";
            
                String description() default "";
            
                enum AuthenticationType {
                    CONTAINER,
                    APPLICATION;
            
                    AuthenticationType() {
                    }
                }
            }
            """;

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import a.Resources;
            import a.Resource;
            
            @Resources({
                    @Resource(name = "xx", lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = "zz", type = java.lang.Integer.class)
            })
            public class C {
                static final String XX = "xx";
            }
            """;

    private static TypeInfo find(String simpleName, List<TypeInfo> types) {
        return types.stream().filter(t -> simpleName.equals(t.simpleName())).findFirst().orElseThrow();
    }

    @Test
    public void test2() {
        List<TypeInfo> types = scan(Map.of("a.Resource", RESOURCE, "a.Resources", RESOURCES, "a.b.C", INPUT2),
                List.of());

        TypeInfo Resource = find("Resource", types);
        assertEquals(2, Resource.annotations().size());
        assertEquals("@Target({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})",
                Resource.annotations().getFirst().toString());
        assertEquals("@Retention(RetentionPolicy.RUNTIME)", Resource.annotations().getLast().toString());

        TypeInfo Resources = find("Resources", types);
        assertEquals(3, Resources.annotations().size());
        assertEquals("@Documented", Resources.annotations().getFirst().toString());
        assertEquals("@Target({ElementType.TYPE,ElementType.PACKAGE})",
                Resources.annotations().getLast().toString());

        TypeInfo C = find("C", types);
        assertEquals(1, C.annotations().size());
        AnnotationExpression ae = C.annotations().getFirst();
        assertEquals("a.Resources", ae.typeInfo().fullyQualifiedName());
        assertEquals(1, ae.keyValuePairs().size());
        AnnotationExpression.KV kv0 = ae.keyValuePairs().getFirst();
        assertEquals("value", kv0.key());
        assertTrue(kv0.keyIsDefault());
        Expression e = kv0.value();
        if (e instanceof ArrayInitializer ai) {
            if (ai.expressions().getFirst() instanceof AnnotationExpression ae1) {
                assertEquals(3, ae1.keyValuePairs().size());
            } else fail();
        } else fail();
    }


    @Language("java")
    private static final String INPUT2b = """
            package a.b;
            
            import a.Resources;
            
            @Resources({ })
            public class C {
                static final String XX = "xx";
            }
            """;

    @Test
    public void test2b() {
        List<TypeInfo> types = scan(Map.of("a.b.C", INPUT2b, "a.Resources", RESOURCES,
                "a.Resource", RESOURCE), List.of());
        TypeInfo C = find("C", types);
        assertEquals(1, C.annotations().size());
        AnnotationExpression ae = C.annotations().getFirst();
        assertEquals("a.Resources", ae.typeInfo().fullyQualifiedName());
        assertEquals(1, ae.keyValuePairs().size());
        AnnotationExpression.KV kv0 = ae.keyValuePairs().getFirst();
        assertEquals("value", kv0.key());
        assertTrue(kv0.keyIsDefault());
        Expression e = kv0.value();
        if (e instanceof ArrayInitializer ai) {
            assertTrue(ai.expressions().isEmpty());
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            import a.Resources;
            import a.Resource;
            
            import static a.b.C.XX;
            
            @Resources({
                    @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = C.ZZ, type = Integer.class)
            })
            public class C {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test3() {
        List<TypeInfo> types = scan(Map.of("a.b.C", INPUT3, "a.Resources", RESOURCES,
                "a.Resource", RESOURCE), List.of());
        TypeInfo C = find("C", types);
        assertEquals("""
                @Resources({\
                @Resource(name=C.XX,lookup="yy",type=TreeMap.class),\
                @Resource(name=C.ZZ,type=Integer.class)\
                })\
                """, C.annotations().getFirst().toString());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            
            import a.Resource;
            import static a.b.C.XX;
            
            @Resource(name = XX, lookup = C.ZZ, type = java.util.TreeMap.class)
            public class C {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test4() {
        List<TypeInfo> types = scan(Map.of("a.b.C", INPUT4, "a.Resources", RESOURCES,
                "a.Resource", RESOURCE), List.of());
        TypeInfo C = find("C", types);
        assertEquals("@Resource(name=C.XX,lookup=C.ZZ,type=TreeMap.class)",
                C.annotations().getFirst().toString());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import a.Resource;
            import static a.b.C.XX;
            
            @Resource(name = XX, lookup = C.ZZ, authenticationType = Resource.AuthenticationType.CONTAINER)
            public class C {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test5() {
        List<TypeInfo> types = scan(Map.of("a.b.C", INPUT5, "a.Resources", RESOURCES,
                "a.Resource", RESOURCE), List.of());
        TypeInfo C = find("C", types);
        AnnotationExpression ae = C.annotations().getFirst();
        assertEquals("@Resource(name=C.XX,lookup=C.ZZ,authenticationType=AuthenticationType.CONTAINER)",
                ae.toString());
        AnnotationExpression.KV kv2 = ae.keyValuePairs().stream().filter(kv -> "authenticationType".equals(kv.key()))
                .findFirst().orElseThrow();
        assertEquals("KVI[key=authenticationType, value=AuthenticationType.CONTAINER]", kv2.toString());
        if (kv2.value() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            assertEquals("a.Resource.AuthenticationType", fr.fieldInfo().owner().toString());
            assertEquals("AuthenticationType", fr.scope().toString());
        }
    }

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            
            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            
            @Target({FIELD})
            @Retention(RUNTIME)
            public @interface C {
            
                Class<?> value();
            
                String extra() default "!";
            }
            """;

    @Test
    public void test6() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT6), List.of()).getFirst();
        assertNotNull(C);
    }


    @Language("java")
    private static final String INPUT7 = """
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.io.ObjectInputStream;
            import java.io.Serializable;
            
            public class C {
            
                public static <T extends Serializable> T u(final String pFilename, final Class<T> pClass) {
                    try {
                        final FileInputStream myFileInputStream = new FileInputStream(pFilename);
                        final ObjectInputStream myObjectInputStream = new ObjectInputStream(myFileInputStream);
                        Object myObject = myObjectInputStream.readObject();
                        myObjectInputStream.close();
                        myFileInputStream.close();
                        if (pClass.isInstance(myObject)) {
                            @SuppressWarnings("unchecked")
                            final T mObject = (T) myObject;
                            return mObject;
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("error " + e);
                    }
                    return null;
                }
            }
            """;

    @Test
    public void test7() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT7), List.of()).getFirst();
        MethodInfo u = C.findUniqueMethod("u", 2);
        Statement ifElse = u.methodBody().statements().getFirst().block().statements().get(5);
        Statement s = ifElse.block().statements().getFirst();
        assertEquals(1, s.annotations().size());
        AnnotationExpression ae = s.annotations().getFirst();
        assertEquals(SuppressWarnings.class.getCanonicalName(), ae.typeInfo().fullyQualifiedName());
        AnnotationExpression.KV kv = ae.keyValuePairs().getFirst();
        assertEquals("value", kv.key());
        assertEquals("unchecked", ((StringConstant) kv.value()).constant());
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.io.BufferedReader;
            import java.io.IOException;
            import java.nio.charset.StandardCharsets;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.util.Properties;
            
            public class C {
              private static String getAnalysisDataDir(Path propFile) {
                Properties prop = new Properties();
                try (BufferedReader reader = Files.newBufferedReader(propFile, StandardCharsets.UTF_8)) {
                  prop.load(reader);
                  return prop.getProperty("analysis.data.dir", "");
                } catch (@SuppressWarnings("unused") IOException e) {
                  return "";
                }
              }
            }
            """;

    @Test
    public void test8() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT8), List.of()).getFirst();
        MethodInfo mi = C.findUniqueMethod("getAnalysisDataDir", 1);
        TryStatement ts = (TryStatement) mi.methodBody().statements().get(1);
        TryStatement.CatchClause cc = ts.catchClauses().getFirst();
        assertEquals(1, cc.annotations().size());
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface C {
                String name() default "";
            
                String lookup() default "";
            
                Class<?> type() default Object.class;
            
                AuthenticationType authenticationType() default AuthenticationType.CONTAINER;
            
                boolean shareable() default true;
            
                String mappedName() default "";
            
                String description() default "";
            
                enum AuthenticationType {
                    CONTAINER,
                    APPLICATION;
            
                    AuthenticationType() {
                    }
                }
            }
            """;

    @Test
    public void test9() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT9), List.of()).getFirst();
        TypeInfo at = C.findSubType("AuthenticationType");
        assertTrue(at.typeNature().isEnum());
        assertEquals(2, at.fields().size());
        FieldInfo c = at.getFieldByName("CONTAINER", true);
        //assertTrue(c.isSynthetic()); TODO is there a reason why they should be synthetic? they're visible
        assertTrue(c.isStatic());
        assertTrue(c.isFinal());

        AnnotationExpression retention = C.annotations().stream()
                .filter(ae -> "Retention".equals(ae.typeInfo().simpleName()))
                .findFirst().orElseThrow();
        String valueForRetention = retention.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                .map(kv -> kv.value().toString()).findFirst().orElseThrow();
        assertEquals("RetentionPolicy.RUNTIME", valueForRetention);
    }


    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            import static java.lang.annotation.ElementType.*;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            @Target({TYPE, FIELD, METHOD})
            @Retention(RUNTIME)
            public @interface C {
                int value();
                public char character();
            }
            """;

    @Test
    public void test10() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT10), List.of()).getFirst();
        {
            AnnotationExpression retention = C.annotations().stream()
                    .filter(ae -> "Retention".equals(ae.typeInfo().simpleName()))
                    .findFirst().orElseThrow();
            String valueForRetention = retention.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                    .map(kv -> kv.value().toString()).findFirst().orElseThrow();
            assertEquals("RetentionPolicy.RUNTIME", valueForRetention);
        }
        {
            AnnotationExpression target = C.annotations().stream()
                    .filter(ae -> "Target".equals(ae.typeInfo().simpleName()))
                    .findFirst().orElseThrow();
            String valueForTarget = target.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                    .map(kv -> kv.value().toString()).findFirst().orElseThrow();
            assertEquals("{ElementType.TYPE,ElementType.FIELD,ElementType.METHOD}", valueForTarget);
        }
    }


    @Language("java")
    private static final String INPUT11 = """
            package a.b;
            
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            import static java.lang.annotation.ElementType.*;
            
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            
            public @interface C {
                @Target({TYPE, FIELD, METHOD})
                int value();
            
                @Retention(RUNTIME)
                public char character();
            }
            """;

    @Test
    public void test11() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT11), List.of()).getFirst();
        {
            MethodInfo value = C.findUniqueMethod("value", 0);
            assertEquals("12-5:13-16", value.source().compact2());
            assertEquals("13-9:13-13", value.source().detailedSources().detail(value.name()).compact2());

            AnnotationExpression target = value.annotations().stream()
                    .filter(ae -> "Target".equals(ae.typeInfo().simpleName()))
                    .findFirst().orElseThrow();
            String valueForTarget = target.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                    .map(kv -> kv.value().toString()).findFirst().orElseThrow();
            assertEquals("{ElementType.TYPE,ElementType.FIELD,ElementType.METHOD}", valueForTarget);
        }
        {
            MethodInfo character = C.findUniqueMethod("character", 0);
            assertEquals("15-5:16-28", character.source().compact2());
            assertEquals("16-17:16-25", character.source().detailedSources().detail(character.name()).compact2());

            AnnotationExpression retention = character.annotations().stream()
                    .filter(ae -> "Retention".equals(ae.typeInfo().simpleName()))
                    .findFirst().orElseThrow();
            String valueForRetention = retention.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                    .map(kv -> kv.value().toString()).findFirst().orElseThrow();
            assertEquals("RetentionPolicy.RUNTIME", valueForRetention);
        }
    }


    @Language("java")
    private static final String INPUT12 = """
            package a.b;
            import org.springframework.core.annotation.AliasFor;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            class C {
            
                @Target(ElementType.ANNOTATION_TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                public @interface NestedAnnotation {
                	String name() default "";
                }
            
                @Retention(RetentionPolicy.RUNTIME)
                public @interface EnclosingAnnotation {
                	@AliasFor("nested2")
                	NestedAnnotation nested1() default @NestedAnnotation;
            
                	@AliasFor("nested1")
                	NestedAnnotation nested2() default @NestedAnnotation;
                }
            
                @EnclosingAnnotation(nested2 = @NestedAnnotation)
                public class AnnotatedComponent {
                }
            }
            """;

    @Test
    public void test12() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT12), List.of()).getFirst();
        TypeInfo enclosingAnnot = C.findSubType("EnclosingAnnotation");
        assertEquals("java.lang.annotation.Annotation",
                enclosingAnnot.interfacesImplemented().getFirst().typeInfo().fullyQualifiedName());
    }

    @Language("java")
    private static final String INPUT13 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            class C {
              static void assertArrayEquals(boolean [] expected, boolean @Nullable ... actual) {
              }
            }
            """;

    @Test
    public void test13() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT13), List.of()).getFirst();
        MethodInfo assertArrayEquals = C.findUniqueMethod("assertArrayEquals", 2);
        ParameterInfo p0 = assertArrayEquals.parameters().getFirst();
        assertEquals("Type boolean[]", p0.parameterizedType().toString());
        ParameterInfo p1 = assertArrayEquals.parameters().getLast();
        assertEquals(1, p1.annotations().size());
        assertEquals("@Nullable", p1.annotations().getFirst().toString());
        assertTrue(p1.isVarArgs());
        assertEquals("Type boolean[]", p1.parameterizedType().toString());
    }

    @Language("java")
    private static final String INPUT14 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            class C {
              static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual) {
              }
            }
            """;

    @Test
    public void test14() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT14), List.of()).getFirst();
        MethodInfo assertArrayEquals = C.findUniqueMethod("assertArrayEquals", 2);
        for (ParameterInfo p1 : assertArrayEquals.parameters()) {
            assertEquals(1, p1.annotations().size());
            assertFalse(p1.isVarArgs());
            assertEquals("Type boolean[]", p1.parameterizedType().toString());
        }
    }

    @Language("java")
    private static final String INPUT15 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            abstract class C {
              abstract boolean @Nullable [] findBooleans();
              void assertArrayEquals() {
                  boolean @Nullable [] expected = findBooleans();
              }
            }
            """;

    @Test
    public void test15() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT15), List.of()).getFirst();
        MethodInfo assertArrayEquals = C.findUniqueMethod("assertArrayEquals", 0);
        MethodInfo findBooleans = C.findUniqueMethod("findBooleans", 0);
        assertEquals("Type boolean[]", findBooleans.returnType().toString());
        LocalVariableCreation lvc = (LocalVariableCreation) assertArrayEquals.methodBody().statements().getFirst();
        assertEquals("Type boolean[]", lvc.localVariable().parameterizedType().toString());
    }

    @Language("java")
    private static final String INPUT16 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            abstract class C {
              abstract String @Nullable [] findStrings();
              void assertArrayEquals() {
                  String @Nullable [] expected = findStrings();
              }
            }
            """;

    @Test
    public void test16() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT16), List.of()).getFirst();
        MethodInfo assertArrayEquals = C.findUniqueMethod("assertArrayEquals", 0);
        MethodInfo findStrings = C.findUniqueMethod("findStrings", 0);
        assertEquals("Type String[]", findStrings.returnType().toString());
        LocalVariableCreation lvc = (LocalVariableCreation) assertArrayEquals.methodBody().statements().getFirst();
        assertEquals("Type String[]", lvc.localVariable().parameterizedType().toString());
    }


    @Language("java")
    private static final String INPUT17 = """
            package a.b;
            import org.e2immu.annotation.Independent
            ;import org.e2immu.annotation.Modified;
            import org.e2immu.annotation.NotModified;
            import org.e2immu.annotation.NotNull;
            import java.util.Collection;
            abstract class C {
               <T> boolean addAll(@NotNull @Modified @Independent(hcParameters = {1}) Collection<? super T> c, @NotModified T... elements);
            }
            """;

    @Test
    public void test17() {
        TypeInfo C = scan(Map.of("a.b.C", INPUT17), List.of()).getFirst();
        MethodInfo method = C.findUniqueMethod("addAll", 2);
        ParameterInfo p0 = method.parameters().getFirst();
        assertEquals(3, p0.annotations().size());
    }
}
