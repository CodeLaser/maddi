package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.ConstantExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ConstantExpressionImpl<T> extends ExpressionImpl implements ConstantExpression<T> {

    protected ConstantExpressionImpl(List<Comment> comments, Source source, int complexity) {
        super(comments, source, complexity);
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.TOP;
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
}
