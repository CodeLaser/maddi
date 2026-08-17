package io.codelaser.maddi.modification.link.impl.translate;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class TranslateConstants implements TranslationMap {
    private final Runtime runtime;
    private final Map<Variable, Expression> map = new HashMap<>();

    public TranslateConstants(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression put(Variable v, Expression e) {
        return map.put(v, e);
    }

    public void remove(Variable v) {
        map.remove(v);
    }

    public Expression get(Variable v) {
        return map.get(v);
    }

    @Override
    public Expression translateVariableExpressionNullIfNotTranslated(Variable variable) {
        return map.get(variable);
    }

    @Override
    public Variable translateVariableRecursively(Variable variable) {
        return runtime.translateVariableRecursively(this, variable);
    }
}
