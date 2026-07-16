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
    private final Set<Info> failed = new HashSet<>();

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
        Set<TypeInfo> abstractTypes = new HashSet<>();
        List<TypeInfo> typesInOrder = new ArrayList<>(analysisOrder.size());

        List<MethodInfo> abstractMethods = new ArrayList<>();
        for (Info info : analysisOrder) {
            if (faultTolerant && failed.contains(info)) continue; // an earlier iteration already crashed on this one
            try {
                if (info instanceof MethodInfo methodInfo) {
                    if (firstIteration && methodInfo.isAbstract() && abstractTypes.add(info.typeInfo())) {
                        shallowTypeAnalyzer.analyze(info.typeInfo());
                    }
                    if (!firstIteration || !methodInfo.analysis().haveAnalyzedValueFor(METHOD_LINKS)) {
                        MethodLinkedVariables mlv = linkComputer.doMethod(methodInfo);
                        if (methodInfo.analysis().setAllowControlledOverwrite(METHOD_LINKS, mlv)) {
                            propertiesChanged.incrementAndGet();
                        }
                    }
                    if (methodInfo.isAbstract()) abstractMethods.add(methodInfo);
                } else if (info instanceof FieldInfo fieldInfo) {
                    if (fieldInfo.owner().isAbstract() && firstIteration) {
                        shallowTypeAnalyzer.analyzeField(fieldInfo);
                    }
                    fieldAnalyzer.go(fieldInfo, activateCycleBreaking);
                } else if (info instanceof TypeInfo typeInfo) {
                    runTypeAnalyzers(activateCycleBreaking, typeInfo);
                    typesInOrder.add(typeInfo);
                }
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                LOGGER.error("Caught exception processing {}: {}", info, e.toString());
                if (!faultTolerant) throw e;
                failed.add(info);
                messages.add(crashFinding(info, e));
            }
        }

        try {
            abstractMethodAnalyzer.go(firstIteration, abstractMethods);
        } catch (RuntimeException | AssertionError | StackOverflowError e) {
            LOGGER.error("Caught exception in the abstract-method analyzer: {}", e.toString());
            if (!faultTolerant) throw e;
            // batch step — attribute to the first abstract method so the finding is at least locatable
            if (!abstractMethods.isEmpty()) messages.add(crashFinding(abstractMethods.getFirst(), e));
        }

        /*
        run once more, because the abstract method analyzer may have resolved independence and modification values
        for abstract methods.
         */
        for (TypeInfo typeInfo : typesInOrder) {
            if (faultTolerant && failed.contains(typeInfo)) continue;
            try {
                runTypeAnalyzers(activateCycleBreaking, typeInfo);
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                LOGGER.error("Caught exception (2nd type pass) on {}: {}", typeInfo, e.toString());
                if (!faultTolerant) throw e;
                failed.add(typeInfo);
                messages.add(crashFinding(typeInfo, e));
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
