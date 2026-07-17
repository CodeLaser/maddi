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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.*;
import org.e2immu.analyzer.modification.common.defaults.ShallowTypeAnalyzer;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.MessageImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public class SingleIterationAnalyzerImpl implements SingleIterationAnalyzer, ModAnalyzerForTesting {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleIterationAnalyzerImpl.class);

    private final LinkComputer linkComputer;
    private final FieldAnalyzer fieldAnalyzer;
    private final TypeModIndyAnalyzer typeModIndyAnalyzer;
    private final TypeImmutableAnalyzer typeImmutableAnalyzer;
    private final TypeIndependentAnalyzer typeIndependentAnalyzer;
    private final ShallowTypeAnalyzer shallowTypeAnalyzer;
    private final TypeContainerAnalyzer typeContainerAnalyzer;
    private final AbstractMethodAnalyzer abstractMethodAnalyzer;
    private final AtomicInteger propertiesChanged;
    private final List<Message> messages;
    private final boolean faultTolerant;
    private final Set<Info> failed = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // gate PARALLEL=<n>: run the per-element loop on n threads, iterations 2+ only (iteration 1 runs the
    // on-demand link recursion + abstract-type shallow analysis and stays sequential). Default 1 = sequential.
    private static final int PARALLEL_THREADS = parallelThreads();

    private static int parallelThreads() {
        String s = System.getenv("PARALLEL");
        if (s == null) return 1;
        try {
            return Math.max(1, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            LOGGER.warn("Cannot parse PARALLEL={}, running sequentially", s);
            return 1;
        }
    }
    // worklist support: elements whose analysis changed in the most recent go() (see SingleIterationAnalyzer)
    private final Set<Info> changedInfos = Collections.synchronizedSet(new HashSet<>());
    private final Set<Info> summaryChangedInfos = Collections.synchronizedSet(new HashSet<>());

    public static final String ANALYZER_CRASH = "analyzer-crash";
    public static final String LINK_CRASH = "link-crash";

    public SingleIterationAnalyzerImpl(JavaInspector javaInspector, IteratingAnalyzer.Configuration configuration) {
        this.propertiesChanged = new AtomicInteger();
        this.messages = Collections.synchronizedList(new ArrayList<>());
        this.faultTolerant = configuration.faultTolerant();
        // sv-integration gave LinkComputerImpl a TestVisitor parameter; production passes null
        linkComputer = new LinkComputerImpl(javaInspector, configuration.linkComputerOptions(), propertiesChanged,
                null);
        Runtime runtime = javaInspector.runtime();
        fieldAnalyzer = new FieldAnalyzerImpl(runtime, configuration, propertiesChanged, messages);
        typeModIndyAnalyzer = new TypeModIndyAnalyzerImpl(configuration, propertiesChanged, messages);
        typeImmutableAnalyzer = new TypeImmutableAnalyzerImpl(configuration, propertiesChanged, messages);
        typeIndependentAnalyzer = new TypeIndependentAnalyzerImpl(configuration, propertiesChanged, messages);
        shallowTypeAnalyzer = new ShallowTypeAnalyzer(runtime, Element::annotations, false);
        typeContainerAnalyzer = new TypeContainerAnalyzerImpl(configuration, propertiesChanged, messages);
        abstractMethodAnalyzer = new AbstractMethodAnalyzerImpl(configuration, propertiesChanged, messages);
    }

    @Override
    public int propertiesChanged() {
        return propertiesChanged.get();
    }

    @Override
    public Set<Info> changedInfos() {
        return Set.copyOf(changedInfos);
    }

    @Override
    public Set<Info> summaryChangedInfos() {
        return Set.copyOf(summaryChangedInfos);
    }

    @Override
    public List<Message> messages() {
        // the iteration's own analyzers, plus the shallow analyzer used for abstract types
        return Stream.concat(messages.stream(), shallowTypeAnalyzer.messages().stream()).toList();
    }

    @Override
    public void go(List<Info> analysisOrder) {
        go(analysisOrder, false, true);
    }

    @Override
    public void go(List<Info> analysisOrder, boolean activateCycleBreaking, boolean firstIteration) {
        linkComputer.reset();
        changedInfos.clear();
        summaryChangedInfos.clear();
        TolerantWrite.resetChangedTargets();
        Set<TypeInfo> abstractTypes = new HashSet<>(); // first iteration only, which always runs sequentially

        long startLoop = System.currentTimeMillis();
        if (PARALLEL_THREADS > 1 && !firstIteration) {
            LOGGER.info("Parallel iteration: {} threads over {} elements", PARALLEL_THREADS, analysisOrder.size());
            List<java.util.concurrent.Future<?>> futures = new ArrayList<>(analysisOrder.size());
            java.util.Map<Info, Long> elementMillis = new java.util.concurrent.ConcurrentHashMap<>();
            try (java.util.concurrent.ExecutorService pool =
                         java.util.concurrent.Executors.newFixedThreadPool(PARALLEL_THREADS)) {
                for (Info info : analysisOrder) {
                    futures.add(pool.submit(() -> {
                        long t0 = System.nanoTime();
                        try {
                            processElement(info, activateCycleBreaking, false, abstractTypes);
                        } finally {
                            elementMillis.put(info, (System.nanoTime() - t0) / 1_000_000);
                        }
                    }));
                }
            } // close() awaits completion of all submitted tasks
            for (java.util.concurrent.Future<?> future : futures) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    // only reachable when !faultTolerant (processElement rethrows); preserve that contract
                    if (e.getCause() instanceof RuntimeException re) throw re;
                    if (e.getCause() instanceof Error error) throw error;
                    throw new RuntimeException(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            String slowest = elementMillis.entrySet().stream()
                    .sorted(java.util.Map.Entry.<Info, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> e.getKey().fullyQualifiedName() + "=" + e.getValue() + "ms")
                    .reduce((a, b) -> a + ", " + b).orElse("-");
            LOGGER.info("Slowest elements: {}", slowest);
        } else {
            for (Info info : analysisOrder) {
                processElement(info, activateCycleBreaking, firstIteration, abstractTypes);
            }
        }
        long endLoop = System.currentTimeMillis();
        // derived rather than collected during the loop: same content as before, in deterministic
        // analysisOrder order, independent of parallel completion order
        List<MethodInfo> abstractMethods = analysisOrder.stream()
                .filter(info -> info instanceof MethodInfo mi && mi.isAbstract() && !failed.contains(info))
                .map(info -> (MethodInfo) info)
                .toList();
        List<TypeInfo> typesInOrder = analysisOrder.stream()
                .filter(info -> info instanceof TypeInfo && !failed.contains(info))
                .map(info -> (TypeInfo) info)
                .toList();

        long startAbstract = System.currentTimeMillis();
        int changesBeforeAbstract = propertiesChanged.get();
        try {
            abstractMethodAnalyzer.go(firstIteration, abstractMethods);
        } catch (RuntimeException | AssertionError | StackOverflowError e) {
            LOGGER.error("Caught exception in the abstract-method analyzer", e);
            if (!faultTolerant) throw e;
            // batch step — attribute to the first abstract method so the finding is at least locatable
            if (!abstractMethods.isEmpty()) messages.add(crashFinding(abstractMethods.getFirst(), e));
        } finally {
            // batch step: coarse attribution, any change dirties all abstract methods of this round
            if (propertiesChanged.get() > changesBeforeAbstract) changedInfos.addAll(abstractMethods);
        }

        long startSecondPass = System.currentTimeMillis();
        /*
        run once more, because the abstract method analyzer may have resolved independence and modification values
        for abstract methods.
         */
        for (TypeInfo typeInfo : typesInOrder) {
            if (faultTolerant && failed.contains(typeInfo)) continue;
            int changesBefore2 = propertiesChanged.get();
            try {
                runTypeAnalyzers(activateCycleBreaking, typeInfo);
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                LOGGER.error("Caught exception (2nd type pass) on {}", typeInfo, e);
                if (!faultTolerant) throw e;
                failed.add(typeInfo);
                messages.add(crashFinding(typeInfo, e));
            } finally {
                if (propertiesChanged.get() > changesBefore2) changedInfos.add(typeInfo);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.info("Phase timing: main loop {} ms, abstract batch {} ms, 2nd type pass {} ms",
                endLoop - startLoop, startSecondPass - startAbstract, end - startSecondPass);
        unionInWriteTargets();
    }

    private void processElement(Info info, boolean activateCycleBreaking, boolean firstIteration,
                                Set<TypeInfo> abstractTypes) {
        if (faultTolerant && failed.contains(info)) return; // an earlier iteration already crashed on this one
        int changesBefore = propertiesChanged.get();
        try {
            if (info instanceof MethodInfo methodInfo) {
                if (firstIteration && methodInfo.isAbstract() && abstractTypes.add(info.typeInfo())) {
                    shallowTypeAnalyzer.analyze(info.typeInfo());
                }
                if (!firstIteration || !methodInfo.analysis().haveAnalyzedValueFor(METHOD_LINKS)) {
                    MethodLinkedVariables mlv = linkComputer.doMethod(methodInfo);
                    // methodLinks IS the method's summary: pass the target so the change reaches
                    // summaryChangedInfos and dirties dependents (the 3-arg overload's "?" context did not,
                    // leaving the worklist 0-dirty after a verification pass found methodLinks changes)
                    if (TolerantWrite.setAllowControlledOverwrite(methodInfo.analysis(), METHOD_LINKS, mlv,
                            methodInfo)) {
                        propertiesChanged.incrementAndGet();
                    }
                }
            } else if (info instanceof FieldInfo fieldInfo) {
                if (fieldInfo.owner().isAbstract() && firstIteration) {
                    shallowTypeAnalyzer.analyzeField(fieldInfo);
                }
                fieldAnalyzer.go(fieldInfo, activateCycleBreaking);
            } else if (info instanceof TypeInfo typeInfo) {
                runTypeAnalyzers(activateCycleBreaking, typeInfo);
            }
        } catch (RuntimeException | AssertionError | StackOverflowError e) {
            LOGGER.error("Caught exception processing {}", info, e);
            if (!faultTolerant) throw e;
            failed.add(info);
            messages.add(crashFinding(info, e));
        } finally {
            // under PARALLEL the delta can over-attribute (another thread's change lands in the window);
            // a superset of changed elements is safe for the worklist
            if (propertiesChanged.get() > changesBefore) changedInfos.add(info);
        }
    }

    private void unionInWriteTargets() {
        // union in the write-target attribution: writes that landed on elements other than the one being
        // processed (the link computer's on-demand recursion writing a callee's METHOD_LINKS)
        for (Object target : TolerantWrite.changedTargets()) {
            // ParameterInfo extends Info but is not an analysis-order element: attribute to its method
            Info info = target instanceof org.e2immu.language.cst.api.info.ParameterInfo pi ? pi.methodInfo()
                    : target instanceof Info i ? i : null;
            if (info != null) {
                changedInfos.add(info);
                summaryChangedInfos.add(info); // targets carry only summary-level properties (see TolerantWrite)
            }
        }
    }

    /**
     * Turn an analysis/link crash on one {@code Info} into a located ERROR finding, so a single bad element is
     * isolated (not fatal) and surfaces through the normal {@code messages()} / {@code ErrorReport} channel.
     * The category distinguishes a failure originating in the link module from one in the analyzer proper.
     */
    private static Message crashFinding(Info info, Throwable t) {
        String category = isLinkCrash(t) ? LINK_CRASH : ANALYZER_CRASH;
        String detail = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
        return MessageImpl.error(info, category,
                "analysis crashed on " + info.fullyQualifiedName() + " — " + detail
                + " (isolated; this element was not fully analyzed)");
    }

    private static boolean isLinkCrash(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if (ste.getClassName().startsWith("org.e2immu.analyzer.modification.link.")) return true;
        }
        return false;
    }

    private void runTypeAnalyzers(boolean activateCycleBreaking, TypeInfo typeInfo) {
        typeModIndyAnalyzer.go(typeInfo, activateCycleBreaking);
        typeIndependentAnalyzer.go(typeInfo, activateCycleBreaking);
        typeImmutableAnalyzer.go(typeInfo, activateCycleBreaking);
        typeContainerAnalyzer.go(typeInfo);
    }
}
