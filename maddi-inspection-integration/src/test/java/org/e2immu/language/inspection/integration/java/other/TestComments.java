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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
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
                // at end of class
            }
            // at end of CU
            /* there are two comments */
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

        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        assertEquals(" nothing here", method.methodBody().trailingComments().getFirst().comment());

        assertEquals(" at end of class", typeInfo.trailingComments().getFirst().comment());
        assertEquals(" at end of CU", typeInfo.compilationUnit().trailingComments().getFirst().comment());

        String printed = javaInspector.print2(typeInfo.compilationUnit());
        assertEquals(INPUT1, printed);
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            // comment\tbefore import
            import java.util.List;
            /* a comment\tafter import */
            
            public class X {
                void methodWithComments(List<String> list) {
                    // comment pre-block
                    {
                        System.out.println("?");
                    }
                    // nothing\t here
                }
            }
            """;

    @DisplayName("tabs in comments are expanded")
    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        List<Comment> comments = typeInfo.comments();
        assertEquals(1, comments.size());
        // there appears to be actual alignment to the next tab position
        assertEquals(" a comment\tafter import ", comments.getFirst().comment());
        CompilationUnit compilationUnit = typeInfo.compilationUnit();
        assertEquals(1, compilationUnit.importStatements().size());
        List<Comment> importComments = compilationUnit.importStatements().getFirst().comments();
        assertEquals(1, importComments.size());
        assertEquals(" comment\tbefore import", importComments.getFirst().comment());
        MethodInfo method = typeInfo.findUniqueMethod("methodWithComments", 1);
        assertEquals(" nothing\t here", method.methodBody().trailingComments().getFirst().comment());
        Block block = (Block) method.methodBody().statements().getFirst();
        assertEquals(" comment pre-block", block.comments().getFirst().comment());
    }

}
