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

package io.codelaser.maddi.cst.api.info;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Set;

/**
 * Represents a field declaration in the CST.
 * <p>
 * A field belongs to exactly one owning type ({@link #owner()}) and carries a declared type
 * ({@link #type()}), modifiers, and an optional initialiser expression.
 * <p>
 * Structural data is populated during inspection; analysis properties (nullability, finality
 * by assignment) are attached afterwards.
 */
public interface FieldInfo extends Info {

    /**
     * Returns the analysis property map computed for the field's initialiser expression,
     * or an empty map if no initialiser is present.
     */
    PropertyValueMap analysisOfInitializer();

    /**
     * Returns the zero-based position of this field in the owning type's field list,
     * in declaration order.
     */
    default int indexInType() {
        int count = 0;
        for (FieldInfo f : owner().fields()) {
            if (f == this) return count;
            ++count;
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if the analyzer determined this field is modified after
     * construction.
     */
    default boolean isModified() { return !isUnmodified(); }

    /**
     * Returns {@code true} if the analyzer determined this field's content is never modified
     * after construction.
     */
    boolean isUnmodified();

    /**
     * Returns {@code true} if the analyzer determined that the object held by this field is never modified
     * itself after construction; the objects in its hidden content (a collection's elements) may be. Implied by
     * {@link #isUnmodified()}, the fallback when the structural verdict is absent.
     */
    boolean isStructurallyUnmodified();

    /** Returns the simple name of this field. */
    String name();

    /** Returns the type that declares this field. */
    TypeInfo owner();

    /**
     * Phase 3 of the rewiring protocol: copies the initialiser expression,
     * rewriting all cross-references through {@code infoMap}.
     */
    void rewirePhase3(InfoMap infoMap);

    /** Returns translated copies of this field as described by {@code translationMap}. */
    List<FieldInfo> translate(TranslationMap translationMap);

    /** Returns the declared type of this field. */
    ParameterizedType type();

    /** Returns {@code true} if this field is declared {@code static}. */
    boolean isStatic();

    /** Returns {@code true} if this field is declared {@code final}. */
    boolean isFinal();

    /** Returns {@code true} if this field is declared {@code transient}. */
    boolean isTransient();

    /** Returns {@code true} if this field is declared {@code volatile}. */
    boolean isVolatile();

    /**
     * Returns {@code true} if the analyzer determined this field holds a non-null value,
     * either because its declared type is a primitive or because analysis confirmed it.
     */
    boolean isPropertyNotNull();

    /**
     * Returns {@code true} if the analyzer determined this field is effectively final
     * by assignment (even if not declared {@code final}).
     * Distinct from {@link #isFinal()}, which reflects the source modifier.
     */
    boolean isPropertyFinal();

    /**
     * Returns {@code true} if modifications to this field should be ignored by the analyzer
     * (e.g. fields used only for logging or bookkeeping).
     */
    boolean isIgnoreModifications();

    /** Returns the compilation unit of the owning type. */
    default CompilationUnit compilationUnit() {
        return owner().compilationUnit();
    }

    /**
     * Renders this field to an {@link OutputBuilder}.
     *
     * @param asParameter if {@code true}, omits modifiers and renders the field in
     *                    parameter-declaration style (used for record components)
     */
    OutputBuilder print(Qualification qualification, boolean asParameter);

    /** Returns the mutable builder for this field (available until inspection is committed). */
    Builder builder();

    /** Returns the set of modifiers declared on this field. */
    Set<FieldModifier> modifiers();

    /**
     * Returns the initialiser expression declared with this field,
     * or an {@code EmptyExpression} if no initialiser is written in source.
     */
    Expression initializer();

    /**
     * Returns a copy of this field with a different owning type, leaving all other
     * properties unchanged. Used when rewiring a field into a translated type.
     */
    FieldInfo withOwner(TypeInfo newOwner);

    /**
     * Returns a copy of this field with a different owning type whose inspection has not yet
     * been committed. Used during the early stages of rewiring before the new owner is finalised.
     */
    FieldInfo withOwnerVariableBuilder(TypeInfo newOwner);

    /** Builder for constructing a {@link FieldInfo} during the inspection phase. */
    interface Builder extends Info.Builder<Builder> {

        /** Adds a modifier ({@code public}, {@code static}, {@code final}, …) to this field. */
        @Fluent
        Builder addFieldModifier(FieldModifier fieldModifier);

        /**
         * Replaces the field's type, before the field is committed. For a field built from its symbol before its
         * declaration was scanned, whose type then lacks the declaration's type-use annotations (maddi#145).
         * Field references made before the replacement keep the type they were made with.
         */
        @Fluent
        Builder setType(ParameterizedType type);

        /** Sets the initialiser expression for this field. */
        @Fluent
        Builder setInitializer(Expression initializer);

        /**
         * Returns the initialiser expression accumulated in the builder so far,
         * or {@code null} if none has yet been set.
         */
        Expression initializer();
    }
}
