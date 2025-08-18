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
