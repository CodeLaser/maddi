package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class IsolateMethod {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolateMethod.class);
    public static final String METHOD_MARKER_NAME = "_method_to_be_replaced_";

    private final JavaInspector javaInspector;
    private final Runtime runtime;
    private final String targetPackage;

    public IsolateMethod(JavaInspector javaInspector, String targetPackage) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
        this.targetPackage = targetPackage;
    }

    public record Result(TypeInfo typeInfo, Set<TypeInfo> jdkTypesToImport) {
    }

    public String print(Result result) {
        ImportComputer importComputer = javaInspector.importComputer(4, javaInspector.mainSources());
        result.jdkTypesToImport.forEach(importComputer::add);
        return javaInspector.print2(result.typeInfo.compilationUnit(), runtime.qualificationSimpleNames(), importComputer);
    }

    public String print(Result result, String methodString) {
        ImportComputer importComputer = javaInspector.importComputer(4, javaInspector.mainSources());
        result.jdkTypesToImport.forEach(importComputer::add);
        CompilationUnit compilationUnit = result.typeInfo.compilationUnit();
        Qualification qualification = runtime.qualificationSimpleNames();
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(importComputer, qualification,
                        runtime::newTypePrinter,
                        (_, mi, f2) -> {
                            if (mi.name().equals(METHOD_MARKER_NAME)) {
                                return q -> runtime.newOutputBuilder().add(runtime.newText(methodString));
                            }
                            return runtime.newMethodPrinter(mi);
                        },
                        runtime::newFieldPrinter,
                        runtime::newTypePrinter);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }

    public Result isolate(MethodInfo methodInfo) {
        TypeInfo originalType = methodInfo.typeInfo().primaryType();
        org.e2immu.language.cst.api.runtime.Runtime runtime = javaInspector.runtime();
        CompilationUnit newCu = runtime.newCompilationUnitBuilder()
                .setPackageName(targetPackage)
                .setSourceSet(originalType.compilationUnit().sourceSet())
                .setURI(originalType.compilationUnit().uri())
                .build();
        TypeInfo frame = runtime
                .newTypeInfo(newCu, originalType.simpleName() + "_" + methodInfo.name());
        frame.builder().setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .computeAccess();

        Data data = new Data(methodInfo, frame);
        visit(data, methodInfo);

        data.fieldMap.values().stream().sorted(Comparator.comparing(FieldInfo::name)).forEach(field -> {
            // add each field to its actual owner (the frame, or a stub type), mirroring how methods are added
            field.owner().builder().addField(field);
        });
        // deepest types first, so a nested stub is committed and added to its (still open) enclosing stub before
        // that enclosing stub is itself committed; includes the package 'namespace' stubs
        List<TypeInfo> allStubs = new ArrayList<>(data.typeMap.values());
        allStubs.addAll(data.namespaceMap.values());
        allStubs.stream()
                .sorted(Comparator.comparingInt(IsolateMethod::enclosingDepth).reversed()
                        .thenComparing(TypeInfo::simpleName))
                .forEach(stub -> {
                    stub.builder().commit();
                    stub.compilationUnitOrEnclosingType().getRight().builder().addSubType(stub);
                });
        MethodInfo methodMarker = runtime.newMethod(frame, METHOD_MARKER_NAME, runtime.methodTypeMethod());
        methodMarker.builder().setSource(runtime.noSource()).setMethodBody(runtime.emptyBlock())
                .setReturnType(runtime.voidParameterizedType()).setAccess(runtime.accessPackage())
                .computeAccess().commit();
        frame.builder()
                .addMethod(methodMarker)
                .commit();
        newCu.setTypes(List.of(frame));
        return new Result(frame, data.jdkTypesToImport);
    }

    class Data {
        private final TypeInfo originalType;
        private final MethodInfo originalMethod;
        private final TypeInfo frame;
        final Map<TypeInfo, TypeInfo> typeMap = new HashMap<>();
        final Map<MethodInfo, MethodInfo> methodMap = new HashMap<>();
        final Map<FieldInfo, FieldInfo> fieldMap = new HashMap<>();
        // original type parameter -> the freshly created one on the corresponding stub type
        final Map<TypeParameter, TypeParameter> typeParameterMap = new HashMap<>();
        final Set<TypeInfo> jdkTypesToImport = new HashSet<>(); // TODO
        // package name -> a stub 'namespace' type reproducing that package, so a type referenced by its fully
        // qualified name (e.g. 'org.slf4j.Logger') keeps resolving and does not collide with a simply-named stub
        final Map<String, TypeInfo> namespaceMap = new HashMap<>();
        // fully-qualified names of single-type imports, and packages of star imports, of the original source
        final Set<String> importedFqns = new HashSet<>();
        final Set<String> starImportPackages = new HashSet<>();
        private final String originalPackage;

        Data(MethodInfo originalMethod, TypeInfo frame) {
            this.originalMethod = originalMethod;
            this.originalType = originalMethod.primaryType();
            this.frame = frame;
            this.originalPackage = originalType.packageName();
            for (ImportStatement is : originalType.compilationUnit().importStatements()) {
                if (is.isStatic()) continue;
                String s = is.importString();
                if (is.isStar()) starImportPackages.add(s.substring(0, s.length() - 2)); // strip ".*"
                else importedFqns.add(s);
            }
        }

        // is the (primary) type referenced by its simple name in the original source? (imported, same package, or
        // a star-imported package); if not, it must have been written out fully qualified
        private boolean referencedSimply(TypeInfo primaryType) {
            return importedFqns.contains(primaryType.fullyQualifiedName())
                   || originalPackage.equals(primaryType.packageName())
                   || starImportPackages.contains(primaryType.packageName());
        }

        // a chain of empty stub types reproducing a package, e.g. "org.slf4j" -> frame.org.slf4j
        private TypeInfo namespaceStub(String packageName) {
            if (packageName.isEmpty()) return frame;
            TypeInfo existing = namespaceMap.get(packageName);
            if (existing != null) return existing;
            int lastDot = packageName.lastIndexOf('.');
            TypeInfo parent = namespaceStub(lastDot < 0 ? "" : packageName.substring(0, lastDot));
            String segment = lastDot < 0 ? packageName : packageName.substring(lastDot + 1);
            TypeInfo ns = runtime.newTypeInfo(parent, segment);
            ns.builder().setParentClass(runtime.objectParameterizedType())
                    .setTypeNature(runtime.typeNatureClass())
                    .setSource(runtime.noSource())
                    .setAccess(runtime.accessPackage());
            namespaceMap.put(packageName, ns);
            return ns;
        }

        private TypeInfo ensureType(TypeInfo typeInfo) {
            if (typeInfo.isPrimitive()
                || typeInfo == originalType
                // already one of the new types
                || typeInfo.compilationUnitOrEnclosingType().isRight() && frame == typeInfo.compilationUnitOrEnclosingType().getRight()) {
                return typeInfo;// keep as is
            }
            if (typeInfo.packageName().startsWith("java.")) {
                if (!typeInfo.packageName().equals("java.lang")) {
                    jdkTypesToImport.add(typeInfo);
                }
                return typeInfo; // keep as is
            }
            TypeInfo inMap = typeMap.get(typeInfo);
            if (inMap != null) return inMap;
            LOGGER.info("Creating type {}", typeInfo);
            // reproduce the nesting: a member type 'Ext.Pair' becomes 'Pair' nested in the stub of 'Ext', so that
            // qualified references in the (verbatim) method text keep resolving. Types nested in the original type,
            // and primary types, are nested directly in the frame.
            var enclosing = typeInfo.compilationUnitOrEnclosingType();
            TypeInfo enclosingStub;
            if (enclosing.isRight()) {
                // a member type: nest in the frame (if nested in the original type) or in the enclosing type's stub
                enclosingStub = enclosing.getRight() == originalType ? frame : ensureType(enclosing.getRight());
            } else {
                // a primary type: nest directly in the frame when it is referenced by its simple name, otherwise
                // reproduce its package as a chain of namespace stubs so the qualified reference still resolves
                enclosingStub = referencedSimply(typeInfo) ? frame : namespaceStub(typeInfo.packageName());
            }
            TypeInfo stub = runtime.newTypeInfo(enclosingStub, typeInfo.simpleName());
            typeMap.put(typeInfo, stub); // before recursion: type bounds / fields may refer back to this stub
            // reproduce the real parent class only for exceptions, so the stub stays a Throwable subtype and the
            // pasted method text ('throw new ...', 'throws ...') still compiles; everything else defaults to Object
            // (a verbatim 'extends Record'/'extends Enum' would not compile, and other supertypes are not needed)
            ParameterizedType parentClass = extendsThrowable(typeInfo)
                    ? ensureTypes(typeInfo.parentClass()) : runtime.objectParameterizedType();
            stub.builder().setParentClass(parentClass)
                    .setTypeNature(runtime.typeNatureClass())
                    .setSource(runtime.noSource())
                    .setAccess(runtime.accessPackage());
            // reproduce the type parameters, so 'Box<T>' becomes a generic stub 'class Box<T>'. Two passes: first
            // create+map all of them (a bound may reference a sibling or this type itself), then translate bounds.
            List<TypeParameter> origTps = typeInfo.typeParameters();
            List<TypeParameter> newTps = new ArrayList<>(origTps.size());
            for (TypeParameter origTp : origTps) {
                TypeParameter newTp = runtime.newTypeParameter(origTp.getIndex(), origTp.simpleName(), stub);
                typeParameterMap.put(origTp, newTp);
                stub.builder().addOrSetTypeParameter(newTp);
                newTps.add(newTp);
            }
            for (int i = 0; i < newTps.size(); i++) {
                List<ParameterizedType> newBounds = origTps.get(i).typeBounds().stream()
                        .map(this::ensureTypes).toList();
                newTps.get(i).builder().setTypeBounds(newBounds).commit();
            }
            return stub;
        }

        private ParameterizedType ensureTypes(ParameterizedType pt) {
            if (pt.isPrimitiveExcludingVoid() || pt.typeInfo() != null && pt.typeInfo().isPrimitive()) return pt;
            if (pt.isReturnTypeOfConstructor()) return pt;

            if (pt.typeParameter() != null) {
                TypeParameter origTp = pt.typeParameter();
                // make sure the owning type is stubbed, which populates typeParameterMap for its parameters
                if (origTp.getOwner().isLeft()) {
                    ensureType(origTp.getOwner().getLeft());
                }
                TypeParameter newTp = typeParameterMap.get(origTp);
                if (newTp != null) {
                    return runtime.newParameterizedType(newTp, pt.arrays(), pt.wildcard());
                }
                // a type parameter of the isolated method (or of a kept type): it appears only in the pasted
                // method text, not in the reconstructed model, so keep it as is
                return pt;
            }
            if (pt.typeInfo() == null) return pt;
            TypeInfo newTypeInfo = ensureType(pt.typeInfo());
            List<ParameterizedType> params = pt.parameters().stream().map(this::ensureTypes).toList();
            return runtime.newParameterizedType(newTypeInfo, pt.arrays(), pt.wildcard(), params);
        }

        private void ensureField(TypeInfo owner, FieldInfo fieldInfo) {
            if (owner.packageName().startsWith("java.")) {
                if (!owner.packageName().equals("java.lang")) {
                    jdkTypesToImport.add(owner);
                }
                return; // a field on a JDK type: do not stub (and never mutate the real, committed type)
            }
            FieldInfo inMap = fieldMap.get(fieldInfo);
            if (inMap != null) return;
            ParameterizedType newPt = ensureTypes(fieldInfo.type());
            FieldInfo newField = runtime.newFieldInfo(fieldInfo.name(), fieldInfo.isStatic(), newPt, owner);
            FieldInfo.Builder fieldBuilder = newField.builder().setInitializer(runtime.newEmptyExpression())
                    .setAccess(runtime.accessPackage());
            if (fieldInfo.isStatic()) fieldBuilder.addFieldModifier(runtime.fieldModifierStatic());
            fieldBuilder.commit();
            fieldMap.put(fieldInfo, newField);
        }

        private void ensureMethodInfo(TypeInfo owner, MethodInfo methodInfo) {
            if (owner.packageName().startsWith("java.")) {
                if (!owner.packageName().equals("java.lang")) {
                    jdkTypesToImport.add(owner);
                }
                return;
            }
            MethodInfo inMap = methodMap.get(methodInfo);
            if (inMap != null) return;
            MethodInfo newMethod = runtime.newMethod(owner, methodInfo.name(),
                    methodInfo.isStatic() ? runtime.methodTypeStaticMethod() :
                            methodInfo.isConstructor() ? runtime.methodTypeConstructor() : runtime.methodTypeMethod());
            // reproduce the method's own type parameters, so a called generic method '<X> X foo(X x)' keeps its
            // <X> (and ensureTypes below can translate occurrences of X). Two passes for self-referential bounds.
            List<TypeParameter> origMethodTps = methodInfo.typeParameters();
            List<TypeParameter> newMethodTps = new ArrayList<>(origMethodTps.size());
            for (TypeParameter origTp : origMethodTps) {
                TypeParameter newTp = runtime.newTypeParameter(origTp.getIndex(), origTp.simpleName(), newMethod);
                typeParameterMap.put(origTp, newTp);
                newMethod.builder().addTypeParameter(newTp);
                newMethodTps.add(newTp);
            }
            for (int i = 0; i < newMethodTps.size(); i++) {
                List<ParameterizedType> newBounds = origMethodTps.get(i).typeBounds().stream()
                        .map(this::ensureTypes).toList();
                newMethodTps.get(i).builder().setTypeBounds(newBounds).commit();
            }
            methodInfo.parameters().forEach(pi -> {
                ParameterizedType newType = ensureTypes(pi.parameterizedType());
                ParameterInfo newParam = newMethod.builder().addParameter(pi.name(), newType);
                newParam.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
            });
            Block.Builder mb = runtime.newBlockBuilder();
            ParameterizedType newReturnType = ensureTypes(methodInfo.returnType());
            if (!methodInfo.isConstructor() && !methodInfo.returnType().isVoid()) {
                Expression expression = runtime.nullValue(newReturnType);
                mb.addStatement(runtime.newReturnBuilder().setExpression(expression).build());
            }
            newMethod.builder()
                    .setReturnType(newReturnType)
                    .setAccess(runtime.accessPackage())
                    .setSource(runtime.noSource())
                    .computeAccess()
                    .setMethodBody(mb.build())
                    .commit();
            LOGGER.info("Adding method {}", newMethod);
            owner.builder().addMethod(newMethod);
            methodMap.put(methodInfo, newMethod);
        }
    }

    private class MyVisitor implements Predicate<Element> {
        private final Data data;

        private MyVisitor(Data data) {
            this.data = data;
        }

        @Override
        public boolean test(Element element) {
            switch (element) {
                case TypeExpression te -> data.ensureTypes(te.parameterizedType());
                case LocalVariableCreation lvc -> data.ensureTypes(lvc.localVariable().parameterizedType());
                case InstanceOf instanceOf -> data.ensureTypes(instanceOf.testType());
                case Cast cast -> data.ensureTypes(cast.parameterizedType());
                case ClassExpression classExpression -> data.ensureTypes(classExpression.parameterizedType());
                case Lambda lambda -> {
                    for (ParameterInfo pi : lambda.parameters()) {
                        data.ensureTypes(pi.parameterizedType());
                    }
                    lambda.methodBody().visit(this);
                }
                case ConstructorCall cc -> {
                    if (cc.anonymousClass() != null) {
                        cc.anonymousClass().visit(this);
                    }
                    if (cc.constructor() != null) {
                        // stub the constructed type first, so the constructor lands on the stub, not the real type
                        ParameterizedType constructed = data.ensureTypes(cc.parameterizedType());
                        data.ensureMethodInfo(constructed.typeInfo(), cc.constructor());
                    }
                }
                case MethodCall mc -> {
                    TypeInfo owner;
                    if (mc.object() == null
                        || mc.methodInfo().isStatic() && mc.methodInfo().typeInfo() == data.originalType
                        || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = data.frame;
                    } else {
                        ParameterizedType firstOwner = data.ensureTypes(mc.object().parameterizedType());
                        owner = firstOwner.typeInfo();
                    }
                    data.ensureMethodInfo(owner, mc.methodInfo());
                }
                case MethodReference mr -> {
                    // 'scope::method' (or 'Type::new'): the referenced method must be stubbed just like a call.
                    TypeInfo owner;
                    if (mr.methodInfo().typeInfo() == data.originalType
                        || mr.scope() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = data.frame;
                    } else {
                        ParameterizedType scopeType = data.ensureTypes(mr.scope().parameterizedType());
                        owner = scopeType.typeInfo();
                    }
                    data.ensureMethodInfo(owner, mr.methodInfo());
                }
                case VariableExpression ve -> {
                    if (ve.variable() instanceof FieldReference fr) {
                        TypeInfo owner;
                        if (fr.isDefaultScope()) {
                            owner = data.frame;
                        } else {
                            ParameterizedType realOwner = data.ensureTypes(fr.scope().parameterizedType());
                            owner = realOwner.typeInfo();
                        }
                        data.ensureField(owner, fr.fieldInfo());
                    }
                }
                default -> {

                }
            }
            return true;
        }
    }

    // does this type (transitively) extend java.lang.Throwable? (Throwable itself returns false: its parent is Object)
    private static boolean extendsThrowable(TypeInfo typeInfo) {
        ParameterizedType parent = typeInfo.parentClass();
        while (parent != null && parent.typeInfo() != null) {
            TypeInfo pt = parent.typeInfo();
            if ("java.lang.Throwable".equals(pt.fullyQualifiedName())) return true;
            parent = pt.parentClass();
        }
        return false;
    }

    // number of enclosing types up to the compilation unit (a primary type has depth 0)
    private static int enclosingDepth(TypeInfo typeInfo) {
        int depth = 0;
        var e = typeInfo.compilationUnitOrEnclosingType();
        while (e.isRight()) {
            depth++;
            e = e.getRight().compilationUnitOrEnclosingType();
        }
        return depth;
    }

    private void visit(Data data, MethodInfo methodInfo) {
        for (ParameterInfo pi : methodInfo.parameters()) {
            data.ensureTypes(pi.parameterizedType());
        }
        if (methodInfo.hasReturnValue()) data.ensureTypes(methodInfo.returnType());

        MyVisitor myVisitor = new MyVisitor(data);
        methodInfo.methodBody().visit(myVisitor);
    }

}
