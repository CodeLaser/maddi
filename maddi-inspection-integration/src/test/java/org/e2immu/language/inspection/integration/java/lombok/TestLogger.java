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

package org.e2immu.language.inspection.integration.java.lombok;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLogger extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            import lombok.extern.slf4j.Slf4j;
            
            @Slf4j
            public class X {
                void method(int i) {
                    log.info("I is {}", i);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("log", true);
        assertEquals("org.slf4j.Logger", fieldInfo.type().typeInfo().fullyQualifiedName());
        assertEquals("LoggerFactory.getLogger(X.class)", fieldInfo.initializer().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package org.e2immu.test;
            
            import lombok.extern.java.Log;
            
            @Log
            public class X {
                void method(int i) {
                    log.info("I is " + i);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2,
                new JavaInspectorImpl.ParseOptionsBuilder().setLombok(true).build());
        FieldInfo fieldInfo = typeInfo.getFieldByName("log", true);
        assertEquals("java.util.logging.Logger", fieldInfo.type().typeInfo().fullyQualifiedName());
        assertEquals("Logger.getLogger(X.class.getName())", fieldInfo.initializer().toString());
    }

}
