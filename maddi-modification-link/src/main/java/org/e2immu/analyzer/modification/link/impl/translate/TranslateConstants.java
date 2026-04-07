package org.e2immu.analyzer.modification.link.impl.translate;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

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
