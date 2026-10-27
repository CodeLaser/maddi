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

package io.codelaser.maddi.bytecode.java.asm;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.resource.ParameterNameIndex;
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
        TypeInfo integer = compiledTypesManager.type(Integer.class);
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

        TypeInfo integer = compiledTypesManager.type(Integer.class);
        MethodInfo parseInt = integer.findUniqueMethod("parseInt", 2);
        assertEquals(List.of("s", "radix"), index.parameterNames(ParameterNameIndex.key(parseInt)));
    }
}
