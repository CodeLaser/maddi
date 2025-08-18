package org.e2immu.parser.java.erasure;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.ErasedExpression;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract   class ErasureExpressionImpl implements ErasedExpression, Expression {
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
    public Stream<TypeReference> typesReferenced() {
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
