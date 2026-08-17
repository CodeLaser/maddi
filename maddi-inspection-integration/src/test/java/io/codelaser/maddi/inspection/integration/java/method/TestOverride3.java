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

package io.codelaser.maddi.inspection.integration.java.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestOverride3 extends CommonTest2 {
    @Language("java")
    String A = """
            package a;
            
            public interface A {
                <T extends C> void m(T t);
            }
            """;

    @Language("java")
    String B = """
            package a;
            
            public class B implements A {
                public void m(C c) {}
            }
            """;

    @Language("java")
    String C = """
            package a;
            
            public class C {}
            """;

    @Test
    public void test() throws IOException {
        ParseResult parseResult = init(Map.of("a.A", A, "a.B", B, "a.C", C));
        MethodInfo bm = parseResult.findMethod("a.B.m(a.C)");
        assertFalse(bm.overrides().isEmpty());
    }
}
