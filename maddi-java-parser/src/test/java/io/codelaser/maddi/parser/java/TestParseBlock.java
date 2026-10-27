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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestParseBlock extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                if(args.length == 0) {
                  {
                  System.out.println("Empty");
                  }
                } else if(args.length == 1) {
                  System.out.println("more");
                }
                {
                  System.out.println("One");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof IfElseStatement ifElse) {
            assertEquals("0", ifElse.source().index());
            Statement s000 = ifElse.block().statements().get(0);
            if (s000 instanceof Block block) {
                assertEquals("0.0.0", block.source().index());
                Statement s00000 = block.statements().get(0);
                assertEquals("0.0.0.0.0", s00000.source().index());
            } else fail();
            if (ifElse.elseBlock().statements().get(0) instanceof IfElseStatement ifElse2) {
                assertEquals("0.1.0", ifElse2.source().index());
                if (ifElse2.block().statements().get(0) instanceof ExpressionAsStatement eas2) {
                    assertEquals("0.1.0.0.0", eas2.source().index());
                } else fail();
            } else fail();
        } else fail();
        Statement s1 = main.methodBody().statements().get(1);
        if (s1 instanceof Block block) {
            assertEquals("1", block.source().index());
            Statement s100 = block.statements().get(0);
            assertEquals("1.0.0", s100.source().index());
        } else fail();
    }

    @Language("java")
    String INPUT1 = """
            package a.b;
            class X {
                int method(String s, int k) {
                   if(s.length()==1)
                   // comment
                   {
                      return k;
                      // this was i
                   }
                   int i = s.length();
                   return 2*i;
                }
            }
            """;

    @DisplayName("who owns this comment?")
    @Test
    public void test1() {
        TypeInfo typeInfo = parse(INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        IfElseStatement ifElse = (IfElseStatement) method.methodBody().statements().getFirst();
        assertEquals(1, ifElse.block().comments().size());
        assertEquals(" comment", ifElse.block().comments().getFirst().comment());
        assertEquals(1, ifElse.block().trailingComments().size());
        assertEquals(" this was i", ifElse.block().trailingComments().getFirst().comment());
    }
}
