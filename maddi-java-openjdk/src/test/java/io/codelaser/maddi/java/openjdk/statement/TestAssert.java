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

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.AssertStatement;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssert extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public int length(String[] args) {
                assert args != null;
                int a = args.length;
                assert a > 0 : "expect a to be >0, but got "+a;
                return a+1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo methodInfo = typeInfo.methods().getFirst();


        Block block = methodInfo.methodBody();
        assertEquals(4, block.size());
        if (block.statements().get(0) instanceof AssertStatement as) {
            assertTrue(as.message().isEmpty());
            assertEquals("args!=null", as.expression().toString());
        } else fail();
        if (block.statements().get(2) instanceof AssertStatement as) {
            assertInstanceOf(BinaryOperator.class, as.message());
            assertEquals("\"expect a to be >0, but got \"+a", as.message().toString());
            assertEquals("a>0", as.expression().toString());
        } else fail();
    }
}
