package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/*
each object, even "ephemeral" ones during evaluation (and held in LinkEvaluation), can have statically assigned values
for each of their fields.

So

(new LinkEvaluation.Builder()).setLinkedVariables(abc)

will set the value abc to the field linkedVariables in the newly created builder object, which is the scope of the
setter.

We recognize getters/setters/accessors, @Fluent, @Identity.
We recognize assignment, addition?, array indexing

The links get stored in VI, and eventually in the fields and parameters where relevant.

LoopData ld = new LoopDataImpl.Builder().set(1, i).setBody(this::someMethod).build()

will hold for ld a StaticValues object, returning LoopData as type(), a LoopDataImpl object as a result from the
building in expression(), and values 'variables[0] -> i' and 'body -> this::someMethod' in the values() map.
 */

public interface StaticValues extends Value {
    boolean isEmpty();

    /*
            when stored in a VI object, this should be identical to vi.variable().parameterizedType()
            relevant when ephemeral, as in the above example.
             */
    ParameterizedType type();

    /*
    main assignment, e.g. new LV.Builder() in case of an ephemeral construction

    in a VI object, must be the value assigned to the variable
     */
    Expression expression();

    boolean multipleExpressions();

    /*
    results of setters.

    in the more obvious case, the scope is equal to runtime.newVariableExpression(vi.variable()).
    but it allows us to assign values at some levels deep, e.g. a.b.c
     */
    Map<Variable, Expression> values();

    StaticValues merge(StaticValues other);
    StaticValues remove(Predicate<Variable> predicate);

}
