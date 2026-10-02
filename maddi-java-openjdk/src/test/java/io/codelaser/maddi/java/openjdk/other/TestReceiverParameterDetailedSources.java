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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A receiver parameter ({@code X this}, JLS 8.4.1) no longer costs a compilation unit its detailed sources
 * (maddi#99). The CongoCC grammar rejected it, so the detailed-source pre-scan failed and the unit was kept
 * without comments and positions — for the whole file, not just the method. Three hibernate-core files use it.
 * <p>
 * Comments and the closing parenthesis of a parameter list are what the pre-scan contributes, so they are what
 * is asserted, in a method AFTER the one carrying the receiver as well as in that method itself.
 */
public class TestReceiverParameterDetailedSources extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class X {
                // resets the start
                private void resetStart(X this, int delta) {
                }
                // unrelated, further down
                void other() {
                }
            }
            """;

    @Test
    public void detailedSourcesSurvive() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo resetStart = typeInfo.findUniqueMethod("resetStart", 1);
        assertEquals("delta", resetStart.parameters().getFirst().name(), "the receiver is not a parameter");
        assertEquals(1, resetStart.comments().size(), "comment on the method carrying the receiver");
        assertNotNull(resetStart.source().detailedSources(), "detailed sources of resetStart");
        assertNotNull(resetStart.source().detailedSources().detail(DetailedSources.END_OF_PARAMETER_LIST));

        MethodInfo other = typeInfo.findUniqueMethod("other", 0);
        assertEquals(1, other.comments().size(), "comment on a later method in the same unit");
    }
}
