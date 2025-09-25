package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.util.internal.util.StringUtil;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.parsers.java.ast.AssertStatement;
import org.parsers.java.ast.BreakStatement;
import org.parsers.java.ast.ContinueStatement;
import org.parsers.java.ast.DoStatement;
import org.parsers.java.ast.EmptyStatement;
import org.parsers.java.ast.ReturnStatement;
import org.parsers.java.ast.Statement;
import org.parsers.java.ast.SynchronizedStatement;
import org.parsers.java.ast.ThrowStatement;
import org.parsers.java.ast.TryStatement;
import org.parsers.java.ast.WhileStatement;
import org.parsers.java.ast.YieldStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ParseStatement extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseStatement.class);

    private static final String FIRST_BLOCK = ".0";
    private static final String FIRST_STATEMENT = ".0";
    private static final String SECOND_BLOCK = ".1";

    public ParseStatement(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public org.e2immu.language.cst.api.statement.Statement parse(Context context, String index, Statement statement) {
        try {
            return internalParse(context, index, statement);
        } catch (Throwable t) {
            LOGGER.error("Caught exception parsing statement at line {}", statement.getBeginLine());
            throw t;
        }
    }

    private org.e2immu.language.cst.api.statement.Statement internalParse(Context context, String index, Statement statementIn) {
        String label;
        Statement statement;
        if (statementIn instanceof LabeledStatement ls) {
            label = ls.getFirst().getFirst().getSource();
            statement = (Statement) ls.get(1);
        } else {
            label = null;
            statement = statementIn;
        }
        List<Comment> comments = comments(statement);
        Source source = source(index, statement);
        List<AnnotationExpression> annotations = new ArrayList<>();
        List<LocalVariableCreation.Modifier> lvcModifiers = new ArrayList<>();
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();

        // annotations
        int i = 0;
        while (true) {
            Node si = statement.get(i);
            if (si instanceof Modifiers modifiers) {
                for (Node node : modifiers.children()) {
                    if (node instanceof Annotation a) {
                        AnnotationExpression ae = parsers.parseAnnotationExpression().parseDirectly(context, a);
                        annotations.add(ae);
                        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(ae, source(a));
                    }
                }
            } else if (si instanceof KeyWord) {
                LocalVariableCreation.Modifier m;
                if (Token.TokenType.FINAL.equals(si.getType())) {
                    m = runtime.localVariableModifierFinal();
                } else if (Token.TokenType.VAR.equals(si.getType())) {
                    m = runtime.localVariableModifierVar();
                } else {
                    break;
                }
                lvcModifiers.add(m);
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(si));
            } else if (si instanceof Annotation a) {
                AnnotationExpression ae = parsers.parseAnnotationExpression().parseDirectly(context, a);
                annotations.add(ae);
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(ae, source(a));
            } else break;
            i++;
        }

        if (statement instanceof ExpressionStatement es) {
            StatementExpression se = (StatementExpression) es.children().get(i);
            Expression e = parsers.parseExpression().parseIgnoreComments(context, index, context.emptyForwardType(),
                    se.getFirst());
            return runtime.newExpressionAsStatementBuilder().setExpression(e).setSource(source).setLabel(label)
                    .addComments(comments).addAnnotations(annotations).build();
        }

        if (statement instanceof ReturnStatement rs) {
            ForwardType forwardType = context.newForwardType(context.enclosingMethod().returnType());
            Expression e;
            ++i;
            if (rs.get(i) instanceof Delimiter) {
                e = runtime.newEmptyExpression();
            } else {
                e = parsers.parseExpression().parse(context, index, forwardType, rs.get(i));
            }
            assert e != null;
            return runtime.newReturnBuilder()
                    .setExpression(e).setSource(source).addComments(comments).setLabel(label).addAnnotations(annotations)
                    .build();
        }

        if (statement instanceof VarDeclaration varDeclaration) {
            return localVariableCreationWithVar(context, index, varDeclaration, i, lvcModifiers, source,
                    detailedSourcesBuilder, comments, label, annotations);
        }

        // type declarator delimiter declarator
        if (statement instanceof LocalVariableDeclaration nvd) {
            if (nvd.get(i) instanceof Token t && Token.TokenType.VAR.equals(t.getType())) {
                LocalVariableCreation.Modifier modifierVar = runtime.localVariableModifierVar();
                lvcModifiers.add(modifierVar);
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(modifierVar, source(t));
                return localVariableCreationWithVar(context, index, nvd, i + 1, lvcModifiers, source,
                        detailedSourcesBuilder, comments, label, annotations);
            }
            return localVariableCreation(context, index, nvd, i, lvcModifiers, source, detailedSourcesBuilder,
                    comments, label, annotations);
        }

        if (statement instanceof EnhancedForStatement enhancedFor) {
            // kw, del, noVarDecl (objectType, vardcl), operator, name (Name), del, code block
            LocalVariableCreation loopVariableCreation;
            TypeInfo iterable = runtime.getFullyQualified(Iterable.class, false);
            ForwardType forwardType = iterable == null ? context.emptyForwardType() :
                    context.newForwardType(iterable.asSimpleParameterizedType());
            Expression expression = parsers.parseExpression().parse(context, index, forwardType, enhancedFor.get(i + 4));
            Context newContext = context.newVariableContext("forEach");
            Node varDeclaration = enhancedFor.get(i + 2);
            if (varDeclaration instanceof LocalVariableDeclaration vd) {
                if (vd.hasChildOfType(Token.TokenType.VAR)) {
                    ParameterizedType elementType = computeForwardType(context, iterable, expression.parameterizedType());
                    loopVariableCreation = forEachElementWithVar(context, vd, elementType);
                } else {
                    loopVariableCreation = (LocalVariableCreation) parse(newContext, index, vd);
                }
            } else throw new UnsupportedOperationException();
            Node n6 = enhancedFor.get(i + 6);
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, n6);
            return runtime.newForEachBuilder()
                    .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                    .setInitializer(loopVariableCreation).setBlock(block).setExpression(expression).build();
        }

        if (statement instanceof IfStatement ifStatement) {
            ForwardType forwardType = context.newForwardType(runtime.booleanParameterizedType());
            Expression expression = parsers.parseExpression().parse(context, index, forwardType, ifStatement.get(i + 2));
            Node n4 = ifStatement.get(i + 4);
            Context newContext = context.newVariableContext("ifBlock");
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, n4);
            Block elseBlock;
            if (ifStatement.size() > i + 5 && ifStatement.get(i + 5) instanceof KeyWord) {
                assert Token.TokenType.ELSE.equals(ifStatement.get(i + 5).getType());
                Node n6 = ifStatement.get(i + 6);
                Context newContext2 = context.newVariableContext("elseBlock");
                elseBlock = parseBlockOrStatement(newContext2, index + SECOND_BLOCK, n6);
            } else {
                elseBlock = runtime.emptyBlock();
            }
            return runtime.newIfElseBuilder()
                    .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                    .setExpression(expression).setIfBlock(block).setElseBlock(elseBlock)
                    .build();
        }

        if (statement instanceof TryStatement tryStatement) {
            org.e2immu.language.cst.api.statement.TryStatement.Builder builder = runtime.newTryBuilder();
            ++i;
            Context newContext = context.newVariableContext("tryBlock");
            if (tryStatement.get(i) instanceof ResourcesInTryBlock r) {
                int j = 1;
                int rCount = 0;
                int n = (r.size() - 1) / 2;
                while (j < r.size() && !(r.get(j) instanceof Delimiter)) {
                    // resources
                    String newIndex = index + "+" + StringUtil.pad(rCount, n);
                    Node resourceNode = r.get(j);
                    org.e2immu.language.cst.api.statement.Statement resource;
                    if (resourceNode instanceof Statement) {
                        resource = parse(newContext, newIndex, (Statement) resourceNode);
                    } else if (resourceNode instanceof Name) {
                        Expression e = parsers.parseExpression().parse(newContext, newIndex, context.emptyForwardType(),
                                resourceNode);
                        if (e instanceof VariableExpression ve) {
                            resource = runtime.newExpressionAsStatementBuilder()
                                    .setSource(source(newIndex, resourceNode))
                                    .setExpression(ve)
                                    .build();
                        } else throw new UnsupportedOperationException();
                    } else throw new UnsupportedOperationException("NYI");
                    builder.addResource(resource);
                    j += 2;
                    rCount++;
                }
                i++;
            }
            int n = countCatchBlocks(tryStatement) + 2;
            String firstIndex = index + "." + StringUtil.pad(0, n);
            Block block = parseBlockOrStatement(newContext, firstIndex, tryStatement.get(i));
            i++;
            builder.setBlock(block).addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations);
            int blockCount = 1;
            while (i < tryStatement.size() && tryStatement.get(i) instanceof CatchBlock catchBlock) {
                Context catchContext = context.newVariableContext("catchBlock" + blockCount);
                DetailedSources.Builder detailedSourcesBuilderCb = context.newDetailedSourcesBuilder();
                org.e2immu.language.cst.api.statement.TryStatement.CatchClause.Builder ccBuilder
                        = runtime.newCatchClauseBuilder();
                int j = 2;
                if (catchBlock.get(j) instanceof KeyWord kw) {
                    if (Token.TokenType.FINAL.equals(kw.getType())) {
                        ccBuilder.setFinal(true);
                    } else throw new UnsupportedOperationException();
                    j++;
                } else {
                    ccBuilder.setFinal(false);
                }
                List<ParameterizedType> exceptionTypes = new ArrayList<>();
                // Type Operator Type ...
                while (true) {
                    Node cbj = catchBlock.get(j);
                    if (cbj instanceof Type type) {
                        for (Node child : type.children()) {
                            if (child instanceof Annotation a) {
                                AnnotationExpression ae = parsers.parseAnnotationExpression().parseDirectly(context, a);
                                ccBuilder.addAnnotation(ae);
                                if (detailedSourcesBuilderCb != null) detailedSourcesBuilderCb.put(ae, source(a));
                            }
                        }
                        ParameterizedType pt = parsers.parseType().parse(context, type, detailedSourcesBuilderCb);
                        exceptionTypes.add(pt);
                        ccBuilder.addType(pt);
                    } else if (cbj instanceof Identifier
                               || cbj instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
                        break;
                    } else if (!(cbj instanceof Operator operator
                                 && Token.TokenType.BIT_OR.equals(operator.getType()))) {
                        throw new UnsupportedOperationException();
                    }
                    j++;
                }
                Node id = catchBlock.get(j);
                if (id instanceof Identifier || id instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
                    ParameterizedType commonType;
                    if (exceptionTypes.isEmpty()) throw new UnsupportedOperationException();
                    else if (exceptionTypes.size() == 1) {
                        commonType = exceptionTypes.getFirst();
                    } else {
                        commonType = exceptionTypes.stream().skip(1)
                                .reduce(exceptionTypes.getFirst(), runtime::commonType);
                    }
                    LocalVariable cv;
                    if (id instanceof Identifier identifier) {
                        String variableName = identifier.getSource();
                        cv = runtime.newLocalVariable(variableName, commonType);
                    } else {
                        cv = runtime.newLocalVariable(commonType);
                    }
                    if (detailedSourcesBuilderCb != null) detailedSourcesBuilderCb.put(cv, source(id));
                    ccBuilder.setCatchVariable(cv);
                    catchContext.variableContext().add(cv);
                    j++;
                } else {
                    throw new UnsupportedOperationException();
                }
                j++; // ) delimiter
                if (catchBlock.get(j) instanceof CodeBlock cb) {
                    String newIndex = index + "." + StringUtil.pad(blockCount, n);
                    Block cbb = parsers.parseBlock().parse(catchContext, newIndex, null, cb);
                    blockCount++;
                    Source source1 = source(newIndex, catchBlock);
                    Source source2 = detailedSourcesBuilderCb == null
                            ? source1 : source1.withDetailedSources(detailedSourcesBuilderCb.build());
                    builder.addCatchClause(ccBuilder.setBlock(cbb).setSource(source2).build());
                } else throw new UnsupportedOperationException();
                i++;
            }
            Block finallyBlock;
            if (i < tryStatement.size() && tryStatement.get(i) instanceof FinallyBlock fb) {
                Context finallyContext = context.newVariableContext("finallyBlock");
                String newIndex = index + "." + StringUtil.pad(blockCount, n);
                finallyBlock = parseBlockOrStatement(finallyContext, newIndex, fb.get(1));
            } else {
                finallyBlock = runtime.emptyBlock();
            }
            return builder.setFinallyBlock(finallyBlock).build();
        }

        if (statement instanceof BasicForStatement) {
            org.e2immu.language.cst.api.statement.ForStatement.Builder builder = runtime.newForBuilder();
            Context newContext = context.newVariableContext("for-loop");

            // initializers

            i += 2;
            if (statement.get(i) instanceof Delimiter d && Token.TokenType.SEMICOLON.equals(d.getType())) {
                // no initializer
                i++;
            } else if (statement.get(i) instanceof Statement s) {
                builder.addInitializer(parse(newContext, index, s));
                i += 2;
            } else if (statement.get(i) instanceof StatementExpression) {
                // there could be more than one
                do {
                    if (statement.get(i) instanceof StatementExpression se) {
                        Expression initializer = parsers.parseExpression().parse(newContext, index,
                                newContext.emptyForwardType(), se.getFirst());
                        builder.addInitializer(initializer);
                    } else throw new UnsupportedOperationException();
                    i += 2;
                } while (Token.TokenType.COMMA.equals(statement.get(i - 1).getType()));
            } else throw new UnsupportedOperationException();

            // condition

            if (statement.get(i) instanceof Delimiter d && Token.TokenType.SEMICOLON.equals(d.getType())) {
                // no condition
                builder.setExpression(runtime.newBoolean(List.of(), source(d), true));
                i++;
            } else {
                Expression condition = parsers.parseExpression().parse(newContext, index, context.emptyForwardType(), statement.get(i));
                builder.setExpression(condition);
                i += 2;
            }
            // updaters

            if ((statement.get(i) instanceof Delimiter d && Token.TokenType.RPAREN.equals(d.getType()))) {
                i++; // no updaters
            } else {
                while (statement.get(i) instanceof StatementExpression se) {
                    Expression updater = parsers.parseExpression().parse(newContext, index, newContext.emptyForwardType(),
                            se.getFirst());
                    builder.addUpdater(updater);
                    i += 2;
                    if (statement.get(i - 1) instanceof Delimiter d && Token.TokenType.RPAREN.equals(d.getType())) {
                        break;
                    }
                }
            }
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, statement.get(i));
            return builder.setBlock(block).setSource(source).addComments(comments).setLabel(label)
                    .addAnnotations(annotations).build();
        }

        if (statement instanceof SynchronizedStatement) {
            Context newContext = context.newVariableContext("synchronized");
            Expression expression = parsers.parseExpression().parse(context, index, context.emptyForwardType(),
                    statement.get(i + 2));
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, statement.get(i + 4));
            return runtime.newSynchronizedBuilder().setExpression(expression).setBlock(block)
                    .setSource(source).addComments(comments).setLabel(label).build();
        }

        if (statement instanceof BreakStatement) {
            String goToLabel;
            if (statement.size() > i + 1 && statement.get(i + 1) instanceof Identifier identifier) {
                goToLabel = identifier.getSource();
            } else {
                goToLabel = null;
            }
            return runtime.newBreakBuilder().setSource(source).addComments(comments).setLabel(label)
                    .addAnnotations(annotations).setGoToLabel(goToLabel).build();
        }

        if (statement instanceof ContinueStatement) {
            String goToLabel;
            if (statement.size() > i + 1 && statement.get(i + 1) instanceof Identifier identifier) {
                goToLabel = identifier.getSource();
            } else {
                goToLabel = null;
            }
            return runtime.newContinueBuilder().setSource(source).addComments(comments).setLabel(label)
                    .addAnnotations(annotations).setGoToLabel(goToLabel).build();
        }

        if (statement instanceof AssertStatement) {
            Expression expression = parsers.parseExpression().parse(context, index,
                    context.newForwardType(runtime.booleanParameterizedType()), statement.get(i + 1));
            Expression message = Token.TokenType.COLON.equals(statement.get(i + 2).getType())
                    ? parsers.parseExpression().parse(context, index, context.newForwardType(runtime.stringParameterizedType()),
                    statement.get(i + 3))
                    : runtime.newEmptyExpression();
            return runtime.newAssertBuilder().setSource(source).addComments(comments).setLabel(label)
                    .addAnnotations(annotations).setExpression(expression).setMessage(message).build();
        }

        if (statement instanceof ThrowStatement) {
            TypeInfo throwable = runtime.getFullyQualified(Throwable.class, false);
            ForwardType fwd = throwable != null ? context.newForwardType(throwable.asSimpleParameterizedType())
                    : context.emptyForwardType();
            Expression expression = parsers.parseExpression().parse(context, index, fwd, statement.get(i + 1));
            return runtime.newThrowBuilder()
                    .addComments(comments).setSource(source).setLabel(label)
                    .setExpression(expression).build();
        }

        if (statement instanceof SwitchStatement) {
            int no = countStatementsInOldStyleSwitch(statement);
            if (no > 0) {
                return parseOldStyleSwitch(context, index, statement, comments, source, annotations, label, no, i);
            }
            int nn = countStatementsInNewStyleSwitch(statement);
            if (nn >= 0) {
                return parseNewStyleSwitch(context, index, statement, comments, source, annotations, label, nn, i);
            }
        }

        if (statement instanceof YieldStatement ys) {
            ForwardType forwardType = context.newForwardType(context.enclosingMethod().returnType());
            Expression e;
            if (ys.size() == 2) {
                assert ys.get(1) instanceof Delimiter;
                e = runtime.newEmptyExpression();
            } else {
                e = parsers.parseExpression().parse(context, index, forwardType, ys.get(1));
            }
            assert e != null;
            return runtime.newYieldBuilder()
                    .setExpression(e).setSource(source).addComments(comments).setLabel(label)
                    .build();
        }

        if (statement instanceof CodeBlock cb) {
            // important: for reasons of consistency, an individual block has statements with index .0.n, not just .n
            return parsers.parseBlock().parse(context, index, label, cb, true, 0);
        }

        if (statement instanceof WhileStatement whileStatement) {
            Context newContext = context.newVariableContext("while");
            ForwardType forwardType = context.newForwardType(runtime.booleanParameterizedType());
            Expression expression = parsers.parseExpression().parse(context, index, forwardType, whileStatement.get(i + 2));
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, whileStatement.get(i + 4));
            return runtime.newWhileBuilder()
                    .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                    .setExpression(expression).setBlock(block).build();
        }

        if (statement instanceof DoStatement doStatement) {
            Context newContext = context.newVariableContext("do-while");
            ForwardType forwardType = context.newForwardType(runtime.booleanParameterizedType());
            Block block = parseBlockOrStatement(newContext, index + FIRST_BLOCK, doStatement.get(i + 1));
            Expression expression = parsers.parseExpression().parse(context, index, forwardType, doStatement.get(i + 4));
            return runtime.newDoBuilder()
                    .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                    .setExpression(expression).setBlock(block).build();
        }

        if (statement instanceof EmptyStatement) {
            return runtime.newEmptyStatementBuilder()
                    .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                    .build();
        }
        throw new UnsupportedOperationException("Node " + statement.getClass());
    }

    private ParameterizedType computeForwardType(Context context, TypeInfo iterable, ParameterizedType concreteIterableType) {
        if (iterable == null) {
            throw new UnsupportedOperationException("Cannot deduce var type in enhanced for without Iterable in the classpath");
        }
        if (concreteIterableType.arrays() > 0) {
            return concreteIterableType.copyWithOneFewerArrays();
        }
        ParameterizedType formal = iterable.asParameterizedType();
        Map<NamedType, ParameterizedType> map = context.genericsHelper().translateMap(formal, concreteIterableType,
                true);
        return map.values().stream().findFirst().orElseThrow();
    }

    private LocalVariableCreation localVariableCreation(Context context,
                                                        String index,
                                                        LocalVariableDeclaration nvd, int i,
                                                        List<LocalVariableCreation.Modifier> lvcModifiers,
                                                        Source source, DetailedSources.Builder detailedSourcesBuilder,
                                                        List<Comment> comments, String label,
                                                        List<AnnotationExpression> annotations) {
        LocalVariableCreation.Builder builder = runtime.newLocalVariableCreationBuilder();

        Node typeNode = nvd.get(i);
        ParameterizedType baseType = parsers.parseType().parse(context, typeNode, detailedSourcesBuilder);
        i++;
        boolean first = true;
        while (i < nvd.size() && nvd.get(i) instanceof VariableDeclarator vd) {
            Node vd0 = vd.getFirst();
            LocalVariable lv;
            if (vd0 instanceof KeyWord && Token.TokenType.UNDERSCORE.equals(vd0.getType())) {
                lv = runtime.newUnnamedLocalVariable(baseType, runtime.newEmptyExpression());
            } else {
                Identifier identifier;
                ParameterizedType type;
                if (vd0 instanceof VariableDeclaratorId vdi) {
                    identifier = (Identifier) vdi.getFirst();
                    int arrays = (vdi.size() - 1) / 2;
                    type = baseType.copyWithArrays(arrays);
                    if (detailedSourcesBuilder != null) {
                        detailedSourcesBuilder.put(type, source(typeNode));
                    }
                } else if (vd0 instanceof Identifier id) {
                    identifier = id;
                    type = baseType;
                } else throw new UnsupportedOperationException();
                String variableName = identifier.getSource();
                lv = runtime.newLocalVariable(variableName, type, runtime.newEmptyExpression());
                context.variableContext().add(lv);
                if (vd.size() > 2) {
                    ForwardType forwardType = context.newForwardType(type);
                    Expression expression = parsers.parseExpression().parse(context, index, forwardType, vd.get(2));
                    lv = runtime.newLocalVariable(variableName, type, expression);
                    // replace! See TestAssignment,2 for the cause of this silly construction
                    context.variableContext().add(lv);
                }
                if (detailedSourcesBuilder != null) {
                    detailedSourcesBuilder.put(lv, source(identifier));
                }
            }
            if (first) {
                builder.setLocalVariable(lv);
                first = false;
            } else {
                builder.addOtherLocalVariable(lv);
            }
            i += 2;
        }
        lvcModifiers.forEach(builder::addModifier);
        return builder
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments).setLabel(label).addAnnotations(annotations).build();
    }

    private LocalVariableCreation forEachElementWithVar(Context context, LocalVariableDeclaration varDeclaration,
                                                        ParameterizedType elementType) {
        int i = 0;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        LocalVariableCreation.Builder builder = runtime.newLocalVariableCreationBuilder();
        while (varDeclaration.get(i) instanceof Token token) {
            LocalVariableCreation.Modifier m = null;
            if (Token.TokenType.VAR.equals(token.getType())) {
                m = runtime.localVariableModifierVar();
            } else if (Token.TokenType.FINAL.equals(token.getType())) {
                m = runtime.localVariableModifierFinal();
            }
            if (m != null) {
                builder.addModifier(m);
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(token));
            }
            ++i;
        }
        VariableDeclarator vd = (VariableDeclarator) varDeclaration.get(i);
        LocalVariable lv;
        if (vd.getFirst() instanceof KeyWord kw && Token.TokenType.UNDERSCORE.equals(kw.getType())) {
            lv = runtime.newLocalVariable(elementType).withAssignmentExpression(runtime.newEmptyExpression());
        } else {
            Identifier identifier = (Identifier) vd.getFirst();
            String variableName = identifier.getSource();
            lv = runtime.newLocalVariable(variableName, elementType, runtime.newEmptyExpression());
        }
        Source source = source(varDeclaration);
        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(lv, source(vd.getFirst()));
        context.variableContext().add(lv);
        return builder.setLocalVariable(lv)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments(varDeclaration))
                .build();
    }

    private LocalVariableCreation localVariableCreationWithVar(Context context, String index, Node varDeclaration,
                                                               int i, List<LocalVariableCreation.Modifier> lvcModifiers,
                                                               Source source, DetailedSources.Builder detailedSourcesBuilder,
                                                               List<Comment> comments, String label,
                                                               List<AnnotationExpression> annotations) {
        LocalVariableCreation.Builder builder = runtime.newLocalVariableCreationBuilder();
        VariableDeclarator vd = (VariableDeclarator) varDeclaration.get(i);
        Identifier identifier = (Identifier) vd.get(0);
        ForwardType forwardType = context.emptyForwardType();
        Expression expression = parsers.parseExpression().parse(context, index, forwardType, vd.get(2));
        String variableName = identifier.getSource();
        ParameterizedType type = expression.parameterizedType();
        LocalVariable lv = runtime.newLocalVariable(variableName, type, expression);
        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(lv, source(identifier));
        context.variableContext().add(lv);
        lvcModifiers.forEach(builder::addModifier);
        return builder.setLocalVariable(lv)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments)
                .setLabel(label)
                .addAnnotations(annotations)
                .build();
    }

    private static int countCatchBlocks(TryStatement tryStatement) {
        return (int) tryStatement.children().stream().filter(n -> n instanceof CatchBlock).count();
    }

    private SwitchStatementNewStyle parseNewStyleSwitch(Context context, String index, Statement statement,
                                                        List<Comment> comments, Source source,
                                                        List<AnnotationExpression> annotations,
                                                        String label, int n, int start) {
        Expression selector = parsers.parseExpression().parse(context, index, context.emptyForwardType(),
                statement.get(start + 2));
        ForwardType selectorTypeFwd = context.newForwardType(selector.parameterizedType());
        List<SwitchEntry> entries = new ArrayList<>();

        Context enumContext;
        TypeInfo selectorTypeInfo = selector.parameterizedType().bestTypeInfo();
        if (selectorTypeInfo.typeNature().isEnum()) {
            enumContext = context.newVariableContext("switch-new-style-enum");
            selectorTypeInfo.fields().stream().filter(Info::isSynthetic)
                    .forEach(f -> enumContext.variableContext().add(runtime.newFieldReference(f)));
        } else {
            enumContext = null;
        }
        int count = 0;
        for (Node child : statement) {
            if (child instanceof NewCaseStatement ncs) {
                Context newContext = context.newVariableContext("switch-new-style");
                SwitchEntry.Builder entryBuilder = runtime.newSwitchEntryBuilder()
                        .setSource(source(ncs)).addComments(comments(ncs));
                if (ncs.getFirst() instanceof NewSwitchLabel nsl) {
                    parseNewSwitchLabel(index, nsl, newContext, entryBuilder, selectorTypeFwd, enumContext);
                } else throw new Summary.ParseException(newContext, "Expect NewCaseStatement");
                if (ncs.get(1) instanceof CodeBlock cb) {
                    String newIndex = index + "." + StringUtil.pad(count, n);
                    entryBuilder.setStatement(parsers.parseBlock().parse(newContext, newIndex, null, cb));
                } else if (ncs.get(1) instanceof Statement st) {
                    String newIndex = index + "." + StringUtil.pad(count, n) + "0";
                    entryBuilder.setStatement(parse(newContext, newIndex, st));
                } else throw new Summary.ParseException(newContext, "Expect statement");
                count++;
                entries.add(entryBuilder.build());
            }
        }
        return runtime.newSwitchStatementNewStyleBuilder()
                .addComments(comments)
                .setSource(source)
                .setLabel(label)
                .addAnnotations(annotations)
                .setSelector(selector)
                .addSwitchEntries(entries)
                .build();
    }

    private SwitchStatementOldStyle parseOldStyleSwitch(Context context, String index, Statement statement,
                                                        List<Comment> comments, Source source,
                                                        List<AnnotationExpression> annotations,
                                                        String label, int n, int start) {
        Expression selector = parsers.parseExpression().parse(context, index, context.emptyForwardType(),
                statement.get(2));
        Block.Builder builder = runtime.newBlockBuilder();
        List<SwitchStatementOldStyle.SwitchLabel> switchLabels = new ArrayList<>();
        Context newContext = context.newVariableContext("switch-old-style");

        TypeInfo selectorTypeInfo = selector.parameterizedType().bestTypeInfo();
        if (selectorTypeInfo.typeNature().isEnum()) {
            selectorTypeInfo.fields().stream().filter(Info::isSynthetic)
                    .forEach(f -> newContext.variableContext().add(runtime.newFieldReference(f)));
        }

        int pos = 0;

        for (int i = start + 5; i < statement.size(); i++) {
            if (statement.get(i) instanceof ClassicCaseStatement ccs) {
                if (ccs.getFirst() instanceof ClassicSwitchLabel csl) {
                    if (Token.TokenType._DEFAULT.equals(csl.getFirst().getType())) {
                        SwitchStatementOldStyle.SwitchLabel sld
                                = runtime.newSwitchLabelOldStyle(runtime.newEmptyExpression(), pos, null,
                                runtime.newEmptyExpression());
                        switchLabels.add(sld);
                    } else {
                        assert Token.TokenType.CASE.equals(csl.getFirst().getType());
                        for (int k = 1; k < csl.size(); k += 2) {
                            String newIndex = index + ".0." + StringUtil.pad(pos, n);

                            Node node = csl.get(k);
                            SwitchStatementOldStyle.SwitchLabel sl;
                            if (node instanceof TypePattern || node instanceof RecordPattern) {
                                org.e2immu.language.cst.api.element.RecordPattern recordPattern;
                                if (node instanceof TypePattern lvd) {
                                    recordPattern = parsers.parseRecordPattern()
                                            .parseLocalVariableDeclaration(context, lvd, 0, null);
                                } else {
                                    recordPattern = parsers.parseRecordPattern().parseRecordPattern(context, (RecordPattern) node);
                                }
                                Expression whenExpression;
                                if (csl.get(k + 1) instanceof WhenClause whenClause) {
                                    ForwardType forwardType = context.newForwardType(runtime.booleanParameterizedType());
                                    whenExpression = parsers.parseExpression().parse(context, index, forwardType, whenClause.get(1));
                                    k += 2;
                                } else {
                                    whenExpression = runtime.newEmptyExpression();
                                }
                                sl = runtime.newSwitchLabelOldStyle(null, pos, recordPattern, whenExpression);
                            } else {
                                Expression literal = parsers.parseExpression().parse(newContext, newIndex,
                                        newContext.emptyForwardType(), node);
                                sl = runtime.newSwitchLabelOldStyle(literal, pos,
                                        null, runtime.newEmptyExpression());
                            }
                            switchLabels.add(sl);
                        }
                    }
                }
                for (int j = 1; j < ccs.size(); j++) {
                    if (ccs.get(j) instanceof Statement s) {
                        String newIndex = index + ".0." + StringUtil.pad(pos, n);
                        org.e2immu.language.cst.api.statement.Statement st
                                = parse(context, newIndex, s);
                        builder.addStatement(st);
                        pos++;
                    }
                }
            }
        }
        return runtime.newSwitchStatementOldStyleBuilder()
                .addComments(comments).setSource(source).setLabel(label).addAnnotations(annotations)
                .setSelector(selector).setBlock(builder.build()).addSwitchLabels(switchLabels).build();
    }

    private static int countStatementsInOldStyleSwitch(Statement statement) {
        int n = 0;
        for (int i = 5; i < statement.size(); i++) {
            if (statement.get(i) instanceof ClassicCaseStatement ccs) {
                for (int j = 1; j < ccs.size(); j++) {
                    if (ccs.get(j) instanceof Statement) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static int countStatementsInNewStyleSwitch(Statement statement) {
        return statement.children().stream().mapToInt(n -> n instanceof NewCaseStatement ? 1 : 0).sum();
    }

    private Block parseBlockOrStatement(Context context, String index, Node node) {
        if (node instanceof CodeBlock codeBlock) {
            return parsers.parseBlock().parse(context, index, null, codeBlock);
        }
        if (node instanceof Statement s) {
            org.e2immu.language.cst.api.statement.Statement st = parse(context, index + FIRST_STATEMENT, s);
            return runtime.newBlockBuilder().addStatement(st).build();
        }
        throw new UnsupportedOperationException();
    }
}
