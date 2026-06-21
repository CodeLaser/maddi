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

package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * A fine-grained map from well-known syntactic positions (keywords, punctuation, comma separators)
 * to their precise {@link Source} coordinates within a single CST element.
 * <p>
 * The keys are singleton sentinel objects defined as constants on this interface
 * (e.g. {@link #EXTENDS}, {@link #ARGUMENT_COMMAS}). Lookup is by object identity.
 * <p>
 * {@code DetailedSources} is used during source-accurate pretty-printing and when
 * computing the {@link Element.TypeReference} import information for parameterised types,
 * where the same {@link TypeInfo} may appear multiple times within one element.
 */
public interface DetailedSources {

    /** Position of the closing {@code )} of a record component list or method formal-parameter list. */
    Object END_OF_PARAMETER_LIST = new Object();
    /** Position of the {@code extends} keyword in a type declaration header. */
    Object EXTENDS = new Object();
    /** Position of the {@code implements} keyword in a type declaration header. */
    Object IMPLEMENTS = new Object();
    /** Position of the {@code permits} keyword in a sealed type declaration header. */
    Object PERMITS = new Object();
    /** Source range spanning the entire field declaration (type + name + initialiser). */
    Object FIELD_DECLARATION = new Object();
    /** Position of the comma that <em>precedes</em> an element in a comma-separated list. */
    Object PRECEDING_COMMA = new Object();
    /** Position of the comma that <em>follows</em> an element in a comma-separated list. */
    Object SUCCEEDING_COMMA = new Object();
    /** Position of the {@code =} sign in a field or local-variable declarator. */
    Object SUCCEEDING_EQUALS = new Object();
    /** Positions of all {@code ,} separators in a method-call / constructor-call / annotation argument list. */
    Object ARGUMENT_COMMAS = new Object();
    /** Position of the closing {@code )} of a method-call / constructor-call / annotation argument list. */
    Object END_OF_ARGUMENT_LIST = new Object();
    /** Positions of all {@code ,} separators in a type-argument list (e.g. {@code <A, B>}). */
    Object TYPE_ARGUMENT_COMMAS = new Object();
    /** Positions of all {@code ,} separators in a type's {@code extends} list. */
    Object EXTENDS_COMMAS = new Object();
    /** Positions of all {@code ,} separators in a type's {@code implements} list. */
    Object IMPLEMENTS_COMMAS = new Object();
    /** Positions of all {@code ,} separators in a type's {@code permits} list. */
    Object PERMITS_COMMAS = new Object();
    /** Positions of all {@code ,} separators in a method's {@code throws} list. */
    Object THROWS_COMMAS = new Object();
    /** Positions of all {@code ,} separators in a local-variable declaration with multiple declarators. */
    Object LOCAL_VARIABLE_COMMAS = new Object();
    /** Positions of all {@code =} operators in a multi-declarator local-variable declaration. */
    Object LOCAL_VARIABLE_ASSIGNMENT_OPERATORS = new Object();
    /** Positions of all {@code &} separators in a type-parameter bound (e.g. {@code T extends A & B}). */
    Object TYPE_BOUND_AMPERSANDS = new Object();

    /**
     * Returns the single {@link Source} associated with {@code object}, or {@code null} if absent.
     * Use when at most one position is expected for the given key.
     */
    Source detail(Object object);

    /**
     * Returns all {@link Source} positions associated with {@code object}.
     * Use for list-valued keys (comma positions, etc.) and when the same {@link TypeInfo} may
     * appear multiple times.
     */
    @NotNull
    List<Source> details(Object object);

    /** Returns a new {@code DetailedSources} combining the entries of this and {@code other}. */
    DetailedSources merge(DetailedSources other);

    /**
     * Returns the object associated with {@code object} via a put-with-association call,
     * e.g. the array-stripped version of a parameterised type. Returns {@code null} if none.
     */
    Object associatedObject(Object object);

    /** Returns a copy with the sources for {@code o} replaced by {@code sources}. */
    DetailedSources withSources(Object o, List<Source> sources);

    interface Builder {

        Builder addAll(DetailedSources detailedSources);

        Builder copy();

        Object getAssociated(Object pt);

        Builder put(Object object, Source source);

        default void putIfNotNull(Object object, Source source) {
            if (source != null) put(object, source);
        }

        default void putListIfNotNull(Object object, List<Source> list) {
            if (list != null) putList(object, list);
        }

        Builder putList(Object object, List<Source> sourceList);

        DetailedSources build();

        /** Records the mapping from an array-typed {@link org.e2immu.language.cst.api.type.ParameterizedType} to its non-array counterpart. */
        Builder putWithArrayToWithoutArray(ParameterizedType withArray, ParameterizedType withoutArray);

        record TypeInfoSource(TypeInfo typeInfo, Source source) {
        }

        /** Records qualification information: for {@code typeInfo}, which enclosing types were written out. */
        Builder putTypeQualification(TypeInfo typeInfo, List<TypeInfoSource> associatedList);
    }

    /**
     * Returns the explicitly written qualifier type for {@code typeInfo} (e.g. {@code Map}
     * when the source contains {@code Map.Entry}), or {@code null} if the type was written
     * without a qualifier.
     */
    TypeInfo qualifier(TypeInfo typeInfo);
}
