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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.*;
import org.e2immu.parser.java.erasure.LambdaErasure;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ParseLambdaExpression extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseLambdaExpression.class);

    public ParseLambdaExpression(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Expression parse(Context context,
                            List<Comment> comments,
                            Source source,
                            String index,
                            ForwardType forwardType,
                            LambdaExpression le) {
        if (forwardType.erasure()) {
            return parseInErasureMode(context, source, le);
        }

        Lambda.Builder builder = runtime.newLambdaBuilder();

        List<Lambda.OutputVariant> outputVariants = new ArrayList<>();

        int typeIndex = context.enclosingType().builder().getAndIncrementAnonymousTypes();
        TypeInfo anonymousType = runtime.newAnonymousType(context.enclosingType(), typeIndex);
        assert source != null;
        anonymousType.builder()
                .setSource(source)
                .setAccess(runtime.accessPrivate())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType());

        MethodTypeParameterMap forwardSingleAbstractMethod = forwardType.computeSAM(runtime, context.genericsHelper(),
                context.enclosingType());
        MethodTypeParameterMap singleAbstractMethod;
        if (forwardSingleAbstractMethod == null) {
            singleAbstractMethod = someFunction(context, le);
        } else {
            singleAbstractMethod = forwardSingleAbstractMethod;
        }

        MethodInfo sam = singleAbstractMethod.methodInfo();
        MethodInfo methodInfo = runtime.newMethod(anonymousType, sam.name(), runtime.methodTypeMethod());
        MethodInfo.Builder miBuilder = methodInfo.builder();
        miBuilder.setAccess(runtime.accessPublic());
        miBuilder.setSynthetic(true);

        parseParameters(context, forwardType, le, miBuilder, outputVariants, context, singleAbstractMethod);
        ParameterizedType returnTypeOfLambda = singleAbstractMethod.getConcreteReturnType(runtime);

        miBuilder.setReturnType(returnTypeOfLambda);

        // a lambda does not start a new type context, simply a new variable context. See e.g. TestOverload1, 5.
        Context newContext = context.newVariableContext("lambda");
        methodInfo.parameters().forEach(newContext.variableContext()::add);

        // add all formal -> concrete of the parameters of the SAM, without the return type
        Map<NamedType, ParameterizedType> extra = new HashMap<>();
        methodInfo.parameters().forEach(pi -> {
            Map<NamedType, ParameterizedType> map = pi.parameterizedType().initialTypeParameterMap();
            extra.putAll(map);
        });
        ForwardType newForward = context.newForwardType(returnTypeOfLambda, false, extra);

        // start evaluation

        ParameterizedType concreteReturnType;
        Block methodBody;
        Node le1 = le.get(1);
        if (le1 instanceof org.parsers.java.ast.Expression e) {
            // simple function, supplier, or consumer (void)
            Expression expression = parsers.parseExpression().parse(newContext, "0", newForward, e);
            concreteReturnType = expression.parameterizedType();
            Source expressionSource = source("0", e);
            Statement statement;
            if (concreteReturnType.isVoid()) {
                statement = runtime.newExpressionAsStatementBuilder()
                        .setSource(expressionSource)
                        .setExpression(expression)
                        .build();
            } else {
                statement = runtime.newReturnBuilder()
                        .setSource(expressionSource)
                        .setExpression(expression)
                        .build();
            }
            methodBody = runtime.newBlockBuilder().addStatement(statement).setSource(source(e)).build();
        } else if (le1 instanceof CodeBlock codeBlock) {
            Context newContextMi = newContext.withEnclosingMethod(methodInfo);
            methodBody = parsers.parseBlock().parse(newContextMi, "", null, codeBlock);
            concreteReturnType = mostSpecificReturnType(context.enclosingType(), methodBody);
        } else {
            throw new Summary.ParseException(context, "Expected either expression or code block");
        }

        miBuilder.setMethodBody(methodBody);
        miBuilder.setReturnType(concreteReturnType);
        miBuilder.commit();

        List<ParameterizedType> types = methodInfo.parameters().stream().map(ParameterInfo::parameterizedType).toList();
        ParameterizedType functionalType = singleAbstractMethod.inferFunctionalType(runtime, types, concreteReturnType);

        anonymousType.builder()
                .addMethod(methodInfo)
                .addInterfaceImplemented(functionalType)
                .setEnclosingMethod(context.enclosingMethod())
                .setSingleAbstractMethod(methodInfo)
                .commit();

        return builder
                .setMethodInfo(methodInfo)
                .setOutputVariants(outputVariants)
                .addComments(comments)
                .setSource(source)
                .build();
    }

    private ParameterizedType mostSpecificReturnType(TypeInfo primaryType, Block methodBody) {
        AtomicReference<ParameterizedType> mostSpecific = new AtomicReference<>();
        methodBody.visit(statement -> {
            if (statement instanceof org.e2immu.language.cst.api.statement.ReturnStatement returnStatement) {
                Expression expression = returnStatement.expression();
                if (expression.isEmpty()) {
                    mostSpecific.set(runtime.voidParameterizedType());
                } else if (expression.isNullConstant()) {
                    if (mostSpecific.get() == null) {
                        mostSpecific.set(runtime.objectParameterizedType());
                    }
                } else {
                    ParameterizedType returnType = expression.parameterizedType();
                    mostSpecific.set(mostSpecific.get() == null ? returnType : mostSpecific.get()
                            .mostSpecific(runtime, primaryType, returnType));
                }
                return false;
            }
            if (statement instanceof Lambda || statement instanceof ConstructorCall) {
                return false; // do not go into lambdas, anonymous types, because they have a different return type
            }
            return true;
        });
        return mostSpecific.get() == null ? runtime.voidParameterizedType() : mostSpecific.get();
    }

    private MethodTypeParameterMap someFunction(Context context, LambdaExpression le) {
        if (le.getFirst() instanceof LambdaLHS lhs) {
            int numParams = computeNumberOfParameters(lhs);
            TypeInfo synthetic = runtime.syntheticFunctionalType(numParams, true);
            return context.genericsHelper().newMethodTypeParameterMap(synthetic.singleAbstractMethod(), Map.of());
        } else {
            throw new Summary.ParseException(context, "Expected lambda lhs");
        }
    }

    private static int computeNumberOfParameters(LambdaLHS lhs) {
        int numParams;
        Node first = lhs.getFirst();
        numParams = switch (first) {
            case Identifier nodes -> 1; // x -> ...
            case LambdaParameters lps -> (lps.size() - 1) / 2; // (int i, String j) -> ...
            case Delimiter d when Token.TokenType.LPAREN.equals(d.getType()) -> (lhs.size() - 1) / 2; // (i, j) -> ...
            case null, default -> throw new UnsupportedOperationException();
        };
        return numParams;
    }

    private void parseParameters(Context context,
                                 ForwardType forwardType,
                                 LambdaExpression le,
                                 MethodInfo.Builder miBuilder,
                                 List<Lambda.OutputVariant> outputVariants,
                                 Context newContext,
                                 MethodTypeParameterMap sam) {
        if (!(le.getFirst() instanceof LambdaLHS lhs)) {
            throw new Summary.ParseException(context, "Expected lambda lhs");
        }
        Node lhs0 = lhs.getFirst();
        if (lhs0 instanceof Identifier || lhs0 instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
            // single variable, no type given. we must extract it from the forward type, which must be a functional interface
            ParameterizedType type = sam.getConcreteTypeOfParameter(runtime, 0);
            ParameterInfo pi;
            if (lhs0 instanceof Identifier identifier) {
                String parameterName = identifier.getSource();
                pi = miBuilder.addParameter(parameterName, type);
            } else {
                pi = miBuilder.addUnnamedParameter(type);
            }
            Source source = source(lhs);
            DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(pi.name(), source(lhs0));
            pi.builder().setSource(detailedSourcesBuilder == null ? source
                    : source.withDetailedSources(detailedSourcesBuilder.build()));

            outputVariants.add(runtime.lambdaOutputVariantEmpty());
            pi.builder().commit();
            newContext.variableContext().add(pi);
        } else if (lhs0 instanceof LambdaParameters lambdaParameters) {
            assert lambdaParameters.getFirst() instanceof Delimiter d && Token.TokenType.LPAREN.equals(d.getType());
            // we have a list of parameters, with type
            int i = 1;
            while (i < lambdaParameters.size()) {
                if (lambdaParameters.get(i) instanceof LambdaParameter lp) {
                    ParameterizedType type;
                    Lambda.OutputVariant outputVariant;
                    DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

                    if (lp.get(0) instanceof Token kw && Token.TokenType.VAR.equals(kw.getType())) {
                        type = forwardType.type().parameters().get(1);
                        outputVariant = runtime.lambdaOutputVariantEmpty();
                    } else {
                        type = parsers.parseType().parse(context, lp.getFirst(), detailedSourcesBuilder);
                        outputVariant = runtime.lambdaOutputVariantTyped();
                    }
                    Node lp1 = lp.get(1);
                    ParameterInfo pi;
                    ParameterizedType boxedType = type.ensureBoxed(runtime);
                    if (lp1 instanceof Identifier identifier) {
                        pi = miBuilder.addParameter(identifier.getSource(), boxedType);
                    } else if (lp1 instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
                        pi = miBuilder.addUnnamedParameter(boxedType);
                    } else throw new UnsupportedOperationException();
                    outputVariants.add(outputVariant);

                    Source source = source(lp);
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(pi.name(), source(lp1));
                    pi.builder().setSource(detailedSourcesBuilder == null ? source
                            : source.withDetailedSources(detailedSourcesBuilder.build()));

                    pi.builder().commit();
                    newContext.variableContext().add(pi);
                } else if (lambdaParameters.get(i) instanceof Delimiter) {
                    break;
                } else throw new Summary.ParseException(context, "Expected LambdaParameter");
                if (Token.TokenType.RPAREN.equals(lambdaParameters.get(i + 1).getType())) break;
                i += 2;
            }
        } else {
            assert lhs0 instanceof Delimiter d && Token.TokenType.LPAREN.equals(d.getType());
            // we have a list of parameters, without type
            int i = 1;
            int paramIndex = 0;
            while (i < lhs.size()) {
                Node lhsI = lhs.get(i);
                Source source = source(lhsI);
                ParameterizedType type = sam.getConcreteTypeOfParameter(runtime, paramIndex);
                ParameterInfo pi;
                if (lhsI instanceof Identifier identifier) {
                    String parameterName = identifier.getSource();
                    pi = miBuilder.addParameter(parameterName, type);
                } else if (lhsI instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
                    pi = miBuilder.addUnnamedParameter(type);
                } else {
                    throw new Summary.ParseException(context, "Expected identifier");
                }
                outputVariants.add(runtime.lambdaOutputVariantEmpty());

                DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(pi.name(), source);
                pi.builder().setSource(detailedSourcesBuilder == null ? source
                        : source.withDetailedSources(detailedSourcesBuilder.build()));

                pi.builder().commit();
                newContext.variableContext().add(pi);

                if (Token.TokenType.RPAREN.equals(lhs.get(i + 1).getType())) break;
                paramIndex++;
                i += 2;
            }
        }
        miBuilder.commitParameters();
    }

    private Expression parseInErasureMode(Context context, Source source, LambdaExpression le) {
        int numParameters;
        if (le.getFirst() instanceof LambdaLHS lhs) {
            numParameters = countParameters(lhs);
        } else throw new Summary.ParseException(context, "Expected LambdaLHS");
        Set<MethodResolution.Count> erasures = Set.of(
                new MethodResolution.Count(numParameters, true),
                new MethodResolution.Count(numParameters, false));
        LOGGER.debug("Returning erasure {}", erasures);
        return new LambdaErasure(runtime, erasures, source);
    }

    private int countParameters(LambdaLHS lhs) {
        if (lhs.get(0) instanceof Identifier) return 1;
        if (lhs.get(0) instanceof LambdaParameters lp) {
            // () ->
            if (Token.TokenType.RPAREN.equals(lp.get(1).getType())) return 0;
            return (int) lp.stream().filter(n -> Token.TokenType.COMMA.equals(n.getType())).count() + 1;
        }
        // () ->
        if (Token.TokenType.RPAREN.equals(lhs.get(1).getType())) return 0;
        return (int) lhs.stream().filter(n -> Token.TokenType.COMMA.equals(n.getType())).count() + 1;
    }
}
