package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
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
        // the @Override supertype (if any) is the sibling top-level type; print its method qualifying from the primary
        // type, so the frame-nested types it references resolve as 'Frame.Nested' (simple names everywhere else)
        TypeInfo overrideSuper = compilationUnit.types().stream()
                .filter(t -> t != result.typeInfo).findFirst().orElse(null);
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(importComputer, qualification,
                        runtime::newTypePrinter,
                        (_, mi, _) -> {
                            if (mi.name().equals(METHOD_MARKER_NAME)) {
                                return _ -> runtime.newOutputBuilder().add(runtime.newText(methodString));
                            }
                            if (overrideSuper != null && mi.typeInfo() == overrideSuper) {
                                return _ -> runtime.newMethodPrinter(mi)
                                        .print(runtime.qualificationQualifyFromPrimaryType());
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
        // if the method carries @Override it must override something, or the isolated frame does not compile; give it
        // an abstract supertype declaring the same method so the verbatim @Override stays valid (must run before the
        // frame is committed, as it changes the frame's parent class)
        TypeInfo overrideSuper = isOverride(methodInfo) ? createOverrideSupertype(data, methodInfo, newCu, frame) : null;
        frame.builder()
                .addMethod(methodMarker)
                .commit();
        newCu.setTypes(overrideSuper == null ? List.of(frame) : List.of(overrideSuper, frame));
        return new Result(frame, data.jdkTypesToImport);
    }

    private static boolean isOverride(MethodInfo methodInfo) {
        return methodInfo.annotations().stream()
                .anyMatch(ae -> "java.lang.Override".equals(ae.typeInfo().fullyQualifiedName()));
    }

    // an abstract class 'X_method_super' declaring the isolated method as abstract (signature reproduced with the
    // frame's stubbed types); the frame extends it, so an '@Override' on the method resolves to a real supertype method
    private TypeInfo createOverrideSupertype(Data data, MethodInfo methodInfo, CompilationUnit newCu, TypeInfo frame) {
        TypeInfo superType = runtime.newTypeInfo(newCu, frame.simpleName() + "_super");
        superType.builder().setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                // package-private: a compilation unit may have only one public top-level type (the frame)
                .addTypeModifier(runtime.typeModifierAbstract())
                .computeAccess();
        MethodInfo abstractMethod = runtime.newMethod(superType, methodInfo.name(),
                runtime.methodTypeAbstractMethod());
        methodInfo.parameters().forEach(pi -> {
            ParameterInfo np = abstractMethod.builder().addParameter(pi.name(), data.ensureTypes(pi.parameterizedType()));
            np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
        });
        abstractMethod.builder()
                .setReturnType(data.ensureTypes(methodInfo.returnType()))
                .setAccess(methodInfo.access())
                .addMethodModifier(runtime.methodModifierAbstract())
                .setSource(runtime.noSource())
                // empty (not null) body: printed as ';' since the method is abstract, but a null body would trip
                // the import computer's methodBody().typesReferenced()
                .setMethodBody(runtime.emptyBlock())
                .computeAccess()
                .commit();
        superType.builder().addMethod(abstractMethod).commit();
        frame.builder().setParentClass(superType.asSimpleParameterizedType());
        return superType;
    }

    class Data {
        private final TypeInfo originalType;
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
        // simple names of the original type's member types: they are always referenced simply, so they get first
        // claim on a frame slot; an external type with the same simple name must fall back to a namespace stub
        final Set<String> originalMemberSimpleNames = new HashSet<>();
        // simple name -> the (original) primary type that occupies that slot in the frame; used to detect a genuine
        // simple-name collision, the situation that in source forces a fully-qualified (namespace) reference
        final Map<String, TypeInfo> frameSimpleNameClaims = new HashMap<>();
        // stub types of interface nature: a method added to one must be 'default' (not abstract), so that classes
        // implementing the interface need not override it
        final Set<TypeInfo> interfaceStubs = new HashSet<>();

        Data(MethodInfo originalMethod, TypeInfo frame) {
            this.originalType = originalMethod.primaryType();
            this.frame = frame;
            originalType.recursiveSubTypeStream()
                    .filter(t -> t != originalType)
                    .forEach(t -> originalMemberSimpleNames.add(t.simpleName()));
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

        // 'ds' are the detailed sources of the element that references 'typeInfo' in the pasted method text (or null
        // if unavailable); they tell us how the reference was written, which decides where to place a primary type.
        private TypeInfo ensureType(TypeInfo typeInfo, DetailedSources ds) {
            if (typeInfo.isPrimitive() || typeInfo == originalType || isJdkType(typeInfo)) {
                return typeInfo;// keep as is
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
                enclosingStub = enclosing.getRight() == originalType ? frame : ensureType(enclosing.getRight(), ds);
            } else if (ds != null && ds.detail(typeInfo.packageName()) != null) {
                // detailed sources say the package was written out (fully-qualified, e.g. 'a.b.C'): reproduce its
                // package as a chain of namespace stubs so the qualified reference still resolves
                enclosingStub = namespaceStub(typeInfo.packageName());
            } else if (ds != null) {
                // detailed sources present and the package was not written: referenced simply -> nest in the frame
                frameSimpleNameClaims.put(typeInfo.simpleName(), typeInfo);
                enclosingStub = frame;
            } else {
                // no detailed sources: nest a primary type directly in the frame under its simple name -- the common
                // case. Fall back to a chain of namespace stubs only on a genuine simple-name collision: when a member
                // type of the original owns the slot, or another primary type already claimed it. That collision is
                // exactly what forces a fully-qualified reference in source (e.g. 'org.slf4j.Logger' next to a nested
                // 'Logger'), which the namespace chain resolves.
                String simpleName = typeInfo.simpleName();
                TypeInfo claimer = frameSimpleNameClaims.get(simpleName);
                boolean collision = originalMemberSimpleNames.contains(simpleName)
                                    || claimer != null && claimer != typeInfo;
                if (collision) {
                    enclosingStub = namespaceStub(typeInfo.packageName());
                } else {
                    frameSimpleNameClaims.put(simpleName, typeInfo);
                    enclosingStub = frame;
                }
            }
            TypeInfo stub = runtime.newTypeInfo(enclosingStub, typeInfo.simpleName());
            typeMap.put(typeInfo, stub); // before recursion: type bounds / fields may refer back to this stub
            boolean isInterface = typeInfo.isInterface() && !typeInfo.isAnnotation();
            if (isInterface) interfaceStubs.add(stub);
            stub.builder().setParentClass(reproducedParentClass(typeInfo))
                    // reproduce the nature: an annotation must stay '@interface' (a use '@Marker' would not compile),
                    // an interface must stay 'interface' (so subtypes 'implements'/'extends' it and overload
                    // resolution / generic bounds in the pasted text resolve as in the original); everything else is
                    // a class
                    .setTypeNature(typeInfo.isAnnotation() ? runtime.typeNatureAnnotation()
                            : isInterface ? runtime.typeNatureInterface()
                            : runtime.typeNatureClass())
                    .setSource(runtime.noSource())
                    .setAccess(runtime.accessPackage());
            if (typeInfo.isAnnotation()) {
                // an annotation type must implement java.lang.annotation.Annotation (asserted on commit)
                TypeInfo annotation = javaInspector.compiledTypesManager()
                        .getOrLoad(java.lang.annotation.Annotation.class);
                stub.builder().addInterfaceImplemented(annotation.asSimpleParameterizedType());
            } else {
                // reproduce implemented/extended interfaces, so the subtype edges they create hold in the stubs
                for (ParameterizedType itf : typeInfo.interfacesImplemented()) {
                    stub.builder().addInterfaceImplemented(ensureTypes(itf));
                }
            }
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

        // reproduce the real superclass so the subtype chain holds (e.g. a custom exception keeps 'extends Exception',
        // a 'Dog' keeps 'extends Animal'); but 'extends Record'/'extends Enum' are compiler-managed and illegal to
        // write, and an interface has no superclass, so those default to Object
        private ParameterizedType reproducedParentClass(TypeInfo typeInfo) {
            ParameterizedType parent = typeInfo.parentClass();
            if (parent == null || parent.isJavaLangObject()) return runtime.objectParameterizedType();
            TypeInfo pt = parent.typeInfo();
            if (pt != null) {
                String fqn = pt.fullyQualifiedName();
                if ("java.lang.Record".equals(fqn) || "java.lang.Enum".equals(fqn)) {
                    return runtime.objectParameterizedType();
                }
            }
            return ensureTypes(parent);
        }

        private ParameterizedType ensureTypes(ParameterizedType pt) {
            return ensureTypes(pt, null);
        }

        private ParameterizedType ensureTypes(ParameterizedType pt, DetailedSources ds) {
            if (pt.isPrimitiveExcludingVoid() || pt.typeInfo() != null && pt.typeInfo().isPrimitive()) return pt;
            if (pt.isReturnTypeOfConstructor()) return pt;

            if (pt.typeParameter() != null) {
                TypeParameter origTp = pt.typeParameter();
                // make sure the owning type is stubbed, which populates typeParameterMap for its parameters
                if (origTp.getOwner().isLeft()) {
                    ensureType(origTp.getOwner().getLeft(), null);
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
            // type arguments share the referencing element, hence the same detailed sources
            TypeInfo newTypeInfo = ensureType(pt.typeInfo(), ds);
            List<ParameterizedType> params = pt.parameters().stream().map(p -> ensureTypes(p, ds)).toList();
            return runtime.newParameterizedType(newTypeInfo, pt.arrays(), pt.wildcard(), params);
        }

        private void ensureField(TypeInfo owner, FieldInfo fieldInfo) {
            if (isJdkType(owner)) return;
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
            if (isJdkType(owner)) return;
            MethodInfo inMap = methodMap.get(methodInfo);
            if (inMap != null) return;
            // a non-static method on an interface stub becomes 'default' (keeps the body): an abstract method would
            // force every implementing class stub to override it
            boolean ownerIsInterface = interfaceStubs.contains(owner);
            MethodInfo newMethod = runtime.newMethod(owner, methodInfo.name(),
                    methodInfo.isStatic() ? runtime.methodTypeStaticMethod() :
                            methodInfo.isConstructor() ? runtime.methodTypeConstructor() :
                                    ownerIsInterface ? runtime.methodTypeDefaultMethod() : runtime.methodTypeMethod());
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
                    .setAccess(ownerIsInterface ? runtime.accessPublic() : runtime.accessPackage())
                    .setSource(runtime.noSource())
                    .computeAccess()
                    .setMethodBody(mb.build())
                    .commit();
            LOGGER.info("Adding method {}", newMethod);
            owner.builder().addMethod(newMethod);
            methodMap.put(methodInfo, newMethod);
        }

        private boolean isJdkType(TypeInfo owner) {
            if (owner.packageName() == null || owner.packageName().startsWith("java.")) {
                if (owner.packageName() != null && !owner.packageName().equals("java.lang")) {
                    jdkTypesToImport.add(owner);
                }
                return true;
            }
            return false;
        }

        // an annotation present in the pasted text ('@Marker', '@Named("x")') needs its '@interface' stubbed, plus
        // every attribute it actually uses, plus any types its attribute values reference (e.g. 'SomeClass.class')
        private void ensureAnnotations(Element annotated, MyVisitor visitor) {
            for (AnnotationExpression ae : annotated.annotations()) {
                TypeInfo annotationType = ae.typeInfo();
                // JDK annotations (@Deprecated, @SuppressWarnings, ...) resolve without a stub; @Override resolves
                // too but is checked semantically -- that is handled separately by createOverrideSupertype
                if (isJdkType(annotationType)) continue;
                DetailedSources ds = ae.source() == null ? null : ae.source().detailedSources();
                ensureType(annotationType, ds); // nature 'annotation' -> printed as '@interface'
                TypeInfo stub = typeMap.get(annotationType);
                if (stub == null) continue;
                for (AnnotationExpression.KV kv : ae.keyValuePairs()) {
                    MethodInfo origAttr = annotationType.methodStream()
                            .filter(mm -> mm.name().equals(kv.key())).findFirst().orElse(null);
                    if (origAttr != null && !methodMap.containsKey(origAttr)) {
                        MethodInfo newAttr = runtime.newMethod(stub, origAttr.name(),
                                runtime.methodTypeAbstractMethod());
                        newAttr.builder()
                                .setReturnType(ensureTypes(origAttr.returnType()))
                                .setAccess(runtime.accessPackage())
                                .setSource(runtime.noSource())
                                // empty (not null) body: it is printed as ';' since the method is abstract, but a
                                // null body would trip the import computer's methodBody().typesReferenced()
                                .setMethodBody(runtime.emptyBlock())
                                .commit();
                        stub.builder().addMethod(newAttr);
                        methodMap.put(origAttr, newAttr);
                    }
                    kv.value().visit(visitor);
                }
            }
        }
    }

    private class MyVisitor implements Predicate<Element> {
        private final Data data;

        private MyVisitor(Data data) {
            this.data = data;
        }

        // detailed sources of an element (per-element, when the parse enabled them), null otherwise; they record
        // how each type reference was written (simple, enclosing-qualified, or package-qualified)
        private DetailedSources ds(Element e) {
            Source s = e == null ? null : e.source();
            return s == null ? null : s.detailedSources();
        }

        @Override
        public boolean test(Element element) {
            switch (element) {
                case TypeExpression te -> data.ensureTypes(te.parameterizedType(), ds(te));
                case LocalVariableCreation lvc -> data.ensureTypes(lvc.localVariable().parameterizedType(), ds(lvc));
                case InstanceOf instanceOf -> data.ensureTypes(instanceOf.testType(), ds(instanceOf));
                case Cast cast -> data.ensureTypes(cast.parameterizedType(), ds(cast));
                case ClassExpression classExpression ->
                        data.ensureTypes(classExpression.parameterizedType(), ds(classExpression));
                case Lambda lambda -> {
                    for (ParameterInfo pi : lambda.parameters()) {
                        data.ensureTypes(pi.parameterizedType(), ds(pi));
                    }
                    lambda.methodBody().visit(this);
                }
                case ConstructorCall cc -> {
                    if (cc.anonymousClass() != null) {
                        // TypeInfo.visit() is unsupported, so descend into the bodies of its members ourselves,
                        // to reach references (types, calls, fields) that live only inside the anonymous class
                        cc.anonymousClass().constructorAndMethodStream().forEach(mi -> {
                            Block body = mi.methodBody();
                            if (body != null) body.visit(this);
                        });
                        cc.anonymousClass().fields().forEach(fi -> {
                            if (fi.initializer() != null) fi.initializer().visit(this);
                        });
                    }
                    if (cc.constructor() != null) {
                        // stub the constructed type first, so the constructor lands on the stub, not the real type
                        ParameterizedType constructed = data.ensureTypes(cc.parameterizedType(), ds(cc));
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
                        ParameterizedType firstOwner = data.ensureTypes(mc.object().parameterizedType(), ds(mc.object()));
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
                        ParameterizedType scopeType = data.ensureTypes(mr.scope().parameterizedType(), ds(mr.scope()));
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
                            ParameterizedType realOwner = data.ensureTypes(fr.scope().parameterizedType(), ds(fr.scope()));
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
        MyVisitor myVisitor = new MyVisitor(data);
        for (ParameterInfo pi : methodInfo.parameters()) {
            data.ensureTypes(pi.parameterizedType(), detailedSources(pi.source()));
            data.ensureAnnotations(pi, myVisitor);
        }
        if (methodInfo.hasReturnValue()) {
            data.ensureTypes(methodInfo.returnType(), detailedSources(methodInfo.source()));
        }
        // annotations on the isolated method (and its parameters) appear verbatim in the pasted text
        data.ensureAnnotations(methodInfo, myVisitor);

        methodInfo.methodBody().visit(myVisitor);
    }

    private static DetailedSources detailedSources(Source source) {
        return source == null ? null : source.detailedSources();
    }

}
