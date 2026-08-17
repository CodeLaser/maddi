package io.codelaser.maddi.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Parameter;
import io.codelaser.maddi.aapi.parser.AnalysisHintsConfiguration;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.run.config.GeneralConfiguration;
import io.codelaser.maddi.run.main.Main;
import io.codelaser.maddi.run.openjdkmain.RunAnalyzer;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.impl.runtime.LanguageConfigurationImpl;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compile hand-written analysis hints ({@code .java}) into analysis-result ({@code .json}) files (use case 2).
 * The hints live in {@code inputDirectory}; results are written to {@code outputDirectory}. Aligned with the
 * Gradle plugin: it builds a {@link Configuration} with the hints directory as its source set and an
 * {@code analysisResultsTargetDir}, then lets {@link RunAnalyzer} branch into its analysis-hints-compiler mode.
 * As with {@link RunAnalyzerMojo}, the openjdk parser requires the Maven JVM to have the javac {@code --add-exports}.
 */
@Mojo(name = CompileAnalysisHintsMojo.COMPILE_HINTS_GOAL, defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST)
public class CompileAnalysisHintsMojo extends CommonMojo {
    public static final String COMPILE_HINTS_GOAL = "compile-analysis-hints";

    @Parameter(property = "inputDirectory", defaultValue = "${project.build.directory}/annotatedAPI")
    private File inputDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/compiledAPI")
    private File outputDirectory;

    // only process these packages
    @Parameter(property = "restrictToPackages", defaultValue = "")
    private String restrictToPackages;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (outputDirectory.mkdirs()) {
                getLog().info("Created " + outputDirectory.getAbsolutePath());
            }
            // reuse the project's resolved classpath, but parse the hints sources from inputDirectory
            InputConfiguration projectInput = makeInputConfiguration();
            InputConfiguration hintsInput = makeHintsInputConfiguration(projectInput);

            Map<String, String> hintsMap = new HashMap<>();
            hintsMap.put(Main.ANALYSIS_RESULTS_TARGET_DIR, outputDirectory.getAbsolutePath());
            AnalysisHintsConfiguration hintsConfig = Main.analysisHintsConfiguration(hintsMap);
            GeneralConfiguration general = Main.generalConfiguration(
                    Map.of(Main.ANALYSIS_RESULTS_DIR, outputDirectory.getAbsolutePath()));

            Configuration configuration = new Configuration.Builder()
                    .setInputConfiguration(hintsInput)
                    .setAnalysisHintsConfiguration(hintsConfig)
                    .setGeneralConfiguration(general)
                    .setLanguageConfiguration(new LanguageConfigurationImpl(true))
                    .build();

            getLog().info("Compiling analysis hints from " + inputDirectory);
            RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
            runAnalyzer.run();
            int exit = runAnalyzer.exitValue();
            if (exit != Main.EXIT_OK) {
                throw new MojoExecutionException("analysis-hints compilation failed (exit " + exit + "): "
                                                 + Main.exitMessage(exit));
            }
            getLog().info("Wrote analysis-result .json files to " + outputDirectory);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to compile analysis hints", e);
        }
    }

    private InputConfiguration makeHintsInputConfiguration(InputConfiguration projectInput) {
        Set<String> restrict = restrictToPackages == null || restrictToPackages.isBlank() ? Set.of()
                : Arrays.stream(restrictToPackages.split("[,;]\\s*")).collect(Collectors.toUnmodifiableSet());
        SourceSet hints = new SourceSetImpl.Builder()
                .setName("hints")
                .setSourceDirectories(List.of(inputDirectory.toPath()))
                .setUri(inputDirectory.toURI())
                .setSourceEncoding(StandardCharsets.UTF_8)
                .setRestrictToPackages(restrict)
                .build();
        InputConfiguration.Builder builder = new InputConfigurationImpl.Builder();
        builder.addSourceSets(hints);
        builder.addClassPathParts(projectInput.classPathParts());
        if (projectInput.workingDirectory() != null) {
            builder.setWorkingDirectory(projectInput.workingDirectory().toString());
        }
        if (projectInput.alternativeJREDirectory() != null) {
            builder.setAlternativeJREDirectory(projectInput.alternativeJREDirectory().toString());
        }
        return builder.build();
    }
}
