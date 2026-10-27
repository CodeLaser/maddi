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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
