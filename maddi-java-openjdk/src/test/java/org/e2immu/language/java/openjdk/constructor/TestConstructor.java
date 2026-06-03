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

package org.e2immu.language.java.openjdk.constructor;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestConstructor extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.List;
            
            /**
             * Basic constructor calling.
             */
            public class C {
            
                public void test() {
                    List<String> strings = new ArrayList<>();
                    List<String> list2 = new ArrayList<>(strings);
                    List<String> list3 = new ArrayList<>(3);
                }
            }
            """;

    @Test
    public void test1() {
        scan("a.b.C", INPUT1);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.List;
            
            /**
             * Generics on a constructor. I have never even encountered this until now (20211223).
             */
            public class C {
            
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
        TypeInfo C = scan("a.b.C", INPUT2);
        MethodInfo test = C.findUniqueMethod("test", 0);
        LocalVariableCreation lvc = (LocalVariableCreation) test.methodBody().statements().getFirst();
        ConstructorCall cc = (ConstructorCall) lvc.localVariable().assignmentExpression();
        assertEquals(1, cc.typeArguments().size());
        assertEquals("Type String", cc.typeArguments().getFirst().toString());
        assertEquals("18-31:18-36", cc.source().detailedSources().detail(cc.typeArguments().getFirst()).compact2());

        assertEquals("""
                new <String> Parametrized("3",new ArrayList<>())\
                """, cc.print(runtime.qualificationSimpleNames()).toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            import org.junit.jupiter.api.Test;
            
            import java.lang.reflect.ParameterizedType;
            import java.lang.reflect.Type;
            import java.util.List;
            
            import static org.junit.jupiter.api.Assertions.assertEquals;
            import static org.junit.jupiter.api.Assertions.assertThrows;
            
            /**
             * GSon anonymous constructor trick
             */
            public class C {
            
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
        scan("a.b.C", INPUT3);
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;
            
            public class C {
            
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
        scan("a.b.C", INPUT4);
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            
            public class C {
            
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
        scan("a.b.C", INPUT5);
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.util.List;
            
            /*
            Test because the if(b) statements messes up the variable contexts.
             */
            public class C {
            
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
        scan("a.b.C", INPUT6);
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.HashSet;
            import java.util.List;
            
            public class C {
            
                public void method(List<String> strings) {
                    assert new HashSet<>(strings).size() > 1;
                }
            }
            """;

    @Test
    public void test7() {
        scan("a.b.C", INPUT7);
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            
            public class C {
            
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
        scan("a.b.C", INPUT8);
    }

    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            
            public class C {
            
                record Pair<K, V>(K k, V v) {}
            
                public int method() {
                   return new Pair<>("3", 3).k.length();
                }
            }
            """;

    @Test
    public void test9() {
        TypeInfo typeInfo = scan("a.b.C", INPUT9);
        TypeInfo pair = typeInfo.findSubType("Pair");
        MethodInfo accessorK = pair.findUniqueMethod("k", 0);
        assertEquals("Type param K", accessorK.returnType().toString());
    }


    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            
            public class C {
            
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
        scan("a.b.C", INPUT10);
    }

    @Language("java")
    private static final String INPUT11 = """
            public class C {
            
                private final int i;
            
                public int getI() {
                    return i;
                }
            
                public C(int i) {
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
            
                static Sub copy(C c) {
                    return c.new Sub(c.i);
                }
            }
            """;

    @Test
    public void test11() {
        TypeInfo typeInfo = scan("a.b.C", INPUT11);
        MethodInfo copy = typeInfo.findUniqueMethod("copy", 1);
        if (copy.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertEquals("c", cc.object().toString());
        } else fail();
        MethodInfo getSub = typeInfo.findUniqueMethod("getSub", 1);
        if (getSub.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertEquals("this", cc.object().toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT12 = """
            package a.b;
            
            import java.util.HashMap;
            import java.util.Map;
            
            
            public class C {
            
                private final Map<String, Integer> map;
            
                {
                    map = new HashMap<String, Integer>();
                }
            
                public C(String s, String t) {
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
        scan("a.b.C", INPUT12);
    }

    @Language("java")
    private static final String INPUT13 = """
            package a.b;
            
            import java.util.HashMap;
            import java.util.Map;
            
            public class C {
                static Map<String, String> map = new HashMap<>() {{
                    put("1", "a");
                    put("2", "b");
                }};
            
            }
            """;

    @Test
    public void test13() {
        TypeInfo typeInfo = scan("a.b.C", INPUT13);
        FieldInfo map = typeInfo.getFieldByName("map", true);
        if (map.initializer() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("a.b.C.$0.<init>()", constructor.fullyQualifiedName());
        } else fail();
    }

    @Language("java")
    private static final String INPUT14 = """
            package a.b;
            
            public class C {
            
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
        scan("a.b.C", INPUT14);
    }

    @Language("java")
    private static final String INPUT15 = """
            package a.b;
            
            import java.util.HashMap;
            import java.util.Map;
            
            class C {
            
                private A a;
                private final Map<String, Object> map = new HashMap<>();
            
                void method() {
                    a.new Inner().value = 3;
                    map.put("key", a.new Inner());
                }
            }
            """;

    @Language("java")
    private static final String ABA = """
            package a.b;
            class A {
                class Inner {
                    int value;
                    Inner() {
                        super();
                    }
                }
            }
            """;

    @Test
    public void test15() {
        // we should be looking for other types in the same package, A should be visible.
        scan(false, "a.b.A", ABA, "a.b.C", INPUT15);
    }

    @Language("java")
    private static final String INPUT16 = """
            package a.b;
            
            import java.io.File;
            import java.util.Arrays;
            import java.util.List;
            
            public class C {
            
            
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
        scan("a.b.C", INPUT16);
    }

    @Language("java")
    private static final String INPUT17 = """
            package a.b;
            
            import java.util.HashMap;
            
            public class C {
            
                void test() {
                    new HashMap<>();
                }
            }
            """;

    @Test
    public void test17() {
        scan("a.b.C", INPUT17);
    }

    @Language("java")
    private static final String INPUT18 = """
            package a.b;
            
            public class C {
            
                public C() {
                    System.out.println("This is the constructor");
                }
            
                public void C() {
                    System.out.println("bad practice, but legal!");
                }
            }
            """;

    @Test
    public void test18() {
        scan("a.b.C", INPUT18);
    }

    @Language("java")
    private static final String INPUT19 = """
            package a.b;
            
            import java.util.Set;
            
            public class C {
            
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
        scan("a.b.C", INPUT19);
    }

    @Language("java")
    private static final String INPUT20 = """
            package a.b;
            
            public class C {
            
                interface I {
            
                }
                I i;
            
                C(String s, I... is) {
                }
            
                C(Object... objects) {
                }
            
                static C method(String s) {
                    return new C(s);
                }
            }
            """;

    @Test
    public void test20() {
        scan("a.b.C", INPUT20);
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
                        this(f, g);
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
        TypeInfo X = scan("a.b.X", INPUT21);

        TypeInfo pair = X.findSubType("Pair");
        assertEquals(2, pair.constructors().size());
        MethodInfo cPair = pair.findConstructor(2);
        assertFalse(cPair.isSynthetic());
        assertEquals("4-9:4-12", cPair.source().detailedSources().detail(cPair.name()).compact2());

        MethodInfo cPair3 = pair.findConstructor(3);
        assertFalse(cPair3.isSynthetic());
        assertEquals("9-9:9-12", cPair3.source().detailedSources().detail(cPair3.name()).compact2());

        TypeInfo R = X.findSubType("R");
        assertEquals(1, R.constructors().size());

        MethodInfo cc = R.findConstructor(1);
        assertTrue(cc.isCompactConstructor());
        assertFalse(cc.isSynthetic());
        assertEquals("15-16:15-16", cc.source().detailedSources().detail(cc.name()).compact2());
    }
}
