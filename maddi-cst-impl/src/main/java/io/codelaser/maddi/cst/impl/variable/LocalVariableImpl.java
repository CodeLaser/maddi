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

package io.codelaser.maddi.cst.impl.variable;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.QualifiedNameImpl;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LocalVariableImpl extends VariableImpl implements LocalVariable {
    public static final String UNNAMED = ":";

    private final Expression assignmentExpression;
    private final String name;
    // one per declaration, carried by every copy: see LocalVariable.declarationId (CodeLaser/maddi#127)
    private final Object declarationId;

    public LocalVariableImpl(ParameterizedType parameterizedType, Expression assignmentExpression) {
        this(UNNAMED, parameterizedType, assignmentExpression, new Object());
    }

    public LocalVariableImpl(String name, ParameterizedType parameterizedType, Expression assignmentExpression) {
        this(name, parameterizedType, assignmentExpression, new Object());
    }

    private LocalVariableImpl(String name, ParameterizedType parameterizedType, Expression assignmentExpression,
                              Object declarationId) {
        super(parameterizedType);
        this.name = Objects.requireNonNull(name);
        this.assignmentExpression = assignmentExpression;
        this.declarationId = declarationId;
    }

    @Override
    public Object declarationId() {
        return declarationId;
    }

    /*
     NOTE: a local variable's FQN is its bare name, so equals/hashCode (VariableImpl, FQN-based) make two
     same-named locals from different contexts EQUAL. This is safe under two invariants the analyzers maintain:
     (1) within a method, scope-disjoint same-named locals never coexist in an analysis structure (variables are
         cleared from the link graph at block end), and
     (2) linking/analysis graphs are per-method, so cross-method name collisions cannot meet.
     Do not key a CROSS-method structure by Variable without qualifying locals first. The name-based hashCode is
     deterministic across JVM runs (String hash), which analysis determinism relies on.
     A caller that needs the declaration rather than the name keys on declarationId() (#127).
     */
    @Override
    public String fullyQualifiedName() {
        return name;
    }

    @Override
    public boolean isUnnamed() {
        return UNNAMED.equals(name);
    }

    @Override
    public LocalVariable withAssignmentExpression(Expression expression) {
        return new LocalVariableImpl(name, parameterizedType(), expression, declarationId);
    }

    @Override
    public LocalVariable withName(String name) {
        return new LocalVariableImpl(name, parameterizedType(), assignmentExpression, declarationId);
    }

    @Override
    public LocalVariable withType(ParameterizedType type) {
        return new LocalVariableImpl(name, type, assignmentExpression, declarationId);
    }

    @Override
    public Expression assignmentExpression() {
        return assignmentExpression;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public int complexity() {
        return 2;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeVariable(this) && assignmentExpression != null) {
            assignmentExpression.visit(visitor);
        }
        visitor.afterVariable(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String name = qualification.isFullyQualifiedNames() ? fullyQualifiedName() : simpleName();
        return new OutputBuilderImpl().add(new QualifiedNameImpl(name, null, QualifiedNameImpl.Required.NEVER));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of(this);
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> test, DetailedSources detailedSources) {
        return parameterizedType().typesReferenced(TypeReferenceNature.IMPLICIT, detailedSources);
    }

    @Override
    public LocalVariable translate(TranslationMap translationMap) {
        Variable direct = translationMap.translateVariable(this);
        if (direct != this && direct instanceof LocalVariable lv) return lv;
        // the declaration and every reference are this one object: one translation, not one each
        LocalVariable before = translationMap.translatedLocalVariable(this);
        if (before != null) return before;
        Expression tex = assignmentExpression == null ? null : assignmentExpression.translate(translationMap);
        ParameterizedType type = translationMap.translateType(parameterizedType());
        if (tex != assignmentExpression || type != parameterizedType()) {
            LocalVariable translated = new LocalVariableImpl(name, type, tex, declarationId);
            translationMap.rememberTranslatedLocalVariable(this, translated);
            return translated;
        }
        return this;
    }

    @Override
    public Variable rewire(InfoMapView infoMap) {
        return new LocalVariableImpl(name, parameterizedType().rewire(infoMap), assignmentExpression == null ? null
                : assignmentExpression.rewire(infoMap), declarationId);
    }
}
