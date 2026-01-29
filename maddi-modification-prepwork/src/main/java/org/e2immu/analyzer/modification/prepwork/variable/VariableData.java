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
import java.util.stream.Stream;

public interface VariableData extends Value {
    Set<String> knownVariableNames();

    boolean isKnown(String fullyQualifiedName);

    String knownVariableNamesToString();

    @org.e2immu.annotation.NotNull
    VariableInfo variableInfo(String fullyQualifiedName);

    default String indexOfDefinitionOrNull(Variable variable) {
        VariableInfoContainer vic = variableInfoContainerOrNull(variable.fullyQualifiedName());
        if (vic == null) return null;
        return vic.indexOfDefinition();
    }

    default VariableInfo variableInfo(Variable variable) {
        return variableInfo(variable, Stage.MERGE);
    }

    VariableInfo variableInfo(Variable variable, Stage stage);

    VariableInfo variableInfo(String fullyQualifiedName, Stage stage);

    VariableInfoContainer variableInfoContainerOrNull(String fullyQualifiedName);

    Stream<VariableInfoContainer> variableInfoContainerStream();

    default Iterable<VariableInfo> variableInfoIterable() {
        return variableInfoIterable(Stage.MERGE);
    }

    Iterable<VariableInfo> variableInfoIterable(Stage stage);

    default Stream<VariableInfo> variableInfoStream() {
        return variableInfoStream(Stage.MERGE);
    }

    Stream<VariableInfo> variableInfoStream(Stage stage);
}
