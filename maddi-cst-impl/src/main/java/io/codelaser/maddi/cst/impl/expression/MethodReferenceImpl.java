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
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

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
        // ⚠ THE METHOD ITSELF MUST BE REMAPPED, exactly as MethodCallImpl#translate does. It was not, so a
        // relocated method's body kept 'Origin::method' after 'method' had moved -- and when the origin no
        // longer declares it, javac refuses ("invalid method reference"). rewire() three methods below has
        // always done this correctly (infoMap.methodInfo(methodInfo)); only translate() skipped it.
        // Found through extract.extractCompanion on an OSS corpus, but it is not specific to that verb: every
        // lever that relocates a member translates bodies through the same map.
        MethodInfo translatedMethod = translationMap.translateMethodInfo(methodInfo);
        // ⚠ AND THE SCOPE MUST FOLLOW IT. print() emits '<scope>::<name>', so the owner in the TEXT comes
        // from the scope, not from methodInfo: remapping the method alone retargets the reference while
        // still printing the old owner. When the method moved to a different type and the scope is that old
        // type, rebuild it on the new one.
        if (translatedMethod != methodInfo
            && translatedMethod.typeInfo() != methodInfo.typeInfo()
            && translatedScope instanceof TypeExpression te
            && te.parameterizedType().typeInfo() == methodInfo.typeInfo()) {
            // NO diamond: a type used as a method-reference scope carries no type arguments
            translatedScope = new TypeExpressionImpl(translatedMethod.typeInfo().asSimpleParameterizedType(),
                    DiamondEnum.NO);
        }
        if (translatedScope == scope && transType == parameterizedType && translatedMethod == methodInfo) {
            return this;
        }
        Expression result = new MethodReferenceImpl(comments(), source(), transType, translatedMethod, translatedScope,
                concreteParameterTypes.stream().map(translationMap::translateType).toList(),
                translationMap.translateType(concreteReturnType));
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return scope.variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return scope.typesReferenced(predicate);
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new MethodReferenceImpl(comments(), source(), parameterizedType.rewire(infoMap),
                infoMap.methodInfo(methodInfo), scope.rewire(infoMap),
                concreteParameterTypes.stream().map(pt -> pt.rewire(infoMap)).toList(),
                concreteReturnType.rewire(infoMap));
    }
}
