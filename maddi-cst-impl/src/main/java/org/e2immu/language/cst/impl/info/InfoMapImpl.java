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

package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.impl.statement.BlockImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/*
BASIC RULE OF REWIRING: the fqn stays the same, as does the (name of the) source set.
Conceptually the object stays the same.

As a consequence, it doesn't really matter which object is used as the key.
 */
public class InfoMapImpl implements InfoMap {
    private final Map<TypeInfo, Map<Info, Info>> setOfPrimaryTypesToRewire;
    private final Set<TypeInfo> toRewire;

    public InfoMapImpl(Set<TypeInfo> setOfPrimaryTypesToRewire) {
        this(setOfPrimaryTypesToRewire, Set.of());
    }

    /**
     * @param setOfPrimaryTypesToRewire the primary types to copy: they are unchanged, but they reach types that were
     *                                  re-parsed, so their copies must be pointed at the new objects.
     * @param rebuiltPrimaryTypes       primary types that have <em>already</em> been rebuilt from source (the
     *                                  invalidated ones). They are not rewired — they are new objects already — but
     *                                  they must be in this map, or {@link #typeInfo(TypeInfo)} would hand a rewired
     *                                  type the stale object it used to reach: it returns its argument unchanged for
     *                                  any type whose primary type is not a key here. Seeding each of them under
     *                                  itself is enough, precisely because of the BASIC RULE above — a lookup of the
     *                                  old object finds the new one, equality being fqn + source set.
     */
    public InfoMapImpl(Set<TypeInfo> setOfPrimaryTypesToRewire, Set<TypeInfo> rebuiltPrimaryTypes) {
        Map<TypeInfo, Map<Info, Info>> map = new HashMap<>();
        for (TypeInfo primaryType : setOfPrimaryTypesToRewire) {
            map.put(primaryType, new HashMap<>()); // filled by the rewire phases
        }
        for (TypeInfo rebuilt : rebuiltPrimaryTypes) {
            Map<Info, Info> seeded = new HashMap<>();
            seed(seeded, rebuilt);
            map.put(rebuilt, seeded);
        }
        this.setOfPrimaryTypesToRewire = Map.copyOf(map);
        this.toRewire = Set.copyOf(setOfPrimaryTypesToRewire);
    }

