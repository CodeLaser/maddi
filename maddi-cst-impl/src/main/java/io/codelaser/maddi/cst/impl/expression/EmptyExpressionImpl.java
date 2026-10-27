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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.EmptyExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Predefined;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EmptyExpressionImpl extends ExpressionImpl implements EmptyExpression {
    public static final String EMPTY_EXPRESSION = "<empty>";
    public static final String DEFAULT_EXPRESSION = "<default>"; // negation of the disjunction of all earlier conditions
    public static final String FINALLY_EXPRESSION = "<finally>"; // always true condition
    public static final String NO_RETURN_VALUE = "<no return value>"; // assigned to void methodsprivate final String msg;

    private final String msg;
    private final ParameterizedType parameterizedType;

    public EmptyExpressionImpl(Predefined predefined, String msg) {
        super(1);
        this.msg = msg;
        parameterizedType = predefined.voidParameterizedType();
    }

    private EmptyExpressionImpl(String msg, ParameterizedType parameterizedType, Source source) {
        super(null, source, 1);
        this.msg = msg;
        this.parameterizedType = parameterizedType;
    }

    /**
     * A copy at [source]: a front end's placeholder for code it could not convert keeps the range of that code, so a
     * consumer can tell what the CST does not represent.
     */
    @Override
    public Expression withSource(Source source) {
        return new EmptyExpressionImpl(msg, parameterizedType, source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmptyExpressionImpl that = (EmptyExpressionImpl) o;
        return Objects.equals(msg, that.msg);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(msg);
    }

    @Override
    public String msg() {
        return msg;
    }

    @Override
    public boolean isDefaultExpression() {
        return DEFAULT_EXPRESSION.equals(msg);
    }

    @Override
    public boolean isNoReturnValue() {
        return NO_RETURN_VALUE.equals(msg);
    }

    @Override
    public boolean isNoExpression() {
        return EMPTY_EXPRESSION.equals(msg);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.TOP;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_EMPTY_EXPRESSION;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof EmptyExpression ee) {
            return msg.compareTo(ee.msg());
        }
        throw new InternalCompareToException();
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
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(msg));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        return Stream.empty();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        return translationMap.translateExpression(this);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return this;
    }
}
