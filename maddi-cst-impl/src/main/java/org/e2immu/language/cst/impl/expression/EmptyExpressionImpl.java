package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.EmptyExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Predefined;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

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

    @Override
    public Expression withSource(Source source) {
        throw new UnsupportedOperationException();
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
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.empty();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        return translationMap.translateExpression(this);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return this;
    }
}
