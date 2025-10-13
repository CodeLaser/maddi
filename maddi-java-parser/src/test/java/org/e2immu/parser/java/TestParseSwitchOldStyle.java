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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle;
import org.e2immu.language.cst.api.statement.SwitchStatementOldStyle;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSwitchOldStyle extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                switch(args.length) {
                  case 0:
                    System.out.println("zero!");
                  case 1, 2:
                  case 3:
                    System.out.println("less than 3");
                    return;
                  default:
                    System.out.println("all the rest");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof SwitchStatementOldStyle sso) {
            assertEquals("switch(args.length){case 0:System.out.println(\"zero!\");case 1:case 2:case 3:System.out.println(\"less than 3\");return;default:System.out.println(\"all the rest\");}",
                    sso.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }


    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.analyser.resolver.testexample;

            public class OldSwitchStatement_0 {

                public static String method(int dataType) {
                    String s;
                    a:
                    switch (dataType) {

                        case 3: {
                            s = "x";
                            break;
                        }

                        case 4:
                            s = "z";
                            b:
                            break a;

                        default:
                            s = "y";

                    }
                    return s;
                }

            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(1) instanceof SwitchStatementOldStyle sso) {
            assertEquals("""
                            a: switch(dataType){case 3:{s="x";break;}case 4:s="z";b: break a;default:s="y";}\
                            """,
                    sso.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
              public static void main(String[] args) {
                switch(args.length) {
                  case 0:
                  case 1, 2:
                  case 3:
                  default:
                }
              }
            }
            """;

    // IMPORTANT: we reduce this one to an empty (new-style) switch, because there are no statements
    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof SwitchStatementNewStyle ssns) {
            assertEquals("switch(args.length){}",
                    ssns.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }


}
