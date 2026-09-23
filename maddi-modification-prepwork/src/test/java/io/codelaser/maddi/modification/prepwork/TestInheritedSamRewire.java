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

package io.codelaser.maddi.modification.prepwork;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A functional interface may INHERIT its single abstract method from a type that is not being rewired, a JDK
 * interface most simply. The info map then answers the method itself, which is right: there is no copy to
 * point at. {@code TypeInfoImpl.rewirePhase2} asserted that the rewired method always differs, and every
 * rewire of a project holding such an interface died on it (camel-core-model, 2026-09-21).
 */
public class TestInheritedSamRewire extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Supplier;
            interface X extends Supplier<String> {
                default String twice() { return get() + get(); }
            }
            """;

    @DisplayName("a single abstract method inherited from outside the rewired set stays what it is")
    @Test
    public void test() {
        TypeInfo x = javaInspector.parse(ABX, INPUT);
        MethodInfo sam = x.singleAbstractMethod();
        assertNotNull(sam);
        assertEquals("java.util.function.Supplier", sam.typeInfo().fullyQualifiedName());
        Set<TypeInfo> rewired = runtime.newInfoMap(Set.of(x)).rewireAll();
        TypeInfo x2 = rewired.iterator().next();
        assertNotSame(x, x2);
        assertSame(sam, x2.singleAbstractMethod());
    }
}
