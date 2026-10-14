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

import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CodeLaser/maddi#49: a literal prints as it was written. {@code 0xFFFF} printed as {@code 65535} and
 * {@code 50_000} as {@code 50000}, which restyled every literal of code a refactoring moved. The value is the
 * same, only the spelling is kept; a literal written as its value prints keeps none.
 */
public class TestLiteralSpelling extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int hex = 0xFFFF;
                int allBits = 0xFFFFFFFF;
                int underscores = 1_000_000;
                int binary = 0b1010;
                int octal = 017;
                int min = -2147483648;
                int plain = 42;
                long longHex = 0x7fffffffffffffffL;
                long lower = 10l;
                long longUnderscores = 50_000L;
                double exponent = 1e3;
                double hexFloat = 0x1.8p1;
                float aFloat = 1.5f;
                char escaped = '\\u0041';
                char plainChar = 'A';
            }
            """;

    @DisplayName("each literal prints as written, and its value is unchanged")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        Map<String, String> printed = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            printed.put(fieldInfo.name(), fieldInfo.initializer().print(runtime.qualificationQualifyFromPrimaryType())
                    .toString());
        }
        assertEquals("""
                hex=0xFFFF
                allBits=0xFFFFFFFF
                underscores=1_000_000
                binary=0b1010
                octal=017
                min=-2147483648
                plain=42
                longHex=0x7fffffffffffffffL
                lower=10l
                longUnderscores=50_000L
                exponent=1e3
                hexFloat=0x1.8p1
                aFloat=1.5f
                escaped='\\u0041'
                plainChar='A'
                """, printed.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue() + "\n")
                .reduce("", String::concat));

        assertEquals(65535, constant(typeInfo, "hex"));
        assertEquals(-1, constant(typeInfo, "allBits"));
        assertEquals(15, constant(typeInfo, "octal"));
        assertEquals(Integer.MIN_VALUE, constant(typeInfo, "min"));
        assertEquals(1000.0, constant(typeInfo, "exponent"));
        assertEquals('A', constant(typeInfo, "escaped"));
        assertNull(((ConstantExpression<?>) typeInfo.getFieldByName("plain", true).initializer()).sourceSpelling(),
                "a literal written as its value prints keeps no spelling");
    }

    private static Object constant(TypeInfo typeInfo, String field) {
        Expression e = typeInfo.getFieldByName(field, true).initializer();
        return ((ConstantExpression<?>) e).constant();
    }
}
