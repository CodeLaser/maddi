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

package org.e2immu.analyzer.modification.prepwork.hct;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;

public class ComputeHiddenContent {
    private final Runtime runtime;

    public ComputeHiddenContent(Runtime runtime) {
        this.runtime = runtime;
    }

    public HiddenContentTypes compute(HiddenContentTypes hcsTypeInfo, MethodInfo methodInfo) {
        assert hcsTypeInfo != null : "For method " + methodInfo;

        Map<NamedType, Integer> typeToIndex = new HashMap<>();

        Stream<ParameterizedType> parameterTypeStream = methodInfo.parameters().stream().map(Variable::parameterizedType);
        Stream<ParameterizedType> methodTypeStream =Stream.of( methodInfo.returnType());
        Set<TypeParameter> methodTypeParameters = Stream.concat(parameterTypeStream, methodTypeStream)
                .flatMap(this::typeParameterStream)
                .filter(TypeParameter::isMethodTypeParameter)
                .filter(tp -> !hcsTypeInfo.isKnown(tp))
                .collect(Collectors.toUnmodifiableSet());
        methodTypeParameters.forEach(tp -> typeToIndex.put(tp, tp.getIndex()));

        // are any of the parameter's type's a type parameter, not yet used in the fields? See resolve.Method_15
        for (ParameterInfo pi : methodInfo.parameters()) {
            addExtensible(pi.parameterizedType(), typeToIndex, null, nt -> !hcsTypeInfo.isKnown(nt));
        }
        addExtensible(methodInfo.returnType(), typeToIndex, null, nt -> !hcsTypeInfo.isKnown(nt));

        return new HiddenContentTypes(hcsTypeInfo, methodInfo, typeToIndex, true);
    }

    public HiddenContentTypes compute(TypeInfo typeInfo) {
        return compute(typeInfo, null);
    }

