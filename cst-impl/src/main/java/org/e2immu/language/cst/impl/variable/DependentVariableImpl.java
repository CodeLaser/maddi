package org.e2immu.language.cst.impl.variable;

import org.e2immu.annotation.NotNull;
import org.e2immu.annotation.Nullable;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

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
        } else {
            pt = arrayExpression.parameterizedType().copyWithOneFewerArrays();
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
        fullyQualifiedName = arrayFqn + "[" + indexFqn + "]";
        String arraySimple = arrayVariable == null
                ? arrayExpression.print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString()
                : arrayVariable.simpleName();
        String indexSimple = indexVariable == null
                ? indexExpression.print(QualificationImpl.FULLY_QUALIFIED_NAMES).toString()
                : indexVariable.simpleName();
        simpleName = arraySimple + "[" + indexSimple + "]";
    }

    private static String expressionId(Expression expression) {
        if (expression instanceof ConstantExpression<?>) return expression.toString();
        Source source = expression.source();
        assert source != null;
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
    public Stream<TypeReference> typesReferenced() {
        return Stream.concat(arrayExpression.typesReferenced(), indexExpression.typesReferenced());
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
        return arrayVariable.scopeIsRecursively(variable);
    }

    @Override
    public Stream<Variable> variableStreamDescendIntoScope() {
        return Stream.concat(Stream.of(this), arrayVariable.variableStreamDescendIntoScope());
    }

    @Override
    public Variable rewire(InfoMap infoMap) {
        return new DependentVariableImpl(arrayExpression.rewire(infoMap),
                arrayVariable == null ? null : arrayVariable.rewire(infoMap),
                indexExpression.rewire(infoMap),
                indexVariable == null ? null : indexVariable.rewire(infoMap),
                parameterizedType().rewire(infoMap));
    }
}
