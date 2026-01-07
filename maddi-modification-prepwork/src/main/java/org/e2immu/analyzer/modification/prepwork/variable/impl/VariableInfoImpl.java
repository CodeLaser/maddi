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

package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableInfoImpl implements VariableInfo {
    public static final Property UNMODIFIED_VARIABLE = new PropertyImpl("unmodifiedVariable");

    public static final Property DOWNCAST_VARIABLE = new PropertyImpl("downcastVariable", ValueImpl.SetOfTypeInfoImpl.EMPTY);

    private Links linkedVariables;

    private final PropertyValueMap analysis = new PropertyValueMapImpl();

    private final Variable variable;
    private final Assignments assignments;
    private final Reads reads;
    private final VariableData variableInClosure;

    public VariableInfoImpl(Variable variable, Assignments assignments, Reads reads, VariableData variableInClosure) {
        this.variable = variable;
        this.assignments = assignments;
        this.reads = reads;
        this.variableInClosure = variableInClosure;
    }

    public boolean setLinkedVariables(Links linkedVariables) {
        assert linkedVariables != null;
        if (this.linkedVariables == null) {
            this.linkedVariables = linkedVariables;
            return true;
        }
        if (this.linkedVariables.equals(linkedVariables)) {
            return false;
        }
        if (this.linkedVariables.overwriteAllowed(linkedVariables)) {
            this.linkedVariables = linkedVariables;
            return true;
        }
        throw new UnsupportedOperationException("Not allowed to overwrite");
    }

    @Override
    public Variable variable() {
        return variable;
    }

    @Override
    public Links linkedVariables() {
        return linkedVariables;
    }

    @Override
    public Links linkedVariablesOrEmpty() {
        return linkedVariables == null ? LinksImpl.EMPTY : linkedVariables;
    }

    @Override
    public PropertyValueMap analysis() {
        return analysis;
    }

    @Override
    public Assignments assignments() {
        return assignments;
    }

    @Override
    public Reads reads() {
        return reads;
    }

    @Override
    public boolean hasBeenDefined(String index) {
        if (variable instanceof LocalVariable || variable instanceof ReturnVariable) {
            return assignments.hasAValueAt(index);
        }
        return true;
    }

    @Override
    public boolean isUnmodified() {
        return analysis.getOrDefault(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public VariableData variableInfoInClosure() {
        return variableInClosure;
    }

    @Override
    public String toString() {
        return "VI[" + variable + "]";
    }
}
