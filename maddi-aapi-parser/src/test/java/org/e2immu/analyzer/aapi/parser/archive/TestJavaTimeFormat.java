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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.format.TextStyle;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.CONTAINER_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.IMMUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaTimeFormat extends CommonTest {

    // DecimalStyle and the enums are immutable value types with no output-parameter method ->
    // deep @Immutable + @Container.
    @Test
    public void testImmutableContainerValueTypes() {
        for (Class<?> c : new Class<?>[]{DecimalStyle.class, FormatStyle.class, ResolverStyle.class,
                SignStyle.class, TextStyle.class}) {
            TypeInfo typeInfo = compiledTypesManager().get(c);
            assertSame(IMMUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE),
                    () -> c.getSimpleName() + " should be deep @Immutable");
            assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE),
                    () -> c.getSimpleName() + " should be a @Container");
        }
    }

    // DateTimeFormatter is immutable, but formatTo(...,Appendable)/parse(...,ParsePosition) write to
    // their argument, so it is @Immutable, NOT a @Container (the Charset/String pattern).
    @Test
    public void testDateTimeFormatterImmutableNotContainer() {
        TypeInfo typeInfo = compiledTypesManager().get(DateTimeFormatter.class);
        assertSame(IMMUTABLE, typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE));
        assertSame(FALSE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
    }

    // DateTimeFormatterBuilder is a mutable builder that never modifies a parameter -> @Container;
    // toFormatter() reads state (non-modifying) while append*() mutate the builder.
    @Test
    public void testDateTimeFormatterBuilder() {
        TypeInfo typeInfo = compiledTypesManager().get(DateTimeFormatterBuilder.class);
        assertSame(TRUE, typeInfo.analysis().getOrDefault(CONTAINER_TYPE, FALSE));
        MethodInfo toFormatter = typeInfo.methods().stream()
                .filter(m -> m.name().equals("toFormatter") && m.parameters().isEmpty()).findFirst().orElseThrow();
        assertFalse(toFormatter.isModifying(), "toFormatter() must be non-modifying");
        MethodInfo append = typeInfo.methods().stream()
                .filter(m -> m.name().equals("appendLiteral")).findFirst().orElseThrow();
        assertTrue(append.isModifying(), "appendLiteral() mutates the builder");
    }
}
