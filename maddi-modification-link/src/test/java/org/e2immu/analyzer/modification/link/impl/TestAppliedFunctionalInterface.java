package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link LinkAppliedFunctionalInterface}'s {@code searchAndExpand} path (and its {@code SearchResult}):
 * a method reference wrapped in a <em>record</em> and applied via a field access ({@code r.function.apply(s)}).
 * Because the source of the applied functional interface is the record (a non-standard functional interface), the
 * link computer must open up the record variable, find that it links to a functional-interface variable, and expand
 * it. (Ported from the analyzer's TestModificationFunctional,2 to give the link module direct coverage.)
 */
public class TestAppliedFunctionalInterface extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class X {
                record R(Function<String, Integer> function) {}
                int j;

                int go(String in) {
                    R nr = new R(this::parse);
                    return run(in, nr);
                }
                int run(String s, R r) {
                    return r.function.apply(s);
                }
                int parse(String t) {
                    j = Integer.parseInt(t);
                    return j;
                }
            }
            """;

    @DisplayName("method reference wrapped in a record, applied via field access")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer lc = new LinkComputerImpl(javaInspector);
        lc.doPrimaryType(X);

        // parse(t) modifies this.j
        MethodInfo parse = X.findUniqueMethod("parse", 1);
        MethodLinkedVariables mlvParse = parse.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> parse←this*.j", mlvParse.toString());

        // run(s, r) applies r.function (a functional-interface field inside the record r): searchAndExpand
        MethodInfo run = X.findUniqueMethod("run", 2);
        MethodLinkedVariables mlvRun = run.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-, 1:r.function*↗$_afi2] --> run←$_afi2,run↖Λ1:r.function*", mlvRun.toString());
    }
}
