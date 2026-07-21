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

import org.e2immu.analyzer.modification.analyzer.GuardAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.annotation.Independent;
import org.e2immu.language.cst.impl.analysis.MessageImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;

/**
 * Verifies user-written contracts against the analyzer's computed values.
 * <ul>
 *   <li>{@code @Container} on a type: no non-private method of the type, nor any implementation of its
 *       abstract methods, may modify a parameter. The contract binds implementations only through the
 *       methods they inherit from the contracted type (a subtype may add modifying methods of its own).</li>
 *   <li>{@code @Immutable} on a type: the four rules of immutability, checked one by one per field, so that a
 *       violation names the rule and the member that breaks it.</li>
 *   <li>{@code @NotModified} / {@code @Independent} on an abstract method: no implementation may be modifying,
 *       resp. dependent.</li>
 *   <li>{@code @NotModified} / {@code @Independent} on a concrete method: no override may weaken it.</li>
 * </ul>
 * A violation is only reported on a decided value — an undecided one (cycle, external code, delayed) stays silent,
 * so the guard never reports on incomplete information. Where a contracted property is trusted rather than computed
 * (the analyzers early-return on a good-enough value), the guard reads the computed values one level down: the
 * implementations of an abstract method, the parameters and fields of a contracted type.
 */
public class GuardAnalyzerImpl extends CommonAnalyzerImpl implements GuardAnalyzer {
    public static final String CONTRACT_VIOLATION = "contract-violation";
    /** a contract the guard can see but cannot check locally; see {@link #guardDynamicImmutableFields} */
    public static final String CONTRACT_UNVERIFIABLE = "contract-unverifiable";
    public static final String NEAR_MISS_CONTAINER = "near-miss-container";
    public static final String NEAR_MISS_NOT_MODIFIED = "near-miss-not-modified";
    public static final String NEAR_MISS_INDEPENDENT = "near-miss-independent";
    public static final String NEAR_MISS_IMMUTABLE = "near-miss-immutable";

