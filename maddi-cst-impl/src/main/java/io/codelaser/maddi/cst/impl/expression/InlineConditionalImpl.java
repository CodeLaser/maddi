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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Factory;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InlineConditionalImpl extends ExpressionImpl implements InlineConditional {

    private final Expression condition;
    private final Expression ifTrue;
    private final Expression ifFalse;
    private final ParameterizedType commonType;


    public InlineConditionalImpl(List<Comment> comments, Source source, Expression condition, Expression ifTrue,
                                 Expression ifFalse, ParameterizedType commonType) {
        super(comments, source, 1 + condition.complexity() + ifTrue.complexity() + ifFalse.complexity());
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
        this.commonType = commonType;
    }

    @Override
    public Expression withSource(Source source) {
        return new InlineConditionalImpl(comments(), source, condition, ifTrue, ifFalse, commonType);
    }

    public static final class Builder extends ElementImpl.Builder<InlineConditional.Builder> implements InlineConditional.Builder {
        private Expression condition;
        private Expression ifTrue;
        private Expression ifFalse;

        @Override
        public InlineConditional.Builder setIfTrue(Expression ifTrue) {
            this.ifTrue = ifTrue;
            return this;
        }

        @Override
        public InlineConditional.Builder setIfFalse(Expression ifFalse) {
            this.ifFalse = ifFalse;
            return this;
        }

        @Override
        public InlineConditional.Builder setCondition(Expression condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public InlineConditional build(Factory runtime) {
            return new InlineConditionalImpl(comments, source, condition, ifTrue, ifFalse,
                    runtime.commonType(ifTrue.parameterizedType(), ifFalse.parameterizedType()));
        }
    }

    @Override
    public Expression conditionOfInlineConditional() {
        return condition;
    }

    @Override
    public Expression ifFalse() {
        return ifFalse;
    }

    @Override
    public Expression condition() {
        return condition;
    }

    @Override
    public Expression ifTrue() {
        return ifTrue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineConditionalImpl that = (InlineConditionalImpl) o;
        return Objects.equals(condition, that.condition)
               && Objects.equals(ifTrue, that.ifTrue)
               && Objects.equals(ifFalse, that.ifFalse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, ifTrue, ifFalse);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(outputInParenthesis(qualification, precedence(), condition))
                .add(SymbolEnum.QUESTION_MARK)
                .add(outputInParenthesis(qualification, precedence(), ifTrue))
                .add(SymbolEnum.COLON)
                .add(outputInParenthesis(qualification, precedence(), ifFalse));
    }


    @Override
    public ParameterizedType parameterizedType() {
        return commonType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.TERNARY;
    }

    @Override
    public int order() {
        return condition.order();
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof InlineConditional other) {
            int c = condition.compareTo(other.condition());
            if (c == 0) {
                int d = ifTrue.compareTo(other.ifTrue());
                if (d == 0) {
                    return ifFalse.compareTo(other.ifFalse());
                }
                return d;
            }
            return c;
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            condition.visit(predicate);
            ifTrue.visit(predicate);
            ifFalse.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            condition.visit(visitor);
            ifTrue.visit(visitor);
            ifFalse.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendIntoFieldReferences) {
        return Stream.concat(condition.variables(descendIntoFieldReferences), Stream.concat(
                ifTrue.variables(descendIntoFieldReferences),
                ifFalse.variables(descendIntoFieldReferences)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(condition.typesReferenced(predicate),
                Stream.concat(ifTrue.typesReferenced(predicate), ifFalse.typesReferenced(predicate)));
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression tc = condition.translate(translationMap);
        Expression tt = ifTrue.translate(translationMap);
        Expression tf = ifFalse.translate(translationMap);
        if (tc == condition && tt == ifTrue && tf == ifFalse) return this;
        InlineConditional result = tc instanceof Negation negation
                ? new InlineConditionalImpl(comments(), source(), negation.expression(), tf, tt, commonType)
                : new InlineConditionalImpl(comments(), source(), tc, tt, tf, commonType);
        // Same termination guard as MethodCallImpl and AssignmentImpl: re-translate only while the result still
        // changes by value. The identity check above compares against this.condition/ifTrue/ifFalse, but every
        // pass rebuilds `result` from freshly translated children, so a sub-translation returning an
        // equal-but-new object would never satisfy it and the recursion would not terminate.
        // Note the Negation branch swaps ifTrue/ifFalse, so `result` can legitimately differ from `this` on the
        // first pass and be stable from the second: equals() settles that, identity cannot.
        Expression result2 = translationMap.translateAgain() && !this.equals(result)
                ? result.translate(translationMap) : result;
        return translationMap.postTranslationHandler(this, result2);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new InlineConditionalImpl(comments(), source(), condition.rewire(infoMap), ifTrue.rewire(infoMap),
                ifFalse.rewire(infoMap), commonType.rewire(infoMap));
    }
}
