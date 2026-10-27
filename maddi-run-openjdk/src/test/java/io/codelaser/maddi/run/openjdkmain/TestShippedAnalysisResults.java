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
package io.codelaser.maddi.run.openjdkmain;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * An analysis run loads the analysis results maddi ships unless told otherwise (CodeLaser/maddi-mod#15): without them
 * every library type is an unknown, and a run that never loaded them looks exactly like one that did.
 */
public class TestShippedAnalysisResults {

    private static List<String> preload(String... args) throws ParseException {
        CommandLine cmd = DefaultParser.builder().get().parse(Main.createOptions(), args);
        return Main.parseAnalysisHintsConfiguration(cmd).preloadAnalysisResultsDirs();
    }

    @Test
    public void theShippedResultsAreOnTheClassPath() {
        for (String r : Main.SHIPPED_ANALYSIS_RESULTS) {
            assertNotNull(Main.class.getResource(r.substring("resource:".length())), r);
        }
    }

    @Test
    public void byDefault() throws ParseException {
        assertEquals(Main.SHIPPED_ANALYSIS_RESULTS, preload("--source", "src"));
    }

    @Test
    public void noneLoadsNothing() throws ParseException {
        assertEquals(List.of(), preload("--source", "src", "--preload-analysis-results-dirs", "none"));
    }

    @Test
    public void anExplicitDirectoryReplacesTheDefault() throws ParseException {
        assertEquals(List.of("my/results"), preload("--source", "src", "--preload-analysis-results-dirs", "my/results"));
    }

    @Test
    public void defaultNamesTheShippedResultsInAList() throws ParseException {
        List<String> expected = new java.util.ArrayList<>(Main.SHIPPED_ANALYSIS_RESULTS);
        expected.add("my/results");
        assertEquals(expected, preload("--source", "src", "--preload-analysis-results-dirs", "default,my/results"));
    }

    /* a run that compiles hints produces results: what it preloads changes what it writes, so nothing implicit */
    @Test
    public void notWhenCompilingHints() throws ParseException {
        assertEquals(List.of(), preload("--source", "src", "--analysis-results-target-dir", "out"));
    }
}
