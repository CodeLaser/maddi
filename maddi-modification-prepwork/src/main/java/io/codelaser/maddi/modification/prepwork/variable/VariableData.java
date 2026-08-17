/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface VariableData extends Value {
    Set<String> knownVariableNames();

    boolean isKnown(String fullyQualifiedName);

    default String knownVariableNamesToString() {
        return knownVariableNames().stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
    }

    default VariableInfo variableInfo(String fullyQualifiedName) {
        return variableInfoContainerOrNull(fullyQualifiedName).best();
    }

    default String indexOfDefinitionOrNull(Variable variable) {
        VariableInfoContainer vic = variableInfoContainerOrNull(variable.fullyQualifiedName());
        if (vic == null) return null;
        return vic.indexOfDefinition();
    }

    default VariableInfo variableInfo(Variable variable) {
        return variableInfo(variable, Stage.MERGE);
    }

    default VariableInfo variableInfo(Variable variable, Stage stage) {
        return variableInfoContainerOrNull(variable.fullyQualifiedName()).best(stage);
    }

    default VariableInfo variableInfo(String fullyQualifiedName, Stage stage) {
        return variableInfoContainerOrNull(fullyQualifiedName).best(stage);
    }

    VariableInfoContainer variableInfoContainerOrNull(String fullyQualifiedName);

    Stream<VariableInfoContainer> variableInfoContainerStream();

    default Iterable<VariableInfo> variableInfoIterable() {
        return variableInfoIterable(Stage.MERGE);
    }

    default Iterable<VariableInfo> variableInfoIterable(Stage stage) {
        Stream<VariableInfo> stream = variableInfoContainerStream().map(vic -> vic.best(stage));
        return stream::iterator;
    }

    default Stream<VariableInfo> variableInfoStream() {
        return variableInfoStream(Stage.MERGE);
    }

    default Stream<VariableInfo> variableInfoStream(Stage stage) {
        return variableInfoContainerStream().map(vic -> vic.best(stage));
    }
}
