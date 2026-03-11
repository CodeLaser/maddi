package org.e2immu.analyzer.run.mvnplugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.inspection.api.resource.InputConfiguration;

import java.io.File;

@Mojo(name = WriteInputConfigurationMojo.WRITE_INPUT_CONFIGURATION_GOAL,
        defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class WriteInputConfigurationMojo extends CommonMojo {
    public static final String WRITE_INPUT_CONFIGURATION_GOAL = "write-input-configuration";

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/inputConfiguration.json")
    private File outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // Create output directory if it doesn't exist
            if (outputFile.getParentFile().mkdirs()) {
                getLog().info("Created directories for " + outputFile.getAbsolutePath());
            }
            InputConfiguration inputConfiguration = makeInputConfiguration();
            ObjectMapper mapper = JsonStreaming.objectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, inputConfiguration);
            getLog().info("Input configuration exported to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to write input configuration", e);
        }
    }
}
