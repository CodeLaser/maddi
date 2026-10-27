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

package io.codelaser.maddi.cst.print;

import io.codelaser.maddi.cst.api.output.element.TypeName;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;
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
