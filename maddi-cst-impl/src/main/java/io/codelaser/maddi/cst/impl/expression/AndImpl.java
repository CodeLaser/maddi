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

import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.And;
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
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


public class AndImpl extends ExpressionImpl implements And {

    private final List<Expression> expressions;
    private final ParameterizedType booleanPt;

    public AndImpl(Predefined predefined, List<Expression> expressions) {
        this(predefined.booleanParameterizedType(), expressions);
    }

    private AndImpl(ParameterizedType booleanPt, List<Expression> expressions) {
        super(1 + expressions.stream().mapToInt(Expression::complexity).sum());
        // copy at the call-site end: the Builder already commits List.copyOf, but these public
        // constructors took the caller's list, leaving the field an unprovable container and the
        // type capped at @FinalFields -- a weaker verdict than none at all (see the eventual docs)
        this.expressions = List.copyOf(expressions);
        this.booleanPt = booleanPt;
        assert expressions.size() > 1;
    }

    public AndImpl(List<Comment> comments, Source source, ParameterizedType booleanPt, List<Expression> expressions) {
        super(comments, source, 1 + expressions.stream().mapToInt(Expression::complexity).sum());
        // copy at the call-site end: the Builder already commits List.copyOf, but these public
        // constructors took the caller's list, leaving the field an unprovable container and the
        // type capped at @FinalFields -- a weaker verdict than none at all (see the eventual docs)
        this.expressions = List.copyOf(expressions);
        this.booleanPt = booleanPt;
        assert expressions.size() > 1;
    }

    @Override
    public Expression withSource(Source source) {
        return new AndImpl(comments(), source, booleanPt, expressions);
    }

    @Override
    public List<Expression> expressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        And andValue = (And) o;
        // complexity is a structural invariant cached at construction: an exact O(1) reject before the
        // recursive descent — the boolean simplifier compares big conjunctions constantly
        if (complexity() != andValue.complexity()) return false;
        return expressions.equals(andValue.expressions());
    }

    // @IgnoreModifications (road §050): idempotent memo state, disclaimed -- the VariableImpl.cachedHash
    // precedent; lazily cached because the recursive recompute dominated profiles. Without the disclaimer
    // the slot's write makes hashCode() modifying, and this type can never be (eventually) immutable.
    // NOT an IntMemo, deliberately: see the note on VariableImpl.cachedHash for what a wrapper costs here.
    @IgnoreModifications
    private int hash;

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = Objects.hash(expressions);
            // 0 is the unset sentinel, so a genuine 0 would recompute on EVERY call -- the memo silently
            // stops being a memo for that one expression. Remap it, as VariableImpl.hashCode does.
            if (h == 0) h = 1;
            hash = h;
        }
        return h;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        Precedence precedence = precedence();
        return new OutputBuilderImpl()
                .add(expressions.stream().map(e -> outputInParenthesis(qualification, precedence, e))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.LOGICAL_AND)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return expressions.stream().flatMap(expression -> expression.typesReferenced(predicate));
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanPt;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.LOGICAL_AND;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_AND;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        And andValue = (And) expression;
        return ListUtil.compare(expressions, andValue.expressions());
    }


    @Override
    public Stream<Variable> variables(DescendMode descendIntoFieldReferences) {
        return expressions.stream().flatMap(v -> v.variables(descendIntoFieldReferences));
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            expressions.forEach(v -> v.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            expressions.forEach(e -> e.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;
        List<Expression> translatedExpressions = expressions.isEmpty() ? expressions : expressions.stream()
                .map(e -> e.translate(translationMap))
                .collect(translationMap.toList(expressions));
        if (expressions == translatedExpressions) return this;
        Expression result = new AndImpl(comments(), source(), booleanPt, translatedExpressions);
        return translationMap.postTranslationHandler(this, result);
    }


    public static class Builder extends ElementImpl.Builder<And.Builder> implements And.Builder {
        private final List<Expression> expressions = new ArrayList<>();
        private ParameterizedType booleanPt;

        @Override
        public And.Builder addExpressions(List<Expression> expressions) {
            this.expressions.addAll(expressions);
            return this;
        }

        @Override
        public And.Builder addExpression(Expression expression) {
            this.expressions.add(expression);
            return this;
        }

        public And.Builder setBooleanParameterizedType(ParameterizedType parameterizedType) {
            this.booleanPt = parameterizedType;
            return this;
        }

        @Override
        public And build() {
            return new AndImpl(comments, source, booleanPt, List.copyOf(expressions));
        }
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new AndImpl(comments(), source(), booleanPt,
                expressions.stream().map(e -> e.rewire(infoMap)).toList());
    }
}
