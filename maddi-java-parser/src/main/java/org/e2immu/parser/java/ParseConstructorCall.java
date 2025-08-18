package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.e2immu.parser.java.erasure.ConstructorCallErasure;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParseConstructorCall extends CommonParse {
    private final static Logger LOGGER = LoggerFactory.getLogger(ParseConstructorCall.class);

    protected ParseConstructorCall(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Expression parse(Context context,
                            String index,
                            ForwardType forwardType,
                            org.parsers.java.ast.AllocationExpression aeIn) {
        AllocationExpression ae;
        Node unparsedObject;
        Context newContext;
        if (aeIn instanceof DotNew dn) {
            unparsedObject = dn.getFirst();
            assert Token.TokenType.DOT.equals(dn.get(1).getType());
            ae = (AllocationExpression) dn.get(2);
            Expression object = parsers.parseExpression().parse(context, index, context.emptyForwardType(), unparsedObject);
            TypeInfo typeInfo = object.parameterizedType().bestTypeInfo();
            if (typeInfo != null) {
                // see TestConstructor,15 for an example
                newContext = context.newTypeContext();
                newContext.typeContext().addSubTypesOfHierarchyReturnAllDefined(typeInfo,
                        TypeContext.SUBTYPE_HIERARCHY_ANONYMOUS);
            } else {
                newContext = context; // type is Object... nothing to add
            }
        } else {
            ae = aeIn;
            unparsedObject = null;
            newContext = context;
        }
        assert ae.getFirst() instanceof KeyWord kw && Token.TokenType.NEW.equals(kw.getType());
        int i = 1;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        List<ParameterizedType> methodTypeArguments;
        // new <String>Parameterized(...) == generics on the constructor, see TestConstructor,2
        if (ae.get(1) instanceof TypeArguments tas) {
            methodTypeArguments = parsers.parseType().parseTypeArguments(newContext, tas, true,
                    detailedSourcesBuilder);
            ++i;
        } else {
            methodTypeArguments = List.of();
        }
        ParameterizedType typeAsIs = parsers.parseType().parse(newContext, ae.get(i), detailedSourcesBuilder);
        TypeInfo typeInfo = typeAsIs.typeInfo();
        assert typeInfo != null;
        ParameterizedType formalType = typeInfo.asParameterizedType();
        Source source1 = source(index, ae);
        // all other aspects of detailed sources can be found in the components of the ConstructorCall object
        Source source = detailedSourcesBuilder == null ? source1 : source1.withDetailedSources(detailedSourcesBuilder.build());

        if (forwardType.erasure()) {
            i++;
            ParameterizedType erasureType;
            if (i < ae.size() && ae.get(i) instanceof ArrayDimsAndInits ada) {
                int arrays = countArrays(ada);
                erasureType = formalType.copyWithArrays(arrays);
            } else {
                erasureType = formalType;
            }
            return new ConstructorCallErasure(runtime, source, erasureType);
        }

        Diamond diamond;
        ++i;
        ParameterizedType expectedConcreteType;
        if (ae.get(i) instanceof DiamondOperator) {
            diamond = runtime.diamondYes();
            if (forwardType.type() == null || forwardType.type().isVoid()) {
                expectedConcreteType = null;
            } else {
                expectedConcreteType = inferDiamond(newContext, typeAsIs.typeInfo(), forwardType.type());
            }
            i++;
        } else {
            diamond = typeAsIs.parameters().isEmpty() ? runtime.diamondNo() : runtime.diamondShowAll();
            expectedConcreteType = typeAsIs;
        }

        List<Comment> comments = comments(ae);

        if (ae.get(i) instanceof InvocationArguments ia) {
            boolean isAnonymousType = i + 1 < ae.size() && ae.get(i + 1) instanceof ClassOrInterfaceBody;
            List<Object> unparsedArguments = new ArrayList<>();
            int j = 1;
            while (j < ia.size() && !(ia.get(j) instanceof Delimiter)) {
                unparsedArguments.add(ia.get(j));
                j += 2;
            }
            Source unparsedObjectSource = unparsedObject == null ? runtime.noSource() : source(unparsedObject);
            Expression constructorCall = newContext.methodResolution().resolveConstructor(newContext, comments, source,
                    index, formalType, expectedConcreteType, diamond, unparsedObject, unparsedObjectSource, unparsedArguments,
                    methodTypeArguments, !isAnonymousType, !forwardType.erasureOnFailure());
            if (constructorCall == null && !isAnonymousType) {
                return new ConstructorCallErasure(runtime, source, formalType);
            }
            // inAnonymous() call to prevent parsing the method body when we're re-evaluating the arguments
            if (isAnonymousType) {
                return anonymousType(newContext, comments, source, constructorCall, expectedConcreteType, typeAsIs, ia,
                        (ClassOrInterfaceBody) (ae.get(i + 1)), diamond, methodTypeArguments);
            }
            return constructorCall;
        }
        if (ae.get(i) instanceof ArrayDimsAndInits ada) {
            return arrayCreation(newContext, index, ada, typeInfo, diamond, source, comments);
        }
        throw new Summary.ParseException(newContext, "Expected InvocationArguments or ArrayDimsAndInits, got "
                                                     + ae.get(i).getClass());
    }

    private int countArrays(ArrayDimsAndInits ada) {
        return (int) ada.children().stream()
                .filter(c -> Token.TokenType.LBRACKET.equals(c.getType()))
                .count();
    }

    private ConstructorCall arrayCreation(Context context,
                                          String index,
                                          ArrayDimsAndInits ada,
                                          TypeInfo typeInfo,
                                          Diamond diamond,
                                          Source source,
                                          List<Comment> comments) {
        org.e2immu.language.cst.api.expression.ArrayInitializer initializer;
        ForwardType fwdIndex = context.newForwardType(runtime.intParameterizedType());
        int j = 0;
        boolean haveDimensions = false;
        List<Expression> expressions = new ArrayList<>();
        while (j < ada.size() && !(ada.get(j) instanceof ArrayInitializer)) {
            if (Token.TokenType.LBRACKET.equals(ada.get(j).getType())) {
                j++;
                if (Token.TokenType.RBRACKET.equals(ada.get(j).getType())) {
                    // unknown array dimension
                    expressions.add(runtime.newEmptyExpression());
                    j++;
                } else if (ada.get(j) instanceof org.parsers.java.ast.Expression expression) {
                    Expression e = parsers.parseExpression().parse(context, index, fwdIndex, expression);
                    expressions.add(e);
                    j += 2;
                    haveDimensions = true;
                }
            }
        }
        ParameterizedType formalReturnType = runtime.newParameterizedType(typeInfo, expressions.size());
        MethodInfo constructor = runtime.newArrayCreationConstructor(formalReturnType);
        ParameterizedType concreteReturnType;
        if (haveDimensions) {
            initializer = null;
            concreteReturnType = formalReturnType;
        } else {
            assert j < ada.size();
            if (ada.get(j) instanceof ArrayInitializer ai) {
                LOGGER.debug("Parsing array initializer");
                ForwardType fwd = context.newForwardType(formalReturnType);
                initializer = (org.e2immu.language.cst.api.expression.ArrayInitializer) parsers.parseExpression()
                        .parse(context, index, fwd, ai);
                concreteReturnType = initializer.parameterizedType();
            } else throw new Summary.ParseException(context, "Expected array initializer");
        }
        return runtime.newConstructorCallBuilder()
                .setObject(null)
                .setParameterExpressions(expressions)
                .setConstructor(constructor)
                .setConcreteReturnType(concreteReturnType)
                .setArrayInitializer(initializer)
                .setDiamond(diamond)
                .setSource(source)
                .addComments(comments)
                .build();
    }


    private ParameterizedType inferDiamond(Context context, TypeInfo formalType, ParameterizedType type) {
        if (type.typeInfo() == formalType) return type;
        ParameterizedType formalPt = formalType.asParameterizedType();
        Map<NamedType, ParameterizedType> typeParameterMap = context.genericsHelper().translateMap(formalPt, type,
                false);
        return formalPt.applyTranslation(runtime, typeParameterMap);
    }

    private ConstructorCall anonymousType(Context context,
                                          List<Comment> comments,
                                          Source source,
                                          Expression constructorCall,
                                          ParameterizedType forwardType,
                                          ParameterizedType type,
                                          InvocationArguments ia,
                                          ClassOrInterfaceBody body,
                                          Diamond diamond,
                                          List<ParameterizedType> methodTypeArguments) {
        TypeInfo anonymousType = runtime.newAnonymousType(context.enclosingType(),
                context.enclosingType().builder().getAndIncrementAnonymousTypes());
        TypeNature typeNature = runtime.typeNatureClass();
        TypeInfo.Builder builder = anonymousType.builder();
        builder.setSource(source)
                .setTypeNature(typeNature)
                .setAccess(runtime.accessPrivate())
                .setEnclosingMethod(context.enclosingMethod());
        ParameterizedType concreteReturnType = diamond.isShowAll() ? type : forwardType;
        if (concreteReturnType.typeInfo().isInterface()) {
            builder.addInterfaceImplemented(concreteReturnType);
            builder.setParentClass(runtime.objectParameterizedType());
        } else {
            builder.setParentClass(concreteReturnType);
        }
        MethodInfo constructor;
        List<Expression> arguments;
        if (constructorCall instanceof ConstructorCall cc) {
            constructor = cc.constructor();
            arguments = cc.parameterExpressions();
        } else {
            constructor = null;
            arguments = List.of();
        }
        Context newContext = context.newAnonymousClassBody(anonymousType);
        // we must not only add the types of the enclosing type (this happens inside newAnonymousClassBody()), but
        // also those of the type we're extending:
        newContext.typeContext().addSubTypesOfHierarchyReturnAllDefined(concreteReturnType.typeInfo(),
                TypeContext.SUBTYPE_HIERARCHY_ANONYMOUS);
        parsers.parseTypeDeclaration().parseBody(newContext, body, typeNature, anonymousType, builder, null);
        newContext.resolver().resolve(false);
        builder.commit();
        return runtime.newConstructorCallBuilder()
                .setSource(source)
                .addComments(comments)
                .setDiamond(diamond)
                .setConstructor(constructor)
                .setConcreteReturnType(concreteReturnType)
                .setParameterExpressions(arguments)
                .setTypeArguments(methodTypeArguments)
                .setAnonymousClass(anonymousType).build();
    }

    public Expression parseEnumConstructor(Context context, String index, TypeInfo enumType, InvocationArguments ia) {
        List<Object> unparsedArguments = new ArrayList<>();
        int j = 1;
        while (j < ia.size() && !(ia.get(j) instanceof Delimiter)) {
            unparsedArguments.add(ia.get(j));
            j += 2;
        }
        ParameterizedType formalType = enumType.asSimpleParameterizedType();
        return context.methodResolution().resolveConstructor(context, List.of(), source(ia),
                index, formalType, formalType, runtime.diamondNo(), null, null,
                unparsedArguments, List.of(),
                true, true);
    }
}

