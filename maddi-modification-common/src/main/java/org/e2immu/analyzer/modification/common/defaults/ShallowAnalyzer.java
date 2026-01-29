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

package org.e2immu.analyzer.modification.common.defaults;

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.op.Linearize;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.DEFAULTS_ANALYZER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;

public class ShallowAnalyzer {
    private final Runtime runtime;
    private final AnnotationProvider annotationProvider;
    private final List<Message> messages = new LinkedList<>();
    private final boolean onlyPublic;
    private final DebugVisitor debugVisitor;

    /*
    from type = from return type, field type, parameter type
    from owner = from type containing method, field, parameter
    from field = for final field, ...
    from parameter = for identity
    from method = for identity, fluent
     */
    public enum AnnotationOrigin {ANNOTATED, FROM_OVERRIDE, FROM_TYPE, FROM_OWNER, FROM_PARAMETER, FROM_METHOD, FROM_FIELD, DEFAULT}

    public record InfoData(Map<Property, AnnotationOrigin> originMap) {
        public AnnotationOrigin origin(Property property) {
            return originMap.getOrDefault(property, AnnotationOrigin.DEFAULT);
        }

        public void put(Property property, AnnotationOrigin origin) {
            originMap.put(property, origin);
        }
    }

    public record Result(Map<Element, InfoData> dataMap,
                         List<TypeInfo> allTypes,
                         G<TypeInfo> typeGraph,
                         List<TypeInfo> sorted,
                         List<Message> messages) {
    }

    public ShallowAnalyzer(Runtime runtime, AnnotationProvider annotationProvider, boolean onlyPublic) {
        this(runtime, annotationProvider, onlyPublic, null);
    }

    public ShallowAnalyzer(Runtime runtime, AnnotationProvider annotationProvider, boolean onlyPublic,
                           DebugVisitor debugVisitor) {
        this.runtime = runtime;
        this.annotationProvider = annotationProvider;
        this.onlyPublic = onlyPublic;
        this.debugVisitor = debugVisitor;
    }

    private boolean acceptAccess(Info info) {
        return !onlyPublic || info.access().isPublic();
    }

    public Result go(List<TypeInfo> types) {
        Set<SourceSet> stayWithinSourceSets = types.stream()
                .map(typeInfo -> typeInfo.compilationUnit().sourceSet()).collect(Collectors.toUnmodifiableSet());
        if(debugVisitor != null) debugVisitor.inputTypes(types);
        List<TypeInfo> allTypes = types.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                .filter(this::acceptAccess)
                .flatMap(t -> Stream.concat(Stream.of(t), t.recursiveSuperTypeStream()))
                .distinct()
                .filter(t -> stayWithinSourceSets.contains(t.compilationUnit().sourceSet()))
                .toList();
        if (debugVisitor != null) debugVisitor.allTypes(allTypes);

        G.Builder<TypeInfo> graphBuilder = new ImmutableGraph.Builder<>(Long::sum);
        for (TypeInfo typeInfo : allTypes) {
            List<TypeInfo> allSuperTypes = typeInfo.recursiveSuperTypeStream()
                    .filter(this::acceptAccess)
                    .filter(t -> stayWithinSourceSets.contains(t.compilationUnit().sourceSet()))
                    .toList();
            graphBuilder.add(typeInfo, allSuperTypes);
        }
        G<TypeInfo> typeGraph = graphBuilder.build();
        if (debugVisitor != null) debugVisitor.typeGraph(typeGraph);
        Linearize.Result<TypeInfo> linearize = Linearize.linearize(typeGraph, Linearize.LinearizationMode.ALL);
        List<TypeInfo> sorted = linearize.asList(Comparator.comparing(TypeInfo::fullyQualifiedName));
        if (debugVisitor != null) debugVisitor.sortedLinearized(sorted);
        Map<Element, InfoData> dataMap = new HashMap<>();

        ShallowTypeAnalyzer shallowTypeAnalyzer = new ShallowTypeAnalyzer(runtime, annotationProvider, onlyPublic);
        for (TypeInfo typeInfo : sorted) {
            dataMap.putAll(shallowTypeAnalyzer.analyze(typeInfo));
        }
        if (debugVisitor != null) debugVisitor.dataMapAfterTypeAnalyzer(dataMap);

        ShallowMethodAnalyzer shallowMethodAnalyzer = new ShallowMethodAnalyzer(runtime, annotationProvider);
        for (TypeInfo typeInfo : sorted) {
            dataMap.putAll(shallowTypeAnalyzer.analyzeFields(typeInfo));
            typeInfo.constructorAndMethodStream()
                    .filter(this::acceptAccess)
                    .forEach(mi -> dataMap.putAll(shallowMethodAnalyzer.analyze(mi)));
        }
        if (debugVisitor != null) debugVisitor.dataMapAfterFieldMethodAnalyzer(dataMap);
        for (TypeInfo typeInfo : sorted) {
            if (acceptAccess(typeInfo)) {
                shallowTypeAnalyzer.check(typeInfo);
                typeInfo.analysis().set(DEFAULTS_ANALYZER, TRUE);
            }
        }
        messages.addAll(shallowMethodAnalyzer.messages());
        return new Result(dataMap, allTypes, typeGraph, sorted, messages);
    }
}
