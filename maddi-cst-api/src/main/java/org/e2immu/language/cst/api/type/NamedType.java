package org.e2immu.language.cst.api.type;

public interface NamedType {
    ParameterizedType asParameterizedType();

    ParameterizedType asSimpleParameterizedType();

    String simpleName();
}
