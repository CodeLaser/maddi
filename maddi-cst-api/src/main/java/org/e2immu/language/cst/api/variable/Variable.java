package org.e2immu.language.cst.api.variable;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.util.OneVariable;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.stream.Stream;

/**
 * It is important that we can sort variables, as part of the sorting system of Expressions.
 */
public interface Variable extends Comparable<Variable>, Element, OneVariable {

    @NotNull
    String fullyQualifiedName();

    @NotNull
    String simpleName();

    @NotNull
    ParameterizedType parameterizedType();

    @Override
    default int compareTo(Variable o) {
        return fullyQualifiedName().compareTo(o.fullyQualifiedName());
    }

    default boolean isStatic() {
        return false;
    }

    default boolean scopeIsRecursively(Variable variable) {
        return false;
    }

    default FieldReference fieldReferenceScope() {
        return null;
    }

    default Variable fieldReferenceBase() {
        return null;
    }

    default Stream<Variable> variableStreamDescendIntoScope() {
        return Stream.of(this);
    }

    Variable rewire(InfoMap infoMap);
}
