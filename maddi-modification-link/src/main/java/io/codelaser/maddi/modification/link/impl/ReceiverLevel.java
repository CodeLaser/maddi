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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The receiver of a call decides, next to the callee, whether the call modifies it. Two rules, both sound,
 * neither waiting on an undecided value; gate {@code RECEIVERLEVEL} (environment, presence), off by default
 * while the prototype is measured.
 * <p>
 * <b>Level rule.</b> A call cannot modify a receiver (or an argument) whose object is immutable with hidden
 * content or better: its own fields cannot change through any of its methods, whatever the callee's verdict
 * says. The level is the receiver's <em>effective</em> one: the static type's own verdict (the base type, not
 * the deep immutability of the parameterized type, which folds the type arguments in and says nothing about
 * the container), improved by what the variable is known to HOLD -- {@code IMMUTABLE_FIELD} /
 * {@code IMMUTABLE_PARAMETER}, the dynamic immutability of {@code DynamicImmutability} in the analyzer, until
 * now consumed for independence only. An undecided level is no evidence: the call is judged as before.
 * <p>
 * <b>Cone rule.</b> An abstract callee's verdict is the union over ALL its implementations. The receiver's
 * static type {@code T} bounds the runtime type, so only the implementations in {@code T}'s cone (T and its
 * subtypes) can run: for a source type {@code T} that does not redeclare the callee, the call is judged by the
 * cone's implementations alone. This is the shape that capped vavr's persistent collections ({@code VAVR.md} G1:
 * {@code Value.isEmpty()} is modifying because {@code Iterator} implements it by consuming; a {@code Cons}
 * calling {@code tail.isEmpty()} on a {@code List}-typed field reaches only {@code Cons}/{@code Nil}), and the
 * one that shares the collection interfaces between mutable and immutable implementations in Eclipse
 * Collections and Guava. A contracted callee (annotated {@code @Modified}/{@code @NotModified} at its
 * declaration, or declared in a jar or a hint, where every verdict is a contract) keeps its declaration; a cone
 * without implementations gives no evidence. An undecided implementation counts as modifying, as in
 * {@code AbstractMethodAnalyzerImpl.methodNonModifying}, and the caller's verdict is upgradeable when it decides.
 * <p>
 * Neither rule touches the type-immutability rule: a type's own abstract methods are still folded over the
 * cone by the interface walk of {@code TypeImmutableAnalyzerImpl}; what changes is what a call on a receiver
 * of that type charges to the receiver, and so which fields and parameters come out modified.
 * {@code ShadowModificationPass} mirrors the level rule (a face of a receiver at hc-level contributes no
 * node); the cone rule needs no mirror there once the engine has decided the receiver types.
 */
