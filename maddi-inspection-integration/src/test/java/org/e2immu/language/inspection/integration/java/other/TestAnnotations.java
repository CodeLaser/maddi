package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotations extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.annotation.ImmutableContainer;
            
            public class Annotations_0 {
            
                @ImmutableContainer("false")
                public static boolean method() {
                    return false;
                }
            
            }
            """;

    @Test
    public void test1() {
        javaInspector.parse(INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            
            @Resources({
                    @Resource(name = "xx", lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = "zz", type = java.lang.Integer.class)
            })
            public class Annotations_1 {
                static final String XX = "xx";
            }
            """;

    @Test
    public void test2() {
        TypeInfo t = javaInspector.parse(INPUT2);
        assertEquals(1, t.annotations().size());
        AnnotationExpression ae = t.annotations().getFirst();
        assertEquals("org.e2immu.language.inspection.integration.java.importhelper.a.Resources",
                ae.typeInfo().fullyQualifiedName());
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
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;
            
            @Resources({ })
            public class Annotations_1 {
                static final String XX = "xx";
            }
            """;

    @Test
    public void test2b() {
        TypeInfo t = javaInspector.parse(INPUT2b);
        assertEquals(1, t.annotations().size());
        AnnotationExpression ae = t.annotations().getFirst();
        assertEquals("org.e2immu.language.inspection.integration.java.importhelper.a.Resources",
                ae.typeInfo().fullyQualifiedName());
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
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            
            import static org.e2immu.analyser.resolver.testexample.Annotations_2.XX;
            
            @Resources({
                    @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
                    @Resource(name = Annotations_2.ZZ, type = Integer.class)
            })
            public class Annotations_2 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            import static org.e2immu.analyser.resolver.testexample.Annotations_3.XX;
            
            @Resource(name = XX, lookup = Annotations_3.ZZ, type = java.util.TreeMap.class)
            public class Annotations_3 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
            import static org.e2immu.analyser.resolver.testexample.Annotations_4.XX;
            
            @Resource(name = XX, lookup = Annotations_4.ZZ, authenticationType = Resource.AuthenticationType.CONTAINER)
            public class Annotations_4 {
                static final String XX = "xx";
                static final String ZZ = "zz";
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }

    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.lang.annotation.Retention;
            import java.lang.annotation.Target;
            
            import static java.lang.annotation.ElementType.FIELD;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;
            
            @Target({FIELD})
            @Retention(RUNTIME)
            public @interface Annotations_5 {
            
                Class<?> value();
            
                String extra() default "!";
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }


    @Language("java")
    private static final String INPUT7 = """
            import java.io.FileInputStream;
            import java.io.IOException;
            import java.io.ObjectInputStream;
            import java.io.Serializable;
            
            public class X {
            
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
        TypeInfo X = javaInspector.parse(INPUT7);
        MethodInfo u = X.findUniqueMethod("u", 2);
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
            import java.io.BufferedReader;
            import java.io.IOException;
            import java.nio.charset.StandardCharsets;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.util.Properties;
            
            public class AnalyzerProfile {
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
        TypeInfo ti = javaInspector.parse(INPUT8);
        MethodInfo mi = ti.findUniqueMethod("getAnalysisDataDir", 1);
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

    @Test
    public void test9() {
        TypeInfo resource = javaInspector.parse(INPUT9);
        TypeInfo at = resource.findSubType("AuthenticationType");
        assertTrue(at.typeNature().isEnum());
        assertEquals(2, at.fields().size());
        FieldInfo c = at.getFieldByName("CONTAINER", true);
        assertTrue(c.isSynthetic());
        assertTrue(c.isStatic());
        assertTrue(c.isFinal());

        AnnotationExpression retention = resource.annotations().stream()
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
            public @interface Resource {
                int value();
                public char character();
            }
            """;

    @Test
    public void test10() {
        TypeInfo resource = javaInspector.parse(INPUT10);
        {
            AnnotationExpression retention = resource.annotations().stream()
                    .filter(ae -> "Retention".equals(ae.typeInfo().simpleName()))
                    .findFirst().orElseThrow();
            String valueForRetention = retention.keyValuePairs().stream().filter(kv -> kv.key().equals("value"))
                    .map(kv -> kv.value().toString()).findFirst().orElseThrow();
            assertEquals("RetentionPolicy.RUNTIME", valueForRetention);
        }
        {
            AnnotationExpression target = resource.annotations().stream()
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
            
            public @interface Resource {
                @Target({TYPE, FIELD, METHOD})
                int value();
            
                @Retention(RUNTIME)
                public char character();
            }
            """;

    @Test
    public void test11() {
        TypeInfo resource = javaInspector.parse(INPUT11, JavaInspectorImpl.DETAILED_SOURCES);
        {
            MethodInfo value = resource.findUniqueMethod("value", 0);
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
            MethodInfo character = resource.findUniqueMethod("character", 0);
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
            class X {
            
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
        TypeInfo X = javaInspector.parse(INPUT12, JavaInspectorImpl.DETAILED_SOURCES);
        TypeInfo enclosingAnnot = X.findSubType("EnclosingAnnotation");
        assertEquals("java.lang.annotation.Annotation",
                enclosingAnnot.interfacesImplemented().getFirst().typeInfo().fullyQualifiedName());
    }

    @Language("java")
    private static final String INPUT13 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            class X {
              static void assertArrayEquals(boolean [] expected, boolean @Nullable ... actual) {
              }
            }
            """;

    @Test
    public void test13() {
        TypeInfo X = javaInspector.parse(INPUT13, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo assertArrayEquals = X.findUniqueMethod("assertArrayEquals", 2);
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
            class X {
              static void assertArrayEquals(boolean @Nullable [] expected, boolean @Nullable [] actual) {
              }
            }
            """;

    @Test
    public void test14() {
        TypeInfo X = javaInspector.parse(INPUT14, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo assertArrayEquals = X.findUniqueMethod("assertArrayEquals", 2);
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
            abstract class X {
              abstract boolean @Nullable [] findBooleans();
              void assertArrayEquals() {
                  boolean @Nullable [] expected = findBooleans();
              }
            }
            """;

    @Test
    public void test15() {
        TypeInfo X = javaInspector.parse(INPUT15, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo assertArrayEquals = X.findUniqueMethod("assertArrayEquals", 0);
        MethodInfo findBooleans = X.findUniqueMethod("findBooleans", 0);
        assertEquals("Type boolean[]", findBooleans.returnType().toString());
        LocalVariableCreation lvc = (LocalVariableCreation) assertArrayEquals.methodBody().statements().getFirst();
        assertEquals("Type boolean[]", lvc.localVariable().parameterizedType().toString());
    }

    @Language("java")
    private static final String INPUT16 = """
            package a.b;
            import org.e2immu.annotation.Nullable;
            abstract class X {
              abstract String @Nullable [] findStrings();
              void assertArrayEquals() {
                  String @Nullable [] expected = findStrings();
              }
            }
            """;

    @Test
    public void test16() {
        TypeInfo X = javaInspector.parse(INPUT16, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo assertArrayEquals = X.findUniqueMethod("assertArrayEquals", 0);
        MethodInfo findStrings = X.findUniqueMethod("findStrings", 0);
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
            abstract class X {
               <T> boolean addAll(@NotNull @Modified @Independent(hcParameters = {1}) Collection<? super T> c, @NotModified T... elements);
            }
            """;

    @Test
    public void test17() {
        TypeInfo X = javaInspector.parse(INPUT17, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo method = X.findUniqueMethod("addAll", 2);
        ParameterInfo p0 = method.parameters().getFirst();
        assertEquals(3, p0.annotations().size());
    }
}
