package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.element.ThisName;
import org.e2immu.language.cst.api.output.element.TypeName;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MethodCallImpl extends ExpressionImpl implements MethodCall {
    private final Expression object;
    private final boolean objectIsImplicit;
    private final MethodInfo methodInfo;
    private final List<Expression> parameterExpressions;
    private final ParameterizedType concreteReturnType;
    private final List<ParameterizedType> typeArguments;// X.<T1>someMethod(...)
    private final String modificationTimes;
    private final PropertyValueMap propertyValueMap;

    public MethodCallImpl(List<Comment> comments, Source source,
                          Expression object, boolean objectIsImplicit, MethodInfo methodInfo,
                          List<Expression> parameterExpressions, ParameterizedType concreteReturnType,
                          List<ParameterizedType> typeArguments, String modificationTimes) {
        this(comments, source, object, objectIsImplicit, methodInfo, parameterExpressions, concreteReturnType,
                typeArguments, modificationTimes, new PropertyValueMapImpl());
    }

    private MethodCallImpl(List<Comment> comments, Source source,
                           Expression object, boolean objectIsImplicit, MethodInfo methodInfo,
                           List<Expression> parameterExpressions, ParameterizedType concreteReturnType,
                           List<ParameterizedType> typeArguments,
                           String modificationTimes, PropertyValueMap propertyValueMap) {
        super(comments, source, object.complexity()
                                + parameterExpressions.stream().mapToInt(Expression::complexity).sum());
        this.object = Objects.requireNonNull(object);
        this.objectIsImplicit = objectIsImplicit;
        this.parameterExpressions = Objects.requireNonNull(parameterExpressions);
        this.concreteReturnType = Objects.requireNonNull(concreteReturnType);
        this.typeArguments = Objects.requireNonNull(typeArguments);
        this.methodInfo = Objects.requireNonNull(methodInfo);
        this.modificationTimes = Objects.requireNonNull(modificationTimes);
        assert !(object instanceof TypeExpression) || methodInfo.isStatic();
        this.propertyValueMap = propertyValueMap;
    }

    @Override
    public Expression withSource(Source source) {
        return new MethodCallImpl(comments(), source, object, objectIsImplicit, methodInfo, parameterExpressions,
                concreteReturnType, typeArguments, modificationTimes, propertyValueMap);
    }

    public static class Builder extends ElementImpl.Builder<MethodCall.Builder> implements MethodCall.Builder {
        private Expression object;
        private MethodInfo methodInfo;
        private List<Expression> parameterExpressions;
        private boolean objectIsImplicit;
        private ParameterizedType concreteReturnType;
        private List<ParameterizedType> typeArguments;
        private String modificationTimes = "";

        public Builder() {
        }

        public Builder(MethodCall mc) {
            super(mc);
            object = mc.object();
            methodInfo = mc.methodInfo();
            parameterExpressions = mc.parameterExpressions();
            objectIsImplicit = mc.objectIsImplicit();
            concreteReturnType = mc.concreteReturnType();
            modificationTimes = mc.modificationTimes();
            typeArguments = mc.typeArguments();
        }

        @Override
        public MethodCall build() {
            assert parameterExpressions != null : "Must set parameter expressions!";
            assert object != null : "Must set object, even if it is the implicit 'this', of call to "
                                    + methodInfo;
            assert concreteReturnType != null : "Must set the concrete return type of call to " + methodInfo;
            return new MethodCallImpl(comments, source, object, objectIsImplicit, methodInfo,
                    List.copyOf(parameterExpressions), concreteReturnType,
                    typeArguments == null ? List.of() : List.copyOf(typeArguments), modificationTimes);
        }

        @Override
        public Builder setObject(Expression object) {
            this.object = object;
            return this;
        }

        @Override
        public Builder setMethodInfo(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            return this;
        }

        @Override
        public Builder setModificationTimes(String modificationTimes) {
            this.modificationTimes = modificationTimes;
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
        public Builder setObjectIsImplicit(boolean objectIsImplicit) {
            this.objectIsImplicit = objectIsImplicit;
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
        if (!(o instanceof MethodCallImpl that)) return false;
        return objectIsImplicit == that.objectIsImplicit
               && Objects.equals(object, that.object)
               && Objects.equals(methodInfo, that.methodInfo)
               && Objects.equals(parameterExpressions, that.parameterExpressions)
               // https://github.com/e2immu/e2immu/issues/56
               && Objects.equals(modificationTimes, that.modificationTimes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, objectIsImplicit, methodInfo, parameterExpressions, modificationTimes);
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
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
    public String modificationTimes() {
        return modificationTimes;
    }

    @Override
    public boolean objectIsImplicit() {
        return objectIsImplicit;
    }

    @Override
    public ParameterizedType concreteReturnType() {
        return concreteReturnType;
    }

    @Override
    public MethodCall withParameterExpressions(List<Expression> parameterExpressions) {
        return new MethodCallImpl(comments(), source(), object, objectIsImplicit, methodInfo, parameterExpressions,
                concreteReturnType, typeArguments, modificationTimes, propertyValueMap);
    }

    @Override
    public MethodCall withObject(Expression object) {
        return new MethodCallImpl(comments(), source(), object, objectIsImplicit, methodInfo, parameterExpressions,
                concreteReturnType, typeArguments, modificationTimes, propertyValueMap);
    }

    @Override
    public List<ParameterizedType> typeArguments() {
        return typeArguments;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return concreteReturnType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_METHOD;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return 0;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            object.visit(predicate);
            parameterExpressions.forEach(e -> e.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            object.visit(visitor);
            parameterExpressions.forEach(e -> e.visit(visitor));
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return print(qualification, null);
    }

    // will come directly here only from this method (chaining of method calls produces a guide)
    public OutputBuilder print(Qualification qualification, GuideImpl.GuideGenerator guideGenerator) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        boolean last = false;
        boolean start = false;
        GuideImpl.GuideGenerator gg = null;

        String methodName;
        if (!typeArguments.isEmpty()) {
            // TODO this is a hack; type arguments interfere with the QualifiedNameImpl implementation
            //   implement properly!
            String ta = new OutputBuilderImpl().add(typeArguments.stream()
                    .map(pt -> pt.print(qualification, false, DiamondEnum.SHOW_ALL))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_ANGLE_BRACKET,
                            SymbolEnum.RIGHT_ANGLE_BRACKET, GuideImpl.defaultGuideGenerator()))).toString();
            methodName = ta + " " + methodInfo.name();
        } else {
            methodName = methodInfo.name();
        }

        if (objectIsImplicit && qualification.doNotQualifyImplicit()) {
            outputBuilder.add(new TextImpl(methodName));
        } else {
            VariableExpression ve;
            MethodCall methodCall;
            TypeExpression typeExpression;
            if ((methodCall = object.asInstanceOf(MethodCall.class)) != null) {
                // chaining!
                if (guideGenerator == null) {
                    gg = GuideImpl.defaultGuideGenerator();
                    last = true;
                } else {
                    gg = guideGenerator;
                }
                outputBuilder.add(((MethodCallImpl) methodCall).print(qualification, gg)); // recursive call
                outputBuilder.add(gg.mid());
                outputBuilder.add(SymbolEnum.DOT);
                outputBuilder.add(new TextImpl(methodName));
            } else if ((typeExpression = object.asInstanceOf(TypeExpression.class)) != null) {
                /*
                we may or may not need to write the type here.
                (we check methodInspection is set, because of debugOutput)
                 */
                assert methodInfo.isStatic();
                TypeInfo typeInfo = typeExpression.parameterizedType().typeInfo();
                TypeName typeName = TypeNameImpl.typeName(typeInfo, qualification.qualifierRequired(typeInfo), false);
                outputBuilder.add(new QualifiedNameImpl(methodName, typeName,
                        qualification.qualifierRequired(methodInfo) ? QualifiedNameImpl.Required.YES : QualifiedNameImpl.Required.NO_METHOD));
                if (guideGenerator != null) start = true;
            } else if ((ve = object.asInstanceOf(VariableExpression.class)) != null &&
                       ve.variable() instanceof This thisVar) {
                //     (we check methodInspection is set, because of debugOutput)
                assert !methodInfo.isStatic() : "Have a static method with scope 'this'? "
                                                + methodInfo.fullyQualifiedName() + "; this "
                                                + thisVar.typeInfo().fullyQualifiedName();
                TypeName typeName = TypeNameImpl.typeName(thisVar.typeInfo(),
                        qualification.qualifierRequired(thisVar.typeInfo()), false);
                ThisName thisName = new ThisNameImpl(thisVar.writeSuper(), typeName,
                        qualification.qualifierRequired(thisVar));
                boolean qualifierRequired = qualification.qualifierRequired(methodInfo);
                outputBuilder.add(new QualifiedNameImpl(methodName, thisName,
                        qualifierRequired ? QualifiedNameImpl.Required.YES : QualifiedNameImpl.Required.NO_METHOD));
                if (guideGenerator != null) start = true;
            } else {
                // next level is NOT a gg; if gg != null we're at the start of the chain
                outputBuilder.add(outputInParenthesis(qualification, precedence(), object));
                if (guideGenerator != null) outputBuilder.add(guideGenerator.start());
                outputBuilder.add(SymbolEnum.DOT);
                outputBuilder.add(new TextImpl(methodName));
            }
        }

        if (parameterExpressions.isEmpty()) {
            outputBuilder.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        } else {
            outputBuilder
                    .add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(parameterExpressions.stream()
                            .map(expression -> expression.print(qualification))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (start) {
            outputBuilder.add(guideGenerator.start());
        }
        if (last) {
            outputBuilder.add(gg.end());
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(object.variables(descendMode),
                parameterExpressions.stream().flatMap(e -> e.variables(descendMode)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(typeArguments.stream().flatMap(ParameterizedType::typesReferencedMadeExplicit),
                Stream.concat(object.typesReferenced(),
                        parameterExpressions.stream().flatMap(Expression::typesReferenced)));
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression asExpression = translationMap.translateExpression(this);
        if (asExpression != this) return asExpression;

        List<MethodInfo> translatedMethod = translationMap.translateMethod(methodInfo);
        Expression translatedObject = object.translate(translationMap);
        ParameterizedType translatedReturnType = translationMap.translateType(concreteReturnType);
        List<Expression> translatedParameters = parameterExpressions.isEmpty() ? parameterExpressions :
                parameterExpressions.stream().map(e -> e.translate(translationMap))
                        .filter(e -> !e.isEmpty()) // allows for removal of certain arguments
                        .collect(translationMap.toList(parameterExpressions));
        List<ParameterizedType> trTypeArgs = typeArguments.stream()
                .map(translationMap::translateType)
                .collect(translationMap.toList(typeArguments));
        String newModificationTimes = Objects.requireNonNullElse(
                translationMap.modificationTimes(this, translatedObject, translatedParameters),
                modificationTimes);
        if (translatedMethod.size() == 1 && translatedMethod.getFirst() == methodInfo && translatedObject == object
            && translatedReturnType == concreteReturnType
            && translatedParameters == parameterExpressions
            && newModificationTimes.equals(modificationTimes)
            && trTypeArgs == typeArguments
            && (propertyValueMap.isEmpty() || !translationMap.isClearAnalysis())) {
            return this;
        }
        MethodCall translatedMc = new MethodCallImpl(comments(), source(), translatedObject, objectIsImplicit,
                translatedMethod.getFirst(), translatedParameters, translatedReturnType, trTypeArgs, newModificationTimes,
                translationMap.isClearAnalysis() ? new PropertyValueMapImpl() : propertyValueMap);
        if (translationMap.translateAgain() && !this.equals(translatedMc)) {
            return translatedMc.translate(translationMap);
        }
        return translatedMc;
    }

    @Override
    public PropertyValueMap analysis() {
        return propertyValueMap;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        List<Expression> rewiredParams = parameterExpressions.stream().map(e -> e.rewire(infoMap)).toList();
        return new MethodCallImpl(comments(), source(), object.rewire(infoMap), objectIsImplicit,
                infoMap.methodInfo(methodInfo), rewiredParams, concreteReturnType.rewire(infoMap),
                typeArguments.stream().map(pt -> pt.rewire(infoMap)).toList(),
                modificationTimes,
                propertyValueMap.rewire(infoMap));
    }
}
