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

import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnonymousType extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              public void test(String s) {
                Function<Integer,String> f = new Function<>() {
                  @Override
                  public Integer apply(Integer i) {
                    return i+s;
                  }
                };
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("test", 1);
        if (methodInfo.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            assertEquals("Type java.util.function.Function<Integer,String>", cc.parameterizedType().toString());
            assertEquals(cc.parameterizedType(), cc.anonymousClass().interfacesImplemented().get(0));
            MethodInfo apply = cc.anonymousClass().findUniqueMethod("apply", 1);
            if (apply.methodBody().statements().get(0) instanceof ReturnStatement rs) {
                assertEquals("return i+s;", rs.toString());
                assertEquals("0", rs.source().index());
            }
            assertSame(methodInfo, cc.anonymousClass().enclosingMethod());
        } else fail();
    }
}
