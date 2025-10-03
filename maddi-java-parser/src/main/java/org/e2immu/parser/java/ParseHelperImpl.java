package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.ParseHelper;
import org.e2immu.language.inspection.api.parser.Summary;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.util.internal.util.StringUtil.pad;

public class ParseHelperImpl extends CommonParse implements ParseHelper {
    public ParseHelperImpl(Runtime runtime) {
        this(runtime, new Parsers(runtime));
    }

    public ParseHelperImpl(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    @Override
    public List<AnnotationExpression.KV> parseAnnotationExpression(TypeInfo annotationType, Object annotation, Context context) {
        List<AnnotationExpression.KV> kvs = new ArrayList<>();
        if (annotation instanceof SingleMemberAnnotation sma) {
            MethodInfo methodInfo = annotationType.findUniqueMethod("value", 0);
            ForwardType forwardType = context.newForwardType(methodInfo.returnType());
            Expression expression = parsers.parseExpression().parse(context, "", forwardType, sma.get(3));
            kvs.add(runtime.newAnnotationExpressionKeyValuePair("value", expression));
        } else if (annotation instanceof NormalAnnotation na) {
            // delimiter @, annotation name, ( , mvp, delimiter ',', mvp, delimiter )
            if (na.get(3) instanceof MemberValuePair mvp) {
                String key = mvp.getFirst().getSource();
                MethodInfo methodInfo = annotationType.findUniqueMethod(key, 0);
                ForwardType forwardType = context.newForwardType(methodInfo.returnType());
                Expression value = parsers.parseExpression().parse(context, "", forwardType, mvp.get(2));
                kvs.add(runtime.newAnnotationExpressionKeyValuePair(key, value));
            } else if (na.get(3) instanceof MemberValuePairs pairs) {
                for (int j = 0; j < pairs.size(); j += 2) {
                    if (pairs.get(j) instanceof MemberValuePair mvp) {
                        String key = mvp.getFirst().getSource();
                        MethodInfo methodInfo = annotationType.findUniqueMethod(key, 0);
                        ForwardType forwardType = context.newForwardType(methodInfo.returnType());
                        Expression value = parsers.parseExpression().parse(context, "", forwardType, mvp.get(2));
                        kvs.add(runtime.newAnnotationExpressionKeyValuePair(key, value));
                    } else {
                        throw new Summary.ParseException(context, "Expected mvp");
                    }
                }
            } else {
                throw new Summary.ParseException(context, "Expected mvp");
            }
        } else {
            throw new UnsupportedOperationException("NYI");
        }
        return kvs;
    }

    @Override
    public Expression parseExpression(Context context, String index, ForwardType forward, Object expression) {
        if (expression instanceof InvocationArguments ia) {
            TypeInfo enumType = forward.type().typeInfo();
            // we'll have to parse a constructor, new C(invocation arguments)
            return parsers.parseConstructorCall().parseEnumConstructor(context, index, enumType, ia);
        }
        return parsers.parseExpression().parse(context, index, forward, (Node) expression);
    }

    @Override
    public void resolveMethodInto(MethodInfo.Builder builder,
                                  Context context,
                                  ForwardType forwardType,
                                  Object unparsedEci,
                                  Object expression,
                                  List<Statement> recordAssignments) {
        int n = (recordAssignments == null ? 0 : recordAssignments.size())
                + countTopLevelStatements(unparsedEci, expression);
        org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation eci;
        if (unparsedEci == null) {
            eci = null;
        } else {
            eci = parseEci(context, unparsedEci, n);
        }
        Element e;
        if (expression instanceof CompactConstructorDeclaration ccd) {
            int j = 0;
            while (!Token.TokenType.LBRACE.equals(ccd.get(j).getType())) j++;
            j++;
            if (ccd.get(j) instanceof org.parsers.java.ast.Statement s) {
                e = parseStatements(context, s, 0, n);
            } else if (ccd.get(j) instanceof Delimiter) {
                e = runtime.emptyBlock();
            } else throw new Summary.ParseException(context, "Expected either empty block, or statements");
        } else if (expression instanceof CodeBlock codeBlock) {
            e = parsers.parseBlock().parse(context, "", null, codeBlock, false,
                    eci == null ? 0 : 1);
        } else {
            e = parseStatements(context, forwardType, expression, eci != null, n);
        }
        if (e instanceof Block b) {
            Block bWithEci;
            if (eci != null) {
                bWithEci = runtime.newBlockBuilder().addStatement(eci).addStatements(b.statements()).build();
            } else if (recordAssignments != null) {
                bWithEci = runtime.newBlockBuilder()
                        .addStatements(b.statements())
                        .addStatements(addIndices(recordAssignments, b.statements().size(), n))
                        .build();
            } else {
                bWithEci = b;
            }
            builder.setMethodBody(bWithEci);
        } else if (e instanceof Statement s) {
            Block.Builder bb = runtime.newBlockBuilder();
            if (eci != null) bb.addStatement(eci);
            bb.addStatement(s);
            if (recordAssignments != null) {
                bb.addStatements(addIndices(recordAssignments, bb.statements().size(), n));
            }
            builder.setMethodBody(bb.build());
        } else if (e == null && eci != null) {
            builder.setMethodBody(runtime.newBlockBuilder().addStatement(eci).build());
        } else if (e == null && recordAssignments != null) {
            builder.setMethodBody(runtime.newBlockBuilder()
                    .addStatements(addIndices(recordAssignments, 0, n))
                    .build());
        } else {
            // in Java, we must have a block
            throw new UnsupportedOperationException();
        }
    }

    private int countTopLevelStatements(Object unparsedEci, Object expression) {
        int n = unparsedEci instanceof ExplicitConstructorInvocation ? 1 : 0;
        Node node = (Node) expression;
        if (node instanceof CompactConstructorDeclaration || node instanceof CodeBlock) {
            n += node.childrenOfType(org.parsers.java.ast.Statement.class).size();
        } else if (node instanceof org.parsers.java.ast.Statement) {
            n += 1;
            while (node.nextSibling() instanceof org.parsers.java.ast.Statement st) {
                n += 1;
                node = st;
            }
        } else if (node instanceof org.parsers.java.ast.Expression) {
            ++n;
        } else if (node != null) {
            throw new UnsupportedOperationException();
        }
        return n;
    }

    // each statement must have an index
    private List<Statement> addIndices(List<Statement> statements, int start, int n) {
        AtomicInteger count = new AtomicInteger(start);
        return statements.stream().map(s -> (Statement) ((ExpressionAsStatement) s)
                        .withSource(runtime.newParserSource(pad(count.getAndIncrement(), n),
                                0, 0, 0, 0)))
                .toList();
    }

    /*
    we have and the potential "ECI" to deal with, and the fact that sometimes, multiple
    ExpressionStatements can replace a CodeBlock (no idea why, but we need to deal with it)
    See TestExplicitConstructorInvocation.
     */
    private Element parseStatements(Context context,
                                    ForwardType forwardType,
                                    Object expression,
                                    boolean haveEci,
                                    int n) {
        int start = haveEci ? 1 : 0;
        if (expression instanceof org.parsers.java.ast.Statement est) {
            return parseStatements(context, est, start, n);
        }
        if (expression != null) {
            return parseExpression(context, pad(start, n), forwardType, expression);
        }
        return null;
    }

    private Statement parseStatements(Context context, org.parsers.java.ast.Statement first, int start, int n) {
        Statement firstStatement = parsers.parseStatement().parse(context, pad(start, n), first);

        List<org.parsers.java.ast.Statement> siblings = new ArrayList<>();
        while (first.nextSibling() instanceof org.parsers.java.ast.Statement next) {
            siblings.add(next);
            first = next;
        }
        if (siblings.isEmpty()) {
            return firstStatement;
        }
        Block.Builder b = runtime.newBlockBuilder();
        b.addStatement(firstStatement);
        for (org.parsers.java.ast.Statement es : siblings) {
            ++start;
            Statement s2 = parsers.parseStatement().parse(context, pad(start, n), es);
            b.addStatement(s2);
        }
        return b.build();
    }

    private org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation parseEci(Context context,
                                                                                         Object eciObject,
                                                                                         int n) {
        ExplicitConstructorInvocation unparsedEci = (ExplicitConstructorInvocation) eciObject;

        ConstructorDeclaration cd = (ConstructorDeclaration) unparsedEci.getParent();
        List<Comment> comments = parsers.parseStatement().comments(cd);
        Source source = parsers.parseStatement().source(pad(0, n), cd);
        boolean isSuper = Token.TokenType.SUPER.equals(unparsedEci.getFirst().getType());

        List<Object> unparsedArguments = new ArrayList<>();
        int j = 1;
        if (unparsedEci.get(1) instanceof InvocationArguments ia) {
            while (j < ia.size() && !(ia.get(j) instanceof Delimiter)) {
                unparsedArguments.add(ia.get(j));
                j += 2;
            }
        } else throw new UnsupportedOperationException();
        TypeInfo typeInfo = isSuper ? context.enclosingType().parentClass().typeInfo() : context.enclosingType();
        ParameterizedType formalType = typeInfo.asParameterizedType();
        // see TestMethodCall11,8; a super call
        ParameterizedType expectedConcreteType;
        if (isSuper) {
            expectedConcreteType = context.enclosingType().parentClass();
        } else {
            expectedConcreteType = formalType;
        }
        Expression constructorCall = context.methodResolution().resolveConstructor(context, comments, source,
                pad(0, n), formalType, expectedConcreteType, runtime.diamondNo(), null, runtime.noSource(),
                unparsedArguments, List.of(), true, false);
        if (constructorCall instanceof ConstructorCall cc) {
            assert cc.constructor() != null;
            return runtime.newExplicitConstructorInvocationBuilder()
                    .addComments(comments)
                    .setSource(source)
                    .setIsSuper(isSuper)
                    .setMethodInfo(cc.constructor())
                    .setParameterExpressions(cc.parameterExpressions())
                    .build();
        }
        throw new UnsupportedOperationException("No ECI erasure yet");
    }

    @Override
    public JavaDoc.Tag parseJavaDocReferenceInTag(Context context, Info info, JavaDoc.Tag tag) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        JavaDoc.Tag newTag = parseJavaDocReferenceInTag(context, info, tag, detailedSourcesBuilder);
        if (detailedSourcesBuilder != null) {
            return newTag.withSource(tag.source().withDetailedSources(detailedSourcesBuilder.build()));
        }
        return newTag;
    }

