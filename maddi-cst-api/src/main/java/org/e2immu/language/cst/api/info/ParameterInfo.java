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
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

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
     * Returns {@code true} if the analyser determined this parameter is modified inside
     * the method body.
     */
    default boolean isModified() { return !isUnmodified(); }

    /**
     * Returns {@code true} if the analyser determined this parameter is never modified
     * inside the method body.
     */
    boolean isUnmodified();

    /**
     * Returns analysis data describing which field this parameter is assigned to
     * (directly or via a setter), as determined by the analyser.
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
