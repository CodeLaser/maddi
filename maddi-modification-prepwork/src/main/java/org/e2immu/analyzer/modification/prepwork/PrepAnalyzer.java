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

package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.common.getset.GetSetHelper;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepAnalyzer.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000);
    public static final Predicate<TypeInfo> DO_NOT_ACCEPT_EXTERNALS =
            t -> t.compilationUnit() != null && !t.compilationUnit().externalLibrary();

    private final MethodAnalyzer methodAnalyzer;
    private final Runtime runtime;
    final Options options;
    private int typesProcessed;
    // collects per-type/per-method failures when options.faultTolerant; thread-safe for parallel runs
    private final List<AnalyzerException> exceptions = Collections.synchronizedList(new ArrayList<>());

    public PrepAnalyzer(Runtime runtime) {
        this(runtime, new Options.Builder().build());
    }

    public record Options(boolean doNotRecurseIntoAnonymous, boolean trackObjectCreations, boolean parallel,
                          boolean faultTolerant) {
        public static class Builder {
            boolean doNotRecurseIntoAnonymous;
            boolean trackObjectCreations;
            boolean parallel;
            boolean faultTolerant;

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

            /**
             * When true, a failure while analyzing one type or method is recorded (see {@link #exceptions()})
             * and analysis continues with the remaining types/methods, instead of aborting the whole run.
             * Default false, so tests and direct callers keep their fail-fast behaviour.
             */
            public Builder setFaultTolerant(boolean faultTolerant) {
                this.faultTolerant = faultTolerant;
                return this;
            }

            public Options build() {
                return new Options(doNotRecurseIntoAnonymous, trackObjectCreations, parallel, faultTolerant);
            }
        }
    }

    public PrepAnalyzer(Runtime runtime, Options options) {
        methodAnalyzer = new MethodAnalyzer(runtime, this);
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
            List<MethodInfo> gettersAndSetters = new LinkedList<>();
            List<MethodInfo> otherConstructorsAndMethods = new LinkedList<>();

            doType(typeInfo, gettersAndSetters, otherConstructorsAndMethods);

        /* now do the methods: first getters and setters, then the others
           why? because we must create variables in VariableData for each call to a getter
           therefore, all getters need to be known before they are being used.

           this is the simplest form of analysis order required here.
           the "linked variables" analyzer requires a more complicated one, computed in the statements below.
        */

            gettersAndSetters.forEach(this::doMethodIsolated);
            otherConstructorsAndMethods.forEach(this::doMethodIsolated);
            ++typesProcessed;
        } catch (RuntimeException | AssertionError | StackOverflowError t) {
            if (options.faultTolerant) {
                // backstop for failures in the type-level work (get/set classification, field initializers,
                // hidden content); individual methods are already isolated in doMethodIsolated. Record the
                // type and let the remaining primary types proceed instead of aborting the whole run.
                LOGGER.error("Caught exception in prep analyzer, isolating type {} (processed {} so far)",
                        typeInfo, typesProcessed, t);
                exceptions.add(new AnalyzerException(typeInfo, t));
            } else {
                LOGGER.error("Caught exception in prep analyzer. Processed {}, failing on type {}", typesProcessed, typeInfo);
                throw t;
            }
        }
    }

    // in fault-tolerant mode, isolate a single method: record its failure and carry on with the next method
    private void doMethodIsolated(MethodInfo methodInfo) {
        if (options.faultTolerant) {
            try {
                doMethod(methodInfo);
            } catch (RuntimeException | AssertionError | StackOverflowError t) {
                LOGGER.error("Caught exception in prep analyzer, isolating method {}", methodInfo, t);
                exceptions.add(new AnalyzerException(methodInfo, t));
            }
        } else {
            doMethod(methodInfo);
        }
    }

    /** Failures collected during a fault-tolerant run (empty otherwise); see {@link Options#faultTolerant()}. */
    public List<AnalyzerException> exceptions() {
        synchronized (exceptions) {
            return List.copyOf(exceptions);
        }
    }

    private void doType(TypeInfo typeInfo, List<MethodInfo> gettersAndSetters, List<MethodInfo> otherConstructorsAndMethods) {
        LOGGER.debug("Do type {}", typeInfo);

        // recurse
        typeInfo.subTypes().forEach(this::doType);
        typeInfo.constructorAndMethodStream().forEach(mi -> {
            boolean isGetSet = !mi.isConstructor() && GetSetHelper.doGetSetAnalysis(mi, mi.methodBody());
            if (isGetSet) {
                gettersAndSetters.add(mi);
            } else {
                otherConstructorsAndMethods.add(mi);
            }
        });
        typeInfo.fields().forEach(methodAnalyzer::doInitializerExpression);
    }
}
