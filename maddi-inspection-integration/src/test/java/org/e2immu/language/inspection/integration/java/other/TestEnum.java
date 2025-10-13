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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestEnum extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;

            public class X {
                @interface FieldAnnotation { }
                enum State { START,
                 @FieldAnnotation
                 BUSY,
                 END }

                int method(State state) {
                   return switch(state) {
                       case START -> 3;
                       case END -> 4;
                       default -> 0;
                   };
                }

                int method2(State state) {
                   switch(state) {
                       case START:
                           System.out.println("start");
                           break;
                       case END:
                           System.out.println("end");
                           break;
                   }
                   return -1;
                }

                int method3(State state) {
                    if (State.BUSY.equals(state)) {
                        return 3;
                    }
                    return 2;
                }

                private <T extends Enum<T>> T getEnum(Class<T> enumType, String string) {
                	if (string == null || string.isEmpty()) {
                		return null;
                	}
                	return T.valueOf(enumType, string.toUpperCase());
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        assertTrue(typeInfo.hasImplicitParent());
        TypeInfo state = typeInfo.findSubType("State");
        assertTrue(state.hasImplicitParent());
        assertEquals("[BUSY, END, START]", state.fields().stream().map(FieldInfo::name).sorted().toList().toString());
        FieldInfo BUSY = state.getFieldByName("BUSY", true);
        assertEquals(1, BUSY.annotations().size());
        assertEquals("Type Enum<a.b.X.State>", state.parentClass().toString());
        TypeInfo enumType = state.parentClass().typeInfo();
        assertEquals("Type Enum<E extends Enum<E>>", enumType.asParameterizedType().toString());

        FieldInfo start = state.getFieldByName("START", true);
        assertEquals("Type a.b.X.State", start.type().toString());
    }

}
