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

package io.codelaser.maddi.gradleplugin;

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

    /*
     ⛔⛔ DO NOT ADD A COMPANION VARIANT CARRYING THE PRODUCER'S COMPILE CLASS PATH. It was built and measured on
     2026-08-19, and Gradle 9 refuses it:

         Resolution of the configuration ':pulsar-metadata:compileClasspath' was attempted
         without an exclusive lock. This is unsafe and not allowed.

     A consumable configuration whose artifacts come from `project.provider(() -> main.getCompileClasspath()
     .getFiles())` resolves the PRODUCER's configuration while the CONSUMER is resolving, and that is what the
     lock forbids. Every one of pulsar's 7 sibling projects failed this way; the variant contributed 0 files.

     ⚠ AND THE FUNCTIONAL TEST PASSED. A three-project fixture does not reproduce the locking conditions of a
     79-project build with configuration on demand, so the mechanism looked correct in maddi-gradleplugin's own
     tests and did nothing on a corpus. If it is attempted again, the gate is a real multi-module corpus, not a
     GradleTestKit fixture.

     What it was for: a sibling's `compileOnly` dependencies, which Gradle propagates to no consumer, so they are
     absent from the analysed project's configuration rather than merely mis-scoped. That is the whole of the
     residue on pulsar (99 of 108 diagnostics, from swagger and findbugs annotations declared compileOnly in
     pulsar-common). Everything a sibling declares as an ordinary dependency does reach us transitively.

     A route that could work: have the PRODUCER write its class path during its own build (a task output, so no
     cross-project resolution), and publish that file. It costs a task run per sibling, which is exactly the cost
     this plugin exists to avoid -- so it needs to be worth it before anyone builds it.
     */

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
    /**
     * The JDK modules to put on the parse class path, comma- or semicolon-separated. Unset means
     * {@link io.codelaser.maddi.run.config.util.JavaModules#DEFAULT_JMODS}, i.e. the whole {@code java.se}
     * closure -- <b>not</b> {@code java.base} alone, which is what this field's absent default used to mean while
     * the Maven plugin declared {@code java.se}.
     */
    public String jmods;
    public String jre;
    /**
     * @deprecated Legacy, avoid; fatal on modular projects. See
     * {@link io.codelaser.maddi.cst.api.element.SourceSet#restrictToPackages()}.
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
