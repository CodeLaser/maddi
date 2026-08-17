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
import org.e2immu.language.cst.api.expression.StringConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.impl.output.QualificationImpl;
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
