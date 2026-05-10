package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;

class AnalysisScanner extends TreePathScanner<Void, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisScanner.class);
    private final Deque<TypeInfo> typeStack = new ArrayDeque<>();
    private final Deque<Map<String, Element>> elementStack = new ArrayDeque<>();

    private final Runtime runtime;
    private final List<TypeInfo> collectedPrimaryTypes = new ArrayList<>();
    private final Trees trees;
    private MethodInfo currentMethod;
    private Block.Builder currentBlockBuilder;
    private Expression currentExpression;
    private final CompilationUnit compilationUnit;
    private final SourcePositions sourcePositions;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;
    private final FlagHelper flagHelper;
    private final ConvertType convertType;
    private final TypeData typeData;
    private final Elements elements;

    AnalysisScanner(Runtime runtime, CompilationUnit compilationUnit,
                    CompilationUnitTree compilationUnitTree,
                    Trees trees, SourcePositions sourcePositions,
                    LineMap lineMap,
                    Elements elements) {
        this.runtime = runtime;
        this.compilationUnit = compilationUnit;
        this.trees = trees;
        this.lineMap = lineMap;
        this.sourcePositions = sourcePositions;
        this.compilationUnitTree = compilationUnitTree;
        this.elements = elements;

        typeData = new TypeData();
        flagHelper = new FlagHelper(runtime);
        ClassSymbolScanner classSymbolScanner = new ClassSymbolScanner(runtime, flagHelper, elements, typeData);
        convertType = new ConvertType(runtime, classSymbolScanner, typeData, this::findInElementStack);
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
        flagHelper.type(jcClassDecl.getModifiers().flags, typeInfo.builder());

        // type parameters
        for (JCTree.JCTypeParameter jcTypeParameter : jcClassDecl.getTypeParameters()) {
            int index = jcTypeParameter.pos;
            String name = jcTypeParameter.getName().toString();
            TypeParameter tp = runtime.newTypeParameter(index, name, typeInfo);
            typeInfo.builder().addOrSetTypeParameter(tp);
            elementStack.getLast().put(name, tp);
        }

        // members: methods, fields
        for (var member : node.getMembers()) {
            currentMethod = null;
            scan(member, p);
        }

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
        if ("<init>".equals(methodName)) {
            methodInfo = runtime.newConstructor(currentType);
            currentType.builder().addConstructor(methodInfo);
            methodInfo.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());

        } else {
            MethodInfo.MethodType methodType = flagHelper.methodType(methodFlags);
            methodInfo = runtime.newMethod(currentType, methodName, methodType);
            currentType.builder().addMethod(methodInfo);

            ParameterizedType returnType = convertType.convertTree(node.getReturnType());
            methodInfo.builder().setReturnType(returnType);
        }
        typeData.put(jcMethod.sym, methodInfo);

        // flags
        flagHelper.method(methodFlags, methodInfo.builder());

        // parameters
        for (JCTree.JCVariableDecl jcVariableDecl : jcMethod.getParameters()) {
            String name = jcVariableDecl.getName().toString();
            ParameterizedType type = convertType.convertTree(jcVariableDecl.getType());
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
            parameterInfo.builder().commit();
        }


        // method body
        currentBlockBuilder = runtime.newBlockBuilder();
        elementStack.addLast(new HashMap<>());
        currentMethod = methodInfo;
        scan(node.getBody(), p);
        elementStack.removeLast();
        currentMethod = null;

        methodInfo.builder()
                .setSource(sourceForNode(node))
                .addComments(commentsForNode(node))
                .setMethodBody(currentBlockBuilder.build())
                .computeAccess()
                .commit();
        return null;
    }

    // -- Annotations ---------------------------------------------

    private AnnotationExpression convertAnnotation(JCTree.JCAnnotation annotation) {
        ParameterizedType at = convertType.convertTree(annotation.getAnnotationType());
        return runtime.newAnnotationExpressionBuilder().setTypeInfo(at.typeInfo()).build();
    }

    // -- Statements ---------------------------------------------

    @Override
    public Void visitBlock(BlockTree node, Void unused) {
        elementStack.push(new HashMap<>());
        super.visitBlock(node, unused);
        elementStack.pop();
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
        LOGGER.info("Expression statement");
        super.visitExpressionStatement(node, unused);
        if (currentExpression != null) {
            ExpressionAsStatement statement = runtime.newExpressionAsStatementBuilder()
                    .setSource(sourceForNode(node))
                    .addComments(commentsForNode(node))
                    .setExpression(currentExpression)
                    .build();
            currentBlockBuilder.addStatement(statement);
        } // else: was explicit constructor invocation, a statement
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
        currentExpression = null;
        scan(node.getExpression(), unused);
        currentBlockBuilder.addStatement(runtime.newReturnBuilder()
                .setSource(sourceForNode(node))
                .addComments(commentsForNode(node))
                .setExpression(currentExpression == null ? runtime.newEmptyExpression() : currentExpression)
                .build());
        return null;
    }

    // note: also field declarations
    @Override
    public Void visitVariable(VariableTree node, Void p) {
        LOGGER.info("VARIABLE:" + node.getName().toString());

        if (node instanceof JCTree.JCVariableDecl variableDecl) {
            long flags = variableDecl.getModifiers().flags;
            boolean isStatic = (flags & Flags.STATIC) != 0;
            boolean isFinal = (flags & Flags.FINAL) != 0;

            if (variableDecl.sym instanceof Symbol.VarSymbol varSymbol) {
                String name = varSymbol.toString();
                ParameterizedType type = convertType.convertTree(variableDecl.vartype);

                currentExpression = null;
                scan(node.getInitializer(), p);
                if (currentExpression == null) {
                    currentExpression = runtime.newEmptyExpression();
                }
                if (currentMethod == null) {
                    // field!
                    TypeInfo owner = typeStack.getLast();
                    FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
                    if (isFinal) fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal());
                    fieldInfo.builder().setSource(sourceForNode(node))
                            .setInitializer(currentExpression)
                            .commit();
                    owner.builder().addField(fieldInfo);
                } else {
                    // local variable

                    LocalVariable localVariable = runtime.newLocalVariable(name, type, currentExpression);
                    LocalVariableCreation.Builder lvcb = runtime.newLocalVariableCreationBuilder()
                            .setSource(sourceForNode(node))
                            .setLocalVariable(localVariable);
                    if (isFinal) lvcb.addModifier(runtime.localVariableModifierFinal());
                    currentBlockBuilder.addStatement(lvcb.build());
                    elementStack.getLast().put(localVariable.simpleName(), localVariable);
                }
            }
        }
        return null;
    }

    // -- Expressions ---------------------------------------------

    @Override
    public Void visitBinary(BinaryTree node, Void unused) {
        JCTree.JCBinary binary = (JCTree.JCBinary) node;
        scan(node.getLeftOperand(), unused);
        Expression lhs = currentExpression;
        scan(node.getRightOperand(), unused);
        Expression rhs = currentExpression;
        JCTree.Tag opcode = binary.getTag();
        MethodInfo operator = switch (opcode) {
            case PLUS -> runtime.plusOperatorInt();
            case MINUS -> runtime.minusOperatorInt();
            case MUL -> runtime.multiplyOperatorInt();
            case DIV -> runtime.divideOperatorInt();
            case EQ -> runtime.equalsOperatorInt();
            default -> throw new UnsupportedOperationException("NYI");
        };
        Precedence precedence = switch (opcode) {
            case PLUS, MINUS -> runtime.precedenceAdditive();
            case MUL, DIV -> runtime.precedenceMultiplicative();
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


    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void unused) {

        if (node instanceof JCTree.JCFieldAccess fieldAccess) {
            // class literal
            if ("class".equals(fieldAccess.name.toString())) {
                if (fieldAccess.selected instanceof JCTree.JCIdent ident) {
                    ParameterizedType classType = convertType.convert(fieldAccess.type);
                    ParameterizedType realType = convertType.convert(ident.type);
                    currentExpression = runtime.newClassExpressionBuilder(realType)
                            .setSource(sourceForNode(node))
                            .setClassType(classType).build();
                    return null;
                } else throw new UnsupportedOperationException();
            }

            // static field access, no need to generate a TypeExpression
            if (fieldAccess.sym instanceof Symbol.VarSymbol vs) {
                scan(fieldAccess.getExpression(), unused);
                Expression scope = currentExpression;
                ParameterizedType concreteType = convertType.convert(fieldAccess.type);
                FieldInfo fieldInfo = typeData.getField(vs);
                assert fieldInfo != null : "Cannot find field " + node;
                FieldReference fr = runtime.newFieldReference(fieldInfo, scope, concreteType);
                currentExpression = runtime.newVariableExpressionBuilder()
                        .setSource(sourceForNode(node))
                        .setVariable(fr).build();
                return null;
            }
        }
        super.visitMemberSelect(node, unused);
        return null;
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
                        FieldInfo fieldInfo = typeInfoOwner.getFieldByName(name, true);
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(sourceForNode(node))
                                .setVariable(runtime.newFieldReference(fieldInfo))
                                .build();
                    } else throw new UnsupportedOperationException();
                }
                case LOCAL_VARIABLE -> {
                    Variable variable = (LocalVariable) findInElementStack(name);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(sourceForNode(node))
                            .setVariable(variable)
                            .build();
                }
                // case PARAMETER, ENUM_CONSTANT -> {

                // }
                case PACKAGE -> {
                    LOGGER.debug("Skipping package {}", node);
                }
                case CLASS -> {
                    if (element instanceof Symbol.ClassSymbol classSymbol) {
                        ParameterizedType type = convertType.convert(classSymbol.type);
                        currentExpression = runtime.newTypeExpressionBuilder()
                                .setDiamond(runtime.diamondNo()) // TODO
                                .setParameterizedType(type)
                                .build();
                    } else throw new UnsupportedOperationException("NYI");
                }
                default -> throw new UnsupportedOperationException("NYI: " + element.getKind());
            }
        }
        return null;// super.visitIdentifier(node, p);
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
            case BOOLEAN -> runtime.newBoolean(comments, source, (Boolean) literal.value);
            case CHAR -> runtime.newChar(comments, source, (Character) literal.value);
            case CLASS -> {
                Tree.Kind kind = literal.typetag.getKindLiteral();
                if (Tree.Kind.STRING_LITERAL == kind) {
                    yield runtime.newStringConstant(comments, source, (String) literal.value);
                }
                throw new UnsupportedOperationException("?");
            }
            default -> throw new UnsupportedOperationException();
        };

        return super.visitLiteral(node, unused);
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
            if (currentExpression == null) {
                LOGGER.warn("Have unparsed expression");
                currentExpression = runtime.newEmptyExpression();
            }
            arguments.add(currentExpression);
        }

        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            LOGGER.info("  resolves to:" + element.toString());
            LOGGER.info("  declared in:" + element.getEnclosingElement().toString());
        }

        if (explicitConstructorInvocation) {
            Statement statement = runtime.newExplicitConstructorInvocationBuilder()
                    .setIsSuper("super".equals(methodName))
                    .setMethodInfo(runtime.assignAndOperatorBool())
                    .setParameterExpressions(arguments)
                    .build();
            currentBlockBuilder.addStatement(statement);
            currentExpression = null; // as a marker for ExpressionAsStatement
        } else {
            assert methodInfo != null;
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

    @Override
    public Void visitUnary(UnaryTree node, Void unused) {
        JCTree.JCUnary unary = (JCTree.JCUnary) node;
        scan(unary.getExpression(), unused);
        Expression expression = currentExpression;
        JCTree.Tag opcode = unary.getTag();
        MethodInfo operator = switch (opcode) {
            case BITXOR -> runtime.bitWiseNotOperatorInt();
            case NEG -> runtime.logicalNotOperatorBool();
            default -> throw new UnsupportedOperationException();
        };
        Precedence precedence = runtime.precedenceUnary();
        currentExpression = runtime.newUnaryOperator(List.of(), sourceForNode(node), operator, expression, precedence);
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

    private Source sourceForNode(Tree node) {
        long endPos = sourcePositions.getEndPosition(compilationUnitTree, node);
        if (endPos == Diagnostic.NOPOS) return runtime.noSource(); // synthetic
        long startPos = sourcePositions.getStartPosition(compilationUnitTree, node);
        long startLine = lineMap.getLineNumber(startPos);
        long startCol = lineMap.getColumnNumber(startPos);
        long endLine = lineMap.getLineNumber(endPos);
        long endCol = lineMap.getColumnNumber(endPos) - 1; // we work inclusively
        return runtime.newParserSource("-", (int) startLine, (int) startCol, (int) endLine, (int) endCol);
    }

    private List<Comment> commentsForNode(Tree node) {
        // TODO, using CongoCC data
        return List.of();
    }
}
