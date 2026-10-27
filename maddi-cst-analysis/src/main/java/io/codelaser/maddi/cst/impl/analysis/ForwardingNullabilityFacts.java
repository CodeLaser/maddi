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
package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.Objects;

/**
 * {@link NullabilityFacts} that answer as {@code delegate} does: a base for facts that change one answer or add a
 * concern of their own (a printer's, beside nullability).
 */
public class ForwardingNullabilityFacts implements NullabilityFacts {

    protected final NullabilityFacts delegate;

    public ForwardingNullabilityFacts(NullabilityFacts delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public ParameterizedType field(FieldInfo fieldInfo) {
        return delegate.field(fieldInfo);
    }

    @Override
    public ParameterizedType parameter(ParameterInfo parameterInfo) {
        return delegate.parameter(parameterInfo);
    }

    @Override
    public ParameterizedType returnType(MethodInfo methodInfo) {
        return delegate.returnType(methodInfo);
    }

    @Override
    public ParameterizedType reassignedParameter(ParameterInfo parameterInfo) {
        return delegate.reassignedParameter(parameterInfo);
    }

    @Override
    public ParameterizedType local(MethodInfo method, Element declaration, LocalVariable variable) {
        return delegate.local(method, declaration, variable);
    }

    @Override
    public boolean assertedAtDeclaration(MethodInfo method, Element declaration, LocalVariable variable) {
        return delegate.assertedAtDeclaration(method, declaration, variable);
    }

    @Override
    public boolean unobservedBeforeDereference(MethodInfo method, Element declaration, LocalVariable variable) {
        return delegate.unobservedBeforeDereference(method, declaration, variable);
    }

    @Override
    public boolean nonNullAt(Statement statement, Variable variable) {
        return delegate.nonNullAt(statement, variable);
    }

    @Override
    public boolean nonNullAt(Expression expression, Variable variable) {
        return delegate.nonNullAt(expression, variable);
    }

    @Override
    public boolean callResultNonNull(Expression call) {
        return delegate.callResultNonNull(call);
    }

    @Override
    public boolean preconditionNonNull(ParameterInfo parameterInfo) {
        return delegate.preconditionNonNull(parameterInfo);
    }

    @Override
    public boolean rejectsNull(ParameterInfo parameterInfo) {
        return delegate.rejectsNull(parameterInfo);
    }

    @Override
    public boolean nonNullTypeParameter(io.codelaser.maddi.cst.api.info.TypeParameter typeParameter) {
        return delegate.nonNullTypeParameter(typeParameter);
    }

    @Override
    public boolean nullabilityAnalysed() {
        return delegate.nullabilityAnalysed();
    }
}
