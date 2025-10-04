package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Map;

public interface GenericsHelper {

    MethodTypeParameterMap newMethodTypeParameterMap(MethodInfo methodInfo, Map<NamedType, ParameterizedType> concreteTypes);

    /*
     StringMap<V> -> HashMap<K,V> -> Map<K, V>

     M2: K(map) -> K(hashmap), M1: K(hashmap) -> String

     M1: E=0 in Set -> String, M2: Target = E=0 in Collection.
     We must make the link between E in Collection and E in set

     TODO this code is pretty dedicated; it should be recursive? TODO clean-up, extend
  */
    Map<NamedType, ParameterizedType> biDirectionalCombineMaps(Map<NamedType, ParameterizedType> m1,
                                                               Map<NamedType, ParameterizedType> m2);

    /*
        StringMap<V> -> HashMap<K,V> -> Map<K, V>

        M2: K(map) -> K(hashmap), M1: K(hashmap) -> String
        */
    Map<NamedType, ParameterizedType> combineMaps(Map<NamedType, ParameterizedType> m1,
                                                  Map<NamedType, ParameterizedType> m2);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(ParameterizedType parameterizedType);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(
            ParameterizedType parameterizedType,
            boolean complain);

    // practically the duplicate of the previous, except that we should parameterize initialTypeParameterMap as well to collapse them
    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSubType(TypeInfo ti,
                                                                      ParameterizedType superType);

    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSuperType(TypeInfo ti,
                                                                        ParameterizedType superType);

    Map<NamedType, ParameterizedType> translateMap(ParameterizedType formalType,
                                                   ParameterizedType concreteType,
                                                   boolean concreteTypeIsAssignableToThis);

    ExpressionBuilder newLambdaExpressionBuilder();

    interface ExpressionBuilder extends Element.Builder<ExpressionBuilder> {
        // use when defining field expressions
        @Fluent
        ExpressionBuilder setEnclosingType(TypeInfo typeInfo);

        // use in methods
        @Fluent
        ExpressionBuilder setEnclosingMethod(MethodInfo methodInfo);

        // the variables of the expression which will become parameters
        @Fluent
        ExpressionBuilder addVariable(Variable variable);

        @Fluent
        ExpressionBuilder setExpression(Expression expression);

        @Fluent
        ExpressionBuilder setForwardType(ParameterizedType forwardType);

        @Fluent
        ExpressionBuilder setSourceForStatement(Source source);

        Lambda build();
    }
}
