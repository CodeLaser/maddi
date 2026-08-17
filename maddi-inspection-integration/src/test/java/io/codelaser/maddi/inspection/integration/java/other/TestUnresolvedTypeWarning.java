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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.java.CommonTest2;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 (conservative): a reference to a type that is not on the (partial) classpath is a tolerable
 * <em>warning</em>, not a fatal error — harmonizing the in-house parser with the openjdk front-end. Stubs are
 * disabled here so the unresolved type throws {@code UnresolvedTypeException}, which the resolver now downgrades.
 */
public class TestUnresolvedTypeWarning extends CommonTest2 {

    private static final String C = """
            package a;
            public class C {
                public Object make() {
                    return new Widget(); // Widget is not on the classpath, and no import resolves it
                }
            }
            """;

    @Test
    public void unresolvedTypeIsWarningNotFatal() throws IOException {
        Map<String, String> sources = sourcesByURIString(Map.of("a.C", C));
        InputConfiguration inputConfiguration = makeInputConfiguration(sources, Map.of());
        JavaInspector javaInspector = new JavaInspectorImpl(true, false); // stubs off
        javaInspector.initialize(inputConfiguration);

        Summary summary = javaInspector.parse(new HashMap<>(sources),
                new JavaInspector.ParseOptions.Builder().setFailFast(false).setDetailedSources(true).build());

        assertFalse(summary.haveErrors(), "an unresolved type must be tolerated (warning), not a fatal error");
        assertFalse(summary.parseWarnings().isEmpty(), "the unresolved type must surface as a warning");
        assertTrue(summary.parseWarnings().stream().allMatch(w -> w.level().isWarning()));
    }
}
