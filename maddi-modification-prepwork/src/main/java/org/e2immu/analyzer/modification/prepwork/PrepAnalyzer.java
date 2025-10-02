package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.analyzer.modification.common.getset.GetSetHelper;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField;
import org.e2immu.analyzer.modification.prepwork.hcs.ComputeHCS;
import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;

/*
do all the analysis of this phase

at the level of the type
- hidden content analysis
- call graph, call cycle, partOfConstruction
- simple immutability status

at the level of the method
- variable data, variable info, assignments
- simple modification status
 */
public class PrepAnalyzer {
    private static final Property PREP_ANALYZER = new PropertyImpl("prepAnalyzer");

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepAnalyzer.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000);
    public static final Predicate<TypeInfo> DO_NOT_ACCEPT_EXTERNALS =
            t -> t.compilationUnit() != null && !t.compilationUnit().externalLibrary();

    private final MethodAnalyzer methodAnalyzer;
    private final ComputeHiddenContent computeHiddenContent;
    private final ComputeHCS computeHCS;
    private final Runtime runtime;
    final Options options;
    private int typesProcessed;
    private int typesAlreadyDone;

    public PrepAnalyzer(Runtime runtime) {
        this(runtime, new Options.Builder().build());
    }

    public record Options(boolean doNotRecurseIntoAnonymous, boolean trackObjectCreations, boolean parallel) {
        public static class Builder {
            boolean doNotRecurseIntoAnonymous;
            boolean trackObjectCreations;
            boolean parallel;

            public Builder setDoNotRecurseIntoAnonymous(boolean doNotRecurseIntoAnonymous) {
                this.doNotRecurseIntoAnonymous = doNotRecurseIntoAnonymous;
                return this;
            }

            public Builder setParallel(boolean parallel) {
                this.parallel = parallel;
                return this;
            }

            public Builder setTrackObjectCreations(boolean trackObjectCreations) {
                this.trackObjectCreations = trackObjectCreations;
                return this;
            }

            public Options build() {
                return new Options(doNotRecurseIntoAnonymous, trackObjectCreations, parallel);
            }
        }
    }

    public PrepAnalyzer(Runtime runtime, Options options) {
        methodAnalyzer = new MethodAnalyzer(runtime, this);
        computeHiddenContent = new ComputeHiddenContent(runtime);
        computeHCS = new ComputeHCS(runtime);
        this.runtime = runtime;
        this.options = options;
    }

    boolean trackObjectCreations() {
        return options.trackObjectCreations();
    }

    /*
        we go via the FQN because we're in the process of translating them.
         */
    public void doMethod(MethodInfo methodInfo) {
        doMethod(methodInfo, methodInfo.methodBody());
    }

    public void doMethod(MethodInfo methodInfo, Block methodBlock) {
        methodAnalyzer.doMethod(methodInfo, methodBlock);
    }

    public List<Info> doPrimaryTypes(Set<TypeInfo> primaryTypes) {
        G<Info> callGraph = doPrimaryTypesReturnGraph(primaryTypes);
        ComputeAnalysisOrder cao = new ComputeAnalysisOrder();
        return cao.go(callGraph);
    }

    public G<Info> doPrimaryTypesReturnGraph(Set<TypeInfo> primaryTypes) {
        return doPrimaryTypesReturnComputeCallGraph(primaryTypes, List.of(), DO_NOT_ACCEPT_EXTERNALS, false).graph();
    }

    public ComputeCallGraph doPrimaryTypesReturnComputeCallGraph(Set<TypeInfo> primaryTypes,
                                                                 Collection<ModuleInfo> moduleInfos,
                                                                 Predicate<TypeInfo> externalsToAccept,
                                                                 boolean parallel) {
        AtomicInteger count = new AtomicInteger();
        int total = primaryTypes.size();
        Stream<TypeInfo> stream = parallel ? primaryTypes.parallelStream() : primaryTypes.stream();
        stream.forEach(primaryType -> {
            assert primaryType.isPrimaryType();
            doType(primaryType);
            TIMED_LOGGER.info("Done {} of {} primary types", count, total);
            count.incrementAndGet();
        });

        LOGGER.info("Start compute call graph");
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, primaryTypes, moduleInfos, externalsToAccept);
        G<Info> cg = ccg.go().graph();
        LOGGER.info("Set recursive methods");
        ccg.setRecursiveMethods();
        LOGGER.info("Start compute part of construction, final field");
        ComputePartOfConstructionFinalField cp = new ComputePartOfConstructionFinalField(options.parallel);
        cp.go(cg);
        LOGGER.info("Done, returning ComputeCallGraph object");
        return ccg;
    }

    public List<Info> doPrimaryType(TypeInfo typeInfo) {
        return doPrimaryTypes(Set.of(typeInfo));
    }

    void doType(TypeInfo typeInfo) {
        try {
            if (typeInfo.analysis().getOrNull(PREP_ANALYZER, ValueImpl.BoolImpl.class) == null) {
                List<MethodInfo> gettersAndSetters = new LinkedList<>();
                List<MethodInfo> otherConstructorsAndMethods = new LinkedList<>();

                doType(typeInfo, gettersAndSetters, otherConstructorsAndMethods);

                /* now do the methods: first getters and setters, then the others
                   why? because we must create variables in VariableData for each call to a getter
                   therefore, all getters need to be known before they are being used.

                   this is the simplest form of analysis order required here.
                   the "linked variables" analyzer requires a more complicated one, computed in the statements below.
                */

                gettersAndSetters.forEach(this::doMethod);
                otherConstructorsAndMethods.forEach(this::doMethod);
                ++typesProcessed;
                typeInfo.analysis().set(PREP_ANALYZER, ValueImpl.BoolImpl.TRUE);
            } else {
                ++typesAlreadyDone;
            }
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception in prep analyzer. Processed {}, failing on type {}", typesProcessed, typeInfo);
            throw re;
        }
    }

    private void doType(TypeInfo typeInfo, List<MethodInfo> gettersAndSetters, List<MethodInfo> otherConstructorsAndMethods) {
        LOGGER.debug("Do type {}", typeInfo);
        HiddenContentTypes hctType = typeInfo.analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () ->
                computeHiddenContent.compute(typeInfo));

        // recurse
        typeInfo.subTypes().forEach(this::doType);
        typeInfo.constructorAndMethodStream().forEach(mi -> {
            mi.analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () -> computeHiddenContent.compute(hctType, mi));
            computeHCS.doHiddenContentSelector(mi);
            boolean isGetSet = !mi.isConstructor() && GetSetHelper.doGetSetAnalysis(mi, mi.methodBody());
            if (isGetSet) {
                gettersAndSetters.add(mi);
            } else {
                otherConstructorsAndMethods.add(mi);
            }
        });
        typeInfo.fields().forEach(methodAnalyzer::doInitializerExpression);
    }

    // called from MethodAnalyzer
    void handleSyntheticArrayConstructor(ConstructorCall cc) {
        cc.constructor().analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () -> {
            // synthetic array constructors are not stored listed in typeInfo.constructorAndMethodStream()
            HiddenContentTypes hctConstructorType = cc.constructor().typeInfo().analysis().getOrCreate(HIDDEN_CONTENT_TYPES,
                    () -> computeHiddenContent.compute(cc.constructor().typeInfo()));
            return computeHiddenContent.compute(hctConstructorType, cc.constructor());
        });
        computeHCS.doHiddenContentSelector(cc.constructor());
    }

    public void initialize(List<TypeInfo> typesLoaded) {
        computeHCS.doPrimitives();
        // while the annotated APIs have HCT/HCS values for a number of types, this line takes care of the rest
        typesLoaded.stream().filter(TypeInfo::isPrimaryType).forEach(computeHCS::doType);
    }

    public int getTypesAlreadyDone() {
        return typesAlreadyDone;
    }

    public int getTypesProcessed() {
        return typesProcessed;
    }
}
