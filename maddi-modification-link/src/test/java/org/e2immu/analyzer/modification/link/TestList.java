package org.e2immu.analyzer.modification.link;

import org.e2immu.analyzer.modification.link.impl.*;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestList extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.List;public class X<T> {
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
        assertEquals("get<this.ts,get==this.ts[0:index]", mlv.ofReturnValue().toString());
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
        assertEquals("get<this.ts,get==this.ts[0:index]", lvGet.ofReturnValue().toString());

        // then, do method
        MethodLinkedVariables lvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        Links linksK = k0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("k<1:x.ts,k==1:x.ts[0:index]", linksK.toString());

        assertEquals("[] --> method<1:x.ts,method==1:x.ts[0:index]", lvMethod.toString());
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
        FieldReference virtualContentVariable = runtime.newFieldReference(virtualContentField);
        FieldReference rvContent = runtime.newFieldReference(virtualContentField, runtime.newVariableExpression(ofRv),
                virtualContentField.type());
        assertEquals("java.util.List.of(E)", of.descriptor());
        MethodLinkedVariablesImpl mlvOf = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(ofRv)
                        .add(rvContent, LinkNature.CONTAINS, of.parameters().getFirst())
                        .build(),
                List.of());
        assertEquals("[] --> of.tArray>0:e1", mlvOf.toString());
        of.analysis().set(METHOD_LINKS, mlvOf);

        MethodInfo asShortList = X.findUniqueMethod("asShortList", 0);
        LinkComputerImpl tlc = new LinkComputerImpl(javaInspector, false, false);
        LinkComputerImpl.SourceMethodComputer smc = tlc.new SourceMethodComputer(asShortList);
        ExpressionVisitor ev = new ExpressionVisitor(javaInspector, tlc, smc,
                asShortList, new RecursionPrevention(false), new AtomicInteger());

        // test the evaluation of T t = ts[0]
        LocalVariableCreation lvc = (LocalVariableCreation) asShortList.methodBody().statements().getFirst();
        var map = smc.handleLvc(lvc, null);
        assertEquals("{t=t==this.ts[0], this.ts[0]=this.ts[0]<this.ts}", map.toString());

        // test the evaluation of List.of(t)
        VariableData vd0 = VariableDataImpl.of(lvc);
        ExpressionVisitor.Result r = ev.visit(asShortList.methodBody().statements().getLast().expression(), vd0);
        assertEquals("rv0.tArray>t", r.links().toString());

        // rv0.tArray>t + t=this.ts[0] + this.ts[0]<this.ts  = asShortList.tArray~this.ts
        MethodLinkedVariables lvAsShortList = asShortList.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(asShortList));

        assertEquals("asShortList.tArray~this.ts,asShortList.tArray>this.ts[0]",
                lvAsShortList.ofReturnValue().toString());
    }

    @DisplayName("Analyze 'sub', manually inserting values for List.subList()")
    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT1);

        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo atomicBoolean = javaInspector.compiledTypesManager().getOrLoad(AtomicBoolean.class);
        assertNotNull(atomicBoolean);

        TypeInfo list = javaInspector.compiledTypesManager().get(List.class);
        MethodInfo subList = list.findUniqueMethod("subList", 2);
        FieldInfo virtualContentField = runtime.newFieldInfo("tArray", false,
                runtime.newParameterizedType(list.typeParameters().getFirst(), 1, null), list);
        FieldReference virtualContentVariable = runtime.newFieldReference(virtualContentField);
        FieldInfo virtualModifiedField = runtime.newFieldInfo("M", false,
                atomicBoolean.asSimpleParameterizedType(), list);
        FieldReference virtualModifiedVariable = runtime.newFieldReference(virtualModifiedField);
        ReturnVariable subListRv = new ReturnVariableImpl(subList);
        FieldReference rvTArray = runtime.newFieldReference(virtualContentField,
                runtime.newVariableExpression(subListRv), virtualContentField.type());
        FieldReference rvM = runtime.newFieldReference(virtualModifiedField, runtime.newVariableExpression(subListRv),
                virtualModifiedField.type());
        MethodLinkedVariablesImpl mlvSubList = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(subListRv)
                        .add(rvTArray, LinkNature.INTERSECTION_NOT_EMPTY, virtualContentVariable)
                        .add(rvM, LinkNature.IS_IDENTICAL_TO, virtualModifiedVariable)
                        .build(),
                List.of());
        assertEquals("[] --> subList.M==this.M,subList.tArray~this.tArray", mlvSubList.toString());
        subList.analysis().set(METHOD_LINKS, mlvSubList);

        LinkComputerImpl tlc = new LinkComputerImpl(javaInspector, false, false);
        MethodInfo sub = X.findUniqueMethod("sub", 1);
        LinkComputerImpl.SourceMethodComputer smc = tlc.new SourceMethodComputer(subList);
        ExpressionVisitor ev = new ExpressionVisitor(javaInspector, tlc, smc,
                sub, new RecursionPrevention(false), new AtomicInteger());

        VariableData vd0 = VariableDataImpl.of(sub.methodBody().statements().getFirst());

        // test the evaluation of List<Z> zs = in.subList(2, n);
        LocalVariableCreation lvc = (LocalVariableCreation) sub.methodBody().statements().get(1);
        var map = smc.handleLvc(lvc, vd0);
        assertEquals("{zs=zs.M==0:in.M,zs.tArray~0:in.tArray,zs==0:in}", map.toString());
    }
}
