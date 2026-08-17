package org.e2immu.analyzer.modification.link.impl.large;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/*
 A self-contained distillation of the io.codelaser...parseq linking pathology (deep recursive generic
 structures carried as record fields, flowed through Map<T,Info<T>>, decomposed by instanceof + recursion,
 recombined). No proprietary code. Times PrepAnalyzer + LinkComputer to expose the link-count explosion the
 shared-variable engine is meant to bound.
 */
public class TestDeepStructureBench extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.*;
            class X<T extends Comparable<? super T>> {
                sealed interface CM<T> permits Elt, Seq {}
                record Expr(String s) {}
                record Elt<T>(T t, Expr template) implements CM<T> {}
                record Seq<T>(List<CM<T>> pars, Expr template) implements CM<T> {}
                record Info<T>(T t, int pos, CM<T> largest, Expr template) {
                    Info<T> merge(Info<T> other, Expr op) {
                        return new Info<>(t, pos + other.pos(), largest, new Expr(template.s() + op.s()));
                    }
                }

                int fill(Map<T, Info<T>> map, int index, CM<T> parSeq) {
                    if (parSeq instanceof Elt<T> e) {
                        map.put(e.t(), new Info<>(e.t(), index, e, e.template()));
                        return 1;
                    }
                    if (parSeq instanceof Seq<T> seq) {
                        int cnt = 0;
                        for (CM<T> par : seq.pars()) {
                            int size = fill(map, index + cnt, par);
                            cnt += size;
                        }
                        return cnt;
                    }
                    throw new UnsupportedOperationException();
                }

                Map<T, Info<T>> compute(CM<T> parSeq) {
                    Map<T, Info<T>> map = new HashMap<>();
                    fill(map, 0, parSeq);
                    return Map.copyOf(map);
                }

                Map<T, Info<T>> join(Map<T, Info<T>> mapLeft, Map<T, Info<T>> mapRight, Expr op) {
                    Map<T, Info<T>> map = new HashMap<>();
                    Set<T> rightKeys = new HashSet<>(mapRight.keySet());
                    for (Info<T> i1 : mapLeft.values()) {
                        Info<T> i2 = mapRight.get(i1.t());
                        if (i2 != null) {
                            rightKeys.remove(i1.t());
                            map.put(i1.t(), new Info<>(i1.t(), i1.pos() + i2.pos(), i1.largest(), i1.merge(i2, op).template()));
                        } else {
                            map.put(i1.t(), new Info<>(i1.t(), i1.pos(), i1.largest(), i1.template()));
                        }
                    }
                    for (T t : rightKeys) {
                        Info<T> i2 = mapRight.get(t);
                        map.put(t, new Info<>(t, i2.pos(), i2.largest(), i2.template()));
                    }
                    return map;
                }

                CM<T> before(CM<T> left, CM<T> right, Expr op) {
                    Map<T, Info<T>> ml = compute(left);
                    Map<T, Info<T>> mr = compute(right);
                    Map<T, Info<T>> joined = join(ml, mr, op);
                    List<CM<T>> out = new ArrayList<>();
                    for (Info<T> info : joined.values()) {
                        out.add(new Elt<>(info.t(), info.template()));
                    }
                    return new Seq<>(out, op);
                }
            }
            """;

    @Test
    public void bench() {
        long t0 = System.nanoTime();
        TypeInfo x = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(x);
        long t1 = System.nanoTime();
        new LinkComputerImpl(javaInspector).doPrimaryType(x);
        long t2 = System.nanoTime();
        System.out.println("PROBE prep=" + (t1 - t0) / 1_000_000 + "ms  link=" + (t2 - t1) / 1_000_000 + "ms");
    }
}
