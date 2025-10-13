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
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.support.Either;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface TypeParameter extends NamedType, Info {

    boolean typeBoundsAreSet();

    int getIndex();

    Either<TypeInfo, MethodInfo> getOwner();

    default boolean isMethodTypeParameter() {
        return getOwner().isRight();
    }

    List<ParameterizedType> typeBounds();

    @NotNull
    OutputBuilder print(Qualification qualification, boolean printExtends);

    String toStringWithTypeBounds();

    default TypeInfo primaryType() {
        return getOwner().isLeft() ? getOwner().getLeft().primaryType() : getOwner().getRight().primaryType();
    }

    Builder builder();

    boolean hasBeenInspected();

    TypeParameter withOwnerVariableTypeBounds(MethodInfo methodInfo);

    TypeParameter withOwnerVariableTypeBounds(TypeInfo typeInfo);

    @NotNull
    Stream<Element.TypeReference> typesReferenced(boolean explicit, Set<TypeParameter> visited);

    interface Builder extends Info.Builder<Builder> {
        List<ParameterizedType> typeBounds();

        @Fluent
        Builder setTypeBounds(List<ParameterizedType> typeBounds);
    }
}
