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
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.IfElseStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseIfElse extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                if(args.length == 0) {
                  System.out.println("Empty");
                } else if(args.length == 1) {
                  System.out.println("One");
                  System.out.println("more");
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
            if (ifElse.block().statements().get(0) instanceof ExpressionAsStatement eas) {
                assertEquals("0.0.0", eas.source().index());
            } else fail();
            if (ifElse.elseBlock().statements().get(0) instanceof IfElseStatement ifElse2) {
                assertEquals("0.1.0", ifElse2.source().index());
                if (ifElse2.block().statements().get(0) instanceof ExpressionAsStatement eas2) {
                    assertEquals("0.1.0.0.0", eas2.source().index());
                } else fail();
                if (ifElse2.block().statements().get(1) instanceof ExpressionAsStatement eas2) {
                    assertEquals("0.1.0.0.1", eas2.source().index());
                } else fail();
                assertTrue(ifElse2.elseBlock().isEmpty());
            } else fail();
        } else fail();
    }
}
