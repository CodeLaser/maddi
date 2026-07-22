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

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Computes {@link PropertyImpl#STATIC_SIDE_EFFECTS_METHOD}: a method has a static side effect when it modifies
 * static/global state that belongs to a type <em>other</em> than its own primary type (road-to-immutability
 * §050, "Static side effects"). It flags three shapes: an assignment to another type's static field; a
 * modifying call on another type's static field ({@code Other.COUNTER.incrementAndGet()}); and a call to a
 * method that itself has a static side effect. The third shape is the <b>callee-annotation</b> half: a static
 * <em>method</em> call that reconfigures global state, e.g. {@code System.setOut(other)}, whose effect is not
 * visible from JDK source, is recognised through a contracted {@code @StaticSideEffects} on the library's
 * safe surface (an AAPI declaration; see {@code maddi-aapi-archive .../jdk/JavaLang.java}) and propagates to
 * its callers, transitively.
 * <p>
 * It is informational — it does not cap immutability (which inspects only the type's own fields) — and it is the
 * <b>global-escape arm</b> of the {@code @IgnoreModifications} confinement guard: a modification reached through
 * the ignored stratum that is a static side effect has left it.
 * <p>
 * Gated on env {@code SSE} until a broader corpus A/B clears it. Additive: it writes only its own property and
 * changes no existing verdict.
 */
public class StaticSideEffectAnalyzerImpl {
    // non-final so a test can flip it; the env is the production default
    public static boolean ENABLED = System.getenv("SSE") != null;

    private final AtomicInteger propertyChanges;

    public StaticSideEffectAnalyzerImpl(AtomicInteger propertyChanges) {
        this.propertyChanges = propertyChanges;
    }

    public void go(TypeInfo typeInfo) {
        if (!ENABLED) return;
        TypeInfo primaryType = typeInfo.primaryType();
        typeInfo.constructorAndMethodStream().forEach(methodInfo -> {
            if (methodInfo.analysis().haveAnalyzedValueFor(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD)) return;
            if (methodInfo.isAbstract() || methodInfo.methodBody().isEmpty()) return;
            Boolean sse = computeStaticSideEffect(primaryType, methodInfo);
            if (sse != null && TolerantWrite.setAllowControlledOverwrite(methodInfo.analysis(),
                    PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.from(sse), methodInfo)) {
                propertyChanges.incrementAndGet();
            }
        });
    }

    /** TRUE = has a static side effect; FALSE = proven none; null = a callee's modification is still undecided. */
    private Boolean computeStaticSideEffect(TypeInfo primaryType, MethodInfo methodInfo) {
        boolean[] sse = {false};
        boolean[] undecided = {false};
        methodInfo.methodBody().visit(e -> {
            if (sse[0]) return false;
            // assignment to another type's static field: this.OtherType.STATIC = ...
            if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                && fr.fieldInfo().isStatic() && escapes(primaryType, methodInfo, fr)) {
                sse[0] = true;
                return false;
            }
            // modifying call on another type's static field: Other.STATIC.add(...), System.out.println(...)
            if (e instanceof MethodCall mc && mc.object() instanceof VariableExpression ve
                && ve.variable() instanceof FieldReference fr
                && fr.fieldInfo().isStatic() && escapes(primaryType, methodInfo, fr)) {
                Value.Bool nonModifying = mc.methodInfo().analysis()
                        .getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
                if (nonModifying == null) undecided[0] = true;
                else if (nonModifying.isFalse()) {
                    sse[0] = true;
                    return false;
                }
            }
            // a call to a method that itself has a static side effect: the effect propagates to the caller. This
            // is the callee-annotation half of the arm -- it is how an AAPI-contracted @StaticSideEffects method
            // whose global effect is invisible from source (System.setOut(other) replacing System.out) makes its
            // caller a static-side-effect method too. Works transitively for source callees as well.
            if (e instanceof MethodCall mc) {
                switch (calleeStaticSideEffect(mc.methodInfo())) {
                    case TRUE -> { sse[0] = true; return false; }
                    case UNDECIDED -> undecided[0] = true;
                    case FALSE -> { /* no escape via this call */ }
                }
            }
            return true;
        });
        if (sse[0]) return Boolean.TRUE;
        return undecided[0] ? null : Boolean.FALSE;
    }

    private enum CalleeSse {TRUE, FALSE, UNDECIDED}

    /**
     * The static-side-effect verdict of a callee, for propagation to the caller. A contracted callee (an AAPI
     * safe-surface method such as {@code System.setOut}, or a source method the author asserted) reads TRUE
     * immediately. Absence is ambiguous, so it is resolved structurally: a source callee (a body we will compute)
     * whose value is not in yet is UNDECIDED — wait for it, exactly as the modifying-call case waits on
     * {@code NON_MODIFYING_METHOD}; a shallow/abstract callee (no body, no contract) can only be a static side
     * effect by annotation, so its absence is a decided FALSE and never stalls the caller.
     */
    private static CalleeSse calleeStaticSideEffect(MethodInfo callee) {
        Value.Bool sse = callee.analysis().getOrNull(PropertyImpl.STATIC_SIDE_EFFECTS_METHOD, ValueImpl.BoolImpl.class);
        if (sse != null) return sse.isTrue() ? CalleeSse.TRUE : CalleeSse.FALSE;
        boolean computable = !callee.isAbstract() && !callee.methodBody().isEmpty();
        return computable ? CalleeSse.UNDECIDED : CalleeSse.FALSE;
    }

    /**
     * Does a modification of this static field escape the primary type's own state? A static field of the
     * primary type (or a nested type of it, same primary type) is the type's own static state — handled by the
     * static-field / {@code @IgnoreModifications} rules, not a side effect. A static field inherited from a
     * parent is likewise excused (road §050). Only another type's static field escapes.
     */
    private static boolean escapes(TypeInfo primaryType, MethodInfo methodInfo, FieldReference fr) {
        TypeInfo owner = fr.fieldInfo().owner();
        if (owner.primaryType() == primaryType) return false;
        return methodInfo.typeInfo().superTypesExcludingJavaLangObject().stream().noneMatch(st -> st == owner);
    }
}
