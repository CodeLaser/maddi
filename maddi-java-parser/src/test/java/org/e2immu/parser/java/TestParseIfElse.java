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
