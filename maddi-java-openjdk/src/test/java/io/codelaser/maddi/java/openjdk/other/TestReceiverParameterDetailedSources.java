/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
