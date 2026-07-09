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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_METHOD;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaTime extends CommonTest {

    // The whole java.time family is immutable value types whose methods return new instances and never
    // modify a parameter -> @ImmutableContainer. Final classes/enums land deep @Immutable; the interfaces
    // and abstract ZoneId are extensible, so they land as @Immutable(hc=true). All are containers.
    @Test
    public void testJavaTimeImmutableContainers() {
        for (Class<?> c : new Class<?>[]{
                DayOfWeek.class, Instant.class, LocalDate.class, LocalDateTime.class, ZoneId.class,
                ZonedDateTime.class, ChronoLocalDate.class, ChronoLocalDateTime.class, ChronoZonedDateTime.class,
                ChronoUnit.class, Temporal.class, TemporalAccessor.class, TemporalAdjuster.class,
                TemporalAmount.class, TemporalUnit.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            Value.Immutable imm = (Value.Immutable) typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
            assertTrue(imm.isAtLeastImmutableHC(), () -> c.getSimpleName() + " should be (at least) @Immutable(hc=true)");
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE),
                    () -> c.getSimpleName() + " should be a @Container");
        }
    }

    // Final concrete value types are deep @Immutable (not the hidden-content variant).
    @Test
    public void testFinalTimeTypesAreDeepImmutable() {
        for (Class<?> c : new Class<?>[]{
                DayOfWeek.class, Instant.class, LocalDate.class, LocalDateTime.class, ChronoUnit.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(IMMUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE),
                    () -> c.getSimpleName() + " should be deep @Immutable");
        }
    }

    @Test
    public void testDurationOfMillis() {
        TypeInfo typeInfo = compiledTypesManager().get(Duration.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("ofMillis", 1);
        assertFalse(methodInfo.allowsInterrupts());
        assertFalse(methodInfo.isModifying());
        assertSame(INDEPENDENT, methodInfo.analysis().getOrDefault(INDEPENDENT_METHOD, DEPENDENT));
        assertSame(IMMUTABLE, methodInfo.analysis().getOrDefault(IMMUTABLE_METHOD, MUTABLE));
    }

}
