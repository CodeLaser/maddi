package org.e2immu.analyzer.modification.link.impl.large;

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
            import java.util.stream.Stream;
            
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
                    Expression combine(List<Expression> templates);
                    Expression combine(Expression template1, Expression template2);
                }
                interface CMParSeq<T> {
                    Runtime runtime(); 
                    int size(); 
                    Expression template(); 
                    List<T> toList();
                    CMParSeq<T> before(CMParSeq<T> other, Operator operator);
                    CMParSeq<T> inParallelWith(CMParSeq<T> other, Operator operator);
                    boolean contains(T t);
                }
                interface Parallel<T extends Comparable<? super T>> extends CMParSeq<T> { }
                interface Sequential<T extends Comparable<? super T>> extends CMParSeq<T> { }
                List<T> elements;
                Runtime runtime;
                Expression expression;
                record EmptyParSeq<T extends Comparable<? super T>>(Runtime runtime, Expression template) 
                        implements Parallel<T>, Sequential<T> {
                }
                record SeqElements<T extends Comparable<? super T>>(Runtime runtime,
                      List<T> elements, Expression template) implements Sequential<T> {
                }
                static <X> List<X> add(X t, List<X> l1) {
                    return Stream.concat(Stream.of(t), l1.stream()).toList();
                }
                static <T extends Comparable<? super T>> CMParSeq<T> create(Runtime runtime, List<Parallel<T>> newPars, Operator operator) {
                    assert !newPars.isEmpty();
                    if (newPars.size() == 1) {
                        return newPars.get(0);
                    }
                    Expression template = operator.combine(newPars.stream().map(CMParSeq::template).toList());
                    if (newPars.stream().allMatch(np -> np instanceof ParSeqElement<T>)) {
                        return new SeqElements<>(runtime, newPars.stream()
                                .map(p -> ((ParSeqElement<T>) p).t()).toList(), template);
                    }
                    return new SeqPars<>(runtime, newPars, template);
                }
                record ParSeqElement<T>(Runtime runtime, T t, Expression template) implements Parallel<T> {
                    @Override
                    public CMParSeq<T> withTemplate(Expression template) {
                        return new ParSeqElement<>(runtime, t, template);
                    }
            
                    @Override
                    public CMParSeq<T> before(CMParSeq<T> other, Operator operator) {
                        if (equals(other)) return this; // Note that this requires the same template
                        Expression newTemplate = operator.combine(template, other.template());
                        if (other instanceof EmptyParSeq<T>) {
                            return withTemplate(newTemplate);
                        }
                        if (other instanceof ParSeqElement<T> element) {
                            // e1 -> e2 = Seq(e1, e2), with e1 != e2
                            if (t.equals(element.t)) {
                                return new ParSeqElement<>(runtime, t, newTemplate);
                            }
                            return new SeqElements<>(runtime, List.of(t, element.t), newTemplate);
                        }
                        if (other instanceof SeqElements<T> seq) {
                            // e1 -> e2, e3, e4, ... at least one of e2, e3, ... != e1
                            List<T> keep = seq.elements().stream().filter(t -> !contains(t)).toList();
                            return new SeqElements<>(runtime, add(t, keep), newTemplate);
                        }
                        if (other instanceof SeqPars<T> seqPars) {
                            // TestParSeqElement, test3()
                            // e1 -> (e2. e3. {e4; e5}) == (e1. e2. e3. {e4; e5})
                            if (!seqPars.contains(t)) {
                                return new SeqPars<>(runtime, add(this, seqPars.pars()), newTemplate);
                            }
                            // e1 -> (e2. e1. {e4; e5}) == (e1. e2. {e4; e5})  (remove)
                            // e1 -> (e2. e3. {e1; e4; e5}) == (e1. e2. {e4; e5})
                            List<Parallel<T>> newPars = Stream.concat(Stream.of(this),
                                    seqPars.pars().stream().flatMap(p -> toParallelStreamWithoutMe(p, operator))).toList();
                            return create(runtime, newPars, operator);
                        }
                        if (other instanceof ParSeqs<T> ps) {
                            if (!ps.contains(t)) {
                                // e1 -> {e2, e3}, with e2, e3 all != e1: [e1, {e2, e3}]
                                return new SeqPars<>(runtime, List.of(this, ps), newTemplate);
                            }
                            List<Parallel<T>> newPars = Stream.concat(Stream.of(this),
                                    ps.parSeqs().stream().flatMap(p -> toParallelStreamWithoutMe(p, operator))).toList();
                            return create(runtime, newPars, operator);
                        }
                        throw new UnsupportedOperationException("THERE_SHOULD_BE_NO_OTHER_OPTION");
                    }
                    public Stream<Parallel<T>> toParallelStreamWithoutMe(CMParSeq<T> p, Operator operator) {
                        if (equals(p) || p instanceof ParSeqElement<T> element && t.equals(element.t)) {
                            return Stream.of();
                        }
                        throw new UnsupportedOperationException();
                    }
                }
            
                record ParSeqs<T extends Comparable<? super T>>(Runtime runtime,
                                                List<CMParSeq<T>> parSeqs,
                                                Expression template,
                                                Operator operator) implements Parallel<T> {
                }
                record SeqPars<T extends Comparable<? super T>>(Runtime runtime,
                                                                List<Parallel<T>> pars,
                                                                Expression template) implements Sequential<T> {
                    @Override
                    public boolean contains(T t) {
                        return pars.stream().anyMatch(par -> par.contains(t));
                    }
                    
                }
                static final ParSeqElement<Integer> e1 = new ParSeqElement<>(runtime, 1, expression);
                static final ParSeqElement<Integer> e2 = new ParSeqElement<>(runtime, 2, expression);
                static final ParSeqElement<Integer> e3 = new ParSeqElement<>(runtime, 3, expression);
                static final ParSeqElement<Integer> e4 = new ParSeqElement<>(runtime, 4, expression);
                static final ParSeqElement<Integer> e5 = new ParSeqElement<>(runtime, 5, expression);
            
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
