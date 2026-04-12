package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// variant on EquivalenceGroup, to be used for equivalent SharedVariable objects
public class SharedVariables {
    private final Runtime runtime;
    private final Map<String, SharedVariable> sharedVariablesByName = new HashMap<>();
    private final Map<Variable, SharedVariable> memberToGroup = new HashMap<>();
    private final VariableTranslationMap variableTranslationMap;

    public SharedVariables(Runtime runtime) {
        this.runtime = runtime;
        variableTranslationMap = new VariableTranslationMap(runtime);
    }

    public boolean isAssignedFrom(Variable from, Variable to) {
        SharedVariable sv1 = memberToGroup.get(from);
        SharedVariable sv2 = memberToGroup.get(to);
        if (sv1 == null && sv2 == null) {
            create(from, to);
            return true;
        }
        if (sv1 == sv2) {
            return false; // already in the same group
        }
        if (sv1 == null) {
            add(sv2, from);
        }
        if (sv2 == null) {
            add(sv1, to);
        }
        // merge 2 groups
        merge(sv1, sv2, from, to);
        return true;
    }

    public Variable translateForward(Variable variable) {
        return variableTranslationMap.translateVariableRecursively(variable);
    }

    public String print(Function<Variable, String> variablePrinter) {
        return sharedVariablesByName.entrySet().stream()
                .map(e -> e.getKey() + ": "
                          + e.getValue().variables().stream().sorted(Variable::compareTo)
                                  .map(variablePrinter)
                                  .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    public void remove(Set<Variable> variables) {
        memberToGroup.keySet().removeAll(variables);
        sharedVariablesByName.values().forEach(g -> g.removeAll(variables));
    }

    private void create(Variable referenceVariable, Variable firstAssignedTo) {
        SharedVariable sv = new SharedVariable(referenceVariable.simpleName(), referenceVariable.parameterizedType(),
                runtime);
        sharedVariablesByName.put(sv.fullyQualifiedName(), sv);
        add(sv, referenceVariable);
        add(sv, firstAssignedTo);
    }

    private void add(SharedVariable sharedVariable, Variable variable) {
        sharedVariable.add(variable);
        memberToGroup.put(variable, sharedVariable);
        variableTranslationMap.put(variable, sharedVariable);
    }

    private void merge(SharedVariable sv1, SharedVariable sv2, Variable from, Variable to) {
        throw new UnsupportedOperationException("NYI");
    }

}
