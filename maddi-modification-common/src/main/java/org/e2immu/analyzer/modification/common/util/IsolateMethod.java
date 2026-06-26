package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class IsolateMethod {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolateMethod.class);

    private final JavaInspector javaInspector;
    private final Runtime runtime;

    public IsolateMethod(JavaInspector javaInspector) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
    }

    public record Result(TypeInfo typeInfo, Set<TypeInfo> jdkTypesToImport) {
    }

    public String print(Result result) {
        ImportComputer importComputer = javaInspector.importComputer(4, javaInspector.mainSources());
        result.jdkTypesToImport.forEach(importComputer::add);
        return javaInspector.print2(result.typeInfo.compilationUnit(), runtime.qualificationSimpleNames(), importComputer);
    }

    public Result isolate(MethodInfo methodInfo) {
        TypeInfo originalType = methodInfo.typeInfo().primaryType();
        org.e2immu.language.cst.api.runtime.Runtime runtime = javaInspector.runtime();
        CompilationUnit newCu = runtime.newCompilationUnitBuilder()
                .setPackageName("")
                .setSourceSet(originalType.compilationUnit().sourceSet())
                .setURI(originalType.compilationUnit().uri())
                .build();
        TypeInfo frame = runtime.newTypeInfo(newCu, originalType.simpleName() + "_"
                                                    + methodInfo.name());
        Data data = new Data(frame);
        visit(data, methodInfo);

        data.fieldMap.values().stream().sorted(Comparator.comparing(FieldInfo::name)).forEach(field -> {
            frame.builder().addField(field);
        });
        data.typeMap.values().stream().sorted(Comparator.comparing(TypeInfo::simpleName)).forEach(stub -> {
            stub.builder()
                    .commit();
            frame.builder().addSubType(stub);
        });
        frame.builder()
                .setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .computeAccess()
                .commit();
        newCu.setTypes(List.of(frame));
        return new Result(frame, data.jdkTypesToImport);
    }

    class Data {
        private final TypeInfo frame;
        final Map<TypeInfo, TypeInfo> typeMap = new HashMap<>();
        final Map<MethodInfo, MethodInfo> methodMap = new HashMap<>();
        final Map<FieldInfo, FieldInfo> fieldMap = new HashMap<>();
        final Set<TypeParameter> typeParameters = new HashSet<>();
        final Set<TypeInfo> jdkTypesToImport = new HashSet<>(); // TODO

        Data(TypeInfo frame) {
            this.frame = frame;
        }

        private TypeInfo ensureType(TypeInfo typeInfo) {
            if (typeInfo.isPrimitive()
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
            TypeInfo stub = runtime.newTypeInfo(frame, typeInfo.simpleName());
            stub.builder().setParentClass(runtime.objectParameterizedType())
                    .setTypeNature(runtime.typeNatureClass())
                    .setSource(runtime.noSource())
                    .setAccess(runtime.accessPackage());
            typeMap.put(typeInfo, stub);
            return stub;
        }

        private ParameterizedType ensureTypes(ParameterizedType pt) {
            if (pt.isPrimitiveExcludingVoid() || pt.typeInfo() != null && pt.typeInfo().isPrimitive()) return pt;
            if (pt.isReturnTypeOfConstructor()) return pt;

            if (pt.typeParameter() != null) {
                typeParameters.add(pt.typeParameter());
                for (ParameterizedType bound : pt.typeParameter().typeBounds()) {
                    ensureTypes(bound);
                }
                throw new UnsupportedOperationException("NYI: " + pt);
            }
            if (pt.typeInfo() == null) return pt;
            TypeInfo newTypeInfo = ensureType(pt.typeInfo());
            List<ParameterizedType> params = pt.parameters().stream().map(this::ensureTypes).toList();
            return runtime.newParameterizedType(newTypeInfo, pt.arrays(), pt.wildcard(), params);
        }

        private void ensureField(TypeInfo owner, FieldInfo fieldInfo) {
            FieldInfo inMap = fieldMap.get(fieldInfo);
            if (inMap != null) return;
            ParameterizedType newPt = ensureTypes(fieldInfo.type());
            FieldInfo newField = runtime.newFieldInfo(fieldInfo.name(), fieldInfo.isStatic(), newPt, owner);
            newField.builder().setInitializer(runtime.newEmptyExpression())
                    .setAccess(runtime.accessPackage())
                    .commit();
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
                    methodInfo.isConstructor() ? runtime.methodTypeConstructor() : runtime.methodTypeMethod());
            methodInfo.parameters().forEach(pi -> {
                ParameterizedType newType = ensureTypes(pi.parameterizedType());
                ParameterInfo newParam = newMethod.builder().addParameter(pi.name(), newType);
                newParam.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
            });
            Block.Builder mb = runtime.newBlockBuilder();
            ParameterizedType newReturnType = ensureTypes(methodInfo.returnType());
            if (!methodInfo.isConstructor()) {
                Expression expression = methodInfo.returnType().isVoid()
                        ? runtime.newEmptyExpression() : runtime.nullValue(newReturnType);
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
                        data.ensureMethodInfo(cc.constructor().typeInfo(), cc.constructor());
                    }
                }
                case MethodCall mc -> {
                    TypeInfo owner;
                    if (mc.object() == null || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = data.frame;
                    } else {
                        ParameterizedType firstOwner = data.ensureTypes(mc.object().parameterizedType());
                        owner = firstOwner.typeInfo();
                    }
                    data.ensureMethodInfo(owner, mc.methodInfo());
                }
                case VariableExpression ve -> {
                    if (ve.variable() instanceof FieldReference fr) {
                        TypeInfo owner;
                        if (fr.isDefaultScope()) {
                            if (fr.scopeIsThis()) {
                                owner = data.frame;
                            } else {
                                throw new UnsupportedOperationException("TODO static import of field");
                            }
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

    private void visit(Data data, MethodInfo methodInfo) {
        for (ParameterInfo pi : methodInfo.parameters()) {
            data.ensureTypes(pi.parameterizedType());
        }
        if (methodInfo.hasReturnValue()) data.ensureTypes(methodInfo.returnType());

        MyVisitor myVisitor = new MyVisitor(data);
        methodInfo.methodBody().visit(myVisitor);
    }

}
