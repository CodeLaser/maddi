package org.e2immu.language.inspection.integration.java.constructor;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecordConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public record X(String s1, String s2, String s3) {
            	public static final int NUMBER_OF_COLUMNS = 3;
            	public X(String s1, String s2, String s3) {
            		this.s1 = s1 == null ? "": s1;
            		this.s2 = s2 == null ? "": s2;
            		this.s3 = s3 == null ? "": s3;
            	}
            	public String[] getArrayOfValues() {
            		return new String[] { s1, s2, s3 };
            	}
            }
            
            
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        // identical signature, therfore: main constructor
        assertEquals(1, typeInfo.constructors().size());
    }

}
