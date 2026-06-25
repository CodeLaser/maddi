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

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.resource.ParameterNameIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestParameterNameIndex extends CommonJmodBaseTests {

    @Test
    public void test() throws IOException {
        ParameterNameIndex index = new ParameterNameIndex();
        TypeInfo integer = compiledTypesManager.getOrLoad(Integer.class);
        index.putRecursively(integer);

        // real names, read from the LocalVariableTable: Integer.parseInt(String s, int radix)
        MethodInfo parseInt = integer.findUniqueMethod("parseInt", 2);
        // note: erasedForFQN().fullyQualifiedName() abbreviates java.lang (like MethodMap), hence "String"
        String key = ParameterNameIndex.key(parseInt);
        assertEquals("java.lang.Integer.parseInt(String,int)", key);
        assertEquals(List.of("s", "radix"), index.parameterNames(key));

        // round-trip through the text format
        StringWriter sw = new StringWriter();
        index.write(sw);
        ParameterNameIndex reloaded = ParameterNameIndex.read(new StringReader(sw.toString()));
        assertEquals(index.size(), reloaded.size());
        assertEquals(List.of("s", "radix"), reloaded.parameterNames(key));
    }

    @Test
    public void testDriverEnumeratesPackage() {
        SourceSet javaBase = compiledTypesManager.javaBase();
        // drive the whole java.lang package (directly, no subpackages) through the enumerator
        ParameterNameIndex index = BuildParameterNameIndex.build(compiledTypesManager, classPath, javaBase,
                sf -> sf.fullyQualifiedNameFromPath().startsWith("java.lang.")
                      && sf.fullyQualifiedNameFromPath().indexOf('.', "java.lang.".length()) < 0);
        assertTrue(index.size() > 100, "expected many methods, got " + index.size());

        TypeInfo integer = compiledTypesManager.getOrLoad(Integer.class);
        MethodInfo parseInt = integer.findUniqueMethod("parseInt", 2);
        assertEquals(List.of("s", "radix"), index.parameterNames(ParameterNameIndex.key(parseInt)));
    }
}
