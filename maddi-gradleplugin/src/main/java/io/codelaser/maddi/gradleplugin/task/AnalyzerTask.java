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

package org.e2immu.gradleplugin.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * Runs the maddi analyzer over the project's sources. It deserializes the {@link org.e2immu.analyzer.run.config.Configuration}
 * computed by the plugin (source sets, classpath, analysis steps, results directory), serialized to
 * {@link #getConfigurationJson()}, and hands it to the openjdk-parser-based
 * {@link org.e2immu.analyzer.run.openjdkmain.RunAnalyzer}, which parses, runs the prep- and modification-analysis,
 * and writes the results to {@link #getAnalysisResultsDir()} (default {@code <build>/e2immu}).
 * <p>
 * The analysis runs in a <em>forked</em> worker JVM (see {@link AnalyzerWorkAction}): the openjdk front-end reaches
 * into {@code jdk.compiler}'s {@code com.sun.tools.javac.*} internals, which need {@code --add-exports}. Injecting
 * them into a forked worker keeps the consuming build's Gradle daemon unaffected.
 * <p>
 * The task holds no {@code Project} reference at execution time and uses lazy managed properties, so it is
 * configuration-cache compatible and declares its inputs/outputs for up-to-date checking and build caching.
 */
@CacheableTask
public abstract class AnalyzerTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(AnalyzerTask.class);

    // the openjdk front-end (JavacFileManager / Trees / Symbols) reaches into these javac internals
    private static final List<String> ADD_EXPORTS = List.of(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED");

    /** The serialized {@code run-config} {@link org.e2immu.analyzer.run.config.Configuration}. */
    @Input
    public abstract Property<String> getConfigurationJson();

    /** The source directories and classpath the analysis reads, for up-to-date checking. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getAnalyzedInputs();

    /** Where the analyzer writes its results (matches {@code analysisResultsDir} in the configuration). */
    @OutputDirectory
    public abstract DirectoryProperty getAnalysisResultsDir();

    /** Working directory of the forked analyzer; the analyzer resolves relative source-set paths against it. */
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    @Inject
    public abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void run() {
        String configurationJson = getConfigurationJson().get();
        File workingDir = getWorkingDirectory().getAsFile().getOrNull();
        LOGGER.info("Running the e2immu analyzer in a forked worker, working directory {}", workingDir);
        WorkQueue workQueue = getWorkerExecutor().processIsolation(spec -> {
            spec.getForkOptions().jvmArgs(ADD_EXPORTS);
            // the analyzer resolves the (relative) source-set paths against the process working directory
            if (workingDir != null) spec.getForkOptions().setWorkingDir(workingDir);
        });
        workQueue.submit(AnalyzerWorkAction.class, params -> params.getConfigurationJson().set(configurationJson));
        workQueue.await(); // surface analyzer failures as a build failure of this task
    }
}
