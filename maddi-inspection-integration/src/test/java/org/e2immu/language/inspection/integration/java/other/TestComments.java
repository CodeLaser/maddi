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
