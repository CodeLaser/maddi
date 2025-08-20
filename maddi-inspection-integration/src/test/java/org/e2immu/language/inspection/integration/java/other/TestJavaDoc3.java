package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

public class TestJavaDoc3 extends CommonTest2 {

    @Language("java")
    String MAIN_A = """
            package a;
            public class A {

            }
            """;

    @Language("java")
    String TEST_A = """
            package a;
            /**
             * link to {@Link A}
             */
            public class TestA {
            }
            """;

    @DisplayName("Test across source sets, same package")
    @Test
    public void test4() throws IOException {
        ParseResult pr1 = init(Map.of("a.A", MAIN_A), Map.of("a.TestA", TEST_A));
        {
            TypeInfo A = pr1.findType("a.A");
            assertEquals("test-protocol:a.A", A.compilationUnit().sourceSet().name());
            assertFalse(A.compilationUnit().sourceSet().test());
        }
        {
            TypeInfo TestA = pr1.findType("a.TestA");
            assertTrue(TestA.compilationUnit().sourceSet().test());
            assertEquals("test-protocol:a.TestA", TestA.compilationUnit().sourceSet().name());
            JavaDoc.Tag tag = TestA.javaDoc().tags().getFirst();
            assertEquals("a.A", tag.resolvedReference().toString());
        }
    }
}