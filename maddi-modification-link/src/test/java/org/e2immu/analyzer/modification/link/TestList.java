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
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("get<this.ts,get==this.ts[index]", mlv.ofReturnValue().toString());
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
        assertEquals("get<this.ts,get==this.ts[index]", lvGet.ofReturnValue().toString());

        // then, do method
        MethodLinkedVariables lvMethod = method.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(method));

        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().getFirst());
        VariableInfo k0 = vd0.variableInfo("k");
        Links linksK = k0.analysis().getOrDefault(LINKS, LinksImpl.EMPTY);
        assertEquals("k<x.ts,k==x.ts[index]", linksK.toString());

        assertEquals("[] --> method<x.ts,method==x.ts[index]", lvMethod.toString());
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
        FieldInfo virtualContentField = runtime.newFieldInfo("tArray", false,
                runtime.newParameterizedType(list.typeParameters().getFirst(), 1, null), list);
        FieldReference virtualContentVariable = runtime.newFieldReference(virtualContentField);

        assertEquals("java.util.List.of(E)", of.descriptor());
        ReturnVariable ofRv = new ReturnVariableImpl(of);
        MethodLinkedVariablesImpl mlvOf = new MethodLinkedVariablesImpl(
                new LinksImpl.Builder(ofRv)
                        .add(LinkNature.CONTAINS, of.parameters().getFirst())
                        .build(),
                List.of());
        assertEquals("[] --> of>0:e1", mlvOf.toString());
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
        assertEquals("rv0>t", r.links().toString());

        // rv0>t + t=this.ts[0] + this.ts[0]<this.ts  = asShortList~this.ts
        MethodLinkedVariables lvAsShortList = asShortList.analysis().getOrCreate(METHOD_LINKS,
                () -> tlc.doMethod(asShortList));
        // the intermediary step after evaluating the
        assertEquals("asShortList~this.ts", lvAsShortList.ofReturnValue().toString());

    }
}