    private JavaDoc.Tag parseJavaDocReferenceInTag(Context context, Info info, JavaDoc.Tag tag,
                                                   DetailedSources.Builder detailedSourcesBuilder) {
        int hash = tag.content().indexOf('#');
        String packageOrType = (hash < 0 ? tag.content() : tag.content().substring(0, hash)).trim();
        String memberDescriptor = hash < 0 ? null : tag.content().substring(hash + 1).trim();
        NamedType namedType;
        List<? extends NamedType> nts;
        if (hash == 0) {
            namedType = info.typeInfo();
            nts = null;
        } else {
            nts = context.typeContext().getWithQualification(packageOrType, false);
            namedType = nts == null ? null : nts.getLast();
        }
        if (namedType instanceof TypeInfo typeInfo) {
            if (detailedSourcesBuilder != null && nts != null) {
                addDetailedSources(tag, detailedSourcesBuilder, typeInfo, packageOrType);
            }

            if (memberDescriptor == null) {
                return tag.withResolvedReference(typeInfo);
            }
            int open = memberDescriptor.indexOf('(');
            String member;
            List<String> parameterTypes;
            if (open < 0) {
                member = memberDescriptor.trim();
                parameterTypes = null;
                Info resolved = typeInfo.getFieldByName(member, false);
                if (resolved == null) {
                    resolved = typeInfo.methodStream().filter(m -> m.name().equalsIgnoreCase(member))
                            .findFirst().orElse(null);
                }
                if (resolved != null) {
                    if (detailedSourcesBuilder != null) {
                        // offset adds 1 extra for the '#' character itself
                        detailedSourcesBuilder.put(resolved, makeSource(tag, member, hash + 1));
                        detailedSourcesBuilder.put(resolved.simpleName(), makeSource(tag, member, hash + 1));
                    }
                    return tag.withResolvedReference(resolved);
                }
            } else {
                member = memberDescriptor.substring(0, open);
                int close = memberDescriptor.indexOf(')', open + 1);
                if (close < 0) return tag;// cannot continue, not legal
                String parametersString = memberDescriptor.substring(open + 1, close);
                if (parametersString.trim().isEmpty()) {
                    parameterTypes = List.of();
                } else {
                    String[] paramStrings = parametersString.split(",\\s*");
                    parameterTypes = new ArrayList<>(Arrays.asList(paramStrings));
                }
            }
            MethodInfo methodInfo;
            if (member.equals(typeInfo.simpleName())) {
                methodInfo = typeInfo.constructors().stream()
                        .filter(c -> parameterTypes == null || parameterTypesMatch(c.parameters(), parameterTypes))
                        .findFirst().orElse(null);
            } else {
                methodInfo = typeInfo.methodStream()
                        .filter(m -> member.equals(m.name()))
                        .filter(m -> parameterTypes == null || parameterTypesMatch(m.parameters(), parameterTypes))
                        .findFirst().orElse(null);
            }
            if (methodInfo != null && detailedSourcesBuilder != null) {
                // offset adds 1 extra for the '#' character itself
                detailedSourcesBuilder.put(methodInfo.name(), makeSource(tag, member, hash + 1));
                detailedSourcesBuilder.put(methodInfo, makeSource(tag, tag.content().substring(hash + 1),
                        hash + 1));
            }
            return tag.withResolvedReference(methodInfo);
        }
        return tag;
    }

