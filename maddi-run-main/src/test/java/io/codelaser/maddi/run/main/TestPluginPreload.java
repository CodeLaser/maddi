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
package io.codelaser.maddi.run.main;

import io.codelaser.maddi.run.config.ShippedAnalysisResults;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Gradle and Maven plugins configure a run through {@link PluginOptions#analysisHintsMap}, which leaves the preload
 * key out when nothing was configured. Such a run loads the shipped analysis results, as the command lines do
 * (CodeLaser/maddi-mod#15): before 2026-10-11 it loaded nothing, and every library type was an unknown.
 */
public class TestPluginPreload {

    private static List<String> preload(String preload, String analysisResultsTargetDir) {
        Map<String, String> map = PluginOptions.analysisHintsMap(preload, analysisResultsTargetDir, null, null, null);
        return Main.analysisHintsConfiguration(map).preloadAnalysisResultsDirs();
    }

    @Test
    public void unconfigured() {
        assertEquals(ShippedAnalysisResults.RESOURCES, preload(null, null));
        assertEquals(ShippedAnalysisResults.RESOURCES, preload("", ""), "the Maven plugin's empty default");
    }

    @Test
    public void none() {
        assertEquals(List.of(), preload("none", null));
    }

    @Test
    public void explicitAndDefault() {
        assertEquals(List.of("my/results"), preload("my/results", null));
        List<String> expected = new ArrayList<>(ShippedAnalysisResults.RESOURCES);
        expected.add("my/results");
        assertEquals(expected, preload("default,my/results", null));
    }

    @Test
    public void notWhenCompilingHints() {
        assertEquals(List.of(), preload(null, "out"));
    }
}
