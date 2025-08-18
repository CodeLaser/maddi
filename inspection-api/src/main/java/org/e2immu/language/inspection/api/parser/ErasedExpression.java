package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Set;

public interface ErasedExpression extends Expression {
    Set<ParameterizedType> erasureTypes();
}
