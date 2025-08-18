package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MethodReferenceImpl extends ExpressionImpl implements MethodReference {
    private final ParameterizedType parameterizedType;
    private final MethodInfo methodInfo;
    private final Expression scope;
    private final List<ParameterizedType> concreteParameterTypes;
    private final ParameterizedType concreteReturnType;

    public MethodReferenceImpl(List<Comment> comments, Source source,
                               ParameterizedType parameterizedType, MethodInfo methodInfo, Expression scope,
                               List<ParameterizedType> concreteParameterTypes,
                               ParameterizedType concreteReturnType) {
        super(comments, source, 1 + scope.complexity());
        this.parameterizedType = Objects.requireNonNull(parameterizedType);
        this.methodInfo = methodInfo;
        this.scope = scope;
        this.concreteParameterTypes = Objects.requireNonNull(concreteParameterTypes);
        this.concreteReturnType = Objects.requireNonNull(concreteReturnType);
    }

    @Override
    public Expression withSource(Source source) {
        return new MethodReferenceImpl(comments(), source, parameterizedType, methodInfo, scope, concreteParameterTypes,
                concreteReturnType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodReferenceImpl that)) return false;
        return Objects.equals(methodInfo, that.methodInfo) && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodInfo, scope);
    }

    public static class Builder extends ElementImpl.Builder<MethodReference.Builder> implements MethodReference.Builder {
        private ParameterizedType parameterizedType;
        private MethodInfo methodInfo;
        private Expression scope;
        private List<ParameterizedType> concreteParameterTypes = new ArrayList<>();
        private ParameterizedType concreteReturnType;

        @Override
        public Builder setScope(Expression expression) {
            this.scope = expression;
            return this;
        }

        @Override
        public Builder setMethod(MethodInfo method) {
            this.methodInfo = method;
            return this;
        }

        @Override
        public Builder setConcreteFunctionalType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public Builder setConcreteParameterTypes(List<ParameterizedType> concreteParameterTypes) {
            this.concreteParameterTypes = concreteParameterTypes;
            return this;
        }

        @Override
        public Builder setConcreteReturnType(ParameterizedType concreteReturnType) {
            this.concreteReturnType = concreteReturnType;
            return this;
        }

        @Override
        public MethodReference build() {
            return new MethodReferenceImpl(comments, source, parameterizedType, methodInfo, scope,
                    List.copyOf(concreteParameterTypes),
                    concreteReturnType);
        }
    }

    @Override
    public List<ParameterizedType> concreteParameterTypes() {
        return concreteParameterTypes;
    }

    @Override
    public ParameterizedType concreteReturnType() {
        return concreteReturnType;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_METHOD_REFERENCE;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof MethodReference mr) {
            int c = methodInfo.fullyQualifiedName().compareTo(mr.methodInfo().fullyQualifiedName());
            if (c == 0) return scope.compareTo(mr.scope());
            return c;
        }
        throw new InternalCompareToException();
    }

    @Override
    public Expression scope() {
        return scope;
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            scope.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            scope.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String methodName = methodInfo.isConstructor() ? "new" : methodInfo.name();
        return new OutputBuilderImpl().add(scope.print(qualification)).add(SymbolEnum.DOUBLE_COLON).add(new TextImpl(methodName));
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated != this) return translated;

        Expression translatedScope = scope.translate(translationMap);
        ParameterizedType transType = translationMap.translateType(parameterizedType);
        if (translatedScope == scope && transType == parameterizedType) return this;
        return new MethodReferenceImpl(comments(), source(), transType, methodInfo, translatedScope,
                concreteParameterTypes.stream().map(translationMap::translateType).toList(),
                translationMap.translateType(concreteReturnType));
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return scope.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return scope.typesReferenced();
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new MethodReferenceImpl(comments(), source(), parameterizedType.rewire(infoMap),
                infoMap.methodInfo(methodInfo), scope.rewire(infoMap),
                concreteParameterTypes.stream().map(pt -> pt.rewire(infoMap)).toList(),
                concreteReturnType.rewire(infoMap));
    }
}
