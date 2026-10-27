/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
