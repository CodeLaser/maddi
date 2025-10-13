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

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignmentsNoExit extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.io.Reader;
            class X {
                public static String readLine(Reader in) throws IOException {
                    int state = 0;
                    StringBuilder str = new StringBuilder();
                    for (; ; ) {
                        in.mark(1);
                        int c = in.read();
                        switch(c) {
                            case -1:
                                switch(state) {
                                    case 0:
                                        return null;
                                    default:
                                        return str.toString();
                                }
                            case '\\n':
                                return str.toString();
                            default:
                                switch(state) {
                                    case 2:
                                    case 3:
                                        in.reset();
                                        return str.toString();
                                    default:
                                        state = 1;
                                        str.append((char) c);
                                }
                        }
                    }
                }
            }
            """;


    @DisplayName("complicated example with for(;;) and 2 switches")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("readLine", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[2.0.2.0.0.0.0, 2.0.2.0.0.0.1, 2.0.2.0.0=M, 2.0.2.0.1, 2.0.2.0.2.0.1]",
                rvVi.assignments().toString());
        // important: 2=M is absent, because there is no assignment at 2.0._ level, because there is no
        // return statement in the 'default' branch of the outer switch
        assertFalse(rvVi.hasBeenDefined("2=M"));
        assertFalse(rvVi.hasBeenDefined("3.0.0"));
        // the code still compiles because the compiler reasons "there is an exit point" rather than
        // "there is a guaranteed exit" inside the block
    }

}
