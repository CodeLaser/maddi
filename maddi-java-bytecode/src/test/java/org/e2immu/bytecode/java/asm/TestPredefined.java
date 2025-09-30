package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class TestPredefined extends CommonJmodBaseTests {

    @Test
    public void test() {
        TypeInfo string = runtime.stringTypeInfo();
        TypeInfo string2 = compiledTypesManager.get(String.class);
        assertSame(string, string2);
        CompiledTypesManager.TypeData typeData = compiledTypesManager.typeDataOrNull("java.lang.String",
                compiledTypesManager.javaBase(), null, false);
        assertSame(string, typeData.typeInfo());
    }
}
