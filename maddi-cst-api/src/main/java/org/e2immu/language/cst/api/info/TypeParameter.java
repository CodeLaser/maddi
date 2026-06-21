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
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.support.Either;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a type parameter declaration, such as {@code T} in {@code class Box<T>}
 * or {@code E extends Comparable<E>} in a method signature.
 * <p>
 * A type parameter is owned by either a {@link TypeInfo} or a {@link MethodInfo}
 * (see {@link #getOwner()}), and is identified by its zero-based index within the
 * owner's type-parameter list ({@link #getIndex()}).
 * <p>
 * Upper bounds are represented as {@link ParameterizedType} values in
 * {@link #typeBounds()}; an absent bound corresponds to an implicit
 * {@code extends java.lang.Object}.
 */
public interface TypeParameter extends NamedType, Info {

    /**
     * Returns {@code true} once the type bounds have been resolved and set.
     * Type bounds may be set after the initial inspection commit if the bound types
     * are discovered during hierarchy resolution.
     */
    boolean typeBoundsAreSet();

    /** Returns the zero-based index of this type parameter within the owner's declaration. */
    int getIndex();

    /**
     * Returns a left {@link TypeInfo} when this parameter is declared on a type,
     * or a right {@link MethodInfo} when it is declared on a method.
     */
    Either<TypeInfo, MethodInfo> getOwner();

    /**
     * Returns {@code true} if this type parameter is declared on a method
     * (i.e. {@link #getOwner()} is a right value).
     */
    default boolean isMethodTypeParameter() {
        return getOwner().isRight();
    }

    /**
     * Returns the upper bounds of this type parameter, in declaration order.
     * For a parameter declared as {@code T extends A & B}, this returns {@code [A, B]}.
     * Returns an empty list when there are no explicit bounds (implicit {@code Object} bound).
     */
    List<ParameterizedType> typeBounds();

    /**
     * Renders this type parameter to an {@link OutputBuilder}.
     *
     * @param printExtends if {@code true}, includes the {@code extends} clause with type bounds
     */
    @NotNull
    OutputBuilder print(Qualification qualification, boolean printExtends);

    /**
     * Returns a string representation that includes the type bounds,
     * e.g. {@code "T extends Comparable<T>"}.
     */
    String toStringWithTypeBounds();

    /**
     * Returns the outermost enclosing type: the primary type of the owning type or method.
     */
    default TypeInfo primaryType() {
        return getOwner().isLeft() ? getOwner().getLeft().primaryType() : getOwner().getRight().primaryType();
    }

    /** Returns the mutable builder for this type parameter (available until inspection is committed). */
    Builder builder();

    /**
     * Returns a copy of this type parameter with the same index and name but a new owning method,
     * and with the type bounds expressed in terms of the new owner's context.
     */
    TypeParameter withOwnerVariableTypeBounds(MethodInfo methodInfo);

    /**
     * Returns a copy of this type parameter with the same index and name but a new owning type,
     * and with the type bounds expressed in terms of the new owner's context.
     */
    TypeParameter withOwnerVariableTypeBounds(TypeInfo typeInfo);

    /**
     * Streams the type references made by this type parameter's bounds, filtered and annotated
     * according to {@code typeReferenceNature}. The {@code visited} set guards against cycles
     * when bounds are self-referential.
     */
    @NotNull
    Stream<Element.TypeReference> typesReferenced(TypeReferenceNature typeReferenceNature,
                                                  DetailedSources detailedSources,
                                                  Set<TypeParameter> visited);

    /** Builder for constructing a {@link TypeParameter} during the inspection phase. */
    interface Builder extends Info.Builder<Builder> {

        /** Returns the type bounds accumulated in the builder so far. */
        List<ParameterizedType> typeBounds();

        /** Sets the upper bounds for this type parameter. */
        @Fluent
        Builder setTypeBounds(List<ParameterizedType> typeBounds);
    }
}
