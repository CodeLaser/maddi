/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * do-while: the condition is evaluated after the body, so its reads carry the {@code :E} (EVAL_UPDATE) stage, and
 * because the body runs unconditionally a {@code =M} merge is produced (unlike a plain while).
 */
public class TestAssignmentsDoWhile extends CommonTest {

    @Language("java") private static final String INPUT = """
            package a.b;
            class X {
                static int m(int n) {
                    int sum = 0;
                    int i = 0;
                    do {
                        sum += i;
                        i++;
                    } while (i < n);
                    return sum;
                }
            }""";

    private static VariableInfo local(VariableData vd, String simpleName) {
        return vd.variableInfoStream().filter(x -> x.variable().simpleName().equals(simpleName))
                .findFirst().orElseThrow();
    }

    private static VariableInfo rv(VariableData vd) {
        return vd.variableInfoStream().filter(x -> x.variable() instanceof ReturnVariable).findFirst().orElseThrow();
    }

    @DisplayName("do-while: reads/assignments stages and the unconditional merge")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        MethodInfo m = X.findUniqueMethod("m", 1);
        new PrepAnalyzer(runtime).doMethod(m);
        VariableData vd = VariableDataImpl.of(m);

        VariableInfo sum = local(vd, "sum");
        assertEquals("2.0.0, 3", sum.reads().toString());
        assertEquals("D:0, A:[0, 2.0.0, 2=M]", sum.assignments().toString());

        VariableInfo i = local(vd, "i");
        // condition 'i < n' is read at 2:E (the post-body evaluation point); 'i++' both reads and assigns at 2.0.1
        assertEquals("2.0.0, 2.0.1, 2:E", i.reads().toString());
        assertEquals("D:1, A:[1, 2.0.1, 2=M]", i.assignments().toString());

        VariableInfo n = local(vd, "n");
        assertEquals("2:E", n.reads().toString());
        assertEquals("D:-, A:[]", n.assignments().toString());

        assertEquals("D:-, A:[3]", rv(vd).assignments().toString());
    }
}
