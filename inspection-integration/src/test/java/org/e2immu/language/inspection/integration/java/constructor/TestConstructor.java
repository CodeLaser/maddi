package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestConstructor extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.ArrayList;
            import java.util.List;
            
            /**
             * Basic constructor calling.
             */
            public class Constructor_0 {
            
                public void test() {
                    List<String> strings = new ArrayList<>();
                    List<String> list2 = new ArrayList<>(strings);
                    List<String> list3 = new ArrayList<>(3);
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
            
            import java.util.ArrayList;
            import java.util.List;
            
            /**
             * Generics on a constructor. I have never even encountered this until now (20211223).
             */
            public class Constructor_1 {
            
                static class Parametrized {
                    <T> Parametrized(T t, List<T> list) {
                        list.add(t);
                    }
                }
            
                public void test() {
                    Parametrized p = new <String>Parametrized("3", new ArrayList<>());
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo test = C.findUniqueMethod("test", 0);
        LocalVariableCreation lvc = (LocalVariableCreation) test.methodBody().statements().getFirst();
        ConstructorCall cc = (ConstructorCall) lvc.localVariable().assignmentExpression();
        assertEquals(1, cc.typeArguments().size());
        assertEquals("Type String", cc.typeArguments().getFirst().toString());
        assertEquals("18-31:18-36", cc.source().detailedSources().detail(cc.typeArguments().getFirst()).compact2());

        assertEquals("""
                new <String> Parametrized("3",new ArrayList<>())\
                """, cc.print(javaInspector.runtime().qualificationSimpleNames()).toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import org.junit.jupiter.api.Test;
            
            import java.lang.reflect.ParameterizedType;
            import java.lang.reflect.Type;
            import java.util.List;
            
            import static org.junit.jupiter.api.Assertions.assertEquals;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            /**
             * GSon anonymous constructor trick
             */
            public class Constructor_2 {
            
                @Test
                public void test() {
                    Type type = new TypeToken<List<String>>() {
                    }.type;
                    assertEquals("java.util.List<java.lang.String>", type.toString());
                }
            
                @Test
                public void test2() {
                    assertThrows(RuntimeException.class, () -> {
                        Type type = new TypeToken<List<String>>().type;
                        assertEquals("java.util.List<java.lang.String>", type.toString());
                    });
                }
            
                static class TypeToken<T> {
                    final Type type;
            
                    TypeToken() {
                        this.type = getSuperclassTypeParameter(getClass());
                    }
            
                    static Type getSuperclassTypeParameter(Class<?> subclass) {
                        Type superclass = subclass.getGenericSuperclass();
                        if (superclass instanceof Class) {
                            throw new RuntimeException("Missing type parameter.");
                        }
                        ParameterizedType parameterized = (ParameterizedType) superclass;
                        return parameterized.getActualTypeArguments()[0];
                    }
                }
            }
            """;

    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            
            public class Constructor_3 {
            
                static record MemberValuePair(String clazz, String s) {
                }
            
                public void method(Map<Class<?>, Map<String, Object>> map) {
                    for (Map.Entry<Class<?>, Map<String, Object>> entry : map.entrySet()) {
                        List<Object> list;
                        if (entry.getValue().equals(Map.of())) {
                            list = List.of();
                        } else {
                            list = entry.getValue().entrySet().stream()
                               .map(e -> new MemberValuePair(e.getKey(), e.getValue().toString()))
                               .collect(Collectors.toList());
                        }
                        System.out.println(list);
                    }
                }
            }
            """;

    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            
            public class Constructor_4 {
            
                interface AnnotationExpression {
            
                }
            
                static class AnnotationExpressionImpl implements AnnotationExpression {
                    AnnotationExpressionImpl(String s) {
            
                    }
                }
            
                static class E2 {
            
                    public String immutableAnnotation(Class<?> key, List<AnnotationExpression> list) {
                        return key.getCanonicalName();
                    }
                }
            
                public void method(E2 e2) {
                    List<AnnotationExpression> list = new ArrayList<>();
                    Map<Class<?>, String> map = Map.of(String.class, "String");
                    for(Map.Entry<Class<?>, String> entry: map.entrySet()) {
                        AnnotationExpression expression = new AnnotationExpressionImpl(e2.immutableAnnotation(entry.getKey(), list));
                    }
                }
            }
            """;

    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.List;
            
            /*
            Test because the if(b) statements messes up the variable contexts.
             */
            public class Constructor_5 {
            
                interface X {
                    String get();
            
                    void accept(int j);
                }
            
                // do NOT change the code of this method lightly, severely inspected in the test
                public void method(List<X> xs) {
                    boolean b = xs.isEmpty();
                    if(b) {
                        String s = "abc";
                        xs.add(new X() {
                            @Override
                            public String get() {
                                return "abc " + s.toLowerCase();
                            }
            
                            @Override
                            public void accept(int j) {
                                System.out.println(s.toUpperCase() + ";" + j);
                            }
                        });
                    }
                }
            }
            """;

    @Test
    public void test6() {
        javaInspector.parse(INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashSet;
            import java.util.List;
            
            public class Constructor_6 {
            
                public void method(List<String> strings) {
                    assert new HashSet<>(strings).size() > 1;
                }
            }
            """;

    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_7 {
            
                record R(String... strings) {}
            
                public int method() {
                    R r1 = new R();
                    R r2 = new R("a");
                    R r = new R("a", "b");
                    return r.strings.length;
                }
            }
            """;

    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_8 {
            
                record Pair<K, V>(K k, V v) {}
            
                public int method() {
                   return new Pair<>("3", 3).k.length();
                }
            }
            """;

    @Test
    public void test9() {
        TypeInfo typeInfo = javaInspector.parse(INPUT9);
        TypeInfo pair = typeInfo.findSubType("Pair");
        MethodInfo accessorK = pair.findUniqueMethod("k", 0);
        assertEquals("Type param K", accessorK.returnType().toString());
    }


    @Language("java")
    private static final String INPUT10 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_9 {
            
                interface Analyser {
                    record SharedState(int iteration, String context) {}
                }
            
                static class ParameterAnalyser implements Analyser {
                    record SharedState(int iteration) {}
            
                    public void method() {
                        SharedState sharedState = new SharedState(3);
                    }
                }
            }
            """;

    @Test
    public void test10() {
        javaInspector.parse(INPUT10);
    }

    @Language("java")
    private static final String INPUT11 = """
            public class Constructor_10Scope {
            
                private final int i;
            
                public int getI() {
                    return i;
                }
            
                public Constructor_10Scope(int i) {
                    this.i = i;
                }
            
                public class Sub {
                    final int j;
            
                    Sub(int j) {
                        this.j = j;
                    }
            
                    @Override
                    public String toString() {
                        return "together = " + i + ", " + j;
                    }
                }
            
                Sub getSub(int j) {
                    return this.new Sub(j);
                }
            
                static Sub copy(Constructor_10Scope c) {
                    return c.new Sub(c.i);
                }
            }
            """;

    @Test
    public void test11() {
        TypeInfo typeInfo = javaInspector.parse(INPUT11);
        MethodInfo copy = typeInfo.findUniqueMethod("copy", 1);
        if (copy.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertEquals("c", cc.object().toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT12 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.Map;
            
            
            public class Constructor_11CommonConstructorBlock {
            
                private final Map<String, Integer> map;
            
                {
                    map = new HashMap<String, Integer>();
                }
            
                public Constructor_11CommonConstructorBlock(String s, String t) {
                    map.put(s, 1);
                    map.put(t, 2);
                }
            
                public Integer get(String s) {
                    return map.get(s);
                }
            }
            """;

    @Test
    public void test12() {
        javaInspector.parse(INPUT12);
    }

    @Language("java")
    private static final String INPUT13 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            import java.util.Map;
            
            public class Constructor_12DoubleBrace {
                static Map<String, String> map = new HashMap<>() {{
                    put("1", "a");
                    put("2", "b");
                }};
            
            }
            """;

    @Test
    public void test13() {
        TypeInfo typeInfo = javaInspector.parse(INPUT13);
        FieldInfo map = typeInfo.getFieldByName("map", true);
        if (map.initializer() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("org.e2immu.analyser.resolver.testexample.Constructor_12DoubleBrace.$0", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("org.e2immu.analyser.resolver.testexample.Constructor_12DoubleBrace.$0.<init>()", constructor.fullyQualifiedName());
        } else fail();
    }

    @Language("java")
    private static final String INPUT14 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_13A {
            
                class Inner {
                    int value;
            
                    Inner() {
                        super();
                    }
                }
            }
            """;

    @Test
    public void test14() {
        javaInspector.parse(INPUT14);
    }

    @Language("java")
    private static final String INPUT15 = """
            package org.e2immu.language.inspection.integration.java.importhelper;
            
            import java.util.HashMap;
            import java.util.Map;
            
            class Constructor_13B {
            
                private Constructor_13A a;
                private final Map<String, Object> map = new HashMap<>();
            
                void method() {
                    a.new Inner().value = 3;
                    map.put("key", a.new Inner());
                }
            }
            """;

    @Test
    public void test15() {
        // we should be looking for other types in the same package, Constructor_13A should be visible.
        javaInspector.parse(INPUT15);
    }

    @Language("java")
    private static final String INPUT16 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.io.File;
            import java.util.Arrays;
            import java.util.List;
            
            public class Constructor_14 {
            
            
                static class TempFile {
                    TempFile(File file, boolean b) {
                    }
            
                    private List<TempFile> toTempFiles(File[] files) {
                        // problem: the type produced by Arrays.asList(files).stream() is not good enough
                        // Arrays.stream(files)  == OK
                        // files.stream()        == OK if we make files of type List<File>
                        // ==> this is all about the scope of map() producing sufficient information for the Lambda to work with
                        return Arrays.asList(files).stream().map(f -> new TempFile(f, true)).toList();
                    }
                }
            }
            """;

    @Test
    public void test16() {
        javaInspector.parse(INPUT16);
    }

    @Language("java")
    private static final String INPUT17 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.HashMap;
            
            public class Constructor_15 {
            
                void test() {
                    new HashMap<>();
                }
            }
            """;

    @Test
    public void test17() {
        javaInspector.parse(INPUT17);
    }

    @Language("java")
    private static final String INPUT18 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_16 {
            
                public Constructor_16() {
                    System.out.println("This is the constructor");
                }
            
                public void Constructor_16() {
                    System.out.println("bad practice, but legal!");
                }
            }
            """;

    @Test
    public void test18() {
        javaInspector.parse(INPUT18);
    }

    @Language("java")
    private static final String INPUT19 = """
            package org.e2immu.analyser.resolver.testexample;
            
            import java.util.Set;
            
            public class Constructor_17 {
            
                static class G<T> {
            
                }
            
                static class V<T> {
            
                }
            
                static class BreakCycles<T> {
                    public BreakCycles(ActionComputer<T> actionComputer) {
            
                    }
                }
            
                interface ActionComputer<T> {
                    Action<T> compute(G<T> g, Set<V<T>> cycle);
                }
            
                interface Action<T> {
                    G<T> apply();
                    ActionInfo<T> info();
                }
            
                public interface ActionInfo<T> {
            
                }
            
                void method(V<String> v6, V<String> v9) {
                    BreakCycles<String> bc2 = new BreakCycles<>((g1, cycle) -> {
                        if (cycle.contains(v6)) {
                            return new Action<String>() {
                                @Override
                                public G<String> apply() {
                                    return g1;
                                }
            
                                @Override
                                public ActionInfo<String> info() {
                                    return null;
                                }
                            };
                        }
                        if (cycle.contains(v9)) {
                            return null; // we cannot break it
                        }
                        throw new UnsupportedOperationException();
                    });
                }
            }
            """;

    @Test
    public void test19() {
        javaInspector.parse(INPUT19);
    }

    @Language("java")
    private static final String INPUT20 = """
            package org.e2immu.analyser.resolver.testexample;
            
            public class Constructor_18 {
            
                interface I {
            
                }
                I i;
            
                Constructor_18(String s, I... is) {
                }
            
                Constructor_18(Object... objects) {
                }
            
                static Constructor_18 method(String s) {
                    return new Constructor_18(s);
                }
            }
            """;

    @Test
    public void test20() {
        javaInspector.parse(INPUT20);
    }


    @Language("java")
    public static final String INPUT21 = """
            package a.b;
            class X {
                record Pair<F, G>(F f, G g) {
                    Pair(F f, G g) {
                        this.f = f;
                        this.g = g;
                        System.out.println(f + " " + g);
                    }
                    Pair(F f, G g, String msg) {
                        this.f = f;
                        this.g = g;
                        System.out.println(msg+": " + f + " " + g);
                    }
                }
                record R<F, G>(Pair<F, G> pair) {
                    public R {
                        assert pair != null;
                    }
                }
                static <X, Y> R<Y, X> method(R<X, Y> r) {
                    return new R<>(new Pair<>(r.pair.g, r.pair.f));
                }
            }
            """;

    @DisplayName("parameter-less record constructor")
    @Test
    public void test21() {
        TypeInfo X = javaInspector.parse(INPUT21, new JavaInspectorImpl.ParseOptionsBuilder()
                .setDetailedSources(true).build());

        TypeInfo pair = X.findSubType("Pair");
        assertEquals(2, pair.constructors().size());
        MethodInfo cPair = pair.findConstructor(2);
        assertFalse(cPair.isSynthetic());
        assertEquals("4-9:4-12", cPair.source().detailedSources().detail(cPair.name()).compact2());

        TypeInfo R = X.findSubType("R");
        assertEquals(1, R.constructors().size());

        MethodInfo cc = R.findConstructor(1);
        assertTrue(cc.isCompactConstructor());
        assertFalse(cc.isSynthetic());
        assertEquals("16-16:16-16", cc.source().detailedSources().detail(cc.name()).compact2());
    }
}
