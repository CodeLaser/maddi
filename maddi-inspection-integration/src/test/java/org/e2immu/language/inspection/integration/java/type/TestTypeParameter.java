package org.e2immu.language.inspection.integration.java.type;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTypeParameter extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            
            class X {
               static class ArrayList<T> extends java.util.ArrayList<T> {
                   // no need for anything here
               }
               static class I {
                   int k;
               }
            
               static void set(ArrayList<I[]> iArrayList, int i, int j, int k) {
                   iArrayList.get(i)[j].k = k;
               }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo set = typeInfo.findUniqueMethod("set", 4);
        Expression expression = set.methodBody().lastStatement().expression();
        assertEquals("iArrayList.get(i)[j].k=k", expression.toString());
        if (expression instanceof Assignment a) {
            assertEquals("a.b.X.I.k#`12-8`[a.b.X.set(a.b.X.ArrayList<a.b.X.I[]>,int,int,int):2:j]",
                    a.variableTarget().fullyQualifiedName());
        }
    }


    @Language("java")
    public static final String INPUT2 = """
            package b;
            
            import java.util.List;
            
            class C {
                interface D { }
                interface A {
                    interface B<T> { }
                    List<B<D>> get();
                }
            
                void m(A a) {
                    List<A.B<D>> x = a.get();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2, JavaInspectorImpl.DETAILED_SOURCES);
        MethodInfo m = typeInfo.findUniqueMethod("m", 1);
        LocalVariableCreation lvc = (LocalVariableCreation) m.methodBody().lastStatement();
        ParameterizedType list = lvc.localVariable().parameterizedType();
        assertEquals("java.util.List<b.C.A.B<b.C.D>>", list.fullyQualifiedName());
        ParameterizedType pt = list.parameters().getFirst();
        assertEquals("13-14:13-19", lvc.source().detailedSources().detail(pt).compact2());

        //noinspection ALL
        List<DetailedSources.Builder.TypeInfoSource> tis = (List<DetailedSources.Builder.TypeInfoSource>)
                lvc.source().detailedSources().associatedObject(pt.typeInfo());
        assertEquals("13-14:13-16", lvc.source().detailedSources().detail(pt.typeInfo()).compact2());
        assertEquals(1, tis.size());
        assertEquals("TypeInfoSource[typeInfo=b.C.A, source=@13:14-13:14]", tis.getFirst().toString());
        // Note that there is no b.C are, the qualification is A.
    }


    @Language("java")
    public static final String INPUT3 = """
            package a.b;
            class A {
                public record B<Z extends A>(Z t) {}
                public record BB<Z extends a.b.A>(Z t) {}
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3, JavaInspectorImpl.DETAILED_SOURCES);
        {
            TypeInfo clazz = typeInfo.findSubType("B");
            TypeParameter tp = clazz.typeParameters().getFirst();
            assertEquals("Z=TP#0 in B [Type a.b.A]", tp.toStringWithTypeBounds());
            assertEquals("3-21:3-31", tp.source().compact2());
            ParameterizedType bound = tp.typeBounds().getFirst();
            assertEquals("3-31:3-31", tp.source().detailedSources().detail(bound).compact2());
        }
        {
            TypeInfo clazz = typeInfo.findSubType("BB");
            TypeParameter tp = clazz.typeParameters().getFirst();
            assertEquals("Z=TP#0 in BB [Type a.b.A]", tp.toStringWithTypeBounds());
            assertEquals("4-22:4-36", tp.source().compact2());
            ParameterizedType bound = tp.typeBounds().getFirst();
            assertEquals("4-32:4-36", tp.source().detailedSources().detail(bound).compact2());
            assertEquals("4-32:4-34", tp.source().detailedSources().detail(bound.typeInfo().packageName()).compact2());
        }
    }

    @Language("java")
    public static final String INPUT4 = """
            package a.b;
            class X {
                interface SofPolicy { }
                abstract class ZA { }
                abstract class ZB { }
                abstract class ZC { }
                abstract class ZD { }
                abstract class ZE { }
                abstract class ZF { }
                abstract class ZG<A extends ZA> { }
            
                interface ZH<
                			E extends ZG<A>,
                			A extends ZA,
                			R extends ZB,
                			F extends ZC,
                			Y extends ZD,
                			H extends ZE>
                		extends SofPolicy {
                }
                class ZI<E extends ZG<A>, A extends ZA> { }
                abstract class ZJ<
                            T extends ZI<E, A>,
                			E extends ZG<A>,
                			A extends ZA,
                			R extends ZB,
                			F extends ZC,
                			Y extends ZD,
                			H extends ZE>
                	extends ZH<E, A, R, F, Y, H> {
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);

        for (TypeInfo sub : typeInfo.subTypes()) {
            for (TypeParameter tp : sub.typeParameters()) {
                assertTrue(tp.typeBoundsAreSet());
            }
        }
        TypeInfo request = typeInfo.findSubType("ZJ");
        TypeParameter tp1 = request.typeParameters().get(1);
        assertEquals("E", tp1.simpleName());
        assertEquals("[Type ZG<A extends a.b.X.ZA>]", tp1.typeBounds().toString());
    }

    @Language("java")
    public static final String INPUT5 = """
            package a.b;
            import org.e2immu.annotation.Container;
            import org.e2immu.annotation.Independent;
            class X {
              class Class$<@Independent @Container T> {
            
              }
            }
            """;

    @Test
    public void test5() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5);
        TypeInfo clazz = typeInfo.findSubType("Class$");
        TypeParameter tp = clazz.typeParameters().getFirst();
        assertTrue(tp.hasBeenInspected());
        assertEquals(2, tp.annotations().size());
        assertEquals("""
                package a.b;
                import org.e2immu.annotation.Container;
                import org.e2immu.annotation.Independent;
                class X { class Class$<@Independent @Container T> { } }
                """, javaInspector.print2(typeInfo));
    }

    @Language("java")
    public static final String INPUT5b = """
            package a.b;
            import org.e2immu.annotation.Container;
            import org.e2immu.annotation.Independent;
            class X {
              class Class$<@Independent @Container(comment = X.COMMENT) T> {
            
              }
              private static final String COMMENT = "comment";
            }
            """;

    @Test
    public void test5b() {
        TypeInfo typeInfo = javaInspector.parse(INPUT5b);
        TypeInfo clazz = typeInfo.findSubType("Class$");
        TypeParameter tp = clazz.typeParameters().getFirst();
        assertTrue(tp.hasBeenInspected());
        assertEquals(2, tp.annotations().size());
        assertEquals("""
                package a.b;
                import org.e2immu.annotation.Container;
                import org.e2immu.annotation.Independent;
                class X { private static final String COMMENT = "comment"; class Class$<@Independent@Container(comment = COMMENT) T> { } }
                """, javaInspector.print2(typeInfo));
    }


    @Language("java")
    public static final String INPUT6 = """
            package a.b;
            import org.springframework.lang.Nullable;
            import java.util.AbstractMap;
            import java.util.concurrent.ConcurrentMap;
            import java.util.concurrent.locks.ReentrantLock;
            class X {
                static class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
                    protected final class Segment extends ReentrantLock {
                        public <T> @Nullable T doTask(final int hash, final @Nullable Object key, final Task<T> task) {
            
                        }
                    }
            
                    private abstract class Task<T> {
                        // not really relevant for this test
                    }
                }
            }
            """;

    @Test
    public void test6() {
        TypeInfo typeInfo = javaInspector.parse(INPUT6);
        TypeInfo map = typeInfo.findSubType("ConcurrentReferenceHashMap");
        TypeInfo task = map.findSubType("Task");
        assertEquals(1, task.typeParameters().size());
        assertEquals("T=TP#0 in Task []", task.typeParameters().getFirst().toStringWithTypeBounds());
    }
}
