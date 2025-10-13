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

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.SingleLineComment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTryCatch extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.io.Writer;
            class X {
                public void method(String in, Writer writer) {
                    try { //
                        writer.append("input: ").append(in);
                    } catch (IOException | RuntimeException e) {
                        System.out.println("Caught io or runtime exception!");
                        throw e;
                    } finally {
                        System.out.println("this was method1");
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().get(0);
        Statement s0 = ts.block().statements().get(0);
        assertEquals(1, s0.comments().size());
        Comment c = s0.comments().get(0);
        if(c instanceof SingleLineComment slc) {
            assertEquals("//\n", slc.print(javaInspector.runtime().qualificationQualifyFromPrimaryType()).toString());
        } else fail();
        TryStatement.CatchClause cc = ts.catchClauses().get(0);
        assertEquals("e", cc.catchVariable().simpleName());
        assertEquals("Type Exception", cc.catchVariable().parameterizedType().toString());
    }
}
