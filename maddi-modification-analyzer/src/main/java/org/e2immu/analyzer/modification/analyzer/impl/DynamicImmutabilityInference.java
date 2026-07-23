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

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Infers what a field HOLDS, as opposed to what its declared type says it may hold — the producer for
 * {@link DynamicImmutability}, which consumes it, and for {@code SourceContractMaterializer}, which until now was
 * the only thing that could supply it.
 * <p>
 * The shape this exists for:
 * <pre>
 * TypeInspectionImpl.Builder.commit():  new TypeInspectionImpl(..., List.copyOf(fields), ...);
 * the product constructor:              this.fields = fields;
 * </pre>
 * A "field assigned {@code List.copyOf(...)}" detector fires on nothing here: the copy happens at the CALL SITE.
 * Closing it means carrying immutability from a call-site argument, through a constructor parameter, to a field.
 *
 * <h2>Soundness, which is the whole design</h2>
 * Two enumerations must be COMPLETE or the conclusion is false rather than merely imprecise. Each has its own
 * gate, and where a gate cannot be met we decline to conclude:
 * <ul>
 *   <li><b>Assignments to the field</b> — gated on {@link FieldInfo#isPropertyFinal()}. Effective finality is
 *       computed by prepwork's {@code ComputePartOfConstructionFinalField} from the CALL GRAPH, whose vertices
 *       include the methods of anonymous and local types; it is set false as soon as the field is assigned
 *       anywhere outside the construction of its own static type. So a final field's assignments all lie in
 *       construction, and the whole-primary-type walk below sees them. Leaning on that computation is what lets
 *       this class avoid re-deriving a complete assignment set of its own — and what stops it silently missing
 *       the enclosing-type or sibling-nested-type assignment that
 *       {@code ComputePartOfConstructionFinalField} documents (fernflower's {@code ClassesProcessor}).</li>
 *   <li><b>Callers of the method owning a parameter</b> — gated on {@code private}. Every caller of a private
 *       member is inside the same primary type, which is exactly the scope walked here (and exactly
 *       {@code ComputeCallGraph}'s scope). Deliberately NOT "non-public": package-private callers can live in
 *       another primary type of the same package and would escape the walk entirely.</li>
 * </ul>
 *
 * <h2>Meet, and why silence is not a verdict</h2>
 * A field's value is the MEET over every assignment; a parameter's over every call site's argument. One
 * unproven contributor and nothing is written. Since properties are write-once and absence means "undecided,
 * revisited next pass", declining to write is how this participates in the fixpoint: an input that is merely
 * not decided YET costs an iteration, never a wrong answer. There is no optimistic default anywhere in here —
 * that is the defect {@code docs/independent-type-optimism.md} records, and re-introducing it in a new place
 * would be worse than not having the feature.
 *
 * <h2>What counts as proof</h2>
 * The AAPI's own {@code IMMUTABLE_METHOD}, exactly as {@code GuardAnalyzerImpl} uses it — never a hand-written
 * list of factory names. {@code List.copyOf} carries {@code @ImmutableContainer(hc=true)} and genuinely copies;
 * {@code Collections.unmodifiableList} deliberately does not, because it returns a <em>view</em> whose backing
 * list a caller can still change. One definition, in one place, so the producer and the guard cannot drift.
 */
public class DynamicImmutabilityInference {

    /**
     * A parameter's value can depend on another parameter's ({@code this(...)} delegation, a private factory
     * calling a private constructor). Recursion is bounded rather than tracked: these chains are short, and a
     * cycle must yield "unproven", which is what running out of depth produces.
     */
    private static final int MAX_DEPTH = 5;

    // AnalysisHelper holds no state, so one instance serves the static classification path that GuardAnalyzerImpl
    // shares (see provenImmutability): producer and guard must agree on what counts as proof, or the guard would
    // warn about a promise the inference is happy to make.
    private static final AnalysisHelper ANALYSIS_HELPER = new AnalysisHelper();

    private final AtomicInteger propertyChanges;

    public DynamicImmutabilityInference(AtomicInteger propertyChanges) {
        this.propertyChanges = propertyChanges;
    }

    public void infer(FieldInfo fieldInfo) {
        if (fieldInfo.analysis().haveAnalyzedValueFor(PropertyImpl.IMMUTABLE_FIELD)) {
            return; // contracted (SourceContractMaterializer) or inferred in an earlier pass; both win over this
        }
        // Only a field whose declared type leaves something to say. A field already immutable by declared type
        // needs no dynamic value, and DynamicImmutability would ignore anything below immutable-HC anyway.
        Value.Immutable ofDeclaredType = ANALYSIS_HELPER.typeImmutable(fieldInfo.type());
        if (ofDeclaredType != null && ofDeclaredType.isAtLeastImmutableHC()) return;

        if (!fieldInfo.isPropertyFinal()) return; // see "Soundness": the assignment set would be incomplete

        List<Expression> assigned = assignedValues(fieldInfo);
        if (assigned.isEmpty()) return; // never assigned here: nothing to conclude, and nothing to gain
        Value.Immutable meet = ValueImpl.ImmutableImpl.IMMUTABLE;
        for (Expression value : assigned) {
            Value.Immutable proven = immutabilityOf(value, 0);
            if (proven == null) return; // unproven or not yet decided: decline, and try again next pass
            meet = meet.min(proven);
        }
        if (meet.isAtLeastImmutableHC()) {
            fieldInfo.analysis().set(PropertyImpl.IMMUTABLE_FIELD, meet);
            CommonAnalyzerImpl.DECIDE.debug("DII: Decide dynamic immutable of field {} = {}", fieldInfo, meet);
            propertyChanges.incrementAndGet();
        }
    }

    /**
     * Every assignment to the field anywhere in its primary type — deliberately NOT filtered on
     * {@code scopeIsRecursivelyThis()}, unlike {@code GuardAnalyzerImpl.assignmentsTo}. The guard reports what it
     * can see and may under-report; an inference that misses an assignment concludes something false. Java lets
     * an enclosing or sibling nested type assign a nested type's private field, so those methods must be walked
     * too even though the reference is not {@code this.f}.
     */
    /**
     * What {@code value} can be shown to produce, for a caller that only wants the judgement and writes nothing —
     * {@code GuardAnalyzerImpl}, which must not warn "cannot be verified here" about an assignment this class
     * would happily prove.
     */
    public static Value.Immutable provenImmutability(Expression value) {
        return immutabilityOf(value, 0);
    }

    private static List<Expression> assignedValues(FieldInfo fieldInfo) {
        List<Expression> result = new ArrayList<>();
        if (fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) {
            result.add(fieldInfo.initializer());
        }
        forEachMethodOfPrimaryType(fieldInfo.owner(), mi -> mi.methodBody().visit(e -> {
            if (e instanceof Assignment asg && asg.variableTarget() instanceof FieldReference fr
                && fr.fieldInfo() == fieldInfo) {
                result.add(asg.value());
            }
            return true;
        }));
        return result;
    }

    /**
     * What this expression provably produces, or {@code null} when it cannot be shown from here — which covers
     * both "provably not immutable" and "not decided yet". The two need no distinction: the caller declines
     * either way, and a value that is merely undecided will be decided in a later pass.
     */
    private static Value.Immutable immutabilityOf(Expression value, int depth) {
        if (value.isNullConstant()) return ValueImpl.ImmutableImpl.IMMUTABLE; // holds nothing, shares nothing
        Value.Immutable ofDeclaredType = ANALYSIS_HELPER.typeImmutable(value.parameterizedType());
        if (ofDeclaredType != null && ofDeclaredType.isAtLeastImmutableHC()) return ofDeclaredType;
        if (value instanceof MethodCall mc) return ofMethodCall(mc);
        if (value instanceof VariableExpression ve) {
            if (ve.variable() instanceof FieldReference fr) {
                return DynamicImmutability.ofField(fr.fieldInfo()); // already carries the promise
            }
            if (ve.variable() instanceof ParameterInfo pi) {
                return immutabilityOfParameter(pi, depth);
            }
        }
        // ConstructorCall of a mutable type, a local variable, a ternary, ...: not provable from here.
        return null;
    }

    /**
     * The meet over every call site's argument in this parameter's slot — the inter-procedural step, and the
     * only one that can reach {@code TypeInspectionImpl}'s fields.
     * <p>
     * Refuses unless the owning method is {@code private}, so that the walk below is provably the complete set
     * of callers. Refuses on varargs, where argument position does not correspond to parameter position. Refuses
     * when there are no callers at all: the meet over an empty set is vacuously immutable, which is sound but
     * would let dead code manufacture a verdict, and a private member nobody calls is a smell rather than an
     * input worth trusting.
     */
    private static Value.Immutable immutabilityOfParameter(ParameterInfo pi, int depth) {
        if (depth >= MAX_DEPTH) return null;
        MethodInfo owner = pi.methodInfo();
        if (!owner.access().isPrivate()) return null;
        if (pi.isVarArgs()) return null;

        List<Expression> arguments = new ArrayList<>();
        forEachMethodOfPrimaryType(owner.typeInfo(), mi -> mi.methodBody().visit(e -> {
            if (e instanceof ConstructorCall cc && cc.constructor() == owner) {
                collectArgument(cc.parameterExpressions(), pi, arguments);
            } else if (e instanceof MethodCall mc && mc.methodInfo() == owner) {
                collectArgument(mc.parameterExpressions(), pi, arguments);
            }
            return true;
        }));
        if (arguments.isEmpty()) return null;

        Value.Immutable meet = ValueImpl.ImmutableImpl.IMMUTABLE;
        for (Expression argument : arguments) {
            Value.Immutable proven = immutabilityOf(argument, depth + 1);
            if (proven == null) return null;
            meet = meet.min(proven);
        }
        return atLeastHcOrNull(meet);
    }

    private static void collectArgument(List<Expression> parameterExpressions, ParameterInfo pi,
                                        List<Expression> into) {
        if (pi.index() < parameterExpressions.size()) {
            into.add(parameterExpressions.get(pi.index()));
        }
    }

    /**
     * Every constructor and method of the primary type this type belongs to, including those of its nested
     * types. Both enumerations here need the same scope: it is the set of places that may touch a private
     * member, and — given the finality gate — the set of places that may assign a final field.
     */
    private static void forEachMethodOfPrimaryType(TypeInfo typeInfo, java.util.function.Consumer<MethodInfo> action) {
        typeInfo.primaryType().recursiveSubTypeStream()
                .flatMap(TypeInfo::constructorAndMethodStream)
                .filter(mi -> !mi.methodBody().isEmpty())
                .forEach(action);
    }

    /**
     * What a call provably produces. {@code IMMUTABLE_METHOD} alone is NOT the answer, and assuming it was is a
     * false-positive factory: for a method the analyzer has no source of, {@code ShallowMethodAnalyzer
     * .computeMethodImmutable} fills that property in from the DECLARED RETURN TYPE. {@code Objects.requireNonNull}
     * returns {@code T}, an unbound type parameter, which is immutable-HC as a type — so the property reads
     * "immutable-HC" for a method that hands back the very object it was given, and
     * {@code this.set = Objects.requireNonNull(set)} would be "proven" to store an immutable set while the caller
     * still holds it. ({@code TestGuardIndependentType} catches exactly that.)
     * <p>
     * So two further conditions, both about telling a real claim from the fallback:
     * <ul>
     *   <li>the declared return type must NOT itself be immutable-HC. When it is, the property is merely echoing
     *       the type and carries no dynamic information; when the declared type is mutable ({@code List<E>}) and
     *       the property still says immutable-HC, only an annotation can have put it there — which is precisely
     *       the {@code @ImmutableContainer(hc=true)} on {@code List.copyOf}, i.e. a genuine promise of a fresh
     *       immutable object.</li>
     *   <li>the method must not be {@code @Identity}: such a method returns one of its arguments by definition,
     *       so whatever it returns is exactly as shared as what it was passed.</li>
     * </ul>
     * Sharing an immutable object is harmless — independence is about reachable mutable content — so a method
     * that returns a cached immutable instance rather than a fresh one is still fine here.
     */
    private static Value.Immutable ofMethodCall(MethodCall mc) {
        MethodInfo methodInfo = mc.methodInfo();
        Value.Bool identity = methodInfo.analysis().getOrNull(PropertyImpl.IDENTITY_METHOD, ValueImpl.BoolImpl.class);
        if (identity != null && identity.isTrue()) return null;
        Value.Immutable ofReturnType = ANALYSIS_HELPER.typeImmutable(methodInfo.returnType());
        if (ofReturnType != null && ofReturnType.isAtLeastImmutableHC()) return null; // the fallback, not a claim
        return atLeastHcOrNull(methodInfo.analysis()
                .getOrNull(PropertyImpl.IMMUTABLE_METHOD, ValueImpl.ImmutableImpl.class));
    }

    /** Below immutable-HC says nothing about sharing, so it is not a result; mirrors {@link DynamicImmutability}. */
    private static Value.Immutable atLeastHcOrNull(Value.Immutable immutable) {
        return immutable != null && immutable.isAtLeastImmutableHC() ? immutable : null;
    }
}
