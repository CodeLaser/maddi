package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestList extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;
            public class X<T> {
                T[] ts;
                private T get(int index) {
                    return ts[index];
                }
            
                public static <K> K method(int i, X<K> x) {
                    K k = x.get(i);
                    return k;
                }
            
                public List<T> asShortList() {
                    T t = ts[0];
                    return List.of(t);
                }
            
                public static <Z> List<Z> sub(List<Z> in) {
                    int n = in.size();
                    List<Z> zs = in.subList(2, n);
                    return zs;
                }
            
                public void set(T t, int index) {
                    ts[index] = t;
                }
            }
            """;

    @DisplayName("Analyze 'get', array access")
    @Test
    public void test1a() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("get∈this.ts,get≡this.ts[0:index]", mlv.ofReturnValue().toString());
    }

    @DisplayName("Analyze 'method', given method links for 'get'")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("get", 1);
        MethodInfo method = X.findUniqueMethod("method", 2);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        // first, do get()
        MethodLinkedVariables lvGet = get.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(get));
        assertEquals("get∈this.ts,get≡this.ts[0:index]", lvGet.ofReturnValue().toString());

        // then, do method
        MethodLinkedVariables lvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        Links linksK = k0.linkedVariablesOrEmpty();
        assertEquals("k∈1:x.ts,k≡1:x.ts[0:i]", linksK.toString());

        assertEquals("[-, -] --> method∈1:x.ts,method≡1:x.ts[0:i]", lvMethod.toString());
    }

    @DisplayName("Analyze 'asShortList', manually inserting values for List.of()")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        MethodInfo of = list.methodStream()
                .filter(m -> "of".equals(m.name()) &&
                             m.parameters().size() == 1
                             && m.parameters().getFirst().parameterizedType().arrays() == 0)
                .findFirst().orElseThrow();
        ReturnVariable ofRv = new ReturnVariableImpl(of);
        FieldInfo virtualContentField = runtime.newFieldInfo("tArray", false,
                runtime.newParameterizedType(list.typeParameters().getFirst(), 1, null), list);
        FieldReference rvContent = runtime.newFieldReference(virtualContentField, runtime.newVariableExpression(ofRv),
                virtualContentField.type());
        assertEquals("java.util.List.of(E)", of.descriptor());
        MethodLinkedVariablesImpl mlvOf = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(ofRv)
                        .add(rvContent, LinkNatureImpl.CONTAINS_AS_MEMBER, of.parameters().getFirst())
                        .build(),
                List.of(LinksImpl.EMPTY));
        assertEquals("[-] --> of.tArray∋0:e1", mlvOf.toString());
        of.analysis().set(METHOD_LINKS, mlvOf);

        MethodInfo asShortList = X.findUniqueMethod("asShortList", 0);
        LinkComputerImpl tlc = new LinkComputerImpl(javaInspector, false, false);
        LinkComputerImpl.SourceMethodComputer smc = tlc.new SourceMethodComputer(asShortList);
        ExpressionVisitor ev = new ExpressionVisitor(javaInspector, new VirtualFieldComputer(javaInspector), tlc, smc,
                asShortList, new RecursionPrevention(false), new AtomicInteger());

        // test the evaluation of T t = ts[0]
        LocalVariableCreation lvc = (LocalVariableCreation) asShortList.methodBody().statements().getFirst();
        ExpressionVisitor.Result r = smc.handleLvc(lvc, null);
        assertEquals("t: t≡this.ts[0]; this.ts[0]: this.ts[0]∈this.ts", r.extra().toString());

        // test the evaluation of List.of(t)
        VariableData vd0 = VariableDataImpl.of(lvc);
        ExpressionVisitor.Result r2 = ev.visit(asShortList.methodBody().statements().getLast().expression(), vd0);
        assertEquals("$__rv0.tArray∋t", r2.links().toString());

        // rv0.tArray>t + t=this.ts[0] + this.ts[0]<this.ts  = asShortList.tArray~this.ts
        MethodLinkedVariables lvAsShortList = asShortList.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(asShortList));

        assertEquals("""
                asShortList.tArray~this.ts,asShortList.tArray∋this.ts[0]\
                """, lvAsShortList.ofReturnValue().toString());
    }

    @DisplayName("Analyze 'sub'")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl tlc = new LinkComputerImpl(javaInspector, true, false);
        MethodInfo sub = X.findUniqueMethod("sub", 1);
        MethodLinkedVariables mlvSub = sub.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(sub));

        assertEquals("[-] --> sub.§m≡0:in.§m,sub.§zs⊆0:in.§zs", mlvSub.toString());
    }


    @DisplayName("Analyze 'set'")
    @Test
    public void test4() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        MethodInfo set = X.findUniqueMethod("set", 2);

        Expression assignment = set.methodBody().statements().getFirst().expression();
        LinkComputerImpl tlc = new LinkComputerImpl(javaInspector, false, false);
        LinkComputerImpl.SourceMethodComputer smc = tlc.new SourceMethodComputer(set);
        ExpressionVisitor ev = new ExpressionVisitor(javaInspector, new VirtualFieldComputer(javaInspector), tlc, smc,
                set, new RecursionPrevention(false), new AtomicInteger());
        ExpressionVisitor.Result r = ev.visit(assignment, null);
        assertEquals("this.ts[1:index]≡0:t", r.links().toString());
        assertEquals("0:t: -; this.ts[1:index]: this.ts[1:index]∈this.ts", r.extra().toString());

        // now the same, but as a statement; then, the data will be saved
        VariableData vd = smc.doStatement(set.methodBody().statements().getFirst(), null, true);
        List<Links> list = new Expand(runtime).parameters(set, vd);
        // MethodLinkedVariables mlv = tlc.doMethod(set)
        assertEquals("0:t∈this.ts,0:t≡this.ts[1:index]", list.getFirst().toString());
    }

    @Language("java")
    private static final String INPUT5a = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            
            public class X<T> {
                private final List<T> list;
                X(List<T> in) {
                    this.list = in;
                }
                public List<T> getList() {
                    return list;
                }
            }
            """;

    @DisplayName("Analyze direct link from constructor parameter to getter")
    @Test
    public void test5a() {
        TypeInfo X = javaInspector.parse(INPUT5a);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo get = X.findUniqueMethod("getList", 0);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodLinkedVariables mlv = tlc.doMethod(get);
        assertEquals("getList≡this.list", mlv.ofReturnValue().toString());

        MethodInfo constructor = X.findConstructor(1);
        MethodLinkedVariables mlvConstructor = tlc.doMethod(constructor);
        assertEquals("[0:in≡this.list] --> -", mlvConstructor.toString());
    }

    @Language("java")
    private static final String INPUT5b = """
            package a.b;
            import java.util.List;
            import java.util.ArrayList;
            
            public class X<T> {
                private final List<T> list;
                X(List<T> in) {
                    this.list = new ArrayList<>(in);
                }
                public List<T> getList() {
                    return list;
                }
            }
            """;

    @DisplayName("Analyze constructor inbetween constructor parameter and getter")
    @Test
    public void test5b() {
        TypeInfo X = javaInspector.parse(INPUT5b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo collection = javaInspector.compiledTypesManager().getOrLoad(Collection.class);
        TypeInfo arrayList = javaInspector.compiledTypesManager().getOrLoad(ArrayList.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("§m - E[] §es", vfc.compute(arrayList).toString());

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, false, false);

        MethodInfo c1 = arrayList.findConstructor(collection);
        MethodLinkedVariables mlvC1 = c1.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(c1));
        assertEquals("[0:c.§es⊇this.§es] --> -", mlvC1.toString());

        MethodInfo get = X.findUniqueMethod("getList", 0);
        MethodLinkedVariables mlv = linkComputer.doMethod(get);
        assertEquals("getList≡this.list", mlv.ofReturnValue().toString());

        MethodInfo constructor = X.findConstructor(1);
        MethodLinkedVariables mlvConstructor = linkComputer.doMethod(constructor);

        VariableData vd = VariableDataImpl.of(constructor.methodBody().statements().getFirst());
        VariableInfo viList = vd.variableInfo("a.b.X.list");
        assertEquals("this.list.§ts⊆0:in.§ts", viList.linkedVariables().toString());

        assertEquals("[0:in.§ts⊇this.list.§ts] --> -", mlvConstructor.toString());
    }

    @DisplayName("Analyze constructor, ensure recursive computation")
    @Test
    public void test5c() {
        TypeInfo X = javaInspector.parse(INPUT5b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);
        MethodInfo constructor = X.findConstructor(1);
        MethodLinkedVariables mlvConstructor = linkComputer.doMethod(constructor);
        assertEquals("[0:in.§ts⊇this.list.§ts] --> -", mlvConstructor.toString());
    }

    @Language("java")
    private static final String INPUT6a = """
            package a.b;
            import java.util.List;
            class X<T> {
                List<T> list;
                void listAdd(T t) {
                   list.add(t);
                }
            }
            """;

    @DisplayName("Analyze 'add' on List, instance")
    @Test
    public void test6a() {
        TypeInfo X = javaInspector.parse(INPUT6a);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 1);
        MethodLinkedVariables mlvListAdd = listAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(listAdd));
        assertEquals("[0:t∈this.list.§ts] --> -", mlvListAdd.toString());
    }

    @Language("java")
    private static final String INPUT6b = """
            package a.b;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t) {
                   list.add(t);
                }
            }
            """;

    @DisplayName("Analyze 'add' on List, static")
    @Test
    public void test6b() {
        TypeInfo X = javaInspector.parse(INPUT6b);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);

        MethodInfo listAdd = X.findUniqueMethod("listAdd", 2);
        MethodLinkedVariables mlvListAdd = listAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(listAdd));
        assertEquals("[0:list.§ts∋1:t, 1:t∈0:list.§ts] --> -", mlvListAdd.toString());
    }

    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            import java.util.Collections;
            import java.util.List;
            class X<K> {
                static <T> boolean listAdd(List<T> list, T t) {
                   list.add(t);
                }
                boolean add(K k, List<K> in) {
                    return listAdd(in, k);
                }
                static <L> boolean add2(L l, List<L> in) {
                    return listAdd(in, l);
                }
            }
            """;

    @DisplayName("Analyze static add all")
    @Test
    public void test7() {
        TypeInfo X = javaInspector.parse(INPUT7);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo collections = javaInspector.compiledTypesManager().getOrLoad(Collections.class);
        VirtualFieldComputer vfc = new VirtualFieldComputer(javaInspector);
        assertEquals("/ - /", vfc.compute(collections).toString());

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);
        MethodInfo listAdd = X.findUniqueMethod("listAdd", 2);
        MethodLinkedVariables mlvListAdd = listAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(listAdd));
        assertEquals("[0:list.§ts∋1:t, 1:t∈0:list.§ts] --> -", mlvListAdd.toString());

        MethodInfo add = X.findUniqueMethod("add", 2);
        MethodLinkedVariables mlvAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(add));
        assertEquals("[0:k∈1:in.§ks, 1:in.§ks∋0:k] --> -", mlvAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("add2", 2);
        MethodLinkedVariables mlvAdd2 = add2.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(add2));
        assertEquals("[0:l∈1:in.§ls, 1:in.§ls∋0:l] --> -", mlvAdd2.toString());
    }

    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            import java.util.Collections;
            import java.util.List;
            class X {
                static <T> void listAdd(List<T> list, T t1, T t2) {
                    Collections.addAll(list, t1, t2);
                }
            }
            """;

    @DisplayName("Analyze Collections.addAll(...), cross links")
    @Test
    public void test8() {
        TypeInfo X = javaInspector.parse(INPUT8);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);
        MethodInfo listAdd = X.findUniqueMethod("listAdd", 3);
        MethodLinkedVariables mlvListAdd = listAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(listAdd));
        assertEquals("[0:list.§ts∋1:t1,0:list.§ts∋2:t2, 1:t1∈0:list.§ts, 2:t2∈0:list.§ts] --> -", mlvListAdd.toString());
    }


    @Language("java")
    private static final String INPUT9 = """
            package a.b;
            import java.util.Collections;
            import java.util.List;
            class X<K> {
                static <T> List<T> one(T t) {
                   return List.of(t);
                }
                List<K> callOne(K k) {
                    return one(k);
                }
                static <L> List<L> callOne2(L l) {
                    return one(l);
                }
            }
            """;

    @DisplayName("Analyze static List.of()")
    @Test
    public void test9() {
        TypeInfo X = javaInspector.parse(INPUT9);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        LinkComputerImpl linkComputer = new LinkComputerImpl(javaInspector, true, false);
        MethodInfo listAdd = X.findUniqueMethod("one", 1);
        MethodLinkedVariables mlvListAdd = listAdd.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(listAdd));
        assertEquals("[-] --> one.§ts∋0:t", mlvListAdd.toString());

        MethodInfo add = X.findUniqueMethod("callOne", 1);
        MethodLinkedVariables mlvAdd = add.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(add));
        assertEquals("[-] --> callOne.§ks∋0:k", mlvAdd.toString());

        MethodInfo add2 = X.findUniqueMethod("callOne2", 1);
        MethodLinkedVariables mlvAdd2 = add2.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(add2));
        assertEquals("[-] --> callOne2.§ls∋0:l", mlvAdd2.toString());
    }
}
