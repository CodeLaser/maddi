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

package io.codelaser.maddi.modification.prepwork.variable.impl;

import io.codelaser.maddi.modification.prepwork.variable.Link;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.ReturnVariable;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.PropertyValueMapImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

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
        } else if (this.linkedVariables.equals(linkedVariables)) {
            // Links equality is primary-only: with the same primary, a recomputed value that differs in CONTENT
            // replaced nothing, so a variable's statement-level links stayed at the method's FIRST link computation
            // for the rest of the run -- every later pass computed and discarded the settled links, and the
            // statement-data readers (ShadowModificationPass, FieldAnalyzerImpl, the link computer's own
            // previous-statement reads) worked from the first pass (EC O4: 'iterator ↦ -' kept over
            // 'iterator.§m ☷{remove} this.§m'). The last computation wins.
            if (!sameContent(this.linkedVariables, linkedVariables)) this.linkedVariables = linkedVariables;
        } else {
            if (this.linkedVariables.overwriteAllowed(linkedVariables)) {
                this.linkedVariables = linkedVariables;
            } else {
                throw new UnsupportedOperationException("Not allowed to overwrite");
            }
        }
    }

    private static boolean sameContent(Links l1, Links l2) {
        return contentKey(l1).equals(contentKey(l2));
    }

    private static java.util.Set<java.util.List<Object>> contentKey(Links links) {
        java.util.Set<java.util.List<Object>> set = new java.util.HashSet<>();
        for (Link link : links) {
            set.add(java.util.List.of(link.from(), link.linkNature(), link.to(), link.mediated()));
        }
        return set;
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
