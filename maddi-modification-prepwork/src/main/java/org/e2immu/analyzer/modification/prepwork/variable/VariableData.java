package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Set;
import java.util.stream.Stream;

public interface VariableData extends Value {
    Set<String> knownVariableNames();

    boolean isKnown(String fullyQualifiedName);

    String knownVariableNamesToString();

    @org.e2immu.annotation.NotNull
    VariableInfo variableInfo(String fullyQualifiedName);

    default VariableInfo variableInfo(Variable variable) {
        return variableInfo(variable, Stage.MERGE);
    }

    default String indexOfDefinitionOrNull(Variable variable) {
        VariableInfoContainer vic = variableInfoContainerOrNull(variable.fullyQualifiedName());
        if(vic == null) return null;
        return vic.indexOfDefinition();
    }

    VariableInfo variableInfo(Variable variable, Stage stage);

    VariableInfoContainer variableInfoContainerOrNull(String fullyQualifiedName);

    Stream<VariableInfoContainer> variableInfoContainerStream();

    Iterable<VariableInfo> variableInfoIterable();

    default Stream<VariableInfo> variableInfoStream() {
        return variableInfoStream(Stage.MERGE);
    }

    Stream<VariableInfo> variableInfoStream(Stage stage);
}
