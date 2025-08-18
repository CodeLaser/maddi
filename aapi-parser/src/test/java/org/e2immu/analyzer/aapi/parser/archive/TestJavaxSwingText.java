package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import javax.swing.text.JTextComponent;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaxSwingText extends CommonTest {

    @Test
    public void testJTextComponentSetText() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(JTextComponent.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("setText", 1);
        assertTrue(methodInfo.isModifying());
    }

}
