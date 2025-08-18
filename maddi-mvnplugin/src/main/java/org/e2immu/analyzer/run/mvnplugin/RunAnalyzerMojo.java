package org.e2immu.analyzer.run.mvnplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.linkedvariables.IteratingAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.SingleIterationAnalyzer;
import org.e2immu.analyzer.modification.linkedvariables.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.impl.SingleIterationAnalyzerImpl;
import org.e2immu.analyzer.modification.linkedvariables.io.LinkedVariablesCodec;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.util.internal.graph.G;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = RunAnalyzerMojo.RUN_ANALYZER_GOAL,
        defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class RunAnalyzerMojo extends CommonMojo {
    public static final String RUN_ANALYZER_GOAL = "run";

    @Parameter(property = "modificationAnalysis", defaultValue = "true")
    private boolean modificationAnalysis;

    /*
    failFast: stop immediately
    collect: collect all exceptions and fail at the end
    report: do not stop, but report to error log
     */
    @Parameter(property = "errorMode", defaultValue = "failFast")
    private String errorMode;

    @Parameter(property = "maxIterations", defaultValue = "5")
    private int maxIterations;

    @Override
    public void execute() throws MojoExecutionException {
        boolean storeErrors = !"failFast".equalsIgnoreCase(errorMode);
        boolean failWhenStoredErrors = !"report".equalsIgnoreCase(errorMode);

        try {
            ParseSourcesResult psr = parseSources();
            Runtime runtime = psr.javaInspector().runtime();

            Codec codec = new LinkedVariablesCodec(runtime).codec();
            new LoadAnalyzedPackageFiles().go(codec, List.of(ToolChain.currentJdkAnalyzedPackages(),
                    ToolChain.commonLibsAnalyzedPackages()));

            getLog().info("Starting prep analyzer");
            PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
            prepAnalyzer.initialize(psr.javaInspector().compiledTypesManager().typesLoaded());
            G<Info> dependencyGraph = prepAnalyzer.doPrimaryTypesReturnGraph(Set.copyOf(psr.parseResult().primaryTypes()));
            ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
            List<Info> order = cao.go(dependencyGraph);
            if (getLog().isDebugEnabled() && order.size() < 50) {
                getLog().debug("Analysis order: " + order);
            } else {
                Map<String, Integer> histogram = order.stream().collect(Collectors.toUnmodifiableMap(Info::info,
                        i -> 1, Integer::sum));
                getLog().info("Type histogram: " + histogram);
            }
            if (modificationAnalysis) {
                getLog().info("Starting modification analyzer");
                IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setStoreErrors(storeErrors)
                        .setMaxIterations(maxIterations)
                        .build();
                IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(runtime, configuration);
                IteratingAnalyzer.Output output = analyzer.analyze(order);

                if (storeErrors && !output.analyzerExceptions().isEmpty()) {
                    int n = output.analyzerExceptions().size();
                    getLog().error("Modification analysis halted with " + n + " exceptions");
                    for (AnalyzerException ae : output.analyzerExceptions()) {
                        getLog().error("In " + ae.getInfo() + ": " + ae.getMessage());
                    }
                    if (failWhenStoredErrors) {
                        throw new MojoExecutionException("Failed to run analyzer, caught " + n + " exceptions");
                    }
                }
            } else {
                getLog().info("Skip modification analyzer");
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run analyzer", e);
        }
    }
}
