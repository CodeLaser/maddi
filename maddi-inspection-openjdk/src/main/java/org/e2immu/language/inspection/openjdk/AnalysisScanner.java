package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.net.URI;
import java.util.*;

class AnalysisScanner extends TreePathScanner<Void, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisScanner.class);
    private final Deque<TypeInfo> typeStack = new ArrayDeque<>();

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
    private final Elements elements;
    private final FlagHelper flagHelper;

    private final Map<String, TypeInfo> typeTable = new HashMap<>();

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
        this.flagHelper = new FlagHelper(runtime);
    }

    // -- Indentation helper ----------------------------------------------

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
        typeTable.put(typeInfo.fullyQualifiedName(), typeInfo);
        flagHelper.type(jcClassDecl, typeInfo.builder());

        for (var member : node.getMembers()) {
            currentMethod = null;
            scan(member, p);
        }

        typeStack.removeLast();
        return null;
    }

    // -- Method declarations ---------------------------------------------

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        JCTree.JCMethodDecl jcMethod = (JCTree.JCMethodDecl) node;
        String methodName = node.getName().toString();

        TypeInfo currentType = typeStack.getLast();
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

        flagHelper.method(jcMethod, currentMethod.builder());

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
        if (type instanceof JCTree.JCIdent identifier) {
            if (identifier.type instanceof Type.ClassType ct) {
                String fullyQualifiedType = ct.toString();
                TypeInfo typeInfo = typeTable.get(fullyQualifiedType);
                if (typeInfo == null) {
                    if (ct.tsym instanceof Symbol.ClassSymbol cs) {
                        String packageName = cs.owner.toString();
                        TypeInfo primaryType = typeStack.getFirst(); // FIXME can we see which jar?
                        SourceSet sourceSet = primaryType.compilationUnit().sourceSet();
                        URI uri = cs.classfile.toUri();
                        CompilationUnit cu = runtime.newCompilationUnitBuilder()
                                .setPackageName(packageName)
                                .setSourceSet(sourceSet)
                                .setURI(uri)
                                .build();
                        TypeInfo newTypeInfo = runtime.newTypeInfo(cu, identifier.toString());
                        typeTable.put(newTypeInfo.fullyQualifiedName(), newTypeInfo);
                        //The following completely loads 'cs'
                        List<? extends Element> members = elements.getAllMembers(cs);
                        for (var member : members) {
                            scanByteCode(newTypeInfo, member);
                        }
                        return newTypeInfo.asParameterizedType();
                    }
                }
            }
        }
        throw new UnsupportedOperationException("NYI");
    }

    private void scanByteCode(TypeInfo typeInfo, Element member) {
        LOGGER.info("Adding members to {}", typeInfo);
        if (member instanceof Symbol.MethodSymbol ms) {
            String name = ms.getSimpleName().toString();
            MethodInfo method = runtime.newMethod(typeInfo, name, runtime.methodTypeMethod());
            typeInfo.builder().addMethod(method);
        }
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
        LOGGER.info("Expression statement");
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
        LOGGER.info("VARIABLE:" + node.getName().toString());

        if (node instanceof JCTree.JCVariableDecl variableDecl) {
            if (currentMethod == null) {
                // field!
                long flags = variableDecl.getModifiers().flags;
                boolean isStatic = (flags & Flags.STATIC) != 0;
                boolean isFinal = (flags & Flags.FINAL) != 0;
                if (variableDecl.sym instanceof Symbol.VarSymbol varSymbol) {
                    String name = varSymbol.toString();
                    ParameterizedType type = convertType(variableDecl.vartype);
                    TypeInfo owner = typeStack.getLast();
                    FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
                    if (isFinal) fieldInfo.builder().addFieldModifier(runtime.fieldModifierFinal());
                    fieldInfo.builder().setSource(sourceForNode(node))
                            .setInitializer(runtime.newEmptyExpression())
                            .commit();
                    owner.builder().addField(fieldInfo);
                }
            }
        }
        return null;
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
        ExpressionTree methodSelect = node.getMethodSelect();
        String callSite = methodSelect.toString();
        Expression object;
        String methodName;
        if (methodSelect instanceof IdentifierTree it) {
            TypeInfo currentType = typeStack.getLast(); // FIXME temp value, can also be static
            object = runtime.newVariableExpressionBuilder()
                    .setVariable(runtime.newThis(currentType.asParameterizedType()))
                    .setSource(runtime.noSource()).build();
            methodName = it.getName().toString();
        } else if (methodSelect instanceof MemberSelectTree mst) {
            scan(mst.getExpression(), p);
            object = currentExpression;
            methodName = mst.getIdentifier().toString();
        } else throw new UnsupportedOperationException("?");
        LOGGER.info("Method call to {}", methodName);

        List<Expression> arguments = new ArrayList<>(node.getArguments().size());
        for (var arg : node.getArguments()) {
            scan(arg, p);
            arguments.add(currentExpression);
        }

        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            LOGGER.info("  resolves to:" + element.toString());
            LOGGER.info("  declared in:" + element.getEnclosingElement().toString());
        }

        // getTypeMirror on a method invocation gives you the *return type*
        // of the call expression as seen by the type checker.
        var returnType = trees.getTypeMirror(getCurrentPath());
        if (returnType != null) {
            LOGGER.info("  return type:" + returnType.toString());
        }
        super.visitMethodInvocation(node, p);

        currentExpression = runtime.newMethodCallBuilder()
                .setSource(sourceForNode(node))
                .setObjectIsImplicit(methodSelect instanceof IdentifierTree)
                .setObject(object == null ? runtime.newEmptyExpression() : object)
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
            String name = node.getName().toString();
            switch (element.getKind()) {
                case FIELD -> {
                    LOGGER.info("Field {}", name);
                    if (element instanceof Symbol.VarSymbol vs) {
                        String owner = vs.owner.toString();
                        TypeInfo typeInfoOwner = typeTable.get(owner);
                        FieldInfo fieldInfo = typeInfoOwner.getFieldByName(name, true);
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(sourceForNode(node))
                                .setVariable(runtime.newFieldReference(fieldInfo))
                                .build();
                    }
                }
                case LOCAL_VARIABLE, PARAMETER, ENUM_CONSTANT -> {
                    LOGGER.info("variable identifier:" + node.getName().toString());
                    LOGGER.info("  kind:" + element.getKind().toString());
                    var type = trees.getTypeMirror(getCurrentPath());
                    if (type != null) LOGGER.info("  type:" + type.toString());
                }

            }
        }
        return null;// super.visitIdentifier(node, p);
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
