package org.e2immu.analyzer.modification.link.impl2;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class Test3 extends CommonTest {

    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.util.List;
            import java.util.Objects;
            
            public class C<T extends Comparable<? super T>> {
                interface MethodInfo { }
                interface Precedence { }
                interface Runtime {
                    Precedence precedenceAdditive();
                    MethodInfo plusOperatorInt();
                    Expression newEmptyExpression();
                }
                interface Expression { 
                    String name();
                    Precedence precedence();
                }
                interface Operator {
                    record Binary(Runtime runtime, MethodInfo methodInfo, Precedence precence) implements Operator {}
                    static Operator STATEMENT(Runtime runtime) { return null; }
                }
                interface CMParSeq<T> {
                    Runtime runtime(); 
                    int size(); 
                    Expression template(); 
                    List<T> toList();
                    CMParSeq<T> before(CMParSeq<T> other, Operator operator);
                    CMParSeq<T> inParallelWith(CMParSeq<T> other, Operator operator);
                }
                interface Parallel<T extends Comparable<? super T>> extends CMParSeq<T> { }
                interface Sequential<T extends Comparable<? super T>> extends CMParSeq<T> { }
                List<T> elements;
                Runtime runtime;
                record ParSeqElement<T>(Runtime runtime, T t) implements Parallel<T> { }
            
                record ParSeqs<T extends Comparable<? super T>>(Runtime runtime,
                                                List<CMParSeq<T>> parSeqs,
                                                Expression template,
                                                Operator operator) implements Parallel<T> {
                }
                record SeqPars<T extends Comparable<? super T>>(Runtime runtime,
                                                                List<Parallel<T>> pars,
                                                                Expression template) implements Sequential<T> {
                }
                static final ParSeqElement<Integer> e1 = new ParSeqElement<>(runtime, 1);
                static final ParSeqElement<Integer> e2 = new ParSeqElement<>(runtime, 2);
                static final ParSeqElement<Integer> e3 = new ParSeqElement<>(runtime, 3);
                static final ParSeqElement<Integer> e4 = new ParSeqElement<>(runtime, 4);
                static final ParSeqElement<Integer> e5 = new ParSeqElement<>(runtime, 5);

                static void assertEquals(String s1, String s2) { assert s1.equals(s2); }
                static final Operator.Binary plus = new Operator.Binary(runtime, runtime.plusOperatorInt(),
                            runtime.precedenceAdditive());
                static final Expression T = runtime.newEmptyExpression();
                
                public void test3() {
                     ParSeqs<Integer> e45 = new ParSeqs<>(runtime, List.of(e4, e5), T, plus);
                     SeqPars<Integer> seqPars = new SeqPars<>(runtime, List.of(e2, e3, e45), T);
                     assertEquals("(2. 3. {4; 5})", seqPars.toString());
             
                     CMParSeq<Integer> s1 = e1.before(seqPars, Operator.STATEMENT(runtime));
                     assertEquals("(1. 2. 3. {4; 5})", s1.toString());
                     CMParSeq<Integer> s12 = e1.inParallelWith(seqPars, Operator.STATEMENT(runtime));
                     assertEquals("(1. 2. 3. {4; 5})", s12.toString());
             
                     CMParSeq<Integer> s2 = e3.before(seqPars, Operator.STATEMENT(runtime));
                     assertEquals("(3. 2. {4; 5})", s2.toString());
             
                     CMParSeq<Integer> s3 = e4.before(seqPars, Operator.STATEMENT(runtime));
                     assertEquals("[4, 2, 3, 5]", s3.toString());
             
                     ParSeqs<Integer> e451 = new ParSeqs<>(runtime, List.of(e4, e5, e1), T, plus);
                     SeqPars<Integer> seqPars2 = new SeqPars<>(runtime, List.of(e2, e3, e451), T);
                     assertEquals("(2. 3. {4; 5; 1})", seqPars2.toString());
             
                     CMParSeq<Integer> s4 = e1.before(seqPars2, Operator.STATEMENT(runtime));
                     assertEquals("(1. 2. 3. {4; 5})", s4.toString());
                 }
            }
            """;

    @DisplayName("null virtual field")
    @Test
    public void test6() {
        TypeInfo C = javaInspector.parse(INPUT6);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(C);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(C);
    }
}
