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
        TypeInfo result = (TypeInfo) map.get(typeInfo);
        if (result == null && registerAnonymousOnDemand(typeInfo)) {
            result = (TypeInfo) map.get(typeInfo);
        }
        if (result == null && isRebuilt(typeInfo.primaryType())) return typeInfo;
        return Objects.requireNonNull(result, "Should have been present: " + typeInfo);
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

    /**
     * The type values of the inner maps of the types we rewired: that is every type the phases created, the ones
     * {@link #typeInfoRecurseAllPhases} made on demand included. The rebuilt (seeded) primary types are excluded —
     * they are not ours, the parse produced and registered them.
     */
    @Override
    public Set<TypeInfo> rewiredTypes() {
        return toRewire.stream()
                .flatMap(primaryType -> setOfPrimaryTypesToRewire.get(primaryType).values().stream())
                .filter(info -> info instanceof TypeInfo)
                .map(info -> (TypeInfo) info)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public TypeInfo typeInfoRecurseAllPhases(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        assert map != null;
        TypeInfo inMap = (TypeInfo) map.get(typeInfo);
        if (inMap != null) return inMap;
        // the enclosing type should already have been done — except for a nested anonymous type, whose enclosing
        // is itself an anonymous type that may not have been reached yet; recurse to register it first.
        TypeInfo enclosingType = typeInfoRecurseAllPhases(typeInfo.compilationUnitOrEnclosingType().getRight());
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
        TypeInfo owner = methodInfo.typeInfo();
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(owner.primaryType());
        if (map == null) return methodInfo;
        MethodInfo result = (MethodInfo) map.get(methodInfo);
        if (result == null && registerAnonymousOnDemand(owner)) {
            result = (MethodInfo) map.get(methodInfo);
        }
        if (result == null && isRebuilt(owner.primaryType())) return methodInfo;
        return Objects.requireNonNull(result, "Cannot find " + methodInfo.fullyQualifiedName());
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
        TypeInfo owner = fieldInfo.owner();
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(owner.primaryType());
        if (map == null) return fieldInfo;
        FieldInfo result = (FieldInfo) map.get(fieldInfo);
        if (result == null && registerAnonymousOnDemand(owner)) {
            result = (FieldInfo) map.get(fieldInfo);
        }
        if (result == null && isRebuilt(owner.primaryType())) return fieldInfo;
        return Objects.requireNonNull(result,
                () -> "Cannot find " + fieldInfo.fullyQualifiedName() + ", owner " + owner
                      + ", primary type " + owner.primaryType()
                      + " (in the rewire set, but this field was never registered)");
    }

    @Override
    public ParameterInfo parameterInfo(ParameterInfo parameterInfo) {
        TypeInfo owner = parameterInfo.typeInfo();
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(owner.primaryType());
        if (map == null) return parameterInfo;
        ParameterInfo result = (ParameterInfo) map.get(parameterInfo);
        if (result == null && registerAnonymousOnDemand(owner)) {
            result = (ParameterInfo) map.get(parameterInfo);
        }
        if (result == null && isRebuilt(owner.primaryType())) return parameterInfo;
        return Objects.requireNonNull(result,
                () -> "Cannot find parameter " + parameterInfo + " of "
                      + parameterInfo.methodInfo().fullyQualifiedName()
                      + " (in the rewire set, but this parameter was never registered)");
    }

    /**
     * A primary type present in the map but <em>rebuilt</em> (reparsed and seeded onto itself), as opposed to one
     * we rewire. Its members are already the new objects, so an absent seed entry (e.g. seed does not walk
     * anonymous types) resolves to identity rather than a failure — see {@link #seed}.
     */
    private boolean isRebuilt(TypeInfo primaryType) {
        return setOfPrimaryTypesToRewire.containsKey(primaryType) && !toRewire.contains(primaryType);
    }

    /**
     * Anonymous and lambda types are not part of {@code subTypes()}, so the structural rewire phases
     * ({@code rewirePhase0/1}) never register them: they are rewired lazily, when the constructor call in the
     * method body or field initializer that creates them is rewired ({@code ConstructorCallImpl.rewire} ->
     * {@link #typeInfoRecurseAllPhases}). A carried analysis value (e.g. {@code METHOD_LINKS}) can name such a
     * member <em>before</em> that body has been rewired — across types in the rewire cone, or simply because
     * within a type methods are rewired before fields ({@code TypeInfoImpl.rewirePhase3}). Rather than fail the
     * lookup, register the anonymous owner on demand here. {@link #typeInfoRecurseAllPhases} is idempotent and
     * recurses through the enclosing chain, so a fully-rewired outermost anonymous ancestor registers every
     * nested anonymous type and subtype beneath it.
     *
     * @return true if it registered something, so the caller should retry its lookup.
     */
    private boolean registerAnonymousOnDemand(TypeInfo owner) {
        TypeInfo primary = owner.primaryType();
        if (!toRewire.contains(primary)) return false; // rebuilt/seeded types are new objects, never rewired
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(primary);
        TypeInfo outermostAbsentAnon = null;
        for (TypeInfo t = owner; t != null;
             t = t.compilationUnitOrEnclosingType().isRight() ? t.compilationUnitOrEnclosingType().getRight() : null) {
            if (t.isAnonymous() && map.get(t) == null) outermostAbsentAnon = t;
        }
        if (outermostAbsentAnon == null) return false;
        typeInfoRecurseAllPhases(outermostAbsentAnon);
        return true;
    }
}
