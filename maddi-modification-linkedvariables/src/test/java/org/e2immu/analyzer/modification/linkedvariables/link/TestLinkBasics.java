package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestLinkBasics extends CommonTest {


    @Language("java")
    private static final String INPUT0 = """
            package a.b;
            import java.util.List;
            class X<T> {
                List<T> list;
                T get(int pos) {
                    return list.get(pos);
                }
            }
            """;

    // see also TestLinkArrays
    @DisplayName("get from a list")
    @Test
    public void test0() {
        TypeInfo X = javaInspector.parse(INPUT0);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo get = X.findUniqueMethod("get", 1);
        VariableData vd = VariableDataImpl.of(get);
        assertNotNull(vd);

        Statement s0 = get.methodBody().lastStatement();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viList0 = vd0.variableInfo(get.fullyQualifiedName());
        assertEquals("*-4-0:_synthetic_list, -1-:_synthetic_list[pos], *M-4-0M:list",
                viList0.linkedVariables().toString());
    }


    @Language("java")
    private static final String INPUT00 = """
            package a.b;
            import java.util.List;
            class X<T> {
                List<T> list;
                T remove(int pos) {
                    return list.remove(pos*2);
                }
            }
            """;

    @DisplayName("get element from a list, non-getter")
    @Test
    public void test00() {
        TypeInfo X = javaInspector.parse(INPUT00);
        List<Info> analysisOrder = prepWork(X);
        MethodInfo get = X.findUniqueMethod("remove", 1);
        assertNull(get.getSetField().field()); // this method is not a getter!

        analyzer.go(analysisOrder);

        Statement s0 = get.methodBody().lastStatement();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viList0 = vd0.variableInfo(get.fullyQualifiedName());
        assertEquals("*-4-0:list", viList0.linkedVariables().toString());
    }

    @Language("java")
    private static final String INPUT0B = """
            package a.b;
            import java.util.List;
            class X {
                static class M {
                    int i;
                    void setI(int i) { this.i = i; }
                }
                List<M> list;
                M get(int pos) {
                    return list.get(pos);
                }
            }
            """;

    @DisplayName("get from a list, modifying")
    @Test
    public void test0B() {
        TypeInfo X = javaInspector.parse(INPUT0B);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo get = X.findUniqueMethod("get", 1);
        VariableData vd = VariableDataImpl.of(get);
        assertNotNull(vd);

        Statement s0 = get.methodBody().lastStatement();
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viList0 = vd0.variableInfo(get.fullyQualifiedName());
        assertEquals("*M-2-0M|*-?:_synthetic_list, -1-:_synthetic_list[pos], *M-2-0M|*-0.?:list",
                viList0.linkedVariables().toString());
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Set;
            class X {
                static boolean setAdd(Set<String> set, String s) {
                    return set.add(s);
                }
            }
            """;

    @DisplayName("add to set, immutable type parameter")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo setAdd = X.findUniqueMethod("setAdd", 2);
        VariableData vd = VariableDataImpl.of(setAdd);
        assertNotNull(vd);
        ParameterInfo set = setAdd.parameters().getFirst();
        Statement s0 = setAdd.methodBody().statements().getFirst();
        MethodCall mc = (MethodCall) s0.expression();
        assertTrue(mc.methodInfo().analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isFalse());

        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo viSet0 = vd0.variableInfo(set);
        assertTrue(viSet0.isModified());
        assertEquals(LinkedVariablesImpl.EMPTY, viSet0.linkedVariables());

        // this should have reached the method
        assertTrue(set.analysis().getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isFalse());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t) {
                    list.add(t);
                }
            }
            """;

    @DisplayName("list.add(t)")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 2);
        Statement s0 = listAdd.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        assertNotNull(vd0);
        ParameterInfo listAdd0 = listAdd.parameters().getFirst();
        VariableInfo vi0 = vd0.variableInfo(listAdd0);
        assertEquals("0-4-*:t", vi0.linkedVariables().toString());
        assertEquals(vi0.linkedVariables(),
                listAdd0.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER, LinkedVariablesImpl.EMPTY));
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.util.Collections;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t1, T t2) {
                    Collections.addAll(list, t1, t2);
                }
            }
            """;

    @DisplayName("Collections.addAll(list, t1, t2)")
    @Test
    public void test3() {
        TypeInfo collections = javaInspector.compiledTypesManager().get(Collections.class);
        MethodInfo addAll = collections.findUniqueMethod("addAll", 2);
        ParameterInfo addAll0 = addAll.parameters().getFirst();
        assertTrue(addAll0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE).isFalse());
        Value.Independent i0 = addAll0.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.DEPENDENT);
        assertEquals(1, i0.linkToParametersReturnValue().size());

        ParameterInfo addAll1 = addAll.parameters().get(1);
        assertSame(TRUE, addAll1.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));

        TypeInfo X = javaInspector.parse(INPUT3);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        Statement s0 = listAdd.methodBody().statements().getFirst();
        VariableData vd0 = VariableDataImpl.of(s0);
        assertNotNull(vd0);

        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t1 = listAdd.parameters().get(1);
        ParameterInfo t2 = listAdd.parameters().get(2);

        VariableInfo vi0list = vd0.variableInfo(list);
        assertEquals("0-4-*:t1, 0-4-*:t2", vi0list.linkedVariables().toString());
        assertEquals(vi0list.linkedVariables(),
                list.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER, LinkedVariablesImpl.EMPTY));
        assertTrue(vi0list.isModified());

        VariableInfo vi0t1 = vd0.variableInfo(t1);
        assertEquals("*-4-0:list", vi0t1.linkedVariables().toString());
        assertEquals(vi0t1.linkedVariables(),
                t1.analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER, LinkedVariablesImpl.EMPTY));
        assertFalse(vi0t1.isModified());

        assertSame(FALSE, list.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
        assertTrue(list.isModified());
        assertSame(TRUE, t1.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
        assertFalse(t1.isModified());
        assertSame(TRUE, t2.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
        assertFalse(t2.isModified());
    }

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class X {
                final static class M { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
                static class ME { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static <T> List<T> copy(List<T> list) {
                    return new ArrayList<>(list);
                }
                static List<M> copyM(List<M> list) {
                    return new ArrayList<>(list);
                }
                static List<ME> copyME(List<ME> list) {
                    return new ArrayList<>(list);
                }
                static List<String> copyS(List<String> list) {
                    return new ArrayList<>(list);
                }
            }
            """;

    @DisplayName("return new ArrayList<>(list)")
    @Test
    public void test4() {
        testArrayListConstructor();
        TypeInfo X = javaInspector.parse(INPUT4);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo methodInfo = X.findUniqueMethod("copy", 1);
        assertEquals(" - 0=T, 1=List", methodInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.NO_VALUE).detailedSortedTypes());
        assertEquals("0=0,1=*", methodInfo.analysis().getOrDefault(HCS_METHOD, HiddenContentSelector.NONE).detailed());
        ParameterInfo p0 = methodInfo.parameters().getFirst();
        assertEquals("0=0,1=*", p0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());

        LinkedVariables lvRv = methodInfo.analysis().getOrDefault(LINKED_VARIABLES_METHOD, LinkedVariablesImpl.EMPTY);
        assertEquals("0-4-0:list", lvRv.toString());

        TypeInfo M = X.findSubType("M");
        assertTrue(M.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());
        assertFalse(M.isExtensible());
        assertTrue(M.isStatic());

        MethodInfo methodInfoM = X.findUniqueMethod("copyM", 1);
        assertSame(M, methodInfoM.returnType().parameters().getFirst().typeInfo());
        LinkedVariables lvRvM = methodInfoM.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY);
        // for this to be correct, M must be modifying
        assertEquals("0M-4-0M:list", lvRvM.toString());

        TypeInfo ME = X.findSubType("ME");
        assertTrue(ME.isExtensible());
        MethodInfo methodInfoME = X.findUniqueMethod("copyME", 1);
        assertSame(ME, methodInfoME.returnType().parameters().getFirst().typeInfo());
        LinkedVariables lvRvME = methodInfoME.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY);
        // for this to be correct, M must be modifying
        assertEquals("0M-4-0M:list", lvRvME.toString());

        MethodInfo methodInfoS = X.findUniqueMethod("copyS", 1);
        assertSame(runtime.stringTypeInfo(), methodInfoS.returnType().parameters().getFirst().typeInfo());
        LinkedVariables lvRvS = methodInfoS.analysis().getOrDefault(LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY);

        assertEquals(" - 0=List", methodInfoS.analysis().getOrDefault(HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.NO_VALUE).detailedSortedTypes());
        assertEquals("0=*", methodInfoS.analysis().getOrDefault(HCS_METHOD, HiddenContentSelector.NONE).detailed());
        ParameterInfo p0S = methodInfoS.parameters().getFirst();
        assertEquals("0=*", p0S.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE).detailed());
        assertEquals("", lvRvS.toString());
    }

    // copied from TestJavaUtil, to ensure that the encoding/decoding did its job properly
    private void testArrayListConstructor() {
        TypeInfo typeInfo = javaInspector.compiledTypesManager().get(ArrayList.class);
        TypeInfo collectionTypeInfo = javaInspector.compiledTypesManager().get(Collection.class);
        MethodInfo methodInfo = typeInfo.findConstructor(collectionTypeInfo);
        assertEquals("java.util.ArrayList.<init>(java.util.Collection<? extends E>)", methodInfo.fullyQualifiedName());
        HiddenContentTypes methodHct = methodInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
        assertEquals("0=E - 1=Collection", methodHct.detailedSortedTypes());
        assertEquals("ArrayList:E - <init>:Collection", methodHct.toString());
        HiddenContentSelector methodHcs = methodInfo.analysis().getOrDefault(HCS_METHOD, HiddenContentSelector.NONE);
        assertEquals("0=0", methodHcs.detailed());

        ParameterInfo p0 = methodInfo.parameters().getFirst();
        HiddenContentSelector paramHcs = p0.analysis().getOrDefault(HCS_PARAMETER, HiddenContentSelector.NONE);
        assertEquals("0=0,1=*", paramHcs.detailed());
    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.List;
            class X {
                final static class M { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
                static class ME { int i; int getI() { return i; } void setI(int i) { this.i = i; } }
            
                static <T> List<T> listAdd(List<T> list, T t) {
                    List<T> l = list.subList(0, 10);
                    l.add(t);
                    return l;
                }
                static List<M> listAddM(List<M> list, M t) {
                    List<M> l = list.subList(0, 10);
                    l.add(t);
                    return l;
                }
                static List<ME> listAddME(List<ME> list, ME t) {
                    List<ME> l = list.subList(0, 10);
                    l.add(t);
                    return l;
                }
                static List<String> listAdd3(List<String> list, String t) {
                    List<String> l = list.subList(0, 10);
                    l.add(t);
                    return l;
                }
            }
            """;

    @DisplayName("add on sublist")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            MethodInfo listAdd = X.findUniqueMethod("listAdd", 2);

            Statement s0 = listAdd.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            assertNotNull(vd0);
            VariableInfo vi0 = vd0.variableInfo("l");
            assertEquals("0-2-0:list", vi0.linkedVariables().toString());

            Statement s1 = listAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            ParameterInfo listAdd0 = listAdd.parameters().getFirst();
            ParameterInfo t1 = listAdd.parameters().get(1);

            VariableInfo vi1 = vd1.variableInfo("l");
            assertEquals("0-2-0:list, 0-4-*:t", vi1.linkedVariables().toString());
            assertTrue(vi1.isModified());

            // this one can only be computed through the graph algorithm
            VariableInfo vi1p0 = vd1.variableInfo(listAdd0);
            assertEquals("0-2-0:l, 0-4-*:t", vi1p0.linkedVariables().toString());
            // propagation of @Modified via graph
            assertTrue(vi1p0.isModified());

            VariableInfo vi1p1 = vd1.variableInfo(t1);
            assertEquals("*-4-0:l, *-4-0:list", vi1p1.linkedVariables().toString());

            // in the parameter's list, the local variable has been filtered out
            LinkedVariables lvP0 = listAdd0.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                    LinkedVariablesImpl.class);
            assertEquals("0-4-*:t", lvP0.toString());
            LinkedVariables lvP1 = t1.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                    LinkedVariablesImpl.class);
            assertEquals("*-4-0:list", lvP1.toString());

            LinkedVariables lvMethod = listAdd.analysis().getOrNull(LINKED_VARIABLES_METHOD,
                    LinkedVariablesImpl.class);
            assertEquals("0-2-0:list, 0-4-*:t", lvMethod.toString());

            Statement s2 = listAdd.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);

            VariableInfo vi2l = vd2.variableInfo("l");
            assertTrue(vi2l.isModified());
            VariableInfo vi2p0 = vd2.variableInfo(listAdd0);
            assertTrue(vi2p0.isModified());
            // and propagation to the parameter itself
            assertSame(FALSE, listAdd0.analysis().getOrDefault(UNMODIFIED_PARAMETER, FALSE));
        }
        {
            MethodInfo listAddM = X.findUniqueMethod("listAddM", 2);
            LinkedVariables lvMethod2 = listAddM.analysis().getOrNull(LINKED_VARIABLES_METHOD,
                    LinkedVariablesImpl.class);
            assertEquals("0M-2-0M:list, 0M-4-*M:t", lvMethod2.toString());
        }
        {
            MethodInfo listAddME = X.findUniqueMethod("listAddME", 2);
            LinkedVariables lvMethod2 = listAddME.analysis().getOrNull(LINKED_VARIABLES_METHOD,
                    LinkedVariablesImpl.class);
            assertEquals("0M-2-0M:list, 0M-4-*M:t", lvMethod2.toString());
        }
        {
            MethodInfo listAdd3 = X.findUniqueMethod("listAdd3", 2);
            LinkedVariables lvMethod3 = listAdd3.analysis().getOrNull(LINKED_VARIABLES_METHOD,
                    LinkedVariablesImpl.class);
            assertEquals("-2-:list", lvMethod3.toString());
        }
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t) {
                    List<T> l = list.subList(0, 10);
                    List<T> l2 = l;
                    l2.add(t);
                }
            }
            """;

    @DisplayName("add on sublist, extra local variable")
    @Test
    public void test6() {
        TypeInfo X = javaInspector.parse(INPUT6);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 2);
        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t = listAdd.parameters().get(1);

        // statement 0
        {
            Statement s0 = listAdd.methodBody().statements().getFirst();
            VariableData vd0 = VariableDataImpl.of(s0);
            assertNotNull(vd0);
            VariableInfo vi0 = vd0.variableInfo("l");
            assertEquals("0-2-0:list", vi0.linkedVariables().toString());

            VariableInfo vi0list = vd0.variableInfo(list);
            assertEquals("0-2-0:l", vi0list.linkedVariables().toString());
        }
        // statement 1
        {
            Statement s1 = listAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1l2 = vd1.variableInfo("l2");
            assertEquals("-1-:l, 0-2-0:list", vi1l2.linkedVariables().toString());

            VariableInfo vi1l = vd1.variableInfo("l");
            assertEquals("-1-:l2, 0-2-0:list", vi1l.linkedVariables().toString());
        }

        // statement 2
        {
            Statement s2 = listAdd.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(s2);

            VariableInfo vi1l = vd2.variableInfo("l");
            assertEquals("-1-:l2, 0-2-0:list, 0-4-*:t", vi1l.linkedVariables().toString());

            VariableInfo vi2l2 = vd2.variableInfo("l2");
            assertEquals("-1-:l, 0-2-0:list, 0-4-*:t", vi2l2.linkedVariables().toString());

            // this one can only be computed through the graph algorithm
            VariableInfo vi2list = vd2.variableInfo(list);
            assertEquals("0-2-0:l, 0-2-0:l2, 0-4-*:t", vi2list.linkedVariables().toString());

            VariableInfo vi2t = vd2.variableInfo(t);
            assertEquals("*-4-0:l, *-4-0:l2, *-4-0:list", vi2t.linkedVariables().toString());
        }

        // in the parameter's list, the local variable has been filtered out
        LinkedVariables lvP0 = list.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("0-4-*:t", lvP0.toString());
        LinkedVariables lvP1 = t.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP1.toString());
    }


    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t, T t2) {
                    list.add(t);
                    list.add(t2);
                }
            }
            """;

    @DisplayName("add 2x to list")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t = listAdd.parameters().get(1);
        ParameterInfo t2 = listAdd.parameters().get(2);

        // statement 1
        {
            Statement s1 = listAdd.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);

            VariableInfo vi1list = vd1.variableInfo(list);
            assertEquals("0-4-*:t, 0-4-*:t2", vi1list.linkedVariables().toString());

            VariableInfo vi1t = vd1.variableInfo(t);
            assertEquals("*-4-0:list", vi1t.linkedVariables().toString());

            VariableInfo vi1t2 = vd1.variableInfo(t2);
            assertEquals("*-4-0:list", vi1t2.linkedVariables().toString());
        }

        // in the parameter's list, the local variable has been filtered out
        LinkedVariables lvP0 = list.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("0-4-*:t, 0-4-*:t2", lvP0.toString());
        LinkedVariables lvP1 = t.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP1.toString());
        LinkedVariables lvP2 = t2.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP2.toString());
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t, T t2) {
                    List<T> l = list.subList(0, 10);
                    l.add(t);
                    l.add(t2);
                }
            }
            """;

    @DisplayName("add 2x on sublist")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t = listAdd.parameters().get(1);
        ParameterInfo t2 = listAdd.parameters().get(2);

        // statement 2
        {
            Statement s4 = listAdd.methodBody().statements().get(2);
            VariableData vd4 = VariableDataImpl.of(s4);

            VariableInfo vi4l = vd4.variableInfo("l");
            assertEquals("0-2-0:list, 0-4-*:t, 0-4-*:t2", vi4l.linkedVariables().toString());

            VariableInfo vi4list = vd4.variableInfo(list);
            assertEquals("0-2-0:l, 0-4-*:t, 0-4-*:t2", vi4list.linkedVariables().toString());

            VariableInfo vi4t = vd4.variableInfo(t);
            assertEquals("*-4-0:l, *-4-0:list", vi4t.linkedVariables().toString());

            VariableInfo vi4t2 = vd4.variableInfo(t2);
            assertEquals("*-4-0:l, *-4-0:list", vi4t2.linkedVariables().toString());
        }

        // in the parameter's list, the local variable has been filtered out
        LinkedVariables lvP0 = list.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("0-4-*:t, 0-4-*:t2", lvP0.toString());
        LinkedVariables lvP1 = t.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP1.toString());
        LinkedVariables lvP2 = t2.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP2.toString());
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t, T t2) {
                    List<T> l = list.subList(0, 10);
                    List<T> l2 = l;
                    l2.add(t);
                    l2.add(t2);
                }
            }
            """;

    @DisplayName("add 2x on sublist, extra local variable")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t = listAdd.parameters().get(1);
        ParameterInfo t2 = listAdd.parameters().get(2);

        // statement 3
        {
            Statement s3 = listAdd.methodBody().statements().get(3);
            VariableData vd3 = VariableDataImpl.of(s3);

            VariableInfo vi3l = vd3.variableInfo("l");
            assertEquals("-1-:l2, 0-2-0:list, 0-4-*:t, 0-4-*:t2", vi3l.linkedVariables().toString());

            VariableInfo vi3l2 = vd3.variableInfo("l2");
            assertEquals("-1-:l, 0-2-0:list, 0-4-*:t, 0-4-*:t2", vi3l2.linkedVariables().toString());

            VariableInfo vi3list = vd3.variableInfo(list);
            assertEquals("0-2-0:l, 0-2-0:l2, 0-4-*:t, 0-4-*:t2", vi3list.linkedVariables().toString());

            VariableInfo vi3t = vd3.variableInfo(t);
            assertEquals("*-4-0:l, *-4-0:l2, *-4-0:list", vi3t.linkedVariables().toString());

            VariableInfo vi3t2 = vd3.variableInfo(t2);
            assertEquals("*-4-0:l, *-4-0:l2, *-4-0:list", vi3t2.linkedVariables().toString());
        }

        // in the parameter's list, the local variable has been filtered out
        LinkedVariables lvP0 = list.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("0-4-*:t, 0-4-*:t2", lvP0.toString());
        LinkedVariables lvP1 = t.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP1.toString());
        LinkedVariables lvP2 = t2.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP2.toString());
    }


    @Language("java")
    private static final String INPUT10 = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t, T t2) {
                    List<T> l = list.subList(0, 10);
                    List<T> l2 = l;
                    l2.add(t);
                    List<T> l3 = l2;
                    l3.add(t2);
                }
            }
            """;

    @DisplayName("add 2x on sublist, extra local variables")
    @Test
    public void test10() {
        TypeInfo X = javaInspector.parse(INPUT10);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        ParameterInfo list = listAdd.parameters().getFirst();
        ParameterInfo t = listAdd.parameters().get(1);
        ParameterInfo t2 = listAdd.parameters().get(2);

        // statement 4
        {
            Statement s4 = listAdd.methodBody().statements().get(4);
            VariableData vd4 = VariableDataImpl.of(s4);

            VariableInfo vi4l = vd4.variableInfo("l");
            assertEquals("-1-:l2, -1-:l3, 0-2-0:list, 0-4-*:t, 0-4-*:t2", vi4l.linkedVariables().toString());

            VariableInfo vi4l2 = vd4.variableInfo("l2");
            assertEquals("-1-:l, -1-:l3, 0-2-0:list, 0-4-*:t, 0-4-*:t2", vi4l2.linkedVariables().toString());

            VariableInfo vi4l3 = vd4.variableInfo("l3");
            assertEquals("-1-:l, -1-:l2, 0-2-0:list, 0-4-*:t, 0-4-*:t2", vi4l3.linkedVariables().toString());

            VariableInfo vi4list = vd4.variableInfo(list);
            assertEquals("0-2-0:l, 0-2-0:l2, 0-2-0:l3, 0-4-*:t, 0-4-*:t2", vi4list.linkedVariables().toString());

            VariableInfo vi4t = vd4.variableInfo(t);
            assertEquals("*-4-0:l, *-4-0:l2, *-4-0:l3, *-4-0:list", vi4t.linkedVariables().toString());

            VariableInfo vi4t2 = vd4.variableInfo(t2);
            assertEquals("*-4-0:l, *-4-0:l2, *-4-0:l3, *-4-0:list", vi4t2.linkedVariables().toString());
        }

        // in the parameter's list, the local variable has been filtered out
        LinkedVariables lvP0 = list.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("0-4-*:t, 0-4-*:t2", lvP0.toString());
        LinkedVariables lvP1 = t.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP1.toString());
        LinkedVariables lvP2 = t2.analysis().getOrNull(LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.class);
        assertEquals("*-4-0:list", lvP2.toString());
    }

}
