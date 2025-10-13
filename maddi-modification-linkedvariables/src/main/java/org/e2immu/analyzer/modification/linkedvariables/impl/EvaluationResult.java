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

package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class EvaluationResult {

    public static final EvaluationResult EMPTY = new Builder().build();

    private final LinkedVariables linkedVariables;
    // values accumulated as we evaluate the expression
    private final StaticValues staticValues;

    private final Map<Variable, LinkedVariables> links;
    // other assignments accumulated along the way, as "side effects"
    private final Map<Variable, StaticValues> assignments;

    private final Set<Variable> modified;
    private final Map<FieldReference, Boolean> modifiedFunctionalComponents;
    private final Map<Variable, Set<ParameterizedType>> casts;

    private EvaluationResult(LinkedVariables linkedVariables,
                             Map<Variable, LinkedVariables> links,
                             Set<Variable> modified,
                             StaticValues staticValues,
                             Map<Variable, StaticValues> assignments,
                             Map<FieldReference, Boolean> modifiedFunctionalComponents,
                             Map<Variable, Set<ParameterizedType>> casts) {
        this.linkedVariables = linkedVariables;
        this.links = links;
        assert ensureNoIdentityLinks(this.links);
        this.modified = modified;
        this.staticValues = staticValues;
        this.assignments = assignments;
        this.modifiedFunctionalComponents = modifiedFunctionalComponents;
        this.casts = casts;
    }

    private static boolean ensureNoIdentityLinks(Map<Variable, LinkedVariables> links) {
        return links.entrySet().stream()
                .noneMatch(e -> e.getValue().variableStream()
                        .anyMatch(v -> v.equals(e.getKey())));
    }

    /*
    SV E=expression
    Assignment expression.field=value

    -> SV E=expression this.field=value

    IMPORTANT: very similar code is already in the builder detection code in StaticValuesHelper.checkCaseForBuilder.
    However, over there we have a chance to change the value of "this" to the correct one for propagation.
    Here, we simply collect.
     */
    public StaticValues gatherAllStaticValues(Runtime runtime) {
        Map<Variable, Expression> updatedValueMap = new HashMap<>();
        if (staticValues.values() != null) updatedValueMap.putAll(staticValues.values());
        // now copy from assignments, using translation
        if (staticValues.expression() != null) {
            TypeInfo bestType = staticValues.expression().parameterizedType().bestTypeInfo();
            if (bestType != null && !assignments.isEmpty()) {
                Variable thisVar = runtime.newThis(staticValues.expression().parameterizedType());
                VariableExpression thisVarVe = runtime.newVariableExpressionBuilder().setVariable(thisVar)
                        .setSource(staticValues.expression().source()).build();
                TranslationMap tm = runtime.newTranslationMapBuilder().put(staticValues.expression(), thisVarVe).build();
                for (Map.Entry<Variable, StaticValues> entry : assignments.entrySet()) {
                    VariableExpression variableExpression = runtime.newVariableExpression(entry.getKey());
                    Expression translatedKey = variableExpression.translate(tm);
                    if (translatedKey != variableExpression && entry.getValue().expression() != null) {
                        updatedValueMap.put(((VariableExpression) translatedKey).variable(), entry.getValue().expression());
                    }
                }
            }
        }
        return new StaticValuesImpl(staticValues.type(), staticValues.expression(), false, updatedValueMap);
    }

    static class Builder {
        private LinkedVariables linkedVariables = LinkedVariablesImpl.EMPTY;
        private StaticValues staticValues = StaticValuesImpl.NONE;
        private final Map<Variable, LinkedVariables> links = new HashMap<>();
        private final Map<Variable, StaticValues> assignments = new HashMap<>();
        private final Set<Variable> modified = new HashSet<>();
        private final Map<FieldReference, Boolean> modifiedFunctionalComponents = new HashMap<>();
        private final Map<Variable, Set<ParameterizedType>> casts = new HashMap<>();

        @Fluent
        Builder merge(EvaluationResult evaluationResult) {
            if (evaluationResult == EMPTY) return this;
            linkedVariables = evaluationResult.linkedVariables;
            staticValues = evaluationResult.staticValues;

            evaluationResult.links.forEach((v, lv) -> links.merge(v, lv, LinkedVariables::merge));
            assert ensureNoIdentityLinks(this.links);
            evaluationResult.assignments.forEach((v, sv) -> assignments.merge(v, sv, StaticValues::merge));
            modified.addAll(evaluationResult.modified);
            modifiedFunctionalComponents.putAll(evaluationResult.modifiedFunctionalComponents);
            evaluationResult.casts.forEach((v, set) ->
                    casts.computeIfAbsent(v, vv -> new HashSet<>()).addAll(set));
            return this;
        }

        @Fluent
        Builder mergeLinkedVariablesOfExpression(LinkedVariables lv) {
            linkedVariables = linkedVariables.merge(lv);
            return this;
        }

        @Fluent
        Builder setLinkedVariables(LinkedVariables linkedVariables) {
            assert linkedVariables != null;
            this.linkedVariables = linkedVariables;
            return this;
        }

        @Fluent
        public Builder setStaticValues(StaticValues staticValues) {
            assert staticValues != null;
            this.staticValues = staticValues;
            return this;
        }

        @Fluent
        Builder merge(Variable variable, LinkedVariables lv) {
            assert variable != null;
            assert lv != null;
            LinkedVariables lvWithout = lv.remove(v -> v.equals(variable));
            if (!lvWithout.isEmpty()) {
                this.links.merge(variable, lvWithout, LinkedVariables::merge);
                assert ensureNoIdentityLinks(this.links);
            }
            return this;
        }

        @Fluent
        Builder merge(Variable variable, StaticValues sv) {
            assert variable != null;
            assert sv != null;
            if (!sv.isEmpty()) {
                this.assignments.merge(variable, sv, StaticValues::merge);
            }
            return this;
        }

        @Fluent
        Builder addModified(Variable variable) {
            modified.add(variable);
            return this;
        }

        @Fluent
        Builder addModifiedFunctionalInterfaceComponent(FieldReference fieldReference, boolean propagateOrFailOnModified) {
            modifiedFunctionalComponents.put(fieldReference, propagateOrFailOnModified);
            return this;
        }

        @Fluent
        Builder addCast(Variable variable, ParameterizedType type) {
            casts.computeIfAbsent(variable, v -> new HashSet<>()).add(type);
            return this;
        }

        EvaluationResult build() {
            return new EvaluationResult(linkedVariables, Map.copyOf(links), Set.copyOf(modified),
                    staticValues, Map.copyOf(assignments), Map.copyOf(modifiedFunctionalComponents), Map.copyOf(casts));
        }

        @Override
        public String toString() {
            return "Builder{" +
                   "linkedVariables=" + linkedVariables +
                   ", staticValues=" + staticValues +
                   ", links=" + links +
                   ", assignments=" + assignments +
                   ", modified=" + modified +
                   ", modifiedFunctionalComponents=" + modifiedFunctionalComponents +
                   '}';
        }
    }

    public LinkedVariables linkedVariables() {
        return linkedVariables;
    }

    public Map<Variable, LinkedVariables> links() {
        return links;
    }

    public Map<Variable, StaticValues> assignments() {
        return assignments;
    }

    public StaticValues staticValues() {
        return staticValues;
    }

    public Set<Variable> modified() {
        return modified;
    }

    public Map<FieldReference, Boolean> modifiedFunctionalComponents() {
        return modifiedFunctionalComponents;
    }

    public Map<Variable, Set<ParameterizedType>> casts() {
        return casts;
    }
}
