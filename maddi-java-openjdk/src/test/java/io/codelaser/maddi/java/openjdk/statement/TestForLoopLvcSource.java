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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A classic for-loop's local-variable-creation initializer ({@code for (int i = 0; ...)}) must carry a source.
 */
public class TestForLoopLvcSource extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int single(int n) {
                    int sum = 0;
                    for (int i = 0; i < n; i++) {
                        sum += i;
                    }
                    return sum;
                }
                int multi(int n) {
                    int sum = 0;
                    for (int i = 0, j = n; i < j; i++, j--) {
                        sum += i;
                    }
                    return sum;
                }
            }
            """;

    private static LocalVariableCreation forInitLvc(MethodInfo methodInfo) {
        ForStatement fs = (ForStatement) methodInfo.methodBody().statements().get(1); // after 'int sum = 0;'
        Element init = fs.initializers().getFirst();
        return (LocalVariableCreation) init;
    }

    @DisplayName("classic for-loop, single declarator: the LVC has a source")
    @Test
    public void single() {
        TypeInfo X = scan("a.b.X", INPUT);
        LocalVariableCreation lvc = forInitLvc(X.findUniqueMethod("single", 1));
        assertEquals("i", lvc.localVariable().simpleName());
        assertNotNull(lvc.source(), "for-loop LVC has no source");
        assertEquals("1.?.0@5:14-5:22", lvc.source().toString()); // 'int i = 0' on line 5 (for-init pseudo-block)
    }

    @DisplayName("classic for-loop, multiple declarators: the LVC has a source spanning both")
    @Test
    public void multi() {
        TypeInfo X = scan("a.b.X", INPUT);
        LocalVariableCreation lvc = forInitLvc(X.findUniqueMethod("multi", 1));
        assertEquals("i", lvc.localVariable().simpleName());
        assertEquals(1, lvc.otherLocalVariables().size());
        assertEquals("j", lvc.otherLocalVariables().getFirst().simpleName());
        assertNotNull(lvc.source(), "for-loop LVC has no source");
        // 'int i = 0, j = n' on line 12: the source spans both declarators
        assertEquals("1.?.0@12:14-12:29", lvc.source().toString());
    }
}