    private HiddenContentTypes compute(TypeInfo typeInfo, Set<TypeInfo> cycleProtection) {
        assert typeInfo != null && typeInfo.source() != null;
        boolean compiledCode = typeInfo.source().isCompiledClass();

        int offset;
        Map<NamedType, Integer> fromEnclosing;
        if (typeInfo.isInnerClass()) {
            HiddenContentTypes hct;
            MethodInfo enclosingMethod = typeInfo.enclosingMethod();
            Stream<Map.Entry<Integer, NamedType>> indexToTypeStream;
            if (enclosingMethod != null) {
                hct = getOrCompute(enclosingMethod, cycleProtection);
                indexToTypeStream = Stream.concat(hct.getHctTypeInfo().getIndexToType().entrySet().stream(),
                        hct.getIndexToType().entrySet().stream());
            } else {
                TypeInfo enclosing = typeInfo.compilationUnitOrEnclosingType().getRight();
                hct = getOrCompute(enclosing, cycleProtection);
                indexToTypeStream = hct == null ? Stream.of() : hct.getIndexToType().entrySet().stream();
            }
            offset = hct == null ? 0 : hct.size();
            fromEnclosing = indexToTypeStream.collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
        } else {
            offset = 0;
            fromEnclosing = Map.of();
        }

        Set<TypeParameter> typeParametersInFields;
        if (compiledCode || typeInfo.isInterface()) {
            // TODO add types of getters and setters
            typeParametersInFields = typeInfo.typeParameters().stream().collect(Collectors.toUnmodifiableSet());
        } else {
            typeParametersInFields = typeInfo.fields().stream()
                    .flatMap(fi -> typeParameterStream(fi.type()))
                    .collect(Collectors.toUnmodifiableSet());
        }
        Map<NamedType, Integer> fromThis = typeParametersInFields.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> tp.getIndex() + offset, (tp1, tp2) -> tp1, HashMap::new));
        fromThis.putAll(fromEnclosing);

        Map<NamedType, Integer> superTypeToIndex = new HashMap<>();
        if (typeInfo.parentClass() != null && !typeInfo.parentClass().isJavaLangObject()) {
            addFromSuperType(typeInfo.parentClass(), fromThis, superTypeToIndex, cycleProtection);
        }
        if (typeInfo.isInterface()) {
            for (ParameterizedType interfaceType : typeInfo.interfacesImplemented()) {
                addFromSuperType(interfaceType, fromThis, superTypeToIndex, cycleProtection);
            }
        }
        /*
         Finally, we add hidden content types from extensible fields without type parameters.

         NOTE: Linking to extensible fields with type parameters is done at the level of those type parameters ONLY
         in the current implementation, which does not allow for the combination of ALL and CsSet.
         */
        typeInfo.methods().stream().filter(MethodInfo::isAbstract).forEach(mi -> {
            Value.FieldValue fv = mi.getSetField();
            if (fv.field() != null) {
                // record->accessors, all @GetSet marked methods cause a synthetic field
                addExtensible(fv.field().type(), fromThis, cycleProtection, nt -> true);
            }
        });
        if (!compiledCode) {
            for (FieldInfo f : typeInfo.fields()) {
                if (!f.isSynthetic()) {
                    addExtensible(f.type(), fromThis, cycleProtection, nt -> true);
                }
            }
        }
        return HiddenContentTypes.of(typeInfo, typeInfo.isExtensible(), fromThis, superTypeToIndex);
    }

    /*
    If a type is not extensible, it may still have hidden content (e.g. a final List<T> implementation).
    Problem is that "hasHiddenContent" is a computation that requires HCT, which can lead to cycles in the computation.
    If we encounter a cycle, we must stop and assume that there is no hidden content.

    IMPROVE:  we should not add types whose sole hidden content is already present?
     */
    private void addExtensible(ParameterizedType type, Map<NamedType, Integer> fromThis, Set<TypeInfo> cycleProtection,
                               Predicate<NamedType> accept) {
        if (!type.isTypeParameter()) {
            TypeInfo bestType = Objects.requireNonNullElse(type.bestTypeInfo(), runtime.objectTypeInfo());
            boolean hasHiddenContent;
            if (bestType.isExtensible()) {
                hasHiddenContent = true;
            } else {
                HiddenContentTypes hct = getOrCompute(bestType, cycleProtection);
                hasHiddenContent = hct != null && hct.hasHiddenContent();
            }
            if (hasHiddenContent && accept.test(bestType)) {
                int index = fromThis.size();
                fromThis.putIfAbsent(bestType, index);
            }
            for (ParameterizedType pt : type.parameters()) {
                addExtensible(pt, fromThis, cycleProtection, accept);
            }
        }
    }

    private void addFromSuperType(ParameterizedType superType,
                                  Map<NamedType, Integer> fromThis,
                                  Map<NamedType, Integer> superTypeToIndex,
                                  Set<TypeInfo> cycleProtection) {
        HiddenContentTypes hctParent = getOrCompute(superType.typeInfo(), cycleProtection);
        if (hctParent != null) {
            if (!hctParent.isEmpty()) {
                Map<NamedType, ParameterizedType> fromMeToParent = superType.initialTypeParameterMap();
                assert fromMeToParent != null;
                // the following include all recursively computed
                hctParent.getTypeToIndex().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().asSimpleParameterizedType().fullyQualifiedName()))
                        .forEach(e -> {
                            NamedType typeInParent = hctParent.getIndexToType().get(e.getValue()); // this step is necessary for recursively computed...
                            ParameterizedType typeHere = fromMeToParent.get(typeInParent);
                            if (typeHere != null) {
                                NamedType namedTypeHere = typeHere.namedType();
                                Integer indexHere = fromThis.get(namedTypeHere);
                                if (indexHere == null && (namedTypeHere instanceof TypeParameter
                                                          || namedTypeHere instanceof TypeInfo ti && ti.isExtensible())) {
                                    // no field with this type, but we must still add it, as it exists in the parent
                                    // if shallow, it has already been added, there is no check on fields
                                    int ih = fromThis.size();
                                    fromThis.put(namedTypeHere, ih);
                                    superTypeToIndex.put(e.getKey(), ih);
                                }
                            } // see e.g. resolve.Constructor_2
                        });
            }
        } //running against the cycle protection... bail out
    }

    private HiddenContentTypes getOrCompute(TypeInfo enclosing, Set<TypeInfo> cycleProtection) {
        HiddenContentTypes hct = enclosing.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        if (hct != NO_VALUE) {
            return hct;
        }
        if (cycleProtection != null && !cycleProtection.add(enclosing)) {
            return null; // cannot compute anymore!!
        }
        Set<TypeInfo> cp = cycleProtection == null ? new HashSet<>() : cycleProtection;
        return compute(enclosing, cp);
    }

    private HiddenContentTypes getOrCompute(MethodInfo enclosing, Set<TypeInfo> cycleProtection) {
        HiddenContentTypes hct = enclosing.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        if (hct != NO_VALUE) {
            return hct;
        }
        HiddenContentTypes hctTypeInfo = getOrCompute(enclosing.typeInfo(), cycleProtection);
        return compute(hctTypeInfo, enclosing);
    }

    private Stream<TypeParameter> typeParameterStream(ParameterizedType type) {
        assert type != null;
        if (type.isTypeParameter()) return Stream.of(type.typeParameter());
        return type.parameters().stream().flatMap(this::typeParameterStream);
    }

}
