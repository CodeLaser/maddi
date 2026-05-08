package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeModifier;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;

import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class AnalysisScanner extends TreePathScanner<Void, Void> {
    private final Runtime runtime;
    private final List<TypeInfo> collectedTypes = new ArrayList<>();
    private final Trees trees;
    private TypeInfo currentType;
    private MethodInfo currentMethod;
    private Block.Builder currentBlockBuilder;
    private Expression currentExpression;
    private final CompilationUnit compilationUnit;
    private final SourcePositions sourcePositions;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;

    AnalysisScanner(Runtime runtime, CompilationUnit compilationUnit,
                    CompilationUnitTree compilationUnitTree,
                    Trees trees, SourcePositions sourcePositions,
                    LineMap lineMap) {
        this.runtime = runtime;
        this.compilationUnit = compilationUnit;
        this.trees = trees;
        this.lineMap = lineMap;
        this.sourcePositions = sourcePositions;
        this.compilationUnitTree = compilationUnitTree;
    }

    // -- Indentation helper ----------------------------------------------

    public Collection<TypeInfo> types() {
        return collectedTypes;
    }

    // -- Class declarations ----------------------------------------------

    @Override
    public Void visitClass(ClassTree node, Void p) {
        currentType = runtime.newTypeInfo(compilationUnit, node.getSimpleName().toString());
        collectedTypes.add(currentType);

        TypeNature typeNature = runtime.typeNatureClass();
        node.getModifiers().getFlags().forEach(modifier -> {
            TypeModifier tm = switch (modifier.name()) {
                case "PUBLIC" -> runtime.typeModifierPublic();
                case "PROTECTED" -> runtime.typeModifierProtected();
                default -> throw new UnsupportedOperationException("NYI");
            };
            currentType.builder().addTypeModifier(tm);
        });

        currentType.builder().setTypeNature(typeNature);

        return super.visitClass(node, p);   // visit children
    }

    // -- Method declarations ---------------------------------------------

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        JCTree.JCMethodDecl jcMethod = (JCTree.JCMethodDecl) node;
        String methodName = node.getName().toString();

        if ("<init>".equals(methodName)) {
            currentMethod = runtime.newConstructor(currentType);
            currentType.builder().addConstructor(currentMethod);
            currentMethod.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());

        } else {
            currentMethod = runtime.newMethod(currentType, methodName, runtime.methodTypeMethod());
            currentType.builder().addMethod(currentMethod);

            ParameterizedType returnType = convertType(node.getReturnType());
            currentMethod.builder().setReturnType(returnType);
        }

        // The cleaner check: flags directly encode synthetic/bridge/etc.
        long flags = jcMethod.getModifiers().flags;
        boolean isSynthetic = (flags & Flags.SYNTHETIC) != 0;
        boolean isBridge = (flags & Flags.BRIDGE) != 0;
        boolean isGeneratedConstructor = (flags & Flags.GENERATEDCONSTR) != 0;
        boolean isPublic = (flags & Flags.PUBLIC) != 0;
        boolean isPrivate = (flags & Flags.PRIVATE) != 0;
        boolean isProtected = (flags & Flags.PROTECTED) != 0;
        if (isPublic) {
            currentMethod.builder().addMethodModifier(runtime.methodModifierPublic());
        }
        if (isPrivate) {
            currentMethod.builder().addMethodModifier(runtime.methodModifierPrivate());
        }
        if (isProtected) {
            currentMethod.builder().addMethodModifier(runtime.methodModifierProtected());
        }
        if (isSynthetic || isGeneratedConstructor) {
            currentMethod.builder().setSynthetic(true);
        }

        // getElement() gives you the javax.lang.model.element.Element for
        // this declaration — here an ExecutableElement for the method.
        // You can cast it to query parameter types, return type, throws, etc.
        var element = trees.getElement(getCurrentPath());

        currentBlockBuilder = runtime.newBlockBuilder();
        super.visitMethod(node, p);

        currentMethod.builder()
                .setSource(sourceForNode(node))
                .addComments(commentsForNode(node))
                .setMethodBody(currentBlockBuilder.build())
                .computeAccess()
                .commit();
        return null;
    }

    private ParameterizedType convertType(Tree type) {
        if (type == null) return runtime.voidParameterizedType();
        if (type instanceof JCTree.JCPrimitiveTypeTree ptt) {
            TypeKind primitiveTypeKind = ptt.typetag.getPrimitiveTypeKind();
            if (primitiveTypeKind != null) {
                return switch (primitiveTypeKind) {
                    case VOID -> runtime.voidParameterizedType();
                    case INT -> runtime.intParameterizedType();
                    case DOUBLE -> runtime.doubleParameterizedType();
                    case LONG -> runtime.longParameterizedType();
                    case FLOAT -> runtime.floatParameterizedType();
                    case SHORT -> runtime.shortParameterizedType();
                    case BOOLEAN -> runtime.booleanParameterizedType();
                    case CHAR -> runtime.charParameterizedType();
                    default -> throw new UnsupportedOperationException();
                };
            }
        }
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
        super.visitReturn(node, unused);
        currentBlockBuilder.addStatement(runtime.newReturnBuilder()
                .setSource(sourceForNode(node))
                .addComments(commentsForNode(node))
                .setExpression(currentExpression)
                .build());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
        System.out.println("Expression statement");
        super.visitExpressionStatement(node, unused);
        assert currentExpression != null;
        ExpressionAsStatement statement = runtime.newExpressionAsStatementBuilder()
                .setSource(sourceForNode(node))
                .addComments(commentsForNode(node))
                .setExpression(currentExpression)
                .build();
        currentBlockBuilder.addStatement(statement);
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

    // -- Variable declarations (fields and locals) -----------------------

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        System.out.println("VARIABLE:" + node.getName().toString());

        var typeMirror = trees.getTypeMirror(getCurrentPath());
        if (typeMirror != null) {
            System.out.println("  type:" + typeMirror.toString());
        }
        return super.visitVariable(node, p);
    }

    // -- Method calls ----------------------------------------------------
    //
    // This is probably the most important node for code-quality analysis.
    // After attribution, getElement() on a method invocation path gives
    // you the ExecutableElement of the *resolved* method — including the
    // declaring class, parameter types, and return type — even through
    // overloading and generics.

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {


        // The method select is usually a MemberSelectTree ("obj.method")
        // or an IdentifierTree ("method"), both giving the method name.
        String callSite = node.getMethodSelect().toString();
        Expression object;
        boolean objectIsImplicit = node.getMethodSelect() instanceof JCTree.JCIdent;
        if (objectIsImplicit) {
            object = runtime.newVariableExpressionBuilder().setVariable(runtime.newThis(currentType.asParameterizedType()))
                    .setSource(runtime.noSource()).build();
        } else {
            scan(node.getMethodSelect(), p);
            object = currentExpression;
        }

        List<Expression> arguments = new ArrayList<>(node.getArguments().size());
        for (var arg : node.getArguments()) {
            scan(arg, p);
            arguments.add(currentExpression);
        }

        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            System.out.print("  resolves to:" + element.toString());
            System.out.print("  declared in:" + element.getEnclosingElement().toString());
        }

        // getTypeMirror on a method invocation gives you the *return type*
        // of the call expression as seen by the type checker.
        var returnType = trees.getTypeMirror(getCurrentPath());
        if (returnType != null) {
            System.out.print("  return type:" + returnType.toString());
        }
        super.visitMethodInvocation(node, p);

        currentExpression = runtime.newMethodCallBuilder()
                .setSource(sourceForNode(node))
                .setObjectIsImplicit(objectIsImplicit)
                .setObject(runtime.newEmptyExpression()) // object
                .setMethodInfo(runtime.assignAndOperatorBool())
                .setParameterExpressions(arguments)
                .setConcreteReturnType(runtime.intParameterizedType())
                .build();
        return null;
    }

    // -- Identifier references -------------------------------------------
    //
    // An identifier is any bare name: a local variable, a field, a class
    // name, a type parameter, etc.  getElement() tells you which one.

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            // Filter out TYPE and PACKAGE identifiers for brevity;
            // you probably want all of these in a real analyzer.
            switch (element.getKind()) {
                case LOCAL_VARIABLE, PARAMETER, FIELD, ENUM_CONSTANT -> {
                    System.out.print("IDENT:" + node.getName().toString());
                    System.out.print("  kind:" + element.getKind().toString());
                    var type = trees.getTypeMirror(getCurrentPath());
                    if (type != null) System.out.print("  type:" + type.toString());
                }
                default -> { /* skip type/package refs for brevity */ }
            }
        }
        return super.visitIdentifier(node, p);
    }

    // -- If you want to see EVERY node, uncomment this: ------------------

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

        return List.of();
    }
}
