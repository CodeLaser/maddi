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

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassSymbolScanner extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Map;
            public class C {
                Map.Entry<String, Integer> entry;
            }
            """;

    @Test
    public void test() {
        TypeInfo c = scan("a.b.C", INPUT);
        assertTrue(c.typeNature().isClass());
        FieldInfo entryField = c.getFieldByName("entry", true);
        ParameterizedType type = entryField.type();
        assertEquals("Type java.util.Map.Entry<String,Integer>", type.toString());
        TypeInfo entry = type.typeInfo();
        assertEquals("java.util", entry.packageName());
        TypeInfo map = entry.compilationUnitOrEnclosingType().getRight();
        assertEquals("Map", map.simpleName());
        assertEquals("Entry", entry.simpleName());
        assertTrue(entry.methods().isEmpty());
        assertEquals(1, map.subTypes().size());

        TypeInfo mapLoaded = loadType("java.util.Map");
        assertTrue(mapLoaded.methods().size() > 10);
        // seen multiple values:
        // "jrt:/java.base/java/util/Map.class"
        // jar:file:///opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home/lib/ct.sym!/BCDEFGHIJKLMNOP/java.base/java/util/Map.sig
        assertTrue(mapLoaded.compilationUnit().uri().toString().contains("java/util/Map"));
        assertEquals(map.compilationUnit().uri(), mapLoaded.compilationUnit().uri());

        // for now
        assertNotSame(map, mapLoaded);
    }

}
