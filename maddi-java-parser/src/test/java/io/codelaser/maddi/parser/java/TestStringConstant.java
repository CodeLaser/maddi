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

import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestStringConstant extends CommonTestParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStringConstant.class);

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C {
              String s = "[\\\\d]{5}\\\\.xml$";
            }
            """;

    @Test
    public void test1() {
        LOGGER.info(INPUT1);
        TypeInfo typeInfo = parse(INPUT1);
        FieldInfo s = typeInfo.getFieldByName("s", true);
        if (s.initializer() instanceof StringConstant sc) {
            assertEquals("\"[\\\\d]{5}\\\\.xml$\"", sc.print(QualificationImpl.SIMPLE_NAMES).toString());
        } else fail();
    }


    public static final String INPUT2 = "package a.b; public class X { String[] s = { \"test1\", \"\\\\'test2\\\\'\", \"\\\\'test3\", \"\\\\\\\"test4\" }; }";

    @Test
    public void test2() {
        LOGGER.info(INPUT2);
        TypeInfo X = parse(INPUT2);
        FieldInfo s = X.getFieldByName("s", true);
        if (s.initializer() instanceof ArrayInitializer ai) {
            assertEquals("test1 " + '\\' + "'test2" + '\\' + "' \\'test3 \\\"test4",
                    ai.expressions().stream().map(e -> ((StringConstant) e).constant())
                            .collect(Collectors.joining(" ")));
        } else fail();
    }

    public static final String INPUT3 = "package a.b; public class X { public void parse() { String s = \"a \\\" and \\\" b\"; } }";

    @Test
    public void test3() {
        LOGGER.debug(INPUT3);
        TypeInfo typeInfo = parse(INPUT3);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof StringConstant sc) {
                assertEquals("a \" and \" b", sc.constant());
            } else fail();
        } else fail();
    }

}
