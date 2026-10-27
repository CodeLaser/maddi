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
