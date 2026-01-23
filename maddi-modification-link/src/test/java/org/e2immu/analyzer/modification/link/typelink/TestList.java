package org.e2immu.analyzer.modification.link.typelink;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestList extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Arrays;public class X<T> {
                T[] ts;
                private T get(int index) {
                    return ts[index];
                }
                public static <K> K method(int i, X<K> x) {
                    K k = x.get(i);
                    return k;
                }
                public void print() { System.out.println(ts); }
            }
            """;

    LinkComputer.Options doNotRecurse = new LinkComputer.Options(false, false, true);
    LinkComputer.Options forceShallow = new LinkComputer.Options(true, true, true);

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        MethodInfo get = X.findUniqueMethod("get", 1);
        ReturnVariable rv = new ReturnVariableImpl(get);
        FieldInfo tsField = X.getFieldByName("ts", true);
        FieldReference ts = runtime.newFieldReference(tsField);
        get.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(rv).add(LinkNatureImpl.IS_ELEMENT_OF, ts).build(), List.of(LinksImpl.EMPTY),
                Set.of()));
        get.analysis().set(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.TRUE);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        MethodLinkedVariables mlvGet = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        Links tlvK0 = k0.linkedVariablesOrEmpty();
        assertEquals("k∈1:x.ts", tlvK0.toString());
        VariableInfo x0 = vd0.variableInfo(method.parameters().getLast());
        assertEquals("1:x.ts∋k", x0.linkedVariables().toString());
        assertFalse(x0.isModified());

        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, -] --> method∈1:x.ts", tlvMethod.toString());
    }


    @DisplayName("shallow get/multiplicity 1 instead of 2")
    @Test
    public void testShallow1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, forceShallow);
        tlc.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodLinkedVariables tlvGet = get.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> get←this*.§t", tlvGet.toString());

        MethodInfo method = X.findUniqueMethod("method", 2);
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, -] --> method←1:x*.§k", tlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X<T> {
                T[] ts;
                private T get(int index) {
                    return ts[index];
                }
                private void set(int index, T t) {
                    ts[index] = t;
                }
                public static <K> K method(int i, X<K> x, K k) {
                    K prev = x.get(i);
                    x.set(i, k);
                    return prev;
                }
            
                // to have multiplicity 2
                T[] getTs() { return ts; }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 3);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo prev0 = vd0.variableInfo("prev");
        Links tlvPrev0 = prev0.linkedVariablesOrEmpty();
        assertEquals("prev←1:x.ts[0:i],prev∈1:x.ts", tlvPrev0.toString());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo prev1 = vd1.variableInfo("prev");
        Links tlvPrev1 = prev1.linkedVariablesOrEmpty();
        assertEquals("prev←1:x.ts[0:i],prev←2:k,prev∈1:x.ts", tlvPrev1.toString());


        ParameterInfo k = method.parameters().get(2);
        VariableInfo k1 = vd1.variableInfo(k);
        Links tlvK1 = k1.linkedVariablesOrEmpty();
        assertEquals("2:k→1:x.ts[0:i],2:k→prev,2:k∈1:x.ts", tlvK1.toString());

        ParameterInfo x = method.parameters().get(1);
        VariableInfo x1 = vd1.variableInfo(x);
        Links tlvX1 = x1.linkedVariablesOrEmpty();
        assertEquals("1:x.ts[0:i]→prev,1:x.ts[0:i]←2:k,1:x.ts[0:i]∈1:x.ts,1:x.ts∋2:k,1:x.ts∋prev",
                tlvX1.toString());

        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("""
                [-, 1:x.ts*[0:i]←2:k*,1:x.ts*[0:i]∈1:x.ts*,1:x.ts*∋2:k*, 2:k*→1:x.ts*[0:i],2:k*∈1:x.ts*] --> \
                method←1:x.ts*[0:i],method←2:k*,method∈1:x.ts*\
                """, tlvMethod.toString());
    }


    @Test
    public void testShallow2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, forceShallow);
        tlc.doPrimaryType(X);

        MethodInfo set = X.findUniqueMethod("set", 2);
        MethodLinkedVariables tlvSet = set.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, 1:t∈this*.§ts] --> -", tlvSet.toString());

        MethodInfo method = X.findUniqueMethod("method", 3);
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, -, -] --> method∈1:x*.§ks,method←2:k", tlvMethod.toString());
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class X<T> {
                T[] ts;
                private X(T[] ts) { this.ts = ts; }
                private X<T> copy() {
                    return new X<>(ts);
                }
                public static <K> X<K> method(X<K> x) {
                    X<K> y = x.copy();
                    return y;
                }
            }
            """;

    // this test models the .sublist(...) type of methods
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);

        MethodInfo copy = X.findUniqueMethod("copy", 0);
        ReturnVariable rv = new ReturnVariableImpl(copy);
        FieldInfo tsField = X.getFieldByName("ts", true);
        FieldReference ts = runtime.newFieldReference(tsField);
        FieldReference rvTs = runtime.newFieldReference(tsField, runtime.newVariableExpression(rv), tsField.type());
        copy.analysis().set(METHOD_LINKS, new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(rv).add(rvTs, LinkNatureImpl.SHARES_ELEMENTS, ts).build(), List.of(),
                Set.of()));

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo method = X.findUniqueMethod("method", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, doNotRecurse);
        tlc.doPrimaryType(X);

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo y0 = vd0.variableInfo("y");
        Links tlvY0 = y0.linkedVariablesOrEmpty();
        assertEquals("y.ts~0:x.ts", tlvY0.toString());

        // * because of doNotRecurse, shallow analysis
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> method.ts~0:x*.ts", tlvMethod.toString());
    }


    @Test
    public void testShallow3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, forceShallow);
        tlc.doPrimaryType(X);

        MethodInfo copy = X.findUniqueMethod("copy", 0);
        MethodLinkedVariables tlvSet = copy.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> copy.§t←this*.§t,copy.§m≡this*.§m", tlvSet.toString());

        MethodInfo method = X.findUniqueMethod("method", 1);
        MethodLinkedVariables tlvMethod = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> method.§k←0:x*.§k", tlvMethod.toString());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                static class II {
                    void method1(String s) { }
                    void method2(int i) { }
                }
                void methodA() {
                    List<II> iis = new ArrayList<>();
                    iis.add(new II());
                    for(II ii: iis) ii.method1("abc");
                    iis.remove(0).method2(4);
                }
                void methodB() {
                    List<II> iis = new ArrayList<>();
                    II removed = iis.removeFirst();
                    removed.method2(4);
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT4);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        {
            MethodInfo methodB = X.findUniqueMethod("methodB", 0);
            methodB.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(methodB));
            Statement callM2 = methodB.methodBody().statements().get(2);
            VariableData vd2 = VariableDataImpl.of(callM2);
            VariableInfo removed = vd2.variableInfoContainerOrNull("removed").best(Stage.EVALUATION);
            Links tlvT1 = removed.linkedVariablesOrEmpty();
            assertEquals("removed∈iis.§$s", tlvT1.toString());
        }
        {
            MethodInfo methodA = X.findUniqueMethod("methodA", 0);
            methodA.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(methodA));

            Statement callM2 = methodA.methodBody().statements().get(3);
            MethodCall methodCall = (MethodCall) callM2.expression();
            assertEquals("iis.remove(0).method2(4)", methodCall.toString());
            Value.VariableBooleanMap tlvMc = methodCall.analysis()
                    .getOrNull(LinkComputerImpl.VARIABLES_LINKED_TO_OBJECT, ValueImpl.VariableBooleanMapImpl.class);
            assertEquals("iis=true", tlvMc.toString());
        }
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            import java.util.List;
            public class X {
                List<String> method1(List<String> in) {
                    return in.subList(0, 4);
                }
                List<String> method2(List<String> in) {
                    List<String> intermediate1 = in.subList(0, 4);
                    System.out.println(intermediate1);
                    List<String> intermediate2 = intermediate1;
                    return intermediate2;
                }
            }
            """;

    @DisplayName("Show how §m is present after an assignment")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);

        MethodInfo method1 = X.findUniqueMethod("method1", 1);
        MethodLinkedVariables mlv1 = method1.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method1));
        assertEquals("[-] --> method1.§$s⊆0:in.§$s,method1.§m≡0:in.§m", mlv1.toString());

        MethodInfo method2 = X.findUniqueMethod("method2", 1);
        MethodLinkedVariables mlv2 = method2.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method2));

        VariableData vd2 = VariableDataImpl.of(method2.methodBody().statements().get(2));
        VariableInfo viIntermediate2 = vd2.variableInfo("intermediate2");
        // the 4th link is created by LinkGraph.makeComparableSub
        assertEquals("""
                intermediate2.§$s←intermediate1.§$s,\
                intermediate2.§$s⊆0:in.§$s,\
                intermediate2.§m≡0:in.§m,\
                intermediate2.§m≡intermediate1.§m,\
                intermediate2←intermediate1\
                """, viIntermediate2.linkedVariables().toString());
        assertEquals("[-] --> method2.§$s⊆0:in.§$s,method2.§m≡0:in.§m", mlv2.toString());
    }

}
