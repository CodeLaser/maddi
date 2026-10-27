/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.impl.parser;

import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.inspection.api.parser.VariableContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableContextImpl implements VariableContext {
    private final VariableContext parentContext;

    public VariableContextImpl() {
        this(null, new HashMap<>());
    }

    public VariableContextImpl(VariableContext parentContext) {
        this(parentContext, new HashMap<>());
    }

    private VariableContextImpl(VariableContext parentContext, Map<String, FieldReference> staticallyImportedFields) {
        this.fields = new HashMap<>(staticallyImportedFields);
        this.parentContext = parentContext;
    }

    private final Map<String, FieldReference> fields;
    private final Map<String, LocalVariable> localVars = new HashMap<>();
    private final Map<String, ParameterInfo> parameters = new HashMap<>();

    @Override
    public Variable get(String name, boolean complain) {
        Variable variable = localVars.get(name);
        if (variable != null) return variable;
        variable = parameters.get(name);
        if (variable != null) return variable;
        variable = fields.get(name);
        if (variable != null) return variable;
        if (parentContext != null) {
            variable = parentContext.get(name, false);
        }
        if (variable == null && complain) {
            throw new RuntimeException("Unknown variable in context: '" + name + "'");
        }
        return variable;
    }

    /**
     * we'll add them in the correct order, so no overwriting!
     *
     * @param variable the variable to be added
     */
    @Override
    public void add(FieldReference variable) {
        String name = variable.simpleName();
        if (!fields.containsKey(name)) {
            fields.put(name, variable);
        }
    }

    @Override
    public void add(ParameterInfo variable) {
        parameters.put(variable.simpleName(), variable);
    }

    @Override
    public void add(LocalVariable variable) {
        localVars.put(variable.simpleName(), variable);
    }

    @Override
    public VariableContext newEmpty() {
        return new VariableContextImpl();
    }

    public void addAll(List<LocalVariable> localVariables) {
        localVariables.forEach(lvr -> {
            localVars.put(lvr.simpleName(), lvr);
        });
    }

    @Override
    public VariableContext newVariableContext() {
        return new VariableContextImpl(this);
    }

    @Override
    public String toString() {
        return "VariableContext{" +
               parentContext +
               ", local " + localVars.keySet() + ", fields " + fields.keySet() +
               '}';
    }
}
