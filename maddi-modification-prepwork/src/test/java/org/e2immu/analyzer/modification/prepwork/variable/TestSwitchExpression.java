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
import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSwitchExpression extends CommonTest {
    @Language("java")
    public static final String INPUT1 = """
            import java.util.ArrayList;import java.util.List;
            record X(List<String> list, int k) {
                int method() {
                    return switch(k) {
                        case 0 -> list.get(0).length();
                        case 1 -> {
                            System.out.println("?");
                            yield k+3;
                        }
                        default -> throw new UnsupportedOperationException();
                    };
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        new PrepAnalyzer(runtime).doPrimaryType(X);
        assertTrue(X.typeNature().isRecord());

        MethodInfo method = X.findUniqueMethod("method", 0);
        SwitchExpression switchExpression = (SwitchExpression) method.methodBody().statements().getFirst().expression();
        Statement s0 = switchExpression.entries().getFirst().statement();
        assertEquals("5-23:5-42", s0.source().compact2());
        VariableData vd0 = VariableDataImpl.of(s0);
        assertEquals("X.list, X.this, java.util.List._synthetic_list#X.list, java.util.List._synthetic_list#X.list[0]",
                vd0.knownVariableNamesToString());
        Block b1 = (Block) switchExpression.entries().get(1).statement();
        Statement s1 = b1.statements().get(1);
        assertEquals("8-17:8-26", s1.source().compact2());
        assertNotNull(VariableDataImpl.of(s1).knownVariableNamesToString());
    }
}
