package org.e2immu.language.cst.api.variable;

import org.e2immu.language.cst.api.expression.Expression;

public interface DependentVariable extends Variable {
    Variable arrayVariable();

    default Variable arrayVariableBase() {
        Variable av = arrayVariable();
        while (av instanceof DependentVariable dv) {
            av = dv.arrayVariable();
        }
        return av;
    }

    Variable indexVariable();

    Expression arrayExpression();

    Expression indexExpression();

    @Override
    default Variable fieldReferenceBase() {
        return arrayVariable().fieldReferenceBase();
    }

    @Override
    default FieldReference fieldReferenceScope() {
        return arrayVariable().fieldReferenceScope();
    }
}
