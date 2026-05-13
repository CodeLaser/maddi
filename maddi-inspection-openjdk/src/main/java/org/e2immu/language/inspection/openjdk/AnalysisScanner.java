package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.util.RecordSynthetics;
import org.e2immu.util.internal.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;

class AnalysisScanner extends TreePathScanner<Void, Void> implements SourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisScanner.class);

    private record BlockData(Block.Builder blockBuilder, String index, int numberOfStatements) {
    }

    private final Deque<TypeInfo> typeStack = new ArrayDeque<>();
    private final Deque<Map<String, Element>> elementStack = new ArrayDeque<>();

    private final Runtime runtime;
    private final List<TypeInfo> collectedPrimaryTypes = new ArrayList<>();
    private final Trees trees;
    private MethodInfo currentMethod;
    private final Deque<BlockData> blockBuilders = new ArrayDeque<>();
    private Expression currentExpression;
    private final CompilationUnit compilationUnit;
    private final SourcePositions sourcePositions;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;
    private final FlagHelper flagHelper;
    private final ConvertType convertType;
    private final TypeData typeData;
    private final SourceCodeScan.Result scanResult;

    AnalysisScanner(Runtime runtime,
                    CompilationUnit compilationUnit,
                    CompilationUnitTree compilationUnitTree,
                    Trees trees,
                    SourcePositions sourcePositions,
                    LineMap lineMap,
                    Elements elements,
                    SourceCodeScan.Result scanResult) {
        this.runtime = runtime;
        this.compilationUnit = compilationUnit;
        this.trees = trees;
        this.lineMap = lineMap;
        this.sourcePositions = sourcePositions;
        this.compilationUnitTree = compilationUnitTree;
        this.scanResult = scanResult;

        typeData = new TypeData();
        flagHelper = new FlagHelper(runtime);
        ClassSymbolScanner classSymbolScanner = new ClassSymbolScanner(runtime, flagHelper, elements, typeData);
        convertType = new ConvertType(runtime, classSymbolScanner, typeData, this::findInElementStack, this);
        classSymbolScanner.setConvertType(convertType);
    }

    // result
    public Collection<TypeInfo> types() {
        return collectedPrimaryTypes;
    }

    // -- Class declarations ----------------------------------------------

    @Override
    public Void visitClass(ClassTree node, Void p) {
        JCTree.JCClassDecl jcClassDecl = (JCTree.JCClassDecl) node;
        TypeInfo typeInfo;

        String simpleName = node.getSimpleName().toString();
        if (typeStack.isEmpty()) {
            typeInfo = runtime.newTypeInfo(compilationUnit, simpleName);
            collectedPrimaryTypes.add(typeInfo);
        } else {
            TypeInfo enclosed = typeStack.getLast();
            typeInfo = runtime.newTypeInfo(enclosed, simpleName);
            enclosed.builder().addSubType(typeInfo);
        }
        typeStack.addLast(typeInfo);
        elementStack.addLast(new HashMap<>());
        typeData.put(typeInfo);

        // flags: modifiers, type nature
        flagHelper.type(jcClassDecl.getModifiers().flags, typeInfo.builder(), simpleName);

        // type parameters
        for (JCTree.JCTypeParameter jcTypeParameter : jcClassDecl.getTypeParameters()) {
            int index = jcTypeParameter.pos;
            String name = jcTypeParameter.getName().toString();
            TypeParameter tp = runtime.newTypeParameter(index, name, typeInfo);
            typeInfo.builder().addOrSetTypeParameter(tp);
            elementStack.getLast().put(name, tp);
        }

        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType parentClass;
        if (typeInfo.typeNature().isEnum()) {
            if (jcClassDecl.type instanceof Type.ClassType ct) {
                parentClass = convertType.convert(ct.supertype_field);
            } else throw new UnsupportedOperationException("NYI");
        } else {
            ParameterizedType explicitParentClass = convertType.convertTree(jcClassDecl.extending, dsb);
            parentClass = explicitParentClass.isVoid() ? runtime.objectParameterizedType()
                    : explicitParentClass;
        }
        typeInfo.builder().setParentClass(parentClass);
        if (!jcClassDecl.implementing.isEmpty()) {
            String keyword = typeInfo.isInterface() ? "extends" : "implements";
            Source source = scanResult.find(keyword, sourceForNode(jcClassDecl.implementing.getFirst()));
            dsb.put(DetailedSources.IMPLEMENTS, source);
            for (JCTree.JCExpression i : jcClassDecl.implementing) {
                typeInfo.builder().addInterfaceImplemented(convertType.convertTree(i, dsb));
            }
        }
        for (JCTree.JCExpression permits : jcClassDecl.permitting) {
            TypeInfo permitted = convertType.convert(permits.type).typeInfo();
            typeInfo.builder().addPermittedType(permitted);
            dsb.put(permitted, sourceForNode(permits));
        }

        // record components: fields and accessors
        if (typeInfo.typeNature().isRecord()) {
            RecordSynthetics recordSynthetics = new RecordSynthetics(runtime, typeInfo);
            for (var rc : jcClassDecl.sym.getRecordComponents()) {
                ParameterizedType pt = convertType.convert(rc.type);
                FieldInfo fieldInfo = runtime.newFieldInfo(rc.name.toString(), false, pt, typeInfo);
                fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal())
                        .addFieldModifier(runtime.fieldModifierPrivate());
                typeInfo.builder().addField(fieldInfo);
                MethodInfo accessor = recordSynthetics.createAccessor(fieldInfo);
                typeInfo.builder().addMethod(accessor);
                typeData.put(rc.accessor, accessor);
            }
        }
        // annotations
        for (JCTree.JCAnnotation annotation : jcClassDecl.getModifiers().getAnnotations()) {
            AnnotationExpression ae = convertAnnotation(annotation);
            typeInfo.builder().addAnnotation(ae);
        }

        // members: methods, fields
        for (var member : node.getMembers()) {
            currentMethod = null;
            scan(member, p);
        }
        MethodInfo singleAbstractMethod = convertType.computeSAM(typeInfo);
        typeInfo.builder().setSingleAbstractMethod(singleAbstractMethod);

        Source source = sourceForNode(node, dsb);
        typeInfo.builder()
                .addTrailingComments(trailingCommentsForNode(source))
                .addComments(commentsForNode(source))
                .setSource(source);

        typeStack.removeLast();
        elementStack.removeLast();
        return null;
    }

    // -- Method declarations ---------------------------------------------

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        JCTree.JCMethodDecl jcMethod = (JCTree.JCMethodDecl) node;
        String methodName = node.getName().toString();
        MethodInfo methodInfo;
        // construction of the method
        TypeInfo currentType = typeStack.getLast();
        long methodFlags = jcMethod.getModifiers().flags;
        boolean isConstructor = "<init>".equals(methodName);
        if (isConstructor) {
            methodInfo = runtime.newConstructor(currentType);
            currentType.builder().addConstructor(methodInfo);
            methodInfo.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());
        } else {
            MethodInfo.MethodType methodType = flagHelper.methodType(methodFlags, typeStack.getLast().isInterface());
            methodInfo = runtime.newMethod(currentType, methodName, methodType);
            currentType.builder().addMethod(methodInfo);
        }
        typeData.put(jcMethod.sym, methodInfo);

        // flags
        flagHelper.method(methodFlags, methodInfo.builder());

        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();

        // type parameters
        for (JCTree.JCTypeParameter typeParameter : jcMethod.getTypeParameters()) {
            int index = typeParameter.pos;
            String name = typeParameter.getName().toString();
            TypeParameter tp = runtime.newTypeParameter(index, name, methodInfo);
            methodInfo.builder().addTypeParameter(tp);
            elementStack.getLast().put(name, tp);
        }

        // return type

        if (!isConstructor) {
            ParameterizedType returnType = convertType.convertTree(node.getReturnType(), dsb);
            methodInfo.builder().setReturnType(returnType);
        }

        // parameters
        HashMap<String, Element> parameterMap = new HashMap<>();
        for (JCTree.JCVariableDecl jcVariableDecl : jcMethod.getParameters()) {
            String name = jcVariableDecl.getName().toString();
            DetailedSources.Builder dsbParam = runtime.newDetailedSourcesBuilder();
            ParameterizedType type = convertType.convertTree(jcVariableDecl.getType(), dsbParam);
            ParameterInfo parameterInfo = methodInfo.builder().addParameter(name, type);

            // flags
            long flags = jcVariableDecl.getModifiers().flags;
            boolean isFinal = (flags & Flags.FINAL) != 0;
            boolean varargs = (flags & Flags.VARARGS) != 0;
            parameterInfo.builder().setVarArgs(varargs).setIsFinal(isFinal);

            // annotations
            for (JCTree.JCAnnotation annotation : jcVariableDecl.getModifiers().getAnnotations()) {
                AnnotationExpression ae = convertAnnotation(annotation);
                parameterInfo.builder().addAnnotation(ae);
            }
            parameterInfo.builder().setSource(sourceForNode(jcVariableDecl, dsbParam)).commit();
            parameterMap.put(parameterInfo.simpleName(), parameterInfo);
        }
        methodInfo.builder().commitParameters();

        // record synthetic constructor?
        if (methodInfo.isSynthetic() && currentType.typeNature().isRecord()) {
            for (ParameterInfo pi : methodInfo.parameters()) {
                FieldInfo field = currentType.getFieldByName(pi.name(), true);
                field.builder().setInitializer(runtime.newVariableExpression(pi));
            }
        }

        // annotations
        for (JCTree.JCAnnotation annotation : jcMethod.getModifiers().getAnnotations()) {
            AnnotationExpression ae = convertAnnotation(annotation);
            methodInfo.builder().addAnnotation(ae);
        }

        // method body
        elementStack.addLast(parameterMap);
        currentMethod = methodInfo;
        Block methodBody = parseBlock("-", node.getBody());
        elementStack.removeLast();
        currentMethod = null;

        Source source = sourceForNode(node, dsb);
        methodInfo.builder()
                .setSource(source)
                .addComments(commentsForNode(source))
                .setMethodBody(methodBody)
                .computeAccess()
                .commit();
        return null;
    }

    // -- Annotations ---------------------------------------------

    // FIXME add expressions
    private AnnotationExpression convertAnnotation(JCTree.JCAnnotation annotation) {
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType at = convertType.convertTree(annotation.getAnnotationType(), dsb);
        return runtime.newAnnotationExpressionBuilder()
                .setSource(sourceForNode(annotation, dsb))
                .setTypeInfo(at.typeInfo())
                .build();
    }

    // -- Statements ---------------------------------------------


    private void replaceLastStatement(Statement statement) {
        List<Statement> statements = blockBuilders.getLast().blockBuilder.statements();
        statements.removeLast();
        statements.add(statement);
    }

    private Statement lastStatement() {
        BlockData bd = blockBuilders.getLast();
        if (bd.blockBuilder.statements().isEmpty()) return null;
        return bd.blockBuilder.statements().getLast();
    }

    private void addStatement(Statement statement) {
        blockBuilders.getLast().blockBuilder.addStatement(statement);
    }

    private String statementIndex() {
        if (blockBuilders.isEmpty()) return "-";
        BlockData bd = blockBuilders.getLast();
        String padded = StringUtil.pad(bd.blockBuilder.statements().size(), bd.numberOfStatements);
        return ("-".equals(bd.index) ? "" : bd.index + ".") + padded;
    }

    private Block parseBlock(String blockIndex, Tree node) {
        List<JCTree.JCStatement> statements;
        Source source = statementSourceForNode(node);

        switch (node) {
            case JCTree.JCBlock block -> statements = block.stats;
            case JCTree.JCStatement statement -> statements = List.of(statement);
            case null -> {
                return runtime.newBlockBuilder()
                        .setSource(source)
                        .addComments(commentsForNode(source))
                        .build();
            }
            default -> throw new UnsupportedOperationException("NYI");
        }
        elementStack.push(new HashMap<>());

        int n = statements.size();
        String i = "-".equals(blockIndex) ? "-" : statementIndex() + "." + blockIndex;
        blockBuilders.addLast(new BlockData(runtime.newBlockBuilder(), i, n));

        for (JCTree.JCStatement statement : statements) {
            if (statement instanceof JCTree.JCBlock subBlock) {
                Block parsedSub = parseBlock("0", subBlock);
                addStatement(parsedSub);
            } else {
                scan(statement, null);
            }
        }

        elementStack.pop();

        return blockBuilders.removeLast().blockBuilder
                .setSource(source)
                .addTrailingComments(trailingCommentsForNode(source))
                .addComments(commentsForNode(source))
                .build();
    }

    @Override
    public Void visitBreak(BreakTree node, Void unused) {
        String gotoLabel = node.getLabel() == null ? null : node.getLabel().toString();
        addStatement(runtime.newBreakBuilder().setGoToLabel(gotoLabel).setSource(statementSourceForNode(node)).build());
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree node, Void unused) {
        String gotoLabel = node.getLabel() == null ? null : node.getLabel().toString();
        addStatement(runtime.newContinueBuilder().setGoToLabel(gotoLabel).setSource(statementSourceForNode(node)).build());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
        LOGGER.info("Expression statement");
        super.visitExpressionStatement(node, unused);
        if (currentExpression != null) {
            Source source = statementSourceForNode(node);
            ExpressionAsStatement statement = runtime.newExpressionAsStatementBuilder()
                    .setSource(source)
                    .addComments(commentsForNode(source))
                    .setExpression(currentExpression)
                    .build();
            addStatement(statement);
        } // else: was explicit constructor invocation, a statement
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
        currentExpression = null;
        scan(node.getExpression(), unused);
        Expression iterable = currentExpression;
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();

        LocalVariableCreation lvc;
        if (node.getVariable() instanceof JCTree.JCVariableDecl variableDecl) {
            String name = variableDecl.name.toString();
            ParameterizedType type = convertType.convertTree(node.getVariable().getType(), dsb);
            currentExpression = runtime.newEmptyExpression();
            lvc = continueLocalVariableCreation(variableDecl, name, type, dsb, null);
        } else throw new UnsupportedOperationException("NYI");

        Block block = parseBlock("0", node.getStatement());
        addStatement(runtime.newForEachBuilder()
                .setSource(statementSourceForNode(node))
                .setBlock(block)
                .setExpression(iterable)
                .setInitializer(lvc)
                .build());
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void unused) {
        ForStatement.Builder forBuilder = runtime.newForBuilder();

        Map<String, Element> map = new HashMap<>();
        elementStack.addLast(map);
        if (!node.getInitializer().isEmpty()) {
            if (node.getInitializer().getFirst() instanceof JCTree.JCVariableDecl) {
                // sequence of LVCs, but we want them all together in one statement
                LocalVariableCreation.Builder lvcBuilder = null;
                for (StatementTree statementTree : node.getInitializer()) {
                    Block initBlock = parseBlock("?", statementTree);
                    Statement first = initBlock.statements().getFirst();
                    if (first instanceof LocalVariableCreation f) {
                        if (lvcBuilder == null) {
                            lvcBuilder = runtime.newLocalVariableCreationBuilder().setLocalVariable(f.localVariable());
                        } else {
                            LocalVariableCreation.Builder finalLvcBuilder = lvcBuilder;
                            f.localVariableStream().forEach(finalLvcBuilder::addOtherLocalVariable);
                        }
                    } else throw new UnsupportedOperationException("NYI");
                }
                assert lvcBuilder != null;
                LocalVariableCreation built = lvcBuilder.build();
                forBuilder.addInitializer(built);
                built.localVariableStream().forEach(lv -> map.put(lv.simpleName(), lv));
            } else {
                for (StatementTree statementTree : node.getInitializer()) {
                    Block initBlock = parseBlock("?", statementTree);
                    Statement first = initBlock.statements().getFirst();
                    if (first instanceof ExpressionAsStatement eas) {
                        forBuilder.addInitializer(eas.expression());
                    } else throw new UnsupportedOperationException("NYI");
                }
            }
        }
        currentExpression = null;
        scan(node.getCondition(), unused);
        forBuilder.setExpression(currentExpression == null ? runtime.constantTrue() : currentExpression);

        for (ExpressionStatementTree est : node.getUpdate()) {
            Block initBlock = parseBlock("?", est);
            Statement first = initBlock.statements().getFirst();
            if (first instanceof ExpressionAsStatement eas) {
                forBuilder.addUpdater(eas.expression());
            }
        }

        Block block = parseBlock("0", node.getStatement());
        elementStack.removeLast();
        addStatement(forBuilder
                .setBlock(block)
                .setSource(statementSourceForNode(node))
                .build());
        return null;
    }

    @Override
    public Void visitIf(IfTree node, Void unused) {
        currentExpression = null;
        scan(node.getCondition(), unused);
        Expression condition = currentExpression;

        Block block = parseBlock("0", node.getThenStatement());
        Block elseBlock = parseBlock("1", node.getElseStatement());

        addStatement(runtime.newIfElseBuilder()
                .setSource(statementSourceForNode(node))
                .setIfBlock(block)
                .setElseBlock(elseBlock)
                .setExpression(condition)
                .build());
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
        currentExpression = null;
        scan(node.getExpression(), unused);
        Source source = statementSourceForNode(node);
        addStatement(runtime.newReturnBuilder()
                .setSource(source)
                .addComments(commentsForNode(source))
                .setExpression(currentExpression == null ? runtime.newEmptyExpression() : currentExpression)
                .build());
        return null;
    }

    // note: also field declarations

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        LOGGER.info("VARIABLE:" + node.getName().toString());

        if (node instanceof JCTree.JCVariableDecl variableDecl) {
            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
            if (variableDecl.sym instanceof Symbol.VarSymbol varSymbol) {
                String name = varSymbol.toString();
                ParameterizedType type = convertType.convertTree(variableDecl.vartype, dsb);

                currentExpression = null;
                scan(node.getInitializer(), p);
                if (currentExpression == null) {
                    currentExpression = runtime.newEmptyExpression();
                }
                if (currentMethod == null) {
                    // field!
                    long flags = variableDecl.getModifiers().flags;
                    boolean isStatic = (flags & Flags.STATIC) != 0;
                    TypeInfo owner = typeStack.getLast();
                    if (!owner.typeNature().isRecord() || isStatic) {
                        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
                        flagHelper.field(flags, fieldInfo.builder());
                        fieldInfo.builder().setSource(sourceForNode(node))
                                .setInitializer(currentExpression);
                        owner.builder().addField(fieldInfo);

                        // annotations
                        for (JCTree.JCAnnotation annotation : variableDecl.getModifiers().getAnnotations()) {
                            AnnotationExpression ae = convertAnnotation(annotation);
                            fieldInfo.builder().addAnnotation(ae);
                        }

                        fieldInfo.builder()
                                .setSource(sourceForNode(variableDecl, dsb))
                                .commit();
                        typeData.put(varSymbol, fieldInfo);
                    } // else: non-static record components are dealt with in the type visitor
                } else {

                    // local variable

                    Statement prev = lastStatement();
                    LocalVariableCreation prevLvc = prev instanceof LocalVariableCreation lvc2 ? lvc2 : null;
                    LocalVariableCreation lvc = continueLocalVariableCreation(variableDecl, name, type, dsb, prevLvc);
                    if (prevLvc != null && sameLvc(prevLvc, lvc)) {
                        LocalVariableCreation merged = prevLvc.withAdditionalLocalVariable(lvc);
                        replaceLastStatement(merged);
                    } else {
                        addStatement(lvc);
                    }
                }
            }
        }
        return null;
    }

    private boolean sameLvc(LocalVariableCreation lvc1, LocalVariableCreation lvc2) {
        Source s1 = lvc1.source();
        Source s2 = lvc2.source();
        return s1.beginPos() == s2.beginPos() && s1.beginLine() == s2.beginLine();
    }

    private @NotNull LocalVariableCreation continueLocalVariableCreation(JCTree.JCVariableDecl variableDecl,
                                                                         String name,
                                                                         ParameterizedType type,
                                                                         DetailedSources.Builder dsb, LocalVariableCreation prevLvc) {
        LocalVariable localVariable = runtime.newLocalVariable(name, type, currentExpression);
        LocalVariableCreation.Builder lvcb = runtime.newLocalVariableCreationBuilder()
                .setSource(sourceForNode(variableDecl))
                .setLocalVariable(localVariable);
        long flags = variableDecl.getModifiers().flags;
        boolean isFinal = (flags & Flags.FINAL) != 0;
        if (isFinal) lvcb.addModifier(runtime.localVariableModifierFinal());

        // annotations
        for (JCTree.JCAnnotation annotation : variableDecl.getModifiers().getAnnotations()) {
            AnnotationExpression ae = convertAnnotation(annotation);
            lvcb.addAnnotation(ae);
        }

        // this is a lot of work to find out exactly where in the sources the 2nd one starts...
        // FIXME the -2 in startAtEnd is hardCoded and not obviously correct, representing the ", "
        String s = variableDecl.toString(); // int b = 3; even in 'int a = 4, b = 3'
        Source thisSource = sourceForNode(variableDecl);
        Source source = prevLvc != null ? startAtEnd(prevLvc.source(), thisSource) : thisSource;
        // FIXME ideally we check for at least a space before name, and a non-alphanumeric after name
        Source namePos = source.ofIndex(s, s.indexOf(name), name.length());
        assert namePos != null;
        dsb.put(name, namePos);
        Source assignSource = source.ofIndex(s, s.indexOf("="), 1);
        if (assignSource != null) {
            dsb.putList(DetailedSources.LOCAL_VARIABLE_ASSIGNMENT_OPERATORS, List.of(assignSource));
        }
        lvcb.setSource(statementSourceForNode(variableDecl, dsb));
        elementStack.getLast().put(localVariable.simpleName(), localVariable);
        return lvcb.build();
    }

    private Source startAtEnd(Source s1, Source s2) {
        return runtime.newParserSource(s1.index(), s1.endLine(), s1.endPos() - 2, s2.endLine(), s2.endPos());
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void unused) {
        currentExpression = null;
        scan(node.getCondition(), unused);
        Expression condition = currentExpression;
        Block block = parseBlock("0", node.getStatement());
        addStatement(runtime.newWhileBuilder()
                .setSource(statementSourceForNode(node))
                .setBlock(block)
                .setExpression(condition)
                .build());
        return null;
    }

    // -- Expressions ---------------------------------------------


    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void unused) {
        JCTree.JCArrayAccess aa = (JCTree.JCArrayAccess) node;
        scan(aa.indexed, unused);
        Expression array = currentExpression;
        scan(aa.index, unused);
        Expression index = currentExpression;
        currentExpression = runtime.newVariableExpressionBuilder().setSource(sourceForNode(node))
                .setVariable(runtime.newDependentVariable(array, index)).build();
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
        JCTree.JCAssign assign = (JCTree.JCAssign) node;
        scan(assign.rhs, unused);
        Expression value = currentExpression;
        scan(assign.lhs, unused);
        VariableExpression target = (VariableExpression) currentExpression;
        currentExpression = runtime.newAssignmentBuilder()
                .setSource(sourceForNode(node))
                .setTarget(target)
                .setValue(value)
                .build();
        return null;
    }

    @Override
    public Void visitBinary(BinaryTree node, Void unused) {
        JCTree.JCBinary binary = (JCTree.JCBinary) node;
        JCTree.Tag opcode = binary.getTag();
        if (JCTree.Tag.AND.equals(opcode)) {
            List<Expression> expressions = andOrExpressions(JCTree.Tag.AND, binary);
            currentExpression = runtime.newAndBuilder()
                    .setSource(sourceForNode(node))
                    .addExpressions(expressions)
                    .build();
            return null;
        }
        if (JCTree.Tag.OR.equals(opcode)) {
            List<Expression> expressions = andOrExpressions(JCTree.Tag.OR, binary);
            currentExpression = runtime.newOrBuilder()
                    .setSource(sourceForNode(node))
                    .addExpressions(expressions)
                    .build();
            return null;
        }

        scan(node.getLeftOperand(), unused);
        Expression lhs = currentExpression;
        scan(node.getRightOperand(), unused);
        Expression rhs = currentExpression;

        MethodInfo operator = switch (opcode) {
            case PLUS -> {
                if (lhs.parameterizedType().isJavaLangString() || rhs.parameterizedType().isJavaLangString()) {
                    yield runtime.plusOperatorString();
                }
                yield runtime.plusOperatorInt();
            }
            case BITXOR -> runtime.xorOperatorInt();
            case BITAND -> runtime.andOperatorInt();
            case BITOR -> runtime.orOperatorInt();
            case MINUS -> runtime.minusOperatorInt();
            case MUL -> runtime.multiplyOperatorInt();
            case DIV -> runtime.divideOperatorInt();
            case MOD -> runtime.remainderOperatorInt();
            case EQ -> runtime.equalsOperatorInt();
            case NE -> runtime.notEqualsOperatorInt();
            case GE -> runtime.greaterEqualsOperatorInt();
            case GT -> runtime.greaterOperatorInt();
            case LE -> runtime.lessEqualsOperatorInt();
            case LT -> runtime.lessOperatorInt();
            case SL -> runtime.leftShiftOperatorInt();
            case SR -> runtime.signedRightShiftOperatorInt();
            case USR -> runtime.unsignedRightShiftOperatorInt();
            default -> throw new UnsupportedOperationException("NYI");
        };
        Precedence precedence = switch (opcode) {
            case PLUS, MINUS -> runtime.precedenceAdditive();
            case MUL, DIV -> runtime.precedenceMultiplicative();
            case EQ -> runtime.precedenceEquality();
            case BITOR -> runtime.precedenceBitwiseOr();
            case BITXOR -> runtime.precedenceBitwiseXor();
            case BITAND -> runtime.precedenceBitwiseAnd();
            case LT, LE, GT, GE -> runtime.precedenceRelational();
            default -> throw new UnsupportedOperationException();
        };
        ParameterizedType type = convertType.convert(binary.type);
        currentExpression = runtime.newBinaryOperatorBuilder()
                .setLhs(lhs).setOperator(operator).setRhs(rhs)
                .setSource(sourceForNode(node))
                .setPrecedence(precedence)
                .setParameterizedType(type)
                .build();
        return null;
    }

    private List<Expression> andOrExpressions(JCTree.Tag tag, JCTree.JCBinary binary) {
        JCTree.JCBinary b = binary;
        List<Expression> expressions = new ArrayList<>();
        while (b.getLeftOperand() instanceof JCTree.JCBinary sub && tag.equals(sub.getTag())) {
            scan(sub.rhs, null);
            expressions.addFirst(currentExpression);
            b = sub;
        }
        scan(b.getLeftOperand(), null);
        expressions.addFirst(currentExpression);
        scan(binary.getRightOperand(), null);
        expressions.add(currentExpression);
        return List.copyOf(expressions);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        JCTree.JCLambda lambda = (JCTree.JCLambda) node;
        Source source = sourceForNode(node);

        TypeInfo enclosingType = currentMethod.typeInfo();
        int typeIndex = enclosingType.builder().getAndIncrementAnonymousTypes();
        TypeInfo anonymousType = runtime.newAnonymousType(enclosingType, typeIndex);
        anonymousType.builder()
                .setAccess(runtime.accessPrivate())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType());

        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) lambda.owner;
        String methodName = methodSymbol.name.toString();
        MethodInfo methodInfo = runtime.newMethod(anonymousType, methodName, runtime.methodTypeMethod());
        MethodInfo.Builder miBuilder = methodInfo.builder();

        ParameterizedType concreteReturnType = convertType.convert(methodSymbol.getReturnType());
        ParameterizedType functionalType = convertType.convert(lambda.target);

        List<Lambda.OutputVariant> outputVariants = new ArrayList<>();

        Map<String, Element> lambdaParameters = new HashMap<>();
        elementStack.addLast(lambdaParameters);
        for (var parameter : lambda.getParameters()) {
            if (parameter instanceof JCTree.JCVariableDecl vd) {
                DetailedSources.Builder dsbParam = runtime.newDetailedSourcesBuilder();
                ParameterInfo pi;
                String name = vd.name.toString();
                ParameterizedType type = convertType.convertTree(vd.getType(), dsbParam);
                if ("_".equals(name)) {
                    pi = miBuilder.addUnnamedParameter(type);
                } else {
                    pi = miBuilder.addParameter(name, type);
                }
                pi.builder()
                        .setSource(sourceForNode(parameter, dsbParam))
                        .commit();
                outputVariants.add(runtime.lambdaOutputVariantEmpty()); // TODO
                lambdaParameters.put(name, pi);
            } else throw new UnsupportedOperationException("NYI");
        }
        Block methodBody;
        if (lambda.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION) {
            scan(lambda.body, unused);
            Expression tExpression = currentExpression;

            Statement returnStatement = runtime.newReturnBuilder()
                    .setSource(sourceForNode(lambda.body))
                    .setExpression(tExpression)
                    .build();
            methodBody = runtime.newBlockBuilder().addStatement(returnStatement).build();
        } else if (lambda.getBodyKind() == LambdaExpressionTree.BodyKind.STATEMENT) {
            throw new UnsupportedOperationException("NYI");
        } else {
            throw new UnsupportedOperationException("NYI");
        }

        elementStack.removeLast();

        miBuilder.setAccess(runtime.accessPublic())
                .setSynthetic(true)
                .setSource(source)
                .setMethodBody(methodBody)
                .setReturnType(concreteReturnType)
                .commit();

        anonymousType.builder()
                .addMethod(methodInfo)
                .addInterfaceImplemented(functionalType)
                .setEnclosingMethod(currentMethod)
                .setSingleAbstractMethod(methodInfo)
                .setSource(source)
                .commit();

        currentExpression = runtime.newLambdaBuilder()
                .addAnnotations(List.of()) // TODO
                .addComments(List.of()) // TODO
                .setSource(source)
                .setMethodInfo(methodInfo)
                .setOutputVariants(outputVariants)
                .build();
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void unused) {
        JCTree.JCLiteral literal = (JCTree.JCLiteral) node;

        List<Comment> comments = List.of();
        Source source = sourceForNode(node);
        currentExpression = switch (literal.typetag) {
            case INT -> runtime.newInt(comments, source, (Integer) literal.value);
            case DOUBLE -> runtime.newDouble(comments, source, (Double) literal.value);
            case LONG -> runtime.newLong(comments, source, (Long) literal.value);
            case FLOAT -> runtime.newFloat(comments, source, (Float) literal.value);
            case SHORT -> runtime.newShort(comments, source, (Short) literal.value);
            case BOOLEAN -> runtime.newBoolean(comments, source, ((Integer) literal.value) == 1);
            case CHAR -> runtime.newChar(comments, source, (char) (int) (Integer) literal.value);
            case CLASS -> {
                Tree.Kind kind = literal.typetag.getKindLiteral();
                if (Tree.Kind.STRING_LITERAL == kind) {
                    yield runtime.newStringConstant(comments, source, (String) literal.value);
                }
                throw new UnsupportedOperationException("?");
            }
            case BOT -> runtime.newNullConstant(comments, source);
            default -> throw new UnsupportedOperationException();
        };

        return super.visitLiteral(node, unused);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            // Filter out TYPE and PACKAGE identifiers for brevity;
            // you probably want all of these in a real analyzer.
            String name = node.getName().toString();
            switch (element.getKind()) {
                case FIELD -> {
                    if (element instanceof Symbol.VarSymbol vs) {
                        String owner = vs.owner.toString();
                        TypeInfo typeInfoOwner = typeData.getType(owner);
                        boolean isThis = "this".equals(name);
                        boolean isSuper = "super".equals(name);
                        Variable variable;
                        if (isThis || isSuper) {
                            // TODO explicitly write type
                            variable = runtime.newThis(typeInfoOwner.asParameterizedType(), null, isSuper);
                        } else {
                            FieldInfo fieldInfo = typeInfoOwner.getFieldByName(name, false);
                            if (fieldInfo == null) {
                                throw new UnsupportedOperationException("Cannot find field " + name + " in " + owner);
                            }
                            variable = runtime.newFieldReference(fieldInfo);
                        }
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(sourceForNode(node))
                                .setVariable(variable)
                                .build();

                    } else throw new UnsupportedOperationException();
                }
                case LOCAL_VARIABLE, PARAMETER -> {
                    Variable variable = (Variable) findInElementStack(name);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(sourceForNode(node))
                            .setVariable(variable)
                            .build();
                }
                case PACKAGE -> LOGGER.debug("Skipping package {}", node);
                case CLASS, INTERFACE, RECORD -> {
                    if (element instanceof Symbol.ClassSymbol classSymbol) {
                        ParameterizedType type = convertType.convert(classSymbol.type);
                        currentExpression = runtime.newTypeExpressionBuilder()
                                .setDiamond(runtime.diamondNo()) // TODO
                                .setParameterizedType(type)
                                .build();
                    } else throw new UnsupportedOperationException("NYI");
                }
                case ENUM -> {
                    //   if (element instanceof Symbol.ClassSymbol) {
                    //ParameterizedType type = convertType.convert(classSymbol.type);
                    //       throw new UnsupportedOperationException("NYI");
                    //   }
                }
                case ENUM_CONSTANT -> {
                    if (element instanceof Symbol.VarSymbol vs) {
                        FieldInfo fieldInfo = typeData.getField(vs);
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(sourceForNode(node))
                                .setVariable(runtime.newFieldReference(fieldInfo))
                                .build();
                    } else throw new UnsupportedOperationException("NYI");
                }
                case TYPE_PARAMETER -> {
                    if (element instanceof Symbol.TypeVariableSymbol tvs) {
                        TypeParameter typeParameter = (TypeParameter) findInElementStack(tvs.name.toString());
                        currentExpression = runtime.newTypeExpressionBuilder()
                                .setDiamond(runtime.diamondNo()) // TODO
                                .setParameterizedType(runtime.newParameterizedType(typeParameter, 0, null))
                                .build();
                    }
                }
                default -> throw new UnsupportedOperationException("NYI");
            }
        }
        return null;// super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree node, Void unused) {
        JCTree.JCMemberReference mr = (JCTree.JCMemberReference) node;
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        currentExpression = null;
        scan(mr.getQualifierExpression(), unused);
        Expression scope = currentExpression;
        MethodInfo method;
        if (mr.sym instanceof Symbol.MethodSymbol ms) {
            method = typeData.getMethod(ms);
        } else throw new UnsupportedOperationException();
        ParameterizedType concreteFunctionalType = convertType.convert(mr.type);
        MethodInfo sam = concreteFunctionalType.typeInfo().singleAbstractMethod();
        ParameterizedType concreteReturnType = method.isConstructor()
                ? scope.parameterizedType()
                : method.returnType();
        List<ParameterizedType> concreteParameterTypes = method.parameters().stream()
                .map(ParameterInfo::parameterizedType).toList();
        currentExpression = runtime.newMethodReferenceBuilder()
                .setScope(scope)
                .setMethod(method)
                .setConcreteFunctionalType(concreteFunctionalType)
                .setConcreteParameterTypes(concreteParameterTypes)
                .setConcreteReturnType(concreteReturnType)
                .setSource(sourceForNode(node, dsb))
                .build();
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {

        if (node instanceof JCTree.JCFieldAccess fieldAccess) {
            // class literal
            if ("class".equals(fieldAccess.name.toString())) {
                DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                ParameterizedType classType = convertType.convertTree(fieldAccess, dsb);
                ParameterizedType realType;
                if (fieldAccess.selected instanceof JCTree.JCIdent ident) {
                    realType = convertType.convert(ident.type);
                } else if (fieldAccess.selected instanceof JCTree.JCFieldAccess fa) {
                    realType = convertType.convertTree(fa, dsb);
                } else {
                    throw new UnsupportedOperationException();
                }
                currentExpression = runtime.newClassExpressionBuilder(realType)
                        .setSource(sourceForNode(node, dsb))
                        .setClassType(classType).build();
                return null;
            }

            // static field access, no need to generate a TypeExpression
            if (fieldAccess.sym instanceof Symbol.VarSymbol vs) {
                scan(fieldAccess.getExpression(), unused);
                Expression scope = currentExpression;
                ParameterizedType concreteType = convertType.convert(fieldAccess.type);
                if ("length".equals(vs.name.toString())) {
                    currentExpression = runtime.newArrayLengthBuilder()
                            .setSource(sourceForNode(node))
                            .setExpression(scope)
                            .build();
                } else {
                    FieldInfo fieldInfo = typeData.getField(vs);
                    assert fieldInfo != null : "Cannot find field " + node;
                    FieldReference fr = runtime.newFieldReference(fieldInfo, scope, concreteType);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(sourceForNode(node))
                            .setVariable(fr).build();
                }
                return null;
            }
        }
        super.visitMemberSelect(node, unused);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) node;

        ExpressionTree methodSelect = node.getMethodSelect();
        Expression object;
        String methodName;
        boolean objectIsImplicit;
        boolean explicitConstructorInvocation;
        ParameterizedType concreteReturnType;
        MethodInfo methodInfo;

        if (methodSelect instanceof IdentifierTree it) {
            TypeInfo currentType = typeStack.getLast(); // FIXME temp value, can also be static
            methodName = it.getName().toString();
            if ("super".equals(methodName) || "this".equals(methodName)) {
                explicitConstructorInvocation = true;
                object = null;
                concreteReturnType = runtime.parameterizedTypeReturnTypeOfConstructor();
                methodInfo = null;
            } else {
                object = runtime.newVariableExpressionBuilder()
                        .setVariable(runtime.newThis(currentType.asParameterizedType()))
                        .setSource(runtime.noSource()).build();
                concreteReturnType = convertType.convert(methodInvocation.type);
                explicitConstructorInvocation = false;
                if (it instanceof JCTree.JCIdent jcIdent && jcIdent.sym instanceof Symbol.MethodSymbol methodSymbol) {
                    methodInfo = typeData.getMethod(methodSymbol);
                    if (methodInfo == null) {
                        throw new UnsupportedOperationException("Cannot find method " + methodSymbol);
                    }
                } else throw new UnsupportedOperationException("NYI");
            }
            objectIsImplicit = true;
        } else if (methodSelect instanceof MemberSelectTree mst) {
            scan(mst.getExpression(), p);
            object = currentExpression;
            methodName = mst.getIdentifier().toString();
            objectIsImplicit = false;
            concreteReturnType = convertType.convert(methodInvocation.type);
            explicitConstructorInvocation = false;
            if (methodInvocation.meth instanceof JCTree.JCFieldAccess fieldAccess
                && fieldAccess.sym instanceof Symbol.MethodSymbol methodSymbol) {
                methodInfo = typeData.getMethod(methodSymbol);
                if (methodInfo == null) {
                    throw new UnsupportedOperationException("Cannot find method " + fieldAccess);
                }
            } else throw new UnsupportedOperationException("NYI");
        } else throw new UnsupportedOperationException("NYI");

        LOGGER.info("Method call to {}", methodName);

        List<Expression> arguments = new ArrayList<>(node.getArguments().size());
        for (var arg : node.getArguments()) {
            currentExpression = null;
            scan(arg, p);
            arguments.add(currentExpression);
        }

        if (explicitConstructorInvocation) {
            Statement statement = runtime.newExplicitConstructorInvocationBuilder()
                    .setIsSuper("super".equals(methodName))
                    .setMethodInfo(runtime.assignAndOperatorBool())
                    .setParameterExpressions(arguments)
                    .build();
            addStatement(statement);
            currentExpression = null; // as a marker for ExpressionAsStatement
        } else {
            currentExpression = runtime.newMethodCallBuilder()
                    .setSource(sourceForNode(node))
                    .setObjectIsImplicit(objectIsImplicit)
                    .setObject(object == null ? runtime.newEmptyExpression() : object)
                    .setMethodInfo(methodInfo)
                    .setParameterExpressions(arguments)
                    .setConcreteReturnType(concreteReturnType)
                    .build();
        }
        return null;
    }

    // new Node[3]
    // { ... }
    @Override
    public Void visitNewArray(NewArrayTree node, Void unused) {
        JCTree.JCNewArray newArray = (JCTree.JCNewArray) node;

        if (newArray.getInitializers() != null) {
            List<Expression> expressions = new ArrayList<>(newArray.getInitializers().size());
            for (JCTree.JCExpression e : newArray.getInitializers()) {
                scan(e, unused);
                expressions.add(currentExpression);
            }
            currentExpression = runtime.newArrayInitializerBuilder()
                    .setSource(sourceForNode(node))
                    .setCommonType(convertType.convert(newArray.type))
                    .setExpressions(expressions)
                    .build();
            return null;
        }
        List<Expression> dimensions = new ArrayList<>();
        for (var dim : newArray.dims) {
            scan(dim, unused);
            Expression dimension = currentExpression;
            dimensions.add(dimension);
        }
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType elementType = convertType.convertTree(newArray.elemtype, dsb);
        ParameterizedType concreteReturnType = elementType.copyWithArrays(dimensions.size());
        MethodInfo constructor = runtime.newArrayCreationConstructor(concreteReturnType);
        currentExpression = runtime.newConstructorCallBuilder()
                .setSource(sourceForNode(node, dsb))
                .setConstructor(constructor)
                .setConcreteReturnType(concreteReturnType)
                .setDiamond(runtime.diamondNo())
                .setParameterExpressions(dimensions)
                .build();
        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void unused) {
        JCTree.JCNewClass newClass = (JCTree.JCNewClass) node;
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        List<Expression> arguments = new ArrayList<>(node.getArguments().size());
        for (var arg : node.getArguments()) {
            currentExpression = null;
            scan(arg, unused);
            arguments.add(currentExpression);
        }
        TypeInfo anonymousType;
        ParameterizedType concreteReturnType;
        MethodInfo constructor;
        if (newClass.def != null) {
            JCTree.JCClassDecl anonBody = newClass.def;
            if (!anonBody.implementing.isEmpty()) {
                concreteReturnType = convertType.convertTree(anonBody.implementing.getFirst(), dsb);
                constructor = null;
                TypeInfo enclosingType = typeStack.getLast();
                anonymousType = runtime.newAnonymousType(enclosingType, enclosingType.builder().getAndIncrementAnonymousTypes());
                TypeInfo.Builder builder = anonymousType.builder()
                        .setTypeNature(runtime.typeNatureClass())
                        .setAccess(runtime.accessPrivate())
                        .setParentClass(runtime.objectParameterizedType())
                        .setEnclosingMethod(currentMethod);
                MethodInfo enclosingMethod = currentMethod;

                // The anonymous class's own members — fields, methods, etc.
                typeStack.addLast(anonymousType);
                for (JCTree member : anonBody.defs) {
                    currentMethod = null;
                    scan(member, unused);
                }
                typeStack.removeLast();
                currentMethod = enclosingMethod;

                builder.setSource(sourceForNode(node)).commit();
            } else throw new UnsupportedOperationException();
        } else {
            concreteReturnType = convertType.convert(newClass.type);
            anonymousType = null;
            constructor = typeData.getMethod((Symbol.MethodSymbol) newClass.constructor);
        }
        currentExpression = runtime.newConstructorCallBuilder()
                .setSource(sourceForNode(node, dsb))
                .setConstructor(constructor)
                .setDiamond(runtime.diamondNo()) // TODO
                .setConcreteReturnType(concreteReturnType)
                .setAnonymousClass(anonymousType)
                .setParameterExpressions(arguments)
                .build();
        return null;
    }

    @Override
    public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
        JCTree.JCSwitchExpression se = (JCTree.JCSwitchExpression) node;
        currentExpression = null;
        scan(se.getExpression(), unused);
        Expression selector = currentExpression;
        List<SwitchEntry> switchEntries = new ArrayList<>();
        int i = 0;
        int n = se.cases.size();
        for (JCTree.JCCase jcCase : se.getCases()) {
            List<Expression> conditions = new ArrayList<>();
            for (JCTree.JCCaseLabel caseLabel : jcCase.getLabels()) {
                LOGGER.debug("Case label {}", caseLabel);
                if (caseLabel instanceof JCTree.JCConstantCaseLabel ccl) {
                    scan(ccl.getConstantExpression(), unused);
                    Expression constantExpression = currentExpression;
                    conditions.add(constantExpression);
                } else if (caseLabel instanceof JCTree.JCDefaultCaseLabel) {
                    conditions.add(runtime.newEmptyExpression());
                } else {
                    throw new UnsupportedOperationException("NYI");
                }
            }
            Statement statement;
            if (jcCase.getBody() instanceof JCTree.JCBlock block) {
                statement = parseBlock(StringUtil.pad(i, n), block);
            } else if (jcCase.getBody() instanceof JCTree.JCExpression e) {
                scan(e, unused);
                Expression expression = currentExpression;
                statement = runtime.newExpressionAsStatementBuilder()
                        .setSource(sourceForNode(e))
                        .setExpression(expression).build();
            } else throw new UnsupportedOperationException("NYI");
            Expression whenExpression;
            if (jcCase.getGuard() != null) {
                scan(jcCase.getGuard(), unused);
                whenExpression = currentExpression;
            } else {
                whenExpression = runtime.newEmptyExpression();
            }
            SwitchEntry switchEntry = runtime.newSwitchEntryBuilder()
                    .addConditions(conditions)
                    .setStatement(statement)
                    .setWhenExpression(whenExpression)
                    .build();
            switchEntries.add(switchEntry);
            ++i;
        }
        currentExpression = runtime.newSwitchExpressionBuilder()
                .setSelector(selector)
                .setSource(sourceForNode(node))
                .setParameterizedType(convertType.convert(se.type))
                .addSwitchEntries(switchEntries)
                .build();
        return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, Void unused) {
        JCTree.JCUnary unary = (JCTree.JCUnary) node;
        scan(unary.getExpression(), unused);
        Expression expression = currentExpression;
        JCTree.Tag opcode = unary.getTag();
        MethodInfo operator;
        boolean assign;
        switch (opcode) {
            case BITXOR -> {
                operator = runtime.bitWiseNotOperatorInt();
                assign = false;
            }
            case NOT -> {
                operator = runtime.logicalNotOperatorBool();
                assign = false;
            }
            case NEG -> {
                operator = runtime.unaryMinusOperatorInt();
                assign = false;
            }
            case POSTINC, PREINC -> {
                assign = true;
                operator = runtime.assignPlusOperatorInt();
            }
            case POSTDEC, PREDEC -> {
                operator = runtime.assignMinusOperatorInt();
                assign = true;
            }
            default -> throw new UnsupportedOperationException();
        }
        if (assign) {
            boolean isPlus = opcode == JCTree.Tag.PREINC || opcode == JCTree.Tag.POSTINC;
            boolean isPrefix = opcode == JCTree.Tag.PREINC || opcode == JCTree.Tag.PREDEC;
            currentExpression = runtime.newAssignmentBuilder().setAssignmentOperator(operator)
                    .setPrefixPrimitiveOperator(isPrefix)
                    .setAssignmentOperatorIsPlus(isPlus)
                    .setBinaryOperator(isPlus ? runtime.plusOperatorInt() : runtime.minusOperatorInt())
                    .setTarget((VariableExpression) expression)
                    .setValue(runtime.intOne(runtime.noSource()))
                    .setSource(sourceForNode(node))
                    .build();
        } else {
            Precedence precedence = runtime.precedenceUnary();
            currentExpression = runtime.newUnaryOperator(List.of(), sourceForNode(node), operator, expression, precedence);
        }
        return null;
    }

    // -- HELPERS ------------------

    private Element findInElementStack(String name) {
        for (Map<String, Element> map : elementStack.reversed()) {
            Element v = map.get(name);
            if (v != null) return v;
        }
        throw new UnsupportedOperationException("Cannot find element '" + name + "' on stack");
    }

    private Source sourceForNode(Tree node, DetailedSources.Builder dsb) {
        return sourceForNode(node).withDetailedSources(dsb.build());
    }

    @Override
    public Source sourceForNode(Tree node) {
        return sourceForNode(node, "-");
    }

    private Source statementSourceForNode(Tree node) {
        return sourceForNode(node, statementIndex());
    }

    private Source statementSourceForNode(Tree node, DetailedSources.Builder dsb) {
        return sourceForNode(node, statementIndex()).withDetailedSources(dsb.build());
    }

    private Source sourceForNode(Tree node, String index) {
        long endPos = sourcePositions.getEndPosition(compilationUnitTree, node);
        if (endPos == Diagnostic.NOPOS) return runtime.noSource(); // synthetic
        long startPos = sourcePositions.getStartPosition(compilationUnitTree, node);
        long startLine = lineMap.getLineNumber(startPos);
        long startCol = lineMap.getColumnNumber(startPos);
        long endLine = lineMap.getLineNumber(endPos);
        long endCol = lineMap.getColumnNumber(endPos) - 1; // we work inclusively
        return runtime.newParserSource(index, (int) startLine, (int) startCol, (int) endLine, (int) endCol);
    }

    private List<Comment> commentsForNode(Source source) {
        return scanResult.findComments(source);
    }

    private List<Comment> trailingCommentsForNode(Source source) {
        return scanResult.findTrailingComments(source);
    }
}
