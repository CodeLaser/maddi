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
