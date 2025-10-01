package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.parser.java.util.EscapeSequence;
import org.e2immu.parser.java.util.TextBlockParser;
import org.e2immu.util.internal.util.StringUtil;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.parsers.java.ast.ArrayInitializer;
import org.parsers.java.ast.MethodCall;
import org.parsers.java.ast.MethodReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.parsers.java.Token.TokenType.*;

public class ParseExpression extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseExpression.class);
    public static final String LENGTH = "length";

    public ParseExpression(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public Expression parseIgnoreComments(Context context, String index, ForwardType forwardType, Node node) {
        return parse(context, index, forwardType, node, true);
    }

    public Expression parse(Context context, String index, ForwardType forwardType, Node node) {
        return parse(context, index, forwardType, node, false);
    }

    public Expression parse(Context context, String index, ForwardType forwardType, Node node, boolean ignoreComments) {
        try {
            return internalParse(context, index, forwardType, node, ignoreComments);
        } catch (Throwable t) {
            LOGGER.error("Caught exception parsing expression at line {}, pos {}. Current info {}", node.getBeginLine(),
                    node.getBeginColumn(), context.info());
            throw t;
        }
    }

    private Expression internalParse(Context context, String index, ForwardType forwardType, Node node, boolean ignoreComments) {
        Source source = source(index, node);
        List<Comment> comments = ignoreComments ? List.of() : comments(node);

        return switch (node) {
            case DotName dotName -> parseDotName(context, comments, source, index, dotName);
            case MethodCall mc -> parsers.parseMethodCall().parse(context, comments, source, index, forwardType, mc);
            case LiteralExpression le -> parseLiteral(context, comments, source, le);
            case ConditionalAndExpression _, ConditionalOrExpression _ ->
                    parseConditionalExpression(context, comments, source, index, (org.parsers.java.ast.Expression) node);
            case AdditiveExpression ae -> parseAdditive(context, index, ae);
            case MultiplicativeExpression _,
                 AndExpression _,
                 InclusiveOrExpression _,
                 ExclusiveOrExpression _,
                 ShiftExpression _ -> parseMultiplicative(context, index, node);
            case RelationalExpression re -> parseRelational(context, index, re);
            case EqualityExpression eq -> parseEquality(context, index, eq);
            case UnaryExpression _, UnaryExpressionNotPlusMinus _ ->
                    parseUnaryExpression(context, index, (org.parsers.java.ast.Expression) node);
            case Identifier i -> parseIdentifier(context, comments, source, i);
            case Name name -> {
                if (name.children().size() >= 3) {
                    yield parseDottedName(context, comments, source, name, name.children().size() - 1);
                }
                // see TestMethodCall7.test6 for an example where we arrive here with "Collections.", size 2
                // recurse into first child; single identifier will end up in parseIdentifier
                yield parse(context, index, forwardType, name.getFirst());
            }
            case CastExpression castExpression -> parseCast(context, index, comments, source, castExpression);
            case AssignmentExpression assignmentExpression ->
                    parseAssignment(context, index, assignmentExpression, comments, source);
            case ArrayAccess arrayAccess ->
                    parseArrayAccess(context, index, forwardType, arrayAccess, comments, source);
            case ClassLiteral cl -> parseClassLiteral(context, comments, source, cl);
            case Parentheses p -> parseParentheses(context, index, comments, source, forwardType, p);
            case AllocationExpression ae -> parsers.parseConstructorCall().parse(context, index, forwardType, ae);
            case MethodReference mr ->
                    parsers.parseMethodReference().parse(context, comments, source, index, forwardType, mr);
            case LambdaExpression le ->
                    parsers.parseLambdaExpression().parse(context, comments, source, index, forwardType, le);
            case PostfixExpression _ -> plusPlusMinMin(context, index, comments, source, 0, 1, false, node);
            case PreIncrementExpression _, PreDecrementExpression _ ->
                    plusPlusMinMin(context, index, comments, source, 1, 0, true, node);
            case TernaryExpression _ -> inlineConditional(context, index, forwardType, comments, source, node);
            case ArrayInitializer _, MemberValueArrayInitializer _ ->
                    arrayInitializer(context, index, forwardType, comments, source, node);
            case org.parsers.java.ast.SwitchExpression _ ->
                    parseSwitchExpression(context, index, forwardType, comments, source, node.getFirst());
            case InstanceOfExpression ioe -> parseInstanceOf(context, index, forwardType, comments, source, ioe);
            case ObjectType ot -> {
                // maybe really hard-coded, but serves ParseMethodReference, e.g. TestMethodCall0,9
                if (ot.size() == 3 && Token.TokenType.DOT.equals(ot.get(1).getType())) {
                    yield parseDotName(context, comments, source, index, node);
                }
                // ditto, see TestParseMethodReference
                if (ot.size() == 1 && ot.getFirst() instanceof Identifier i) {
                    yield parseIdentifier(context, comments, source, i);
                }
                throw new UnsupportedOperationException();
            }
            case Annotation a -> parsers.parseAnnotationExpression().parseDirectly(context, a);
            case DotThis _, DotSuper _ -> parseDotThisDotSuper(context, node, source);
            case NullLiteral _ -> runtime.newNullConstant(comments, source);
            default -> {
                throw new UnsupportedOperationException("node " + node.getClass());
            }
        };
    }

    private Expression parseArrayAccess(Context context, String index, ForwardType forwardType,
                                        ArrayAccess arrayAccess, List<Comment> comments, Source source) {
        assert arrayAccess.size() == 4 : "Not implemented";
        ParameterizedType forwardPt = forwardType.type() == null
                ? null// better than runtime.objectParameterizedType().copyWithArrays(1)
                : forwardType.type().copyWithArrays(forwardType.type().arrays() + 1);
        ForwardType arrayForward = context.newForwardType(forwardPt);
        Expression ae = parse(context, index, arrayForward, arrayAccess.get(0));
        ForwardType fwdInt = context.newForwardType(runtime.intParameterizedType());
        Expression ie = parse(context, index, fwdInt, arrayAccess.get(2));
        Variable variable = runtime.newDependentVariable(ae, ie);
        return runtime.newVariableExpressionBuilder().addComments(comments).setSource(source)
                .setVariable(variable).build();
    }

    private Expression parseDotThisDotSuper(Context context, Node node, Source source) {
        ParameterizedType type;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        if (node.get(0) instanceof Name name) {
            type = parsers.parseType().parse(context, name, detailedSourcesBuilder);
        } else throw new Summary.ParseException(context, "expected Name");
        boolean isSuper = node instanceof DotSuper;
        TypeInfo explicitType = type.bestTypeInfo();
        return runtime.newVariableExpressionBuilder()
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .setVariable(runtime.newThis(type, explicitType, isSuper))
                .build();
    }

    private Expression parseInstanceOf(Context context, String index, ForwardType forwardType,
                                       List<Comment> comments, Source source, InstanceOfExpression ioe) {
        Expression expression = parse(context, index, forwardType, ioe.get(0));
        assert INSTANCEOF.equals(ioe.get(1).getType());
        ParameterizedType testType;
        RecordPattern recordPattern;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        Node ioe2 = ioe.get(2);
        if (ioe2 instanceof LocalVariableDeclaration nvd) {
            testType = parsers.parseType().parse(context, nvd.get(0), detailedSourcesBuilder);
            String lvName = nvd.get(1).getSource();
            LocalVariable patternVariable = runtime.newLocalVariable(lvName, testType);
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(patternVariable, source(nvd.get(1)));
            context.variableContext().add(patternVariable);
            recordPattern = runtime.newRecordPatternBuilder()
                    .setSource(source(nvd))
                    .setLocalVariable(patternVariable)
                    .build();
        } else if (ioe2 instanceof org.parsers.java.ast.RecordPattern rp) {
            recordPattern = parsers.parseRecordPattern().parseRecordPattern(context, rp);
            testType = recordPattern.recordType();
        } else {
            testType = parsers.parseType().parse(context, ioe2, detailedSourcesBuilder);
            recordPattern = null;
        }
        if (detailedSourcesBuilder != null && recordPattern != null) {
            detailedSourcesBuilder.put(recordPattern, source(ioe2));
        }
        return runtime.newInstanceOfBuilder()
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments)
                .setExpression(expression)
                .setTestType(testType)
                .setPatternVariable(recordPattern)
                .build();
    }


    private Expression parseSwitchExpression(Context context,
                                             String index,
                                             ForwardType forwardType,
                                             List<Comment> comments, Source source, Node node) {
        Expression selector = parsers.parseExpression().parse(context, index, context.emptyForwardType(),
                node.get(2));
        int n = (int) node.children().stream()
                .filter(child -> child instanceof NewCaseStatement)
                .count();
        ForwardType selectorTypeFwd = context.newForwardType(selector.parameterizedType());
        List<SwitchEntry> entries = new ArrayList<>();
        TypeInfo selectorTypeInfo = selector.parameterizedType().bestTypeInfo();
        Context enumContext;
        if (selectorTypeInfo.typeNature().isEnum()) {
            enumContext = context.newVariableContext("switch-expression-enum");
            selectorTypeInfo.fields().stream().filter(Info::isSynthetic)
                    .forEach(f -> enumContext.variableContext().add(runtime.newFieldReference(f)));
        } else {
            enumContext = null;
        }
        int count = 0;
        ParameterizedType commonType = null;
        for (Node child : node) {
            if (child instanceof NewCaseStatement ncs) {
                Context newContext = context.newVariableContext("switch-expression");
                SwitchEntry.Builder entryBuilder = runtime.newSwitchEntryBuilder()
                        .setSource(source(ncs)).addComments(comments(ncs));
                if (ncs.get(0) instanceof NewSwitchLabel nsl) {
                    parseNewSwitchLabel(index, nsl, newContext, entryBuilder, selectorTypeFwd, enumContext);
                } else throw new Summary.ParseException(newContext, "Expect NewCaseStatement");
                Node ncs1 = ncs.get(1);
                switch (ncs1) {
                    case CodeBlock cb -> {
                        String newIndex = index + "." + StringUtil.pad(count, n);
                        entryBuilder.setStatement(parsers.parseBlock().parse(newContext, newIndex, null, cb));
                    }
                    case Statement statement -> {
                        // throw statement is allowed!
                        String newIndex = index + "." + StringUtil.pad(count, n) + "0";
                        org.e2immu.language.cst.api.statement.Statement st = parsers.parseStatement()
                                .parse(newContext, newIndex, statement);
                        entryBuilder.setStatement(st);
                    }
                    case org.parsers.java.ast.Expression expression -> {
                        String newIndex = index + "." + StringUtil.pad(count, n) + "0";
                        Expression pe = parse(newContext, newIndex, forwardType, expression);
                        entryBuilder.setStatement(runtime.newExpressionAsStatementBuilder().setExpression(pe)
                                .setSource(pe.source()).build());
                        commonType = commonType == null ? pe.parameterizedType()
                                : runtime.commonType(commonType, pe.parameterizedType());
                    }
                    default -> throw new Summary.ParseException(newContext, "Expect statement, got " + ncs1.getClass());
                }
                count++;
                entries.add(entryBuilder.build());
            }
        }
        assert commonType != null;
        ParameterizedType parameterizedType = commonType.mostSpecific(runtime, context.enclosingType().primaryType(),
                forwardType.type());
        return runtime.newSwitchExpressionBuilder().addComments(comments).setSource(source)
                .setParameterizedType(parameterizedType)
                .setSelector(selector)
                .addSwitchEntries(entries).build();
    }

    private Expression parseClassLiteral(Context context, List<Comment> comments, Source source, ClassLiteral cl) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        ParameterizedType pt = parsers.parseType().parse(context, cl, detailedSourcesBuilder);
        return runtime.newClassExpressionBuilder(pt)
                .addComments(comments)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .build();
    }

    /*
    can be a type (scope of a method), or a variable
     */
    private Expression parseDottedName(Context context, List<Comment> comments, Source source, Name name, int endIncl) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        assert endIncl > 0 && endIncl < name.children().size();
        String trimmedAfterDot = name.get(endIncl).getSource();
        if (LENGTH.equals(trimmedAfterDot)) {
            Expression arrayExpression = parseDottedVariable(context, comments, source, name, endIncl - 2,
                    detailedSourcesBuilder);
            if (arrayExpression.parameterizedType().arrays() > 0) {
                return runtime.newArrayLengthBuilder()
                        .addComments(comments)
                        .setSource(source)
                        .setExpression(arrayExpression)
                        .build();
            }
        }
        String nameUpToEnd = name.children().subList(0, endIncl + 1).stream().map(Node::getSource).collect(Collectors.joining());
        List<? extends NamedType> nts = context.typeContext().getWithQualification(nameUpToEnd, false);
        if (nts != null) {
            NamedType namedType = nts.getLast();
            if (namedType instanceof TypeInfo typeInfo) {
                ParameterizedType pt = runtime.newParameterizedType(typeInfo, 0);
                if (detailedSourcesBuilder != null) {
                    detailedSourcesBuilder.put(pt, source(name, 0, endIncl));
                    List<DetailedSources.Builder.TypeInfoSource> associatedList = computeTypeInfoSources(nts, name);
                    if (!associatedList.isEmpty()) {
                        detailedSourcesBuilder.putTypeQualification(typeInfo, List.copyOf(associatedList));
                    }
                    Source pkgNameSource = sourceOfPrefix(name, associatedList.size());
                    if (pkgNameSource != null) {
                        detailedSourcesBuilder.put(typeInfo.packageName(), pkgNameSource);
                    }
                }
                return runtime.newTypeExpressionBuilder()
                        .setParameterizedType(pt)
                        .setDiamond(runtime.diamondNo())
                        .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                        .build();
            } else if (namedType instanceof TypeParameter) {
                throw new Summary.ParseException(context, "?");
            }
        }
        // since we don't have a type, we must have a variable
        // this will set the recursion going, from right to left
        return parseDottedVariable(context, comments, source, name, endIncl, detailedSourcesBuilder);
    }

    private Expression parseIdentifier(Context context, List<Comment> comments, Source source, Identifier identifier) {
        String name = identifier.getSource();
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        Variable v1 = context.variableContext().get(name, false);
        Variable v2;
        if (v1 != null) {
            v2 = v1;
        } else {
            v2 = context.typeContext().findStaticFieldImport(name);
            if (v2 != null) {
                // for further reference, so that we don't have to go through the search procedure again
                context.variableContext().add((FieldReference) v2);
            }
        }
        if (v2 != null) {
            if (detailedSourcesBuilder != null && v1 instanceof FieldReference fr) {
                detailedSourcesBuilder.put(fr.fieldInfo(), source);
            }
            return runtime.newVariableExpressionBuilder().setVariable(v2)
                    .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                    .addComments(comments).build();
        }
        if (context.enclosingType() != null) {
            FieldReference fr = findField(context, null, name, false);
            if (fr != null) {
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(fr.fieldInfo(), source);
                return runtime.newVariableExpressionBuilder().setVariable(fr)
                        .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                        .addComments(comments).build();
            }
        } // else: see for example parsing of annotation '...importhelper.a.Resources', line 6
        List<? extends NamedType> nts = context.typeContext().getWithQualification(name, false);
        if (nts == null) throw new Summary.ParseException(context, "Unknown identifier '" + name + "'");
        assert nts.size() == 1; // identifier does not contain '.', so simple name
        NamedType namedType = nts.getLast();
        TypeInfo typeInfo;
        if (namedType instanceof TypeInfo ti) {
            typeInfo = ti;
        } else if (namedType instanceof TypeParameter tp) {
            typeInfo = tp.typeBounds().size() != 1 ? runtime.objectTypeInfo() :
                    tp.typeBounds().stream().findFirst().orElseThrow().typeInfo();
        } else throw new UnsupportedOperationException();
        if (detailedSourcesBuilder != null) {
            detailedSourcesBuilder.put(typeInfo, source);
        }
        ParameterizedType parameterizedType = runtime.newParameterizedType(typeInfo, 0);
        return runtime.newTypeExpressionBuilder()
                .setParameterizedType(parameterizedType)
                .setDiamond(runtime.diamondShowAll())
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .build();
    }

    private FieldReference findField(Context context, Expression scope, String name, boolean complain) {
        ParameterizedType pt;
        if (scope != null) {
            pt = scope.parameterizedType();
        } else {
            pt = context.enclosingType().asParameterizedType();
        }
        TypeInfo typeInfo = pt.bestTypeInfo();
        if (typeInfo == null) {
            if (complain) {
                throw new Summary.ParseException(context, "Cannot find field named '" + name + "'; null TypeInfo");
            }
            return null;
        }
        FieldInfo fieldInfo = findRecursively(typeInfo, name);
        if (fieldInfo == null) {
            if (complain) {
                throw new Summary.ParseException(context, "Cannot find field named '" + name + "' in hierarchy of " + pt);
            }
            return null;
        }
        // fieldInfo.type() is the formal type; we need the concrete type here (e.g. TestMethodCall1,2)
        // but the field can belong to a supertype of pt.typeInfo(), in which case .initialTypeParameterMap() is not
        // sufficient (e.g. TestMethodCall4,3)
        Map<NamedType, ParameterizedType> map;
        if (typeInfo == fieldInfo.owner()) {
            map = pt.initialTypeParameterMap();
        } else {
            // either fieldInfo.owner() is a super type of typeInfo,
            // boolean ownerIsSuperType = typeInfo.superTypesExcludingJavaLangObject().contains(fieldInfo.owner());
            // or, fieldInfo.owner() is an enclosing type, typeInfo an inner type
            //boolean ownerIsEnclosingType = typeInfo.primaryType().equals(fieldInfo.owner().primaryType());
            //assert ownerIsSuperType || ownerIsEnclosingType;

            ParameterizedType superType = fieldInfo.owner().asParameterizedType();
            // we need to obtain a translation map to get the concrete types or type bounds
            if (superType.typeInfo() == context.enclosingType()) {
                // see e.g. TestField2,3
                map = pt.initialTypeParameterMap();
            } else {
                map = context.genericsHelper().mapInTermsOfParametersOfSuperType(context.enclosingType(), superType);
            }
        }
        ParameterizedType concreteType = map == null || map.isEmpty() ? fieldInfo.type()
                : fieldInfo.type().applyTranslation(runtime, map);
        return runtime.newFieldReference(fieldInfo, scope, concreteType);
    }

    private FieldInfo findRecursively(TypeInfo typeInfo, String name) {
        FieldInfo fieldInfo = typeInfo.getFieldByName(name, false);
        if (fieldInfo != null) return fieldInfo;
        if (typeInfo.parentClass() != null && !typeInfo.parentClass().isJavaLangObject()) {
            FieldInfo fi = findRecursively(typeInfo.parentClass().typeInfo(), name);
            if (fi != null) return fi;
        }
        if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
            FieldInfo fi = findRecursively(typeInfo.compilationUnitOrEnclosingType().getRight(), name);
            if (fi != null) return fi;
        }
        if (typeInfo.enclosingMethod() != null) {
            return findRecursively(typeInfo.enclosingMethod().typeInfo(), name);
        }
        for (ParameterizedType interfaceImplemented : typeInfo.interfacesImplemented()) {
            FieldInfo fi = findRecursively(interfaceImplemented.typeInfo(), name);
            if (fi != null) return fi;
        }
        return null;
    }

    private Expression parseDottedVariable(Context context, List<Comment> comments, Source source, Name name, int end,
                                           DetailedSources.Builder detailedSourcesBuilder) {
        if (end == 0) {
            return parseIdentifier(context, comments, source(name, 0, 0), (Identifier) name.getFirst());
        }
        String varName = name.get(end).getSource();
        Expression scope;
        if (end == 2) {
            scope = parseIdentifier(context, comments, source(name, 0, 0), (Identifier) name.getFirst());
        } else {
            scope = parseDottedName(context, comments, source, name, end - 2);
        }
        FieldReference fr = findField(context, scope, varName, true);
        assert fr != null;
        if (detailedSourcesBuilder != null) {
            detailedSourcesBuilder.put(fr.fieldInfo(), source(name.get(end)));
        }
        return runtime.newVariableExpressionBuilder().setVariable(fr)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .build();
    }

    private Expression arrayInitializer(Context context, String index, ForwardType forwardType, List<Comment> comments,
                                        Source source, Node arrayInitializer) {
        List<Expression> expressions = new ArrayList<>();
        ParameterizedType commonType = null;
        ParameterizedType oneFewer = forwardType.type() == null ? null : forwardType.type().copyWithOneFewerArrays();
        ForwardType forwardTypeOneArrayLess = context.newForwardType(oneFewer);
        for (int i = 1; i < arrayInitializer.size(); i += 2) {
            Node ai = arrayInitializer.get(i);
            if (ai instanceof Delimiter) break; // empty expression list
            Expression e = parse(context, index, forwardTypeOneArrayLess, ai);
            expressions.add(e);
            commonType = commonType == null ? e.parameterizedType() : runtime.commonType(commonType, e.parameterizedType());
        }
        ParameterizedType type;
        if (forwardType.type() != null) {
            type = forwardType.type().copyWithOneFewerArrays();
        } else {
            type = commonType;
        }
        assert type != null;
        return runtime.newArrayInitializerBuilder()
                .setExpressions(expressions).setCommonType(type).setSource(source).addComments(comments)
                .build();
    }

    private Expression inlineConditional(Context context, String index, ForwardType forwardType, List<Comment> comments, Source source, Node node) {
        if (!node.get(1).getType().equals(HOOK)) throw new UnsupportedOperationException();
        if (!node.get(3).getType().equals(COLON)) throw new UnsupportedOperationException();

        Expression condition = parse(context, index, context.newForwardType(runtime.booleanParameterizedType()), node.get(0));
        Expression ifTrue = parse(context, index, forwardType, node.get(2));
        Expression ifFalse = parse(context, index, forwardType, node.get(4));
        return runtime.newInlineConditionalBuilder()
                .addComments(comments).setSource(source).setCondition(condition).setIfTrue(ifTrue).setIfFalse(ifFalse)
                .build(runtime);
    }

    private Expression parseAssignment(Context context,
                                       String index,
                                       AssignmentExpression assignmentExpression,
                                       List<Comment> comments,
                                       Source source) {
        Expression wrappedTarget = parse(context, index, context.emptyForwardType(), assignmentExpression.get(0));
        Expression target = unwrapEnclosed(wrappedTarget);
        if (!(target instanceof VariableExpression targetVE)) {
            throw new Summary.ParseException(context, "Expected assignment target to be a variable expression");
        }
        ForwardType fwd = context.newForwardType(target.parameterizedType());
        Expression value = parse(context, index, fwd, assignmentExpression.get(2));
        MethodInfo assignmentOperator;
        MethodInfo binaryOperator;
        Node.NodeType tt = assignmentExpression.get(1).getType();
        if (ASSIGN.equals(tt)) {
            binaryOperator = null;
            assignmentOperator = null;
        } else if (Token.TokenType.PLUSASSIGN.equals(tt)) {
            // String s += c; with c a char, is legal!
            if (value.parameterizedType().isJavaLangString() || target.parameterizedType().isJavaLangString()) {
                binaryOperator = runtime.plusOperatorString();
                assignmentOperator = runtime.assignPlusOperatorString();
            } else {
                binaryOperator = runtime.plusOperatorInt();
                assignmentOperator = runtime.assignPlusOperatorInt();
            }
        } else if (MINUSASSIGN.equals(tt)) {
            binaryOperator = runtime.minusOperatorInt();
            assignmentOperator = runtime.assignMinusOperatorInt();
        } else if (STARASSIGN.equals(tt)) {
            binaryOperator = runtime.multiplyOperatorInt();
            assignmentOperator = runtime.assignMultiplyOperatorInt();
        } else if (SLASHASSIGN.equals(tt)) {
            binaryOperator = runtime.divideOperatorInt();
            assignmentOperator = runtime.assignDivideOperatorInt();
        } else if (REMASSIGN.equals(tt)) {
            binaryOperator = runtime.remainderOperatorInt();
            assignmentOperator = runtime.assignRemainderOperatorInt();
        } else if (XORASSIGN.equals(tt)) {
            binaryOperator = runtime.xorOperatorInt();
            assignmentOperator = runtime.assignXorOperatorInt();
        } else if (ANDASSIGN.equals(tt)) {
            binaryOperator = runtime.andOperatorInt();
            assignmentOperator = runtime.assignAndOperatorInt();
        } else if (ORASSIGN.equals(tt)) {
            binaryOperator = runtime.orOperatorInt();
            assignmentOperator = runtime.assignOrOperatorInt();
        } else if (RSIGNEDSHIFTASSIGN.equals(tt)) {
            binaryOperator = runtime.signedRightShiftOperatorInt();
            assignmentOperator = runtime.assignSignedRightShiftOperatorInt();
        } else if (RUNSIGNEDSHIFTASSIGN.equals(tt)) {
            binaryOperator = runtime.unsignedRightShiftOperatorInt();
            assignmentOperator = runtime.assignUnsignedRightShiftOperatorInt();
        } else if (LSHIFTASSIGN.equals(tt)) {
            binaryOperator = runtime.leftShiftOperatorInt();
            assignmentOperator = runtime.assignLeftShiftOperatorInt();
        } else {
            throw new UnsupportedOperationException("NYI");
        }
        return runtime.newAssignmentBuilder().setValue(value).setTarget(targetVE)
                .setBinaryOperator(binaryOperator)
                .setAssignmentOperator(assignmentOperator)
                .setAssignmentOperatorIsPlus(false)// not relevant for +=, =
                .setPrefixPrimitiveOperator(null)
                .addComments(comments).setSource(source).build();
    }

    private Expression unwrapEnclosed(Expression wrappedTarget) {
        if (wrappedTarget instanceof EnclosedExpression ee) {
            return unwrapEnclosed(ee.inner());
        }
        return wrappedTarget;
    }

    private Expression plusPlusMinMin(Context context, String index, List<Comment> comments, Source source,
                                      int expressionIndex, int opIndex, boolean pre, Node node) {
        ForwardType fwd = context.newForwardType(runtime.intParameterizedType());
        Expression target = parse(context, index, fwd, node.get(expressionIndex));
        if (!(target instanceof VariableExpression targetVE)) {
            throw new Summary.ParseException(context, "Expected assignment target to be a variable expression");
        }
        MethodInfo binaryOperator;
        MethodInfo assignmentOperator;
        boolean isPlus;
        if (node.get(opIndex) instanceof Operator operator) {
            if (INCR.equals(operator.getType())) {
                isPlus = true;
                assignmentOperator = runtime.assignPlusOperatorInt();
                binaryOperator = runtime.plusOperatorInt();
            } else if (DECR.equals(operator.getType())) {
                isPlus = false;
                assignmentOperator = runtime.assignMinusOperatorInt();
                binaryOperator = runtime.minusOperatorInt();
            } else throw new UnsupportedOperationException();
            return runtime.newAssignmentBuilder().setValue(runtime.intOne(source)).setTarget(targetVE)
                    .setAssignmentOperator(assignmentOperator).setAssignmentOperatorIsPlus(isPlus)
                    .setBinaryOperator(binaryOperator)
                    .setPrefixPrimitiveOperator(pre)
                    .addComments(comments).setSource(source).build();
        } else throw new UnsupportedOperationException();
    }

    private Expression parseConditionalExpression(Context context, List<Comment> comments, Source source,
                                                  String index, org.parsers.java.ast.Expression e) {

        List<Expression> components = new ArrayList<>();
        ForwardType fwd = context.newForwardType(runtime.booleanParameterizedType());
        for (int i = 0; i < e.size(); i += 2) {
            Expression parsed = parse(context, index, fwd, e.get(i));
            components.add(parsed);
        }
        if (e instanceof ConditionalAndExpression) {
            return runtime.newAndBuilder()
                    .addComments(comments).setSource(source)
                    .addExpressions(components)
                    .build();
        }
        if (e instanceof ConditionalOrExpression) {
            return runtime.newOrBuilder()
                    .addComments(comments).setSource(source)
                    .addExpressions(components).build();
        }
        throw new UnsupportedOperationException();
    }

    private Expression parseParentheses(Context context, String index,
                                        List<Comment> comments, Source source,
                                        ForwardType forwardType, Parentheses p) {
        Expression e = parse(context, index, forwardType, p.getNestedExpression());
        return runtime.newEnclosedExpressionBuilder()
                .addComments(comments).setSource(source).setExpression(e)
                .build();
    }

    private Expression parseUnaryExpression(Context context, String index, org.parsers.java.ast.Expression ue) {
        MethodInfo methodInfo;
        ForwardType forwardType;
        if (ue.get(0) instanceof Operator operator) {
            methodInfo = switch (operator.getType()) {
                case PLUS -> null; // ignore!
                case MINUS -> runtime.unaryMinusOperatorInt();
                case BANG -> runtime.logicalNotOperatorBool();
                case TILDE -> runtime.bitWiseNotOperatorInt();
                default -> throw new UnsupportedOperationException();
            };
            forwardType = context.newForwardType(switch (operator.getType()) {
                case PLUS, TILDE, MINUS -> runtime.intParameterizedType();
                case BANG -> runtime.booleanParameterizedType();
                default -> throw new UnsupportedOperationException();
            });
        } else throw new UnsupportedOperationException();
        Expression expression = parse(context, index, forwardType, ue.get(1));
        if (methodInfo == null) {
            return expression;
        }
        if (operator.getType() == MINUS) {
            if (expression instanceof IntConstant ic) {
                return runtime.newInt(ic.comments(), source(ue), -ic.constant());
            } else if (expression instanceof DoubleConstant dc) {
                return runtime.newDouble(dc.comments(), source(ue), -dc.constant());
            } else if (expression instanceof FloatConstant fc) {
                return runtime.newFloat(fc.comments(), source(ue), -fc.constant());
            } else if (expression instanceof LongConstant lc) {
                return runtime.newLong(lc.comments(), source(ue), -lc.constant());
            } else if (expression instanceof ByteConstant bc) {
                return runtime.newByte(bc.comments(), source(ue), (byte) (-bc.constant()));
            } else if (expression instanceof ShortConstant sc) {
                return runtime.newShort(sc.comments(), source(ue), (short) (-sc.constant()));
            }
        }
        return runtime.newUnaryOperator(comments(ue), source(ue), methodInfo, expression, runtime.precedenceUnary());
    }

    private Expression parseDotName(Context context, List<Comment> comments, Source source, String index, Node dotName) {
        Node nameNode = dotName.get(2);
        String name = nameNode.getSource();
        Expression scope;
        FieldReference fr;
        Node n0 = dotName.getFirst();
        if (n0 instanceof LiteralExpression le) {
            if ("this".equals(le.getSource())) {
                scope = runtime.newVariableExpressionBuilder()
                        .setSource(source(le))
                        .setVariable(runtime.newThis(context.enclosingType().asParameterizedType()))
                        .build();
                fr = findField(context, scope, name, true);
            } else if ("super".equals(le.getSource())) {
                TypeInfo parentClass = context.enclosingType().parentClass().typeInfo();
                scope = runtime.newVariableExpressionBuilder()
                        .setSource(source(le))
                        .setVariable(runtime.newThis(parentClass.asParameterizedType()))
                        .build();
                fr = findField(context, scope, name, true);
            } else {
                throw new UnsupportedOperationException("NYI");
            }
        } else {
            scope = parse(context, index, context.emptyForwardType(), n0);
            if (LENGTH.equals(name)) {
                if (scope.parameterizedType().arrays() > 0) {
                    return runtime.newArrayLengthBuilder().setExpression(scope)
                            .addComments(comments).setSource(source(nameNode))
                            .build();
                } // else: can be a normal field name!
            }
            fr = findField(context, scope, name, true);
        }
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        if (detailedSourcesBuilder != null) {
            assert fr != null;
            detailedSourcesBuilder.put(fr.fieldInfo(), source(nameNode));
        }
        return runtime.newVariableExpressionBuilder()
                .addComments(comments)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .setVariable(fr)
                .build();
    }

    private Expression parseCast(Context context, String index, List<Comment> comments, Source source, CastExpression castExpression) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        // 0 = '(', 2 = ')'
        ParameterizedType pt = parsers.parseType().parse(context, castExpression.get(1), detailedSourcesBuilder);
        // can technically be anything
        ForwardType fwd = context.newForwardType(pt);
        Expression expression = parse(context, index, fwd, castExpression.get(3));
        return runtime.newCastBuilder().setExpression(expression).setParameterizedType(pt)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments).build();
    }

    private Expression parseAdditive(Context context, String index, AdditiveExpression ae) {
        Node.NodeType token1 = ae.get(1).getType();
        ForwardType fwd1;
        if (token1.equals(MINUS)) {
            fwd1 = context.newForwardType(runtime.intParameterizedType());
        } else {
            // for plus, we could have either string, or int; with string, all bets are off
            fwd1 = context.emptyForwardType();
        }
        Expression accumulated = parse(context, index, fwd1, ae.get(0));
        int i = 2;
        while (i < ae.size()) {
            Node.NodeType token = ae.get(i - 1).getType();
            ForwardType fwt;
            if (token.equals(MINUS)) {
                fwt = context.newForwardType(runtime.intParameterizedType());
            } else {
                // for plus, we could have either string, or int; with string, all bets are off
                fwt = context.emptyForwardType();
            }

            Expression rhs = parse(context, index, fwt, ae.get(i));
            MethodInfo operator;
            if (token.equals(PLUS)) {
                if (accumulated.parameterizedType().isJavaLangString() || rhs.parameterizedType().isJavaLangString()) {
                    operator = runtime.plusOperatorString();
                } else {
                    operator = runtime.plusOperatorInt();
                }
            } else if (token.equals(MINUS)) {
                operator = runtime.minusOperatorInt();
            } else {
                throw new UnsupportedOperationException();
            }
            ParameterizedType pt = runtime.widestTypeUnbox(accumulated.parameterizedType(), rhs.parameterizedType());
            accumulated = runtime.newBinaryOperatorBuilder()
                    .setOperator(operator)
                    .setLhs(accumulated).setRhs(rhs)
                    .setParameterizedType(pt)
                    .setPrecedence(runtime.precedenceAdditive())
                    .setSource(source(index, ae))
                    .addComments(comments(ae))
                    .build();
            i += 2;
        }
        return accumulated;
    }

    private Expression parseMultiplicative(Context context, String index, Node me) {
        ForwardType fwd = context.newForwardType(runtime.intParameterizedType());
        Expression accumulated = parse(context, index, fwd, me.getFirst());
        int i = 2;
        while (i < me.size()) {
            Expression rhs = parse(context, index, fwd, me.get(i));
            Node.NodeType token = me.get(i - 1).getType();
            MethodInfo operator;
            Precedence precedence;
            if (token.equals(STAR)) {
                operator = runtime.multiplyOperatorInt();
                precedence = runtime.precedenceMultiplicative();
            } else if (token.equals(SLASH)) {
                operator = runtime.divideOperatorInt();
                precedence = runtime.precedenceMultiplicative();
            } else if (token.equals(REM)) {
                precedence = runtime.precedenceMultiplicative();
                operator = runtime.remainderOperatorInt();
            } else if (token.equals(BIT_AND)) {
                precedence = runtime.precedenceBitwiseAnd();
                operator = runtime.andOperatorInt();
            } else if (token.equals(BIT_OR)) {
                precedence = runtime.precedenceBitwiseOr();
                operator = runtime.orOperatorInt();
            } else if (token.equals(XOR)) {
                precedence = runtime.precedenceBitwiseXor();
                operator = runtime.xorOperatorInt();
            } else if (token.equals(LSHIFT)) {
                precedence = runtime.precedenceShift();
                operator = runtime.leftShiftOperatorInt();
            } else if (token.equals(RSIGNEDSHIFT)) {
                precedence = runtime.precedenceShift();
                operator = runtime.signedRightShiftOperatorInt();
            } else if (token.equals(RUNSIGNEDSHIFT)) {
                precedence = runtime.precedenceShift();
                operator = runtime.unsignedRightShiftOperatorInt();
            } else {
                throw new UnsupportedOperationException();
            }
            ParameterizedType pt = runtime.widestTypeUnbox(accumulated.parameterizedType(), rhs.parameterizedType());
            accumulated = runtime.newBinaryOperatorBuilder()
                    .setOperator(operator)
                    .setLhs(accumulated).setRhs(rhs)
                    .setParameterizedType(pt)
                    .setPrecedence(precedence)
                    .setSource(source(index, me))
                    .addComments(comments(me))
                    .build();
            i += 2;
        }
        return accumulated;
    }

    private Expression parseRelational(Context context, String index, RelationalExpression re) {
        ForwardType fwd = context.newForwardType(runtime.intParameterizedType());
        Expression lhs = parse(context, index, fwd, re.get(0));
        Expression rhs = parse(context, index, fwd, re.get(2));
        Node.NodeType token = re.get(1).getType();
        MethodInfo operator;
        if (token.equals(LE)) {
            operator = runtime.lessEqualsOperatorInt();
        } else if (token.equals(LT)) {
            operator = runtime.lessOperatorInt();
        } else if (token.equals(GE)) {
            operator = runtime.greaterEqualsOperatorInt();
        } else if (token.equals(GT)) {
            operator = runtime.greaterOperatorInt();
        } else {
            throw new UnsupportedOperationException();
        }
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(runtime.booleanParameterizedType())
                .setPrecedence(runtime.precedenceRelational())
                .setSource(source(index, re))
                .addComments(comments(re))
                .build();
    }

    private Expression parseEquality(Context context, String index, EqualityExpression eq) {
        ForwardType forwardType = context.emptyForwardType();
        Expression lhs = parse(context, index, forwardType, eq.get(0));
        Expression rhs = parse(context, index, forwardType, eq.get(2));
        Node.NodeType token = eq.get(1).getType();
        MethodInfo operator;
        boolean isNumeric = lhs.isNumeric() && rhs.isNumeric();
        if (token.equals(EQ)) {
            operator = isNumeric ? runtime.equalsOperatorInt() : runtime.equalsOperatorObject();
        } else if (token.equals(NE)) {
            operator = isNumeric ? runtime.notEqualsOperatorInt() : runtime.notEqualsOperatorObject();
        } else throw new UnsupportedOperationException();
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(runtime.booleanParameterizedType())
                .setPrecedence(runtime.precedenceEquality())
                .setSource(source(index, eq))
                .addComments(comments(eq))
                .build();
    }

    private static final long TWO32 = 0xFF_FF_FF_FFL;

    private Expression parseLiteral(Context context, List<Comment> comments, Source source, LiteralExpression le) {
        Node child = le.children().get(0);
        if (child instanceof IntegerLiteral il) {
            long l = parseLong(il.getSource());
            if (l <= Integer.MAX_VALUE) {
                return runtime.newInt(comments, source, (int) l);
            }
            if (l <= TWO32) {
                long complement = ((~l) & TWO32);
                int negative = (int) (-(complement + 1));
                return runtime.newInt(comments, source, negative);
            }
            throw new UnsupportedOperationException();
        }
        if (child instanceof LongLiteral ll) {
            long l = parseLong(ll.getSource());
            return runtime.newLong(comments, source, l);
        }
        if (child instanceof FloatingPointLiteral fp) {
            String src = fp.getSource().toLowerCase().replace("_", "");
            if (src.endsWith("f") || src.endsWith("F")) {
                return runtime.newFloat(comments, source, Float.parseFloat(src.substring(0, src.length() - 1)));
            }
            if (src.endsWith("d") || src.endsWith("D")) {
                return runtime.newDouble(comments, source, Double.parseDouble(src.substring(0, src.length() - 1)));
            }
            return runtime.newDouble(comments, source, Double.parseDouble(src));
        }
        if (child instanceof BooleanLiteral bl) {
            return runtime.newBoolean(comments, source, "true".equals(bl.getSource()));
        }
        if (child instanceof CharacterLiteral cl) {
            char c = cl.charAt(1);
            if (c == '\\') {
                c = EscapeSequence.escapeSequence(cl.getSource().substring(2));
            }
            return runtime.newChar(comments, source, c);
        }
        if (child instanceof StringLiteral sl) {
            if (Token.TokenType.TEXT_BLOCK_LITERAL.equals(sl.getType())) {
                return new TextBlockParser(runtime).parseTextBlock(comments, source, sl);
            }
            String content = sl.getString();
            return runtime.newStringConstant(comments, source, content);
        }
        if (child instanceof NullLiteral) {
            return runtime.newNullConstant(comments, source);
        }
        if (child instanceof ThisLiteral) {
            This thisVar = runtime.newThis(context.enclosingType().asParameterizedType());
            return runtime.newVariableExpressionBuilder()
                    .setSource(source).addComments(comments)
                    .setVariable(thisVar).build();
        }
        if (!le.children().isEmpty() && le.get(0) instanceof KeyWord kw && SUPER.equals(kw.getType())) {
            This thisVar = runtime.newThis(context.enclosingType().parentClass().typeInfo().asParameterizedType(),
                    null, true);
            return runtime.newVariableExpressionBuilder()
                    .setSource(source).addComments(comments)
                    .setVariable(thisVar).build();
        }
        throw new UnsupportedOperationException("literal expression " + le.getClass());
    }

    private static long parseLong(String s) {
        String src = s.toLowerCase().replace("_", "").replace("l", "");
        if (src.startsWith("0x")) {
            return Long.parseLong(src.substring(2), 16);
        }
        if (src.startsWith("0b")) {
            return Long.parseLong(src.substring(2), 2);
        }
        if (src.startsWith("0") && src.length() > 1 && Character.isDigit(src.charAt(1))) {
            return Long.parseLong(src.substring(1), 8);
        }
        return Long.parseLong(src);
    }
}
