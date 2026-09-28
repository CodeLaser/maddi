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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * #66: a switch entry's labels read variables too. For Java that is an enum constant (a static field read); K2
 * lowers a subject-less Kotlin {@code when} to a switch over {@code true} whose labels are arbitrary expressions,
 * and a consumer asking "which variables does this switch read" missed them.
 */
public class TestSwitchLabelVariables extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              enum Kind { CONST, VAR }
              static String name(Kind kind, String fallback) {
                switch (kind) {
                  case CONST -> { return "constant"; }
                  case VAR -> { return fallback; }
                  default -> { return "?"; }
                }
              }
            }
            """;

    @DisplayName("the enum labels of a switch are among its variables")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo name = typeInfo.findUniqueMethod("name", 2);
        Statement first = name.methodBody().statements().getFirst();
        SwitchStatementNewStyle ssn = (SwitchStatementNewStyle) first;
        List<String> all = ssn.variableStreamDescend().map(Variable::simpleName).distinct().sorted().toList();
        assertEquals(List.of("CONST", "VAR", "fallback", "kind"), all);
        // the label alone, from the entry
        List<String> entry0 = ssn.entries().getFirst().variableStreamDescend().map(Variable::simpleName).toList();
        assertEquals(List.of("CONST"), entry0);
    }
}
