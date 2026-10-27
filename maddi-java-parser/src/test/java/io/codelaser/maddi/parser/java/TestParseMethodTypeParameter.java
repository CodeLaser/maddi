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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseMethodTypeParameter extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              interface I {}
              <T> T method(String s) { return null; }
              <T, S extends I> T method2(S s) { return null; }
              private <T> C(T t, int i) { }
              <T> C(T t) { }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT, true);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());

        MethodInfo methodInfo2 = typeInfo.findUniqueMethod("method2", 1);
        assertEquals("Type param T", methodInfo.returnType().toString());
        assertSame(methodInfo, methodInfo.returnType().typeParameter().getOwner().getRight());
        assertEquals(2, methodInfo2.typeParameters().size());
        TypeParameter tp0 = methodInfo2.typeParameters().get(0);
        TypeParameter tp1 = methodInfo2.typeParameters().get(1);
        assertEquals("S=TP#1 in C.method2", tp1.toString());
        assertEquals("main::a.b.C.method2(main::a.b.C.I)[S]", tp1.descriptor());
        assertSame(methodInfo2, tp1.getOwner().getRight());
        assertEquals("main::a.b.C.method2(main::a.b.C.I)", methodInfo2.descriptor());
        assertTrue(tp1.isMethodTypeParameter());

        DetailedSources ds0 = tp0.source().detailedSources();
        assertEquals("6-5:6-5", ds0.detail(DetailedSources.SUCCEEDING_COMMA).compact2());
        assertNull(ds0.detail(DetailedSources.PRECEDING_COMMA));
        DetailedSources ds1 = tp1.source().detailedSources();
        assertEquals("6-5:6-5", ds1.detail(DetailedSources.PRECEDING_COMMA).compact2());
        assertNull(ds1.detail(DetailedSources.SUCCEEDING_COMMA));
    }
}