    /** Map every Info of an already-rebuilt type onto itself; the old objects resolve to these by equality. */
    private static void seed(Map<Info, Info> map, TypeInfo typeInfo) {
        map.put(typeInfo, typeInfo);
        for (MethodInfo methodInfo : typeInfo.constructorsAndMethods()) {
            map.put(methodInfo, methodInfo);
            for (ParameterInfo parameterInfo : methodInfo.parameters()) {
                map.put(parameterInfo, parameterInfo);
            }
        }
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            map.put(fieldInfo, fieldInfo);
        }
        for (TypeInfo subType : typeInfo.subTypes()) {
            seed(map, subType);
        }
    }

    @Override
    public void put(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        if (map != null) {
            map.put(typeInfo, typeInfo); // here, we use the "newly wired" object as a key; eq is based on fqn+source set
        }
    }

    @Override
    public void put(MethodInfo original, MethodInfo rewired) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(original.typeInfo().primaryType());
        if (map != null) {
            map.put(original, rewired); // here, we add the original, because the rewired's FQN has not been built yet
        }
    }

    @Override
    public void put(FieldInfo fieldInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(fieldInfo.owner().primaryType());
        if (map != null) {
            map.put(fieldInfo, fieldInfo); // here, we use the "newly wired" object as a key; eq is based on fqn+source set
        }
    }

    @Override
    public void put(ParameterInfo original, ParameterInfo rewired) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(original.typeInfo().primaryType());
        if (map != null) {
            map.put(original, rewired); // here, we add the original, because the rewired's FQN has not been built yet
        }
    }

    @Override
    public TypeInfo typeInfo(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        if (map == null) return typeInfo;
        return (TypeInfo) Objects.requireNonNull(map.get(typeInfo), "Should have been present: " + typeInfo);
    }

    @Override
    public TypeInfo typeInfoNullIfAbsent(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        if (map == null) return typeInfo;
        return (TypeInfo) map.get(typeInfo);
    }

    /** Only the types to rewire go through the phases: the rebuilt ones are new objects already, and are seeded. */
    public Set<TypeInfo> rewireAll() {
        for (TypeInfo primaryType : toRewire) {
            primaryType.rewirePhase0(this, null);
        }
        for (TypeInfo primaryType : toRewire) {
            primaryType.rewirePhase1(this);
        }
        for (TypeInfo primaryType : toRewire) {
            primaryType.rewirePhase2(this);
        }
        for (TypeInfo primaryType : toRewire) {
            primaryType.rewirePhase3(this);
        }
        return toRewire.stream()
                .map(primaryType -> (TypeInfo) setOfPrimaryTypesToRewire.get(primaryType).get(primaryType))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public TypeInfo typeInfoRecurseAllPhases(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        assert map != null;
        TypeInfo inMap = (TypeInfo) map.get(typeInfo);
        if (inMap != null) return inMap;
        // the enclosing type should already have been done
        TypeInfo enclosingType = typeInfo(typeInfo.compilationUnitOrEnclosingType().getRight());
        TypeInfo rewired = typeInfo.rewirePhase0(this, enclosingType);
        typeInfo.rewirePhase1(this);
        assert rewired != null : "Rewiring of " + typeInfo + " returns null";
        assert map.containsKey(typeInfo);
        typeInfo.rewirePhase2(this);
        typeInfo.rewirePhase3(this);
        return rewired;
    }

    @Override
    public MethodInfo methodInfo(MethodInfo methodInfo) {
        assert methodInfo != null;
        if (methodInfo.isSyntheticArrayConstructor()) {
            return createSyntheticArrayConstructor(methodInfo);
        }
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(methodInfo.typeInfo().primaryType());
        if (map == null) return methodInfo;
        return (MethodInfo) Objects.requireNonNull(map.get(methodInfo),
                "Cannot find " + methodInfo.fullyQualifiedName());
    }

    private MethodInfo createSyntheticArrayConstructor(MethodInfo methodInfo) {
        // the synthetic array constructor won't be in the map
        MethodInfo mi = new MethodInfoImpl(MethodInfoImpl.MethodTypeEnum.SYNTHETIC_ARRAY_CONSTRUCTOR,
                MethodInfoImpl.CONSTRUCTOR_NAME, typeInfo(methodInfo.typeInfo()));
        mi.builder()
                .addAnnotations(methodInfo.annotations().stream()
                        .map(a -> (AnnotationExpression) a.rewire(this)).toList())
                .addComments(methodInfo.comments().stream().map(c -> c.rewire(this)).toList())
                .setSource(methodInfo.source())
                .setReturnType(methodInfo.returnType().rewire(this))
                .addMethodModifier(MethodModifierEnum.PUBLIC)
                .setMethodBody(new BlockImpl.Builder().build())
                .setMissingData(methodInfo.missingData())
                .computeAccess();
        for (int i = 0; i < methodInfo.returnType().arrays(); i++) {
            ParameterInfo pii = methodInfo.parameters().get(i);
            ParameterInfo pi = mi.builder().addParameter(pii.name(), pii.parameterizedType());
            pi.builder()
                    .addAnnotations(methodInfo.annotations().stream()
                            .map(a -> (AnnotationExpression) a.rewire(this)).toList())
                    .addComments(pii.comments().stream().map(c -> c.rewire(this)).toList())
                    .setSource(pii.source());
        }
        mi.builder().commitParameters().commit();
        // and we don't store it either
        return mi;
    }

    @Override
    public FieldInfo fieldInfo(FieldInfo fieldInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(fieldInfo.owner().primaryType());
        if (map == null) return fieldInfo;
        return (FieldInfo) Objects.requireNonNull(map.get(fieldInfo));
    }

    @Override
    public ParameterInfo parameterInfo(ParameterInfo parameterInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(parameterInfo.typeInfo().primaryType());
        if (map == null) return parameterInfo;
        return (ParameterInfo) Objects.requireNonNull(map.get(parameterInfo));
    }
}
