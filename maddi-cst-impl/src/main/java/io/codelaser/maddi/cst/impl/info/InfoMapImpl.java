/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.impl.element.CompilationUnitImpl;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.statement.BlockImpl;

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
    // see InfoMapView#rewired: one copy per original object, so that identity-keyed DetailedSources find it
    private final java.util.IdentityHashMap<Object, Object> rewiredObjects = new java.util.IdentityHashMap<>();
    private final boolean carryIntrinsic;

    public InfoMapImpl(Set<TypeInfo> setOfPrimaryTypesToRewire) {
        this(setOfPrimaryTypesToRewire, Set.of(), false);
    }

    public InfoMapImpl(Set<TypeInfo> setOfPrimaryTypesToRewire, Set<TypeInfo> rebuiltPrimaryTypes) {
        this(setOfPrimaryTypesToRewire, rebuiltPrimaryTypes, false);
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
     * @param carryIntrinsic            a partial re-parse: carry the intrinsic tier, see {@link #carryIntrinsic()}
     */
    public InfoMapImpl(Set<TypeInfo> setOfPrimaryTypesToRewire, Set<TypeInfo> rebuiltPrimaryTypes,
                       boolean carryIntrinsic) {
        this.carryIntrinsic = carryIntrinsic;
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

    @Override
    public boolean carryIntrinsic() {
        return carryIntrinsic;
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T rewired(T original, java.util.function.Supplier<T> rewire) {
        Object done = rewiredObjects.get(original);
        if (done != null) return (T) done;
        T result = rewire.get();
        // the computation may have registered this very object on the way (a type parameter does, before its bounds)
        Object registered = rewiredObjects.putIfAbsent(original, result);
        return registered == null ? result : (T) registered;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T rewiredIfPresent(T original) {
        return (T) rewiredObjects.get(original);
    }

    @Override
    public void registerRewired(Object original, Object rewired) {
        rewiredObjects.put(original, rewired);
    }

    @Override
    public TypeInfo typeInfo(TypeInfo typeInfo) {
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(typeInfo.primaryType());
        if (map == null) return typeInfo;
        TypeInfo result = (TypeInfo) map.get(typeInfo);
        if (result == null && registerMethodEnclosedOnDemand(typeInfo)) {
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
        // every copy exists now: the units the copies were built in (see TypeInfoImpl.rewirePhase0) get their types
        for (TypeInfo primaryType : toRewire) {
            if (typeInfo(primaryType).compilationUnit() instanceof CompilationUnitImpl unit
                && unit != primaryType.compilationUnit()) {
                unit.rewireTypes(primaryType.compilationUnit(), this);
            }
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
        // the copies are made; kept, the memo would hold this generation's originals for as long as the map lives
        // (the inspector keeps the last one for its lookups)
        rewiredObjects.clear();
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
        if (result == null && registerMethodEnclosedOnDemand(owner)) {
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
                .setSource(methodInfo.rewireSource(this))
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
                    .setSource(pii.rewireSource(this));
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
        if (owner.fields().stream().noneMatch(f -> f == fieldInfo)) {
            // a field the analysis made rather than the parser: the link engine's virtual fields (§...) and its
            // hierarchy translations (a superclass field seen on a subtype, or a copy of a declared one) are FieldInfo
            // objects owned by a type without being among its fields. Prep's data holds none; the analyzer output a
            // seeded analysis carries does (CarryDerived). A copy on the rewired owner, made once per rewire like
            // every other copy. Before the lookup below: the map finds by name, and handing such a copy the declared
            // field instead changes how the analysis encodes it (Vstats against Fstats(17) on fernflower)
            return rewired(fieldInfo, () -> new FieldInfoImpl(fieldInfo.name(), fieldInfo.isStatic(),
                    fieldInfo.type().rewire(this), typeInfo(owner)));
        }
        FieldInfo result = (FieldInfo) map.get(fieldInfo);
        if (result == null && registerMethodEnclosedOnDemand(owner)) {
            result = (FieldInfo) map.get(fieldInfo);
        }
        if (result == null && isRebuilt(owner.primaryType())) return fieldInfo;
        if (result == null && owner.getFieldByName(fieldInfo.name(), false) == null) {
            return rewired(fieldInfo, () -> rewiredNonMember(fieldInfo, owner));
        }
        return Objects.requireNonNull(result,
                () -> "Cannot find " + fieldInfo.fullyQualifiedName() + ", owner " + owner
                      + ", primary type " + owner.primaryType()
                      + " (in the rewire set, but this field was never registered)");
    }

    /*
     A field that is no member of its owner, so that no rewire phase registered it. Two kinds:
     - a field of a supertype, seen through the type that inherits it: the link engine translates a callee's 'this'
       into its caller's, and FieldInfoImpl.translate then gives an inherited field the caller's type as its owner.
       Its copy is the rewired declaration, owned by the rewired owner, with the view's own (rewired) type.
     - a virtual field of the link engine (§$, §$s, ...: VirtualFieldComputer), the hidden content of a type that
       declares none, such as one extending a library container. Its copy is itself, owned by the rewired owner, with
       its type rewired; it has no declaration, and its inspection (an empty initializer) names nothing of a generation.
     A member that is missing is not one of these: the caller still fails on it.
     */
    private FieldInfo rewiredNonMember(FieldInfo fieldInfo, TypeInfo owner) {
        TypeInfo rewiredOwner = typeInfo(owner);
        ParameterizedType rewiredType = fieldInfo.type().rewire(this);
        for (TypeInfo superType : owner.superTypesExcludingJavaLangObject()) {
            FieldInfo declared = superType.getFieldByName(fieldInfo.name(), false);
            if (declared != null && declared.owner() == superType) {
                return ((FieldInfoImpl) fieldInfo(declared)).reowned(rewiredOwner, rewiredType);
            }
        }
        return ((FieldInfoImpl) fieldInfo).reowned(rewiredOwner, rewiredType);
    }

    @Override
    public ParameterInfo parameterInfo(ParameterInfo parameterInfo) {
        TypeInfo owner = parameterInfo.typeInfo();
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(owner.primaryType());
        if (map == null) return parameterInfo;
        ParameterInfo result = (ParameterInfo) map.get(parameterInfo);
        if (result == null && registerMethodEnclosedOnDemand(owner)) {
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
     * A type declared inside a method body — anonymous, lambda, or a <em>local class</em> — is not part of
     * {@code subTypes()}, so the structural rewire phases ({@code rewirePhase0/1}) never register it: it is
     * rewired lazily, when the thing that declares it is rewired ({@code ConstructorCallImpl.rewire} for an
     * anonymous type, {@code LocalTypeDeclarationImpl.rewire} for a local class, both via
     * {@link #typeInfoRecurseAllPhases}). A carried analysis value (e.g. {@code METHOD_LINKS}) can name such a
     * member <em>before</em> that body has been rewired — across types in the rewire cone, or simply because
     * within a type methods are rewired before fields ({@code TypeInfoImpl.rewirePhase3}). Rather than fail the
     * lookup, register the owner on demand here. {@link #typeInfoRecurseAllPhases} is idempotent and recurses
     * through the enclosing chain, so a fully-rewired outermost method-enclosed ancestor registers every nested
     * type and subtype beneath it.
     * <p>
     * The test is {@code enclosingMethod() != null}, which is what "declared inside a method body" means
     * ({@link TypeInfo#enclosingMethod()}: "the method whose body syntactically contains this type
     * declaration"). It used to be {@code isAnonymous()}, which is narrower — that is
     * {@code simpleName.startsWith("$")}, true for anonymous and lambda types only, while a local class keeps
     * its written name ({@code TypeInfoImpl(MethodInfo, String, int)}). A local class was therefore never
     * registered and the lookup failed with "Should have been present". It surfaces only when something
     * rewrites the enclosing method — an ordinary parse never asks — which is why it outlived the anonymous
     * case. Measured on a decompiler corpus: 21 methods across 3 types, each losing a whole primary type.
     *
     * @return true if it registered something, so the caller should retry its lookup.
     */
    private boolean registerMethodEnclosedOnDemand(TypeInfo owner) {
        TypeInfo primary = owner.primaryType();
        if (!toRewire.contains(primary)) return false; // rebuilt/seeded types are new objects, never rewired
        Map<Info, Info> map = setOfPrimaryTypesToRewire.get(primary);
        TypeInfo outermostAbsent = null;
        for (TypeInfo t = owner; t != null;
             t = t.compilationUnitOrEnclosingType().isRight() ? t.compilationUnitOrEnclosingType().getRight() : null) {
            if (t.enclosingMethod() != null && map.get(t) == null) outermostAbsent = t;
        }
        if (outermostAbsent == null) return false;
        typeInfoRecurseAllPhases(outermostAbsent);
        return true;
    }
}
