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
import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.MessageImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;

/**
 * First iteration of the guard: verifies the two contracts most central to the container story.
 * <ul>
 *   <li>{@code @Container} on a type: no non-private method of the type, nor any implementation of its
 *       abstract methods, may modify a parameter. The contract binds implementations only through the
 *       methods they inherit from the contracted type (a subtype may add modifying methods of its own).</li>
 *   <li>{@code @NotModified} on an abstract method: no implementation may be modifying.</li>
 * </ul>
 * A violation is only reported on a decided FALSE — an undecided value (cycle, external code) stays silent,
 * so the guard never reports on incomplete information.
 */
public class GuardAnalyzerImpl extends CommonAnalyzerImpl implements GuardAnalyzer {
    public static final String CONTRACT_VIOLATION = "contract-violation";

    private final ContractReader contractReader;

    public GuardAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration, List<Message> messages) {
        super(configuration, null, messages);
        this.contractReader = new ContractReader(runtime);
    }

    @Override
    public void go(List<Info> analysisOrder) {
        for (Info info : analysisOrder) {
            if (info instanceof TypeInfo typeInfo) {
                guardType(typeInfo);
            } else if (info instanceof MethodInfo methodInfo) {
                guardMethod(methodInfo);
            }
        }
    }

    private void guardType(TypeInfo typeInfo) {
        Map<Property, Value> contracts = contractReader.contracts(typeInfo);
        if (contracts.get(CONTAINER_TYPE) instanceof Value.Bool container && container.isTrue()) {
            guardContainer(typeInfo);
        }
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
                    reportViolation(impl, null,
                            MessageImpl.cause(methodInfo, "@Independent contracted here"),
                            impl.fullyQualifiedName() + " is dependent (it exposes or shares mutable state), "
                            + "violating the @Independent contract on " + methodInfo.fullyQualifiedName());
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
