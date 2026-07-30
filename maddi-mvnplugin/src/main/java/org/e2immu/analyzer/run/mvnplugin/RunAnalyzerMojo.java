package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.main.Main;
import org.e2immu.analyzer.run.openjdkmain.RunAnalyzer;

/**
 * Runs the maddi analyzer over the project's sources. Like the Gradle plugin's {@code AnalyzerTask}/{@code
 * AnalyzerWorkAction}, it builds a {@link Configuration} and hands it to the openjdk-parser-based
 * {@link RunAnalyzer}, which parses, runs the prep- and modification-analysis, loads the pre-computed analysis
 * hints (use case 1) or compiles/writes them (use cases 2/3), and writes the results to the configured
 * {@code analysisResultsDir} (default {@code <build>/e2immu}).
 * <p>
 * Because the openjdk front-end reaches into {@code jdk.compiler}'s {@code com.sun.tools.javac.*} internals, the
 * Maven JVM hosting this plugin must be started with the corresponding {@code --add-exports}. Unlike Gradle (which
 * forks a worker), a Maven mojo runs in-process, so add these via {@code .mvn/jvm.config} or {@code MAVEN_OPTS}:
 * <pre>
 * --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
 * --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
 * </pre>
 * <p>
 * Bound to {@code process-test-classes}, not {@code compile}: {@code ComputeSourceSets} hands the analyzer the
 * project's <em>test</em> source set as well as its main one, and the openjdk front end resolves each set's
 * references into the sets it depends on through their compiled classes — so {@code test-compile} has to have run,
 * which under {@code compile} it had not. The other analyzing goals are bound the same way.
 */
@Mojo(name = RunAnalyzerMojo.RUN_ANALYZER_GOAL,
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST)
public class RunAnalyzerMojo extends CommonMojo {
    public static final String RUN_ANALYZER_GOAL = "run";

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Configuration configuration = computeConfiguration();
            RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
            runAnalyzer.run();
            int exitValue = runAnalyzer.exitValue();
            if (exitValue != Main.EXIT_OK) {
                throw new MojoExecutionException("e2immu analyzer failed (exit " + exitValue + "): "
                                                 + Main.exitMessage(exitValue));
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run analyzer", e);
        }
    }
}
