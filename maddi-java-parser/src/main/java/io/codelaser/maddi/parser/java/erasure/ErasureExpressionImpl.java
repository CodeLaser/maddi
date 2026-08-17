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

package io.codelaser.maddi.parser.java.erasure;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.inspection.api.parser.ErasedExpression;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ErasureExpressionImpl implements ErasedExpression, Expression {
    protected final Runtime runtime;
    protected final Source source;

    protected ErasureExpressionImpl(Runtime runtime, Source source) {
        this.runtime = runtime;
        this.source = source;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return runtime.objectParameterizedType();
    }

    @Override
    public int compareTo(Expression o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int complexity() {
        return 10;
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeExpression(this);
        visitor.afterExpression(this);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        throw new UnsupportedOperationException();
    }
}
