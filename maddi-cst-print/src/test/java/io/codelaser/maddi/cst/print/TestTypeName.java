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

package org.e2immu.language.cst.print;

import org.e2immu.language.cst.api.output.element.TypeName;
import org.e2immu.language.cst.impl.output.TypeNameImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTypeName {

    @Test
    public void test() {
        TypeName typeName1 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "main::com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE,false);
        assertEquals("Bar.Bar2", typeName1.minimal());
        TypeName typeName2 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "main::com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.FQN, false);
        assertEquals("com.foo.Bar.Bar2", typeName2.minimal());
        TypeName typeName2b = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "main::com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.DESCRIPTOR, false);
        assertEquals("main::com.foo.Bar.Bar2", typeName2b.minimal());
        TypeName typeName3 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "main::com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.DOLLARIZED_FQN, false);
        assertEquals("com.foo.Bar$Bar2", typeName3.minimal());

        TypeName typeName4 = new TypeNameImpl("Bar2", "com.foo.Bar.Bar2",
                "main::com.foo.Bar.Bar2",
                "Bar.Bar2", TypeNameImpl.Required.DOLLARIZED_FQN, true);
        assertEquals("@com.foo.Bar$Bar2", typeName4.minimal());
    }
}
