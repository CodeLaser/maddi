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

import org.e2immu.language.cst.api.expression.BooleanConstant;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseFor extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                for(int i=0; i<args.length; i++) {
                  System.out.println(args[i]);
                }
              }
            }
            """;


    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof ForStatement s) {
            assertEquals(1, s.initializers().size());
            assertInstanceOf(LocalVariableCreation.class, s.initializers().get(0));
            assertEquals("int i=0;", s.initializers().get(0).toString());

            assertEquals("i<args.length", s.expression().toString());

            assertEquals("i++", s.updaters().get(0).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class C {
              public static void main(String[] args) {
                for(int i=0, j=10; i<args.length && j>0; i++, --j) {
                  System.out.println(args[i]);
                }
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof ForStatement s) {
            assertEquals(1, s.initializers().size());
            assertInstanceOf(LocalVariableCreation.class, s.initializers().get(0));
            assertEquals("int i=0,j=10;", s.initializers().get(0).toString());

            assertEquals("i<args.length&&j>0", s.expression().toString());
            assertEquals("j>=1&&-1+args.length>=i", runtime.sortAndSimplify(true, s.expression()).toString());

            assertEquals("i++", s.updaters().get(0).toString());
            assertEquals("--j", s.updaters().get(1).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            class C {
              public static void main(String[] args) {
                int i, j;
                for(i=0, j=10; i<args.length && j>0; ) {
                  System.out.println(args[i]);
                  i++;
                  j -= 2;
                }
              }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        assertEquals(2, main.methodBody().size());
        if (main.methodBody().statements().get(1) instanceof ForStatement s) {
            assertEquals(2, s.initializers().size());
            assertEquals("i<args.length&&j>0", s.expression().toString());
            assertEquals("j>0&&i<args.length", runtime.sortAndSimplify(false, s.expression()).toString());
            assertTrue(s.updaters().isEmpty());
        } else fail();
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class C {
              public static void main(String[] args) {
                int i = 0, j = 10;
                for( ; ; ) {
                  if(i >= args.length || j <= 0) break;
                  if(i == 1) continue;
                  System.out.println(args[i]);
                  i++;
                  j -= 2;
                }
              }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = parse(INPUT4);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        assertEquals(2, main.methodBody().size());
        if (main.methodBody().statements().get(1) instanceof ForStatement s) {
            assertTrue(s.initializers().isEmpty());
            assertTrue(s.expression() instanceof BooleanConstant bc && bc.constant());
            assertTrue(s.updaters().isEmpty());

            if (s.block().statements().get(0) instanceof IfElseStatement ifElse) {
                assertInstanceOf(BreakStatement.class, ifElse.block().statements().get(0));
            } else fail();
            if (s.block().statements().get(1) instanceof IfElseStatement ifElse) {
                assertInstanceOf(ContinueStatement.class, ifElse.block().statements().get(0));
            } else fail();
        } else fail();
    }
}
