package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LambdaImpl extends ExpressionImpl implements Lambda {
    private final MethodInfo methodInfo;
    private final List<OutputVariant> outputVariants;

    public LambdaImpl(List<Comment> comments,
                      Source source,
                      MethodInfo methodInfo,
                      List<OutputVariant> outputVariants) {
        super(comments, source, 1 + methodInfo.complexity());
        this.methodInfo = methodInfo;
        assert methodInfo.typeInfo().compilationUnitOrEnclosingType().isRight();
        assert methodInfo.typeInfo().methods().size() == 1;
        assert methodInfo.isPublic() : "This method implements a functional interface, so it must be public";
        this.outputVariants = outputVariants;
    }

    @Override
    public Lambda withMethodInfoAndMethodBody(MethodInfo methodInfo) {
        return new LambdaImpl(comments(), source(), methodInfo, outputVariants);
    }

    @Override
    public Expression withSource(Source source) {
        return new LambdaImpl(comments(), source, methodInfo, outputVariants);
    }

    public enum OutputVariantImpl implements OutputVariant {
        TYPED, VAR, EMPTY;

        @Override
        public OutputBuilder print(ParameterInfo parameterInfo, Qualification qualification) {
            OutputBuilder ob = new OutputBuilderImpl();
            if (this != EMPTY) {
                //Stream<OutputBuilder> annotationStream = parameterInfo.buildAnnotationOutput(qualification);
                //OutputBuilder annotationOutput = annotationStream
                //       .collect(OutputBuilderImpl.joining(SpaceEnum.ONE_REQUIRED_EASY_SPLIT, Guide.generatorForAnnotationList()));
                if (this == TYPED) {
                    ob.add(parameterInfo.parameterizedType()
                                    .print(qualification, parameterInfo.isVarArgs(), DiamondEnum.SHOW_ALL))
                            .add(SpaceEnum.ONE);
                }
                if (this == VAR) {
                    ob.add(KeywordImpl.VAR).add(SpaceEnum.ONE);
                }
                //if (!annotationOutput.isEmpty()) {
                //    return annotationOutput.add(SpaceEnum.ONE_REQUIRED_EASY_SPLIT).add(ob);
                //}
            }
            return ob;
        }

        @Override
        public boolean isEmpty() {
            return this == EMPTY;
        }

        @Override
        public boolean isVar() {
            return this == VAR;
        }

        @Override
        public boolean isTyped() {
            return this == TYPED;
        }
    }

    public static class Builder extends ElementImpl.Builder<Lambda.Builder> implements Lambda.Builder {
        private MethodInfo methodInfo;
        private List<OutputVariant> outputVariants;

        @Override
        public Builder setMethodInfo(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            return this;
        }


        @Override
        public Builder setOutputVariants(List<OutputVariant> outputVariants) {
            this.outputVariants = outputVariants;
            return this;
        }

        @Override
        public Lambda build() {
            return new LambdaImpl(comments, source, methodInfo, outputVariants);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LambdaImpl lambda)) return false;
        return methodInfo.equals(lambda.methodInfo);
    }

    @Override
    public int hashCode() {
        return methodInfo.hashCode();
    }

    @Override
    public MethodInfo methodInfo() {
        return methodInfo;
    }

    @Override
    public List<OutputVariant> outputVariants() {
        return outputVariants;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_LAMBDA;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof Lambda l) {
            return methodInfo.fullyQualifiedName().compareTo(l.methodInfo().fullyQualifiedName());
        }
        throw new InternalCompareToException();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            Expression single = singleExpression();
            if (single != null) {
                single.visit(predicate);
            } else {
                methodInfo.methodBody().visit(predicate);
            }
        }
    }

    private Expression singleExpression() {
        List<Statement> statements = methodInfo.methodBody().statements();
        if (statements.size() == 1) {
            return statements.get(0).expression();
        }
        return null;
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            Expression single = singleExpression();
            if (single != null) {
                single.visit(visitor);
            } else {
                visitor.startSubBlock(0);
                methodInfo.methodBody().visit(visitor);
                visitor.endSubBlock(0);
            }
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        List<ParameterInfo> parameters = methodInfo.parameters();
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (parameters.isEmpty()) {
            outputBuilder.add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        } else if (parameters.size() == 1 && outputVariants.get(0).isEmpty()) {
            outputBuilder.add(parameters.get(0).print(qualification));
        } else {
            outputBuilder.add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(parameters.stream().map(pi -> outputVariants.get(pi.index())
                                    .print(pi, qualification)
                                    .add(pi.print(qualification)))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        outputBuilder.add(SymbolEnum.LAMBDA);

        Expression singleExpression = singleExpression();
        if (singleExpression != null) {
            outputBuilder.add(outputInParenthesis(qualification, precedence(), singleExpression));
        } else {
            outputBuilder.add(methodInfo.methodBody().print(qualification));
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return methodInfo.methodBody().variables(descendMode);
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(methodInfo.parameters().stream().flatMap(ParameterInfo::typesReferenced),
                methodInfo.methodBody().typesReferenced());
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression tLambda = translationMap.translateExpression(this);
        if (tLambda == null) return this;
        if (tLambda != this) return tLambda;
        TypeInfo tTypeInfo = methodInfo.typeInfo().translate(translationMap).getFirst();
        if (tTypeInfo != methodInfo.typeInfo()) {
            assert tTypeInfo.methods().size() == 1;
            MethodInfo implementationOfSam = tTypeInfo.methods().getFirst();
            return new LambdaImpl(comments(), source(), implementationOfSam, outputVariants);
        }
        return this;
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        MethodInfo rewired = infoMap.typeInfoRecurseAllPhases(methodInfo.typeInfo()).singleAbstractMethod();
        assert rewired != null;
        return new LambdaImpl(comments(), source(), rewired, outputVariants);
    }
}
