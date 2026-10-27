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

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.QualificationImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class DependentVariableImpl extends VariableImpl implements DependentVariable {
    @NotNull
    private final Expression indexExpression;

    @NotNull
    private final Expression arrayExpression;

    @Nullable
    private final Variable arrayVariable;

    @Nullable
    private final Variable indexVariable;

    private final String simpleName;
    private final String fullyQualifiedName;

    public static DependentVariable create(Expression arrayExpression,
                                           Expression indexExpression,
                                           ParameterizedType parameterizedType) {
        assert arrayExpression != null;
        assert indexExpression != null;
        Variable av = extractVariable(arrayExpression);
        Variable iv = extractVariable(indexExpression);
        ParameterizedType pt;
        if (parameterizedType != null) {
            pt = parameterizedType;
        } else if (arrayExpression.parameterizedType().arrays() > 0) {
            pt = arrayExpression.parameterizedType().copyWithOneFewerArrays();
        } else {
            // not statically an array (e.g. an Object-typed slot standing in for an array after translation):
            // the element type cannot be derived; keep the slot's type rather than failing
            pt = arrayExpression.parameterizedType();
        }
        return new DependentVariableImpl(arrayExpression, av, indexExpression, iv, pt);
    }

    private static Variable extractVariable(Expression e) {
        if (e instanceof VariableExpression ve) {
            return ve.variable();
        }
        if (e instanceof Cast cast) {
            return extractVariable(cast.expression());
        }
        if (e instanceof EnclosedExpression ee) {
            return extractVariable(ee.inner());
        }
        return null;
    }

    public DependentVariableImpl(Expression arrayExpression,
                                 Variable arrayVariable,
                                 Expression indexExpression,
                                 Variable indexVariable,
                                 ParameterizedType parameterizedType) {
        super(parameterizedType);
        this.indexExpression = indexExpression;
        this.arrayExpression = arrayExpression;
        this.arrayVariable = arrayVariable;
        this.indexVariable = indexVariable;
        String arrayFqn = arrayVariable == null ? expressionId(arrayExpression) : arrayVariable.fullyQualifiedName();
        String indexFqn = indexVariable == null ? expressionId(indexExpression) : indexVariable.fullyQualifiedName();
        fullyQualifiedName = (arrayFqn + "[" + indexFqn + "]").intern(); // see FieldReferenceImpl
        String arraySimple = arrayVariable == null
                ? arrayExpression.print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString()
                : arrayVariable.simpleName();
        String indexSimple = indexVariable == null
                ? indexExpression.print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString()
                : indexVariable.simpleName();
        simpleName = (arraySimple + "[" + indexSimple + "]").intern();
    }

    private static String expressionId(Expression expression) {
        if (expression instanceof ConstantExpression<?>) return expression.toString();
        Source source = expression.source();
        if (source == null) {
            // synthetic expressions (built by analyzers, e.g. the link engine's virtual faces) carry no source;
            // fall back to their print for a stable identity
            return "`" + expression + "`";
        }
        return "`" + source.beginLine() + "-" + source.beginPos() + "`";
    }

    @Override
    public Variable arrayVariable() {
        return arrayVariable;
    }

    @Override
    public int complexity() {
        return arrayExpression.complexity() + indexExpression.complexity();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            arrayExpression.visit(predicate);
            indexExpression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeVariable(this)) {
            arrayExpression.visit(visitor);
            indexExpression.visit(visitor);
        }
        visitor.afterVariable(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputInParenthesis;
        OutputBuilder printedArrayExpression = arrayExpression.print(qualification);
        if (PrecedenceEnum.ACCESS.greaterThan(arrayExpression.precedence())) {
            outputInParenthesis = new OutputBuilderImpl()
                    .add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(printedArrayExpression)
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        } else {
            outputInParenthesis = printedArrayExpression;
        }
        return new OutputBuilderImpl()
                .add(outputInParenthesis)
                .add(SymbolEnum.LEFT_BRACKET)
                .add(indexExpression.print(qualification))
                .add(SymbolEnum.RIGHT_BRACKET);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        if (descendMode.isNo()) return Stream.of(this);
        Stream<Variable> s1 = Stream.concat(Stream.ofNullable(arrayVariable), arrayExpression.variables(descendMode));
        Stream<Variable> s2 = Stream.concat(Stream.ofNullable(indexVariable), indexExpression.variables(descendMode));
        return Stream.concat(Stream.of(this), Stream.concat(s1, s2));
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate, DetailedSources detailedSources) {
        return Stream.concat(arrayExpression.typesReferenced(predicate), indexExpression.typesReferenced(predicate));
    }

    @Override
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public String simpleName() {
        return simpleName;
    }

    @Override
    public Variable indexVariable() {
        return indexVariable;
    }

    @Override
    public Expression arrayExpression() {
        return arrayExpression;
    }

    @Override
    public Expression indexExpression() {
        return indexExpression;
    }

    @Override
    public boolean scopeIsRecursively(Variable variable) {
        if (variable.equals(arrayVariable)) return true;
        // arrayVariable is null whenever the array expression is not a variable, e.g. map.get(k)[0];
        // the constructor, variables() and rewire() all treat that as normal, these two did not
        return arrayVariable != null && arrayVariable.scopeIsRecursively(variable);
    }

    @Override
    public Stream<Variable> variableStreamDescendIntoScope() {
        return arrayVariable == null ? Stream.of(this)
                : Stream.concat(Stream.of(this), arrayVariable.variableStreamDescendIntoScope());
    }

    @Override
    public Variable rewire(InfoMapView infoMap) {
        return new DependentVariableImpl(arrayExpression.rewire(infoMap),
                arrayVariable == null ? null : arrayVariable.rewire(infoMap),
                indexExpression.rewire(infoMap),
                indexVariable == null ? null : indexVariable.rewire(infoMap),
                parameterizedType().rewire(infoMap));
    }
}
