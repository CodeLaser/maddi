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

package io.codelaser.maddi.modification.prepwork.variable;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.modification.prepwork.CommonTest;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #74: a local assigned or read inside a switch EXPRESSION's arms is assigned or read in the statement that holds
 * the switch expression, and so in every later statement. Found by the Kotlin tier (a `when` used as a value is
 * a switch expression), through the Java twins.
 */
public class TestSwitchExpressionLocals extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int c(int i) { int x = 0; int r = switch (i) { case 0 -> { x = 5; yield 1; } default -> 2; }; return r + x; }
                int d(int i) { int x = 0; return switch (i) { case 0 -> { x = 5; yield x + 1; } default -> x; }; }
            }
            """;

    private static String summary(MethodInfo methodInfo) {
        return summary(VariableDataImpl.of(methodInfo));
    }

    private static String summary(VariableData vd) {
        return vd.variableInfoStream()
                .filter(vi -> !(vi.variable() instanceof This) && !(vi.variable() instanceof FieldReference))
                .sorted(Comparator.comparing(vi -> vi.variable().simpleName()))
                .map(vi -> vi.variable().simpleName() + ": " + vi.assignments() + " | R " + vi.reads())
                .collect(Collectors.joining("\n"));
    }

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        MethodInfo c = X.findUniqueMethod("c", 1);
        MethodInfo d = X.findUniqueMethod("d", 1);
        // the arm's `x = 5` is an assignment inside statement 1, at the arm's own index: conditional, as an
        // if-statement's branch assignment is
        assertEquals("""
                i: D:-, A:[] | R 1
                r: D:1, A:[1] | R 2
                return c: D:-, A:[2] | R -
                x: D:0, A:[0, 1.0.0] | R 2""", summary(c));
        // `x` is read in both arms, and assigned in the first
        assertEquals("""
                i: D:-, A:[] | R 1
                return d: D:-, A:[1] | R -
                x: D:0, A:[0, 1.0.0] | R 1.0.1, 1.10""", summary(d));
    }
}
