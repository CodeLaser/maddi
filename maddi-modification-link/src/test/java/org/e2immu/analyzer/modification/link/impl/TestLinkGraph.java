package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestLinkGraph extends CommonTest {

    // this.runtime.ctm.bci.t.runtime....


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.support.SetOnce;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            
            class X {
                interface TypeInfo {
                    String fullyQualifiedName();
                 }
                interface Runtime {
                    List<TypeInfo> predefined();
                 }
            
                static class RuntimeImpl implements Runtime {
                    private CompiledTypesManager ctm;
                    RuntimeImpl(CompiledTypesManager ctm) {
                        this.ctm = ctm;
                    }
                    public List<TypeInfo> predefined() {
                        return new ArrayList<>();
                    }
                }
                interface ByteCodeInspector {
                    TypeInfo get(String fqn);
                }
                interface CompiledTypesManager {
                    void addPredefinedObjects(List<TypeInfo> typeInfos);
                    void setByteCodeInspector(ByteCodeInspector bc);
                    TypeInfo get(String fqn);
                }
                static class ByteCodeInspectorImpl implements ByteCodeInspector {
                    private final Runtime runtime;
                    private final CompiledTypesManager compiledTypesManager;
            
                    public ByteCodeInspectorImpl(Runtime runtime, CompiledTypesManager compiledTypesManager) {
                        this.runtime = runtime;
                        this.compiledTypesManager = compiledTypesManager;
                    }
                    public TypeInfo get(String fqn) {
                        return compiledTypesManager.get(fqn);
                    }
                }
                static class CompiledTypesManagerImpl implements CompiledTypesManager {
                    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
                    private final Map<String, TypeInfo> mapSingleTypeForFQN = new HashMap<>();
            
                    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
                        this.byteCodeInspector.set(byteCodeInspector);
                    }
                    public void addPredefinedObjects(List<TypeInfo> typeInfos) {
                        for(TypeInfo typeInfo: typeInfos) {
                            this.mapSingleTypeForFQN.put(typeInfo.fullyQualifiedName(), get(typeInfo.fullyQualifiedName()));
                        }
                    }
                    public TypeInfo get(String fqn) {
                        return this.byteCodeInspector.get().get(fqn);
                    }
                }
                private Runtime runtime;
                private CompiledTypesManager ctm;
            
                void initialize(List<TypeInfo> typeInfos) {
                    CompiledTypesManagerImpl ctmi = new CompiledTypesManagerImpl();
            
                    runtime = new RuntimeImpl(ctm);
                    ByteCodeInspector bc = new ByteCodeInspectorImpl(runtime, ctmi);
                    ctm.setByteCodeInspector(bc);
                    ctm.addPredefinedObjects(runtime.predefined());
                }
             }
            """;

    //{DependentVariableImpl@6346}this.runtime.compiledTypesManager.byteCodeInspector.t.runtime.compiledTypesManager.byteCodeInspector.t.runtime.compiledTypesManager.byteCodeInspector.t.compiledTypesManager.mapSingleTypeForFQN.§$$s[-2] -> {HashMap@6347} size = 5
    @DisplayName("class reference cycles")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            
            import java.util.List;import java.util.stream.Stream;class X {
                interface Runtime { Expression newEmptyExpression(); }
                interface Expression { }
                interface Operator {
                    Expression combine(Expression template, Expression other) { }
                    static Operator STATEMENT(Runtime runtime) {
                        return new EraseTemplate(runtime, ";");
                    }
                    record EraseTemplate(Runtime runtime, String name) implements Operator {
                            Expression combine(Expression template, Expression other) {
                                return template == null ? template: other;
                            }
                     }
                 }
                interface CMParSeq<T> {
                    Runtime runtime();
                    Expression template();
                }
                interface Parallel<T extends Comparable<? super T>> extends CMParSeq<T> { }
                interface Sequential<T extends Comparable<? super T>> extends CMParSeq<T> { }
                static class RuntimeImpl implements Runtime {
                     public Expression newEmptyExpression() {
                        return null;
                     }
                }
                private Runtime runtime = new RuntimeImpl();
                private Operator plus;
                private Expression T = runtime.newEmptyExpression();
                static class BlankOutVariables {
                    private Runtime runtime;
                    private Expression expression;
                    BlankOutVariables(Runtime runtime) { this.runtime = runtime; }
                    Expression ve() { return this.expression; }
                }
                protected static final BlankOutVariables blankOutVariables = new BlankOutVariables(runtime);

                record ParSeqs<T extends Comparable<? super T>>(Runtime runtime, List<CMParSeq<T>> parSeqs,
                                                                Expression template, Operator operator)
                                                                 implements Parallel<T> {}
                record SeqPars<T extends Comparable<? super T>>(Runtime runtime, List<Parallel<T>> pars,
                                                                Expression template) implements Sequential<T> {
                    boolean contains(T t) { 
                        return false;
                    }
                                                                }
                record ParSeqElement<T extends Comparable<? super T>>(Runtime runtime, T t, Expression template)
                                                                 implements Parallel<T>, Sequential<T> {
                    public CMParSeq<T> before(CMParSeq<T> other, Operator operator) {
                       if (equals(other)) return this;
                       Expression newTemplate = operator.combine(template, other.template());
                       if(other instanceof ParSeqElement<T> element) {
                           if (t.equals(element.t)) {
                               return new ParSeqElement<>(runtime, t, newTemplate);
                           }
                       }
                       if (other instanceof SeqPars<T> seqPars) {
                           if (!seqPars.contains(t)) {
                                  return new SeqPars<>(runtime, add(this, seqPars.pars()), newTemplate);
                           }
                           List<Parallel<T>> newPars = Stream.concat(Stream.of(this),
                                       seqPars.pars().stream().flatMap(p -> toParallelStreamWithoutMe(p, operator))).toList();
                           return new SeqPars<>(runtime, newPars, template);
                       }
                       return null;
                    }
                    public CMParSeq<T> inParallelWith(CMParSeq<T> other, Operator operator) {
                        return null;
                    }
                    public Stream<Parallel<T>> toParallelStreamWithoutMe(CMParSeq<T> p, Operator operator) {
                        if (equals(p) || p instanceof ParSeqElement<T> element && t.equals(element.t)) {
                            return Stream.of();
                        }
                        if (p instanceof Parallel<T> parallel) {
                           return Stream.of(parallel);
                        }
                        return Stream.of();
                    }
                    public static <T> List<T> add(T t, List<T> l1) {
                        return Stream.concat(Stream.of(t), l1.stream()).toList();
                    }
                }
                static void assertEquals(String s1, String s2) {
                    assert s1.equals(s2);
                }
                protected static final ParSeqElement<Integer> e1 = new ParSeqElement<>(runtime, 1, blankOutVariables.ve());
                protected static final ParSeqElement<Integer> e2 = new ParSeqElement<>(runtime, 2, blankOutVariables.ve());
                protected static final ParSeqElement<Integer> e3 = new ParSeqElement<>(runtime, 3, blankOutVariables.ve());
                protected static final ParSeqElement<Integer> e4 = new ParSeqElement<>(runtime, 4, blankOutVariables.ve());
                protected static final ParSeqElement<Integer> e5 = new ParSeqElement<>(runtime, 5, blankOutVariables.ve());

                void test3() {
                    ParSeqs<Integer> e45 = new ParSeqs<>(runtime, List.of(e4, e5), T, plus);
                    SeqPars<Integer> seqPars = new SeqPars<>(runtime, List.of(e2, e3, e45), T);
                    assertEquals("(2. 3. {4; 5})", seqPars.toString());
                    CMParSeq<Integer> s1 = e1.before(seqPars, Operator.STATEMENT(runtime));
                    assertEquals("(1. 2. 3. {4; 5})", s1.toString());
                    CMParSeq<Integer> s12 = e1.inParallelWith(seqPars, Operator.STATEMENT(runtime));
                }
             }
            """;

    @DisplayName("class reference cycles, 2")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
    }
}