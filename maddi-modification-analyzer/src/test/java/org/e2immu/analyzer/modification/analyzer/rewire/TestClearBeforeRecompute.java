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

package org.e2immu.analyzer.modification.analyzer.rewire;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The early-cutoff skip's <em>clear-before-recompute</em> primitive ({@code PropertyValueMap.removeIf}). When the
 * worklist carries a REWIRE type optimistically and then finds it dirty, re-analysis may need to <em>lower</em> a
 * carried verdict — which the monotonic-overwrite guard rejects. Clearing the carried value first unblocks it. See
 * {@code analysis-rewiring.md}.
 */
public class TestClearBeforeRecompute {

    @DisplayName("removeIf clears a carried value so a lowering re-analysis no longer hits the monotonic guard")
    @Test
    public void test() {
        PropertyValueMap analysis = new PropertyValueMapImpl();

        // a carried (optimistic) high value: @Immutable
        analysis.set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);
        assertSame(ValueImpl.ImmutableImpl.IMMUTABLE,
                analysis.getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class));

        // re-analysis lowers it (IMMUTABLE -> MUTABLE): the strictly-increasing overwrite guard rejects that
        assertThrows(UnsupportedOperationException.class, () ->
                analysis.setAllowControlledOverwrite(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE));

        // clear-before-recompute: removeIf clears the carried value, so re-analysis can set the correct lower value
        analysis.removeIf(PropertyImpl.IMMUTABLE_TYPE::equals);
        assertNull(analysis.getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class));
        assertTrue(analysis.setAllowControlledOverwrite(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE));
        assertSame(ValueImpl.ImmutableImpl.MUTABLE,
                analysis.getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class));

        // and removeIf can clear a whole tier at once (a predicate), leaving the rest intact
        analysis.set(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.TRUE);
        analysis.removeIf(p -> "immutableType".equals(p.key()));
        assertNull(analysis.getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class));
        assertSame(ValueImpl.BoolImpl.TRUE, analysis.getOrNull(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.class));
    }
}
