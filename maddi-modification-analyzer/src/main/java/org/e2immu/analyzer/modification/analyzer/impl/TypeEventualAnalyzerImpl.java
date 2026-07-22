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

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeEventualAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeImmutableAnalyzer;
import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.Negation;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUAL_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.FINAL_FIELD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.EventualImpl.NOT_EVENTUAL;

public class TypeEventualAnalyzerImpl extends CommonAnalyzerImpl implements TypeEventualAnalyzer {

    /** which side of the state transition a call places its caller on; ordered by precedence */
    private enum Side {
        MARK, ONLY_BEFORE, ONLY_AFTER
    }

    private final ContractReader contractReader;
    private final TypeImmutableAnalyzer typeImmutableAnalyzer;
    private final EventualCluster eventualCluster;
    // the support classes are reached over and over, from every consumer; re-deriving their contracts each time
    // would be wasteful. Concurrent: the type loop may run in parallel.
    private final Map<TypeInfo, Value.EventuallyImmutable> eventuallyImmutableCache = new ConcurrentHashMap<>();
    private final Map<MethodInfo, Value.Eventual> eventualCache = new ConcurrentHashMap<>();
    private final Map<TypeInfo, Value.Immutable> immutableCache = new ConcurrentHashMap<>();
    private final Map<MethodInfo, Value.Independent> independentCache = new ConcurrentHashMap<>();

    public TypeEventualAnalyzerImpl(Runtime runtime, TypeImmutableAnalyzer typeImmutableAnalyzer,
                                    IteratingAnalyzer.Configuration configuration,
                                    AtomicInteger propertiesChanged, List<Message> analyzerMessages,
                                    EventualCluster eventualCluster) {
        super(configuration, propertiesChanged, analyzerMessages);
        this.contractReader = new ContractReader(runtime);
        this.typeImmutableAnalyzer = typeImmutableAnalyzer;
        this.eventualCluster = eventualCluster;
    }

    /**
     * The eventual contract of a called method. Reading {@code analysis()} is not enough: a compiled type is
     * shallow-analyzed lazily, per method, only where the link computer happened to need it, so a support class
     * arrives here with an empty property map more often than not. The {@link ContractReader} re-derives what the
     * user wrote from the CST, which is exactly what is needed and is what the guard does too.
     */
    private Value.Eventual eventualOf(MethodInfo methodInfo) {
        Value.Eventual fromAnalysis = methodInfo.analysis().getOrDefault(EVENTUAL_METHOD, NOT_EVENTUAL);
        if (fromAnalysis.isEventual()) return fromAnalysis;
        return eventualCache.computeIfAbsent(methodInfo, mi ->
                contractReader.contracts(mi).get(EVENTUAL_METHOD) instanceof Value.Eventual e ? e : NOT_EVENTUAL);
    }

