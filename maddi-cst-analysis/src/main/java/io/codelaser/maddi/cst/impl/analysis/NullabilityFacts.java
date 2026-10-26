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
package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

/**
 * What a nullability analysis knows about the program, for a printer that writes a null-checked language (Kotlin's
 * {@code ?}, C#'s nullable reference types): a declaration's type with its {@link NullableState}s decided, the
 * verdicts of local variables, and use-site facts. The printer asks; the analysis, which the printer does not run
 * itself, answers. maddi-mod's NullabilityPass computes these ({@code Report}); {@link #STORED} answers from what
 * the analyzer stored on the declarations, which is nothing when no analysis ran.
 * <p>
 * A declaration verdict replaces the declared type when it is not null, type arguments and array components
 * included ({@code List<String?>}); null means "no verdict", and the printer uses the declared type as it is.
 * Every answer defaults to "unknown", so that a client supplies only what it has.
 */
public interface NullabilityFacts {

    /** No facts: every type as declared, nothing known at a use site. */
    NullabilityFacts NONE = new NullabilityFacts() {
    };

    /**
     * The verdicts the analysis stored on the declarations: {@code NULLABILITY_FIELD}, {@code _PARAMETER} and
     * {@code _METHOD}, which maddi-mod's analyzer writes when it runs with nullability. Local variables are not
     * {@link Info}s and have no property: a client with the analysis's report supplies them.
     */
    NullabilityFacts STORED = new NullabilityFacts() {
        @Override
        public ParameterizedType field(FieldInfo fieldInfo) {
            return stored(fieldInfo, PropertyImpl.NULLABILITY_FIELD, fieldInfo.type());
        }

        @Override
        public ParameterizedType parameter(ParameterInfo parameterInfo) {
            return stored(parameterInfo, PropertyImpl.NULLABILITY_PARAMETER, parameterInfo.parameterizedType());
        }

        @Override
        public ParameterizedType returnType(MethodInfo methodInfo) {
            return stored(methodInfo, PropertyImpl.NULLABILITY_METHOD, methodInfo.returnType());
        }

        private static ParameterizedType stored(Info info, Property property, ParameterizedType declared) {
            Value.Nullability n = info.analysis().getOrDefault(property, ValueImpl.NullabilityImpl.UNSPECIFIED);
            return n.isDefault() ? null : n.applyTo(declared);
        }
    };

    /** {@code NULLABLE}: the field may hold null; its type, with the verdict's states. Null: no verdict. */
    default ParameterizedType field(FieldInfo fieldInfo) {
        return null;
    }

    default ParameterizedType parameter(ParameterInfo parameterInfo) {
        return null;
    }

    /** The method may return null: its return type, with the verdict's states. Null: no verdict. */
    default ParameterizedType returnType(MethodInfo methodInfo) {
        return null;
    }

    /**
     * A parameter the body assigns: the verdict of the values the body assigns it, the type of the Kotlin printer's
     * {@code var p = p}. The parameter's own verdict ({@link #parameter}) is the caller's value only. Null when the
     * parameter is never assigned, or not known.
     */
    default ParameterizedType reassignedParameter(ParameterInfo parameterInfo) {
        return null;
    }

