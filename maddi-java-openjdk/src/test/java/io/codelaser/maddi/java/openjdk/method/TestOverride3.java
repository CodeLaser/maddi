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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestOverride3 extends CommonTest {
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
    public void test() {
        MethodInfo bm = scan(false, "a.A", A, "a.B", B, "a.C", C).get("a.B").findUniqueMethod("m", 1);
        assertFalse(bm.overrides().isEmpty());
    }
}
