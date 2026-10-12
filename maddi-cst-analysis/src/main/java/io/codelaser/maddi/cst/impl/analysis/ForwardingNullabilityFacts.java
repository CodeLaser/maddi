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
    public boolean nullabilityAnalysed() {
        return delegate.nullabilityAnalysed();
    }
}
