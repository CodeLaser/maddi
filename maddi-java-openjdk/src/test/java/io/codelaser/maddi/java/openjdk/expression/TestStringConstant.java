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

package io.codelaser.maddi.java.openjdk.expression;

import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestStringConstant extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStringConstant.class);

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class C { String s = "[\\\\d]{5}\\\\.xml$"; }
            """;

    @Test
    public void test1() {
        LOGGER.info(INPUT1);
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        FieldInfo s = typeInfo.getFieldByName("s", true);
        if (s.initializer() instanceof StringConstant sc) {
            assertEquals("\"[\\\\d]{5}\\\\.xml$\"", sc.print(QualificationImpl.SIMPLE_NAMES).toString());
        } else fail();
        String out = print2(typeInfo.compilationUnit());
        assertEquals(INPUT1, out);
    }


    public static final String INPUT2 = "package a.b;\npublic class X { String [] s = { \"test1\", \"\\\\'test2\\\\'\", \"\\\\'test3\", \"\\\\\\\"test4\" }; }\n";

    @Test
    public void test2() {
        LOGGER.info(INPUT2);
        TypeInfo X = scan("a.b.X", INPUT2);
        FieldInfo s = X.getFieldByName("s", true);
        if (s.initializer() instanceof ArrayInitializer ai) {
            assertEquals("test1 " + '\\' + "'test2" + '\\' + "' \\'test3 \\\"test4",
                    ai.expressions().stream().map(e -> ((StringConstant) e).constant())
                            .collect(Collectors.joining(" ")));
        } else fail();
        String out = print2(X.compilationUnit());
        assertEquals(INPUT2, out);
    }

    public static final String INPUT3 = "package a.b;\npublic class X {public void parse() { String s = \"a \\\" and \\\" b\"; } }\n";

    @Test
    public void test3() {
        LOGGER.debug(INPUT3);
        TypeInfo typeInfo = scan("a.b.X", INPUT3);
        MethodInfo parse = typeInfo.findUniqueMethod("parse", 0);
        if (parse.methodBody().statements().getFirst() instanceof LocalVariableCreation lvc) {
            if (lvc.localVariable().assignmentExpression() instanceof StringConstant sc) {
                assertEquals("a \" and \" b", sc.constant());
            } else fail();
        } else fail();
        String out = print2(typeInfo.compilationUnit());
        assertEquals(INPUT3, out);
    }

}
