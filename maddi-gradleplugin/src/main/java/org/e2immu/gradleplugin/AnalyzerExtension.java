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

package org.e2immu.gradleplugin;

import org.gradle.api.Action;

public class AnalyzerExtension {
    public static final String ANALYZER_EXTENSION_NAME = "e2immu";
    public static final String ANALYZER_TASK_NAME = "e2immu-analyzer";
    public static final String WRITE_INPUT_CONFIGURATION_TASK_NAME = "e2immu-write-input-configuration";

    /**
     * The consumable configuration each analyzed project publishes its own source directories on, and the
     * {@code Category} value that identifies it. A depending project reselects this variant to co-analyze its
     * siblings' <em>sources</em> instead of their jars; without that, an interface and its implementations end
     * up on opposite sides of the source/classpath divide and nothing can travel between them.
     */
    public static final String SOURCE_ELEMENTS_CONFIGURATION_NAME = "e2immuSourceElements";
    public static final String SOURCES_CATEGORY = "e2immu-sources";

    public boolean skipProject;

    /* GeneralConfiguration */
    public boolean incrementalAnalysis;
    public String analysisResultsDir;
    public boolean parallel;
    public String analysisSteps;
    public boolean quiet;
    public boolean warnNearMisses;
    public String debugTargets;

    /* InputConfiguration */
    public String jmods;
    public String jre;
    /**
     * @deprecated Legacy, avoid; fatal on modular projects. See
     * {@link org.e2immu.language.cst.api.element.SourceSet#restrictToPackages()}.
     */
    @Deprecated
    public String sourcePackages;
    /**
     * @deprecated As {@link #sourcePackages}.
     */
    @Deprecated
    public String testSourcePackages;
    public String excludeFromClasspath;
    public String workingDirectory;

    /* from AnalysisHintsConfiguration */
    // use case 1
    public String preloadAnalysisResultsDirs;
    // use case 2
    public String analysisResultsTargetDir;
    // use case 3
    public String updatedHintsDir;
    public String hintsPackages;
    public String updatedHintsPackage;


    private final ActionBroadcast<AnalyzerProperties> propertiesActions;

    public AnalyzerExtension(ActionBroadcast<AnalyzerProperties> propertiesActions) {
        this.propertiesActions = propertiesActions;
    }

    public void properties(Action<? super AnalyzerProperties> action) {
        propertiesActions.add(action);
    }
}
