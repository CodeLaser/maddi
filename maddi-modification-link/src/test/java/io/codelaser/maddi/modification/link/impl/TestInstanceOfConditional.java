package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 A pattern variable bound in the condition of a conditional EXPRESSION links to the value it was bound from, as it
 does in an `if` statement (#79).
 */
public class TestInstanceOfConditional extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                StringBuilder smartIf(Object o) { if (o instanceof StringBuilder s) return s; return null; }
                StringBuilder smartSb(Object o) { return o instanceof StringBuilder s ? s : null; }
                StringBuilder smartLocal(Object o) { StringBuilder r = o instanceof StringBuilder s ? s : null; return r; }
                StringBuilder smartElse(Object o) { return !(o instanceof StringBuilder s) ? null : s; }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        // the `if` form, which always linked
        assertEquals("[-] --> smartIf←$_ce0,smartIf←0:o,smartIf.§m≡0:o.§m",
                tlc.doMethod(X.findUniqueMethod("smartIf", 1)).toString());
        // the conditional expression: returned, via a local, and with the binding in a negated condition
        assertEquals("[-] --> smartSb←$_ce0,smartSb←0:o,smartSb.§m≡0:o.§m",
                tlc.doMethod(X.findUniqueMethod("smartSb", 1)).toString());
        assertEquals("[-] --> smartLocal←$_ce0,smartLocal←0:o,smartLocal.§m≡0:o.§m",
                tlc.doMethod(X.findUniqueMethod("smartLocal", 1)).toString());
        assertEquals("[-] --> smartElse←$_ce0,smartElse←0:o,smartElse.§m≡0:o.§m",
                tlc.doMethod(X.findUniqueMethod("smartElse", 1)).toString());
    }
}
