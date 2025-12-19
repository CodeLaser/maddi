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

import org.e2immu.analyzer.modification.prepwork.variable.impl.Assignments;
import org.e2immu.analyzer.modification.prepwork.variable.impl.Reads;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.variable.Variable;

public interface VariableInfo {

    // FIRST PASS DATA, assigned in the prep-work stage during construction

    // for later
    //Set<Integer> readAtStatementTimes();

    /*
    Information about assignments to this variable.
     */
    Assignments assignments();

    default boolean isVariableInClosure() {
        return variableInfoInClosure() != null;
    }

    Links linkedVariablesOrEmpty();

    VariableData variableInfoInClosure();

    Reads reads();

    /*
    is there a definite value for this variable at the end of this statement?

    when this is the return variable:
    return true when the method's flow cannot go beyond index.
    e.g. true when at index, there is a return or throws, or an if in which both blocks exit.
     */
    boolean hasBeenDefined(String index);

    Variable variable();

    // SECOND PASS DATA, assigned in the modification stage; eventually immutable

    Links linkedVariables();

    PropertyValueMap analysis();

    // for later
    //int modificationTime();

    boolean isUnmodified();

    // for testing only
    default boolean isModified() {
        return !isUnmodified();
    }
}
