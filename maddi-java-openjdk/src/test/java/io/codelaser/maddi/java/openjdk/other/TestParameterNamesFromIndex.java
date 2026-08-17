package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.e2immu.language.java.openjdk.CommonTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestParameterNamesFromIndex extends CommonTest {

    @Test
    public void test() throws IOException {
        // inject a faithful name for Integer.parseInt(String, int); the key uses erasedForFQN().fullyQualifiedName(),
        // which abbreviates java.lang, hence "String"
        parameterNameIndex = ParameterNameIndex.read(new StringReader(
                "java.lang.Integer.parseInt(String,int)\ts,radix\n"));
        scan("a.b.X", "package a.b; class X { int m() { return Integer.parseInt(\"3\", 10); } }");

        TypeInfo integer = classSymbolScanner.getType("java.lang.Integer");
        MethodInfo parseInt = integer.findUniqueMethod("parseInt", 2);
        assertEquals("s", parseInt.parameters().get(0).name());
        assertEquals("radix", parseInt.parameters().get(1).name());
    }
}
