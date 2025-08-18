package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.analyzer.aapi.parser.AnnotatedApiParser;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.io.WriteAnalysis;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.io.CodecImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.util.internal.util.Trie;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = CompileAnnotatedAPIsMojo.COMPILE_AAPI_GOAL, defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class CompileAnnotatedAPIsMojo extends CommonMojo {
    public static final String COMPILE_AAPI_GOAL = "compile-annotated-apis";

    @Parameter(property = "inputDirectory", defaultValue = "${project.build.directory}/annotatedAPI")
    private File inputDirectory;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/compiledAPI")
    private File outputDirectory;

    // only write for these packages
    @Parameter(property = "restrictToPackages", defaultValue = "")
    private String restrictToPackages;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // Create output directory if it doesn't exist
            if (outputDirectory.mkdirs()) {
                getLog().info("Created " + outputDirectory.getAbsolutePath());
            }

            // 0. make the input configuration from the dependency information
            InputConfiguration inputConfiguration = makeInputConfiguration();

            // 1. load the AAPI files; build the classpath
            AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
            InputConfiguration aapiInputConfiguration = makeAapiInputConfiguration(inputConfiguration);
            AnnotatedAPIConfiguration aapiConfiguration = new AnnotatedAPIConfigurationImpl.Builder().build();

            getLog().info("Start parsing AnnotatedAPI files from " + inputDirectory);
            annotatedApiParser.initialize(aapiInputConfiguration, aapiConfiguration);
            JavaInspector javaInspector = annotatedApiParser.javaInspector();
            getLog().info("Loaded AAPI files, now running shallow analyzer");

            // 2. run the shallow analyzer on all the loaded types
            ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(annotatedApiParser.runtime(), annotatedApiParser,
                    true);
            ShallowAnalyzer.Result rs = shallowAnalyzer.go(annotatedApiParser.typesParsed());
            getLog().info("Ran shallow analyzer on " + annotatedApiParser.types() + " types");

            // 3. write out the result
            Trie<TypeInfo> typeTrie = new Trie<>();
            rs.allTypes().forEach(ti -> typeTrie.add(ti.packageName().split("\\."), ti));

            WriteAnalysis writeAnalysis = new WriteAnalysis(javaInspector.runtime());
            Codec codec = new CodecImpl(javaInspector.runtime(), PropertyProviderImpl::get,
                    null, null); // we don't have to decode
            writeAnalysis.write(outputDirectory, typeTrie, codec,
                    set -> packagePrefixGenerator("", set));
            getLog().info("Wrote .json files to " + outputDirectory);
        } catch (RuntimeException | IOException | DependencyResolutionException e) {
            throw new MojoExecutionException("Failed to write input configuration", e);
        }
    }

    private InputConfiguration makeAapiInputConfiguration(InputConfiguration inputConfiguration) {
        InputConfiguration.Builder builder = new InputConfigurationImpl.Builder();
        Set<String> restrictToPackagesSet = restrictToPackages == null || restrictToPackages.isBlank() ?
                Set.of() : Arrays.stream(restrictToPackages
                .split("[,;]\\s*")).collect(Collectors.toUnmodifiableSet());
        SourceSetImpl main = new SourceSetImpl("main", List.of(inputDirectory.toPath()),
                inputDirectory.toURI(), StandardCharsets.UTF_8, false, false, false,
                false, false, restrictToPackagesSet, Set.of());
        builder.addSourceSets(main);
        // copy the classpath parts
        builder.addClassPathParts(inputConfiguration.classPathParts().toArray(SourceSet[]::new));
        if (inputConfiguration.alternativeJREDirectory() != null) {
            builder.setAlternativeJREDirectory(inputConfiguration.alternativeJREDirectory().toString());
        }
        return builder.build();
    }

}