    private final ContractReader contractReader;
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public GuardAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration, List<Message> messages) {
        super(configuration, null, messages);
        this.contractReader = new ContractReader(runtime);
    }

    @Override
    public void go(List<Info> analysisOrder) {
        if (configuration.guardContracts()) {
            for (Info info : analysisOrder) {
                if (info instanceof TypeInfo typeInfo) {
                    guardType(typeInfo);
                } else if (info instanceof MethodInfo methodInfo) {
                    guardMethod(methodInfo);
                    guardOverride(methodInfo);
                }
            }
        }
        if (configuration.warnNearMisses()) {
            nearMissPass(analysisOrder);
        }
    }

    // ---- near-miss warnings: the mirror image of the guard (no contract written, one culprit short) ----

    /** A single parameter of a non-private constructor/method — the unit the container rule quantifies over. */
    private record Slot(MethodInfo method, ParameterInfo parameter, int index) {
    }

    /** A finding plus the keys it is ranked by, so the most compelling suggestions survive a capped display. */
    private record RankedFinding(Message message, int blocking, int totalSlots) {
    }

    /**
     * Container near-misses over the whole analysis order, ranked most-compelling first (fewest blocking slots,
     * then largest surface) before they join the shared message list.
     */
    private void nearMissPass(List<Info> analysisOrder) {
        IteratingAnalyzer.NearMissPolicy policy = configuration.nearMissPolicy();
        List<RankedFinding> findings = new ArrayList<>();
        for (Info info : analysisOrder) {
            if (info instanceof TypeInfo typeInfo) {
                RankedFinding container = containerNearMiss(typeInfo, policy);
                if (container != null) findings.add(container);
                typeImmutabilityNearMisses(typeInfo, policy, findings);
            } else if (info instanceof MethodInfo methodInfo) {
                methodNearMisses(methodInfo, policy, findings);
            }
        }
        findings.sort(Comparator.comparingInt(RankedFinding::blocking)
                .thenComparing(Comparator.comparingInt(RankedFinding::totalSlots).reversed()));
        for (RankedFinding f : findings) analyzerMessages.add(f.message());
    }

    /**
     * Is {@code typeInfo} one culprit short of {@code @Container}? Only when it wrote no {@code @Container}, its
     * computed {@code CONTAINER_TYPE} is decided FALSE, every parameter slot is decided (else we could not claim
     * "all the others are fine"), and the blocking slots — at most {@code maxBlockingSlots}, on a surface of at
     * least {@code minParameterSlots} — each pass their gate (a slot on an abstract method must be attributable to
     * a single implementation out of many). Mirrors {@link TypeContainerAnalyzerImpl}'s parameter enumeration.
     *
     * @return the ranked finding, or {@code null} when it is not a near-miss.
     */
    private RankedFinding containerNearMiss(TypeInfo typeInfo, IteratingAnalyzer.NearMissPolicy policy) {
        // the user contracted @Container (the guard polices it), or it already is one: nothing to suggest.
        if (contractReader.contracts(typeInfo).get(CONTAINER_TYPE) instanceof Value.Bool contracted
            && contracted.isTrue()) {
            return null;
        }
        Value.Bool computed = typeInfo.analysis().getOrNull(CONTAINER_TYPE, ValueImpl.BoolImpl.class);
        if (computed == null || computed.isTrue()) return null; // undecided, or already a container

        List<MethodInfo> methods = typeInfo.constructorAndMethodStream()
                .filter(mi -> !mi.access().isPrivate()).toList();
        List<Slot> blocking = new ArrayList<>();
        int totalSlots = 0;
        for (MethodInfo mi : methods) {
            List<ParameterInfo> parameters = mi.parameters();
            for (int i = 0; i < parameters.size(); i++) {
                ParameterInfo pi = parameters.get(i);
                Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
                if (unmodified == null) return null; // undecided discipline: cannot claim the near-miss
                totalSlots++;
                if (unmodified.isFalse()) blocking.add(new Slot(mi, pi, i));
            }
        }
        if (totalSlots < policy.minParameterSlots()) return null;
        if (blocking.isEmpty() || blocking.size() > policy.maxBlockingSlots()) return null;

        List<Message> causes = new ArrayList<>();
        for (Slot slot : blocking) {
            Message cause = slot.method().isAbstract() ? abstractSlotCause(slot, policy) : concreteSlotCause(slot);
            if (cause == null) return null; // a blocking slot that does not pass its gate: not a near-miss
            causes.add(cause);
        }
        String slotList = blocking.stream()
                .map(s -> "parameter '" + s.parameter().simpleName() + "' of " + s.method().fullyQualifiedName())
                .collect(Collectors.joining(", "));
        String message = typeInfo.fullyQualifiedName() + " would satisfy @Container but for " + blocking.size()
                         + " of its " + totalSlots + " parameter slots (" + slotList + " modified); consider @Container";
        Message warning = MessageImpl.warn(typeInfo, NEAR_MISS_CONTAINER, message, causes.toArray(new Message[0]));
        return new RankedFinding(warning, blocking.size(), totalSlots);
    }

    /** A concrete method modifying its own parameter: the direct-site blame walk is the cause (or a plain note). */
    private Message concreteSlotCause(Slot slot) {
        Message blame = blameParameterModified(slot.method(), slot.parameter());
        return blame != null ? blame : MessageImpl.cause(slot.parameter(), "parameter '"
                + slot.parameter().simpleName() + "' of " + slot.method().fullyQualifiedName() + " is modified");
    }

    /**
     * An abstract method whose parameter aggregate is modified: the "1 of N" gate. Attribute the modification to
     * the implementations that cause it; a near-miss only when at least {@code minImplementations} exist and at
     * most {@code maxBlockingImplementations} of them modify. Returns {@code null} (suppressing the whole finding)
     * when the modification is widespread, when there are too few implementations, or when any implementation's
     * value is undecided (then the count cannot be trusted).
     */
    private Message abstractSlotCause(Slot slot, IteratingAnalyzer.NearMissPolicy policy) {
        int total = 0;
        List<MethodInfo> modifying = new ArrayList<>();
        for (MethodInfo impl : implementationsOf(slot.method())) {
            total++;
            if (slot.index() >= impl.parameters().size()) return null; // defensive: signature shape mismatch
            ParameterInfo implPi = impl.parameters().get(slot.index());
            Value.Bool unmodified = implPi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
            if (unmodified == null) return null; // an undecided implementation: cannot attribute "1 of N"
            if (unmodified.isFalse()) modifying.add(impl);
        }
        if (total < policy.minImplementations()) return null;
        if (modifying.isEmpty() || modifying.size() > policy.maxBlockingImplementations()) return null;

        MethodInfo culprit = modifying.getFirst();
        Message blame = blameParameterModified(culprit, culprit.parameters().get(slot.index()));
        String attribution = culprit.fullyQualifiedName() + " modifies parameter '" + slot.parameter().simpleName()
                             + "' — the only " + modifying.size() + " of " + total + " implementations of "
                             + slot.method().fullyQualifiedName() + " that does";
        return blame != null ? MessageImpl.cause(culprit, attribution, blame)
                : MessageImpl.cause(culprit, attribution);
    }

    /**
     * Method-level near-misses: an abstract method one implementation short of being contractable
     * {@code @NotModified} (no implementation modifies its receiver) or {@code @Independent} (no implementation
     * exposes state). Skipped when the user already contracted that property — the guard polices those. A method
     * can yield both a {@code @NotModified} and an {@code @Independent} near-miss.
     */
    private void methodNearMisses(MethodInfo methodInfo, IteratingAnalyzer.NearMissPolicy policy,
                                  List<RankedFinding> findings) {
        if (!methodInfo.isAbstract()) return;
        Map<Property, Value> contracts = contractReader.contracts(methodInfo);
        if (!(contracts.get(NON_MODIFYING_METHOD) instanceof Value.Bool nm && nm.isTrue())) {
            RankedFinding f = notModifiedNearMiss(methodInfo, policy);
            if (f != null) findings.add(f);
        }
        if (!(contracts.get(INDEPENDENT_METHOD) instanceof Value.Independent ind && ind.isAtLeastIndependentHc())) {
            RankedFinding f = independentNearMiss(methodInfo, policy);
            if (f != null) findings.add(f);
        }
    }

    /**
     * {@code @NotModified} near-miss: exactly one modifying implementation (out of at least
     * {@code minImplementations}). The implementations carry genuinely computed values, so the count is trusted;
     * an undecided implementation suppresses the finding.
     */
    private RankedFinding notModifiedNearMiss(MethodInfo abstractMethod, IteratingAnalyzer.NearMissPolicy policy) {
        int total = 0;
        List<MethodInfo> modifying = new ArrayList<>();
        for (MethodInfo impl : implementationsOf(abstractMethod)) {
            total++;
            Value.Bool nonModifying = impl.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
            if (nonModifying == null) return null; // undecided implementation: cannot attribute "1 of N"
            if (nonModifying.isFalse()) modifying.add(impl);
        }
        if (total < policy.minImplementations()) return null;
        if (modifying.isEmpty() || modifying.size() > policy.maxBlockingImplementations()) return null;

        MethodInfo culprit = modifying.getFirst();
        Message blame = blameMethodModifying(culprit);
        String attribution = culprit.fullyQualifiedName() + " is modifying — the only " + modifying.size() + " of "
                             + total + " implementations of " + abstractMethod.fullyQualifiedName() + " that is";
        Message cause = blame != null ? MessageImpl.cause(culprit, attribution, blame)
                : MessageImpl.cause(culprit, attribution);
        String message = abstractMethod.fullyQualifiedName() + " would satisfy @NotModified but for " + modifying.size()
                         + " of its " + total + " implementations (" + culprit.fullyQualifiedName()
                         + "); consider @NotModified";
        return new RankedFinding(MessageImpl.warn(abstractMethod, NEAR_MISS_NOT_MODIFIED, message, cause),
                modifying.size(), total);
    }

    /**
     * {@code @Independent} near-miss: exactly one decided-DEPENDENT implementation (out of at least
     * {@code minImplementations}). Reads the implementations, never the abstract method's own value — that one may
     * be a {@code ShallowMethodAnalyzer} default rather than a computation (see {@code guardIndependentType}).
     */
    private RankedFinding independentNearMiss(MethodInfo abstractMethod, IteratingAnalyzer.NearMissPolicy policy) {
        int total = 0;
        List<MethodInfo> dependent = new ArrayList<>();
        for (MethodInfo impl : implementationsOf(abstractMethod)) {
            total++;
            Value.Independent ind = impl.analysis().getOrNull(INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
            if (ind == null) return null; // undecided implementation: cannot attribute "1 of N"
            if (ind.isDependent()) dependent.add(impl);
        }
        if (total < policy.minImplementations()) return null;
        if (dependent.isEmpty() || dependent.size() > policy.maxBlockingImplementations()) return null;

        MethodInfo culprit = dependent.getFirst();
        Message blame = blameMethodDependent(culprit);
        String attribution = culprit.fullyQualifiedName() + " is dependent — the only " + dependent.size() + " of "
                             + total + " implementations of " + abstractMethod.fullyQualifiedName() + " that is";
        Message cause = blame != null ? MessageImpl.cause(culprit, attribution, blame)
                : MessageImpl.cause(culprit, attribution);
        String message = abstractMethod.fullyQualifiedName() + " would satisfy @Independent but for " + dependent.size()
                         + " of its " + total + " implementations (" + culprit.fullyQualifiedName()
                         + "); consider @Independent";
        return new RankedFinding(MessageImpl.warn(abstractMethod, NEAR_MISS_INDEPENDENT, message, cause),
                dependent.size(), total);
    }

    /**
     * Type-level immutability near-misses. {@code @Immutable} subsumes {@code @Independent} (its rule 3), so an
     * immutable near-miss suppresses the independence one for the same type — reporting both would name the same
     * dependent field twice, exactly the noise {@code guardIndependentType} avoids on the contract side.
     */
    private void typeImmutabilityNearMisses(TypeInfo typeInfo, IteratingAnalyzer.NearMissPolicy policy,
                                            List<RankedFinding> findings) {
        RankedFinding immutable = immutableNearMiss(typeInfo, policy);
        if (immutable != null) {
            findings.add(immutable);
            return;
        }
        RankedFinding independent = independentTypeNearMiss(typeInfo, policy);
        if (independent != null) findings.add(independent);
    }

    /**
     * {@code @Immutable} near-miss: a type one field short of immutability. Skipped when the user contracted
     * {@code @Immutable} (the guard polices it), when the type is eventually immutable, or when its computed
     * {@code IMMUTABLE_TYPE} is undecided or already at-least-immutable-HC. Counts fields failing an immutability
     * rule via the same {@link #immutableFieldFailingRule} the guard uses; a type whose non-immutability comes from
     * something other than a field (e.g. a modifying abstract method) has no blocking field and stays silent.
     */
    private RankedFinding immutableNearMiss(TypeInfo typeInfo, IteratingAnalyzer.NearMissPolicy policy) {
        if (contractReader.contracts(typeInfo).get(IMMUTABLE_TYPE) instanceof Value.Immutable contracted
            && contracted.isAtLeastImmutableHC()) {
            return null;
        }
        if (isEventual(typeInfo)) return null;
        Value.Immutable computed = typeInfo.analysis().getOrNull(IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        if (computed == null || computed.isAtLeastImmutableHC()) return null; // undecided, or already immutable

        List<FieldInfo> fields = typeInfo.fields();
        if (fields.size() < policy.minFields()) return null;
        List<Message> causes = new ArrayList<>();
        int blocking = 0;
        for (FieldInfo fieldInfo : fields) {
            int rule = immutableFieldFailingRule(fieldInfo);
            if (rule < 0) continue;
            if (++blocking > policy.maxBlockingSlots()) return null;
            Message blame = rule == 3 ? blameFieldDependent(fieldInfo) : null;
            String description = "field '" + fieldInfo.name() + "' " + immutableRuleFragment(rule, fieldInfo);
            causes.add(blame != null ? MessageImpl.cause(fieldInfo, description, blame)
                    : MessageImpl.cause(fieldInfo, description));
        }
        if (blocking == 0) return null;
        String message = typeInfo.fullyQualifiedName() + " would satisfy @Immutable but for " + blocking + " of its "
                         + fields.size() + " fields; consider @Immutable";
        return new RankedFinding(MessageImpl.warn(typeInfo, NEAR_MISS_IMMUTABLE, message, causes.toArray(new Message[0])),
                blocking, fields.size());
    }

    /** A short human fragment naming the immutability rule a field fails, for near-miss causes. */
    private static String immutableRuleFragment(int rule, FieldInfo fieldInfo) {
        return switch (rule) {
            case 0 -> "is assignable after construction (rule 0: all fields effectively final)";
            case 1 -> "is modified (rule 1: all fields @NotModified)";
            case 2 -> "is not private, and its type " + fieldInfo.type().detailedString()
                      + " is not immutable (rule 2)";
            default -> "is dependent: exposed to, or shared with, the outside world (rule 3)";
        };
    }

    /**
     * {@code @Independent} near-miss on a type: one field short of independence. Skipped when the user wrote
     * {@code @Independent} (the guard polices it) or the type is contracted/near {@code @Immutable} (that subsumes
     * independence — handled by {@code typeImmutabilityNearMisses}), and when the computed {@code INDEPENDENT_TYPE}
     * is undecided or already at-least-independent-HC. Fields only, exactly as {@code guardIndependentType}: an
     * abstract method's independence may be a {@code ShallowMethodAnalyzer} default rather than a computation.
     */
    private RankedFinding independentTypeNearMiss(TypeInfo typeInfo, IteratingAnalyzer.NearMissPolicy policy) {
        if (hasExplicitIndependentAnnotation(typeInfo)) return null;
        if (contractReader.contracts(typeInfo).get(IMMUTABLE_TYPE) instanceof Value.Immutable contracted
            && contracted.isAtLeastImmutableHC()) {
            return null;
        }
        Value.Independent computed = typeInfo.analysis().getOrNull(INDEPENDENT_TYPE, ValueImpl.IndependentImpl.class);
        if (computed == null || computed.isAtLeastIndependentHc()) return null; // undecided, or already independent

        List<FieldInfo> fields = typeInfo.fields();
        if (fields.size() < policy.minFields()) return null;
        List<Message> causes = new ArrayList<>();
        int blocking = 0;
        for (FieldInfo fieldInfo : fields) {
            Value.Independent independent = fieldInfo.analysis().getOrNull(INDEPENDENT_FIELD,
                    ValueImpl.IndependentImpl.class);
            if (independent == null || !independent.isDependent()) continue;
            if (++blocking > policy.maxBlockingSlots()) return null;
            Message blame = blameFieldDependent(fieldInfo);
            String description = "field '" + fieldInfo.name() + "' is dependent: external modifications can reach the"
                                 + " accessible content of the type";
            causes.add(blame != null ? MessageImpl.cause(fieldInfo, description, blame)
                    : MessageImpl.cause(fieldInfo, description));
        }
        if (blocking == 0) return null;
        String message = typeInfo.fullyQualifiedName() + " would satisfy @Independent but for " + blocking + " of its "
                         + fields.size() + " fields; consider @Independent";
        return new RankedFinding(MessageImpl.warn(typeInfo, NEAR_MISS_INDEPENDENT, message, causes.toArray(new Message[0])),
                blocking, fields.size());
    }

    private void guardType(TypeInfo typeInfo) {
        guardDynamicImmutableFields(typeInfo);
        Map<Property, Value> contracts = contractReader.contracts(typeInfo);
        if (contracts.get(CONTAINER_TYPE) instanceof Value.Bool container && container.isTrue()) {
            guardContainer(typeInfo);
        }
        if (contracts.get(IMMUTABLE_TYPE) instanceof Value.Immutable immutable && immutable.isAtLeastImmutableHC()) {
            if (!isEventual(typeInfo)) guardImmutable(typeInfo);
        } else if (hasExplicitIndependentAnnotation(typeInfo)
                   && contracts.get(INDEPENDENT_TYPE) instanceof Value.Independent ind
                   && ind.isAtLeastIndependentHc()) {
            // only in the absence of an @Immutable contract: that one covers independence as its rule 3, and
            // reporting the same field twice is noise.
            guardIndependentType(typeInfo);
        }
    }

    /**
     * Did the user actually write {@code @Independent} on this type? The contract map cannot answer that:
     * {@code AnnotationToProperty} synthesizes {@code INDEPENDENT_TYPE} for <em>every</em> type that has no
     * {@code @Independent} annotation (via {@code simpleComputeIndependent}), and that fallback returns INDEPENDENT
     * for an unannotated type whose non-private methods only speak primitives. Keying the guard off the map would
     * therefore report violations against a contract nobody wrote. {@code IMMUTABLE_TYPE} and {@code CONTAINER_TYPE}
     * need no such check: only a real annotation ever sets them.
     */
    private boolean hasExplicitIndependentAnnotation(TypeInfo typeInfo) {
        return typeInfo.annotations().stream()
                .anyMatch(ae -> Independent.class.getCanonicalName().equals(ae.typeInfo().fullyQualifiedName()));
    }

    /**
     * {@code @Independent} on a type (§080): external modifications must not be able to impact the accessible content.
     * A field whose computed independence is decided DEPENDENT is exactly that accessible content escaping, so it is
     * reported and named.
     * <p>
     * Fields only, deliberately. The abstract methods of the type, and their parameters, also carry independence
     * values, but for an abstract method those may come from {@code ShallowMethodAnalyzer}'s <em>defaults</em>
     * (`DEPENDENT_DEFAULT` when the return type is mutable and the type is not immutable) rather than from a
     * computation — the same "a default is not a decision" trap as {@code isPropertyFinal()}. Blaming on those would
     * report on incomplete information.
     */
    private void guardIndependentType(TypeInfo contractHolder) {
        Message contractLocation = MessageImpl.cause(contractHolder,
                "@Independent contracted on " + contractHolder.simpleName());
        for (FieldInfo fieldInfo : contractHolder.fields()) {
            Value.Independent independent = fieldInfo.analysis().getOrNull(INDEPENDENT_FIELD,
                    ValueImpl.IndependentImpl.class);
            if (independent != null && independent.isDependent()) {
                reportViolation(fieldInfo, blameFieldDependent(fieldInfo), contractLocation,
                        "field '" + fieldInfo.name() + "' of " + contractHolder.fullyQualifiedName()
                        + " is dependent: external modifications can reach the accessible content of the type,"
                        + " violating the @Independent contract on " + contractHolder.fullyQualifiedName());
            }
        }
    }

    /**
     * Eventual immutability ({@code after="..."}) is read as a contract, but not yet computed by the analyzer (road
     * to immutability, §060): the fields carrying the state transition are assignable, and the methods that effect
     * it are modifying, by design. The immutability rules only hold after the mark, which the analyzer cannot see,
     * so guarding such a type would report its own design as a violation. Once eventuality is computed, this is
     * where the guard starts checking those types instead of skipping them.
     */
    private boolean isEventual(TypeInfo typeInfo) {
        return contractReader.contracts(typeInfo)
                .get(EVENTUALLY_IMMUTABLE_TYPE) instanceof Value.EventuallyImmutable ev
               && ev.isEventual();
    }

    /**
     * {@code @Immutable} (or {@code @ImmutableContainer}) on a type: verify the rules of immutability one by one, so
     * the finding can name the rule and the member that breaks it. Mirrors {@code TypeImmutableAnalyzerImpl}, but
     * reads the <em>per-member</em> computed values rather than the type's own {@code IMMUTABLE_TYPE}: a contracted
     * type carries the trusted contract there (the analyzer early-returns rather than computing), exactly as
     * {@code guardContainer} reads parameters rather than {@code CONTAINER_TYPE}.
     * <p>
     * At most one finding per field: the rules are ordered, and a field that fails rule 0 usually fails others too.
     */
    private void guardImmutable(TypeInfo contractHolder) {
        Message contractLocation = MessageImpl.cause(contractHolder,
                "@Immutable contracted on " + contractHolder.simpleName());
        for (FieldInfo fieldInfo : contractHolder.fields()) {
            guardImmutableField(contractHolder, fieldInfo, contractLocation);
        }
        // rule 1 for abstract methods: they have no body, so no field of theirs can betray them (variant of rule 1,
        // §050). Concrete modifying methods need no separate check: to modify, they must assign or modify a field,
        // which rule 0 or rule 1 catches on that field.
        for (MethodInfo methodInfo : contractHolder.methods()) {
            if (methodInfo.isAbstract()) {
                Value.Bool nonModifying = methodInfo.analysis().getOrNull(NON_MODIFYING_METHOD,
                        ValueImpl.BoolImpl.class);
                if (nonModifying != null && nonModifying.isFalse()) {
                    reportViolation(methodInfo, null, contractLocation,
                            "abstract method " + methodInfo.fullyQualifiedName() + " is modifying, violating rule 1"
                            + " (all fields @NotModified) of the @Immutable contract on "
                            + contractHolder.fullyQualifiedName());
                }
            }
        }
    }

    private void guardImmutableField(TypeInfo contractHolder, FieldInfo fieldInfo, Message contractLocation) {
        int rule = immutableFieldFailingRule(fieldInfo);
        if (rule < 0) return;
        String prefix = "field '" + fieldInfo.name() + "' of " + contractHolder.fullyQualifiedName();
        String suffix = " of the @Immutable contract on " + contractHolder.fullyQualifiedName();
        String core = switch (rule) {
            case 0 -> " is assignable after construction, violating rule 0 (all fields effectively final)";
            case 1 -> " is modified, violating rule 1 (all fields @NotModified)";
            case 2 -> " is not private, and its type " + fieldInfo.type().detailedString()
                      + " is not immutable, violating rule 2 (fields private, or of immutable type)";
            default -> " is dependent: it is exposed to, or shared with, the outside world, violating rule 3"
                       + " (no parameter or return value dependent on the fields)";
        };
        Message blame = rule == 3 ? blameFieldDependent(fieldInfo) : null;
        reportViolation(fieldInfo, blame, contractLocation, prefix + core + suffix);
    }

    /**
     * The first immutability rule a field <em>decidedly</em> fails, or {@code -1} if it passes every decided rule.
     * Shared by {@code guardImmutableField} (contract violation) and {@code immutableNearMiss} (advisory). Reads
     * only decided values (never {@code isPropertyFinal()}, whose {@code getOrDefault} would read an undecided
     * field as "not final"), so an undecided rule counts as "passes here" — the guard's decided-only discipline.
     * <ul>
     *   <li>rule 0 — all fields effectively final;</li>
     *   <li>rule 1 — all fields {@code @NotModified};</li>
     *   <li>rule 2 — fields private, or of immutable type;</li>
     *   <li>rule 3 — nothing dependent on the accessible part of the fields.</li>
     * </ul>
     */
    private int immutableFieldFailingRule(FieldInfo fieldInfo) {
        if (!fieldInfo.isFinal()) {
            Value.Bool finalField = fieldInfo.analysis().getOrNull(FINAL_FIELD, ValueImpl.BoolImpl.class);
            if (finalField != null && finalField.isFalse()) return 0;
        }
        Value.Bool unmodified = fieldInfo.analysis().getOrNull(UNMODIFIED_FIELD, ValueImpl.BoolImpl.class);
        if (unmodified != null && unmodified.isFalse()) return 1;
        if (!fieldInfo.access().isPrivate() && isNotSelf(fieldInfo)) {
            Value.Immutable fieldTypeImmutable = analysisHelper.typeImmutableNullIfUndecided(fieldInfo.type());
            if (fieldTypeImmutable != null && !fieldTypeImmutable.isAtLeastImmutableHC()) return 2;
        }
        Value.Independent independent = fieldInfo.analysis().getOrNull(INDEPENDENT_FIELD,
                ValueImpl.IndependentImpl.class);
        if (independent != null && independent.isDependent()) return 3;
        return -1;
    }

    /** A field of the type it is declared in does not have to be immutable for rule 2 (cf. TypeImmutableAnalyzerImpl). */
    private static boolean isNotSelf(FieldInfo fieldInfo) {
        TypeInfo bestType = fieldInfo.type().bestTypeInfo();
        return bestType == null || !bestType.equals(fieldInfo.owner());
    }

    private void guardContainer(TypeInfo contractHolder) {
        contractHolder.constructorAndMethodStream()
                .filter(mi -> !mi.access().isPrivate())
                .forEach(mi -> {
                    if (mi.isAbstract()) {
                        Value.SetOfMethodInfo implementations = mi.analysis().getOrDefault(IMPLEMENTATIONS,
                                ValueImpl.SetOfMethodInfoImpl.EMPTY);
                        for (MethodInfo implementation : implementations.methodInfoSet()) {
                            checkParametersUnmodified(contractHolder, mi, implementation);
                        }
                    } else {
                        // the contracted type's own concrete (default, static) methods and constructors
                        checkParametersUnmodified(contractHolder, mi, mi);
                    }
                });
    }

    private void checkParametersUnmodified(TypeInfo contractHolder, MethodInfo declaration, MethodInfo target) {
        for (ParameterInfo pi : target.parameters()) {
            Value.Bool unmodified = pi.analysis().getOrNull(UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.class);
            if (unmodified != null && unmodified.isFalse()) {
                Message contractLocation = MessageImpl.cause(contractHolder,
                        "@Container contracted on " + contractHolder.simpleName());
                Message via = target == declaration ? contractLocation
                        : MessageImpl.cause(declaration, target.simpleName() + " implements "
                                                         + declaration.fullyQualifiedName()
                                                         + ", declared in the @Container type", contractLocation);
                // Phase 3 — the "why" chain: point at the statement that actually modifies the parameter.
                Message blame = blameParameterModified(target, pi);
                Message[] causes = blame == null ? new Message[]{via} : new Message[]{blame, via};
                analyzerMessages.add(MessageImpl.error(pi, CONTRACT_VIOLATION,
                        "parameter '" + pi.simpleName() + "' of " + target.fullyQualifiedName()
                        + " is modified, violating the @Container contract on "
                        + contractHolder.fullyQualifiedName(), causes));
            }
        }
    }

    /**
     * {@code @Immutable} on a FIELD is a promise about what the field <em>holds</em>, not about its declared type:
     * a {@code List<String>} field said to hold an immutable list. Since {@code DynamicImmutability} that promise
     * is load-bearing — it lifts the field, the accessor and often the whole type out of {@code @Dependent} — so
     * it needs checking, or a wrong annotation silently manufactures a verdict.
     * <p>
     * The check is <b>local</b>: every assignment to the field that this type itself makes (field initializer,
     * constructors, methods). That is enough to catch the flat lie — storing a caller's collection and calling it
     * immutable — and it is all that can be done without the inter-procedural walk that is part 2.
     *
     * <h2>The parameter case, and why it warns rather than accuses or stays silent</h2>
     * {@code this.items = items} with a parameter on the right is the shape that matters, and a local check cannot
     * settle it: {@code TypeInspectionImpl}'s contract is true (every caller passes {@code List.copyOf(...)} from
     * {@code Builder.commit()}) while {@code TestGuardDynamicImmutable}'s is false (the caller keeps its list), and
     * the two are syntactically identical. Reporting a violation would accuse correct code; staying silent would
     * let a now-load-bearing promise pass unremarked. So it is reported at lower severity, in its own category:
     * the user is told exactly which promise is being trusted rather than verified, and that the obligation has
     * moved to the callers. When part 2 lands and can follow the argument to the call site, this warning becomes
     * a real verdict.
     *
     * <h2>What counts as proof</h2>
     * The AAPI's own judgement ({@code IMMUTABLE_METHOD}), never a hand-written list of factory names. That keeps
     * one definition of "produces an immutable object" instead of two that can drift, and it is more accurate than
     * a name list would be: {@code List.copyOf} is annotated and genuinely copies, while
     * {@code Collections.unmodifiableList} is deliberately not — it returns a <em>view</em>, so a caller holding
     * the original can still change what the field sees, and treating it as proof would bless a false contract.
     */
    private void guardDynamicImmutableFields(TypeInfo typeInfo) {
        for (FieldInfo fieldInfo : typeInfo.fields()) {
            if (!(contractReader.contracts(fieldInfo).get(IMMUTABLE_FIELD) instanceof Value.Immutable contracted)
                || !contracted.isAtLeastImmutableHC()) {
                continue; // no dynamic-immutability promise on this field: nothing to check
            }
            Message contractLocation = MessageImpl.cause(fieldInfo,
                    "@Immutable contracted on field '" + fieldInfo.name() + "'");
            for (Assigned assigned : assignmentsTo(typeInfo, fieldInfo)) {
                switch (classify(assigned.value())) {
                    case MUTABLE -> analyzerMessages.add(MessageImpl.error(fieldInfo, CONTRACT_VIOLATION,
                            "field '" + fieldInfo.name() + "' is assigned a mutable object in "
                            + assigned.where() + ", violating its @Immutable contract",
                            assigned.cause(), contractLocation));
                    case UNKNOWN -> analyzerMessages.add(MessageImpl.warn(fieldInfo, CONTRACT_UNVERIFIABLE,
                            "the @Immutable contract on field '" + fieldInfo.name() + "' cannot be verified here:"
                            + " " + assigned.where() + " assigns a value whose immutability is not visible locally"
                            + " (every caller must pass an immutable object)",
                            assigned.cause(), contractLocation));
                    case IMMUTABLE -> {
                        // proven at this assignment; nothing to report
                    }
                }
            }
        }
    }

    /** One assignment to the guarded field, with where it was found, for the message's located cause. */
    private record Assigned(Expression value, String where, Message cause) {
    }

    private enum Proof {IMMUTABLE, MUTABLE, UNKNOWN}

    private List<Assigned> assignmentsTo(TypeInfo typeInfo, FieldInfo fieldInfo) {
        List<Assigned> result = new ArrayList<>();
        if (fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) {
            result.add(new Assigned(fieldInfo.initializer(), "its initializer",
                    MessageImpl.cause(fieldInfo, "assigned here")));
        }
        typeInfo.constructorAndMethodStream().forEach(mi -> {
            if (mi.methodBody().isEmpty()) return;
            mi.methodBody().visit(e -> {
                if (e instanceof Assignment asg && asg.variableTarget() instanceof FieldReference fr
                    && fr.scopeIsRecursivelyThis() && fr.fieldInfo() == fieldInfo) {
                    result.add(new Assigned(asg.value(), "'" + mi.name() + "'",
                            MessageImpl.cause(e.source(), mi, "assigned here")));
                }
                return true;
            });
        });
        return result;
    }

    /**
     * Can this right-hand side be shown, from here, to produce an immutable object? See
     * {@link #guardDynamicImmutableFields} for why {@code IMMUTABLE_METHOD} is the only accepted proof of a
     * factory call.
     */
    private Proof classify(Expression value) {
        if (value.isNullConstant()) return Proof.IMMUTABLE; // holds nothing, shares nothing
        Value.Immutable ofDeclaredType = analysisHelper.typeImmutable(value.parameterizedType());
        if (ofDeclaredType != null && ofDeclaredType.isAtLeastImmutableHC()) return Proof.IMMUTABLE;
        if (value instanceof MethodCall mc) {
            Value.Immutable dynamic = mc.methodInfo().analysis().getOrNull(IMMUTABLE_METHOD,
                    ValueImpl.ImmutableImpl.class);
            return dynamic != null && dynamic.isAtLeastImmutableHC() ? Proof.IMMUTABLE : Proof.UNKNOWN;
        }
        if (value instanceof ConstructorCall) {
            // `new ArrayList<>(x)` -- the declared type was already consulted above and is not immutable, and a
            // freshly constructed object of a mutable type is mutable. This is the one shape we can refute.
            return Proof.MUTABLE;
        }
        if (value instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
            && DynamicImmutability.ofField(fr.fieldInfo()) != null) {
            return Proof.IMMUTABLE; // another field carrying the same promise
        }
        return Proof.UNKNOWN;
    }

    private void guardMethod(MethodInfo methodInfo) {
        if (!methodInfo.isAbstract()) return;
        Map<Property, Value> contracts = contractReader.contracts(methodInfo);
        // @NotModified: no implementation may modify its receiver.
        if (contracts.get(NON_MODIFYING_METHOD) instanceof Value.Bool nonModifying && nonModifying.isTrue()) {
            for (MethodInfo impl : implementationsOf(methodInfo)) {
                Value.Bool implNonModifying = impl.analysis().getOrNull(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                if (implNonModifying != null && implNonModifying.isFalse()) {
                    reportViolation(impl, blameMethodModifying(impl),
                            MessageImpl.cause(methodInfo, "@NotModified contracted here"),
                            impl.fullyQualifiedName() + " is modifying, violating the @NotModified contract on "
                            + methodInfo.fullyQualifiedName());
                }
            }
        }
        // @Independent: no implementation may be dependent (expose or share mutable state).
        if (contracts.get(INDEPENDENT_METHOD) instanceof Value.Independent contractInd
            && contractInd.isAtLeastIndependentHc()) {
            for (MethodInfo impl : implementationsOf(methodInfo)) {
                Value.Independent implInd = impl.analysis().getOrNull(INDEPENDENT_METHOD, ValueImpl.IndependentImpl.class);
                if (implInd != null && implInd.isDependent()) {
                    reportViolation(impl, blameMethodDependent(impl),
                            MessageImpl.cause(methodInfo, "@Independent contracted here"),
                            impl.fullyQualifiedName() + " is dependent (it exposes or shares mutable state), "
                            + "violating the @Independent contract on " + methodInfo.fullyQualifiedName());
                }
            }
        }
    }

    /**
     * Hierarchy monotonicity: a concrete method may not weaken a contract its overridden method carries. Covers
     * the case the {@code IMPLEMENTATIONS} path (abstract → implementations) misses — a <em>concrete</em> parent
     * method contracted {@code @NotModified}/{@code @Independent}, overridden by a subclass method that modifies or
     * becomes dependent. The override's own value is genuinely computed (only the contracted parent is trusted).
     */
    private void guardOverride(MethodInfo methodInfo) {
        if (methodInfo.isAbstract()) return; // an override has a body
        for (MethodInfo parent : methodInfo.overrides()) {
            if (parent.isAbstract()) continue; // abstract parent → covered by guardMethod via IMPLEMENTATIONS
            if (parent.typeInfo().compilationUnit().externalLibrary()) continue; // don't police library contracts
            Map<Property, Value> contracts = contractReader.contracts(parent);
            if (contracts.get(NON_MODIFYING_METHOD) instanceof Value.Bool nm && nm.isTrue()
                && methodInfo.isModifying()) {
                reportViolation(methodInfo, blameMethodModifying(methodInfo),
                        MessageImpl.cause(parent, "@NotModified contracted here"),
                        methodInfo.fullyQualifiedName() + " overrides " + parent.fullyQualifiedName()
                        + " but is modifying, violating its @NotModified contract");
            }
            if (contracts.get(INDEPENDENT_METHOD) instanceof Value.Independent ind && ind.isAtLeastIndependentHc()) {
                Value.Independent miInd = methodInfo.analysis().getOrNull(INDEPENDENT_METHOD,
                        ValueImpl.IndependentImpl.class);
                if (miInd != null && miInd.isDependent()) {
                    reportViolation(methodInfo, blameMethodDependent(methodInfo),
                            MessageImpl.cause(parent, "@Independent contracted here"),
                            methodInfo.fullyQualifiedName() + " overrides " + parent.fullyQualifiedName()
                            + " but is dependent, violating its @Independent contract");
                }
            }
        }
    }

    private Iterable<MethodInfo> implementationsOf(MethodInfo abstractMethod) {
        return abstractMethod.analysis()
                .getOrDefault(IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY).methodInfoSet();
    }

    /** Emit a contract violation: {@code [ blame?, contract-provenance ]} as the why-chain. */
    private void reportViolation(Info blamed, Message blame, Message contractLocation, String message) {
        Message[] causes = blame == null ? new Message[]{contractLocation} : new Message[]{blame, contractLocation};
        analyzerMessages.add(MessageImpl.error(blamed, CONTRACT_VIOLATION, message, causes));
    }

    /**
     * Blame walk for a modified parameter: scan {@code target}'s body for the first statement that modifies
     * {@code pi}, and return it as a located cause ("... modifies 'message'"). Re-derives the evidence from the
     * CST reading only already-computed callee properties ({@code NON_MODIFYING_METHOD},
     * {@code UNMODIFIED_PARAMETER}) — the per-call link data that knew this directly is discarded after linking.
     * Returns {@code null} when no direct site is found (e.g. modification is indirect, through a field link or a
     * cycle); the violation is still reported, just without the deepest "where".
     */
    private Message blameParameterModified(MethodInfo target, ParameterInfo pi) {
        if (target.methodBody().isEmpty()) return null;
        AtomicReference<Message> found = new AtomicReference<>();
        target.methodBody().visit(e -> {
            if (found.get() != null) return false;
            if (e instanceof MethodCall mc) {
                // (1) receiver: pi.m(...) where m modifies its own object
                if (isVariable(mc.object(), pi) && isModifyingMethod(mc.methodInfo())) {
                    found.set(MessageImpl.cause(e.source(), target, "'" + pi.simpleName() + "."
                            + mc.methodInfo().name() + "(...)' modifies '" + pi.simpleName() + "'"));
                    return false;
                }
                // (2) argument: pi passed into a parameter slot that the callee modifies
                List<Expression> args = mc.parameterExpressions();
                List<ParameterInfo> params = mc.methodInfo().parameters();
                for (int k = 0; k < args.size() && k < params.size(); k++) {
                    if (isVariable(args.get(k), pi) && isModifiedParameter(params.get(k))) {
                        found.set(MessageImpl.cause(e.source(), target, "'" + pi.simpleName()
                                + "' is passed to '" + mc.methodInfo().name() + "', which modifies its parameter '"
                                + params.get(k).simpleName() + "'"));
                        return false;
                    }
                }
            } else if (e instanceof Assignment asg && pi.equals(rootOf(asg.variableTarget()))) {
                // (3) pi.field = ... or pi[i] = ...
                found.set(MessageImpl.cause(e.source(), target,
                        "a field or element of '" + pi.simpleName() + "' is assigned"));
                return false;
            }
            return true;
        });
        return found.get();
    }

    /**
     * Blame walk for a dependent field: <em>why</em> can the outside world reach this field's object graph? The
     * answer is already computed — it is the field's {@code LINKS} (written by {@code FieldAnalyzerImpl}, phase 2),
     * from which that same analyzer derives {@code INDEPENDENT_FIELD}. This mirrors its {@code computeIndependent}
     * loop and reports the link that drags the field down to DEPENDENT: a link to a parameter of a non-private
     * method (the caller kept a reference to what it passed in), or to a return value of one (the field is handed
     * out).
     * <p>
     * Reading the links rather than scanning the CST for {@code this.f = p} is what makes this correct: linking
     * computes <em>exact</em> assignments, so it sees through {@code this.x = Objects.requireNonNull(x)}, through a
     * local variable, through {@code list.subList(..)} — every shape a syntactic match would miss or misread.
     * <p>
     * {@code null} when the links are undecided, or when no single link explains it.
     */
    private Message blameFieldDependent(FieldInfo fieldInfo) {
        Links links = fieldInfo.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
        if (links == null) return null;
        TypeInfo owner = fieldInfo.owner();
        Value.Independent independentOfType = analysisHelper.typeIndependentFromImmutableOrNull(owner,
                fieldInfo.type());
        if (independentOfType == null) return null;
        for (Link link : links) {
            Value.Independent toIndependent = independenceOfLink(link, links, independentOfType);
            if (toIndependent == null || !toIndependent.isDependent()) continue;
            if (link.to() instanceof ParameterInfo pi && !pi.methodInfo().access().isPrivate()
                && owner.inHierarchyOf(pi.typeInfo())) {
                return MessageImpl.cause(pi, "the field is linked to parameter '" + pi.simpleName() + "' of "
                                             + describe(pi.methodInfo()) + ": the caller keeps a reference to it");
            }
            if (link.to() instanceof ReturnVariable rv && !rv.methodInfo().access().isPrivate()
                && owner.inHierarchyOf(rv.methodInfo().typeInfo())) {
                return MessageImpl.cause(rv.methodInfo(), "the field is linked to the return value of "
                                                          + describe(rv.methodInfo())
                                                          + ": it is handed to the caller");
            }
        }
        return null;
    }

    /** A constructor's {@code name()} is {@code <init>}; name it after its type instead. */
    private static String describe(MethodInfo methodInfo) {
        return methodInfo.isConstructor()
                ? "the constructor of '" + methodInfo.typeInfo().simpleName() + "'"
                : "method '" + methodInfo.name() + "'";
    }

    /** The independence a single link contributes, exactly as {@code FieldAnalyzerImpl.computeIndependent} folds it. */
    private Value.Independent independenceOfLink(Link link, Links links, Value.Independent independentOfType) {
        if (link.from().equals(links.primary()) && link.linkNature().isIdenticalToOrAssignedFromTo()) {
            return independentOfType;
        }
        if (!link.linkNature().isDecoration()) {
            return analysisHelper.typeIndependentFromImmutableOrNull(link.to().parameterizedType());
        }
        return null;
    }

    /**
     * Blame walk for a dependent method: which field does the return value expose? Read from the method's computed
     * {@code METHOD_LINKS} — {@code TypeModIndyAnalyzerImpl.doIndependentMethod} decides independence as
     * {@code worstLinkToFields(mlv.ofReturnValue())}, so the link that makes that fold return DEPENDENT is, exactly,
     * the reason. {@code LinkToField} holds the shared definition of "reaches a mutable field of the instance".
     * <p>
     * Reading the links rather than scanning for {@code return <field>} matters for the same reason it does on
     * fields: linking follows the object, not the syntax, so {@code return wrap(data)}, a return through a local, or
     * a field handed out via a builder are all named — none of which a syntactic match would see.
     * <p>
     * {@code null} when the links are undecided or nothing in them points at a mutable field of this.
     */
    private Message blameMethodDependent(MethodInfo impl) {
        MethodLinkedVariables mlv = impl.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        if (mlv == null) return null;
        FieldReference fr = LinkToField.firstDependentLinkToField(mlv.ofReturnValue(), analysisHelper);
        if (fr == null) return null;
        return MessageImpl.cause(impl, "its return value is linked to the field '" + fr.fieldInfo().name()
                                       + "', which is mutable: the caller can reach the type's own state through it");
    }

    /**
     * Blame walk for a modifying method: the first self-modification in {@code impl}'s body — a write to a field
     * of the instance ({@code count++}), or a modifying call on {@code this} or one of its fields. Located cause;
     * {@code null} when nothing direct is found.
     */
    private Message blameMethodModifying(MethodInfo impl) {
        if (impl.methodBody().isEmpty()) return null;
        AtomicReference<Message> found = new AtomicReference<>();
        impl.methodBody().visit(e -> {
            if (found.get() != null) return false;
            if (e instanceof Assignment asg && asg.variableTarget() instanceof FieldReference fr
                && fr.scopeIsRecursivelyThis()) {
                found.set(MessageImpl.cause(e.source(), impl, "assigns field '" + fr.fieldInfo().name() + "'"));
                return false;
            }
            if (e instanceof MethodCall mc && isModifyingMethod(mc.methodInfo())
                && (mc.object() == null || mc.object() instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference frv && frv.scopeIsRecursivelyThis())) {
                found.set(MessageImpl.cause(e.source(), impl,
                        "calls modifying method '" + mc.methodInfo().name() + "' on the instance"));
                return false;
            }
            return true;
        });
        return found.get();
    }

    private static boolean isVariable(Expression e, Variable v) {
        return e instanceof VariableExpression ve && v.equals(ve.variable());
    }

    // the analyser's own accessors — the same signal that decided the parameter modified — so the blame points at
    // exactly the call the analyser "saw" modifying it, for shallow (JDK) callees as well as source ones.
    private boolean isModifyingMethod(MethodInfo mi) {
        return mi.isModifying();
    }

    private boolean isModifiedParameter(ParameterInfo p) {
        return p.isModified();
    }

    /** The variable a target is rooted at: {@code pi.f} / {@code pi[i]} (possibly nested) → {@code pi}. */
    private static Variable rootOf(Variable v) {
        if (v instanceof FieldReference fr) return fr.fieldReferenceBase();
        if (v instanceof DependentVariable dv) return dv.arrayVariableBase();
        return v;
    }
}
