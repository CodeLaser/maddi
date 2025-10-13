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
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestComments extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            /* comment 1 before package */
            // comment 2 before package
            package a.b;
            // comment before import
            import java.util.List;
            /* comment after import */
            
            public class X {
                void method(List<String> list) {
                    // nothing here
                }
                // FIXME this comment is currently ignored, at end of class
            }
            // FIXME this comment is currently ignored, at end of CU
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        List<Comment> comments = typeInfo.comments();
        assertEquals(1, comments.size());
        assertEquals(" comment after import ", comments.getFirst().comment());
        CompilationUnit compilationUnit = typeInfo.compilationUnit();
        assertEquals(1, compilationUnit.importStatements().size());
        List<Comment> importComments = compilationUnit.importStatements().getFirst().comments();
        assertEquals(1, importComments.size());
        assertEquals(" comment before import", importComments.getFirst().comment());
        assertEquals(2, compilationUnit.comments().size());
        assertEquals(" comment 1 before package ", compilationUnit.comments().getFirst().comment());
        assertEquals(" comment 2 before package", compilationUnit.comments().getLast().comment());

    }

}
