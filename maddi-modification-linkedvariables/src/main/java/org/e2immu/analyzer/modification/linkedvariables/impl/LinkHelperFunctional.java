package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LinkHelperFunctional {
    private final Runtime runtime;
    private final AnalysisHelper analysisHelper;

    public LinkHelperFunctional(Runtime runtime, AnalysisHelper analysisHelper) {
        this.runtime = runtime;
        this.analysisHelper = analysisHelper;
    }

    public LinkedVariables functional(TypeInfo currentPrimaryTYpe,
                                      Value.Independent independentOfMethod,
                                      HiddenContentSelector hcsMethod,
                                      LinkedVariables linkedVariablesOfObject,
                                      ParameterizedType concreteReturnType,
                                      List<Value.Independent> independentOfParameter,
                                      List<HiddenContentSelector> hcsParameters,
                                      List<ParameterizedType> expressionTypes,
                                      List<LinkedVariables> linkedVariablesOfParameters,
                                      ParameterizedType concreteFunctionalType) {
        LinkedVariables lvs = functional(currentPrimaryTYpe, independentOfMethod, hcsMethod, linkedVariablesOfObject,
                concreteReturnType, concreteFunctionalType);
        int i = 0;
        for (ParameterizedType expressionType : expressionTypes) {
            int index = Math.min(hcsParameters.size() - 1, i);
            Value.Independent independent = independentOfParameter.get(index);
            HiddenContentSelector hcs = hcsParameters.get(index);
            if (index < linkedVariablesOfParameters.size()) {
                LinkedVariables linkedVariables = linkedVariablesOfParameters.get(index);
                LinkedVariables lvsParameter;
                if (linkedVariables == LinkedVariablesImpl.NOT_YET_SET) {
                    lvsParameter = linkedVariables;
                } else {
                    lvsParameter = functional(currentPrimaryTYpe, independent, hcs, linkedVariables, expressionType,
                            concreteFunctionalType);
                }
                lvs = lvs.merge(lvsParameter);
            }// else: varargs
            i++;
        }
        return lvs;
    }

    private LinkedVariables functional(TypeInfo currentPrimaryType,
                                       Value.Independent independent,
                                       HiddenContentSelector hcs,
                                       LinkedVariables linkedVariables,
                                       ParameterizedType type,
                                       ParameterizedType concreteFunctionalType) {
        if (independent.isIndependent()) return LinkedVariablesImpl.EMPTY;
        boolean independentHC = independent.isAtLeastIndependentHc();
        Map<Variable, LV> map = new HashMap<>();
        linkedVariables.forEach(e -> {
            Value.Immutable mutable = analysisHelper.typeImmutable(currentPrimaryType, type);
            if (!mutable.isImmutable()) {
                Map<Indices, Link> correctedMap = new HashMap<>();
                for (int hctIndex : hcs.set()) {
                    Indices indices = new IndicesImpl(hctIndex);
                    // see e.g. Linking_1A,f9m(): we correct 0 to 0;1, and 1 to 0;1
                    Indices corrected = indices.allOccurrencesOf(runtime, concreteFunctionalType);
                    if (corrected != null) {
                        Link link = new LinkImpl(indices, mutable.isMutable());
                        correctedMap.put(corrected, link);
                    }
                }
                Links links = new LinksImpl(correctedMap);
                LV lv = independentHC ? LVImpl.createHC(links) : LVImpl.createDependent(links);
                map.put(e.getKey(), lv);
            }
        });
        if (map.isEmpty()) return LinkedVariablesImpl.EMPTY;
        return LinkedVariablesImpl.of(map);
    }

    public record LambdaResult(List<LinkedVariables> linkedToParameters, LinkedVariables linkedToReturnValue) {
        public LinkedVariables mergedLinkedToParameters() {
            return linkedToParameters.stream().reduce(LinkedVariablesImpl.EMPTY, LinkedVariables::merge);
        }
    }

    /*
    SITUATION 0: both return value and all parameters @Independent
    no linking

    predicate is the typical example

    SITUATION 1: interesting return value, no parameters, or all parameters @Independent

    rv = supplier.get()             rv *-4-0 supplier       non-modifying, object to return value
    s = () -> supplier.get()        s 0-4-0 supplier
    s = supplier::get               s 0-4-0 supplier

    rv = iterator.next()            rv *-4-0 iterator (it is not relevant if the method is modifying, or not)
    s = () -> iterator.next()       s 0-4-0 iterator
    s = iterator::next              s 0-4-0 iterator
    t = s.get()                     t *-4-0 s, *-0-4 iterator

    rv = biSupplier.get()           rv *-4-0.0;0.1
    s = () -> biSupplier.get()      s 0.0;0.1-4-0.0;0.1
    pair = s.get()                  pair 0.0;0.1-4-0.0;0.1 biSupplier,s
    x = pair.x                      x *-4-0.0 pair, *-4-0.0 biSupplier

    -4- links depending on the HCS of the method (@Independent(hc), @Dependent?)
    -4-M links depending on the concrete type of rv when computing
    -2- links are also possible, e.g. subList(0, 2)

    sub = list.subList(0, 3)        sub 0-2-0 list
    s = i -> list.subList(0, i)     s 0-2-0 list

    conclusion:
     - irrespective of method modification.
     - @Independent of method is primary selector (-2-,-4-,no link)
     - * gets upgraded to the value in method HCS

    SITUATION 2: no return value, or an @Independent return value; at least one interesting parameter

    consumer.accept(t)              t *-4-0 consumer        modifying, parameter to object
    s = (t -> consumer.accept(t))   s 0-4-0 consumer
    sx.foreach(consumer)            sx 0-4-0 consumer

    list.add(t)                     t *-4-0 list
    s = t -> list.add(t)            s 0-4-0 list

    conclusion:
    - identical to situation 1, where the parameter(s) takes the role of the return value; each independently of the other

    SITUATION 3: neither return value, nor at least one parameter is @Independent
    (the return value links to the object, and at least one parameter links to the object)

    do both of 1 and 2, and take union. 0.0-4-0.1 and 0.0-4-0.0 may result in 0.0;0.1-4-0.0;0.1
    example??

    FIXME split method so that it can be called from MR as well
    */
    public static LambdaResult lambdaLinking(Runtime runtime, MethodInfo concreteMethod) {
        if (concreteMethod.methodBody().isEmpty()) {
            return new LambdaResult(List.of(), LinkedVariablesImpl.EMPTY);
        }
        List<Statement> statements = concreteMethod.methodBody().statements();
        Statement lastStatement = statements.get(statements.size() - 1);
        List<LinkedVariables> result = new ArrayList<>(concreteMethod.parameters().size() + 1);

        for (ParameterInfo pi : concreteMethod.parameters()) {
            VariableInfoContainer vic = VariableDataImpl.of(lastStatement).variableInfoContainerOrNull(pi.fullyQualifiedName());
            if (vic != null) {
                VariableInfo vi = vic.best();
                LinkedVariables lv = vi.linkedVariables();
                if (lv != null) {
                    result.add(lv);
                }
            }
        }
        if (concreteMethod.hasReturnValue()) {
            ReturnVariable returnVariable = new ReturnVariableImpl(concreteMethod);
            VariableInfoContainer vic = VariableDataImpl.of(lastStatement).variableInfoContainerOrNull(returnVariable.fullyQualifiedName());
            if (vic != null) {
                LinkedVariables lv = vic.best().linkedVariables();
                //  if (concreteMethod.parameters().isEmpty()) {
                if (lv != null) {
                    return new LambdaResult(result, lv);
                }
            }
            //  }
            // link to the input types rather than the output type, see also HCT.mapMethodToTypeIndices
            // Map<Indices, Indices> correctionMap = new HashMap<>();
          /*  // FIXME 1??
            correctionMap.put(new IndicesImpl(1), new IndicesImpl(0));
            LinkedVariables corrected = vi.linkedVariables().map(lv -> lv.correctTo(correctionMap));
            return new LambdaResult(result, corrected);*/// throw new UnsupportedOperationException();
        }
        return new LambdaResult(result, LinkedVariablesImpl.EMPTY);
    }

}
