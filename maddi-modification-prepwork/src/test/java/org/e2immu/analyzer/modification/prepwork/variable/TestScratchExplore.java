package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestScratchExplore extends CommonTest {

    private void dump(String label, String input, int params) {
        TypeInfo X = javaInspector.parse(ABX, input);
        MethodInfo m = X.findUniqueMethod("m", params);
        new PrepAnalyzer(runtime).doMethod(m);
        VariableData vd = VariableDataImpl.of(m);
        System.out.println(">>> " + label);
        vd.variableInfoStream().forEach(vi -> {
            var a = vi.assignments();
            StringBuilder rd = new StringBuilder();
            for (String r : vi.reads().indices()) {
                boolean def = vi.hasBeenDefined(Util.stripStage(r));
                rd.append(r).append("->").append(def).append(def ? " " : " [!!] ");
            }
            // also test hasBeenDefined just after the switch (fictitious "9") to probe definite assignment
            System.out.println(">>>   '" + vi.variable().simpleName()
                    + "' | reads=[" + vi.reads() + "] | assign=" + a
                    + " | readDef{" + rd.toString().trim() + "} | hbd(9)=" + vi.hasBeenDefined("9"));
        });
    }

    @Language("java") private static final String OS1 =
            "package a.b; class X { static int m(int x){ int r=0; switch(x){ case 1: r=1; case 2: r=r+10; break; default: r=-1; } return r; } }";
    @Language("java") private static final String OS2 =
            "package a.b; class X { static void m(int x){ int r; switch(x){ case 1: r=1; System.out.println(r); break; case 2: r=2; System.out.println(r); break; } } }";
    @Language("java") private static final String OS3 =
            "package a.b; class X { static int m(int x){ int r; switch(x){ case 1: case 2: r=10; break; default: r=0; } return r; } }";
    @Language("java") private static final String OS5 =
            "package a.b; class X { static int m(int x){ int r; switch(x){ case 1: r=1; break; default: r=0; break; case 2: r=2; break; } return r; } }";

    @Test public void explore() {
        dump("OS1 fall-through reads prior assignment", OS1, 1);
        dump("OS2 missing default (definite assignment)", OS2, 1);
        dump("OS3 multiple labels on one case", OS3, 1);
        dump("OS5 default in the middle", OS5, 1);
    }
}
