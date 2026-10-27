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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.integration.JavaInspectorImpl;
import io.codelaser.maddi.inspection.integration.java.CommonTest2;
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
