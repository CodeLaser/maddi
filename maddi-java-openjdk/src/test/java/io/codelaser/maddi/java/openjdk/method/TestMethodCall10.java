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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMethodCall10 extends CommonTest {

    @Language("java")
    private static final String ABX = """
            package a.b;
            import static a.c.Y.when;
            class X {
                interface Mocked {
                    void deleteObject(String key, String id);
                }
                static <T> T any() { return null; }
                interface Stubber {
                    <T> T when(T t);
                }
                static Stubber doNothing() { return null; }

                void method(Mocked mocked){
                    doNothing().when(mocked).deleteObject(any(), any());
                }
            }
            """;

    @Language("java")
    private static final String ACY = """
            package a.c;
            public class Y {
                public static <T> String when(T t) { return "" + t; }
            }
            """;

    @DisplayName("instance and static method, in instance setup")
    @Test
    public void test1() {
        scan(false, "a.b.X", ABX, "a.c.Y", ACY);
    }
}
