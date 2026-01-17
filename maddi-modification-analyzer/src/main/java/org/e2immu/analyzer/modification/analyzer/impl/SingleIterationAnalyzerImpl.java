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
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public class SingleIterationAnalyzerImpl implements SingleIterationAnalyzer, ModAnalyzerForTesting {
    private final IteratingAnalyzer.Configuration configuration;
    private final LinkComputer linkComputer;
    private final FieldAnalyzer fieldAnalyzer;
    private final TypeModIndyAnalyzer typeModIndyAnalyzer;
    private final TypeImmutableAnalyzer typeImmutableAnalyzer;
    private final TypeIndependentAnalyzer typeIndependentAnalyzer;
    private final ShallowTypeAnalyzer shallowTypeAnalyzer;
    private final TypeContainerAnalyzer typeContainerAnalyzer;
    private final AtomicInteger propertiesChanged;

    public SingleIterationAnalyzerImpl(JavaInspector javaInspector, IteratingAnalyzer.Configuration configuration) {
        this.configuration = configuration;
        this.propertiesChanged = new AtomicInteger();
        linkComputer = new LinkComputerImpl(javaInspector, configuration.linkComputerOptions(), propertiesChanged);
        Runtime runtime = javaInspector.runtime();
        fieldAnalyzer = new FieldAnalyzerImpl(runtime, configuration, propertiesChanged);
        typeModIndyAnalyzer = new TypeModIndyAnalyzerImpl(configuration, propertiesChanged);
        typeImmutableAnalyzer = new TypeImmutableAnalyzerImpl(configuration, propertiesChanged);
        typeIndependentAnalyzer = new TypeIndependentAnalyzerImpl(configuration, propertiesChanged);
        shallowTypeAnalyzer = new ShallowTypeAnalyzer(runtime, Element::annotations, false);
        typeContainerAnalyzer = new TypeContainerAnalyzerImpl(configuration, propertiesChanged);
    }

    @Override
    public int propertiesChanged() {
        return propertiesChanged.get();
    }

    @Override
    public void go(List<Info> analysisOrder) {
        go(analysisOrder, false, true);
    }

    @Override
    public void go(List<Info> analysisOrder, boolean activateCycleBreaking, boolean firstIteration) {
        Set<TypeInfo> primaryTypes = new HashSet<>();
        Set<TypeInfo> abstractTypes = new HashSet<>();
        List<TypeInfo> typesInOrder = new ArrayList<>(analysisOrder.size());

        for (Info info : analysisOrder) {
            if (info instanceof MethodInfo methodInfo) {
                if (methodInfo.isAbstract() && abstractTypes.add(info.typeInfo())) {
                    shallowTypeAnalyzer.analyze(info.typeInfo());
                }
                MethodLinkedVariables mlv = linkComputer.doMethod(methodInfo);
                if (methodInfo.analysis().setAllowControlledOverwrite(METHOD_LINKS, mlv)) {
                    propertiesChanged.incrementAndGet();
                }
            } else if (info instanceof FieldInfo fieldInfo) {
                if (fieldInfo.owner().isAbstract()) {
                    shallowTypeAnalyzer.analyzeField(fieldInfo);
                }
                fieldAnalyzer.go(fieldInfo, activateCycleBreaking);
            } else if (info instanceof TypeInfo typeInfo) {
                runTypeAnalyzers(activateCycleBreaking, typeInfo);
                if (typeInfo.isPrimaryType()) primaryTypes.add(typeInfo);
                typesInOrder.add(typeInfo);
            }
        }
        AbstractMethodAnalyzer abstractMethodAnalyzer = new AbstractMethodAnalyzerImpl(configuration,
                propertiesChanged, primaryTypes);
        abstractMethodAnalyzer.go(firstIteration);

        /*
        run once more, because the abstract method analyzer may have resolved independence and modification values
        for abstract methods.
         */
        for (TypeInfo typeInfo : typesInOrder) {
            runTypeAnalyzers(activateCycleBreaking, typeInfo);
        }
    }

    private void runTypeAnalyzers(boolean activateCycleBreaking, TypeInfo typeInfo) {
        typeModIndyAnalyzer.go(typeInfo, activateCycleBreaking);
        typeIndependentAnalyzer.go(typeInfo, activateCycleBreaking);
        typeImmutableAnalyzer.go(typeInfo, activateCycleBreaking);

    }
}
