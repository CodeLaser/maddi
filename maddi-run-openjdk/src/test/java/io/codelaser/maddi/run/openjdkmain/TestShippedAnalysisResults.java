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
