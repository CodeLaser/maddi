package org.e2immu.language.java.openjdk;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.util.CreateSyntheticFieldsForGetSet;
import org.e2immu.language.inspection.api.util.RecordSynthetics;
import org.e2immu.util.internal.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

class ScanCompilationUnit extends TreePathScanner<Void, Void> implements SourceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCompilationUnit.class);

    private record BlockData(Block.Builder blockBuilder, String index, int numberOfStatements) {
    }

    private final Deque<TypeInfo> typeStack = new ArrayDeque<>();
    private final ElementStack elementStack = new ElementStack();
    private final Runtime runtime;
    private final List<TypeInfo> collectedPrimaryTypes = new ArrayList<>();
    private final List<ModuleInfo> collectedModules = new ArrayList<>();
    private final Trees trees;
    private MethodInfo currentMethod;
    private final Deque<BlockData> blockBuilders = new ArrayDeque<>();
    private Expression currentExpression;
    private final Map<StatementTree, String> statementLabels = new IdentityHashMap<>();
    private final CompilationUnit.Builder compilationUnitBuilder;
    private CompilationUnit compilationUnit;
    private final SourcePositions sourcePositions;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;
    private final FlagHelper flagHelper;
    private final ConvertType convertType;
    private final TypeData typeData;
    private final SourceCodeScan.Result scanResult;
    private final Types types;
    private final Elements elements;
    private final ComputeMethodOverrides computeMethodOverrides;
    private final CreateSyntheticFieldsForGetSet createSyntheticFieldsForGetSet;
    private final DocTrees docTrees;
    private final ScanJavaDoc scanJavaDoc;

    ScanCompilationUnit(Runtime runtime,
                        TypeData typeData,
                        CompilationUnit.Builder compilationUnitBuilder,
                        CompilationUnitTree compilationUnitTree,
                        Trees trees,
                        SourcePositions sourcePositions,
                        LineMap lineMap,
                        Elements elements,
                        Types types,
                        DocTrees docTrees,
                        SourceCodeScan.Result scanResult,
                        ComputeMethodOverrides computeMethodOverrides,
                        FlagHelper flagHelper,
                        ClassSymbolScanner classSymbolScanner) {
        this.runtime = runtime;
        this.typeData = typeData;
        this.compilationUnitBuilder = compilationUnitBuilder;
        this.trees = trees;
        this.lineMap = lineMap;
        this.sourcePositions = sourcePositions;
        this.compilationUnitTree = compilationUnitTree;
        this.scanResult = scanResult;
        this.types = types;
        this.elements = elements;
        this.flagHelper = flagHelper;
        this.computeMethodOverrides = computeMethodOverrides;
        this.docTrees = docTrees;
        this.createSyntheticFieldsForGetSet = new CreateSyntheticFieldsForGetSet(runtime);

        DocSourcePositions docSourcePositions = docTrees.getSourcePositions();
        this.scanJavaDoc = new ScanJavaDoc(runtime, typeData, docSourcePositions, compilationUnitTree, lineMap);

        convertType = classSymbolScanner;
        convertType.startCompilationUnit(this, elementStack);
    }

    // result
    public List<TypeInfo> types() {
        return collectedPrimaryTypes;
    }

    public List<ModuleInfo> modules() {
        return collectedModules;
    }

    // -- Class declarations ----------------------------------------------


    @Override
    public Void visitModule(ModuleTree node, Void unused) {
        boolean open = node.getModuleType() == ModuleTree.ModuleKind.OPEN;
        ModuleInfo.Builder builder = runtime.newModuleInfoBuilder();
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        String name = node.getName().toString();
        for (DirectiveTree d : node.getDirectives()) {
            visitDirective(d, builder);
        }
        ModuleInfo moduleInfo = builder
                .setOpen(open)
                .setCompilationUnit(compilationUnit)
                .setName(name)
                .setSource(sourceForNode(node, dsb))
                .build();
        collectedModules.add(moduleInfo);
        return null;
    }

    private void visitDirective(DirectiveTree dt, ModuleInfo.Builder builder) {
        Source source = scanSource(dt);
        List<Comment> comments = commentsForNode(source);
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        switch (dt) {
            case JCTree.JCRequires rd -> {
                String moduleName = rd.moduleName.toString();
                dsb.put(moduleName, scanResult.find(moduleName, scanSource(rd.getModuleName())));
                builder.addRequires(source.withDetailedSources(dsb.build()), comments,
                        moduleName, rd.isStatic(), rd.isTransitive());
            }
            case JCTree.JCExports ed -> builder.addExports(source, comments, ed.getPackageName().toString(),
                    ed.moduleNames == null ? null : ed.moduleNames.getFirst().toString());
            case JCTree.JCOpens od -> {
                String packageName = od.getPackageName().toString();
                dsb.put(packageName, scanResult.find(packageName, scanSource(od.getPackageName())));
                String moduleName = od.moduleNames == null ? null : od.moduleNames.getFirst().toString();
                if (moduleName != null) {
                    dsb.put(moduleName, scanResult.find(moduleName, scanSource(od.getModuleNames().getFirst())));
                }
                builder.addOpens(source.withDetailedSources(dsb.build()), comments, packageName, moduleName);
            }
            case JCTree.JCProvides p -> builder.addProvides(source, comments, p.getServiceName().toString(),
                    p.implNames == null ? null : p.implNames.getFirst().toString());
            case JCTree.JCUses u -> builder.addUses(source, comments, u.getServiceName().toString());
            case null, default -> throw new UnsupportedOperationException();
        }
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
        try {
            // continue the building of the compilation unit: imports
            for (ImportTree importTree : node.getImports()) {
                ImportStatement is = parseImportStatement(importTree);
                compilationUnitBuilder.addImportStatement(is);
            }
            Source source = sourceForNode(node);
            compilationUnitBuilder.setSource(source)
                    .addComments(commentsForNode(source))
                    .addTrailingComments(trailingCommentsForNode(source));
            compilationUnit = compilationUnitBuilder.build();

            // now we have one built
            if (node.getTypeDecls().isEmpty()) {
                // package-info
                List<AnnotationExpression> annotations = new ArrayList<>();
                for (AnnotationTree at : node.getPackageAnnotations()) {
                    annotations.add(convertAnnotation((JCTree.JCAnnotation) at));
                }
                TypeInfo pkgInfoType = runtime.newTypeInfo(compilationUnit, "package-info");
                pkgInfoType.builder().setTypeNature(runtime.typeNaturePackageInfo())
                        .addAnnotations(annotations)
                        .setParentClass(runtime.objectParameterizedType())
                        .setAccess(runtime.accessPublic())
                        .setSource(source);
                // don't commit yet
                collectedPrimaryTypes.add(pkgInfoType);
            } else {
                for (Tree ct : node.getTypeDecls()) {
                    scan(ct, null);
                }
            }
            if (node.getModule() != null) {
                scan(node.getModule(), null);
            }
            compilationUnit.setTypes(collectedPrimaryTypes);
            return null;
        } catch (RuntimeException | AssertionError | StackOverflowError re) {
            LOGGER.error("Caught exception in compilation unit {}", compilationUnit);
            throw re;
        }
    }

    private ImportStatement parseImportStatement(ImportTree importTree) {
        boolean isStatic = importTree.isStatic();
        String im = importTree.getQualifiedIdentifier().toString();
        Source source = sourceForNode(importTree);
        return runtime.newImportStatementBuilder()
                .setSource(source)
                .addComments(commentsForNode(source))
                .setImport(im)
                .setIsStatic(isStatic)
                .build();
    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        try {
            JCTree.JCClassDecl jcClassDecl = (JCTree.JCClassDecl) node;
            TypeInfo typeInfo;
            String fullyQualifiedName = jcClassDecl.sym.fullname.toString();
            TypeInfo known = typeData.getType(fullyQualifiedName);
            String simpleName = node.getSimpleName().toString();

            if (known != null) {
                typeInfo = known;
            } else {
                if (typeStack.isEmpty()) {
                    typeInfo = runtime.newTypeInfo(compilationUnit, simpleName);
                } else {
                    TypeInfo enclosed = typeStack.getLast();
                    typeInfo = runtime.newTypeInfo(enclosed, simpleName);
                    enclosed.builder().addSubType(typeInfo);
                }
                typeData.put(typeInfo);
            }
            if (typeInfo.isPrimaryType()) {
                collectedPrimaryTypes.add(typeInfo);
            }
            continueType(typeInfo, jcClassDecl);
            // don't commit yet, happens at the end of ScanCompilationUnits, after JavaDoc resolution
            return null;
        } catch (RuntimeException | AssertionError | StackOverflowError re) {
            LOGGER.error("Caught exception in type {}", node.getSimpleName());
            throw re;
        }
    }

    private void continueType(TypeInfo typeInfo, JCTree.JCClassDecl jcClassDecl) {
        typeStack.addLast(typeInfo);
        elementStack.push();

        // flags: modifiers, type nature
        TypeInfo.Builder builder = typeInfo.builder();
        flagHelper.type(jcClassDecl.sym, builder);
        builder.computeAccess();

        // type parameters; must be done in 2 stages
        if (!jcClassDecl.getTypeParameters().isEmpty()) {
            int index = 0;
            List<TypeParameter> newTypeParameters = new ArrayList<>();
            for (JCTree.JCTypeParameter jcTypeParameter : jcClassDecl.getTypeParameters()) {
                String name = jcTypeParameter.getName().toString();
                TypeParameter tp = runtime.newTypeParameter(index, name, typeInfo);
                builder.addOrSetTypeParameter(tp);
                elementStack.put(name, tp);
                newTypeParameters.add(tp);
                ++index;
            }
            int i = 0;
            for (JCTree.JCTypeParameter jcTypeParameter : jcClassDecl.getTypeParameters()) {
                TypeParameter tp = newTypeParameters.get(i++);
                parseTypeBoundsAndCommit(jcClassDecl.sym, tp, jcTypeParameter);
            }
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
        assert parentClass != null;
        builder.setParentClass(parentClass);
        if (!jcClassDecl.implementing.isEmpty()) {
            if (scanResult != null) {
                String keyword = typeInfo.isInterface() ? "extends" : "implements";
                Source source = scanResult.find(keyword, sourceForNode(jcClassDecl.implementing.getFirst()));
                dsb.put(DetailedSources.IMPLEMENTS, source);
            }
            for (JCTree.JCExpression i : jcClassDecl.implementing) {
                builder.addInterfaceImplemented(convertType.convertTree(i, dsb));
            }
        }
        if (typeInfo.typeNature().isAnnotation()) {
            ParameterizedType javaLangAnnotationAnnotation = convertType.convert(jcClassDecl.sym.getInterfaces().getFirst());
            builder.addInterfaceImplemented(javaLangAnnotationAnnotation);
        }
        for (JCTree.JCExpression permits : jcClassDecl.permitting) {
            TypeInfo permitted = convertType.convert(permits.type).typeInfo();
            builder.addPermittedType(permitted);
            dsb.put(permitted, sourceForNode(permits));
        }

        // record components: fields and accessors
        if (typeInfo.typeNature().isRecord()) {
            RecordSynthetics recordSynthetics = new RecordSynthetics(runtime, typeInfo);
            for (Symbol.RecordComponent rc : jcClassDecl.sym.getRecordComponents()) {
                String fieldName = rc.name.toString();
                FieldInfo fieldInfo;
                // check presence
                FieldInfo inMap = typeInfo.getFieldByName(fieldName, false);
                if (inMap == null) {
                    ParameterizedType pt = convertType.convert(rc.type);
                    fieldInfo = runtime.newFieldInfo(fieldName, false, pt, typeInfo);
                    Symbol.VarSymbol varSym = (Symbol.VarSymbol) jcClassDecl.sym.members()
                            .findFirst(rc.name, sym -> sym.getKind() == ElementKind.FIELD);
                    typeData.put(varSym, fieldInfo);
                    builder.addField(fieldInfo);
                } else {
                    fieldInfo = inMap;
                }
                if (fieldInfo.modifiers().isEmpty()) {
                    fieldInfo.builder()
                            .addFieldModifier(runtime.fieldModifierFinal())
                            .addFieldModifier(runtime.fieldModifierPrivate());
                }
                fieldInfo.builder().setAccess(runtime.accessPrivate());

                // check presence
                MethodInfo miInMap = typeInfo.methodStream()
                        .filter(mi -> fieldName.equals(mi.name()) && mi.parameters().isEmpty())
                        .findFirst().orElse(null);
                if (miInMap == null) {
                    MethodInfo accessor = recordSynthetics.createAccessor(fieldInfo);
                    List<MethodInfo> overrides = computeMethodOverrides.findOverriddenMethods(rc.accessor)
                            .stream()
                            .map(typeData::getOrLoadMethod)
                            .toList();
                    accessor.builder().addOverrides(overrides).commit();
                    builder.addMethod(accessor);
                    typeData.put(rc.accessor, accessor);
                }
            }
        }
        // annotations
        for (JCTree.JCAnnotation annotation : jcClassDecl.getModifiers().getAnnotations()) {
            AnnotationExpression ae = convertAnnotation(annotation);
            builder.addAnnotation(ae);
        }

        // members: methods, fields
        for (var member : jcClassDecl.getMembers()) {
            currentMethod = null;
            scan(member, null);
        }
        if (typeInfo.typeNature().isEnum()) {
            for (var symbol : jcClassDecl.sym.members().getSymbols()) {
                if (symbol instanceof Symbol.MethodSymbol ms
                    && ("values".equals(ms.name.toString()) && ms.params().isEmpty()
                        || "valueOf".equals(ms.name.toString())
                           && ms.params().size() == 1
                           && ms.params().head.type.tsym.flatName().contentEquals("java.lang.String"))
                    && typeData.getMethod(ms) == null) {
                    convertType.ensureMethod(ms, true);
                }
            }
        }
        MethodInfo singleAbstractMethod = convertType.computeSAM(jcClassDecl.type);
        builder.setSingleAbstractMethod(singleAbstractMethod);

        // add synthetic getters and setters for methods annotated with @GetSet
        if (typeInfo.typeNature().isInterface() || typeInfo.typeNature().isClass() && builder.isAbstract()) {
            createSyntheticFieldsForGetSet.createSyntheticFields(typeInfo);
        }

        DocCommentTree docComment = docTrees.getDocCommentTree(getCurrentPath());
        if (docComment != null) {
            JavaDoc javaDoc = scanJavaDoc.scan(docComment);
            builder.addComment(javaDoc);
            builder.setJavaDoc(javaDoc);
        }

        Source source = sourceForNode(jcClassDecl, dsb);
        builder.addTrailingComments(trailingCommentsForNode(source))
                .addComments(commentsForNode(source))
                .setSource(source);

        typeStack.removeLast();
        elementStack.pop();
    }

    private void parseTypeBoundsAndCommit(Symbol owner, TypeParameter tp, JCTree.JCTypeParameter jcTypeParameter) {
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        List<ParameterizedType> typeBounds = jcTypeParameter.getBounds().stream()
                .map(e -> parseTypeBoundCheckSelfReference(owner, tp, e, dsb))
                .toList();
        List<AnnotationExpression> annotationExpressions = jcTypeParameter.annotations.stream()
                .map(this::convertAnnotation).toList();
        tp.builder()
                .setSource(sourceForNode(jcTypeParameter, dsb))
                .addAnnotations(annotationExpressions)
                .setTypeBounds(typeBounds)
                .commit();
    }

    private ParameterizedType parseTypeBoundCheckSelfReference(Symbol owner,
                                                               TypeParameter tp,
                                                               JCTree.JCExpression expression,
                                                               DetailedSources.Builder dsb) {
        if (expression.type.tsym == owner) {
            LOGGER.debug("Self-reference");
            return runtime.newParameterizedType(tp.getOwner().getLeft(),
                    List.of(runtime.newParameterizedType(tp, 0, null)));
        }
        return convertType.convertTree(expression, dsb);
    }

    // -- Method declarations ---------------------------------------------

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        try {
            JCTree.JCMethodDecl jcMethod = (JCTree.JCMethodDecl) node;
            String methodName = node.getName().toString();
            boolean isConstructor = "<init>".equals(methodName);
            long methodFlags = jcMethod.getModifiers().flags;
            TypeInfo currentType = typeStack.getLast();
            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
            Map<String, Element> parameterMap = elementStack.push();

            MethodInfo methodInfo;
            MethodInfo known = typeData.getMethod(jcMethod.sym);
            boolean isKnown = known != null;
            MethodInfo.Builder builder;
            if (isKnown) {
                methodInfo = known;
                methodInfo.parameters().forEach(pi -> parameterMap.put(pi.name(), pi));
                methodInfo.typeParameters().forEach(tp -> parameterMap.put(tp.simpleName(), tp));
                builder = methodInfo.builder();
            } else {
                // construction of the method
                if (isConstructor) {
                    methodInfo = runtime.newConstructor(currentType, flagHelper.constructorType(methodFlags));
                    builder = methodInfo.builder();
                    currentType.builder().addConstructor(methodInfo);
                    builder.setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor());
                } else {
                    MethodInfo.MethodType methodType = flagHelper.methodType(methodFlags,
                            currentType.isInterface() || currentType.isAnnotation());
                    methodInfo = runtime.newMethod(currentType, methodName, methodType);
                    builder = methodInfo.builder();
                    currentType.builder().addMethod(methodInfo);
                }
                typeData.put(jcMethod.sym, methodInfo);


                // flags
                flagHelper.method(methodFlags, builder);

                // type parameters; must be done in 2 stages
                if (!jcMethod.getTypeParameters().isEmpty()) {
                    int index = 0;
                    List<TypeParameter> newTypeParameters = new ArrayList<>();
                    for (JCTree.JCTypeParameter typeParameter : jcMethod.getTypeParameters()) {
                        String name = typeParameter.getName().toString();
                        TypeParameter tp = runtime.newTypeParameter(index, name, methodInfo);
                        builder.addTypeParameter(tp);
                        elementStack.put(name, tp);
                        newTypeParameters.add(tp);
                        ++index;
                    }
                    int i = 0;
                    for (JCTree.JCTypeParameter typeParameter : jcMethod.getTypeParameters()) {
                        TypeParameter tp = newTypeParameters.get(i++);
                        parseTypeBoundsAndCommit(jcMethod.sym, tp, typeParameter);
                    }
                }

                // return type
                if (!isConstructor) {
                    List<AnnotationExpression> annots = new ArrayList<>();
                    ParameterizedType returnType = convertTypeWithAnnotations(node.getReturnType(), dsb, annots::add);
                    builder.setReturnType(returnType)
                            .addAnnotations(annots);
                }

                // parameters
                for (JCTree.JCVariableDecl jcVariableDecl : jcMethod.getParameters()) {
                    String name = jcVariableDecl.getName().toString();
                    DetailedSources.Builder dsbParam = runtime.newDetailedSourcesBuilder();
                    List<AnnotationExpression> annots = new ArrayList<>();
                    ParameterizedType type = convertTypeWithAnnotations(jcVariableDecl.getType(), dsbParam, annots::add);
                    ParameterInfo parameterInfo = builder.addParameter(name, type);
                    parameterInfo.builder().addAnnotations(annots);

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

                // exception types
                if (!jcMethod.thrown.isEmpty()) {
                    jcMethod.thrown.stream()
                            .map(e -> convertType.convertTree(e, dsb))
                            .forEach(builder::addExceptionType);
                }
                builder.commitParameters();
            }

            // method name
            String sourceMethodName = isConstructor ? currentType.simpleName() : methodName;
            dsb.put(methodName, sourceOfIdentifier(sourceMethodName, jcMethod.pos));

            // annotations
            for (JCTree.JCAnnotation annotation : jcMethod.getModifiers().getAnnotations()) {
                AnnotationExpression ae = convertAnnotation(annotation);
                builder.addAnnotation(ae);
            }

            Block methodBody;
            // method body
            if (methodInfo.isAbstract() && currentType.typeNature().isAnnotation()) {
                methodBody = runtime.emptyBlock();
                // TODO: an idea is to add a "return defaultValue;" statement so that we don't drop the value
            } else {
                currentMethod = methodInfo;
                // the sum of statements in a compact constructor may be > 9 so we need to pad correctly
                int addToStatementsSize = methodInfo.methodType().isCompactConstructor()
                                          && currentType.typeNature().isRecord()
                        ? methodInfo.parameters().size() : 0;
                methodBody = parseBlock("-", node.getBody(), addToStatementsSize);
                elementStack.pop();
                currentMethod = null;
            }
            if ((methodInfo.methodType().isSyntheticConstructor() || methodInfo.methodType().isCompactConstructor())
                && currentType.typeNature().isRecord()) {
                Block.Builder bb = runtime.newBlockBuilder();
                bb.addStatements(methodBody.statements());
                int n = methodBody.statements().size() - 1; // 1 for the synthetic super() statement to be ignored
                int sum = methodInfo.parameters().size() + methodBody.statements().size();
                for (ParameterInfo pi : methodInfo.parameters()) {
                    FieldInfo field = currentType.getFieldByName(pi.name(), true);
                    Assignment a = runtime.newAssignmentBuilder()
                            .setValue(runtime.newVariableExpressionBuilder()
                                    .setSource(runtime.noSource()).setVariable(pi)
                                    .build())
                            .setTarget(runtime.newVariableExpressionBuilder().setSource(runtime.noSource())
                                    .setVariable(runtime.newFieldReference(field)).build())
                            .build();
                    bb.addStatement(runtime.newExpressionAsStatementBuilder()
                            .setSource(runtime.noSource().withIndex(StringUtil.pad(n + pi.index(), sum)))
                            .setExpression(a)
                            .build());
                }
                methodBody = bb.build();
            }

            //overrides
            List<Symbol.MethodSymbol> overridden = computeMethodOverrides.findOverriddenMethods(jcMethod.sym);
            Set<MethodInfo> overrides = overridden.stream()
                    .map(typeData::getOrLoadMethod)
                    .collect(Collectors.toUnmodifiableSet());

            DocCommentTree docComment = docTrees.getDocCommentTree(getCurrentPath());
            if (docComment != null) {
                JavaDoc javaDoc = scanJavaDoc.scan(docComment);
                builder.addComment(javaDoc);
                builder.setJavaDoc(javaDoc);
            }

            Source source = sourceForNode(node, dsb);
            builder.addOverrides(overrides)
                    .setSource(source)
                    .addComments(commentsForNode(source))
                    .setMethodBody(methodBody)
                    .computeAccess();
            // don't commit yet, happens at the end of ScanCompilationUnits, after JavaDoc resolution
            return null;
        } catch (RuntimeException | AssertionError | StackOverflowError re) {
            LOGGER.error("Caught exception in method {}", node.getName().toString());
            throw re;
        }
    }

    // -- Annotations ---------------------------------------------

    private ParameterizedType convertTypeWithAnnotations(Tree node,
                                                         DetailedSources.Builder dsb,
                                                         Consumer<AnnotationExpression> consumer) {
        Tree rt;
        if (node instanceof JCTree.JCAnnotatedType at) {
            rt = at.getUnderlyingType();
            for (JCTree.JCAnnotation annotationTree : at.getAnnotations()) {
                consumer.accept(convertAnnotation(annotationTree));
            }
        } else {
            rt = node;
        }
        return convertType.convertTree(rt, dsb);
    }

    private AnnotationExpression convertAnnotation(JCTree.JCAnnotation annotation) {
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType at = convertType.convertTree(annotation.getAnnotationType(), dsb);
        List<AnnotationExpression.KV> kvs = new ArrayList<>();
        for (var c : annotation.getArguments()) {
            kvs.add(convertAnnotationKv(c));
        }
        return runtime.newAnnotationExpressionBuilder()
                .setKeyValuesPairs(kvs)
                .setSource(sourceForNode(annotation, dsb))
                .setTypeInfo(at.typeInfo())
                .build();
    }

    private AnnotationExpression.KV convertAnnotationKv(JCTree.JCExpression c) {
        String key;
        Expression value;
        if (c instanceof JCTree.JCAssign assign) {
            if (assign.lhs instanceof JCTree.JCIdent ident) {
                key = ident.name.toString();
            } else throw new UnsupportedOperationException();
            scan(assign.rhs, null);
        } else {
            key = "value";
            scan(c, null);
        }
        value = currentExpression;
        return runtime.newAnnotationExpressionKeyValuePair(key, value);
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

    private Block parseBlock(String blockIndex, Tree node, LocalVariable... variablesToAdd) {
        return parseBlock(blockIndex, node, 0, variablesToAdd);
    }

    private Block parseBlock(String blockIndex, Tree node, int addToStatementsSize, LocalVariable... variablesToAdd) {
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
        return parseBlock(blockIndex, statements, addToStatementsSize,
                statementLabels.get(node), source, variablesToAdd);
    }

    private Block parseBlock(String blockIndex,
                             List<JCTree.JCStatement> statements,
                             int addToStatementsSize,
                             String label,
                             Source source,
                             LocalVariable... variablesToAdd) {
        Map<String, Element> localVariableMap = elementStack.push();
        for (LocalVariable lv : variablesToAdd) {
            localVariableMap.put(lv.simpleName(), lv);
        }
        int n = statements.size() + addToStatementsSize;
        String i = "-".equals(blockIndex) ? "-" : statementIndex() + "." + blockIndex;
        blockBuilders.addLast(new BlockData(runtime.newBlockBuilder(), i, n));

        for (JCTree.JCStatement statement : statements) {
            if (statement instanceof JCTree.JCBlock subBlock) {
                Block parsedSub = parseBlock("0", subBlock, 0);
                addStatement(parsedSub);
            } else if (statement instanceof JCTree.JCClassDecl localType) {
                Statement localTypeCreation = handleLocalType(localType);
                addStatement(localTypeCreation);
            } else {
                scan(statement, null);
            }
        }

        elementStack.pop();

        return blockBuilders.removeLast().blockBuilder
                .setLabel(label)
                .setSource(source)
                .addTrailingComments(trailingCommentsForNode(source))
                .addComments(commentsForNode(source))
                .build();
    }

    private Statement handleLocalType(JCTree.JCClassDecl localType) {
        String simpleName = localType.getSimpleName().toString();
        int index = currentMethod.typeInfo().builder().getAndIncrementAnonymousTypes();
        TypeInfo typeInfo = runtime.newTypeInfo(currentMethod, simpleName, index);
        MethodInfo here = currentMethod;
        elementStack.put(simpleName, typeInfo);
        continueType(typeInfo, localType);
        currentMethod = here;
        typeInfo.builder()
                .setAccess(runtime.accessPrivate())
                .commit();
        return runtime.newLocalTypeDeclarationBuilder()
                .setLabel(statementLabels.get(localType))
                .setTypeInfo(typeInfo)
                .setSource(statementSourceForNode(localType))
                .build();
    }

    @Override
    public Void visitAssert(AssertTree node, Void unused) {
        JCTree.JCAssert jcAssert = (JCTree.JCAssert) node;
        currentExpression = null;
        scan(jcAssert.getCondition(), unused);
        Expression condition = currentExpression;
        currentExpression = null;
        scan(jcAssert.getDetail(), unused);
        Expression message = Objects.requireNonNullElseGet(currentExpression, runtime::newEmptyExpression);

        Source source = statementSourceForNode(node);
        addStatement(runtime.newAssertBuilder()
                .setLabel(statementLabels.get(node))
                .setSource(source)
                .addComments(commentsForNode(source))
                .setExpression(condition)
                .setMessage(message)
                .build());
        return null;
    }

    // only for (static and instance) initializer blocks
    @Override
    public Void visitBlock(BlockTree node, Void unused) {
        if (node instanceof JCTree.JCBlock jcBlock && (jcBlock.flags & Flags.STATIC) != 0) {
            TypeInfo typeInfo = typeStack.getLast();
            int index = (int) typeInfo.methods().stream().filter(MethodInfo::isStaticInitializer).count();
            MethodInfo methodInfo = runtime.newMethod(typeInfo, "<static_" + index + ">",
                    runtime.methodTypeStaticInitializer());
            methodInfo.builder().setReturnType(runtime.voidParameterizedType())
                    .setSource(sourceForNode(node))
                    .setAccess(runtime.accessPrivate())
                    .addMethodModifier(runtime.methodModifierPrivate())
                    .addMethodModifier(runtime.methodModifierStatic())
                    .commitParameters();
            currentMethod = methodInfo;
            Block block = parseBlock("-", jcBlock);
            currentMethod = null;
            methodInfo.builder().setMethodBody(block);
            typeInfo.builder().addMethod(methodInfo);
            return null;
        } else {
            Tree parent = getCurrentPath().getParentPath().getLeaf();
            if (parent instanceof ClassTree) {
                TypeInfo typeInfo = typeStack.getLast();
                int index = (int) typeInfo.methods().stream().filter(MethodInfo::isInstanceInitializer).count();
                MethodInfo methodInfo = runtime.newMethod(typeInfo,
                        "<init_" + index + ">", runtime.methodTypeInstanceInitializer());
                methodInfo.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                        .setSource(sourceForNode(node))
                        .setAccess(runtime.accessPrivate())
                        .addMethodModifier(runtime.methodModifierPrivate())
                        .commitParameters();
                Block block = parseBlock("-", node);
                methodInfo.builder().setMethodBody(block);
                typeInfo.builder().addMethod(methodInfo);
                return null;
            }
            if (parent instanceof NewClassTree) {
                throw new UnsupportedOperationException();
            }
        }
        return super.visitBlock(node, unused);
    }

    @Override
    public Void visitBreak(BreakTree node, Void unused) {
        String gotoLabel = node.getLabel() == null ? null : node.getLabel().toString();
        addStatement(runtime.newBreakBuilder()
                .setLabel(statementLabels.get(node))
                .setGoToLabel(gotoLabel)
                .setSource(statementSourceForNode(node)).build());
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree node, Void unused) {
        String gotoLabel = node.getLabel() == null ? null : node.getLabel().toString();
        addStatement(runtime.newContinueBuilder()
                .setLabel(statementLabels.get(node))
                .setGoToLabel(gotoLabel)
                .setSource(statementSourceForNode(node)).build());
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
        Block block = parseBlock("0", node.getStatement());
        currentExpression = null;
        scan(node.getCondition(), unused);
        Expression condition = currentExpression;
        addStatement(runtime.newDoBuilder()
                .setLabel(statementLabels.get(node))
                .setSource(statementSourceForNode(node))
                .setBlock(block)
                .setExpression(condition)
                .build());
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, Void unused) {
        super.visitExpressionStatement(node, unused);
        if (currentExpression != null) {
            Source source = statementSourceForNode(node);
            ExpressionAsStatement statement = runtime.newExpressionAsStatementBuilder()
                    .setLabel(statementLabels.get(node))
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
            List<AnnotationExpression> annotations = new ArrayList<>();
            ParameterizedType type = convertTypeWithAnnotations(node.getVariable().getType(), dsb, annotations::add);
            currentExpression = runtime.newEmptyExpression();
            lvc = continueLocalVariableCreation(variableDecl, name, type, dsb, null, annotations);
        } else throw new UnsupportedOperationException("NYI");

        Block block = parseBlock("0", node.getStatement());
        addStatement(runtime.newForEachBuilder()
                .setLabel(statementLabels.get(node))
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

        Map<String, Element> map = elementStack.push();
        if (!node.getInitializer().isEmpty()) {
            if (node.getInitializer().getFirst() instanceof JCTree.JCVariableDecl) {
                // sequence of LVCs, but we want them all together in one statement
                LocalVariableCreation.Builder lvcBuilder = null;
                for (StatementTree statementTree : node.getInitializer()) {
                    Block initBlock = parseBlock("?", statementTree);
                    assert !initBlock.statements().isEmpty();
                    Statement first = initBlock.statements().getFirst();
                    if (first instanceof LocalVariableCreation f) {
                        LocalVariable lv = f.localVariable();
                        if (lvcBuilder == null) {
                            lvcBuilder = runtime.newLocalVariableCreationBuilder().setLocalVariable(lv);
                        } else {
                            lvcBuilder.addOtherLocalVariable(lv);
                        }
                        map.put(lv.simpleName(), lv);
                    } else throw new UnsupportedOperationException("NYI");
                }
                assert lvcBuilder != null;
                LocalVariableCreation built = lvcBuilder.build();
                forBuilder.addInitializer(built);
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
        elementStack.pop();
        addStatement(forBuilder
                .setLabel(statementLabels.get(node))
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
                .setLabel(statementLabels.get(node))
                .setSource(statementSourceForNode(node))
                .setIfBlock(block)
                .setElseBlock(elseBlock)
                .setExpression(condition)
                .build());
        return null;
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree node, Void unused) {
        statementLabels.put(node.getStatement(), node.getLabel().toString());
        return super.visitLabeledStatement(node, unused);
    }

    @Override
    public Void visitReturn(ReturnTree node, Void unused) {
        currentExpression = null;
        scan(node.getExpression(), unused);
        Source source = statementSourceForNode(node);
        addStatement(runtime.newReturnBuilder()
                .setLabel(statementLabels.get(node))
                .setSource(source)
                .addComments(commentsForNode(source))
                .setExpression(currentExpression == null ? runtime.newEmptyExpression() : currentExpression)
                .build());
        return null;
    }

    @Override
    public Void visitYield(YieldTree node, Void unused) {
        currentExpression = null;
        scan(node.getValue(), unused);
        assert currentExpression != null;
        Source source = statementSourceForNode(node);
        addStatement(runtime.newYieldBuilder()
                .setLabel(statementLabels.get(node))
                .setSource(source)
                .addComments(commentsForNode(source))
                .setExpression(currentExpression)
                .build());
        return null;
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void unused) {
        scan(node.getExpression(), unused);
        Expression selector = currentExpression;

        JCTree.JCSwitch jcSwitch = (JCTree.JCSwitch) node;
        boolean newStyle = jcSwitch.cases.getFirst().caseKind == CaseTree.CaseKind.RULE;

        Statement s;
        if (newStyle) {
            List<SwitchEntry> switchEntries = doSwitchEntries(unused, jcSwitch.cases);
            s = runtime.newSwitchStatementNewStyleBuilder()
                    .setSelector(selector)
                    .setSource(statementSourceForNode(node))
                    .addSwitchEntries(switchEntries)
                    .build();
        } else {
            List<SwitchStatementOldStyle.SwitchLabel> switchLabels = new ArrayList<>();
            int statementCount = 0;
            List<JCTree.JCStatement> statementsToParse = new ArrayList<>();
            for (JCTree.JCCase jcCase : jcSwitch.cases) {
                for (JCTree.JCCaseLabel caseLabel : jcCase.getLabels()) {

                    RecordPattern patternVariable;
                    Expression expression;
                    switch (caseLabel) {
                        case JCTree.JCConstantCaseLabel ccl -> {
                            scan(ccl.getConstantExpression(), unused);
                            expression = currentExpression;
                            patternVariable = null;
                        }
                        case JCTree.JCDefaultCaseLabel _ -> {
                            expression = runtime.newEmptyExpression();
                            patternVariable = null;
                        }
                        case JCTree.JCPatternCaseLabel pcl -> {
                            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                            List<AnnotationExpression> annotations = new ArrayList<>();
                            RecordPatternResult rpr = parseRecordPattern(pcl.getPattern(), dsb, annotations);
                            patternVariable = rpr.rp;
                            expression = runtime.newEmptyExpression();
                            rpr.newVariables.forEach(lv -> elementStack.put(lv.simpleName(), lv));
                        }
                        case null, default -> throw new UnsupportedOperationException("NYI");
                    }

                    currentExpression = null;
                    scan(jcCase.getGuard(), null);
                    Expression whenExpression = currentExpression;

                    SwitchStatementOldStyle.SwitchLabel switchLabel = runtime.newSwitchLabelOldStyle
                            (expression, statementCount, patternVariable, whenExpression);
                    switchLabels.add(switchLabel);
                }
                statementsToParse.addAll(jcCase.stats);
                statementCount += jcCase.stats.size();
            }
            Block block = parseBlock("0", statementsToParse, 0,
                    null, sourceForNode(node));
            s = runtime.newSwitchStatementOldStyleBuilder()
                    .setLabel(statementLabels.get(node))
                    .setSelector(selector)
                    .addSwitchLabels(switchLabels)
                    .setSource(statementSourceForNode(node))
                    .setBlock(block)
                    .build();
        }
        addStatement(s);
        return null;
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void unused) {
        scan(node.getExpression(), unused);
        Expression expression = currentExpression;
        Block block = parseBlock("0", node.getBlock());
        Source source = statementSourceForNode(node);
        addStatement(runtime.newSynchronizedBuilder()
                .setLabel(statementLabels.get(node))
                .setSource(source)
                .setBlock(block)
                .setExpression(expression)
                .addComments(commentsForNode(source))
                .build());
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree node, Void unused) {
        scan(node.getExpression(), unused);
        Expression expression = currentExpression;
        addStatement(runtime.newThrowBuilder()
                .setLabel(statementLabels.get(node))
                .setExpression(expression)
                .setSource(statementSourceForNode(node))
                .build());
        return null;
    }

    @Override
    public Void visitTry(TryTree node, Void unused) {
        TryStatement.Builder tryBuilder = runtime.newTryBuilder();
        Source source = statementSourceForNode(node);

        List<LocalVariable> resourceVariables = new ArrayList<>();
        int resourceCount = 0;
        for (Tree resource : node.getResources()) {
            String index = source.index() + "+" + resourceCount;
            Statement first;
            if (resource instanceof JCTree.JCIdent) {
                scan(resource, unused);
                Expression expression = currentExpression;
                first = runtime.newExpressionAsStatementBuilder()
                        .setSource(sourceForNode(resource, index))
                        .setExpression(expression).build();
            } else {
                Block b = parseBlock("?", resource, resourceVariables.toArray(LocalVariable[]::new));
                assert b.statements().size() == 1;
                Statement s = b.statements().getFirst();
                if (s instanceof LocalVariableCreation lvc) {
                    lvc.localVariableStream().forEach(resourceVariables::add);
                    first = lvc.withSource(s.source().withIndex(index));
                } else throw new UnsupportedOperationException("NYI");
            }
            tryBuilder.addResource(first);
            ++resourceCount;
        }
        int n = 1 + node.getCatches().size() + (node.getFinallyBlock() != null ? 1 : 0);
        Block block = parseBlock(StringUtil.pad(0, n), node.getBlock(), resourceVariables.toArray(LocalVariable[]::new));
        int i = 1;
        for (CatchTree c : node.getCatches()) {
            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
            TryStatement.CatchClause.Builder builder = runtime.newCatchClauseBuilder();
            Tree typeOfParameter = c.getParameter().getType();
            ParameterizedType unionType;
            List<AnnotationExpression> annots = new ArrayList<>();

            if (typeOfParameter instanceof JCTree.JCTypeUnion typeUnion) {
                unionType = convertType.convert(typeUnion.type);
                for (Tree alternative : typeUnion.alternatives) {
                    ParameterizedType type = convertTypeWithAnnotations(alternative, dsb, annots::add);
                    builder.addType(type);
                }
            } else {
                ParameterizedType type = convertTypeWithAnnotations(typeOfParameter, dsb, annots::add);
                builder.addType(type);
                unionType = type;
            }
            LocalVariable lv = runtime.newLocalVariable(c.getParameter().getName().toString(), unionType);
            boolean isFinal = c.getParameter().getModifiers().getFlags().contains(javax.lang.model.element.Modifier.FINAL);
            Block catchBlock = parseBlock(StringUtil.pad(i, n), c.getBlock(), lv);

            // annotations
            builder.addAnnotations(annots);
            for (AnnotationTree at : c.getParameter().getModifiers().getAnnotations()) {
                AnnotationExpression ae = convertAnnotation((JCTree.JCAnnotation) at);
                builder.addAnnotation(ae);
            }

            tryBuilder.addCatchClause(builder
                    .setCatchVariable(lv)
                    .setFinal(isFinal)
                    .setBlock(catchBlock)
                    .setSource(sourceForNode(c, dsb))
                    .build());
            ++i;
        }
        Block finallyBlock;
        if (node.getFinallyBlock() != null) {
            finallyBlock = parseBlock(StringUtil.pad(i, n), node.getFinallyBlock());
        } else {
            finallyBlock = runtime.emptyBlock();
        }
        Statement s = tryBuilder
                .setLabel(statementLabels.get(node))
                .setBlock(block)
                .setFinallyBlock(finallyBlock)
                .setSource(source)
                .build();
        addStatement(s);
        return null;
    }

    // note: also field declarations

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        if (node instanceof JCTree.JCVariableDecl variableDecl) {
            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
            if (variableDecl.sym instanceof Symbol.VarSymbol varSymbol) {
                String name = varSymbol.toString();

                List<AnnotationExpression> annots = new ArrayList<>();
                ParameterizedType type = convertTypeWithAnnotations(variableDecl.vartype, dsb, annots::add);

                currentExpression = null;
                scan(node.getInitializer(), p);
                if (currentExpression == null) {
                    currentExpression = runtime.newEmptyExpression();
                }
                Expression initializer = currentExpression;
                if (currentMethod == null) {
                    createField(variableDecl, varSymbol, name, type, annots, dsb, initializer);
                } else {

                    // local variable
                    Statement prev = lastStatement();
                    LocalVariableCreation prevLvc = prev instanceof LocalVariableCreation lvc2 ? lvc2 : null;
                    LocalVariableCreation lvc = continueLocalVariableCreation(variableDecl, name, type, dsb, prevLvc,
                            annots);
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

    private void createField(JCTree.JCVariableDecl variableDecl,
                             Symbol.VarSymbol varSymbol,
                             String name,
                             ParameterizedType type,
                             List<AnnotationExpression> annots,
                             DetailedSources.Builder dsb,
                             Expression initializer) {
        long flags = variableDecl.getModifiers().flags;
        boolean isStatic = (flags & Flags.STATIC) != 0;
        TypeInfo owner = typeStack.getLast();
        if (!owner.typeNature().isRecord() || isStatic) {
            FieldInfo inMap = owner.getFieldByName(name, false);
            FieldInfo fieldInfo;
            if (inMap == null) {
                fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
                owner.builder().addField(fieldInfo);
                typeData.put(varSymbol, fieldInfo);
            } else {
                fieldInfo = inMap;
            }
            fieldInfo.builder().addAnnotations(annots);

            flagHelper.field(flags, fieldInfo.builder());

            // annotations
            for (JCTree.JCAnnotation annotation : variableDecl.getModifiers().getAnnotations()) {
                AnnotationExpression ae = convertAnnotation(annotation);
                fieldInfo.builder().addAnnotation(ae);
            }

            // source, source of name
            Source vdSource = sourceForNode(variableDecl); // declaration, but only of one field
            Source nameSource = sourceOfIdentifier(name, variableDecl.pos);
            dsb.put(name, nameSource);
            Source nameAndInitSource;
            if (variableDecl.init != null) {
                Source s = sourceForNode(variableDecl.init);
                nameAndInitSource = nameSource.max(s);
            } else {
                nameAndInitSource = nameSource;
            }
            dsb.put(DetailedSources.FIELD_DECLARATION, vdSource);

            fieldInfo.builder()
                    .addComments(commentsForNode(vdSource))
                    .setSource(nameAndInitSource.withDetailedSources(dsb.build()))
                    .setInitializer(initializer)
                    .computeAccess()
                    .commit();
            assert fieldInfo.access() != null;
        } // else: non-static record components are dealt with in the type visitor
    }

    private boolean sameLvc(LocalVariableCreation lvc1, LocalVariableCreation lvc2) {
        Source s1 = lvc1.source();
        Source s2 = lvc2.source();
        return s1.beginPos() == s2.beginPos() && s1.beginLine() == s2.beginLine();
    }

    private @NotNull LocalVariableCreation continueLocalVariableCreation(JCTree.JCVariableDecl variableDecl,
                                                                         String name,
                                                                         ParameterizedType type,
                                                                         DetailedSources.Builder dsb,
                                                                         LocalVariableCreation prevLvc,
                                                                         List<AnnotationExpression> annotations) {
        boolean isUnnamed = name.isEmpty();
        LocalVariable localVariable = runtime.newLocalVariable(isUnnamed ? ":" : name, type, currentExpression);
        LocalVariableCreation.Builder lvcb = runtime.newLocalVariableCreationBuilder()
                .setSource(sourceForNode(variableDecl))
                .setLocalVariable(localVariable);
        long flags = variableDecl.getModifiers().flags;
        boolean isFinal = (flags & Flags.FINAL) != 0;
        if (isFinal) lvcb.addModifier(runtime.localVariableModifierFinal());
        if (variableDecl.declaredUsingVar()) lvcb.addModifier(runtime.localVariableModifierVar());

        lvcb.addAnnotations(annotations);
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
        String searchFor = isUnnamed ? "_" : name;
        Source namePos = source.ofIndex(s, s.indexOf(searchFor), searchFor.length());
        assert namePos != null;
        dsb.put(name, namePos);
        Source assignSource = source.ofIndex(s, s.indexOf("="), 1);
        if (assignSource != null) {
            dsb.putList(DetailedSources.LOCAL_VARIABLE_ASSIGNMENT_OPERATORS, List.of(assignSource));
        }
        Source statementSource = statementSourceForNode(variableDecl, dsb);
        lvcb.setSource(statementSource);
        elementStack.put(localVariable.simpleName(), localVariable);
        return lvcb
                .addComments(commentsForNode(statementSource))
                .setLabel(statementLabels.get(variableDecl))
                .build();
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
                .setLabel(statementLabels.get(node))
                .setSource(statementSourceForNode(node))
                .setBlock(block)
                .setExpression(condition)
                .build());
        return null;
    }

    // -- Expressions ---------------------------------------------


    @Override
    public Void visitAnnotation(AnnotationTree node, Void unused) {
        JCTree.JCAnnotation annotation = (JCTree.JCAnnotation) node;
        currentExpression = convertAnnotation(annotation);
        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void unused) {
        JCTree.JCArrayAccess aa = (JCTree.JCArrayAccess) node;
        scan(aa.indexed, unused);
        Expression array = currentExpression;
        scan(aa.index, unused);
        Expression index = currentExpression;
        currentExpression = runtime.newVariableExpressionBuilder()
                .setSource(sourceForNode(node))
                .setVariable(runtime.newDependentVariable(array, index))
                .build();
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
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        JCTree.JCAssignOp assignOp = (JCTree.JCAssignOp) node;
        scan(assignOp.rhs, p);
        Expression value = currentExpression;
        scan(assignOp.lhs, p);
        VariableExpression target = (VariableExpression) currentExpression;
        Tree.Kind kind = node.getKind();
        MethodInfo operator = switch (kind) {
            case PLUS_ASSIGNMENT -> runtime.assignPlusOperatorInt();
            case MINUS_ASSIGNMENT -> runtime.assignMinusOperatorInt();
            case MULTIPLY_ASSIGNMENT -> runtime.assignMultiplyOperatorInt();
            case DIVIDE_ASSIGNMENT -> runtime.assignDivideOperatorInt();
            case REMAINDER_ASSIGNMENT -> runtime.assignRemainderOperatorInt();
            case AND_ASSIGNMENT -> runtime.assignAndOperatorInt();
            case OR_ASSIGNMENT -> runtime.assignOrOperatorInt();
            case XOR_ASSIGNMENT -> runtime.assignXorOperatorInt();
            case LEFT_SHIFT_ASSIGNMENT -> runtime.assignLeftShiftOperatorInt();
            case RIGHT_SHIFT_ASSIGNMENT -> runtime.assignSignedRightShiftOperatorInt();
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> runtime.assignUnsignedRightShiftOperatorInt();
            default -> throw new UnsupportedOperationException("NYI");
        };
        currentExpression = runtime.newAssignmentBuilder()
                .setAssignmentOperator(operator)
                .setValue(value)
                .setTarget(target)
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
            case MUL, DIV, MOD -> runtime.precedenceMultiplicative();
            case EQ, NE -> runtime.precedenceEquality();
            case BITOR -> runtime.precedenceBitwiseOr();
            case BITXOR -> runtime.precedenceBitwiseXor();
            case BITAND -> runtime.precedenceBitwiseAnd();
            case LT, LE, GT, GE -> runtime.precedenceRelational();
            case SR, SL, USR -> runtime.precedenceShift();
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
        List<JCTree.JCExpression> toParse = new ArrayList<>();
        toParse.add(binary.rhs);
        while (b.getLeftOperand() instanceof JCTree.JCBinary sub && tag.equals(sub.getTag())) {
            toParse.addFirst(sub.rhs);
            b = sub;
        }
        toParse.addFirst(b.lhs);
        // now we have all the clauses in order; important when they contain instanceof pattern variables
        // which must exist sequentially...
        return toParse.stream().map(jce -> {
            scan(jce, null);
            return currentExpression;
        }).toList();
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void unused) {
        JCTree.JCTypeCast jcTypeCast = (JCTree.JCTypeCast) node;
        scan(jcTypeCast.expr, unused);
        Expression expression = currentExpression;
        List<AnnotationExpression> annotations = new ArrayList<>();
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType type = convertTypeWithAnnotations(jcTypeCast.getType(), dsb, annotations::add);
        currentExpression = runtime.newCastBuilder()
                .addAnnotations(annotations)
                .setSource(sourceForNode(node, dsb))
                .setExpression(expression)
                .setParameterizedType(type)
                .build();
        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
        scan(node.getCondition(), unused);
        Expression condition = currentExpression;
        scan(node.getTrueExpression(), unused);
        Expression ifTrue = currentExpression;
        scan(node.getFalseExpression(), unused);
        Expression ifFalse = currentExpression;
        currentExpression = runtime.newInlineConditionalBuilder()
                .setSource(sourceForNode(node))
                .setCondition(condition)
                .setIfTrue(ifTrue)
                .setIfFalse(ifFalse)
                .build(runtime);
        return null;
    }

    private record RecordPatternResult(ParameterizedType type, RecordPattern rp, List<LocalVariable> newVariables) {
    }

    private RecordPatternResult parseRecordPattern(JCTree.JCPattern p,
                                                   DetailedSources.Builder dsb,
                                                   List<AnnotationExpression> annotations) {
        if (p instanceof JCTree.JCBindingPattern bp) {
            ParameterizedType type = convertTypeWithAnnotations(bp.var.getType(), dsb, annotations::add);
            String name = bp.var.name.toString();
            LocalVariable lv = runtime.newLocalVariable(name, type);
            Source source = sourceForNode(bp, dsb);
            RecordPattern recordPattern = runtime.newRecordPatternBuilder()
                    .setSource(source)
                    .setLocalVariable(lv)
                    .build();
            dsb.put(recordPattern, source);
            dsb.put(lv, source);
            dsb.put(lv.simpleName(), sourceOfIdentifier(lv.simpleName(), bp.var.pos));
            return new RecordPatternResult(type, recordPattern, List.of(lv));
        }
        if (p instanceof JCTree.JCRecordPattern rp) {
            DetailedSources.Builder newDsb = runtime.newDetailedSourcesBuilder();
            ParameterizedType type = convertTypeWithAnnotations(rp.getDeconstructor(), newDsb, annotations::add);
            List<RecordPattern> patterns = new ArrayList<>();
            List<LocalVariable> newVariables = new ArrayList<>();
            for (JCTree.JCPattern pattern : rp.getNestedPatterns()) {
                RecordPatternResult recordPatternResult = parseRecordPattern(pattern, newDsb, annotations);
                patterns.add(recordPatternResult.rp);
                newVariables.addAll(recordPatternResult.newVariables);
            }
            Source source = sourceForNode(rp, newDsb);
            RecordPattern recordPattern = runtime.newRecordPatternBuilder()
                    .setSource(source)
                    .setRecordType(type)
                    .setPatterns(patterns)
                    .build();
            dsb.put(recordPattern, sourceForNode(rp));
            return new RecordPatternResult(type, recordPattern, newVariables);
        }
        if (p instanceof JCTree.JCAnyPattern anyPattern) {
            ParameterizedType type = convertType.convert(anyPattern.type);
            Source source = sourceForNode(anyPattern, dsb);
            RecordPattern recordPattern = runtime.newRecordPatternBuilder()
                    .setSource(source)
                    .setUnnamedPattern(true)
                    .build();
            dsb.put(recordPattern, sourceForNode(anyPattern));
            return new RecordPatternResult(type, recordPattern, List.of());
        }
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree node, Void unused) {
        JCTree.JCInstanceOf jcInstanceOf = (JCTree.JCInstanceOf) node;
        scan(jcInstanceOf.expr, unused);
        Expression expression = currentExpression;
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
        ParameterizedType type;
        RecordPattern recordPattern;
        List<AnnotationExpression> annotations = new ArrayList<>();
        if (jcInstanceOf.pattern instanceof JCTree.JCPattern p) {
            RecordPatternResult rpr = parseRecordPattern(p, dsb, annotations);
            type = rpr.type;
            recordPattern = rpr.rp;
            rpr.newVariables.forEach(lv -> elementStack.put(lv.simpleName(), lv));
        } else {
            type = convertTypeWithAnnotations(jcInstanceOf.pattern, dsb, annotations::add);
            recordPattern = null;
        }
        currentExpression = runtime.newInstanceOfBuilder()
                .addAnnotations(annotations)
                .setSource(sourceForNode(node, dsb))
                .setExpression(expression)
                .setTestType(type)
                .setPatternVariable(recordPattern)
                .build();
        return null;
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
        JCTree.JCLambda lambda = (JCTree.JCLambda) node;
        Source source = sourceForNode(node);

        TypeInfo enclosingType = typeStack.getLast();
        int typeIndex = enclosingType.builder().getAndIncrementAnonymousTypes();
        TypeInfo anonymousType = runtime.newAnonymousType(enclosingType, typeIndex);
        anonymousType.builder()
                .setAccess(runtime.accessPrivate())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType());

        ParameterizedType functionalType = convertType.convert(lambda.target);
        ConvertType.SAMDescriptor sd = convertType.findInstantiatedSAM(lambda.target);
        MethodInfo sam = sd.methodInfo();
        assert sam != null;
        String methodName = sam.name();
        MethodInfo methodInfo = runtime.newMethod(anonymousType, methodName, runtime.methodTypeMethod());
        MethodInfo.Builder miBuilder = methodInfo.builder();

        ParameterizedType concreteReturnType = convertType.convert(sd.instantiatedType().restype);

        List<Lambda.OutputVariant> outputVariants = new ArrayList<>();

        Map<String, Element> lambdaParameters = elementStack.push();
        for (var parameter : lambda.getParameters()) {
            if (parameter instanceof JCTree.JCVariableDecl vd) {
                DetailedSources.Builder dsbParam = runtime.newDetailedSourcesBuilder();
                ParameterInfo pi;
                String name = vd.name.toString();
                List<AnnotationExpression> annots = new ArrayList<>();
                ParameterizedType type = convertTypeWithAnnotations(vd.getType(), dsbParam, annots::add);
                if (name.isEmpty()) {
                    pi = miBuilder.addUnnamedParameter(type);
                } else {
                    pi = miBuilder.addParameter(name, type);
                }
                pi.builder()
                        .addAnnotations(annots)
                        .setSource(sourceForNode(parameter, dsbParam))
                        .commit();
                Lambda.OutputVariant ov;
                if (vd.declaredUsingVar()) {
                    ov = runtime.lambdaOutputVariantVar();
                } else if (vd.vartype == null || vd.vartype.pos == vd.pos) {
                    ov = runtime.lambdaOutputVariantEmpty();
                } else {
                    ov = runtime.lambdaOutputVariantTyped();
                }
                outputVariants.add(ov);
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
            methodBody = runtime.newBlockBuilder()
                    .setSource(sourceForNode(lambda.body))
                    .addStatement(returnStatement).build();
        } else if (lambda.getBodyKind() == LambdaExpressionTree.BodyKind.STATEMENT) {
            MethodInfo outer = currentMethod;
            currentMethod = methodInfo;
            methodBody = parseBlock("-", lambda.body);
            currentMethod = outer;
        } else {
            throw new UnsupportedOperationException("NYI");
        }

        elementStack.pop();

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
            String name = node.getName().toString();
            final Source source = sourceForNode(node);
            switch (element.getKind()) {
                case FIELD -> {
                    if (element instanceof Symbol.VarSymbol vs) {
                        TypeInfo typeInfoOwner = convertType.convert(vs.owner.type).bestTypeInfo();
                        assert typeInfoOwner != null;
                        boolean isThis = "this".equals(name);
                        boolean isSuper = "super".equals(name);
                        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                        Variable variable;
                        if (isThis || isSuper) {
                            // TODO explicitly write type
                            variable = runtime.newThis(typeInfoOwner.asParameterizedType(), null, isSuper);
                            dsb.put(variable, source);
                        } else {
                            FieldInfo fieldInfo = Objects.requireNonNullElseGet(
                                    typeInfoOwner.getFieldByName(name, false), () -> convertType.ensureField(vs));
                            variable = runtime.newFieldReference(fieldInfo);
                            dsb.put(fieldInfo, source);
                        }
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(source.withDetailedSources(dsb.build()))
                                .setVariable(variable)
                                .build();

                    } else throw new UnsupportedOperationException();
                }
                case LOCAL_VARIABLE, PARAMETER, BINDING_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER -> {
                    Variable variable = (Variable) elementStack.find(name);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(source)
                            .setVariable(variable)
                            .build();
                }
                case PACKAGE -> {
                }
                case ENUM, CLASS, INTERFACE, RECORD, ANNOTATION_TYPE -> {
                    if (element instanceof Symbol.ClassSymbol) {
                        List<AnnotationExpression> annots = new ArrayList<>();
                        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                        ParameterizedType type = convertTypeWithAnnotations(node, dsb, annots::add);
                        currentExpression = runtime.newTypeExpressionBuilder()
                                .addAnnotations(annots)
                                .setSource(sourceForNode(node, dsb))
                                .setDiamond(runtime.diamondNo()) // TODO
                                .setParameterizedType(type)
                                .build();
                    } else throw new UnsupportedOperationException("NYI");
                }
                case ENUM_CONSTANT -> {
                    if (element instanceof Symbol.VarSymbol vs) {
                        FieldInfo fieldInfo = typeData.getOrLoadField(vs);
                        currentExpression = runtime.newVariableExpressionBuilder()
                                .setSource(source)
                                .setVariable(runtime.newFieldReference(fieldInfo))
                                .build();
                    } else throw new UnsupportedOperationException("NYI");
                }
                case TYPE_PARAMETER -> {
                    if (element instanceof Symbol.TypeVariableSymbol tvs) {
                        TypeParameter typeParameter = (TypeParameter) elementStack.find(tvs.name.toString());
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
        Expression evaluatedScope = currentExpression;
        MethodInfo method;
        ParameterizedType concreteFunctionalType;
        ParameterizedType concreteReturnType;
        List<ParameterizedType> concreteParameterTypes;
        Expression scope;

        if (mr.sym instanceof Symbol.MethodSymbol ms) {
            if (ms.isConstructor() && "Array".equals(ms.owner.getQualifiedName().toString())) {
                // array construction
                concreteReturnType = evaluatedScope.parameterizedType().copyWithArrays(1);
                scope = runtime.newTypeExpressionBuilder().setDiamond(runtime.diamondNo())
                        .setSource(evaluatedScope.source())
                        .addComments(evaluatedScope.comments())
                        .setParameterizedType(concreteReturnType).build();
                method = runtime.newArrayCreationConstructor(concreteReturnType);
                TypeElement typeElement = elements.getTypeElement(IntFunction.class.getCanonicalName());
                TypeInfo intFunction = convertType.convert(((Symbol.ClassSymbol) typeElement).type).typeInfo();
                concreteFunctionalType = runtime.newParameterizedType(intFunction, List.of(concreteReturnType));
                concreteParameterTypes = List.of(runtime.intParameterizedType());
            } else {
                method = typeData.getOrLoadMethod(ms);
                concreteFunctionalType = convertType.convert(mr.type);
                Type.MethodType instantiatedSam = (Type.MethodType) types.findDescriptorType(mr.type);
                Type returnType = instantiatedSam.getReturnType();
                List<Type> paramTypes = instantiatedSam.getParameterTypes();
                // Thrown types: List<Type> thrownTypes = instantiatedSam.getThrownTypes();
                scope = evaluatedScope;
                concreteReturnType = convertType.convert(returnType);
                concreteParameterTypes = paramTypes.stream().map(convertType::convert).toList();
            }
        } else throw new UnsupportedOperationException();

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
                ParameterizedType realType = convertType.convertTree(fieldAccess.selected, dsb);
                currentExpression = runtime.newClassExpressionBuilder(realType)
                        .setSource(sourceForNode(node, dsb))
                        .setClassType(classType).build();
                return null;
            }

            // static field access, no need to generate a TypeExpression
            if (fieldAccess.sym instanceof Symbol.VarSymbol vs) {
                currentExpression = null;
                scan(fieldAccess.getExpression(), unused);
                assert currentExpression != null;
                Expression scope = currentExpression;
                ParameterizedType concreteType = convertType.convert(fieldAccess.type);
                String fieldName = vs.name.toString();
                boolean isSuper = false;
                Source source = sourceForNode(node);
                DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                String s = node.toString();
                Source sourceName = source.ofIndex(s, s.lastIndexOf('.') + 1, fieldName.length());
                dsb.put(fieldName, sourceName);
                if ("length".equals(fieldName) && scope.parameterizedType().arrays() > 0) {
                    currentExpression = runtime.newArrayLengthBuilder()
                            .setSource(source.withDetailedSources(dsb.build()))
                            .setSource(source)
                            .setExpression(scope)
                            .build();
                } else if ("this".equals(fieldName) || (isSuper = "super".equals(fieldName))) {
                    ParameterizedType explicitType = convertType.convertTree(fieldAccess.selected, dsb);
                    Variable thisVar = runtime.newThis(explicitType, explicitType.typeInfo(), isSuper);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(source.withDetailedSources(dsb.build()))
                            .setVariable(thisVar)
                            .build();
                } else {
                    FieldInfo fieldInfo = typeData.getOrLoadField(vs);
                    FieldReference fr = runtime.newFieldReference(fieldInfo, scope, concreteType);
                    dsb.put(fieldInfo, sourceName);
                    currentExpression = runtime.newVariableExpressionBuilder()
                            .setSource(source.withDetailedSources(dsb.build()))
                            .setVariable(fr).build();
                }
                return null;
            }
            if (fieldAccess.sym instanceof Symbol.ClassSymbol) {
                DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                ParameterizedType type = convertType.convertTree(node, dsb);
                currentExpression = runtime.newTypeExpressionBuilder()
                        .setParameterizedType(type)
                        .setDiamond(runtime.diamondNo())
                        .setSource(sourceForNode(node, dsb))
                        .build();
                return null;
            }
        }
        super.visitMemberSelect(node, unused);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        JCTree.JCMethodInvocation methodInvocation = (JCTree.JCMethodInvocation) node;
        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();

        ExpressionTree methodSelect = node.getMethodSelect();
        Expression object;
        String methodName;
        boolean objectIsImplicit;
        boolean explicitConstructorInvocation;
        ParameterizedType concreteReturnType;
        MethodInfo methodInfo;

        if (methodSelect instanceof IdentifierTree it) {
            TypeInfo currentType = typeStack.getLast();
            methodName = it.getName().toString();
            if (it instanceof JCTree.JCIdent jcIdent && jcIdent.sym instanceof Symbol.MethodSymbol methodSymbol) {
                methodInfo = typeData.getOrLoadMethod(methodSymbol);

                if ("super".equals(methodName) || "this".equals(methodName)) {
                    explicitConstructorInvocation = true;
                    object = null;
                    concreteReturnType = runtime.parameterizedTypeReturnTypeOfConstructor();
                } else {
                    if (methodInfo.isStatic()) {
                        object = runtime.newTypeExpressionBuilder()
                                .setParameterizedType(methodInfo.typeInfo().asSimpleParameterizedType())
                                .setDiamond(runtime.diamondNo())
                                .build();
                    } else {
                        object = runtime.newVariableExpressionBuilder()
                                .setVariable(runtime.newThis(currentType.asParameterizedType()))
                                .setSource(runtime.noSource()).build();
                    }
                    concreteReturnType = convertType.convert(methodInvocation.type);
                    explicitConstructorInvocation = false;
                }
                objectIsImplicit = true;
            } else throw new UnsupportedOperationException("NYI");
        } else if (methodSelect instanceof MemberSelectTree mst) {
            scan(mst.getExpression(), p);
            object = currentExpression;
            methodName = mst.getIdentifier().toString();
            objectIsImplicit = false;
            concreteReturnType = convertType.convert(methodInvocation.type);
            explicitConstructorInvocation = false;
            if ("clone".equals(methodName) && object.parameterizedType().arrays() > 0) {
                methodInfo = runtime.objectTypeInfo().findUniqueMethod("clone", 0);
            } else if (methodInvocation.meth instanceof JCTree.JCFieldAccess fieldAccess) {
                if (fieldAccess.sym instanceof Symbol.MethodSymbol methodSymbol) {
                    methodInfo = typeData.getOrLoadMethod(methodSymbol);
                } else if (fieldAccess.type instanceof Type.ErrorType) {
                    throw new UnsupportedOperationException("Unresolved method call '" + methodName + "'");
                } else throw new UnsupportedOperationException("NYI");
            } else {
                throw new UnsupportedOperationException("NYI");
            }
        } else throw new UnsupportedOperationException("NYI");

        List<ParameterizedType> typeArguments = node.getTypeArguments().stream()
                .map(expr -> convertType.convertTree(expr, dsb))
                .toList();

        List<Expression> arguments = new ArrayList<>(node.getArguments().size());
        for (var arg : node.getArguments()) {
            currentExpression = null;
            scan(arg, p);
            arguments.add(currentExpression);
        }
        Source src = scanSource(node);
        if (scanResult != null) {
            dsb.putIfNotNull(DetailedSources.END_OF_ARGUMENT_LIST, scanResult.findEndOfArgumentList(src));
            dsb.putListIfNotNull(DetailedSources.ARGUMENT_COMMAS, scanResult.findArgumentCommas(src));
        }
        if (explicitConstructorInvocation) {
            boolean isSuper = "super".equals(methodName);
            boolean isSyntheticSuperCall = isSuper && isSyntheticSuperCall(methodInvocation, compilationUnitTree);
            Source source = isSyntheticSuperCall ? null : statementSourceForNode(node, dsb);
            Statement statement = runtime.newExplicitConstructorInvocationBuilder()
                    .setSynthetic(isSyntheticSuperCall)
                    .setSource(source)
                    .setIsSuper(isSuper)
                    .setMethodInfo(methodInfo)
                    .setParameterExpressions(arguments)
                    .build();
            addStatement(statement);
            currentExpression = null; // as a marker for ExpressionAsStatement
        } else {
            currentExpression = runtime.newMethodCallBuilder()
                    .setSource(sourceForNode(node, dsb))
                    .setObjectIsImplicit(objectIsImplicit)
                    .setObject(object == null ? runtime.newEmptyExpression() : object)
                    .setMethodInfo(methodInfo)
                    .setParameterExpressions(arguments)
                    .setConcreteReturnType(concreteReturnType)
                    .setTypeArguments(typeArguments)
                    .build();
        }
        return null;
    }

    boolean isSyntheticSuperCall(JCTree.JCMethodInvocation call, CompilationUnitTree unit) {
        // A synthetic super() has no corresponding source token —
        // verify by checking what's actually in the source at call.pos
        try {
            CharSequence source = unit.getSourceFile().getCharContent(false);
            int pos = call.pos;

            // Check if "super" actually appears at this position in the source
            if (pos < 0 || pos + 5 > source.length()) return true; // no source = synthetic

            String atPos = source.subSequence(pos, pos + 5).toString();
            return !(atPos.equals("super") || atPos.startsWith("();"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        Expression object;
        if (newClass.encl != null) {
            currentExpression = null;
            scan(newClass.encl, unused);
            object = currentExpression;
        } else {
            object = null;
        }

        List<ParameterizedType> typeArguments = node.getTypeArguments().stream()
                .map(expr -> convertType.convertTree(expr, dsb))
                .toList();

        TypeInfo anonymousType;
        ParameterizedType concreteReturnType;
        MethodInfo constructor;
        Diamond diamond = newClass.clazz instanceof JCTree.JCTypeApply apply
                ? (apply.arguments.isEmpty() ? runtime.diamondYes() : runtime.diamondShowAll()) : runtime.diamondNo();
        if (newClass.def != null) {
            JCTree.JCClassDecl anonBody = newClass.def;
            if (!anonBody.implementing.isEmpty() || anonBody.extending != null) {
                JCTree.JCExpression newTypeExpression = anonBody.extending != null
                        ? anonBody.extending : anonBody.implementing.getFirst();
                concreteReturnType = convertType.convert(newTypeExpression.type);
                constructor = null;
                TypeInfo enclosingType = typeStack.getLast();
                anonymousType = runtime.newAnonymousType(enclosingType, enclosingType.builder().getAndIncrementAnonymousTypes());
                TypeInfo.Builder builder = anonymousType.builder()
                        .setTypeNature(runtime.typeNatureClass())
                        .setAccess(runtime.accessPrivate())
                        .setEnclosingMethod(currentMethod);
                if (concreteReturnType.typeInfo().isInterface()) {
                    builder.setParentClass(runtime.objectParameterizedType())
                            .addInterfaceImplemented(concreteReturnType);
                } else {
                    builder.setParentClass(concreteReturnType);
                }
                MethodInfo enclosingMethod = currentMethod;
                // note that we use the compiler's notation, not ours
                typeData.put(newClass.def.sym.toString(), anonymousType);
                currentMethod = null;
                typeStack.addLast(anonymousType);
                for (JCTree member : anonBody.defs) {
                    if (member instanceof JCTree.JCBlock jcBlock) {
                        // {{ }} extended constructor
                        MethodInfo c2 = runtime.newConstructor(anonymousType);
                        c2.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                                .setSource(sourceForNode(node))
                                .setAccess(runtime.accessPrivate())
                                .addMethodModifier(runtime.methodModifierPrivate())
                                .commitParameters();
                        Block block = parseBlock("-", jcBlock);
                        c2.builder().setMethodBody(block);
                        builder.addConstructor(c2);
                    } else if (!(member instanceof JCTree.JCMethodDecl md && "<init>".equals(md.name.toString()))) {
                        scan(member, unused);
                    } // else: ignore default constructor
                }
                typeStack.removeLast();
                currentMethod = enclosingMethod;
                builder.setSource(sourceForNode(node)).commit();
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            concreteReturnType = convertType.convertTree(newClass.clazz, dsb);
            anonymousType = null;
            if (newClass.constructor instanceof Symbol.MethodSymbol ms) {
                constructor = typeData.getOrLoadMethod(ms);
            } else {
                throw new UnsupportedOperationException(
                        "Compilation error in " + typeStack.getLast() + "? Cannot resolve " + newClass);
            }
        }
        currentExpression = runtime.newConstructorCallBuilder()
                .setObject(object)
                .setSource(sourceForNode(node, dsb))
                .setConstructor(constructor)
                .setDiamond(diamond)
                .setConcreteReturnType(concreteReturnType)
                .setAnonymousClass(anonymousType)
                .setParameterExpressions(arguments)
                .setTypeArguments(typeArguments)
                .build();
        return null;
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree node, Void unused) {
        Tree parent = getCurrentPath().getParentPath().getLeaf();

        scan(node.getExpression(), unused);
        Expression expression = currentExpression;

        boolean isControlFlowParent = switch (parent.getKind()) {
            case IF, WHILE_LOOP, DO_WHILE_LOOP, FOR_LOOP, ENHANCED_FOR_LOOP,
                 SWITCH, SWITCH_EXPRESSION, SYNCHRONIZED -> true;
            default -> false;
        };
        if (!isControlFlowParent) {
            currentExpression = runtime.newEnclosedExpressionBuilder()
                    .setSource(sourceForNode(node))
                    .setExpression(expression)
                    .build();
        }
        return null;
    }

    @Override
    public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
        JCTree.JCSwitchExpression se = (JCTree.JCSwitchExpression) node;
        currentExpression = null;
        scan(se.getExpression(), unused);
        Expression selector = currentExpression;
        List<SwitchEntry> switchEntries = doSwitchEntries(unused, se.cases);
        currentExpression = runtime.newSwitchExpressionBuilder()
                .setSelector(selector)
                .setSource(sourceForNode(node))
                .setParameterizedType(convertType.convert(se.type))
                .addSwitchEntries(switchEntries)
                .build();
        return null;
    }

    private @NotNull List<SwitchEntry> doSwitchEntries(Void unused, List<JCTree.JCCase> cases) {
        List<SwitchEntry> switchEntries = new ArrayList<>();
        int i = 0;
        int n = cases.size();
        for (JCTree.JCCase jcCase : cases) {
            List<Expression> conditions = new ArrayList<>();
            RecordPatternResult recordPatternResult = null;
            for (JCTree.JCCaseLabel caseLabel : jcCase.getLabels()) {
                switch (caseLabel) {
                    case JCTree.JCConstantCaseLabel ccl -> {
                        scan(ccl.getConstantExpression(), unused);
                        Expression constantExpression = currentExpression;
                        conditions.add(constantExpression);
                    }
                    case JCTree.JCDefaultCaseLabel _ -> conditions.add(runtime.newEmptyExpression());
                    case JCTree.JCPatternCaseLabel pcl -> {
                        DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
                        List<AnnotationExpression> annotations = new ArrayList<>();
                        assert recordPatternResult == null : "Not allowed, multiple real record patterns";
                        recordPatternResult = parseRecordPattern(pcl.getPattern(), dsb, annotations);
                    }
                    case null, default -> throw new UnsupportedOperationException("NYI");
                }
            }
            LocalVariable[] newVariablesArray = recordPatternResult == null
                    ? new LocalVariable[0] : recordPatternResult.newVariables.toArray(LocalVariable[]::new);
            boolean haveExtraVariables = newVariablesArray.length > 0;

            Expression whenExpression;
            if (jcCase.getGuard() != null) {
                if (haveExtraVariables) {
                    Map<String, Element> map = elementStack.push();
                    recordPatternResult.newVariables.forEach(lv -> map.put(lv.simpleName(), lv));
                }
                scan(jcCase.getGuard(), unused);
                whenExpression = currentExpression;
                if (haveExtraVariables) {
                    elementStack.pop();
                }
            } else {
                whenExpression = runtime.newEmptyExpression();
            }

            Statement statement;
            String index = StringUtil.pad(i, n);
            if (jcCase.getBody() instanceof JCTree.JCBlock block) {
                statement = parseBlock(index, block, newVariablesArray);
            } else if (jcCase.getBody() instanceof JCTree.JCExpression e) {
                if (haveExtraVariables) {
                    Map<String, Element> map = elementStack.push();
                    recordPatternResult.newVariables.forEach(lv -> map.put(lv.simpleName(), lv));
                }
                scan(e, unused);
                if (haveExtraVariables) {
                    elementStack.pop();
                }
                Expression expression = currentExpression;
                statement = runtime.newExpressionAsStatementBuilder()
                        .setSource(sourceForNode(e).withIndex(statementIndex() + "." + index + ".0"))
                        .setExpression(expression).build();
            } else {
                Block block = parseBlock(index, jcCase.body, newVariablesArray);
                if (jcCase.body instanceof JCTree.JCBlock || block.isEmpty()) {
                    statement = block;
                } else {
                    statement = block.statements().getFirst();
                }
            }

            Source source = sourceForNode(jcCase);
            SwitchEntry switchEntry = runtime.newSwitchEntryBuilder()
                    .setSource(sourceForNode(jcCase))
                    .addComments(commentsForNode(source))
                    .addConditions(conditions)
                    .setStatement(statement)
                    .setWhenExpression(whenExpression)
                    .setPatternVariable(recordPatternResult == null ? null : recordPatternResult.rp)
                    .build();
            switchEntries.add(switchEntry);
            ++i;
        }
        return switchEntries;
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
                operator = runtime.bitwiseXorOperatorInt();
                assign = false;
            }
            case COMPL -> {
                operator = runtime.bitWiseNotOperatorInt();
                assign = false;
            }
            case NEG -> {
                operator = runtime.unaryMinusOperatorInt();
                assign = false;
            }
            case NOT -> {
                operator = runtime.logicalNotOperatorBool();
                assign = false;
            }
            case POS -> {
                operator = runtime.unaryPlusOperatorInt();
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

    private Source sourceOfIdentifier(String identifier, int pos) {
        long line = lineMap.getLineNumber(pos);
        long begin = lineMap.getColumnNumber(pos);
        return runtime.newParserSource("-", (int) line, (int) begin, (int) line,
                (int) (begin + identifier.length() - 1));
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

    private Source scanSource(Tree tree) {
        return sourceForNode(tree, "");
    }

    private Source sourceForNode(Tree node, String index) {
        long startPos = sourcePositions.getStartPosition(compilationUnitTree, node);
        if (startPos == Diagnostic.NOPOS) {
            return runtime.noSource(); // synthetic
        }
        long startLine = lineMap.getLineNumber(startPos);
        long startCol = lineMap.getColumnNumber(startPos);
        long endLine;
        long endCol;
        long endPos = sourcePositions.getEndPosition(compilationUnitTree, node);
        if (endPos == Diagnostic.NOPOS) {
            // quirk in javac, super() call only at the moment
            endLine = startLine;
            endCol = startCol + 4;
        } else {
            endLine = lineMap.getLineNumber(endPos);
            endCol = lineMap.getColumnNumber(endPos) - 1; // we work inclusively
        }
        return runtime.newParserSource(index, (int) startLine, (int) startCol, (int) endLine, (int) endCol);
    }

    private List<Comment> commentsForNode(Source source) {
        return scanResult == null ? List.of() : scanResult.findComments(source);
    }

    private List<Comment> trailingCommentsForNode(Source source) {
        return scanResult == null ? List.of() : scanResult.findTrailingComments(source);
    }
}
