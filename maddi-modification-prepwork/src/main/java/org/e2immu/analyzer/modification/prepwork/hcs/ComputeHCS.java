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

package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;

public class ComputeHCS {

    private final ComputeHiddenContent chc;
    private final Runtime runtime;

    public ComputeHCS(Runtime runtime) {
        this.chc = new ComputeHiddenContent(runtime);
        this.runtime = runtime;
    }

    public void doPrimitives() {
        for (TypeInfo typeInfo : runtime.primitives()) {
            doType(typeInfo);
        }
    }

    public void doType(TypeInfo typeInfo) {
        HiddenContentTypes hctType = typeInfo.analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () -> chc.compute(typeInfo));
        typeInfo.subTypes().forEach(this::doType);
        typeInfo.constructorAndMethodStream().forEach(mi -> {
            HiddenContentTypes hctMethod = mi.analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () -> chc.compute(hctType, mi));
            mi.analysis().getOrCreate(HCS_METHOD, () -> {
                if (mi.isConstructor()) {
                    return HiddenContentSelector.selectAll(hctType, typeInfo.asParameterizedType());
                }
                return HiddenContentSelector.selectAll(hctMethod, mi.returnType());
            });
            for (ParameterInfo pi : mi.parameters()) {
                pi.analysis().getOrCreate(HCS_PARAMETER, () ->
                        HiddenContentSelector.selectAll(hctMethod, pi.parameterizedType()));
            }
        });
    }

    public HiddenContentSelector doHiddenContentSelector(MethodInfo methodInfo) {
        MethodInfo overrideWithMostHiddenContent = overloadWithMostHiddenContent(methodInfo);
        ParameterizedType returnType = overrideWithMostHiddenContent.returnType();
        HiddenContentTypes hctOverride = overrideWithMostHiddenContent.analysis()
                .getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.class);
        assert hctOverride != null : "No HCT for " + overrideWithMostHiddenContent;
        HiddenContentSelector hcs = methodInfo.analysis().getOrCreate(HCS_METHOD, () -> {
            if (methodInfo.isConstructor()) {
                return HiddenContentSelector.selectAll(hctOverride, methodInfo.typeInfo().asParameterizedType());
            }
            return HiddenContentSelector.selectAll(hctOverride, returnType);
        });
        methodInfo.parameters().forEach(pi -> {
            pi.analysis().getOrCreate(HCS_PARAMETER, () -> {
                ParameterizedType pt = overrideWithMostHiddenContent.parameters().get(pi.index()).parameterizedType();
                return HiddenContentSelector.selectAll(hctOverride, pt);
            });
        });
        return hcs;
    }


    private MethodInfo overloadWithMostHiddenContent(MethodInfo methodInfo) {
        Set<MethodInfo> overrides = methodInfo.overrides();
        if (overrides.isEmpty()) return methodInfo;
        overrides.forEach(mi -> doType(mi.typeInfo()));
        if (overrides.size() == 1) return overrides.stream().findFirst().orElseThrow();
        Map<MethodInfo, Integer> map = overrides.stream()
                .collect(Collectors.toUnmodifiableMap(m -> m, this::countHiddenContent));
        return overrides.stream().min((o1, o2) -> map.get(o2) - map.get(o1)).orElseThrow();
    }

    private int countHiddenContent(MethodInfo methodInfo) {
        HiddenContentTypes hct = methodInfo.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class);
        assert hct != null;
        Stream<ParameterizedType> types = Stream.concat(Stream.of(methodInfo.returnType()),
                methodInfo.parameters().stream().map(Variable::parameterizedType));
        return (int) types.filter(t -> hct.indexOfOrNull(t) != null).count();
    }

    public static HiddenContentSelector safeHcsMethod(Runtime runtime, MethodInfo methodInfo) {
        HiddenContentSelector hcsMethod = methodInfo.analysis().getOrNull(HCS_METHOD, HiddenContentSelector.class);
        if (hcsMethod == null && methodInfo.isSyntheticArrayConstructor()) {
            ComputeHiddenContent computeHiddenContent = new ComputeHiddenContent(runtime);
            HiddenContentTypes hctType = methodInfo.typeInfo().analysis().getOrCreate(HIDDEN_CONTENT_TYPES,
                    () -> computeHiddenContent.compute(methodInfo.typeInfo()));
            HiddenContentTypes hctMethod = computeHiddenContent.compute(hctType, methodInfo);
            methodInfo.analysis().set(HIDDEN_CONTENT_TYPES, hctMethod);
            return new ComputeHCS(runtime).doHiddenContentSelector(methodInfo);
        }
        if (hcsMethod == null) {
            throw new UnsupportedOperationException("Have no hidden content selector computed for " + methodInfo);
        }
        return hcsMethod;
    }
}
