package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.e2immu.analyzer.aapi.parser.Composer;
import org.e2immu.analyzer.modification.io.DecoratorImpl;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = WriteAnnotatedAPIsMojo.WRITE_AAPI_GOAL, defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class WriteAnnotatedAPIsMojo extends CommonMojo {
    public static final String WRITE_AAPI_GOAL = "write-annotated-apis";

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/annotatedAPI")
    private File outputDirectory;

    // not part of the output directory
    @Parameter(property = "packagePrefix", defaultValue = "")
    private String packagePrefix;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            // Create output directory if it doesn't exist
            if (outputDirectory.mkdirs()) {
                getLog().info("Created " + outputDirectory.getAbsolutePath());
            }
            ParseSourcesResult psr = parseSources();
            Map<MethodInfo, Integer> methodCallFrequencies = methodCallFrequencies(psr.parseResult());
            getLog().info("Have method call frequencies for " + methodCallFrequencies.size() + " methods");

            Set<TypeInfo> acceptedTypes = computeAcceptedTypes(methodCallFrequencies.keySet());
            Map<MethodInfo, Integer> overrideFrequencies = new HashMap<>();
            methodCallFrequencies.forEach((mi, f) ->
                    mi.overrides().forEach(o -> overrideFrequencies.putIfAbsent(o, f)));
            ImportComputer importComputer = psr.javaInspector().importComputer(Integer.MAX_VALUE);
            Composer composer = new Composer(psr.javaInspector(),
                    importComputer,
                    set -> packagePrefixGenerator(packagePrefix, set),
                    w -> acceptedTypes.contains(w.typeInfo()));
            Set<TypeInfo> primaryTypes = psr.javaInspector().compiledTypesManager()
                    .typesLoaded().stream().map(TypeInfo::primaryType)
                    .collect(Collectors.toUnmodifiableSet());
            getLog().info("Have " + primaryTypes.size() + " primary types loaded");

            Collection<TypeInfo> apiTypes = composer.compose(primaryTypes);
            Map<Element, Element> dollarMap = composer.translateFromDollarToReal();

            Qualification.Decorator decorator = new DecoratorWithComments(getLog(), psr.javaInspector().runtime(),
                    dollarMap, methodCallFrequencies, overrideFrequencies);
            composer.write(apiTypes, outputDirectory, decorator);

        } catch (RuntimeException | IOException | DependencyResolutionException e) {
            throw new MojoExecutionException("Failed to write input configuration", e);
        }
    }

    private Set<TypeInfo> computeAcceptedTypes(Set<MethodInfo> methodInfos) {
        Set<TypeInfo> initial = methodInfos.stream().map(MethodInfo::typeInfo).collect(Collectors.toUnmodifiableSet());
        Set<TypeInfo> superTypes = initial.stream().flatMap(TypeInfo::recursiveSuperTypeStream)
                .collect(Collectors.toUnmodifiableSet());
        Stream<TypeInfo> enclosing = Stream.concat(superTypes.stream(), initial.stream())
                .flatMap(TypeInfo::enclosingTypeStream);
        return Stream.concat(enclosing, Stream.concat(initial.stream(), superTypes.stream()))
                .collect(Collectors.toUnmodifiableSet());
    }

    static class DecoratorWithComments extends DecoratorImpl {
        private final Map<MethodInfo, Integer> methodCallFrequencies;
        private final Map<MethodInfo, Integer> overrideFrequencies;
        private final Runtime runtime;
        private final Map<Element, Element> translationMap;
        private final Log log;

        public DecoratorWithComments(Log log,
                                     Runtime runtime,
                                     Map<Element, Element> translationMap,
                                     Map<MethodInfo, Integer> methodCallFrequencies,
                                     Map<MethodInfo, Integer> overrideFrequencies) {
            super(runtime, translationMap);
            this.translationMap = translationMap;
            this.log = log;
            this.runtime = runtime;
            this.methodCallFrequencies = methodCallFrequencies;
            this.overrideFrequencies = overrideFrequencies;
        }

        @Override
        public List<Comment> comments(Element infoIn) {
            Element info = translationMap == null ? infoIn : translationMap.getOrDefault(infoIn, infoIn);
            List<Comment> comments = super.comments(info);
            if (info instanceof MethodInfo mi) {
                Integer frequency = methodCallFrequencies.get(mi);
                Comment comment;
                if (frequency != null) {
                    comment = runtime.newSingleLineComment(runtime.noSource(), "frequency " + frequency);
                } else {
                    Integer overrideFrequency = overrideFrequencies.get(mi);
                    if (overrideFrequency != null) {
                        comment = runtime.newSingleLineComment(runtime.noSource(), "override has frequency " + overrideFrequency);
                        return Stream.concat(Stream.of(comment), comments.stream()).toList();
                    } else {
                        comment = null;
                    }
                }
                if (comment != null) log.debug("Annotating " + mi + " with " + comment.comment());
                return Stream.concat(Stream.ofNullable(comment), comments.stream()).toList();
            }
            return comments;
        }
    }

}
