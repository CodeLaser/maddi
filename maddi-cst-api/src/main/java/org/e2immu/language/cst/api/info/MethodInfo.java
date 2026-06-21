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

package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.util.ParSeq;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a method or constructor declaration in the CST.
 * <p>
 * Constructors are identified by the special name {@link #CONSTRUCTOR_NAME}.
 * The kind of callable is further refined by {@link #methodType()}.
 * <p>
 * Methods go through the same two-phase lifecycle as other {@link Info} objects:
 * inspection (structure, modifiers, parameters, body) and analysis (modification,
 * nullability, pre/post-conditions, commutation data).
 */
public interface MethodInfo extends Info {

    /** The internal name used for all constructors ({@code "<init>"}). */
    String CONSTRUCTOR_NAME = "<init>";

    /** Returns the checked exception types declared in the {@code throws} clause. */
    List<ParameterizedType> exceptionTypes();

    /**
     * Returns {@code true} if this is a factory method (a static method that returns
     * an instance of its own owning type or a close relative).
     */
    boolean isFactoryMethod();

    /** Returns {@code true} if this is method is a <it>finalizer</it> as defined in the <it>maddi</it> manual.
     * Once a finalizer has been called on a modifiable object, no modifying methods may be called anymore.
     */
    boolean isFinalizer();

    /**
     * Returns {@code true} if this is the synthetic constructor generated for array creation
     * (used internally to model {@code new int[n]} as a constructor call).
     */
    boolean isSyntheticArrayConstructor();

    /** Returns the set of modifiers declared on this method ({@code public}, {@code static}, …). */
    Set<MethodModifier> methodModifiers();

    /**
     * Returns {@code true} if this method does not return a value
     * ({@code void} return type or a constructor).
     */
    boolean noReturnValue();

    /**
     * Returns {@code true} if this method has an explicitly written empty body ({@code {}})
     * as opposed to having no body (abstract) or a body with statements.
     */
    boolean explicitlyEmptyMethod();

    /** Returns {@code true} if this callable is a constructor. */
    boolean isConstructor();

    /** Returns {@code true} if this method is declared {@code final}. */
    boolean isFinal();

    /**
     * Returns {@code true} if this method has the same name and parameter count as a method
     * declared on {@code java.lang.Object} ({@code equals}, {@code hashCode}, {@code toString}, …).
     */
    // FIXME should we rename this to isOverrideOfJLOMethod?
    boolean isOverloadOfJLOMethod();

    /** Returns {@code true} if this method is declared {@code synchronized}. */
    boolean isSynchronized();

    /** Returns the refined kind of this callable (constructor, static initialiser, …). */
    MethodInfo.MethodType methodType();

    /** Returns the outermost enclosing type of the type that owns this method. */
    TypeInfo primaryType();

    /** Returns {@code true} if this is an interface {@code default} method. */
    boolean isDefault();

    /** Returns {@code true} if this method's return type is {@code void}. */
    boolean isVoid();

    /**
     * Returns {@code true} if the method body contains at least one statement
     * (complexity greater than the threshold for a method without code).
     */
    boolean complexityGreaterThanCOMPLEXITY_METHOD_WITHOUT_CODE();

    /** Returns {@code true} if this method has a non-void, non-constructor return type. */
    default boolean hasReturnValue() {
        return !isVoid() && !isConstructor();
    }

    /** Returns {@code true} if this method is declared as a postfix operator. */
    boolean isPostfix();

    /** Returns {@code true} if this method is declared as an infix operator. */
    boolean isInfix();

    /** Returns {@code true} if this method is declared {@code abstract}. */
    boolean isAbstract();

    /** Returns translated copies of this method as described by {@code translationMap}. */
    List<MethodInfo> translate(TranslationMap translationMap);

    /**
     * Phase 3 of the rewiring protocol: copies the method body, rewriting all
     * cross-references through {@code infoMap}.
     */
    void rewirePhase3(InfoMap infoMap);

    /**
     * Returns the declared type of the parameter at the given index, substituting
     * the component type for a varargs parameter when the index exceeds the last declared parameter.
     */
    ParameterizedType typeOfParameterHandleVarargs(int index);

    /**
     * Returns a copy of this method with a different body, leaving all other properties unchanged.
     *
     * @param newBody the replacement body
     */
    MethodInfo withMethodBody(Block newBody);

    /** Returns a copy of this method with a different {@link MethodType}. */
    MethodInfo withMethodType(MethodType methodType);

    /** Returns a copy of this method with the synthetic flag set to the given value. */
    MethodInfo withSynthetic(boolean synthetic);

    /**
     * Classifies the kind of callable represented by a {@link MethodInfo}.
     * Implementations are typically a closed enum of cases.
     */
    interface MethodType {
        /** Returns {@code true} for a compact constructor (Java 16+ records). */
        boolean isCompactConstructor();

        /** Returns {@code true} for a {@code static} method. */
        boolean isStatic();

        /** Returns {@code true} for any constructor (including synthetic and compact ones). */
        boolean isConstructor();

        /** Returns {@code true} for an abstract method. */
        boolean isAbstract();

        /** Returns {@code true} for an interface {@code default} method. */
        boolean isDefault();

        /** Returns {@code true} for a {@code static} initialiser block. */
        boolean isStaticInitializer();

        /**
         * Returns {@code true} for a synthetic constructor generated for anonymous or
         * inner-class capture.
         */
        boolean isSyntheticConstructor();
    }

    /**
     * Tracks which parts of a method's resolution data have not yet been populated.
     * Used to detect incomplete inspection or analysis.
     */
    interface MissingData {
        /** Returns {@code true} if this method's body has not been resolved yet. */
        boolean methodBody();

        /** Returns {@code true} if this method's override set has not been resolved yet. */
        boolean overrides();
    }

    /** Returns the simple name of this method, or {@link #CONSTRUCTOR_NAME} for constructors. */
    String name();

    /** Returns {@code true} if this method is declared {@code static}. */
    boolean isStatic();

    /** Returns the declared return type of this method (a {@code void} type for constructors). */
    ParameterizedType returnType();

    /**
     * Returns the set of methods at the root of the override hierarchy that this method
     * ultimately overrides. If this method does not override anything, returns a singleton
     * containing {@code this}.
     */
    // FIXME rename method to topOfOverridingHierarchy
    Set<MethodInfo> topOfOverloadingHierarchy();

    /**
     * Returns {@code true} if this is the single abstract method of a standard functional
     * interface (from {@code java.util.function}, {@code Runnable}, a synthetic SAM type,
     * or any type annotated with {@code @FunctionalInterface}).
     */
    default boolean isSAMOfStandardFunctionalInterface() {
        return this == typeInfo().singleAbstractMethod()
               && ("java.util.function".equals(typeInfo().packageName())
                   || Runnable.class.getCanonicalName().equals(typeInfo().fullyQualifiedName())
                   || typeInfo().isSynthetic()
                   || typeInfo().annotations().stream()
                           .anyMatch(ae -> "java.lang.FunctionalInterface"
                                   .equals(ae.typeInfo().fullyQualifiedName())));
    }

    /** Returns the parameters of this method in declaration order. */
    List<ParameterInfo> parameters();

    /** Returns the type parameters declared on this method, in declaration order. */
    List<TypeParameter> typeParameters();

    /** Returns the body of this method, or an empty block for abstract / interface methods. */
    Block methodBody();

    /**
     * Returns {@code true} if this method overloads (i.e. has the same name and compatible
     * parameter types as) {@code methodInfo}.
     */
    // FIXME check name and implementation!!
    boolean isOverloadOf(MethodInfo methodInfo);

    /**
     * Returns the set of methods in supertypes that this method overrides.
     * Populated during resolution, after inspection.
     */
    Set<MethodInfo> overrides();

    /**
     * Returns {@code true} if the analyser has determined that this method may cause any of the non-final fields
     * in the current execution to be re-assigned. This is definitely the case when a real interrupt in the
     * thread occurs, but there may be other situations.
     * Trivial methods like Math.max() will not cause an interrupt, but more complex ones like "System.out.println()"
     * will do so.
     * <p>
     * FIXME expand explanation.
     */
    boolean allowsInterrupts();

    /** Returns {@code true} if this method has {@code public} access. */
    boolean isPublic();

    /**
     * Returns {@code true} if this method is accessible from outside its compilation unit,
     * taking nesting and enclosing type access into account.
     */
    boolean isPubliclyAccessible();

    /** Returns {@code true} if this method overloads {@code java.lang.Object.equals(Object)}. */
    // FIXME rename to isOverrideOfJLOEquals
    boolean isOverloadOfJLOEquals();

    /** Returns {@code true} if this is a compact constructor (Java 16+ records). */
    boolean isCompactConstructor();

    /** Returns {@code true} if this is a synthetic constructor generated by the compiler. */
    boolean isSyntheticConstructor();

    /** Returns {@code true} if this represents a {@code static} initialiser block. */
    boolean isStaticInitializer();

    /** Returns {@code true} if this represents an instance initialiser block. */
    boolean isInstanceInitializer();

    /** Returns {@code true} if the analyser determined this method modifies its receiver. */
    default boolean isModifying() {
        return !isNonModifying();
    }

    /**
     * Returns {@code true} if the analyser determined this method does not modify its receiver
     * or any reachable state.
     */
    boolean isNonModifying();

    /**
     * Returns {@code true} if this method is fluent (always returns {@code this}),
     * as determined by analysis.
     */
    boolean isFluent();

    /**
     * Returns {@code true} if this method always returns its first argument unchanged,
     * as determined by analysis.
     */
    boolean isIdentity();

    /**
     * Returns {@code true} if modifications made inside this method should be ignored
     * by the analyser (e.g. logging or assertion helpers).
     */
    boolean isIgnoreModification();

    /** Returns information about which parts of this method's data are still unresolved. */
    MissingData missingData();

    /**
     * Returns a map from each field owned by the enclosing type to a flag indicating
     * whether the field is read ({@code false}) or modified ({@code true}) by this method.
     */
    Map<FieldInfo, Boolean> areOwnFieldsReadModified();

    /**
     * Returns commutation data describing whether calls to this method may be reordered
     * relative to other calls, as determined by analysis.
     */
    Value.CommutableData commutableData();

    /**
     * Returns the parallel-group structure of the parameters, describing which subsets of
     * arguments are interchangeable (order-independent) for the purposes of deduplication.
     */
    Value.ParameterParSeq getParallelGroups();

    /** Returns {@code true} if any parameters belong to a parallel group. */
    default boolean hasParallelGroups() {
        ParSeq<ParameterInfo> parSeq = getParallelGroups().parSeq();
        return parSeq != null && parSeq.containsParallels();
    }

    /**
     * Sorts {@code parameterExpressions} according to the parallel-group structure and
     * natural expression order, for canonical deduplication.
     */
    default List<Expression> sortAccordingToParallelGroupsAndNaturalOrder(List<Expression> parameterExpressions) {
        return getParallelGroups().parSeq().sortParallels(parameterExpressions, Comparator.naturalOrder());
    }

    /**
     * Returns analysis data describing which field this method acts as a getter or setter for,
     * or {@code null} / a default value if it is neither.
     */
    Value.FieldValue getSetField();

    /**
     * Returns analysis data describing equivalent method calls that can replace calls with
     * explicit parameters (e.g. a builder method that sets the same field as a setter).
     */
    Value.GetSetEquivalent getSetEquivalents();

    /** Returns the post-conditions established by this method, as determined by analysis. */
    Value.PostConditions postConditions();

    /** Returns the pre-condition required by this method, as determined by analysis. */
    Value.Precondition precondition();

    /**
     * Returns the set of statement indices for {@code throw} or {@code assert} statements
     * that were not absorbed into a pre- or post-condition by the analyser.
     */
    Value.IndicesOfEscapes indicesOfEscapesNotInPreOrPostConditions();

    /** Returns the mutable builder for this method (available until inspection is committed). */
    Builder builder();

    /**
     * Returns {@code true} if the analyser determined this method always returns a non-null value,
     * as a property (derived from annotations or analysis).
     */
    boolean isPropertyNotNull();

    /**
     * Returns {@code true} if the analyser determined this method may return {@code null},
     * as a property (derived from annotations or analysis).
     */
    boolean isPropertyNullable();

    /** Returns the compilation unit of the owning type. */
    default CompilationUnit compilationUnit() {
        return typeInfo().compilationUnit();
    }

    /** Builder for constructing a {@link MethodInfo} during the inspection phase. */
    interface Builder extends Info.Builder<Builder> {

        /**
         * Signals that all parameters have been added; after this call the method's
         * fully qualified name can be computed.
         */
        @Fluent
        Builder commitParameters();

        /** Sets the method body. */
        @Fluent
        Builder setMethodBody(Block block);

        /** Adds a modifier to this method ({@code public}, {@code static}, …). */
        @Fluent
        Builder addMethodModifier(MethodModifier methodModifier);

        /**
         * Adds a parameter with the given name and type and immediately commits it,
         * without returning the parameter's builder.
         */
        @Fluent
        Builder addAndCommitParameter(String name, ParameterizedType type);

        /** Sets the return type of this method. */
        @Fluent
        Builder setReturnType(ParameterizedType returnType);

        /**
         * Adds a named parameter and returns its builder so that further configuration
         * (annotations, modifiers) can be applied before it is committed.
         */
        ParameterInfo addParameter(String name, ParameterizedType type);

        /**
         * Adds an unnamed parameter (Java 21+ unnamed patterns / unnamed variables)
         * and returns its builder.
         */
        ParameterInfo addUnnamedParameter(ParameterizedType type);

        /** Adds a type parameter declared on this method. */
        @Fluent
        Builder addTypeParameter(TypeParameter typeParameter);

        /** Adds a checked exception type to the {@code throws} clause. */
        @Fluent
        Builder addExceptionType(ParameterizedType exceptionType);

        /** Records the set of methods in supertypes that this method overrides. */
        @Fluent
        Builder addOverrides(Collection<MethodInfo> overrides);

        /** Adds an already-built {@link ParameterInfo} directly (used during translation/rewiring). */
        @Fluent
        Builder addParameter(ParameterInfo parameterInfo);

        /** Records which parts of this method's data are still missing / unresolved. */
        @Fluent
        Builder setMissingData(MissingData missingData);

        /** Returns the parameters accumulated in the builder so far. */
        List<ParameterInfo> parameters();

        /** Returns the type parameters accumulated in the builder so far. */
        List<TypeParameter> typeParameters();
    }

    /**
     * Returns {@code true} if the last declared parameter is a varargs parameter
     * ({@code T...}).
     */
    default boolean isVarargs() {
        if (parameters().isEmpty()) return false;
        return parameters().getLast().isVarArgs();
    }
}