    /**
     * A local variable, identified by the element that declares it (a LocalVariableCreation, ForEachStatement or
     * catch clause) in {@code method}: a local's equality is its name, which does not tell same-named locals apart.
     * {@link NullableState#UNSPECIFIED} on a reference type is a verdict that is no decision (a degraded method's
     * unreached local): see {@link #decided}.
     */
    default ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
        return null;
    }

    /**
     * The local is non-null from its declaration on although its initializer may be null: Java dereferences it
     * unconditionally later in the same block, with no reassignment in between ({@code BasicBlock block =
     * blocks.getWithKey(j); protectedRange.add(block); block.addSuccessorException(h);}). The printer asserts the
     * initializer ({@code blocks.getWithKey(j)!!}, {@code blocks.GetWithKey(j)!}), so the NullPointerException moves
     * to the declaration, before what Java did with the null in between; {@link #local} answers non-null for it, and
     * its null flows no further.
     */
    default boolean assertedAtDeclaration(MethodInfo method, Element declaration, LocalVariable variable) {
        return false;
    }

    /**
     * For a local {@link #assertedAtDeclaration asserted at its declaration}: proven, by the linking engine, that no
     * statement between the declaration and the first dereference refers to the local, directly or through a variable
     * linked to it. The earlier NullPointerException then changes nothing observable but which line throws, and the
     * printer reports it as INFO rather than as a behaviour change. False without that proof.
     */
    default boolean unobservedBeforeDereference(MethodInfo method, Element declaration, LocalVariable variable) {
        return false;
    }

    /**
     * The local variable, parameter or field is known non-null when {@code statement} starts: after
     * {@code if (v == null) return;}, inside {@code if (v != null)}, after {@code v.m()}. Kotlin smart-casts it
     * there, and C#'s flow analysis knows it, so a nullable {@code v} needs no {@code !!} or {@code !}. (Java
     * requires a local a lambda captures to be effectively final, so the smart cast cannot be lost to a lambda.)
     * <p>
     * ⚠ Only facts the target's own flow analysis follows: NullabilityPass's {@code Report.smartCasts()}, not
     * {@code Report.useSites()}, which also counts {@code Objects.requireNonNull(v)}, {@code assert v != null} and
     * a non-null Java contract, after which Kotlin still needs the {@code !!}.
     */
    default boolean nonNullAt(Statement statement, Variable variable) {
        return false;
    }

    /**
     * As {@link #nonNullAt(Statement, Variable)}, at a method or constructor call or a field access: also what the
     * enclosing condition establishes ({@code v.m()} in {@code v != null && v.m()}, {@code f(w)} in
     * {@code w == null ? 0 : f(w)}). For a field: a final one of {@code this}, which Kotlin smart-casts as a val.
     */
    default boolean nonNullAt(Expression expression, Variable variable) {
        return false;
    }

    /** The callee may return null ({@link #returnType}), but not this call: no argument it passes makes the result null. */
    default boolean callResultNonNull(Expression call) {
        return false;
    }

    /**
     * The parameter is non-null because it is a precondition: its method, and every method of its override family,
     * dereferences it, requires it or rejects null on every normal exit, and the analysis declared it non-null although
     * a caller may pass null (maddi-mod's {@code Policy.preconditionsNonNull}). The caller asserts that argument
     * ({@code f(x!!)}, {@code F(x!)}): in Kotlin the NullPointerException moves from the method's body to the call; in
     * C# and in Java's annotations nothing changes at run time. False for a parameter no null reaches, which is
     * non-null without the precondition.
     */
    default boolean preconditionNonNull(ParameterInfo parameterInfo) {
        return false;
    }

    /**
     * The method answers a null argument with its own check, not a dereference: it needs the value on every normal
     * exit, and compares it with null first ({@code if (p == null) throw new IllegalArgumentException(...)}, a helper
     * such as {@code ensureNotBlank}). Declared non-null in Kotlin, a Java caller's null meets Kotlin's entry check, a
     * NullPointerException, instead of that exception (CodeLaser/maddi-mod#28).
     */
    default boolean rejectsNull(ParameterInfo parameterInfo) {
        return false;
    }

    /**
     * A class's type parameter no instantiation makes nullable: guava's {@code LocalCache<K, V>} in a
     * {@code @NullMarked} scope, without an {@code extends @Nullable Object} bound. Kotlin bounds it by {@code Any},
     * as Java's null-marked K is not null; a null-checked target's other members then keep {@code K} non-null.
     * False by default: a type parameter is bounded by its declaration alone.
     */
    default boolean nonNullTypeParameter(TypeParameter typeParameter) {
        return false;
    }

    /**
     * A class's superclass, with the verdict's states on its type arguments: jenkins' {@code OwnerContext extends
     * LinkedList<String>}, whose own methods add null ({@code addFirst(owner)}), is a {@code LinkedList<String?>}.
     * The printer writes it in the class header. Null: no verdict, the declared superclass as it is.
     */
    default ParameterizedType parentClass(TypeInfo typeInfo) {
        return null;
    }

    /**
     * The nullability analysis ran over the whole program under a null-marked policy: a type without {@code ?} is
     * not null, and a translation's project may enable C#'s nullable reference types.
     */
    default boolean nullabilityAnalysed() {
        return false;
    }

    /**
     * A local's verdict as a decision: {@link NullableState#UNSPECIFIED} on a reference type (a degraded method's
     * unreached local) becomes {@code NULLABLE}, the choice that compiles in a null-checked target, which has no
     * platform type for a local. Null stays null: without any verdict the declared type stands.
     */
    static ParameterizedType decided(ParameterizedType local) {
        if (local != null && local.nullable() == NullableState.UNSPECIFIED && !local.isPrimitiveExcludingVoid()) {
            return local.withNullable(NullableState.NULLABLE);
        }
        return local;
    }

    /**
     * These facts, with {@code fallback}'s declaration verdicts ({@link #field}, {@link #parameter},
     * {@link #returnType}) where these have none: a report of the analysed program, with what the analyzer stored
     * on the library's declarations from its hints.
     */
    default NullabilityFacts orElse(NullabilityFacts fallback) {
        return new ForwardingNullabilityFacts(this) {
            @Override
            public ParameterizedType field(FieldInfo fieldInfo) {
                ParameterizedType verdict = super.field(fieldInfo);
                return verdict != null ? verdict : fallback.field(fieldInfo);
            }

            @Override
            public ParameterizedType parameter(ParameterInfo parameterInfo) {
                ParameterizedType verdict = super.parameter(parameterInfo);
                return verdict != null ? verdict : fallback.parameter(parameterInfo);
            }

            @Override
            public ParameterizedType returnType(MethodInfo methodInfo) {
                ParameterizedType verdict = super.returnType(methodInfo);
                return verdict != null ? verdict : fallback.returnType(methodInfo);
            }
        };
    }

    /** These facts, as from a whole-program analysis under a null-marked policy: {@link #nullabilityAnalysed}. */
    default NullabilityFacts nullMarked() {
        return new ForwardingNullabilityFacts(this) {
            @Override
            public boolean nullabilityAnalysed() {
                return true;
            }
        };
    }
}
