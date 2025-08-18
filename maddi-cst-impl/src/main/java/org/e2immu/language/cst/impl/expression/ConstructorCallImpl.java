package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;
import org.e2immu.util.internal.util.ListUtil;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ConstructorCallImpl extends ExpressionImpl implements ConstructorCall {
    private final MethodInfo constructor;
    private final Diamond diamond;
    private final Expression object;
    private final List<Expression> parameterExpressions;
    private final List<ParameterizedType> typeArguments;
    private final ArrayInitializer arrayInitializer;
    private final TypeInfo anonymousClass;
    private final ParameterizedType concreteReturnType;
    private final PropertyValueMap analysis;

    public ConstructorCallImpl(List<Comment> comments, Source source, MethodInfo constructor,
                               ParameterizedType concreteReturnType,
                               Diamond diamond, Expression object, List<Expression> expressions,
                               List<ParameterizedType> typeArguments,
                               ArrayInitializer arrayInitializer, TypeInfo anonymousClass) {
        this(comments, source, constructor, concreteReturnType, diamond, object, expressions,
                typeArguments, arrayInitializer, anonymousClass, new PropertyValueMapImpl());
    }

    public ConstructorCallImpl(List<Comment> comments, Source source, MethodInfo constructor,
                               ParameterizedType concreteReturnType,
                               Diamond diamond, Expression object, List<Expression> expressions,
                               List<ParameterizedType> typeArguments,
                               ArrayInitializer arrayInitializer, TypeInfo anonymousClass, PropertyValueMap analysis) {
        super(comments, source, 1 + (object == null ? 0 : object.complexity())
                                + expressions.stream().mapToInt(Expression::complexity).sum());
        assert constructor != null || anonymousClass != null;
        this.constructor = constructor;
        this.diamond = Objects.requireNonNull(diamond);
        this.object = object;
        parameterExpressions = Objects.requireNonNull(expressions);
        this.typeArguments = Objects.requireNonNull(typeArguments);
        this.arrayInitializer = arrayInitializer;
        this.anonymousClass = anonymousClass;
        this.concreteReturnType = Objects.requireNonNull(concreteReturnType);
        assert anonymousClass == null || anonymousClass.compilationUnitOrEnclosingType().isRight();
        this.analysis = analysis;
    }

    @Override
    public Expression withSource(Source source) {
        return new ConstructorCallImpl(comments(), source, constructor, concreteReturnType, diamond, object,
                parameterExpressions, typeArguments, arrayInitializer, anonymousClass, analysis);
    }

    public static class Builder extends ElementImpl.Builder<ConstructorCall.Builder> implements ConstructorCall.Builder {
        private MethodInfo constructor;
        private Diamond diamond;
        private Expression object;
        private List<Expression> parameterExpressions;
        private List<ParameterizedType> typeArguments;
        private ArrayInitializer arrayInitializer;
        private TypeInfo anonymousClass;
        private ParameterizedType concreteReturnType;

        @Override
        public ConstructorCall build() {
            return new ConstructorCallImpl(comments, source, constructor, concreteReturnType, diamond, object,
                    List.copyOf(parameterExpressions),
                    typeArguments == null ? List.of() : List.copyOf(typeArguments),
                    arrayInitializer, anonymousClass);
        }

        @Override
        public Builder setObject(Expression object) {
            this.object = object;
            return this;
        }

        @Override
        public Builder setDiamond(Diamond diamond) {
            this.diamond = diamond;
            return this;
        }

        @Override
        public Builder setConstructor(MethodInfo constructor) {
            this.constructor = constructor;
            return this;
        }

        @Override
        public Builder setAnonymousClass(TypeInfo anonymousClass) {
            this.anonymousClass = anonymousClass;
            return this;
        }

        @Override
        public Builder setArrayInitializer(ArrayInitializer arrayInitializer) {
            this.arrayInitializer = arrayInitializer;
            return this;
        }

        @Override
        public Builder setParameterExpressions(List<Expression> expressions) {
            this.parameterExpressions = expressions;
            return this;
        }

        @Override
        public Builder setTypeArguments(List<ParameterizedType> typeArguments) {
            this.typeArguments = typeArguments;
            return this;
        }

        @Override
        public Builder setConcreteReturnType(ParameterizedType returnType) {
            this.concreteReturnType = returnType;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstructorCallImpl that)) return false;
        return Objects.equals(constructor, that.constructor)
               && Objects.equals(concreteReturnType, that.concreteReturnType)
               && Objects.equals(object, that.object)
               && Objects.equals(parameterExpressions, that.parameterExpressions)
               && Objects.equals(source(), that.source())
               && Objects.equals(arrayInitializer, that.arrayInitializer)
               && Objects.equals(anonymousClass, that.anonymousClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source(), constructor, concreteReturnType, object, parameterExpressions, arrayInitializer,
                anonymousClass);
    }

    @Override
    public MethodInfo constructor() {
        return constructor;
    }

    @Override
    public Expression object() {
        return object;
    }

    @Override
    public List<Expression> parameterExpressions() {
        return parameterExpressions;
    }

    @Override
    public TypeInfo anonymousClass() {
        return anonymousClass;
    }

    @Override
    public ArrayInitializer arrayInitializer() {
        return arrayInitializer;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return concreteReturnType;
    }

    @Override
    public List<ParameterizedType> typeArguments() {
        return typeArguments;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_NEW_INSTANCE;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof ConstructorCall cc) {
            int c = constructor.fullyQualifiedName().compareTo(cc.constructor().fullyQualifiedName());
            if (c != 0) return c;
            int d = ListUtil.compare(parameterExpressions, cc.parameterExpressions());
            if (d != 0) return d;
            if (object == null && cc.object() != null) return -1;
            if (object != null && cc.object() == null) return 1;
            if (object != null) {
                return object.compareTo(cc.object());
            }
            return 0;
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            if (object != null) object.visit(predicate);
            parameterExpressions.forEach(p -> p.visit(predicate));
            if (arrayInitializer != null) arrayInitializer.visit(predicate);
            //currently not implemented. if (anonymousClass != null) anonymousClass.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            if (object != null) object.visit(visitor);
            parameterExpressions.forEach(p -> p.visit(visitor));
            if (arrayInitializer != null) arrayInitializer.visit(visitor);
            //currently not implemented. if (anonymousClass != null) anonymousClass.visit(visitor);
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (object != null) {
            outputBuilder.add(outputInParenthesis(qualification, precedence(), object));
            outputBuilder.add(SymbolEnum.DOT);
        }
        if (constructor != null || anonymousClass != null) {
            outputBuilder.add(KeywordImpl.NEW);
            if (!typeArguments.isEmpty()) {
                outputBuilder.add(SpaceEnum.ONE).add(typeArguments.stream()
                        .map(pt -> pt.print(qualification, false, DiamondEnum.SHOW_ALL))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_ANGLE_BRACKET,
                                SymbolEnum.RIGHT_ANGLE_BRACKET, GuideImpl.defaultGuideGenerator())));
            }
            outputBuilder.add(SpaceEnum.ONE)
                    .add(concreteReturnType.copyWithoutArrays().print(qualification, false, diamond));
            if (concreteReturnType.arrays() > 0) {
                for (int i = 0; i < concreteReturnType.arrays(); i++) {
                    if (i < parameterExpressions.size()) {
                        outputBuilder.add(SymbolEnum.LEFT_BRACKET);
                        Expression size = parameterExpressions.get(i);
                        if (!(size.isEmpty())) {
                            outputBuilder.add(size.print(qualification));
                        }
                        outputBuilder.add(SymbolEnum.RIGHT_BRACKET);
                    } else {
                        outputBuilder.add(SymbolEnum.OPEN_CLOSE_BRACKETS);
                    }
                }
            } else {
                if (parameterExpressions.isEmpty()) {
                    outputBuilder.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
                } else {
                    outputBuilder
                            .add(SymbolEnum.LEFT_PARENTHESIS)
                            .add(parameterExpressions.stream().map(expression -> expression.print(qualification))
                                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                            .add(SymbolEnum.RIGHT_PARENTHESIS);
                }
            }
            //    }
        }
        if (anonymousClass != null) {
            outputBuilder.add(anonymousClass.print(qualification, false));
        }
        if (arrayInitializer != null) {
            outputBuilder.add(arrayInitializer.print(qualification));
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(Stream.concat(object == null ? Stream.of() : object.variables(descendMode),
                        parameterExpressions.stream().flatMap(e -> e.variables(descendMode))),
                arrayInitializer == null ? Stream.of() : arrayInitializer.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        Stream<Element.TypeReference> typeArgStream = typeArguments.stream().flatMap(ParameterizedType::typesReferencedMadeExplicit);
        Stream<Element.TypeReference> arrayInitStream = arrayInitializer == null ? Stream.of() : arrayInitializer.typesReferenced();
        Stream<Element.TypeReference> anonStream = anonymousClass == null ? Stream.of() : anonymousClass.typesReferenced();
        Stream<Element.TypeReference> objectStream = object == null ? Stream.of() : object.typesReferenced();
        Stream<Element.TypeReference> paramStream = parameterExpressions.stream().flatMap(Expression::typesReferenced);
        Stream<Element.TypeReference> ccTypeStream = Stream.of(new ElementImpl.TypeReference(concreteReturnType.typeInfo(), true));
        Stream<Element.TypeReference> ccTypeParametersStream = diamond.isNo()
                ? Stream.of()
                : concreteReturnType.parameters().stream().flatMap(pt -> diamond.isShowAll()
                ? pt.typesReferencedMadeExplicit() : pt.typesReferenced());
        return Stream.concat(typeArgStream, Stream.concat(arrayInitStream, Stream.concat(anonStream,
                Stream.concat(objectStream, Stream.concat(paramStream, Stream.concat(ccTypeParametersStream,
                        ccTypeStream))))));
    }

    @Override
    public ConstructorCall withParameterExpressions(List<Expression> newParameterExpressions) {
        return new ConstructorCallImpl(comments(), source(), constructor, concreteReturnType, diamond, object,
                newParameterExpressions, typeArguments, arrayInitializer, anonymousClass);
    }

    @Override
    public ConstructorCall withAnonymousClass(TypeInfo newAnonymous) {
        return new ConstructorCallImpl(comments(), source(), constructor, concreteReturnType, diamond, object,
                parameterExpressions, typeArguments, arrayInitializer, newAnonymous);
    }

    @Override
    public Diamond diamond() {
        return diamond;
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated == null) return this;
        if (translated != this) return translated;

        Expression translatedObject = object == null ? null : translationMap.translateExpression(object);
        ParameterizedType translatedType = translationMap.translateType(this.parameterizedType());
        List<Expression> translatedParameterExpressions = parameterExpressions.isEmpty() ? parameterExpressions
                : parameterExpressions.stream().map(e -> e.translate(translationMap))
                .collect(translationMap.toList(parameterExpressions));
        List<ParameterizedType> trTypeArgs = typeArguments.stream()
                .map(translationMap::translateType)
                .collect(translationMap.toList(typeArguments));
        ArrayInitializer translatedInitializer = arrayInitializer == null ? null :
                (ArrayInitializer) arrayInitializer.translate(translationMap);
        TypeInfo tAnonymous = anonymousClass == null ? null : anonymousClass.translate(translationMap).getFirst();
        if (translatedObject == object
            && translatedType == this.parameterizedType()
            && translatedParameterExpressions == this.parameterExpressions
            && trTypeArgs == typeArguments
            && translatedInitializer == arrayInitializer
            && tAnonymous == anonymousClass
            && (analysis.isEmpty() || !translationMap.isClearAnalysis())) {
            return this;
        }
        return new ConstructorCallImpl(comments(), source(),
                constructor,
                translatedType,
                diamond,
                object,
                translatedParameterExpressions,
                trTypeArgs,
                translatedInitializer,
                tAnonymous);
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        List<Expression> rewiredArgs = parameterExpressions.stream().map(e -> e.rewire(infoMap)).toList();
        return new ConstructorCallImpl(comments(), source(),
                constructor == null ? null : infoMap.methodInfo(constructor),
                concreteReturnType.rewire(infoMap), diamond,
                object == null ? null : object.rewire(infoMap),
                rewiredArgs,
                typeArguments.stream().map(pt -> pt.rewire(infoMap)).toList(),
                arrayInitializer == null ? null : (ArrayInitializer) arrayInitializer.rewire(infoMap),
                anonymousClass == null ? null : infoMap.typeInfoRecurseAllPhases(anonymousClass),
                analysis.rewire(infoMap));
    }

    @Override
    public PropertyValueMap analysis() {
        return analysis;
    }
}
