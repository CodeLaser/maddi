package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface LinkedVariables extends Iterable<Map.Entry<Variable, LV>>, Value {

    interface ListOfLinkedVariables extends Value {
        List<LinkedVariables> list();
    }

    Stream<Variable> assignedOrDependentVariables();

    Stream<Variable> dependentVariables();

    Set<Variable> directAssignmentVariables();

    boolean identicalStaticallyAssigned(LinkedVariables linkedVariables);

    boolean isDelayed();

    boolean isNotYetSet();

    LinkedVariables map(Function<LV, LV> function);

    LinkedVariables removeStaticallyAssigned();

    Set<Variable> staticallyAssigned();

    Stream<Variable> staticallyAssignedStream();

    /*
    goal of the 'minimum' parameter:

    x = new X(b); expression b has linked variables b,0 when b is a variable expression
    the variable independent gives the independence of the first parameter of the constructor X(b)

    if DEPENDENT, then x is linked in an accessible way to b (maybe it stores b); max(0,0)=0
    if INDEPENDENT_1, then changes to b may imply changes to the hidden content of x; max(0,1)=1

    if the expression is e.g. c,1, as in new B(c)

    if DEPENDENT, then x is linked in an accessible way to an object b which is content linked to c
    changes to c have an impact on the hidden content of b, which is linked in an accessible way
    (leaving the hidden content hidden?) to x  max(1,0)=1
    if INDEPENDENT_1, then x is hidden content linked to b is hidden content linked to c, max(1,1)=1
     */
    LinkedVariables merge(LinkedVariables other, LV minimum);

    LinkedVariables merge(LinkedVariables other);

    boolean isEmpty();

    boolean contains(Variable variable);

    Stream<Map.Entry<Variable, LV>> stream();

    LinkedVariables maximum(LV other);

    Stream<Variable> variableStream();

    Stream<Variable> variablesAssigned();

    LinkedVariables translate(TranslationMap translationMap);

    LinkedVariables changeToDelay();

    LinkedVariables remove(Set<Variable> reassigned);

    LinkedVariables remove(Predicate<Variable> remove);

    LinkedVariables changeNonStaticallyAssignedToDelay();

    LinkedVariables changeAllTo(LV value);

    LinkedVariables changeAllToUnlessDelayed(LV value);

    LV value(Variable variable);

    Map<Variable, LV> variables();

    // for assertions
    boolean compatibleWith(HiddenContentSelector hcs);

}
