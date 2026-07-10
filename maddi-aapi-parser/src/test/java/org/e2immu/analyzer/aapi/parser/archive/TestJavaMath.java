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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.math.MathContext;
import java.math.RoundingMode;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestJavaMath extends CommonTest {

    // java.math is a package of immutable value types (operations return new instances, no array-output
    // methods) -> all four stubs are @ImmutableContainer. RoundingMode (enum) and MathContext (final class)
    // are deep @Immutable + @Container.
    // NOTE: BigInteger/BigDecimal are annotated the same way in JavaMath.java, but the openjdk class-file
    // scanner in this test harness cannot load them (AnalysisHintsParser logs "cannot load it", the same
    // limitation that affects TestCloneBench); they are therefore only exercised in production/composition,
    // not asserted here.
    @Test
    public void testRoundingModeAndMathContextImmutableContainer() {
        for (Class<?> c : new Class<?>[]{RoundingMode.class, MathContext.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(IMMUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE),
                    () -> c.getSimpleName() + " should be deep @Immutable");
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE),
                    () -> c.getSimpleName() + " should be a @Container");
        }
    }
}
