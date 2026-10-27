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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSealed extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            
            public sealed class Sealed_0 permits Sealed_0.Sub1, Sealed_0.Sub2 {
            
                static final class Sub1 extends Sealed_0 {
            
                }
            
                static final class Sub2 extends Sealed_0 {
            
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.Sealed_0", INPUT);
        assertTrue(typeInfo.typeNature().isClass());
        assertTrue(typeInfo.isSealed());
        assertEquals("[a.b.Sealed_0.Sub1, a.b.Sealed_0.Sub2]", typeInfo.permittedWhenSealed().toString());
        DetailedSources ds = typeInfo.source().detailedSources();
        assertEquals("3-30:3-36", ds.detail(DetailedSources.PERMITS).compact2());

        TypeInfo sub1 = typeInfo.findSubType("Sub1");
        assertEquals("Type a.b.Sealed_0", sub1.parentClass().toString());
    }

}
