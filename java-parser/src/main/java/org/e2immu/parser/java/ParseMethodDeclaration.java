package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.MethodModifier;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ParseMethodDeclaration extends CommonParse {
    private final Logger LOGGER = LoggerFactory.getLogger(ParseTypeDeclaration.class);

    public ParseMethodDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public MethodInfo parse(Context context, Node md, List<ParseTypeDeclaration.RecordField> recordFields) {
        try {
            return internalParse(context, md, recordFields);
        } catch (Summary.FailFastException ffe) {
            throw ffe;
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception parsing method in type {}, source {}", context.info(), source(md).compact2());
            context.summary().addParseException(new Summary.ParseException(context.enclosingType().compilationUnit(),
                    context.enclosingType(), re.getMessage(), re));
            return null;
        }
    }

    private MethodInfo internalParse(Context context, Node md, List<ParseTypeDeclaration.RecordField> recordFields) {
        int i = 0;
        List<Annotation> annotations = new ArrayList<>();
        List<MethodModifier> methodModifiers = new ArrayList<>();
        List<Node> typeParametersToParse = new ArrayList<>();
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        label:
        while (true) {
            Node mdi = md.get(i);
            switch (mdi) {
                case Annotation a:
                    annotations.add(a);
                    break;
                case Modifiers modifiers:
                    for (Node node : modifiers.children()) {
                        if (node instanceof Annotation a) {
                            annotations.add(a);
                        } else if (node instanceof KeyWord keyWord) {
                            MethodModifier m = methodModifier(keyWord);
                            methodModifiers.add(m);
                            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
                        }
                    }
                    break;
                case KeyWord keyWord:
                    MethodModifier m = methodModifier(keyWord);
                    methodModifiers.add(m);
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
                    break;
                case TypeParameters nodes:
                    int j = 1;
                    while (j < nodes.size()) {
                        typeParametersToParse.add(nodes.get(j));
                        j += 2;
                    }
                    break;
                case null:
                default:
                    break label;
            }
            i++;
        }
        ReturnType rt;
        boolean constructor = md instanceof ConstructorDeclaration;
        boolean compactConstructor = md instanceof CompactConstructorDeclaration;
        if (constructor || compactConstructor) {
            rt = null;
        } else if (md.get(i) instanceof ReturnType) {
            rt = (ReturnType) md.get(i);
            i++;
        } else throw new UnsupportedOperationException();

        MethodInfo.MethodType methodType;
        if (compactConstructor) {
            methodType = runtime.methodTypeCompactConstructor();
        } else if (constructor) {
            methodType = runtime.methodTypeConstructor();
        } else if (methodModifiers.contains(runtime.methodModifierDefault())) {
            methodType = runtime.methodTypeDefaultMethod();
        } else if (methodModifiers.contains(runtime.methodModifierStatic())) {
            methodType = runtime.methodTypeStaticMethod();
        } else if (methodModifiers.contains(runtime.methodModifierAbstract())
                   || context.enclosingType().isInterface()) {
            methodType = runtime.methodTypeAbstractMethod();
        } else {
            methodType = runtime.methodTypeMethod();
        }

        String name;
        if (md.get(i) instanceof Identifier identifier) {
            name = methodType.isConstructor() ? MethodInfo.CONSTRUCTOR_NAME : identifier.getSource();
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(name, source(identifier));
            i++;
        } else throw new UnsupportedOperationException();

        MethodInfo methodInfo = runtime.newMethod(context.enclosingType(), name, methodType);
        MethodInfo.Builder builder = methodInfo.builder();

        parseAnnotations(context, builder, annotations);

        Context contextWithTP = context.newTypeContext();

        if (!typeParametersToParse.isEmpty()) {
            List<TypeParameter> typeParameters = new ArrayList<>();
            int typeParameterIndex = 0;
            for (Node tp : typeParametersToParse) {
                TypeParameter typeParameter = parseTypeParameterDoNotInspect(tp, methodInfo, typeParameterIndex++);
                typeParameters.add(typeParameter);
                builder.addTypeParameter(typeParameter);
                contextWithTP.typeContext().addToContext(typeParameter, TypeContext.TYPE_PARAMETER_PRIORITY);
            }
            parseAndResolveTypeParameterBounds(typeParametersToParse, typeParameters, contextWithTP);
        }

        // we need the type parameters in the context, because the return type may be one of them/contain one
        ParameterizedType returnType = rt == null ? runtime.parameterizedTypeReturnTypeOfConstructor()
                : parsers.parseType().parse(contextWithTP, rt, detailedSourcesBuilder);
        builder.setReturnType(returnType);
        ForwardType forwardType = contextWithTP.newForwardType(returnType);
        Context newContext = contextWithTP.newVariableContextForMethodBlock(methodInfo, forwardType);

        List<org.e2immu.language.cst.api.statement.Statement> recordAssignments;
        if (md.get(i) instanceof FormalParameters fps) {
            for (Node child : fps.children()) {
                if (child instanceof FormalParameter fp) {
                    parseFormalParameter(newContext, builder, fp);
                }
            }
            if (detailedSourcesBuilder != null) {
                Delimiter delimiter = (Delimiter) fps.getLast();
                detailedSourcesBuilder.put(DetailedSources.END_OF_PARAMETER_LIST, source(delimiter));
            }
            i++;
            recordAssignments = null;
        } else if (compactConstructor) {
            LOGGER.debug("Adding synthetic parameters");
            recordAssignments = new ArrayList<>(recordFields.size());
            Source noSource = runtime.noSource();
            for (ParseTypeDeclaration.RecordField recordField : recordFields) {
                ParameterInfo pi = builder.addParameter(recordField.fieldInfo().name(), recordField.fieldInfo().type());
                pi.builder().setVarArgs(recordField.varargs());
                pi.builder().setIsFinal(false);
                FieldReference fr = runtime.newFieldReference(recordField.fieldInfo());
                Assignment assignment = runtime.newAssignmentBuilder()
                        .setTarget(runtime.newVariableExpressionBuilder().setVariable(fr).setSource(noSource).build())
                        .setValue(runtime.newVariableExpressionBuilder().setVariable(pi).setSource(noSource).build())
                        .setSource(noSource)
                        .build();
                var statement = runtime.newExpressionAsStatementBuilder()
                        .setSource(noSource)
                        .setExpression(assignment).build();
                recordAssignments.add(statement);
            }
        } else {
            throw new UnsupportedOperationException("Node " + md.get(i).getClass());
        } // a constructor can be a "compact" one in records
        if (md.get(i) instanceof ThrowsList throwsList) {
            for (int j = 1; j < throwsList.size(); j += 2) {
                ParameterizedType pt = parsers.parseType().parse(newContext, throwsList.get(j), detailedSourcesBuilder);
                builder.addExceptionType(pt);
            }
            i++;
        }
        while (i < md.size() && md.get(i) instanceof Delimiter) i++;

        ExplicitConstructorInvocation explicitConstructorInvocation;
        if (i < md.size() && md.get(i) instanceof org.parsers.java.ast.ExplicitConstructorInvocation eci) {
            explicitConstructorInvocation = eci;
            i++;
        } else {
            explicitConstructorInvocation = null;
        }
        Node toResolve;
        Node cdi = i >= md.size() ? null : md.get(i);
        if (compactConstructor) {
            toResolve = md; // because the statements simply follow the identifier
        } else if (cdi instanceof Statement statement) {
            toResolve = statement;
        } else {
            toResolve = null;
        }
        if (toResolve != null || explicitConstructorInvocation != null) {
            Context resContext = context.newVariableContextForMethodBlock(methodInfo, null);
            resContext.resolver().add(methodInfo, builder, resContext.emptyForwardType(), explicitConstructorInvocation,
                    toResolve, newContext, recordAssignments);
        } else {
            builder.setMethodBody(runtime.emptyBlock());
        }

        builder.commitParameters();
        methodModifiers.forEach(builder::addMethodModifier);
        builder.computeAccess();
        builder.addComments(comments(md, context, methodInfo, builder));
        Source source = source(md);
        builder.setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()));
        return methodInfo;
    }

    private void parseFormalParameter(Context context, MethodInfo.Builder builder, FormalParameter fp) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        List<Annotation> annotations = new ArrayList<>();
        Node varargs = fp.stream()
                .filter(child -> child instanceof Token t && Token.TokenType.VAR_ARGS.equals(t.getType()))
                .findFirst().orElse(null);
        boolean isFinal = false;
        ParameterizedType pt = null;
        String parameterName = null;
        int arrayCount = 0;
        // these objects come is such weird orders, that we simply run over all of them
        for (Node child : fp) {
            if (child instanceof Modifiers) {
                annotations.addAll(child.childrenOfType(Annotation.class));
                isFinal |= child.stream().anyMatch(c -> c instanceof Token t && Token.TokenType.FINAL.equals(t.getType()));
            } else if (child instanceof Annotation a) {
                annotations.add(a);
            } else if (child instanceof Type type) {
                annotations.addAll(type.childrenOfType(Annotation.class)); // primitive array type, reference type
                pt = parsers.parseType().parse(context, type, true, varargs, detailedSourcesBuilder);
            } else if (child instanceof Identifier identifier) {
                parameterName = identifier.getSource();
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(parameterName, source(identifier));
            } else if (child instanceof VariableDeclaratorId vdi) {
                if (vdi.getFirst() instanceof Identifier identifier) {
                    parameterName = identifier.getSource();
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(parameterName, source(identifier));
                } else throw new Summary.ParseException(context, "Expect first part to be Identifier");
                arrayCount += (int) vdi.stream().filter(n -> Token.TokenType.LBRACKET.equals(n.getType())).count();
            } else if (child instanceof Token kw) {
                if (Token.TokenType.FINAL.equals(kw.getType())) {
                    isFinal = true;
                }
            }
        }
        if (pt == null) throw new UnsupportedOperationException("No type for formal parameter?");
        if (parameterName == null) throw new UnsupportedOperationException("No name for formal parameter?");
        ParameterizedType typeOfParameter;
        if (arrayCount > 0 || pt.arrays() > 0) {
            typeOfParameter = pt.copyWithArrays(pt.arrays() + arrayCount);
            if (detailedSourcesBuilder != null) {
                if (pt.arrays() > 0) {
                    pt = (ParameterizedType) detailedSourcesBuilder.getAssociated(pt);
                }
                detailedSourcesBuilder.putWithArrayToWithoutArray(typeOfParameter, pt);
            }
        } else {
            typeOfParameter = pt;
        }

        Source source = source(fp);
        ParameterInfo pi = builder.addParameter(parameterName, typeOfParameter);
        pi.builder().addComments(comments(fp))
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .setVarArgs(varargs != null)
                .setIsFinal(isFinal);

        // now that there is a builder, we can parse the annotations
        parseAnnotations(context, pi.builder(), annotations);

        // do not commit yet!
        context.variableContext().add(pi);
    }
}
