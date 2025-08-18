package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodModifier;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;

import java.util.List;
import java.util.Set;

public interface MethodInspection extends Inspection {
    List<ParameterizedType> exceptionTypes();

    Set<MethodInfo> overrides();

    List<TypeParameter> typeParameters();

    Set<MethodModifier> modifiers();

    MethodInspection withSynthetic(boolean synthetic);

    enum OperatorType {
        NONE, INFIX, PREFIX, POSTFIX,
    }

    ParameterizedType returnType();

    OperatorType operatorType();

    Block methodBody();

    String fullyQualifiedName();

    List<ParameterInfo> parameters();

    MethodInfo.MissingData missingData();
}

