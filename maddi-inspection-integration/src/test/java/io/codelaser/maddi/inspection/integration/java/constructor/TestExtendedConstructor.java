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

package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestExtendedConstructor extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;

            import java.util.Map;
            import java.util.HashMap;

            class C {
            private Map<String, String> test() {
                return new HashMap<String, String>() {
                    {
                        put("x", "abc");
                    }
                };

            }

            private Map<String, String> test2() {
                return new HashMap<>() {
                    {
                        put("y", "12345");
                    }
                };
            }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo test = typeInfo.findUniqueMethod("test", 0);
        if (test.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
            assertEquals("a.b.C.$0", cc.anonymousClass().fullyQualifiedName());
            MethodInfo constructor = cc.anonymousClass().findConstructor(0);
            assertEquals("a.b.C.$0.<init>()", constructor.fullyQualifiedName());
        } else fail();

        MethodInfo test2 = typeInfo.findUniqueMethod("test2", 0);
        if (test2.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof ConstructorCall cc) {
            assertNotNull(cc.anonymousClass());
            assertEquals("Type java.util.HashMap<String,String>", cc.anonymousClass().parentClass().toString());
        } else fail();
    }

}