    private void addDetailedSources(JavaDoc.Tag tag,
                                    DetailedSources.Builder detailedSourcesBuilder,
                                    TypeInfo typeInfo,
                                    String packageOrType) {
        detailedSourcesBuilder.put(typeInfo, makeSource(tag, packageOrType, 0));
        Source pkgNameSource = null;
        List<DetailedSources.Builder.TypeInfoSource> associatedList = new ArrayList<>();

        String[] split = packageOrType.split("\\.");
        if (split.length > 1) {
            TypeInfo ti = typeInfo.compilationUnitOrEnclosingType().isRight()
                    ? typeInfo.compilationUnitOrEnclosingType().getRight() : null;
            int i = split.length - 2;
            while (i >= 0) {
                if (ti == null) {
                    // we're at the primary type now. If i>0, we have a package

                    pkgNameSource = makeSource(tag, split, i);

                    break;
                }
                // qualification
                Source src = makeSource(tag, split, i);
                DetailedSources.Builder.TypeInfoSource tis = new DetailedSources.Builder.TypeInfoSource(ti, src);
                associatedList.add(tis);
                ti = ti.compilationUnitOrEnclosingType().isRight() ? ti.compilationUnitOrEnclosingType().getRight()
                        : null;
                --i;
            }
        }

        if (!associatedList.isEmpty()) {
            detailedSourcesBuilder.putTypeQualification(typeInfo, List.copyOf(associatedList));
        }
        if (pkgNameSource != null) {
            detailedSourcesBuilder.put(typeInfo.packageName(), pkgNameSource);
        }
    }

