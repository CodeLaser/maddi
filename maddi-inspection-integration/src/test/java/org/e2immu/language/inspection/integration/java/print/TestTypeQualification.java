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

package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.print.FormatterImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTypeQualification extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.Date;
            class X {
                public java.sql.Date method(Date date) {
                   return new java.sql.Date(date.getTime());
                }
            }
            """;

    @Language("java")
    public static final String OUTPUT1 = """
            package a.b;
            import java.util.Date;
            class X {public java.sql.Date method(Date date) { return new java.sql.Date(date.getTime()); } }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1, JavaInspectorImpl.DETAILED_SOURCES);
        assertEquals("""
                [TypeReference[typeInfo=java.sql.Date, typeReferenceNature=FULLY_QUALIFIED], \
                TypeReference[typeInfo=java.util.Date, typeReferenceNature=EXPLICIT], \
                TypeReference[typeInfo=java.sql.Date, typeReferenceNature=FULLY_QUALIFIED]]\
                """, X.typesReferenced(null).toList().toString());

        ImportComputer importComputer = javaInspector.importComputer(4, null);
        Qualification qualification = javaInspector.runtime().qualificationQualifyFromPrimaryType();
        assertNotNull(qualification);
        ImportComputer.Result result = importComputer.go(X.compilationUnit(), qualification);
        assertEquals(1, result.imports().size());
        assertEquals("java.util.Date", result.imports().getFirst().importString());

        assertEquals(OUTPUT1, javaInspector.print2(X.compilationUnit()));
    }
}
