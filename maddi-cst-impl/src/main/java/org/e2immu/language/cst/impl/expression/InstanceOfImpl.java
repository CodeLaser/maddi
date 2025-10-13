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

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.InstanceOf;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InstanceOfImpl extends ExpressionImpl implements InstanceOf {
    private final Expression expression;
    private final ParameterizedType testType;
    private final RecordPattern patternVariable;
    private final ParameterizedType booleanParameterizedType;
    private final PropertyValueMap propertyValueMap;

    public InstanceOfImpl(List<Comment> comments,
                          Source source,
                          Expression expression,
                          ParameterizedType testType,
                          RecordPattern patternVariable,
                          ParameterizedType booleanParameterizedType) {
        this(comments, source, expression, testType, patternVariable, booleanParameterizedType,
                new PropertyValueMapImpl());
    }

    private InstanceOfImpl(List<Comment> comments,
                           Source source,
                           Expression expression,
                           ParameterizedType testType,
                           RecordPattern patternVariable,
                           ParameterizedType booleanParameterizedType,
                           PropertyValueMap propertyValueMap) {
        super(comments, source, 2 + expression.complexity());
        this.expression = expression;
        this.testType = testType;
        this.patternVariable = patternVariable;
        this.booleanParameterizedType = booleanParameterizedType;
        this.propertyValueMap = propertyValueMap;
    }

    @Override
    public Expression withSource(Source source) {
        return new InstanceOfImpl(comments(), source, expression, testType, patternVariable, booleanParameterizedType,
                propertyValueMap);
    }

    public static class BuilderImpl extends ElementImpl.Builder<InstanceOf.Builder> implements InstanceOf.Builder {
        private final ParameterizedType booleanPt;

        private Expression expression;
        private ParameterizedType testType;
        private RecordPattern patternVariable;

        public BuilderImpl(ParameterizedType booleanPt) {
            this.booleanPt = booleanPt;
        }

        @Override
        public InstanceOf.Builder setExpression(Expression expression) {
            this.expression = expression;
            return this;
        }

        @Override
        public InstanceOf.Builder setPatternVariable(RecordPattern patternVariable) {
            this.patternVariable = patternVariable;
            return this;
        }

        @Override
        public InstanceOf.Builder setTestType(ParameterizedType testType) {
            this.testType = testType;
            return this;
        }

        @Override
        public InstanceOf build() {
            return new InstanceOfImpl(comments, source, expression, testType, patternVariable, booleanPt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstanceOfImpl that)) return false;
        return Objects.equals(expression, that.expression)
               && Objects.equals(testType, that.testType)
               && Objects.equals(patternVariable, that.patternVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, testType, patternVariable);
    }

    @Override
    public Expression expression() {
        return expression;
    }

    @Override
    public ParameterizedType testType() {
        return testType;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return booleanParameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.RELATIONAL;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_INSTANCE_OF;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof InstanceOf other) {
            if (expression instanceof VariableExpression ve
                && other.expression() instanceof VariableExpression ve2) {
                int c = ve.variable().fullyQualifiedName().compareTo(ve2.variable().fullyQualifiedName());
                if (c == 0)
                    c = testType.detailedString().compareTo(other.testType().detailedString());
                return c;
            }
            int c = testType.fullyQualifiedName().compareTo(other.testType().fullyQualifiedName());
            if (c != 0) return c;
            return expression.compareTo(other.expression());
        }
        throw new InternalCompareToException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        ParameterizedType translatedType = translationMap.translateType(this.testType);
        Expression translatedExpression = expression.translate(translationMap);
        RecordPattern translatedLv = patternVariable == null ? null
                : patternVariable.translate(translationMap);
        if (translatedType == testType && translatedExpression == expression
            && translatedLv == patternVariable) {
            return this;
        }
        return new InstanceOfImpl(comments(), source(), translatedExpression, translatedType, translatedLv,
                booleanParameterizedType, propertyValueMap);
    }

    @Override
    public RecordPattern patternVariable() {
        return patternVariable;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            if (patternVariable != null) {
                patternVariable.visit(predicate);
            }
            expression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            if (patternVariable != null) {
                patternVariable.visit(visitor);
            }
            expression.visit(visitor);
        }
        visitor.afterExpression(this);
    }


    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl()
                .add(expression.print(qualification))
                .add(SymbolEnum.INSTANCE_OF);
        if (patternVariable != null) {
            ob.add(patternVariable.print(qualification));
        } else {
            ob.add(testType.print(qualification, false, DiamondEnum.SHOW_ALL));
        }
        return ob;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return expression.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(Stream.of(new ElementImpl.TypeReference(testType.typeInfo(), true)),
                expression.typesReferenced());
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new InstanceOfImpl(comments(), source(), expression.rewire(infoMap), testType.rewire(infoMap),
                patternVariable == null ? null : (RecordPattern) patternVariable.rewire(infoMap),
                booleanParameterizedType, propertyValueMap);
    }

    @Override
    public PropertyValueMap analysis() {
        return propertyValueMap;
    }

}
