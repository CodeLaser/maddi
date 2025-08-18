package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImportComputer extends CommonTest {


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

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        ImportComputer importComputer = javaInspector.importComputer(4);
        Set<String> imports = importComputer.go(X, javaInspector.runtime().qualificationQualifyFromPrimaryType()).imports();
        assertEquals("[java.util.Date]", imports.toString());
    }
}