    /** As {@link #eventualOf(MethodInfo)}, for the type-level {@code after="…"}. */
    private Value.EventuallyImmutable eventuallyImmutable(TypeInfo typeInfo) {
        Value.EventuallyImmutable fromAnalysis = typeInfo.analysis()
                .getOrDefault(EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
        if (fromAnalysis.isEventual()) return fromAnalysis;
        return eventuallyImmutableCache.computeIfAbsent(typeInfo, ti ->
                contractReader.contracts(ti).get(EVENTUALLY_IMMUTABLE_TYPE) instanceof Value.EventuallyImmutable e
                        ? e : ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
    }

    @Override
    public void go(TypeInfo typeInfo, boolean activateCycleBreaking) {
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUAL_METHOD)) continue;
            // contracts win over computation. They are materialized into analysis() here rather than only read
            // through the ContractReader, because everything downstream -- codec, guard, the IDE daemon -- looks
            // in analysis(), and for a source method nothing else puts them there.
            Value.Eventual eventual = eventualOf(methodInfo);
            boolean contracted = eventual.isEventual();
            if (!contracted) eventual = computeEventual(typeInfo, methodInfo);
            if (eventual != null && eventual.isEventual()) {
                methodInfo.analysis().set(EVENTUAL_METHOD, eventual);
                DECIDE.debug("TE: {} eventual of method {} = {}", contracted ? "Contracted" : "Decide",
                        methodInfo, eventual);
                propertyChanges.incrementAndGet();
            }
        }
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUALLY_NON_MODIFYING_METHOD)) continue;
            Set<String> labels = computeEventuallyNonModifying(typeInfo, methodInfo);
            if (labels != null && !labels.isEmpty()) {
                methodInfo.analysis().set(EVENTUALLY_NON_MODIFYING_METHOD,
                        new ValueImpl.SetOfStringsImpl(Set.copyOf(labels)));
                DECIDE.debug("TE: Decide eventually-non-modifying of method {} after {}", methodInfo, labels);
                propertyChanges.incrementAndGet();
            }
        }
        // EXPERIMENTAL (EVENTUALCLUSTER): once this type's method-level eventual intent is known, close it upward
        // so its supertypes join the cluster (InfoImpl and the interfaces have no eventual method of their own).
        eventualCluster.noteCandidate(typeInfo);
        computeTypeLevel(typeInfo, activateCycleBreaking);
    }

    /**
     * The type-level verdict: {@code @Immutable(after="…")}. A type is eventually immutable when it holds fields of
     * eventually immutable type that its own methods mark, and it would satisfy the immutability rules once those
     * fields can no longer change. We ask {@link TypeImmutableAnalyzer} for that level rather than re-deriving it,
     * so the eventual and unconditional verdicts cannot drift apart.
     * <p>
     * Only marks on the type's <em>own</em> fields are handled. A type inheriting its mark (a {@code Freezable}
     * subclass) gets its methods annotated, but its own type-level verdict waits for the parent-inheritance step
     * (old {@code approvedPreconditionsFromParent}); see {@code docs/eventual-immutability.md}.
     */
    private void computeTypeLevel(TypeInfo typeInfo, boolean activateCycleBreaking) {
        if (typeInfo.analysis().haveAnalyzedValueFor(EVENTUALLY_IMMUTABLE_TYPE)) return;
        Value.EventuallyImmutable contracted = eventuallyImmutable(typeInfo);
        if (contracted.isEventual()) {
            typeInfo.analysis().set(EVENTUALLY_IMMUTABLE_TYPE, contracted);
            propertyChanges.incrementAndGet();
            return; // contracts win
        }
        // the mark labels of this type's own @Mark methods, and everything that only changes before the mark
        Set<String> markLabels = new HashSet<>();
        Set<FieldInfo> excusedFields = new HashSet<>();
        Set<MethodInfo> excusedMethods = new HashSet<>();
        for (MethodInfo methodInfo : typeInfo.methods()) {
            Value.Eventual eventual = methodInfo.analysis().getOrDefault(EVENTUAL_METHOD, NOT_EVENTUAL);
            if (eventual.isMark()) {
                markLabels.addAll(eventual.fields());
                excusedMethods.add(methodInfo);
                resolveExcusedFields(typeInfo, eventual.fields(), excusedFields);
            } else if (eventual.isOnly() && Boolean.FALSE.equals(eventual.after())) {
                excusedMethods.add(methodInfo);
            }
            // an @Modified accessor that becomes non-modifying after the mark (TypeInfoImpl.access, and after
            // propagation the abstract Info.access) carries the mark just as a @Mark method does: its after-label
            // is the field that transitions. This is the only mark source an interface like Info has -- it
            // declares no @Mark method -- so without it the interface never gets an eventual verdict.
            Value.SetOfStrings nonModAfter = methodInfo.analysis()
                    .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET);
            if (!nonModAfter.set().isEmpty()) {
                markLabels.addAll(nonModAfter.set());
                excusedMethods.add(methodInfo);
                resolveExcusedFields(typeInfo, nonModAfter.set(), excusedFields);
            }
        }
        // §060: an effectively final field of eventually immutable type "will at some point hold objects that are
        // in their final or after state, in which case they act as immutable fields". It rides along with the mark
        // on its own referent -- FieldInfoImpl.type (a ParameterizedType), MethodInfoImpl.typeInfo -- even when no
        // accessor reads through it. Only fired alongside a mark already found: a type with no transition of its
        // own does not become eventual just by holding such a field.
        if (!markLabels.isEmpty()) {
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                if (fieldInfo.isStatic()) continue;
                if (!fieldInfo.analysis().getOrDefault(FINAL_FIELD, ValueImpl.BoolImpl.FALSE).isTrue()) continue;
                if (!excusedFields.contains(fieldInfo) && fieldHoldsCommittableContent(fieldInfo)) {
                    markLabels.add(fieldInfo.name());
                    excusedFields.add(fieldInfo);
                }
            }
        }
        if (markLabels.isEmpty()) return;

        TypeImmutableAnalyzer.AfterMark afterMark =
                new TypeImmutableAnalyzer.AfterMark(Set.copyOf(excusedFields), Set.copyOf(excusedMethods));
        Value.Immutable afterMarkLevel = typeImmutableAnalyzer.immutableAfterMark(typeInfo, afterMark,
                activateCycleBreaking);
        if (afterMarkLevel == null) {
            UNDECIDED.debug("TE: Eventual immutability of type {} undecided", typeInfo);
            return;
        }
        Value.Immutable unconditional = typeInfo.analysis()
                .getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        if (afterMarkLevel.compareTo(unconditional) <= 0) {
            return; // the mark buys nothing; do not claim eventuality the type does not need
        }
        String label = markLabels.stream().sorted().collect(Collectors.joining(","));
        Value.EventuallyImmutable value = new ValueImpl.EventuallyImmutableImpl(label, afterMarkLevel);
        typeInfo.analysis().set(EVENTUALLY_IMMUTABLE_TYPE, value);
        DECIDE.debug("TE: Decide eventual immutability of type {} = {}", typeInfo, value);
        propertyChanges.incrementAndGet();
    }

    /** Add each label's field to {@code excusedFields}, when it resolves to an own field of eventually immutable
     * type. An unresolved label is an inherited mark, or an abstract type's, and simply excuses no field here. */
    private void resolveExcusedFields(TypeInfo typeInfo, Set<String> labels, Set<FieldInfo> excusedFields) {
        for (String label : labels) {
            FieldInfo fieldInfo = typeInfo.getFieldByName(label, false);
            if (fieldInfo != null && fieldHoldsCommittableContent(fieldInfo)) {
                excusedFields.add(fieldInfo);
            }
        }
    }

    /** A field type that excuses a call on it after the mark: really eventually immutable, or -- under the
     *  EVENTUALCLUSTER gate -- a cluster candidate whose verdict is still circular (see {@link EventualCluster}).
     *  {@code member} is the type holding the field, recorded as the assumer when the excusal is optimistic. */
    private boolean isEventuallyImmutableFieldType(TypeInfo member, TypeInfo fieldType) {
        return eventualCluster.treatAsEventuallyImmutable(member, fieldType, eventuallyImmutable(fieldType));
    }

    /** null when nothing can be concluded; never {@code NOT_EVENTUAL} (we do not record absence). */
    private Value.Eventual computeEventual(TypeInfo typeInfo, MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;

        Value.Eventual testMark = computeTestMark(typeInfo, methodInfo);
        if (testMark != null) return testMark;

        Set<String> marked = new HashSet<>();
        Set<String> onlyBefore = new HashSet<>();
        Set<String> onlyAfter = new HashSet<>();
        methodInfo.methodBody().visit(e -> {
            if (e instanceof MethodCall mc) {
                Value.Eventual callee = eventualOf(mc.methodInfo());
                // a @TestMark call is a state *observation*: it says nothing about the caller (an assert on
                // isVariable() must not turn its enclosing method into a @TestMark)
                if (callee.isEventual() && !callee.isTestMark()) {
                    Set<String> labels = labelsOfReceiver(typeInfo, mc, callee);
                    if (labels != null) {
                        switch (side(callee)) {
                            case MARK -> marked.addAll(labels);
                            case ONLY_BEFORE -> onlyBefore.addAll(labels);
                            case ONLY_AFTER -> onlyAfter.addAll(labels);
                        }
                    }
                }
            }
            return true;
        });
        return combine(methodInfo, marked, onlyBefore, onlyAfter);
    }

    /**
     * The labels after which a method is non-modifying, or null when nothing can be concluded. The method-level
     * twin of {@code @Final(after=)}: a getter such as {@code TypeInfoImpl.access()} is {@code @Modified} only
     * because it reads through {@code this.inspection.get()}, and {@code inspection} is eventually immutable, so
     * once {@code inspection} has been committed the call can no longer modify anything. We conclude
     * {@code @NotModified(after="inspection")}, which the interface then inherits (its abstract accessors are
     * {@code @Modified} for exactly the same reason) and which lets the after-mark verdict excuse the method.
     * <p>
     * Sound and conservative: we conclude only when <em>every</em> modification the method can effect is either a
     * call on {@code this.<own eventually immutable field>} (excused after that field's mark, because the field is
     * then immutable) or a {@code this.<eventually-non-modifying method>} forward (excused after its labels). Any
     * own-field assignment, any modifying call on another receiver, or any {@code this} passed into a call, makes
     * us conclude nothing.
     */
    private Set<String> computeEventuallyNonModifying(TypeInfo typeInfo, MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;
        // a method that effects the transition (@Mark) or is confined to one side (@Only) is not "non-modifying
        // after the mark": it belongs to the transition, and calling it after the mark throws. Leave it eventual.
        if (methodInfo.analysis().getOrDefault(EVENTUAL_METHOD, NOT_EVENTUAL).isEventual()) return null;
        // only relevant for a method that DOES modify: a plain non-modifying method needs no after-label
        Value.Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
        if (nonModifying == null || nonModifying.isTrue()) return null;

        Set<String> labels = new HashSet<>();
        boolean[] bail = {false};
        // EVENTUALCLUSTER: locals holding a 'this'-derived value, with the labels committing it (null = poisoned)
        Map<Variable, Set<String>> localCommit = EventualCluster.ENABLED
                ? buildLocalCommitMap(typeInfo, methodInfo) : Map.of();
        methodInfo.methodBody().visit(e -> {
            if (bail[0]) return false;
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr && fr.scopeIsThis()) {
                bail[0] = true; // assigning an own field is a finality/mark concern, not eventual non-modification
                return false;
            }
            if (e instanceof MethodCall mc) {
                Value.Bool calleeNonMod = mc.methodInfo().analysis()
                        .getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                boolean calleeModifies = calleeNonMod == null || calleeNonMod.isFalse();
                if (EventualCluster.ENABLED) {
                    Set<String> excused = commitExcusedLabels(typeInfo, mc, calleeModifies, localCommit);
                    if (excused == null) {
                        bail[0] = true;
                        return false;
                    }
                    labels.addAll(excused);
                    return true;
                }
                if (calleeModifies) {
                    Set<String> excused = nonModifyingLabels(typeInfo, mc);
                    if (excused == null) {
                        bail[0] = true;
                        return false;
                    }
                    labels.addAll(excused);
                }
                // a modifying-through-parameter call could touch 'this' even after the mark; refuse if 'this'
                // (or one of its fields) is handed to any call
                if (mc.parameterExpressions().stream().anyMatch(this::referencesThis)) {
                    bail[0] = true;
                    return false;
                }
            }
            return true;
        });
        if (bail[0]) return null;
        return labels;
    }

    /**
     * The labels excusing a modifying call, or null when it cannot be excused: a call on {@code this.<own
     * eventually immutable field>} (label = that field, immutable after its mark), or a {@code this.<own
     * eventually-non-modifying method>} forward (labels = the callee's).
     */
    private Set<String> nonModifyingLabels(TypeInfo typeInfo, MethodCall mc) {
        // the call must be one that survives the mark: a @Mark or @Only(before) call is the transition itself
        // (setFinal, setVariable), not a post-mark read, and calling it after the mark throws
        Value.Eventual calleeEventual = eventualOf(mc.methodInfo());
        if (calleeEventual.isMark() || calleeEventual.isOnly() && Boolean.FALSE.equals(calleeEventual.after())) {
            return null;
        }
        // this.<eventually-non-modifying method>() forward: the callee is non-modifying after its own labels
        if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
            Value.SetOfStrings calleeLabels = mc.methodInfo().analysis()
                    .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET);
            return calleeLabels.set().isEmpty() ? null : calleeLabels.set();
        }
        // a call on this.<field>, or on a non-modifying accessor chained off it (this.field.getRight().m())
        return receiverAfterLabels(typeInfo, mc.object());
    }

    /**
     * The labels after which the receiver expression is guaranteed committed (so a call on it cannot modify), or
     * null. Bottoms out at {@code this.<own field holding committable content>}; follows a chain of
     * <em>non-modifying</em> accessors through it, which is how {@code this.compilationUnitOrEnclosingType.getRight()}
     * -- a plain read on an immutable {@code Either} wrapping an eventually immutable {@code TypeInfo} -- resolves to
     * the field {@code compilationUnitOrEnclosingType}.
     */
    private Set<String> receiverAfterLabels(TypeInfo typeInfo, Expression object) {
        if (object instanceof VariableExpression ve
            && ve.variable() instanceof FieldReference fr && fr.scopeIsThis()) {
            FieldInfo fieldInfo = fr.fieldInfo();
            if (!isOwnField(typeInfo, fieldInfo) || !fieldHoldsCommittableContent(fieldInfo)) return null;
            return Set.of(fieldInfo.name());
        }
        if (object instanceof MethodCall inner) {
            // the chained step (getLeft/getRight/get) must itself be non-modifying, and rooted in an own field
            if (!isNonModifyingRead(inner.methodInfo())) return null;
            return receiverAfterLabels(typeInfo, inner.object());
        }
        return null;
    }

    /**
     * EVENTUALCLUSTER reframe of the per-call excusal (handoff: {@code
     * docs/handoff-eventual-interface-nonmodification.md} §5): a call cannot modify {@code this} after mark M iff
     * every {@code this}-derived value it touches -- its receiver <em>and</em> its arguments -- is committed by M.
     * Replaces both the receiver-only rooting of {@link #nonModifyingLabels} and the all-or-nothing parameter
     * guard, which bail on the real cross-reference accessors ({@code returnType().typeInfo().isEnclosedIn(this
     * .typeInfo)}). Null = bail: the enclosing method is not eventually-non-modifying via this path.
     */
    private Set<String> commitExcusedLabels(TypeInfo typeInfo, MethodCall mc, boolean calleeModifies,
                                            Map<Variable, Set<String>> localCommit) {
        // a @Mark or @Only(before) callee is the transition itself (setFinal, setVariable), not a post-mark
        // read; calling it after the mark throws
        Value.Eventual calleeEventual = eventualOf(mc.methodInfo());
        if (calleeEventual.isMark() || calleeEventual.isOnly() && Boolean.FALSE.equals(calleeEventual.after())) {
            return null;
        }
        Set<String> labels = new HashSet<>();
        if (calleeModifies) {
            // only a modifying callee can modify its receiver
            if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                // this.<accessor>() forward: the accessor itself must be eventually-non-modifying
                Set<String> enm = mc.methodInfo().analysis()
                        .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET).set();
                if (enm.isEmpty()) return null;
                labels.addAll(enm);
            } else {
                // top level: only the receiver needs committing; the call's own return value feeds nothing here
                Set<String> receiver = commitLabels(typeInfo, mc.object(), localCommit);
                if (receiver == null) return null;
                labels.addAll(receiver);
            }
        }
        // a call may modify a this-derived argument (a @Modified parameter); commit each
        for (Expression arg : mc.parameterExpressions()) {
            Set<String> argLabels = commitLabels(typeInfo, arg, localCommit);
            if (argLabels == null) return null;
            labels.addAll(argLabels);
        }
        return labels;
    }

    /**
     * EVENTUALCLUSTER only. The mark labels after which {@code expr}'s value can no longer be used to modify
     * {@code owner}'s ({@code this}'s) accessible state: the empty set when the value is not derived from
     * {@code this} at all (a parameter, a fresh object, a constant, a static reference); a non-empty set when it
     * is {@code this}-derived but committed (immutable) once those marks have passed; null when it is
     * {@code this}-derived and cannot be shown committable -- bail. Bare {@code this} is never committable from
     * the inside: mid-transition, it still has its mutable half.
     */
    private Set<String> commitLabels(TypeInfo owner, Expression expr, Map<Variable, Set<String>> localCommit) {
        return commitLabels(owner, expr, localCommit, 0);
    }

    // how many same-class single-return forwards commitLabels may look through (guards against recursion)
    private static final int MAX_LOOK_THROUGH = 3;

    private Set<String> commitLabels(TypeInfo owner, Expression expr, Map<Variable, Set<String>> localCommit,
                                     int depth) {
        if (expr == null || !referencesThisOrTracked(expr, localCommit)) return Set.of();
        if (expr instanceof VariableExpression ve) {
            if (ve.variable() instanceof FieldReference fr && fr.scopeIsThis()) {
                FieldInfo fieldInfo = fr.fieldInfo();
                if (isOwnField(owner, fieldInfo) && fieldHoldsCommittableContent(fieldInfo)) {
                    return Set.of(fieldInfo.name());
                }
                return null; // a non-committable field of this
            }
            if (localCommit.containsKey(ve.variable())) return localCommit.get(ve.variable()); // may be null
            return null; // bare this (mid-transition), or another this-referencing variable shape
        }
        if (expr instanceof MethodCall mc) {
            // an intermediate @Mark or @Only(before) call belongs to the transition; after the mark it throws
            Value.Eventual calleeEventual = eventualOf(mc.methodInfo());
            if (calleeEventual.isMark() || calleeEventual.isOnly() && Boolean.FALSE.equals(calleeEventual.after())) {
                return null;
            }
            Set<String> acc = new HashSet<>();
            // is the receiver a this-derived value that is COMMITTED once acc's labels have passed? (bare this
            // never is: mid-transition it still has its mutable half)
            boolean receiverCommitted;
            if (mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                // this.<accessor>(): the method boundary erases WHICH committed field the result was read
                // through, which handedOnValueSafe often needs -- so look through a same-class single-return
                // forward first, evaluating its body expression in place (one level of inlining)
                if (depth < MAX_LOOK_THROUGH && mc.methodInfo().typeInfo() == owner) {
                    Expression returned = singleReturnExpression(mc.methodInfo());
                    if (returned != null) {
                        Set<String> through = commitLabels(owner, returned, Map.of(), depth + 1);
                        if (through != null) {
                            acc.addAll(through);
                            return commitArguments(owner, mc, localCommit, depth, acc);
                        }
                    }
                }
                // a genuinely non-modifying accessor contributes nothing; a modifying one must be
                // eventually-non-modifying, and contributes its labels
                if (!isNonModifyingRead(mc.methodInfo())) {
                    Set<String> enm = mc.methodInfo().analysis()
                            .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                            .set();
                    if (enm.isEmpty()) return null;
                    acc.addAll(enm);
                }
                receiverCommitted = false;
            } else {
                Set<String> receiver = commitLabels(owner, mc.object(), localCommit, depth);
                if (receiver == null) return null;
                if (receiver.isEmpty()) {
                    // the receiver is not this-derived; neither is anything obtained from it -- only the
                    // arguments still need committing
                    return commitArguments(owner, mc, localCommit, depth, acc);
                }
                acc.addAll(receiver);
                receiverCommitted = true;
            }
            // the aliasing trap (handoff §6, caveat 1): the value an INTERMEDIATE call hands on must not be
            // usable to modify this-derived state downstream
            if (!handedOnValueSafe(owner, mc, receiverCommitted)) return null;
            return commitArguments(owner, mc, localCommit, depth, acc);
        }
        return null; // any other this-referencing expression shape: conservative
    }

    /** The expression of a body that is nothing but {@code return <expr>;}, else null. */
    private static Expression singleReturnExpression(MethodInfo methodInfo) {
        if (methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;
        List<Statement> statements = methodInfo.methodBody().statements().stream()
                .filter(s -> !s.isSynthetic()).toList();
        if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs)) return null;
        return rs.expression();
    }

    /** Commit each argument of the call, folding the labels into {@code acc}; null = bail. */
    private Set<String> commitArguments(TypeInfo owner, MethodCall mc, Map<Variable, Set<String>> localCommit,
                                        int depth, Set<String> acc) {
        for (Expression arg : mc.parameterExpressions()) {
            Set<String> argLabels = commitLabels(owner, arg, localCommit, depth);
            if (argLabels == null) return null;
            acc.addAll(argLabels);
        }
        return acc;
    }

    /**
     * Can the value an intermediate chain call hands on be used, downstream, to modify {@code owner}'s
     * ({@code this}'s) mutable state? Decided from the callee's independence, which says exactly what the
     * result shares with its receiver:
     * <ul>
     * <li>{@code @Independent}: nothing mutable -- always safe.</li>
     * <li>{@code @Dependent} (accessible content): safe when the receiver is committed, because a committed
     *     object's accessible content is immutable; NOT safe off bare {@code this} (the {@code getItems()}
     *     trap: a non-modifying accessor handing out a mutable this-field).</li>
     * <li>{@code @Independent(hc=true)} (hidden content, e.g. {@code Collection.stream},
     *     {@code Either.getRight}): the wrapper layer is fresh by the contract; safe when the shared content
     *     -- the concrete return type's parameters -- is committable ({@code Stream<MethodModifier>}).</li>
     * </ul>
     * A modifying or independence-undecided callee falls back to the return type itself being committable
     * ({@link #returnTypeHoldsCommittableContent}), which also covers the hc-read whose result IS the hidden
     * content ({@code EventuallyFinalOnDemand.get()} returning a cluster candidate).
     */
    private boolean handedOnValueSafe(TypeInfo owner, MethodCall mc, boolean receiverCommitted) {
        if (isNonModifyingRead(mc.methodInfo())) {
            Value.Independent independent = independentOf(mc.methodInfo());
            if (independent != null) {
                if (independent.isIndependent()) return true;
                if (independent.isDependent()) {
                    if (receiverCommitted) return true;
                } else if (typeParametersHoldCommittableContent(owner, mc.concreteReturnType())) {
                    return true;
                }
            }
        }
        return returnTypeHoldsCommittableContent(owner, mc);
    }

    /** All type parameters committable (seed + witness) or at least immutable-hc -- with at least one present:
     *  a parameterless type offers no handle on what an hc-read's result shares. */
    private boolean typeParametersHoldCommittableContent(TypeInfo owner, ParameterizedType parameterizedType) {
        if (parameterizedType.parameters().isEmpty()) return false;
        for (ParameterizedType param : parameterizedType.parameters()) {
            TypeInfo paramType = param.bestTypeInfo();
            if (paramType == null) return false;
            if (!isEventuallyImmutableFieldType(owner, paramType)
                && !immutableOf(paramType).isAtLeastImmutableHC()) {
                return false;
            }
        }
        return true;
    }

    // sentinel for "no verdict, no contract" -- must NOT read as dependent, which would skip the hidden-content
    // check; kept out of the Value.Independent lattice on purpose
    private static final Value.Independent INDEPENDENT_UNDECIDED =
            new ValueImpl.IndependentImpl(-1, Map.of(), List.of());

    /** {@code INDEPENDENT_METHOD}, with the {@link ContractReader} fallback for a jar method whose contract was
     *  never materialised into {@code analysis()}. Mirrors {@link #immutableOf}; null when truly undecided. */
    private Value.Independent independentOf(MethodInfo methodInfo) {
        Value.Independent fromAnalysis = methodInfo.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
        if (fromAnalysis != null) return fromAnalysis;
        Value.Independent cached = independentCache.computeIfAbsent(methodInfo, mi ->
                contractReader.contracts(mi).get(PropertyImpl.INDEPENDENT_METHOD) instanceof Value.Independent i
                        ? i : INDEPENDENT_UNDECIDED);
        return cached == INDEPENDENT_UNDECIDED ? null : cached;
    }

    /**
     * The twin of {@link #fieldHoldsCommittableContent} for the value an intermediate chain call hands on,
     * operating on the concrete return type: committable when eventually immutable or a cluster candidate
     * (seed + witness, via {@link #isEventuallyImmutableFieldType}), deeply immutable (a primitive, String), or an
     * immutable single-indirection wrapper ({@code Either}, {@code Option}) of committable content.
     */
    private boolean returnTypeHoldsCommittableContent(TypeInfo member, MethodCall mc) {
        ParameterizedType returnType = mc.concreteReturnType();
        if (returnType.isPrimitiveExcludingVoid() || returnType.isVoid()) return true; // a value cannot alias
        TypeInfo rtType = returnType.bestTypeInfo();
        if (rtType == null) return false; // an unresolved type parameter: conservative
        if (isEventuallyImmutableFieldType(member, rtType)) return true;
        Value.Immutable immutable = immutableOf(rtType);
        if (immutable.isImmutable()) return true; // no hidden content at all: nothing mutable to alias
        if (!immutable.isAtLeastImmutableHC()) return false; // reads through it might expose mutable content
        for (ParameterizedType param : returnType.parameters()) {
            TypeInfo paramType = param.bestTypeInfo();
            if (paramType != null && isEventuallyImmutableFieldType(member, paramType)) return true;
        }
        return false;
    }

    /**
     * EVENTUALCLUSTER only. Locals (and reassigned parameters) that hold a {@code this}-derived value, mapped to
     * the labels committing that value (null = poisoned: this-derived but not committable). The handoff spec
     * treats every local as "not this-derived" ({@code ∅}); that is exactly the aliasing trap one hop removed
     * ({@code var l = this.items; l.add(x)}), so instead the map is built flow-insensitively over the whole body
     * (a use before the assignment in a loop is still caught) and iterated for chained locals
     * ({@code var b = a;}). Locals never assigned anything this-derived stay untracked, which is the spec's
     * {@code ∅}: a fresh {@code new ArrayList<>()} builder local stays excusable.
     */
    private Map<Variable, Set<String>> buildLocalCommitMap(TypeInfo typeInfo, MethodInfo methodInfo) {
        Map<Variable, Set<String>> map = new HashMap<>();
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 10) {
            boolean[] change = {false};
            methodInfo.methodBody().visit(e -> {
                if (e instanceof LocalVariableCreation lvc) {
                    lvc.localVariableStream().forEach(lv ->
                            change[0] |= trackAssignment(typeInfo, map, lv, lv.assignmentExpression()));
                } else if (e instanceof Assignment a
                           && !(a.variableTarget() instanceof FieldReference)
                           && !(a.variableTarget() instanceof This)) {
                    change[0] |= trackAssignment(typeInfo, map, a.variableTarget(), a.value());
                }
                return true;
            });
            changed = change[0];
        }
        return map;
    }

    /** Fold one assignment into the map; true when the map changed. Once poisoned (null), a variable stays so. */
    private boolean trackAssignment(TypeInfo typeInfo, Map<Variable, Set<String>> map, Variable target,
                                    Expression value) {
        if (value == null || value.isEmpty()) return false;
        if (!referencesThisOrTracked(value, map)) return false; // not this-derived: stays untracked (∅)
        Set<String> labels = commitLabels(typeInfo, value, map);
        if (map.containsKey(target)) {
            Set<String> existing = map.get(target);
            if (existing == null) return false; // already poisoned
            if (labels == null) {
                map.put(target, null);
                return true;
            }
            return existing.addAll(labels);
        }
        map.put(target, labels == null ? null : new HashSet<>(labels));
        return true;
    }

    /** {@link #referencesThis}, extended with the locals tracked as holding this-derived values. */
    private boolean referencesThisOrTracked(Expression expression, Map<Variable, Set<String>> tracked) {
        boolean[] found = {false};
        expression.visit(e -> {
            if (e instanceof VariableExpression ve
                && (ve.variable() instanceof This
                    || ve.variable() instanceof FieldReference fr && fr.scopeIsThis()
                    || tracked.containsKey(ve.variable()))) {
                found[0] = true;
                return false;
            }
            return !found[0];
        });
        return found[0];
    }

    /** Non-modifying by its own verdict, or -- for a jar accessor whose verdict was never materialized --
     *  because it is declared on an immutable type, whose methods cannot modify anything (e.g. Either.getRight). */
    private boolean isNonModifyingRead(MethodInfo methodInfo) {
        Value.Bool nm = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
        if (nm != null) return nm.isTrue();
        return immutableOf(methodInfo.typeInfo()).isAtLeastImmutableHC();
    }

    /**
     * A field whose content becomes immutable after the mark on it: it is itself of eventually immutable type
     * (or a cluster candidate under the gate), or an immutable single-indirection wrapper ({@code Either},
     * {@code Option}) of such content -- once the wrapped object is committed, reads through the wrapper cannot
     * modify anything.
     */
    private boolean fieldHoldsCommittableContent(FieldInfo fieldInfo) {
        // the member leaning on the field's type is the type that declares the field
        TypeInfo member = fieldInfo.owner();
        TypeInfo fieldType = fieldInfo.type().bestTypeInfo();
        if (fieldType == null) return false;
        if (isEventuallyImmutableFieldType(member, fieldType)) return true;
        if (!immutableOf(fieldType).isAtLeastImmutableHC()) return false; // the wrapper read must not itself modify
        for (ParameterizedType arg : fieldInfo.type().parameters()) {
            TypeInfo argType = arg.bestTypeInfo();
            if (argType != null && isEventuallyImmutableFieldType(member, argType)) return true;
        }
        return false;
    }

    /** {@code IMMUTABLE_TYPE}, with the {@link ContractReader} fallback for a jar type whose contract was never
     *  materialised into {@code analysis()} (e.g. the support class {@code Either}). Mirrors {@link #eventualOf}. */
    private Value.Immutable immutableOf(TypeInfo typeInfo) {
        Value.Immutable fromAnalysis = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        if (fromAnalysis != null) return fromAnalysis;
        return immutableCache.computeIfAbsent(typeInfo, ti ->
                contractReader.contracts(ti).get(IMMUTABLE_TYPE) instanceof Value.Immutable i
                        ? i : ValueImpl.ImmutableImpl.MUTABLE);
    }

    private boolean referencesThis(Expression expression) {
        boolean[] found = {false};
        expression.visit(e -> {
            if (e instanceof VariableExpression ve
                && (ve.variable() instanceof This
                    || ve.variable() instanceof FieldReference fr && fr.scopeIsThis())) {
                found[0] = true;
                return false;
            }
            return !found[0];
        });
        return found[0];
    }

    /**
     * The mark labels a call contributes to its caller, or null when the call does not act on this type's state.
     * Two shapes carry the state: a call on a field of this type whose own type is eventually immutable (then the
     * label is that field), and a call on {@code this} to a method that is itself marked (then the labels are the
     * callee's own, typically an inherited {@code Freezable.frozen}).
     */
    private Set<String> labelsOfReceiver(TypeInfo typeInfo, MethodCall mc, Value.Eventual callee) {
        if (!(mc.object() instanceof VariableExpression ve)) return null;
        if (ve.variable() instanceof This) {
            return callee.fields();
        }
        if (ve.variable() instanceof FieldReference fr && fr.scopeIsThis()) {
            FieldInfo fieldInfo = fr.fieldInfo();
            if (!isOwnField(typeInfo, fieldInfo)) return null;
            // the field's type must be the eventually immutable one; otherwise the callee's mark is about
            // something else entirely and must not be re-labelled with our field's name
            TypeInfo fieldType = fieldInfo.type().bestTypeInfo();
            if (fieldType == null) return null;
            if (!eventuallyImmutable(fieldType).isEventual()) return null;
            // the label is OUR field, not the support class's internal field name: the transition our callers
            // observe is "this.inspection has been set", not "EventuallyFinalOnDemand.isFinal"
            return Set.of(fieldInfo.name());
        }
        return null;
    }

    private static boolean isOwnField(TypeInfo typeInfo, FieldInfo fieldInfo) {
        return typeInfo.fields().contains(fieldInfo);
    }

    private static Side side(Value.Eventual callee) {
        if (callee.isMark()) return Side.MARK;
        return Boolean.TRUE.equals(callee.after()) ? Side.ONLY_AFTER : Side.ONLY_BEFORE;
    }

    /**
     * A method belongs to exactly one side of the transition. Mixing sides — marking one field while requiring
     * another to be untouched — is not something we can express, so we conclude nothing rather than guess.
     * {@code @Mark} additionally requires the method to modify: a method known not to modify cannot have effected
     * a state transition, and a claim to the contrary would contradict phase 1.
     */
    private Value.Eventual combine(MethodInfo methodInfo, Set<String> marked, Set<String> onlyBefore,
                                   Set<String> onlyAfter) {
        int sides = (marked.isEmpty() ? 0 : 1) + (onlyBefore.isEmpty() ? 0 : 1) + (onlyAfter.isEmpty() ? 0 : 1);
        if (sides != 1) {
            if (sides > 1) {
                UNDECIDED.debug("TE: Eventual of method {} undecided: mixed sides {} {} {}", methodInfo,
                        marked, onlyBefore, onlyAfter);
            }
            return null;
        }
        if (!marked.isEmpty()) {
            Value.Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
            if (nonModifying == null) return null; // phase 1 has not decided yet; try again next iteration
            if (nonModifying.isTrue()) return null;
            return new ValueImpl.EventualImpl(Set.copyOf(marked), true, null, null);
        }
        if (!onlyBefore.isEmpty()) {
            return new ValueImpl.EventualImpl(Set.copyOf(onlyBefore), false, false, null);
        }
        return new ValueImpl.EventualImpl(Set.copyOf(onlyAfter), false, true, null);
    }

    /**
     * {@code @TestMark} propagates only through a method whose whole body is {@code return <testmark call>} or
     * {@code return !<testmark call>}. Anything looser would classify every method that happens to consult the
     * state — an {@code assert}, a guard — as a state test, which it is not.
     */
    private Value.Eventual computeTestMark(TypeInfo typeInfo, MethodInfo methodInfo) {
        if (!methodInfo.returnType().isBoolean()) return null;
        List<Statement> statements = methodInfo.methodBody().statements().stream()
                .filter(s -> !s.isSynthetic()).toList();
        if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs)) return null;
        Expression expression = rs.expression();
        boolean negated = expression instanceof Negation;
        if (negated) expression = ((Negation) expression).expression();
        if (!(expression instanceof MethodCall mc)) return null;
        Value.Eventual callee = eventualOf(mc.methodInfo());
        if (!callee.isTestMark()) return null;
        Set<String> labels = labelsOfReceiver(typeInfo, mc, callee);
        if (labels == null) return null;
        // callee.test() is true for an 'isSet' style test; a negation in our body flips the sense
        return new ValueImpl.EventualImpl(labels, false, null, callee.test() ^ negated);
    }
}
