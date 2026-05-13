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

package org.e2immu.language.inspection.openjdk.statement;

import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.AssertStatement;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT), List.of()).getFirst();
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
