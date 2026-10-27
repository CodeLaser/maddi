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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(INPUT1.replaceAll("\\s+", ""), printed.replaceAll("\\s+", ""));
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
            // y is a primary type, but it cannot be public
            class Y {
                int i;
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

        assertEquals(2, typeInfo.compilationUnit().types().size());
        TypeInfo Y = typeInfo.compilationUnit().types().getLast();
        assertEquals(" y is a primary type, but it cannot be public", Y.comments().getFirst().comment());
        assertTrue(Y.isPrimaryType());

        String printed = javaInspector.print2(typeInfo.compilationUnit());
        assertEquals(INPUT2.replaceAll("\\s+", ""), printed.replaceAll("\\s+", ""));
    }
}