public final class ReceiverLevel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverLevel.class);
    private static final String GATE = "RECEIVERLEVEL";
    private static final Set<String> CONTRACT_ANNOTATIONS = Set.of("Modified", "NotModified");

    private static volatile boolean enabled = Gate.isSet(GATE);

    static {
        if (enabled) LOGGER.info("Gate {} set: receiver level and cone rules active", GATE);
    }

    private ReceiverLevel() {
        // utility
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Tests only: the gate is an environment variable, read once. */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * The static type's own verdict improved by the variable's dynamic immutability; {@code null} when neither
     * says anything at hc-level or better (an undecided source type, a mutable or final-fields type, a variable
     * without a contract or inference on what it holds).
     */
    public static Value.Immutable effectiveLevel(Variable primary, ParameterizedType staticType) {
        Value.Immutable level = null;
        ParameterizedType pt = staticType != null ? staticType : primary == null ? null : primary.parameterizedType();
        if (pt != null && pt.arrays() == 0) {
            TypeInfo bestType = pt.bestTypeInfo();
            if (bestType != null) {
                Value.Immutable ofType = bestType.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE,
                        ValueImpl.ImmutableImpl.class);
                if (ofType != null && ofType.isAtLeastImmutableHC()) level = ofType;
            }
        }
        Value.Immutable dynamic = dynamicLevel(primary);
        if (dynamic != null) level = level == null ? dynamic : level.max(dynamic);
        return level;
    }

    /** What the variable holds, by contract or inference: {@code IMMUTABLE_FIELD} / {@code IMMUTABLE_PARAMETER}. */
    private static Value.Immutable dynamicLevel(Variable primary) {
        Value.Immutable dynamic = switch (primary) {
            case FieldReference fr -> fr.fieldInfo().analysis().getOrNull(PropertyImpl.IMMUTABLE_FIELD,
                    ValueImpl.ImmutableImpl.class);
            case ParameterInfo pi -> pi.analysis().getOrNull(PropertyImpl.IMMUTABLE_PARAMETER,
                    ValueImpl.ImmutableImpl.class);
            case null, default -> null;
        };
        return dynamic != null && dynamic.isAtLeastImmutableHC() ? dynamic : null;
    }

    /** The level rule: the object the variable holds cannot be modified by any call. */
    public static boolean immutableObject(Variable primary, ParameterizedType staticType) {
        return effectiveLevel(primary, staticType) != null;
    }

    /**
     * The level rule on a declared type alone, for the shadow pass: the base type at hc-level or better. The
     * deep immutability of the parameterized type is what the pass used before this gate; it stays as the
     * second arm, so a fully immutable {@code String} parameter still counts when the gate is off.
     */
    public static boolean immutableBaseType(ParameterizedType pt) {
        if (pt == null || pt.arrays() > 0) return false;
        TypeInfo bestType = pt.bestTypeInfo();
        if (bestType == null) return false;
        Value.Immutable ofType = bestType.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        return ofType != null && ofType.isAtLeastImmutableHC();
    }

    /**
     * The cone rule for the receiver: {@code TRUE} when every implementation of the abstract callee in the cone
     * of the receiver's static type is non-modifying, {@code FALSE} when one is modifying or undecided,
     * {@code null} when the rule does not apply (concrete or contracted callee, jar type, no implementation in
     * the cone, the callee's own type). Every implementation read is reported to {@code dependsOn}: the caller's
     * verdict now rests on it, and the worklist must re-link the caller when it decides (the callee's own
     * summary, which the worklist already tracks, does not change when a cone member does).
     */
    public static Value.Bool coneNonModifying(MethodInfo callee, ParameterizedType receiverType,
                                              Consumer<MethodInfo> dependsOn) {
        List<MethodInfo> cone = coneImplementations(callee, receiverType);
        if (cone == null) return null;
        for (MethodInfo implementation : cone) {
            dependsOn.accept(implementation);
            Value.Bool nonModifying = implementation.analysis().getOrDefault(PropertyImpl.NON_MODIFYING_METHOD,
                    ValueImpl.BoolImpl.FALSE);
            if (nonModifying.isFalse()) return ValueImpl.BoolImpl.FALSE;
        }
        return ValueImpl.BoolImpl.TRUE;
    }

    /** The cone rule for a parameter of the abstract callee, over the implementations' parameter at the same index. */
    public static Value.Bool coneUnmodifiedParameter(ParameterInfo pi, ParameterizedType receiverType,
                                                     Consumer<MethodInfo> dependsOn) {
        List<MethodInfo> cone = coneImplementationsForParameter(pi, receiverType);
        if (cone == null) return null;
        for (MethodInfo implementation : cone) {
            dependsOn.accept(implementation);
            Value.Bool unmodified = implementation.parameters().get(pi.index()).analysis()
                    .getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE);
            if (unmodified.isFalse()) return ValueImpl.BoolImpl.FALSE;
        }
        return ValueImpl.BoolImpl.TRUE;
    }

    /**
     * The implementations of the abstract callee that a receiver of the given static type can dispatch to, or
     * {@code null} when the cone rule does not apply to this call (see {@link #coneNonModifying}). The shadow
     * pass wires a call site's receiver nodes to exactly these, instead of to the callee.
     */
    public static List<MethodInfo> coneImplementations(MethodInfo callee, ParameterizedType receiverType) {
        TypeInfo cone = coneRoot(callee, receiverType);
        if (cone == null || contracted(callee)) return null;
        List<MethodInfo> result = new ArrayList<>();
        for (MethodInfo implementation : implementations(callee)) {
            if (inCone(implementation.typeInfo(), cone)) result.add(implementation);
        }
        return result.isEmpty() ? null : result;
    }

    /** As {@link #coneImplementations}, for the callee's parameter: its own contract counts, and the arity must agree. */
    public static List<MethodInfo> coneImplementationsForParameter(ParameterInfo pi, ParameterizedType receiverType) {
        MethodInfo callee = pi.methodInfo();
        TypeInfo cone = coneRoot(callee, receiverType);
        if (cone == null || contracted(pi)) return null;
        List<MethodInfo> result = new ArrayList<>();
        for (MethodInfo implementation : implementations(callee)) {
            if (!inCone(implementation.typeInfo(), cone)) continue;
            if (pi.index() >= implementation.parameters().size()) return null; // arity differs: no evidence
            result.add(implementation);
        }
        return result.isEmpty() ? null : result;
    }

    /*
    The cone's root: the receiver's static type, when the callee's verdict is a union over implementations, the
    type is a source type strictly below the callee's declaring type (the callee's own type is the full union;
    a jar type's implementations are not registered), and not a type parameter.
     */
    private static TypeInfo coneRoot(MethodInfo callee, ParameterizedType receiverType) {
        if (receiverType == null || receiverType.arrays() > 0) return null;
        if (!io.codelaser.maddi.modification.prepwork.Util.unionOverImplementations(callee)) return null;
        TypeInfo root = receiverType.typeInfo();
        if (root == null || root.equals(callee.typeInfo())) return null;
        if (root.compilationUnit().externalLibrary()) return null;
        if (!root.superTypesExcludingJavaLangObject().contains(callee.typeInfo())) return null;
        return root;
    }

    private static Iterable<MethodInfo> implementations(MethodInfo callee) {
        return callee.analysis().getOrDefault(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY)
                .methodInfoSet();
    }

    private static boolean inCone(TypeInfo typeInfo, TypeInfo root) {
        return typeInfo.equals(root) || typeInfo.superTypesExcludingJavaLangObject().contains(root);
    }

    /*
    An authored @Modified / @NotModified on the declaration is a contract the analyzer's fold does not override
    (ContractResolution); neither does the cone. A jar or hint declaration is excluded earlier (its type is external).
     */
    private static boolean contracted(Info info) {
        for (AnnotationExpression ae : info.annotations()) {
            TypeInfo at = ae.typeInfo();
            if (at != null && CONTRACT_ANNOTATIONS.contains(at.simpleName())) return true;
        }
        return false;
    }
}
