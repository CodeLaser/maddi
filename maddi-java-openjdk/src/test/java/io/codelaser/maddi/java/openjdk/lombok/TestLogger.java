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

package io.codelaser.maddi.java.openjdk.lombok;

import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLogger extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package io.codelaser.maddi.test;

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
        TypeInfo typeInfo = scan("io.codelaser.maddi.test.X", INPUT1);
        FieldInfo fieldInfo = typeInfo.getFieldByName("log", true);
        assertEquals("org.slf4j.Logger", fieldInfo.type().typeInfo().fullyQualifiedName());
        assertEquals("LoggerFactory.getLogger(X.class)", fieldInfo.initializer().toString());
    }


    @Language("java")
    private static final String INPUT2 = """
            package io.codelaser.maddi.test;

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
        TypeInfo typeInfo = scan("io.codelaser.maddi.test.X", INPUT2);
        FieldInfo fieldInfo = typeInfo.getFieldByName("log", true);
        assertEquals("java.util.logging.Logger", fieldInfo.type().typeInfo().fullyQualifiedName());
        assertEquals("Logger.getLogger(X.class.getName())", fieldInfo.initializer().toString());
    }

}
