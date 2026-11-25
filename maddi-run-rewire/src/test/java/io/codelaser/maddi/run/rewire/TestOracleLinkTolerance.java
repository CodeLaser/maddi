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

package io.codelaser.maddi.run.rewire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The rewire oracle tolerates a type whose warm and cold analysis output differ in link summaries only
 * (CodeLaser/maddi-mod#158), and nothing else: a verdict, a missing element or an extra one still fails.
 */
public class TestOracleLinkTolerance {

    private static final String METHOD_COLD = "{\"name\": \"Mparse(9,String)\", \"data\":{\"methodLinks\":[[[\"R\","
                                              + "[\"T\",\"Mparse(9,String)\"]]],[]],\"nonModifyingMethod\":1}},";
    private static final String METHOD_WARM = "{\"name\": \"Mparse(9,String)\", \"data\":{\"methodLinks\":[[[\"R\","
                                              + "[\"T\",\"Mparse(9,String)\"]],[\"F\",[\"T\",\"Fx(0)\"]]],[]],"
                                              + "\"nonModifyingMethod\":1}},";
    private static final String FIELD_COLD = "{\"name\": \"FinferredType(5)\", \"data\":{\"finalField\":0,"
                                             + "\"links\":[[\"F\",[\"T\",\"x]\"]]],\"unmodifiedField\":1}},";
    private static final String FIELD_WARM = "{\"name\": \"FinferredType(5)\", \"data\":{\"finalField\":0,"
                                             + "\"links\":[[\"F\",[\"T\",\"x]\"]],[\"P\",\"y\"]],"
                                             + "\"unmodifiedField\":1}},";

    @Test
    public void linkSummariesOnlyAreTolerated() {
        assertTrue(RunRewireTests.differsOnlyInLinkSummaries(List.of("{\"type\":1}", METHOD_COLD, FIELD_COLD),
                List.of("{\"type\":1}", METHOD_WARM, FIELD_WARM)));
    }

    @Test
    public void aVerdictStillFails() {
        String warm = METHOD_WARM.replace("\"nonModifyingMethod\":1", "\"nonModifyingMethod\":0");
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(METHOD_COLD), List.of(warm)));
        // a verdict next to a link summary difference: still a verdict
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(FIELD_COLD),
                List.of(FIELD_WARM.replace("\"unmodifiedField\":1", "\"unmodifiedField\":0"))));
        // a verdict present on one side only
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(METHOD_COLD),
                List.of(METHOD_WARM.replace(",\"nonModifyingMethod\":1", ""))));
    }

    @Test
    public void aMissingOrExtraElementStillFails() {
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(METHOD_COLD, FIELD_COLD), List.of(METHOD_WARM)));
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(METHOD_COLD),
                List.of(METHOD_WARM, FIELD_WARM)));
        // the same element count, but another element
        assertFalse(RunRewireTests.differsOnlyInLinkSummaries(List.of(METHOD_COLD),
                List.of(METHOD_WARM.replace("Mparse(9,String)\", \"data", "Mparse(10,String)\", \"data"))));
    }

    @Test
    public void withoutLinkSummaries() {
        assertEquals("{\"name\": \"Mparse(9,String)\", \"data\":{\"nonModifyingMethod\":1}},",
                RunRewireTests.withoutLinkSummaries(METHOD_COLD));
        assertEquals("{\"name\": \"FinferredType(5)\", \"data\":{\"finalField\":0,\"unmodifiedField\":1}},",
                RunRewireTests.withoutLinkSummaries(FIELD_COLD));
        assertEquals("{\"data\":{}}", RunRewireTests.withoutLinkSummaries("{\"data\":{\"links\":[]}}"));
        assertEquals("{\"data\":{\"x\":1}}", RunRewireTests.withoutLinkSummaries("{\"data\":{\"x\":1}}"));
    }

    @Test
    public void aKeyInsideAStringIsNotALinkSummary() {
        String line = "{\"name\": \"F\\\"links\\\":(0)\", \"data\":{\"x\":\"\\\"methodLinks\\\":[1]\"}}";
        assertEquals(line, RunRewireTests.withoutLinkSummaries(line));
    }

    @Test
    public void anUnreadableLineComparesStrictly() {
        String line = "{\"data\":{\"links\":[[\"F\",\"a\"]";
        assertEquals(line, RunRewireTests.withoutLinkSummaries(line));
    }
}
