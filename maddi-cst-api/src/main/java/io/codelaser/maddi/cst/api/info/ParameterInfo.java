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
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a single parameter in a method or constructor declaration.
 * <p>
 * {@code ParameterInfo} extends both {@link Variable} (so it can appear as a variable
 * in expressions) and {@link Info} (so it participates in the inspection / analysis lifecycle
 * and carries annotations).
 * <p>
 * Parameters are owned by their declaring method ({@link #methodInfo()}) and identified
 * by their zero-based index ({@link #index()}).
 */
public interface ParameterInfo extends Variable, Info {

    /** Returns the zero-based position of this parameter in the method's parameter list. */
    int index();

    /**
     * Returns the simple name of this parameter as written in source.
     * For unnamed parameters (Java 21+), returns {@code LocalVariableImpl.UNNAMED}.
     */
    String name();

    /** Returns {@code true} if this is a varargs parameter ({@code T... name}). */
    boolean isVarArgs();

    /**
     * Returns {@code true} if this parameter is declared {@code final}.
     * Note: in some languages (e.g. Kotlin) all parameters are implicitly final.
     */
    boolean isFinal();

    /** Returns the mutable builder for this parameter (available until inspection is committed). */
    Builder builder();

    /** Returns translated copies of this parameter as described by {@code translationMap}. */
    List<ParameterInfo> translate(TranslationMap translationMap);

    /**
     * Returns {@code true} if this is an unnamed parameter ({@code _} in Java 21+
     * unnamed patterns or unnamed variables).
     */
    boolean isUnnamed();

    /** Builder for constructing a {@link ParameterInfo} during the inspection phase. */
    interface Builder extends Info.Builder<Builder> {

        /** Sets whether this parameter is declared {@code final}. */
        @Fluent
        Builder setIsFinal(boolean isFinal);

        /** Sets whether this parameter is a varargs parameter. */
        @Fluent
        Builder setVarArgs(boolean varArgs);
    }

    /**
     * Returns {@code true} if the analyzer determined this parameter is modified inside
     * the method body.
     */
    default boolean isModified() { return !isUnmodified(); }

    /**
     * Returns {@code true} if the analyzer determined this parameter is never modified
     * inside the method body.
     */
    boolean isUnmodified();

    /**
     * Returns {@code true} if the analyzer determined that the method never modifies the argument's object
     * itself: no mutator is called on it, it is not handed to a parameter that is structurally modified. Its
     * elements (its hidden content) may still be modified. Implied by {@link #isUnmodified()}, which is the
     * fallback when the structural verdict is absent. A Kotlin {@code List}/{@code Map} parameter is this.
     */
    boolean isStructurallyUnmodified();

    /**
     * Returns analysis data describing which field this parameter is assigned to
     * (directly or via a setter), as determined by the analyzer.
     */
    Value.AssignedToField assignedToField();

    /** Returns the method or constructor that declares this parameter. */
    MethodInfo methodInfo();

    /**
     * Streams all types that are explicitly referenced in this parameter's declared type
     * (for import computation and type dependency tracking).
     */
    Stream<TypeReference> explicitTypesReferenced();

    /**
     * Returns a copy of this parameter with a different declared type,
     * leaving all other properties unchanged.
     */
    ParameterInfo withParameterizedType(ParameterizedType parameterizedType);

    /**
     * Returns a copy of this parameter with a different name and declared type,
     * leaving all other properties unchanged.
     */
    ParameterInfo with(String name, ParameterizedType parameterizedType);

}
