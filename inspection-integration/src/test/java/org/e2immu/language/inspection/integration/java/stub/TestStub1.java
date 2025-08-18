package org.e2immu.language.inspection.integration.java.stub;

import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestStub1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package org.e2immu.test;
            
            public class X {
                private Y y;
                public X(Y y) {
                    this.y = y;
                }
            }
            """;


    @Test
    public void test() {
        assertThrows(Summary.FailFastException.class, () ->
                javaInspector.parse(INPUT1, new JavaInspectorImpl.ParseOptionsBuilder()
                        .setFailFast(true).build()));
    }

}
