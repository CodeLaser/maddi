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
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.Set;

public class VariableInfoImpl implements VariableInfo {
    public static final Property UNMODIFIED_VARIABLE = new PropertyImpl("unmodifiedVariable");

    public static final Property DOWNCAST_VARIABLE = new PropertyImpl("downcastVariable",
            ValueImpl.SetOfTypeInfoImpl.EMPTY);

    private Links linkedVariables;

    private final PropertyValueMap analysis = new PropertyValueMapImpl();

    private final Variable variable;
    private final Assignments assignments;
    private final Reads reads;
    private final boolean isVariableInClosure;

    public VariableInfoImpl(Variable variable, Assignments assignments, Reads reads, boolean isVariableInClosure) {
        this.variable = variable;
        this.assignments = assignments;
        this.reads = reads;
        this.isVariableInClosure = isVariableInClosure;
    }

    public void setLinkedVariables(Links linkedVariables) {
        assert linkedVariables != null;
        if (this.linkedVariables == null) {
            this.linkedVariables = linkedVariables;
        } else if (!this.linkedVariables.equals(linkedVariables)) {
            if (this.linkedVariables.overwriteAllowed(linkedVariables)) {
                this.linkedVariables = linkedVariables;
            } else {
                throw new UnsupportedOperationException("Not allowed to overwrite");
            }
        }
    }

    @Override
    public boolean isVariableInClosure() {
        return isVariableInClosure;
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
    public Set<TypeInfo> downcast() {
        return analysis.getOrDefault(DOWNCAST_VARIABLE, ValueImpl.SetOfTypeInfoImpl.EMPTY).typeInfoSet();
    }

    @Override
    public String toString() {
        return "VI[" + variable + "]";
    }
}
