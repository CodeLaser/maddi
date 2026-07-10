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

package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaNet extends CommonTest {

    // URI/URL/InetAddress are immutable value types (methods return new instances, no setters) ->
    // @ImmutableContainer. URI and URL are final -> deep @Immutable; InetAddress is extensible
    // (Inet4/6Address), so it lands as @Immutable(hc=true). All are containers.
    @Test
    public void testNetValueTypesImmutableContainers() {
        for (Class<?> c : new Class<?>[]{InetAddress.class, URI.class, URL.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            Value.Immutable imm = (Value.Immutable) typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
            assertTrue(imm.isAtLeastImmutableHC(), () -> c.getSimpleName() + " should be (at least) @Immutable(hc=true)");
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE),
                    () -> c.getSimpleName() + " should be a @Container");
        }
    }

    @Test
    public void testFinalNetTypesAreDeepImmutable() {
        for (Class<?> c : new Class<?>[]{URI.class, URL.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(IMMUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE),
                    () -> c.getSimpleName() + " should be deep @Immutable");
        }
    }
}
