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
import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.EnclosedExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.InlineConditional;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.Negation;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.AssertStatement;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.ThrowStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUAL_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.FINAL_FIELD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.EVENTUALLY_UNMODIFIED_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.NON_MODIFYING_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.UNMODIFIED_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.EventualImpl.NOT_EVENTUAL;

public class TypeEventualAnalyzerImpl extends CommonAnalyzerImpl implements TypeEventualAnalyzer {

    // log-only diagnostic gate (MODREACH_EXPLAIN style): substring of a type FQN to trace in computeTypeLevel
    private static final String EC_TYPE_DEBUG = System.getenv("EC_TYPE_DEBUG");

    private static boolean ecTypeDebug(TypeInfo typeInfo) {
        if (EC_TYPE_DEBUG == null) return false;
        for (String part : EC_TYPE_DEBUG.split(",")) {
            if (!part.isBlank() && typeInfo.fullyQualifiedName().contains(part)) return true;
        }
        return false;
    }

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

    /** Diagnostic only (EC_SITE_DEBUG): is the current computation one the user asked to trace? */
    private boolean siteDebug() {
        return EventualCluster.SITE_DEBUG && EventualCluster.siteDebugMatches(eventualCluster.debugContext());
    }

    /** Diagnostic only: an ECSITE line attributed to the current computation. */
    private void ecsite(String message) {
        EventualCluster.sitePrint("[" + eventualCluster.debugContext() + "] " + message);
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
        // every computation runs inside an assumption buffer: only a computation that LANDS its property
        // flushes its optimistic edges into the contraction's ledger (success-only witnessing) -- a bailed
        // attempt, retried and possibly succeeding via a different path, must not leave vestigial edges
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUAL_METHOD)) continue;
            // contracts win over computation. They are materialized into analysis() here rather than only read
            // through the ContractReader, because everything downstream -- codec, guard, the IDE daemon -- looks
            // in analysis(), and for a source method nothing else puts them there.
            Value.Eventual eventual = eventualOf(methodInfo);
            boolean contracted = eventual.isEventual();
            eventualCluster.setDebugContext("eventual " + methodInfo.fullyQualifiedName());
            eventualCluster.beginAssumptionBuffer();
            if (!contracted) eventual = computeEventual(typeInfo, methodInfo);
            if (eventual != null && eventual.isEventual()) {
                if (siteDebug()) ecsite("WRITE eventual=" + eventual);
                methodInfo.analysis().set(EVENTUAL_METHOD, eventual);
                DECIDE.debug("TE: {} eventual of method {} = {}", contracted ? "Contracted" : "Decide",
                        methodInfo, eventual);
                propertyChanges.incrementAndGet();
                eventualCluster.commitAssumptionBuffer();
            } else {
                eventualCluster.discardAssumptionBuffer();
            }
        }
        for (MethodInfo methodInfo : typeInfo.methods()) {
            if (methodInfo.analysis().haveAnalyzedValueFor(EVENTUALLY_NON_MODIFYING_METHOD)) continue;
            eventualCluster.setDebugContext("enm " + methodInfo.fullyQualifiedName());
            eventualCluster.beginAssumptionBuffer();
            Set<String> labels = computeEventuallyNonModifying(typeInfo, methodInfo);
            if (siteDebug()) ecsite(labels != null && !labels.isEmpty()
                    ? "WRITE enm=" + new TreeSet<>(labels) : "no enm (" + labels + ")");
            if (labels != null && !labels.isEmpty()) {
                methodInfo.analysis().set(EVENTUALLY_NON_MODIFYING_METHOD,
                        new ValueImpl.SetOfStringsImpl(Set.copyOf(labels)));
                DECIDE.debug("TE: Decide eventually-non-modifying of method {} after {}", methodInfo, labels);
                propertyChanges.incrementAndGet();
                eventualCluster.commitAssumptionBuffer();
            } else {
                eventualCluster.discardAssumptionBuffer();
            }
        }
        // EVENTUALLY_UNMODIFIED_PARAMETER (spec: docs/spec-eventually-unmodified-parameter.md): the parameter
        // twin of the loop above -- the same commit walk, rooted in a parameter instead of this. Entirely under
        // the gate: nothing here runs, and nothing is written, without EVENTUALCLUSTER.
        if (EventualCluster.ENABLED) {
            for (MethodInfo methodInfo : typeInfo.methods()) {
                for (ParameterInfo pi : methodInfo.parameters()) {
                    if (pi.analysis().haveAnalyzedValueFor(EVENTUALLY_UNMODIFIED_PARAMETER)) continue;
                    eventualCluster.setDebugContext("eup " + pi.fullyQualifiedName());
                    eventualCluster.beginAssumptionBuffer();
                    Set<String> labels = computeEventuallyUnmodified(typeInfo, methodInfo, pi);
                    if (siteDebug()) ecsite(labels != null && !labels.isEmpty()
                            ? "WRITE eup=" + new TreeSet<>(labels) : "no eup (" + labels + ")");
                    if (labels != null && !labels.isEmpty()) {
                        pi.analysis().set(EVENTUALLY_UNMODIFIED_PARAMETER,
                                new ValueImpl.SetOfStringsImpl(Set.copyOf(labels)));
                        DECIDE.debug("TE: Decide eventually-unmodified of parameter {} after {}", pi, labels);
                        propertyChanges.incrementAndGet();
                        eventualCluster.commitAssumptionBuffer();
                    } else {
                        eventualCluster.discardAssumptionBuffer();
                    }
                }
            }
        }
        // EXPERIMENTAL (EVENTUALCLUSTER): once this type's method-level eventual intent is known, close it upward
        // so its supertypes join the cluster (InfoImpl and the interfaces have no eventual method of their own).
        eventualCluster.noteCandidate(typeInfo);
        eventualCluster.noteHierarchy(typeInfo);
        eventualCluster.setDebugContext("typeLevel " + typeInfo.fullyQualifiedName());
        eventualCluster.beginAssumptionBuffer();
        computeTypeLevel(typeInfo, activateCycleBreaking);
        if (typeInfo.analysis().haveAnalyzedValueFor(EVENTUALLY_IMMUTABLE_TYPE)) {
            eventualCluster.commitAssumptionBuffer();
        } else {
            eventualCluster.discardAssumptionBuffer();
        }
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
        // accessor reads through it. Off the gate, only fired alongside a mark already found: a type with no
        // transition of its own does not become eventual just by holding such a field. Under EVENTUALCLUSTER the
        // restriction lifts: a MARKLESS CARRIER (ParameterizedTypeImpl -- final fields of candidate types, no
        // own transition) is immutable as soon as its referents commit; the field names are its observable
        // labels, every optimistic field check is witnessed, and the "buys nothing" guard below still applies.
        if (!markLabels.isEmpty() || EventualCluster.ENABLED) {
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                if (fieldInfo.isStatic()) continue;
                if (!fieldInfo.analysis().getOrDefault(FINAL_FIELD, ValueImpl.BoolImpl.FALSE).isTrue()) continue;
                // the container ride-along extends the markless carrier: a final, owner-unmutated container
                // of committable content (ExpressionImpl.comments) rides along exactly like a field of
                // eventually immutable type -- once the elements commit, the stable wrapper acts immutable
                if (!excusedFields.contains(fieldInfo)
                    && (fieldHoldsCommittableContent(fieldInfo) || containerContentCommittable(fieldInfo))) {
                    markLabels.add(fieldInfo.name());
                    excusedFields.add(fieldInfo);
                }
            }
        }
        // EVENTUALCLUSTER, Part A -- subclass -> abstract superclass mark inheritance (the historical
        // approvedPreconditionsFromParent, in this direction): an abstract class with no mark of its own, all
        // of whose analyzed direct subclasses are eventually immutable with a SHARED mark label (every concrete
        // Info subclass marks its own 'inspection'), inherits that shared label. A subclass whose verdict is
        // still pending counts optimistically via the seed, witnessed like every other assumption -- the
        // contraction validates. The shared label is the intersection: subclasses add their own cross-reference
        // ride-alongs ('typeInfo', 'compilationUnitOrEnclosingType') on top of the common transition.
        if (markLabels.isEmpty() && EventualCluster.ENABLED && typeInfo.isAbstract() && !typeInfo.isInterface()) {
            Set<TypeInfo> subclasses = eventualCluster.knownSubclasses(typeInfo);
            if (!subclasses.isEmpty()) {
                Set<String> shared = null;
                boolean allEventual = true;
                for (TypeInfo subclass : subclasses) {
                    Value.EventuallyImmutable subVerdict = subclass.analysis().getOrDefault(
                            EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);
                    if (subVerdict.isEventual()) {
                        Set<String> subLabels = Set.of(subVerdict.markLabel().split(","));
                        if (shared == null) shared = new HashSet<>(subLabels);
                        else shared.retainAll(subLabels);
                    } else if (!eventualCluster.treatAsEventuallyImmutable(typeInfo, subclass, subVerdict)) {
                        allEventual = false;
                        break;
                    }
                }
                if (allEventual && shared != null && !shared.isEmpty()) {
                    markLabels.addAll(shared);
                    // no own field resolves these labels (they live on the subclasses); the abstract type's own
                    // modifying methods still need their enm labels, collected in the loop above
                }
            }
        }
        // EC_TYPE_DEBUG=<fqn substring>: print the type-level decision path (log-only, env-gated diagnostic
        // in the MODREACH_EXPLAIN style) -- why does a type with fully excused methods still get no verdict?
        boolean dbg = ecTypeDebug(typeInfo);
        if (markLabels.isEmpty()) {
            if (dbg) System.out.println("ECTYPE " + typeInfo.fullyQualifiedName() + " no mark labels");
            return;
        }

        TypeImmutableAnalyzer.AfterMark afterMark =
                new TypeImmutableAnalyzer.AfterMark(Set.copyOf(excusedFields), Set.copyOf(excusedMethods));
        Value.Immutable afterMarkLevel = typeImmutableAnalyzer.immutableAfterMark(typeInfo, afterMark,
                activateCycleBreaking);
        if (dbg) {
            System.out.println("ECTYPE " + typeInfo.fullyQualifiedName() + " markLabels=" + new TreeSet<>(markLabels)
                               + " excusedM=" + excusedMethods.size() + " excusedF=" + excusedFields.size()
                               + " afterMark=" + afterMarkLevel + " cycleBreaking=" + activateCycleBreaking);
        }
        if (afterMarkLevel == null) {
            UNDECIDED.debug("TE: Eventual immutability of type {} undecided", typeInfo);
            return;
        }
        Value.Immutable unconditional = typeInfo.analysis()
                .getOrDefault(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE);
        if (afterMarkLevel.compareTo(unconditional) <= 0) {
            if (dbg) System.out.println("ECTYPE " + typeInfo.fullyQualifiedName() + " buys nothing: afterMark="
                                        + afterMarkLevel + " <= unconditional=" + unconditional);
            return; // the mark buys nothing; do not claim eventuality the type does not need
        }
        // EVENTUALCLUSTER: a weak (final-fields) after-mark level computed BEFORE the terminal phase is
        // usually an artifact of enm labels still accumulating: the verdict is write-once, and
        // immutableSuper's isMutable(@FinalFields) check then hard-sinks every subtype -- the TypeInfo
        // interface froze @FinalFields(after="inspection") in iteration 1 with 2 of its eventual 33 labels
        // present, making TypeInfoImpl MUTABLE forever. Defer weak verdicts to the cycle-breaking
        // iterations, when the method layer has converged; a level still FINAL_FIELDS then is honest.
        if (EventualCluster.ENABLED && afterMarkLevel.isFinalFields() && !activateCycleBreaking) {
            if (dbg) System.out.println("ECTYPE " + typeInfo.fullyQualifiedName()
                                        + " weak after-mark level deferred to the terminal phase");
            return;
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
            if (fieldInfo != null
                && (fieldHoldsCommittableContent(fieldInfo) || containerContentCommittable(fieldInfo))) {
                excusedFields.add(fieldInfo);
            }
        }
    }

    /** A field type that excuses a call on it after the mark: really eventually immutable, or -- under the
     *  EVENTUALCLUSTER gate -- a cluster candidate whose verdict is still circular (see {@link EventualCluster}).
     *  {@code member} is the type holding the field, recorded as the assumer when the excusal is optimistic. */
    private boolean isEventuallyImmutableFieldType(TypeInfo member, TypeInfo fieldType) {
        boolean result = eventualCluster.treatAsEventuallyImmutable(member, fieldType, eventuallyImmutable(fieldType));
        if (result && siteDebug() && !eventuallyImmutable(fieldType).isEventual()) {
            ecsite("lean on " + fieldType.fullyQualifiedName());
        }
        return result;
    }

    /** null when nothing can be concluded; never {@code NOT_EVENTUAL} (we do not record absence). */
    private Value.Eventual computeEventual(TypeInfo typeInfo, MethodInfo methodInfo) {
        if (methodInfo.isStatic() || methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;

        Value.Eventual testMark = computeTestMark(typeInfo, methodInfo);
        if (testMark != null) return testMark;

        Set<String> marked = new HashSet<>();
        Set<String> onlyBefore = new HashSet<>();
        Set<String> onlyAfter = new HashSet<>();
        // EVENTUALCLUSTER (handoff-builder-leans §4A): the PRECONDITION shape. A body guarded by a
        // @TestMark observation -- 'assert inspection.isVariable();' or the if-throw variant -- belongs
        // to the side the guard asserts: calling it on the other side throws. This is what classifies
        // the builder() accessors @Only(before=inspection) instead of leaving them for the enm layer to
        // mislabel via doomed Builder-candidacy leans. computeTestMark above keeps pure @TestMark
        // forwards out of here; a guard elsewhere in the body is not a precondition and stays ignored.
        if (EventualCluster.ENABLED) {
            scanPreconditions(typeInfo, methodInfo, onlyBefore, onlyAfter);
        }
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
     * The leading guards of the body, read as preconditions: leading {@code assert <state test>} statements,
     * and the two if-throw shapes ({@code if (!<test>) throw …; <live>} and {@code if (<test>) { <live> }
     * throw …;}). Each contributes the guard's labels to the side the LIVE path requires.
     */
    private void scanPreconditions(TypeInfo typeInfo, MethodInfo methodInfo,
                                   Set<String> onlyBefore, Set<String> onlyAfter) {
        List<Statement> statements = methodInfo.methodBody().statements().stream()
                .filter(s -> !s.isSynthetic()).toList();
        for (Statement statement : statements) {
            if (statement instanceof AssertStatement as) {
                recordGuardSide(typeInfo, as.expression(), false, onlyBefore, onlyAfter);
            } else {
                break; // preconditions are the LEADING asserts only
            }
        }
        if (!statements.isEmpty() && statements.getFirst() instanceof IfElseStatement ifElse
            && ifElse.elseBlock().isEmpty()) {
            boolean thenThrows = ifElse.block().statements().size() == 1
                                 && ifElse.block().statements().getFirst() instanceof ThrowStatement;
            boolean tailThrows = statements.size() == 2 && statements.get(1) instanceof ThrowStatement;
            if (thenThrows) {
                // if (<guard>) throw …; <live>  -- the live path requires the guard FALSE
                recordGuardSide(typeInfo, ifElse.expression(), true, onlyBefore, onlyAfter);
            } else if (tailThrows) {
                // if (<guard>) { <live> } throw …;  -- the live path requires the guard TRUE
                recordGuardSide(typeInfo, ifElse.expression(), false, onlyBefore, onlyAfter);
            }
        }
    }

    /** Resolve a guard expression to a {@code @TestMark} observation on this type's state and record its
     *  labels on the side the (possibly negated) guard asserts. Anything else is silently ignored. */
    private void recordGuardSide(TypeInfo typeInfo, Expression expression, boolean flip,
                                 Set<String> onlyBefore, Set<String> onlyAfter) {
        Expression expr = expression;
        boolean negated = flip;
        while (expr instanceof Negation negation) {
            negated = !negated;
            expr = negation.expression();
        }
        if (!(expr instanceof MethodCall mc)) return;
        Value.Eventual callee = eventualOf(mc.methodInfo());
        if (!callee.isTestMark()) return;
        Set<String> labels = labelsOfReceiver(typeInfo, mc, callee);
        if (labels == null) return;
        // test() == TRUE: the observation returns true in the AFTER state (isSet); FALSE: in the BEFORE
        // state (isVariable). The live path holds the guard true (modulo negations).
        boolean trueMeansAfter = Boolean.TRUE.equals(callee.test()) ^ negated;
        if (trueMeansAfter) {
            onlyAfter.addAll(labels);
        } else {
            onlyBefore.addAll(labels);
        }
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
        WalkRoot walk = WalkRoot.ofThis(typeInfo, methodInfo);
        // EVENTUALCLUSTER: locals holding a 'this'-derived value (with committing labels; null = poisoned),
        // and locals holding provably fresh objects (every assignment a plain constructor call)
        LocalContext localContext = EventualCluster.ENABLED
                ? buildLocalContext(walk, methodInfo) : LocalContext.EMPTY;
        methodInfo.methodBody().visit(e -> {
            if (bail[0]) return false;
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr && fr.scopeIsThis()) {
                bail[0] = true; // assigning an own field is a finality/mark concern, not eventual non-modification
                return false;
            }
            if (e instanceof ConstructorCall cc && EventualCluster.ENABLED) {
                // a constructor call captures its root-derived arguments into the fresh object: each must be
                // committable (or a qualifying container handoff) -- the value-position discipline, applied
                // at the site. Nested calls are still visited and excused individually below.
                Set<String> excused = commitLabels(walk, cc, localContext);
                if (excused == null) {
                    bail[0] = true;
                    return false;
                }
                labels.addAll(excused);
                return true;
            }
            if (e instanceof MethodCall mc) {
                Value.Bool calleeNonMod = mc.methodInfo().analysis()
                        .getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                boolean calleeModifies = calleeNonMod == null || calleeNonMod.isFalse();
                if (EventualCluster.ENABLED) {
                    // the site-level modification view must not be more optimistic than the dispatch
                    // closure (the ∅-enm gap, 2026-07-23): a contract-non-modifying ABSTRACT callee
                    // (Comparable.compareTo, Object.equals) whose implementations honestly modify
                    // pre-mark (ExpressionImpl.compareTo forces lazy inspection) still demands the
                    // receiver's labels, exactly as if the honest implementation were called directly
                    Set<String> excused = commitExcusedLabels(walk, mc,
                            calleeModifies || implementationHonestlyModifies(mc.methodInfo()), localContext);
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
     * EVENTUALCLUSTER only. The labels after which a call leaves the ARGUMENT for {@code pi} unmodified
     * ({@code EVENTUALLY_UNMODIFIED_PARAMETER}, spec: {@code docs/spec-eventually-unmodified-parameter.md}):
     * the same commit walk as {@link #computeEventuallyNonModifying}, rooted in the parameter instead of
     * {@code this}. Labels are field names in the parameter type's label space. Only meaningful when the plain
     * verdict is an honest FALSE; the empty set is never written (it would coincide with plain
     * {@code unmodified=true}), and null means: modified in ways the machinery cannot excuse.
     */
    private Set<String> computeEventuallyUnmodified(TypeInfo typeInfo, MethodInfo methodInfo, ParameterInfo pi) {
        if (methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;
        Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
        if (unmodified == null || unmodified.isTrue()) {
            if (siteDebug()) ecsite("eup guard: unmodified=" + (unmodified == null ? "null" : "true"));
            return null;
        }
        // the varargs array (and any array) is never committable; a type parameter has no label space
        if (pi.parameterizedType().arrays() > 0) return null;
        TypeInfo pType = pi.parameterizedType().bestTypeInfo();
        if (pType == null) {
            if (siteDebug()) ecsite("eup guard: no pType");
            return null;
        }
        WalkRoot walk = WalkRoot.ofParameter(typeInfo, pType, pi);
        LocalContext localContext = buildLocalContext(walk, methodInfo);
        Set<String> labels = new HashSet<>();
        boolean[] bail = {false};
        methodInfo.methodBody().visit(e -> {
            if (bail[0]) return false;
            if (e instanceof Assignment a) {
                // rebinding the parameter breaks the rooting; a store through a p-rooted face (p.f = x,
                // p.f[i] = x) is a modification no label excuses; and storing a p-derived value into a field
                // of any object is capture the walk does not track -- all conservative bails
                boolean targetRooted = a.variableTarget() instanceof FieldReference fr
                                       && fr.scope() != null && referencesRootOrTracked(walk, fr.scope(), localContext.commit())
                                       || a.variableTarget() instanceof DependentVariable dv
                                          && dv.arrayExpression() != null
                                          && referencesRootOrTracked(walk, dv.arrayExpression(), localContext.commit());
                boolean capture = a.variableTarget() instanceof FieldReference
                                  && referencesRootOrTracked(walk, a.value(), localContext.commit());
                if (pi.equals(a.variableTarget()) || targetRooted || capture) {
                    bail[0] = true;
                    return false;
                }
            }
            if (e instanceof ConstructorCall cc) {
                // capture of p-derived arguments into a fresh object: value-position discipline at the site
                Set<String> excused = commitLabels(walk, cc, localContext);
                if (excused == null) {
                    bail[0] = true;
                    return false;
                }
                labels.addAll(excused);
                return true;
            }
            if (e instanceof MethodCall mc) {
                Value.Bool calleeNonMod = mc.methodInfo().analysis()
                        .getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                boolean calleeModifies = calleeNonMod == null || calleeNonMod.isFalse();
                Set<String> excused = commitExcusedLabels(walk, mc, calleeModifies, localContext);
                if (excused == null) {
                    bail[0] = true;
                    return false;
                }
                labels.addAll(excused);
            }
            return true;
        });
        return bail[0] ? null : labels;
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
    private Set<String> commitExcusedLabels(WalkRoot walk, MethodCall mc, boolean calleeModifies,
                                            LocalContext ctx) {
        // a @Mark or @Only(before) callee is the transition itself (setFinal, setVariable), not a post-mark
        // read; calling it after the mark throws -- UNLESS it acts on a provably fresh object's lifecycle
        // (fi.inspection.setVariable(...) in withOwnerVariableBuilder), OR on a receiver that is provably not
        // root-derived at all (handoff-builder-leans §4A: the rewire machinery filling ANOTHER object's
        // builder): another object's transition cannot modify the root; only the arguments carry root-derived
        // content, and they are committed below like any other call's
        Value.Eventual calleeEventual = eventualOf(mc.methodInfo());
        if ((calleeEventual.isMark() || calleeEventual.isOnly() && Boolean.FALSE.equals(calleeEventual.after()))
            && !rootedInFresh(mc.object(), ctx)
            && !receiverProvablyNotRoot(walk, mc.object(), ctx)) {
            return null;
        }
        Set<String> labels = new HashSet<>();
        if (calleeModifies) {
            // only a modifying callee can modify its receiver
            if (mc.object() instanceof VariableExpression ve && walk.isRoot(ve.variable())) {
                if (EventualCluster.ENABLED && walk.isSelfCall(mc)) {
                    // direct recursion (CommonType.commonType's lattice descent): the callee's excuse set IS
                    // the set under computation (write-once, still in flight) -- by the fixpoint hypothesis
                    // it contributes nothing new; only the arguments still need committing
                    if (siteDebug()) ecsite("self-call excused: " + mc.methodInfo().name());
                } else {
                    // root.<accessor>() forward: the accessor itself must be eventually-non-modifying
                    Set<String> enm = mc.methodInfo().analysis()
                            .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET).set();
                    if (enm.isEmpty()) return null;
                    labels.addAll(enm);
                }
            } else {
                // top level: only the receiver needs committing; the call's own return value feeds nothing here
                Set<String> receiver = commitLabels(walk, mc.object(), ctx);
                if (receiver == null) return null;
                labels.addAll(receiver);
            }
        }
        // a call may modify a root-derived argument (a @Modified parameter); commit each
        return commitArguments(walk, mc, ctx, 0, labels, false);
    }

    /**
     * The root of a commit walk: {@code this} ({@code root == null} -- the receiver walk of
     * {@link #computeEventuallyNonModifying}) or a specific parameter (the eup walk of
     * {@link #computeEventuallyUnmodified}). {@code labelType} spans the label space -- the analyzed type
     * resp. the parameter's declared type; its (implementations') field names are the labels. {@code member}
     * is always the ANALYZED type: the assumer recorded on witnessed cluster assumptions, so the contraction
     * can cascade a broken candidate to whoever consumed this walk's conclusion (label provenance folds a
     * consumer onto {@code member}). For the receiver walk the three coincide with the historical
     * {@code owner} parameter, and the behavior is unchanged.
     */
    private record WalkRoot(TypeInfo member, TypeInfo labelType, Variable root, MethodInfo underAnalysis) {
        static WalkRoot ofThis(TypeInfo typeInfo, MethodInfo underAnalysis) {
            return new WalkRoot(typeInfo, typeInfo, null, underAnalysis);
        }

        static WalkRoot ofParameter(TypeInfo analyzed, TypeInfo parameterType, ParameterInfo pi) {
            return new WalkRoot(analyzed, parameterType, pi, pi.methodInfo());
        }

        /** the root variable itself: bare {@code this}, resp. the bare parameter */
        boolean isRoot(Variable v) {
            return root == null ? v instanceof This : root.equals(v);
        }

        /** a field read directly off the root: {@code this.f}, resp. {@code p.f} */
        boolean scopeIsRoot(FieldReference fr) {
            if (root == null) return fr.scopeIsThis();
            return fr.scope() instanceof VariableExpression ve && root.equals(ve.variable());
        }

        /** a variable that makes an expression root-derived (mirrors the historical referencesThis shapes) */
        boolean matches(Variable v) {
            return isRoot(v) || v instanceof FieldReference fr && scopeIsRoot(fr);
        }

        /** the same walk, re-rooted at {@code this} of {@code type}: inlining an accessor body evaluates the
         *  callee's {@code this}, which IS the root object */
        WalkRoot inlineInto(TypeInfo type) {
            return new WalkRoot(member, type, null, underAnalysis);
        }

        /** direct recursion: the call site's callee is the very method whose labels are being computed */
        boolean isSelfCall(MethodCall mc) {
            return underAnalysis != null && mc.methodInfo() == underAnalysis;
        }
    }

    /** Per-method context for {@link #commitLabels}: locals holding root-derived values (with the labels
     *  committing them; a null value = poisoned), locals holding provably FRESH objects, and locals that
     *  ALIAS a root container field (the {@code list.isEmpty() ? list : rebuilt} short-circuit): their VALUE
     *  stays uncommittable exactly like the bare field's, but the per-site container rescues may judge them
     *  as if the field were spelled inline. */
    private record LocalContext(Map<Variable, Set<String>> commit, Set<Variable> fresh,
                                Map<Variable, FieldInfo> containerAlias) {
        static final LocalContext EMPTY = new LocalContext(Map.of(), Set.of(), Map.of());
    }

    /**
     * Is the object a call acts on provably not the root's? Unwraps a fluent chain
     * ({@code copy.builder().setX(…).commit()}) to its base; true when the base is free of any root-derived
     * content, or is a tracked local the walk itself judged committed-after-∅ (the rewire machinery's
     * {@code infoMap.methodInfo(this)} copies). The bare root, root-scoped fields, and non-∅-tracked locals
     * keep their bails: a transition on the root's own committed content stays {@code @Only(before)}
     * business, not an enm excuse.
     * <p>
     * Two consumers: the transition-callee bail (a {@code @Mark}/{@code @Only(before)} callee on another
     * object's lifecycle only needs its arguments committed), and the handed-on gauntlet (the value a chain
     * on another object's graph hands on cannot reach the root except through the argument-fed content
     * already committed by the accumulated labels -- so no candidacy lean on the return type is needed).
     */
    private boolean receiverProvablyNotRoot(WalkRoot walk, Expression receiver, LocalContext ctx) {
        Expression base = receiver;
        while (true) {
            if (base instanceof MethodCall mc) {
                base = mc.object();
            } else if (base instanceof Cast cast) {
                base = cast.expression();
            } else if (base instanceof EnclosedExpression enclosed) {
                base = enclosed.inner();
            } else {
                break;
            }
        }
        if (base == null) return false;
        if (!referencesRootOrTracked(walk, base, ctx.commit())) return true;
        if (base instanceof VariableExpression ve && !walk.isRoot(ve.variable())) {
            Set<String> tracked = ctx.commit().get(ve.variable());
            return tracked != null && tracked.isEmpty();
        }
        return false;
    }

    /** Is the expression's value rooted in an object constructed in this very method body (a plain constructor
     *  call, or a chain of reads off a local every assignment of which is one)? A transition call on such a
     *  receiver belongs to the fresh object's lifecycle, not to this's transition. */
    private static boolean rootedInFresh(Expression expr, LocalContext ctx) {
        return rootedInFresh(expr, ctx.fresh());
    }

    private static boolean rootedInFresh(Expression expr, Set<Variable> fresh) {
        if (expr instanceof ConstructorCall cc) return cc.anonymousClass() == null;
        if (expr instanceof VariableExpression ve) {
            if (fresh.contains(ve.variable())) return true;
            return ve.variable() instanceof FieldReference fr && !fr.scopeIsThis()
                   && rootedInFresh(fr.scope(), fresh);
        }
        if (expr instanceof MethodCall mc) return mc.object() != null && rootedInFresh(mc.object(), fresh);
        if (expr instanceof Cast cast) return rootedInFresh(cast.expression(), fresh);
        if (expr instanceof EnclosedExpression enclosed) return rootedInFresh(enclosed.inner(), fresh);
        if (expr instanceof InlineConditional inline) {
            return rootedInFresh(inline.ifTrue(), fresh) && rootedInFresh(inline.ifFalse(), fresh);
        }
        return false;
    }

    /**
     * EVENTUALCLUSTER only. The mark labels after which {@code expr}'s value can no longer be used to modify
     * {@code owner}'s ({@code this}'s) accessible state: the empty set when the value is not derived from
     * {@code this} at all (a parameter, a fresh object, a constant, a static reference); a non-empty set when it
     * is {@code this}-derived but committed (immutable) once those marks have passed; null when it is
     * {@code this}-derived and cannot be shown committable -- bail. Bare {@code this} is never committable from
     * the inside: mid-transition, it still has its mutable half.
     */
    private Set<String> commitLabels(WalkRoot walk, Expression expr, LocalContext ctx) {
        return commitLabels(walk, expr, ctx, 0);
    }

    // how many same-class single-return forwards commitLabels may look through (guards against recursion)
    private static final int MAX_LOOK_THROUGH = 3;

    private Set<String> commitLabels(WalkRoot walk, Expression expr, LocalContext ctx, int depth) {
        if (expr == null || !referencesRootOrTracked(walk, expr, ctx.commit())) return Set.of();
        // a value whose TYPE cannot carry mutable state (an int, a String, a parameterless immutable-hc
        // MethodType) is harmless whatever produced it -- the producing calls are excused independently by
        // the enclosing visitor, which sees every MethodCall node
        if (expr.parameterizedType() != null && valueIsHarmless(expr.parameterizedType())) return Set.of();
        if (expr instanceof VariableExpression ve) {
            if (ve.variable() instanceof FieldReference fr && walk.scopeIsRoot(fr)) {
                FieldInfo fieldInfo = fr.fieldInfo();
                // own field, or one inherited from a superclass (UnaryOperatorImpl.operator read inside
                // BitwiseNegationImpl.rewire): the label then names the super's field, which the type level
                // tolerates exactly like an inherited mark -- it excuses no own field of the subclass
                if (isOwnOrInheritedField(walk.labelType(), fieldInfo) && fieldHoldsCommittableContent(fieldInfo)) {
                    return Set.of(fieldInfo.name());
                }
                // an @IgnoreModifications store is manual hidden content (road §050): modifications through
                // it are disclaimed by design, so its value cannot count as modifying the root
                if (fieldInfo.analysis().getOrDefault(PropertyImpl.IGNORE_MODIFICATIONS_FIELD,
                        ValueImpl.BoolImpl.FALSE).isTrue()) {
                    return Set.of();
                }
                // a deeply immutable value (String, a primitive) offers nothing to modify through
                if (valueIsHarmless(fieldInfo.type())) return Set.of();
                if (siteDebug()) ecsite("field bail: " + fieldInfo.name() + " (" + fieldInfo.type() + ")");
                return null; // a non-committable field of the root
            }
            if (ve.variable() instanceof FieldReference fr && !walk.scopeIsRoot(fr)) {
                // a field read off another object (fi.inspection): the result is root-derived only insofar as
                // root-derived content flowed into that object -- committed after the scope's labels
                if (fr.fieldInfo().isStatic()) return null; // static state: not ours to excuse
                return commitLabels(walk, fr.scope(), ctx, depth);
            }
            if (ctx.commit().containsKey(ve.variable())) return ctx.commit().get(ve.variable()); // may be null
            if (walk.isRoot(ve.variable())
                && eventualCluster.treatAsEventuallyImmutable(walk.member(), walk.labelType(),
                    eventuallyImmutable(walk.labelType()))) {
                // bare root handed out (Stream.of(this) in innerClassEnclosingStream): for a cluster candidate,
                // the root is committed once its own marks have passed -- before them the enclosing method is
                // modifying anyway, and the candidate's own verdict (validated by the contraction) carries the
                // promise that post-mark aliases cannot modify it
                return Set.of();
            }
            return null; // bare root (mid-transition, no candidate), or another variable shape
        }
        if (expr instanceof Cast cast) return commitLabels(walk, cast.expression(), ctx, depth);
        if (expr instanceof EnclosedExpression enclosed) return commitLabels(walk, enclosed.inner(), ctx, depth);
        if (expr instanceof InlineConditional inline) {
            // the value is one of the two branches; the condition's calls are excused by the outer visitor,
            // but its VALUE cannot leak (a boolean)
            Set<String> ifTrue = commitLabels(walk, inline.ifTrue(), ctx, depth);
            if (ifTrue == null) return null;
            Set<String> ifFalse = commitLabels(walk, inline.ifFalse(), ctx, depth);
            if (ifFalse == null) return null;
            Set<String> acc = new HashSet<>(ifTrue);
            acc.addAll(ifFalse);
            return acc;
        }
        if (expr instanceof ConstructorCall cc) {
            // a fresh object; a call on it later cannot modify the root except through the root-derived values
            // stored into it here, each committed after its own labels
            if (cc.anonymousClass() != null) return null; // may capture this arbitrarily: conservative
            Set<String> acc = new HashSet<>();
            List<Expression> ccArgs = cc.parameterExpressions();
            for (int i = 0; i < ccArgs.size(); i++) {
                Expression arg = ccArgs.get(i);
                Set<String> argLabels = commitLabels(walk, arg, ctx, depth);
                if (argLabels == null && cc.constructor() != null) {
                    // the container ride-along, argument position (the fluent-copy constructor)
                    argLabels = containerArgumentLabels(walk, cc.constructor(), i, arg, ctx);
                }
                if (argLabels == null) {
                    if (siteDebug()) ecsite("CC arg " + i + " bail at "
                            + (cc.constructor() == null ? "?" : cc.constructor().fullyQualifiedName()));
                    return null;
                }
                acc.addAll(argLabels);
            }
            return acc;
        }
        if (expr instanceof Lambda lambda) {
            return lambdaCommitLabels(walk, lambda, ctx, depth);
        }
        if (expr instanceof MethodReference mr) {
            return methodReferenceCommitLabels(walk, mr, ctx, depth);
        }
        if (expr instanceof MethodCall mc) {
            // an intermediate @Mark or @Only(before) call belongs to the transition; after the mark it throws
            // -- unless it acts on a provably fresh object's lifecycle, or on a receiver that is provably not
            // root-derived (another object's transition; only the arguments matter here)
            Value.Eventual calleeEventual = eventualOf(mc.methodInfo());
            if (siteDebug()) ecsite("MC " + mc.methodInfo().fullyQualifiedName()
                    + " calleeEventual=" + calleeEventual);
            if ((calleeEventual.isMark() || calleeEventual.isOnly() && Boolean.FALSE.equals(calleeEventual.after()))
                && !rootedInFresh(mc.object(), ctx)
                && !receiverProvablyNotRoot(walk, mc.object(), ctx)) {
                if (siteDebug()) ecsite("transition bail: " + mc.methodInfo().fullyQualifiedName());
                return null;
            }
            Set<String> acc = new HashSet<>();
            // is the receiver a root-derived value that is COMMITTED once acc's labels have passed? (the bare
            // root never is: mid-transition it still has its mutable half)
            boolean receiverCommitted;
            if (mc.object() instanceof VariableExpression ve && walk.isRoot(ve.variable())) {
                // root.<accessor>(): the method boundary erases WHICH committed field the result was read
                // through, which handedOnValueSafe often needs -- so look through a same-class single-return
                // forward first, evaluating its body expression in place (one level of inlining). Inside the
                // inlined body, 'this' IS the root object, so the evaluation is re-rooted at this. The
                // inlining is keyed on the DECLARING type's label space: a CONCRETE inherited accessor
                // (StatementImpl.comments() called inside DoStatementImpl.rewire) inlines against the
                // superclass's fields -- an unresolved superclass label simply excuses no own field at the
                // subclass's type level, exactly like an inherited mark.
                if (depth < MAX_LOOK_THROUGH && !mc.methodInfo().isAbstract()) {
                    Expression returned = singleReturnExpression(mc.methodInfo());
                    if (returned != null) {
                        Set<String> through = commitLabels(walk.inlineInto(mc.methodInfo().typeInfo()), returned,
                                LocalContext.EMPTY, depth + 1);
                        if (through != null) {
                            acc.addAll(through);
                            return commitArguments(walk, mc, ctx, depth, acc, true);
                        }
                    }
                }
                // an ABSTRACT accessor (an interface-typed root: the eup walk's declared parameter type, or an
                // inherited accessor) cannot be inlined directly -- bridge through its implementations: every
                // implementation must inline, and the union of their labels is the answer (dynamic dispatch
                // runs one of them; committing the union commits each). Labels are implementation field names,
                // the same convention the enm batch uses on interface methods.
                Set<String> bridged = abstractAccessorLabels(walk, mc.methodInfo(), depth);
                if (bridged != null) {
                    acc.addAll(bridged);
                    return commitArguments(walk, mc, ctx, depth, acc, true);
                }
                // a genuinely non-modifying accessor contributes nothing; a modifying one must be
                // eventually-non-modifying, and contributes its labels. A direct SELF-call is exempt from
                // the empty-enm bail: its excuse set is the one under computation (write-once, in flight)
                // -- the fixpoint hypothesis -- and the result still runs the handed-on gauntlet below.
                if (!isNonModifyingRead(mc.methodInfo())) {
                    if (EventualCluster.ENABLED && walk.isSelfCall(mc)) {
                        if (siteDebug()) ecsite("self-call excused (value): " + mc.methodInfo().name());
                    } else {
                        Set<String> enm = mc.methodInfo().analysis()
                                .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                                .set();
                        if (enm.isEmpty()) return null;
                        acc.addAll(enm);
                    }
                }
                receiverCommitted = false;
            } else {
                Set<String> receiver = commitLabels(walk, mc.object(), ctx, depth);
                if (receiver == null) {
                    // the container ride-along: a qualifying READ on a final container field of committable
                    // content commits after the field's label, even though the bare wrapper value does not
                    receiver = containerReadThroughLabels(walk, mc, ctx);
                    if (receiver == null) {
                        if (siteDebug()) ecsite("receiver bail (" + mc.methodInfo().name() + ")");
                        return null;
                    }
                }
                if (siteDebug()) ecsite("receiver(" + mc.methodInfo().name() + ")=" + receiver);
                if (receiver.isEmpty()) {
                    // the receiver is not root-derived; neither is anything obtained from it -- only the
                    // arguments still need committing
                    return commitArguments(walk, mc, ctx, depth, acc, true);
                }
                acc.addAll(receiver);
                receiverCommitted = true;
            }
            // the aliasing trap (handoff §6, caveat 1): the value an INTERMEDIATE call hands on must not be
            // usable to modify root-derived state downstream. A getter of an @IgnoreModifications store is
            // exempt: modifications through that value are disclaimed by design (road §050). And when the
            // root's type itself is a cluster candidate (seed, witnessed), a root-accessor's result is
            // accessible content of the root, committed once the root's own marks have passed -- the trap
            // shape (an accessor handing out an unmarked mutable field) then sinks the candidate's own type
            // verdict, and the contraction cascades the retraction to everything that leaned on it.
            boolean ownerSeedCoversResult = !receiverCommitted
                    && eventualCluster.treatAsEventuallyImmutable(walk.member(), walk.labelType(),
                    eventuallyImmutable(walk.labelType()));
            // a chain rooted in a fresh object (the fluent newField.builder().setX(..).setY(..)) hands on
            // fresh-owned state; the root-derived values stored into it are committed by the labels in acc
            boolean freshRooted = rootedInFresh(mc.object(), ctx);
            // EVENTUALCLUSTER (handoff-builder-leans §4b resolution): a chain whose BASE is provably not
            // root-derived (the rewired copy's ∅-tracked builder local, the infoMap parameter) hands on a
            // value of another object's graph. The root-derived content that flowed in via the arguments is
            // already committed by the labels in acc -- the §060 ride-along stance -- so the handed-on
            // judgment is settled without consulting the return type's candidacy: this is what kept the
            // doomed Builder leans alive (a MODIFYING fluent setter never reaches handedOnValueSafe's
            // independence branch and fell through to returnTypeHoldsCommittableContent). Checked BEFORE
            // handedOnValueSafe so no optimistic edge is witnessed on the way to a verdict we can reach
            // soundly.
            boolean notRootReceiver = EventualCluster.ENABLED && receiverProvablyNotRoot(walk, mc.object(), ctx);
            if (siteDebug()) ecsite("gauntlet " + mc.methodInfo().name()
                    + " receiverCommitted=" + receiverCommitted + " ownerSeed=" + ownerSeedCoversResult
                    + " freshRooted=" + freshRooted + " notRootReceiver=" + notRootReceiver);
            if (!isIgnoreModificationsAccessor(mc.methodInfo()) && !ownerSeedCoversResult && !freshRooted
                && !notRootReceiver
                && !handedOnValueSafe(walk, mc, receiverCommitted)) {
                if (siteDebug()) ecsite("handedOnValueSafe bail: " + mc.methodInfo().name());
                return null;
            }
            return commitArguments(walk, mc, ctx, depth, acc, true);
        }
        return null; // any other root-referencing expression shape: conservative
    }

    /**
     * The look-through for an abstract accessor called on the bare root: inline each implementation's
     * single-return body, re-rooted at the implementation's {@code this} (which is the root object under
     * dynamic dispatch), and union the labels. Null when any implementation cannot be inlined or excused --
     * the caller falls back to the generic accessor treatment.
     */
    private Set<String> abstractAccessorLabels(WalkRoot walk, MethodInfo accessor, int depth) {
        if (!accessor.isAbstract() || depth >= MAX_LOOK_THROUGH) return null;
        Value.SetOfMethodInfo implementations = accessor.analysis()
                .getOrDefault(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY);
        if (implementations.isEmpty()) return null;
        Set<String> acc = new HashSet<>();
        for (MethodInfo implementation : implementations.methodInfoSet()) {
            Expression returned = singleReturnExpression(implementation);
            if (returned == null) return null;
            Set<String> labels = commitLabels(walk.inlineInto(implementation.typeInfo()), returned,
                    LocalContext.EMPTY, depth + 1);
            if (labels == null) return null;
            acc.addAll(labels);
        }
        return acc;
    }

    /** A getter handing out an {@code @IgnoreModifications} store: the value is manual hidden content, and
     *  whatever is done through it is disclaimed (road §050). */
    private boolean isIgnoreModificationsAccessor(MethodInfo methodInfo) {
        Value.FieldValue fieldValue = methodInfo.getSetField();
        return fieldValue != null && fieldValue.field() != null && !fieldValue.setter()
               && fieldValue.field().analysis()
                       .getOrDefault(PropertyImpl.IGNORE_MODIFICATIONS_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    /** A value of this type cannot be used to modify anything: a primitive, a deeply immutable type (String),
     *  or a parameterless immutable-hc type (MethodType) -- all its methods are non-modifying, and there is
     *  nothing parameterised to launder mutable content through. */
    private boolean valueIsHarmless(ParameterizedType type) {
        if (type.arrays() > 0) return false;
        if (type.isPrimitiveExcludingVoid() || type.isVoid()) return true;
        TypeInfo bestType = type.bestTypeInfo();
        if (bestType == null) return false;
        Value.Immutable immutable = immutableOf(bestType);
        if (immutable.isImmutable()) return true;
        return immutable.isAtLeastImmutableHC() && type.parameters().isEmpty();
    }

    /**
     * A lambda handed to a callee: the callee may invoke it at will, so every this-derived value its body can
     * touch or return must be committed. Calls and constructor calls inside the body are evaluated with the
     * full {@link #commitLabels} discipline (including the handed-on-value checks -- the lambda's return value
     * flows to the callee); a bare captured {@code this} bails.
     */
    private Set<String> lambdaCommitLabels(WalkRoot walk, Lambda lambda, LocalContext ctx, int depth) {
        Set<String> acc = new HashSet<>();
        boolean[] bail = {false};
        lambda.methodBody().visit(e -> {
            if (bail[0]) return false;
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                && walk.scopeIsRoot(fr)) {
                bail[0] = true; // assigning a root field inside the lambda
                return false;
            }
            if (e instanceof MethodCall || e instanceof ConstructorCall || e instanceof VariableExpression) {
                Expression inner = (Expression) e;
                if (referencesRootOrTracked(walk, inner, ctx.commit())) {
                    Set<String> labels = commitLabels(walk, inner, ctx, depth + 1);
                    if (labels == null) {
                        bail[0] = true;
                        return false;
                    }
                    acc.addAll(labels);
                }
                return false; // commitLabels covered the subtree
            }
            return true;
        });
        return bail[0] ? null : acc;
    }

    /**
     * A bound method reference handed to a callee ({@code newField.builder()::addFieldModifier}): the callee
     * will invoke the referenced method on the bound scope. Unbound/static references without this-derived
     * content never reach here (the fast path returns ∅).
     */
    private Set<String> methodReferenceCommitLabels(WalkRoot walk, MethodReference mr, LocalContext ctx,
                                                    int depth) {
        Value.Eventual mrEventual = eventualOf(mr.methodInfo());
        boolean transition = mrEventual.isMark() || mrEventual.isOnly() && Boolean.FALSE.equals(mrEventual.after());
        if (mr.scope() instanceof VariableExpression ve && walk.isRoot(ve.variable())) {
            if (transition) return null;
            Set<String> acc = new HashSet<>();
            if (!isNonModifyingRead(mr.methodInfo())) {
                Set<String> enm = mr.methodInfo().analysis()
                        .getOrDefault(EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET).set();
                if (enm.isEmpty()) return null;
                acc.addAll(enm);
            }
            // the result flows to the callee: same discipline as an intermediate root-accessor
            if (!returnTypeHoldsCommittableContent(walk.member(), mr.methodInfo().returnType())) return null;
            return acc;
        }
        Set<String> scopeLabels = commitLabels(walk, mr.scope(), ctx, depth);
        if (scopeLabels == null) return null;
        if (rootedInFresh(mr.scope(), ctx)) return scopeLabels; // acts on the fresh object's lifecycle
        if (transition) return null;
        if (scopeLabels.isEmpty()) return scopeLabels; // not root-derived at all
        // committed receiver: mirror handedOnValueSafe on the declared return type
        if (isNonModifyingRead(mr.methodInfo())) {
            Value.Independent independent = independentOf(mr.methodInfo());
            if (independent != null && (independent.isIndependent() || independent.isDependent()
                                        || typeParametersHoldCommittableContent(walk.member(), mr.methodInfo().returnType()))) {
                return scopeLabels;
            }
        }
        if (returnTypeHoldsCommittableContent(walk.member(), mr.methodInfo().returnType())) return scopeLabels;
        return null;
    }

    /** The expression of a body that is nothing but {@code return <expr>;}, else null. */
    private static Expression singleReturnExpression(MethodInfo methodInfo) {
        if (methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return null;
        List<Statement> statements = methodInfo.methodBody().statements().stream()
                .filter(s -> !s.isSynthetic()).toList();
        if (statements.size() != 1 || !(statements.getFirst() instanceof ReturnStatement rs)) return null;
        return rs.expression();
    }

    /**
     * Commit each argument of the call, folding the labels into {@code acc}; null = bail. In EXCUSE position
     * ({@code valuePosition == false}: the statement-level call the visitor excuses) this is also where
     * {@code EVENTUALLY_UNMODIFIED_PARAMETER} is CONSUMED (spec §4): the bare root handed to a parameter that
     * promises "unmodified once L has been committed on the argument" contributes L to the caller's label set,
     * provided every label in L is committable on the root's own type -- that is what turns the otherwise
     * label-less candidate excusal of {@code ParameterizedTypePrinter.print(…, this, …)} into a named,
     * type-level-usable promise. The callee's own excusals may lean on cluster assumptions, so the consumer
     * inherits the callee owner's assumption edges (label provenance), and the contraction cascades for free.
     * <p>
     * In VALUE position (the result feeds a chain, a tracked local, an outer argument) the eup labels are
     * deliberately NOT folded in: they excuse THE CALL, they say nothing about when the RESULT's value
     * commits. Folding them made {@code infoMap.methodInfo(this)} locals look root-derived-committed, which
     * routed the rewire builder() chains into committed-receiver semantics and the doomed Builder-candidacy
     * handed-on fallback (handoff-builder-leans §4b).
     */
    private Set<String> commitArguments(WalkRoot walk, MethodCall mc, LocalContext ctx, int depth,
                                        Set<String> acc, boolean valuePosition) {
        List<Expression> args = mc.parameterExpressions();
        for (int i = 0; i < args.size(); i++) {
            Expression arg = args.get(i);
            Set<String> argLabels = commitLabels(walk, arg, ctx, depth);
            if (argLabels == null) {
                // the container ride-along, argument position: the bare wrapper handed to a callee that
                // provably neither mutates nor accessibly retains it
                argLabels = containerArgumentLabels(walk, mc.methodInfo(), i, arg, ctx);
                if (argLabels == null) {
                    if (siteDebug()) ecsite("MC arg " + i + " bail at " + mc.methodInfo().fullyQualifiedName());
                    return null;
                }
            }
            acc.addAll(argLabels);
            if (!valuePosition && arg instanceof VariableExpression ve && walk.isRoot(ve.variable())) {
                Set<String> eup = calleeParameterAfterLabels(mc.methodInfo(), i);
                // a p-walk handing its own root to a parameter of the SAME declared type (the 5-arg -> 6-arg
                // printer overload forward) stays in one label space: pass the labels through unchecked, the
                // committability check is the ultimate this-walk consumer's job (an interface label type has
                // no fields to check against). An ANCESTOR interface spans the same promise space, one level
                // wider (GreaterThanZero -> Expression: compareBinaryToGt0 forwarding into compareVariables)
                // -- the downward-interface-closure argument, mirrored; interface roots only, a class root
                // must pass committability (or the dispatch narrowing below).
                boolean sameLabelSpace = walk.root() != null && !eup.isEmpty()
                        && (calleeParameterType(mc.methodInfo(), i) == walk.labelType()
                            || EventualCluster.ENABLED && walk.labelType().isInterface()
                               && isAncestorType(calleeParameterType(mc.methodInfo(), i), walk.labelType()));
                if (!eup.isEmpty() && (sameLabelSpace || labelsCommittableOnRoot(walk, eup))) {
                    acc.addAll(eup);
                    eventualCluster.noteLabelInheritance(walk.member(), mc.methodInfo().typeInfo());
                } else if (!eup.isEmpty() && EventualCluster.ENABLED) {
                    // dispatch narrowing (spec §7): the abstract-union labels are per-implementation excuses;
                    // a label naming no field anywhere in the argument's runtime cone (the root class + its
                    // known subclasses, fields own or inherited) is vacuous for THIS argument -- the code
                    // paths it excuses cannot execute on it. Fold the committable restriction only when the
                    // entire residue is vacuous; anything less falls through unchanged.
                    Set<String> narrowed = new HashSet<>();
                    Set<String> residue = new HashSet<>();
                    for (String label : eup) {
                        if (labelsCommittableOnRoot(walk, Set.of(label))) narrowed.add(label);
                        else residue.add(label);
                    }
                    if (!narrowed.isEmpty() && residueVacuousOnCone(walk.labelType(), residue)) {
                        if (siteDebug()) ecsite("MC arg " + i + " eup narrowed=" + new TreeSet<>(narrowed)
                                                + " at " + mc.methodInfo().fullyQualifiedName());
                        acc.addAll(narrowed);
                        eventualCluster.noteLabelInheritance(walk.member(), mc.methodInfo().typeInfo());
                    } else if (siteDebug()) {
                        ecsite("MC arg " + i + " eup not narrowable: narrowed=" + new TreeSet<>(narrowed)
                               + " residue live at " + mc.methodInfo().fullyQualifiedName());
                    }
                }
            }
        }
        return acc;
    }

    /** The {@code EVENTUALLY_UNMODIFIED_PARAMETER} labels of the callee's parameter receiving argument
     *  {@code i}; empty when absent, or for the varargs position (the array is never committable). */
    private static Set<String> calleeParameterAfterLabels(MethodInfo callee, int i) {
        List<ParameterInfo> parameters = callee.parameters();
        if (parameters.isEmpty()) return Set.of();
        ParameterInfo pi = parameters.get(Math.min(i, parameters.size() - 1));
        if (pi.isVarArgs() && i >= parameters.size() - 1) return Set.of();
        return pi.analysis().getOrDefault(EVENTUALLY_UNMODIFIED_PARAMETER, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set();
    }

    /** The declared type of the callee's parameter receiving argument {@code i}; null when unresolved. */
    private static TypeInfo calleeParameterType(MethodInfo callee, int i) {
        List<ParameterInfo> parameters = callee.parameters();
        if (parameters.isEmpty()) return null;
        ParameterInfo pi = parameters.get(Math.min(i, parameters.size() - 1));
        return pi.parameterizedType().bestTypeInfo();
    }

    /** Every label names an own field of the root's type holding committable content -- the receiver-labels
     *  rule: committing those fields on the argument commits, by the §060 ride-along, the content the callee
     *  reads through them. A container-ride-along label qualifies too: what callees read through the stable
     *  wrapper is its (committable) element content. */
    private boolean labelsCommittableOnRoot(WalkRoot walk, Set<String> labels) {
        for (String label : labels) {
            FieldInfo fieldInfo = walk.labelType().getFieldByName(label, false);
            if (fieldInfo == null
                || !(fieldHoldsCommittableContent(fieldInfo) || containerContentCommittable(fieldInfo))) {
                return false;
            }
        }
        return true;
    }

    /** {@code ancestor} appears in {@code type}'s transitive parent/interface closure. */
    private static boolean isAncestorType(TypeInfo ancestor, TypeInfo type) {
        if (ancestor == null || type == null) return false;
        for (ParameterizedType superType : type.parentAndInterfacesImplemented()) {
            TypeInfo st = superType.typeInfo();
            if (st == null || st.isJavaLangObject()) continue;
            if (st == ancestor || isAncestorType(ancestor, st)) return true;
        }
        return false;
    }

    /** No residue label names a field -- own or inherited -- anywhere in {@code rootType}'s runtime cone
     *  ({@code rootType} plus its transitively known subclasses): such labels belong to OTHER
     *  implementations' excuses, and the code paths they excuse cannot execute on an argument of this cone. */
    private boolean residueVacuousOnCone(TypeInfo rootType, Set<String> residue) {
        if (residue.isEmpty()) return true;
        java.util.ArrayDeque<TypeInfo> todo = new java.util.ArrayDeque<>();
        Set<TypeInfo> seen = new HashSet<>();
        todo.add(rootType);
        while (!todo.isEmpty()) {
            TypeInfo t = todo.poll();
            if (!seen.add(t)) continue;
            for (String label : residue) {
                if (fieldInTypeOrAncestors(t, label)) return false;
            }
            todo.addAll(eventualCluster.knownSubclasses(t));
        }
        return true;
    }

    /** The named field exists on {@code type} or along its superclass chain. */
    private static boolean fieldInTypeOrAncestors(TypeInfo type, String name) {
        TypeInfo t = type;
        while (t != null) {
            if (t.getFieldByName(name, false) != null) return true;
            ParameterizedType parent = t.parentClass();
            t = parent == null ? null : parent.typeInfo();
        }
        return false;
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
    private boolean handedOnValueSafe(WalkRoot walk, MethodCall mc, boolean receiverCommitted) {
        // on a COMMITTED receiver the result's wrapper layer is either fresh or immutable content of the
        // receiver's graph, whatever the callee's (possibly still undecided -- direct recursion!) independence;
        // the only content it can hand on is typed by the parameters. This is what lets
        // parent.recursiveSuperTypeStream() (Stream<TypeInfo>) and Stream.map chains (Stream<String>) through.
        if (receiverCommitted && typeParametersHoldCommittableContent(walk.member(), mc.concreteReturnType())) {
            return true;
        }
        if (isNonModifyingRead(mc.methodInfo())) {
            Value.Independent independent = independentOf(mc.methodInfo());
            if (independent != null) {
                if (independent.isIndependent()) return true;
                if (independent.isDependent()) {
                    if (receiverCommitted) return true;
                } else if (typeParametersHoldCommittableContent(walk.member(), mc.concreteReturnType())) {
                    return true;
                }
            }
        }
        return returnTypeHoldsCommittableContent(walk.member(), mc);
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
        return returnTypeHoldsCommittableContent(member, mc.concreteReturnType());
    }

    private boolean returnTypeHoldsCommittableContent(TypeInfo member, ParameterizedType returnType) {
        if (returnType.arrays() > 0) return false; // array content stays mutable whatever the element type
        if (returnType.isPrimitiveExcludingVoid() || returnType.isVoid()) return true; // a value cannot alias
        TypeInfo rtType = returnType.bestTypeInfo();
        if (rtType == null) return false; // an unresolved type parameter: conservative
        // a primitive pipeline (mapToInt reductions) hands on VALUES only: its hidden content is primitive,
        // so nothing mutable can be laundered through it. The general immutable-hc route cannot admit it --
        // a stream is contractually consumable -- and, parameterless, it has no type-parameter route either.
        if (EventualCluster.ENABLED && isPrimitiveStream(rtType)) return true;
        if (isEventuallyImmutableFieldType(member, rtType)) return true;
        Value.Immutable immutable = immutableOf(rtType);
        if (immutable.isImmutable()) return true; // no hidden content at all: nothing mutable to alias
        if (!immutable.isAtLeastImmutableHC()) return false; // reads through it might expose mutable content
        // parameterless immutable-hc (MethodInspectionImpl, String): all its methods are non-modifying, so
        // nothing can be modified through the value -- the same trust isNonModifyingRead places in a declared
        // immutable type. Only a PARAMETERISED wrapper (Either, List) can launder mutable content, and then a
        // committable parameter is required.
        if (returnType.parameters().isEmpty()) return true;
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
    private LocalContext buildLocalContext(WalkRoot walk, MethodInfo methodInfo) {
        // pass 1 -- freshness, to a least fixpoint over the assignment graph: a local is fresh when every
        // (non-empty) assignment to it is rooted in a fresh creation -- a plain constructor call, or a chain
        // of reads off an already-fresh local. The transitive step is the local-variable spelling of the
        // fluent chain rootedInFresh already excuses inline: 'TypeInfo.Builder b = typeInfo.builder();
        // b.setX(…)' must behave exactly like 'typeInfo.builder().setX(…)'. Fresh + this-derived-tracked can
        // coexist (the constructor args carry labels).
        Map<Variable, List<Expression>> assignments = new HashMap<>();
        methodInfo.methodBody().visit(e -> {
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv ->
                        recordAssignment(assignments, lv, lv.assignmentExpression()));
            } else if (e instanceof Assignment a
                       && !(a.variableTarget() instanceof FieldReference)
                       && !(a.variableTarget() instanceof This)) {
                recordAssignment(assignments, a.variableTarget(), a.value());
            }
            return true;
        });
        Set<Variable> freshCandidates = new HashSet<>();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<Variable, List<Expression>> entry : assignments.entrySet()) {
                if (!freshCandidates.contains(entry.getKey())
                    && entry.getValue().stream().allMatch(v -> rootedInFresh(v, freshCandidates))) {
                    freshCandidates.add(entry.getKey());
                    grew = true;
                }
            }
        }

        // passes 2+3, twice -- commit labels to a fixpoint for chained locals (var b = a;), then container
        // aliases; a discovered alias un-poisons downstream locals (the copy built FROM the aliased local),
        // so the commit fixpoint is re-derived once with the aliases in place.
        // Pass 3, container aliases: a POISONED local whose every assignment is either the bare read of one
        // root container field F (the list.isEmpty() ? list : rebuilt short-circuit), or a value already
        // committed after a subset of {F}, aliases F. Its VALUE stays uncommittable (null in the commit map,
        // exactly like the bare field), but the per-site container rescues may treat it as the field spelled
        // inline. The subset guard keeps the minted label sound: claiming "safe after F" for a value that is
        // in truth safe after less (or after F itself) only ever weakens.
        Map<Variable, FieldInfo> aliases = Map.of();
        Map<Variable, Set<String>> map = new HashMap<>();
        Set<Variable> fresh = Set.copyOf(freshCandidates);
        for (int round = 0; round < 2; round++) {
            map = new HashMap<>();
            LocalContext ctx = new LocalContext(map, fresh, aliases);
            boolean changed = true;
            int guard = 0;
            while (changed && guard++ < 10) {
                boolean[] change = {false};
                methodInfo.methodBody().visit(e -> {
                    if (e instanceof LocalVariableCreation lvc) {
                        lvc.localVariableStream().forEach(lv ->
                                change[0] |= trackAssignment(walk, ctx, lv, lv.assignmentExpression()));
                    } else if (e instanceof Assignment a
                               && !(a.variableTarget() instanceof FieldReference)
                               && !(a.variableTarget() instanceof This)) {
                        change[0] |= trackAssignment(walk, ctx, a.variableTarget(), a.value());
                    }
                    return true;
                });
                changed = change[0];
            }
            if (round == 1) break; // aliases already applied
            Map<Variable, FieldInfo> found = new HashMap<>();
            for (Map.Entry<Variable, List<Expression>> entry : assignments.entrySet()) {
                if (ctx.commit().get(entry.getKey()) != null) continue; // only poisoned locals need the alias
                FieldInfo alias = null;
                boolean ok = true;
                for (Expression value : entry.getValue()) {
                    FieldInfo f = aliasedContainerField(walk, value, ctx);
                    if (f == null || alias != null && !alias.equals(f)) {
                        ok = false;
                        break;
                    }
                    alias = f;
                }
                if (ok && alias != null) found.put(entry.getKey(), alias);
                if (siteDebug()) ecsite("alias " + entry.getKey().simpleName() + " = "
                        + (ok && alias != null ? alias.name() : "none"));
            }
            if (found.isEmpty()) break; // nothing would change in round 2
            aliases = Map.copyOf(found);
        }
        return new LocalContext(map, fresh, aliases);
    }

    /** The single root container field {@code value} aliases: the bare field read itself, or a ternary whose
     *  branches each alias that field or commit after a subset of its label. Null when it is anything else. */
    private FieldInfo aliasedContainerField(WalkRoot walk, Expression value, LocalContext ctx) {
        if (value instanceof Cast cast) return aliasedContainerField(walk, cast.expression(), ctx);
        if (value instanceof EnclosedExpression enclosed) return aliasedContainerField(walk, enclosed.inner(), ctx);
        if (value instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
            && walk.scopeIsRoot(fr) && containerContentCommittable(fr.fieldInfo())) {
            return fr.fieldInfo();
        }
        if (value instanceof InlineConditional inline) {
            FieldInfo t = aliasedContainerField(walk, inline.ifTrue(), ctx);
            FieldInfo f = aliasedContainerField(walk, inline.ifFalse(), ctx);
            FieldInfo field = t != null ? t : f;
            if (field == null) return null;
            if (t != null && f != null) return t.equals(f) ? t : null;
            Expression other = t != null ? inline.ifFalse() : inline.ifTrue();
            Set<String> otherLabels = commitLabels(walk, other, ctx);
            return otherLabels != null && Set.of(field.name()).containsAll(otherLabels) ? field : null;
        }
        return null;
    }

    private static void recordAssignment(Map<Variable, List<Expression>> assignments,
                                         Variable target, Expression value) {
        if (value == null || value.isEmpty()) return; // a bare declaration neither makes nor breaks freshness
        assignments.computeIfAbsent(target, t -> new ArrayList<>()).add(value);
    }

    /** Fold one assignment into the map; true when the map changed. Once poisoned (null), a variable stays so. */
    private boolean trackAssignment(WalkRoot walk, LocalContext ctx, Variable target, Expression value) {
        if (value == null || value.isEmpty()) return false;
        if (walk.isRoot(target)) return false; // rebinding the root is the outer walk's bail, not a tracked local
        if (!referencesRootOrTracked(walk, value, ctx.commit())) return false; // not root-derived: untracked (∅)
        Set<String> labels = commitLabels(walk, value, ctx);
        if (siteDebug()) ecsite("track " + target.simpleName() + " = " + labels);
        Map<Variable, Set<String>> map = ctx.commit();
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

    /** {@link #referencesThis} generalized to the walk's root, extended with the locals tracked as holding
     *  root-derived values. */
    private boolean referencesRootOrTracked(WalkRoot walk, Expression expression,
                                            Map<Variable, Set<String>> tracked) {
        boolean[] found = {false};
        expression.visit(e -> {
            if (e instanceof VariableExpression ve
                && (walk.matches(ve.variable()) || tracked.containsKey(ve.variable()))) {
                found[0] = true;
                return false;
            }
            return !found[0];
        });
        return found[0];
    }

    /** The dispatch-closure half of the site's modification view: an abstract callee with at least one
     *  implementation honestly decided modifying. Deliberately uncached -- the honest FALSE arrives at the
     *  modreach cutover, and the eventual layer re-derives after it. */
    private static boolean implementationHonestlyModifies(MethodInfo methodInfo) {
        if (!methodInfo.isAbstract()) return false;
        return anyImplementationModifies(methodInfo, PropertyImpl.IMPLEMENTATIONS)
               || anyImplementationModifies(methodInfo, PropertyImpl.EXTERNAL_IMPLEMENTATIONS);
    }

    private static boolean anyImplementationModifies(MethodInfo methodInfo,
                                                     org.e2immu.language.cst.api.analysis.Property property) {
        for (MethodInfo implementation : methodInfo.analysis()
                .getOrDefault(property, ValueImpl.SetOfMethodInfoImpl.EMPTY).methodInfoSet()) {
            Value.Bool nonModifying = implementation.analysis()
                    .getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
            if (nonModifying != null && nonModifying.isFalse()) return true;
        }
        return false;
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
     * modify anything. A MUTABLE container of committable content deliberately does NOT qualify here: its
     * wrapper layer never commits, so the bare value is never safe to hand out -- the weaker, per-read
     * container ride-along lives in {@link #containerReadThroughLabels}.
     */
    private boolean fieldHoldsCommittableContent(FieldInfo fieldInfo) {
        // the member leaning on the field's type is the type that declares the field
        TypeInfo member = fieldInfo.owner();
        TypeInfo fieldType = fieldInfo.type().bestTypeInfo();
        if (fieldType == null) return false;
        if (isEventuallyImmutableFieldType(member, fieldType)) return true;
        if (siteDebug()) ecsite("fieldHolds refusal for " + fieldInfo.name() + ": type "
                + fieldType.fullyQualifiedName() + " not committable for member " + member.simpleName());
        if (!immutableOf(fieldType).isAtLeastImmutableHC()) return false; // the wrapper read must not itself modify
        for (ParameterizedType arg : fieldInfo.type().parameters()) {
            TypeInfo argType = arg.bestTypeInfo();
            if (argType != null && isEventuallyImmutableFieldType(member, argType)) return true;
        }
        return false;
    }

    // positive cache only: wrapper stability becomes provable monotonically as callee verdicts decide
    private final Set<FieldInfo> wrapperStableCache = ConcurrentHashMap.newKeySet();

    /**
     * EVENTUALCLUSTER: the <b>container ride-along</b> (spec-eventually-unmodified-parameter §8.3 item 1),
     * the §060 ride-along one indirection deeper, granted PER READ rather than at the field level: a call on
     * a root-scoped final container field of committable content ({@code children.get(0)},
     * {@code parameters.stream()}) commits after the field's label -- once the elements commit, the read
     * cannot hand on anything modifiable. Soundness is carried by three conditions the strong field rule
     * does not need, because a mutable wrapper never commits:
     * <ul>
     * <li>the read is non-modifying AND not {@code @Dependent} -- a dependent view ({@code iterator()},
     *     {@code subList()}) shares the wrapper's accessible layer and could mutate it downstream;</li>
     * <li>{@link #ownerNeverMutatesWrapper}: no own non-construction code calls a modifying method directly
     *     on the field (construction-phase fills are the fresh object's lifecycle). Aliased mutation needs
     *     the bare wrapper value first, which stays non-committable ({@link #fieldHoldsCommittableContent})
     *     and poisons the aliasing walk instead;</li>
     * <li>exposure to OUTSIDE mutation remains the type-level analyzers' jurisdiction, as for every
     *     accessible-content field: the labels minted here reach a verdict only through
     *     {@code immutableAfterMark}, and cross-type consumers lean on the owner's witnessed,
     *     contraction-validated verdict.</li>
     * </ul>
     * Null when the ride-along does not apply -- the caller bails as before.
     */
    private Set<String> containerReadThroughLabels(WalkRoot walk, MethodCall mc, LocalContext ctx) {
        if (!EventualCluster.ENABLED) return null;
        FieldInfo fieldInfo = rootContainerField(walk, mc.object(), ctx);
        if (fieldInfo == null) return null;
        if (!isNonModifyingRead(mc.methodInfo())) return null;
        Value.Independent independent = independentOf(mc.methodInfo());
        if (independent == null || independent.isDependent()) return null;
        if (!containerContentCommittable(fieldInfo)) return null;
        return Set.of(fieldInfo.name());
    }

    /** The root's container field behind the expression: the bare read {@code this.f} / {@code p.f}, its
     *  accessor spelling {@code comments()} -- a plain non-setter accessor called on the bare root, the way
     *  the whole statement/expression family hands its final lists around -- a local ALIASING the field
     *  ({@link LocalContext#containerAlias}), or the ternary short-circuit shape. Null when none. */
    private FieldInfo rootContainerField(WalkRoot walk, Expression expr, LocalContext ctx) {
        if (expr instanceof VariableExpression ve) {
            if (ve.variable() instanceof FieldReference fr && walk.scopeIsRoot(fr)) {
                return fr.fieldInfo();
            }
            FieldInfo alias = ctx.containerAlias().get(ve.variable());
            if (alias != null) return alias;
        }
        if (expr instanceof MethodCall mc
            && mc.object() instanceof VariableExpression ve && walk.isRoot(ve.variable())) {
            Value.FieldValue fieldValue = mc.methodInfo().getSetField();
            if (fieldValue != null && fieldValue.field() != null && !fieldValue.setter()) {
                return fieldValue.field();
            }
        }
        if (expr instanceof InlineConditional) return aliasedContainerField(walk, expr, ctx);
        return null;
    }

    /**
     * The argument-position half of the container ride-along: the bare wrapper VALUE of a qualifying
     * container field handed to a callee that provably does not mutate it ({@code UNMODIFIED_PARAMETER})
     * and retains at most its element content (an independent or hc-independent parameter -- hidden content
     * IS the committable elements), or stores it only into fields that are themselves qualifying containers
     * (the fluent-copy constructor, {@code new ParameterizedTypeImpl(…, parameters, …)}: the wrapper moves
     * between owners that never mutate it). Null when it does not apply.
     */
    private Set<String> containerArgumentLabels(WalkRoot walk, MethodInfo callee, int index, Expression arg,
                                                LocalContext ctx) {
        if (!EventualCluster.ENABLED) return null;
        FieldInfo fieldInfo = rootContainerField(walk, arg, ctx);
        if (fieldInfo == null) return null;
        if (!containerContentCommittable(fieldInfo)) return null;
        List<ParameterInfo> parameters = callee.parameters();
        if (parameters.isEmpty()) return null;
        ParameterInfo pi = parameters.get(Math.min(index, parameters.size() - 1));
        if (pi.isVarArgs() && index >= parameters.size() - 1) return null;
        if (callee.isConstructor()) {
            // a capturing constructor's parameter reads as modified (the capture joins the fresh object's
            // initialization graph) and @Dependent even for a defensive copy, so the plain properties cannot
            // answer the wrapper questions -- the body is asked directly for all three (mutation, onward
            // handoff, capture target)
            if (!ctorHandlesWrapperSafely(callee, pi)) {
                if (siteDebug()) ecsite("ctor wrapper-unsafe for " + fieldInfo.name()
                        + " at " + callee.fullyQualifiedName());
                return null;
            }
            return Set.of(fieldInfo.name());
        }
        Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
        if (unmodified == null || unmodified.isFalse()) return null;
        Value.Independent independent = pi.analysis()
                .getOrNull(PropertyImpl.INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);
        if (independent != null && !independent.isDependent()) return Set.of(fieldInfo.name());
        // dependent (or undecided): acceptable only when the retention is into qualifying container fields
        Value.AssignedToField assigned = pi.analysis()
                .getOrDefault(PropertyImpl.PARAMETER_ASSIGNED_TO_FIELD, ValueImpl.AssignedToFieldImpl.EMPTY);
        if (assigned.fields().isEmpty()) return null;
        for (FieldInfo target : assigned.fields()) {
            if (!containerContentCommittable(target)) return null;
        }
        return Set.of(fieldInfo.name());
    }

    /**
     * Does the constructor's body provably handle the wrapper handed in via {@code pi} safely? Three checks,
     * all syntactic: only non-modifying reads with the parameter as receiver; every onward handoff to an
     * unmodified, non-dependent callee parameter (a defensive {@code List.copyOf}); and every direct capture
     * ({@code this.f = pi}, possibly through a ternary) into a field that is itself a qualifying container
     * -- the wrapper then moves between owners that never mutate it. An alias into a local is refused.
     */
    private boolean ctorHandlesWrapperSafely(MethodInfo constructor, ParameterInfo pi) {
        if (constructor.methodBody().isEmpty()) return false;
        boolean[] bad = {false};
        constructor.methodBody().visit(e -> {
            if (bad[0]) return false;
            if (e instanceof MethodCall mc) {
                if (isParameter(mc.object(), pi) && !isNonModifyingRead(mc.methodInfo())) {
                    bad[0] = true;
                    return false;
                }
                List<Expression> args = mc.parameterExpressions();
                for (int i = 0; i < args.size(); i++) {
                    if (!isParameter(args.get(i), pi)) continue;
                    // an @Identity forward (this.f = Objects.requireNonNull(p)) is transparent: the value is
                    // judged where the RESULT lands (the assignment branch unwraps it), not here
                    if (i == 0 && isIdentityMethod(mc.methodInfo())) continue;
                    List<ParameterInfo> calleeParams = mc.methodInfo().parameters();
                    ParameterInfo cpi = calleeParams.isEmpty() ? null
                            : calleeParams.get(Math.min(i, calleeParams.size() - 1));
                    Value.Bool unmod = cpi == null ? null
                            : cpi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
                    Value.Independent ind = cpi == null ? null
                            : cpi.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                            ValueImpl.IndependentImpl.class);
                    if (unmod == null || unmod.isFalse() || ind == null || ind.isDependent()) {
                        bad[0] = true;
                        return false;
                    }
                }
            } else if (e instanceof ConstructorCall cc
                       && cc.parameterExpressions().stream().anyMatch(arg -> isParameter(arg, pi))) {
                bad[0] = true; // nested capture: conservative
                return false;
            } else if (e instanceof MethodReference mr
                       && isParameter(mr.scope(), pi) && !isNonModifyingRead(mr.methodInfo())) {
                bad[0] = true;
                return false;
            } else if (e instanceof Assignment a && assignsParameter(a.value(), pi)) {
                if (!(a.variableTarget() instanceof FieldReference fr)
                    || !containerContentCommittable(fr.fieldInfo())) {
                    bad[0] = true;
                    return false;
                }
            }
            return true;
        });
        return !bad[0];
    }

    private static boolean isParameter(Expression expression, ParameterInfo pi) {
        return expression instanceof VariableExpression ve && pi.equals(ve.variable());
    }

    private static boolean isPrimitiveStream(TypeInfo typeInfo) {
        String fqn = typeInfo.fullyQualifiedName();
        return "java.util.stream.IntStream".equals(fqn) || "java.util.stream.LongStream".equals(fqn)
               || "java.util.stream.DoubleStream".equals(fqn);
    }

    /** Is the bare parameter (possibly behind casts, parentheses, one of a ternary's branches, or an
     *  {@code @Identity} forward -- {@code Objects.requireNonNull(p)} IS p) the value being assigned?
     *  Nested occurrences inside other calls are the call scans' business, not a capture. */
    private static boolean assignsParameter(Expression value, ParameterInfo pi) {
        if (value == null) return false;
        if (isParameter(value, pi)) return true;
        if (value instanceof Cast cast) return assignsParameter(cast.expression(), pi);
        if (value instanceof EnclosedExpression enclosed) return assignsParameter(enclosed.inner(), pi);
        if (value instanceof InlineConditional inline) {
            return assignsParameter(inline.ifTrue(), pi) || assignsParameter(inline.ifFalse(), pi);
        }
        if (value instanceof MethodCall mc && isIdentityMethod(mc.methodInfo())
            && !mc.parameterExpressions().isEmpty()) {
            return assignsParameter(mc.parameterExpressions().getFirst(), pi);
        }
        return false;
    }

    /** {@code @Identity} (aapi: {@code Objects.requireNonNull}): the method returns its first parameter. */
    private static boolean isIdentityMethod(MethodInfo methodInfo) {
        return methodInfo.analysis().getOrDefault(PropertyImpl.IDENTITY_METHOD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    /** The field-level half of the container ride-along: final, arrays-free, every type parameter
     *  committable, wrapper provably never mutated by the owner. The per-read half (non-modifying,
     *  non-dependent callee) is {@link #containerReadThroughLabels}'s. */
    private boolean containerContentCommittable(FieldInfo fieldInfo) {
        return EventualCluster.ENABLED
               && fieldInfo.type().arrays() == 0 // array content stays rebindable whatever the element type
               && fieldInfo.analysis().getOrDefault(FINAL_FIELD, ValueImpl.BoolImpl.FALSE).isTrue()
               && typeParametersHoldCommittableContent(fieldInfo.owner(), fieldInfo.type())
               && ownerNeverMutatesWrapper(fieldInfo);
    }

    /**
     * Does the owner's non-construction code provably never mutate the wrapper object held in
     * {@code fieldInfo}? Syntactic scan: no modifying (or still-undecided) call, and no method reference to
     * one, with the field itself as receiver -- on ANY instance ({@code other.pool.add} breaks the promise
     * for the whole class). Constructors are excluded on purpose: a construction-phase fill belongs to the
     * fresh object's lifecycle, before any commitment is observable.
     */
    private boolean ownerNeverMutatesWrapper(FieldInfo fieldInfo) {
        if (wrapperStableCache.contains(fieldInfo)) return true;
        boolean[] mutated = {false};
        for (MethodInfo methodInfo : fieldInfo.owner().methods()) {
            if (mutated[0]) break;
            if (methodInfo.methodBody().isEmpty()) continue;
            methodInfo.methodBody().visit(e -> {
                if (mutated[0]) return false;
                if (e instanceof MethodCall mc && receiverIsField(mc.object(), fieldInfo)
                    && !isNonModifyingRead(mc.methodInfo())) {
                    mutated[0] = true;
                    return false;
                }
                if (e instanceof MethodReference mr && receiverIsField(mr.scope(), fieldInfo)
                    && !isNonModifyingRead(mr.methodInfo())) {
                    mutated[0] = true;
                    return false;
                }
                return true;
            });
        }
        if (mutated[0]) return false;
        wrapperStableCache.add(fieldInfo);
        return true;
    }

    private static boolean receiverIsField(Expression object, FieldInfo fieldInfo) {
        return object instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
               && fr.fieldInfo().equals(fieldInfo);
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
            // something else entirely and must not be re-labelled with our field's name. Under EVENTUALCLUSTER
            // a cluster candidate passes optimistically (seed + witness; off the gate this is the same strict
            // check): the @TestMark forward through a candidate-typed field (TypeInspectionImpl.Builder
            // .hasBeenCommitted -> typeInfo.hasBeenInspected()) is otherwise stuck behind the candidate's own
            // verdict, which in turn waits on this very classification -- the staircase never converges.
            TypeInfo fieldType = fieldInfo.type().bestTypeInfo();
            if (fieldType == null) return null;
            if (!isEventuallyImmutableFieldType(typeInfo, fieldType)) return null;
            // the label is OUR field, not the support class's internal field name: the transition our callers
            // observe is "this.inspection has been set", not "EventuallyFinalOnDemand.isFinal"
            return Set.of(fieldInfo.name());
        }
        return null;
    }

    private static boolean isOwnField(TypeInfo typeInfo, FieldInfo fieldInfo) {
        return typeInfo.fields().contains(fieldInfo);
    }

    /** EVENTUALCLUSTER commit walk only: the walk's root object also owns its superclasses' fields; the
     *  ungated legacy paths keep the strict own-field rule. */
    private static boolean isOwnOrInheritedField(TypeInfo typeInfo, FieldInfo fieldInfo) {
        if (typeInfo.fields().contains(fieldInfo)) return true;
        ParameterizedType parent = typeInfo.parentClass();
        while (parent != null && parent.typeInfo() != null && !parent.typeInfo().isJavaLangObject()) {
            if (parent.typeInfo().fields().contains(fieldInfo)) return true;
            parent = parent.typeInfo().parentClass();
        }
        return false;
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
