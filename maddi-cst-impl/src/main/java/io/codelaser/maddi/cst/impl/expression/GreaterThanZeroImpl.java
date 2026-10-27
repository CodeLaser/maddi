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

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.output.element.Symbol;
import io.codelaser.maddi.cst.api.output.element.Text;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.util.IntUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GreaterThanZeroImpl extends ExpressionImpl implements GreaterThanZero {
    private final Expression expression;
    private final boolean allowEquals;
    private final ParameterizedType booleanPt;

    public GreaterThanZeroImpl(ParameterizedType booleanPt, Expression expression, boolean allowEquals) {
        this(List.of(), null, booleanPt, expression, allowEquals);
    }

    public GreaterThanZeroImpl(List<Comment> comments, Source source, ParameterizedType booleanPt, Expression expression, boolean allowEquals) {
        super(comments, source, 1 + expression.complexity());
        this.expression = expression;
        this.allowEquals = allowEquals;
        this.booleanPt = booleanPt;
    }

    @Override
    public Expression withSource(Source source) {
        return new GreaterThanZeroImpl(comments(), source, booleanPt, expression, allowEquals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GreaterThanZeroImpl that)) return false;
        return allowEquals == that.allowEquals && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, allowEquals);
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public boolean allowEquals() {
        return allowEquals;
    }

    public record XBImpl(Expression x, double b, boolean lessThan) implements XB {
    }

    @Override
    public XB extract(Runtime runtime) {
        Sum sumValue = expression.asInstanceOf(Sum.class);
        if (sumValue != null) {
            Double d = sumValue.numericPartOfLhs();
            if (d != null) {
                Expression v = sumValue.nonNumericPartOfLhs(runtime);
                Expression x;
                boolean lessThan;
                double b;
                if (v instanceof Negation ne) {
                    x = ne.expression();
                    lessThan = true;
                    if (IntUtil.isMathematicalInteger(d) && ne.parameterizedType().isMathematicallyInteger()) {
                        assert !allowEquals : "By convention, we store x < 4 rather than x <= 3";
                        b = d - 1;
                    } else {
                        b = d;
                    }
                } else {
                    x = v;
                    lessThan = false;
                    b = -d;
                }
                return new XBImpl(x, b, lessThan);
            }
        }
        Expression x;
        boolean lessThan;
        double d;
        if (expression instanceof Negation ne) {
            x = ne.expression();
            lessThan = true;
            if (!allowEquals && x.parameterizedType().isMathematicallyInteger()) {
                d = -1;
            } else {
                d = 0;
            }
        } else {
            x = expression;
            lessThan = false;
            d = 0;
        }
        return new XBImpl(x, d, lessThan);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanPt;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.RELATIONAL;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_GEQ0;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof BinaryOperator binary) {
            return -BinaryOperatorImpl.compareBinaryToGt0(binary, this);
        }
        if (!(expression instanceof GreaterThanZero)) throw new InternalCompareToException();

        int c = BinaryOperatorImpl.compareVariables(this, expression);
        if (c != 0) return c;
        return expression.compareTo(((GreaterThanZero) expression).expression());
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedExpression = expression.translate(translationMap);
        if (translatedExpression == expression) return this;
        Expression result = new GreaterThanZeroImpl(comments(), source(), booleanPt, translatedExpression, allowEquals);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expression.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        Symbol gt = SymbolEnum.binaryOperator(allowEquals ? ">=" : ">");
        Symbol lt = SymbolEnum.binaryOperator(allowEquals ? "<=" : "<");
        if (expression instanceof Sum sum) {
            if (sum.lhs() instanceof Numeric ln) {
                if (ln.doubleValue() < 0) {
                    // -1 -a >= 0 will be written as a <= -1
                    if (sum.rhs() instanceof Negation neg) {
                        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), neg.expression()))
                                .add(lt).add(sum.lhs().print(qualification));
                    }
                    // -1 + a >= 0 will be written as a >= 1
                    Text negNumber = new TextImpl(DoubleConstantImpl.formatNumber(-ln.doubleValue(), ln.getClass()));
                    return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), sum.rhs()))
                            .add(gt).add(negNumber);
                } else if (sum.rhs() instanceof Negation neg) {
                    // 1 + -a >= 0 will be written as a <= 1
                    return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), neg.expression()))
                            .add(lt).add(sum.lhs().print(qualification));
                }
            }
            // according to sorting, the rhs cannot be numeric

            // -x + a >= 0 will be written as a >= x
            if (sum.lhs() instanceof Negation neg && !(sum.rhs() instanceof Negation)) {
                return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), sum.rhs()))
                        .add(gt).add(outputInParenthesis(qualification, precedence(), neg.expression()));
            }
            // a + -x >= 0 will be written as a >= x
            if (sum.rhs() instanceof Negation neg && !(sum.lhs() instanceof Negation)) {
                return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), sum.lhs()))
                        .add(gt).add(outputInParenthesis(qualification, precedence(), neg.expression()));
            }
        } else if (expression instanceof Negation neg) {
            return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), neg.expression()))
                    .add(lt).add(new TextImpl("0"));
        }
        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), expression))
                .add(gt).add(new TextImpl("0"));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return expression.typesReferenced(predicate);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new GreaterThanZeroImpl(comments(), source(), booleanPt, expression.rewire(infoMap), allowEquals);
    }
}
