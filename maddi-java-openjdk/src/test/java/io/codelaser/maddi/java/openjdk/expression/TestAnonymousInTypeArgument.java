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

package org.e2immu.language.java.openjdk.expression;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAnonymousInTypeArgument extends CommonTest {

    // Optional.map(i -> new Base(){...}) infers the map's result type as Optional<anonymous Base subclass>,
    // so the anonymous class surfaces in a type-argument position. Because the lambda's target type is
    // converted before the anonymous body is scanned/registered, the anonymous class is still unknown and
    // has a blank simple name: it must be represented by its base type (Base) rather than looked up by name.
    // Mirrors com.sun.tools.javac.code.Types$DescriptorCache.mergeDescriptors, which crashed the openjdk scan.
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.Optional;
            class C {
                abstract static class Base {
                    abstract int getType();
                }
                Base method(Optional<Integer> opt) {
                    return opt.map(i -> new Base() {
                        @Override
                        public int getType() {
                            return i;
                        }
                    }).orElse(null);
                }
            }
            """;

    @DisplayName("anonymous class inferred into a type argument (forward reference)")
    @Test
    public void test1() {
        TypeInfo typeInfo = scan("a.b.C", INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        assertNotNull(method.methodBody());
    }
}
