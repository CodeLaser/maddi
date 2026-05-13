/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.openjdk.other;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        TypeInfo typeInfo = scan(Map.of("a.b.Sealed_0", INPUT), List.of()).getFirst();
        assertTrue(typeInfo.typeNature().isClass());
        assertTrue(typeInfo.isSealed());
        assertEquals("[a.b.Sealed_0.Sub1, a.b.Sealed_0.Sub2]", typeInfo.permittedWhenSealed().toString());

        TypeInfo sub1 = typeInfo.findSubType("Sub1");
        assertEquals("Type a.b.Sealed_0", sub1.parentClass().toString());
    }

}