    private Source makeSource(JavaDoc.Tag tag, String packageOrType, int offset) {
        Source source = tag.sourceOfReference();
        int beginContent = offset + source.beginPos();
        int endPos = beginContent + packageOrType.length() - 1;
        return runtime.newParserSource(source.index(), source.beginLine(), beginContent, source.endLine(), endPos);
    }

    private Source makeSource(JavaDoc.Tag tag, String[] split, int i) {
        Source source = tag.sourceOfReference();
        int endPos = source.beginPos() - 1;
        for (int j = 0; j <= i; ++j) {
            endPos += split[j].length() + 1;
        }
        --endPos;
        return runtime.newParserSource(source.index(), source.beginLine(), source.beginPos(), source.endLine(), endPos);
    }

    private boolean parameterTypesMatch(List<ParameterInfo> parameters, List<String> parameterTypes) {
        if (parameters.size() != parameterTypes.size()) return false;
        for (ParameterInfo pi : parameters) {
            String typeString = parameterTypes.get(pi.index());
            if (!parameterTypeMatch(pi.parameterizedType(), typeString)) return false;
        }
        return true;
    }

    private boolean parameterTypeMatch(ParameterizedType parameterizedType, String typeString) {
        if (parameterizedType.typeParameter() != null) {
            String print1 = parameterizedType.print(runtime.qualificationFullyQualifiedNames(),
                    false, runtime.diamondNo()).toString();
            if (print1.equals(typeString)) return true;
            // type parameter, maybe written as T... instead of T[]
            return parameterizedType.arrays() > 0 && parameterizedType.print(runtime.qualificationFullyQualifiedNames(),
                    true, runtime.diamondNo()).toString().equals(typeString);
        }
        String typeInfoFqn = parameterizedType.typeInfo().fullyQualifiedName();
        if (typeString.equals(typeInfoFqn)) return true;
        if (typeString.equals(parameterizedType.typeInfo().simpleName())) return true;
        return typeString.equals(parameterizedType.typeInfo().fromPrimaryTypeDownwards());
    }
}
