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
import org.e2immu.language.cst.api.type.NamedType;
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
        addJdkImports(importComputer, result);
        return javaInspector.print2(result.typeInfo.compilationUnit(), runtime.qualificationSimpleNames(), importComputer);
    }

    // import the collected JDK types, except any whose simple name clashes with a stub declared in the frame (e.g. a
    // client 'ArrayList' extending java.util.ArrayList): importing the JDK one would print it as the bare simple name
    // and collide with the stub; instead leave it out so the import computer fully-qualifies it ('java.util.ArrayList')
    private void addJdkImports(ImportComputer importComputer, Result result) {
        Set<String> declaredSimpleNames = new HashSet<>();
        result.typeInfo.compilationUnit().types()
                .forEach(t -> t.recursiveSubTypeStream().forEach(st -> declaredSimpleNames.add(st.simpleName())));
        result.jdkTypesToImport.stream()
                .filter(t -> !declaredSimpleNames.contains(t.simpleName()))
                .forEach(importComputer::add);
    }

    public String print(Result result, String methodString) {
        ImportComputer importComputer = javaInspector.importComputer(4, javaInspector.mainSources());
        addJdkImports(importComputer, result);
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

    public Result isolate(MethodInfo methodInfo, String customClassName) {
        TypeInfo originalType = methodInfo.typeInfo().primaryType();
        org.e2immu.language.cst.api.runtime.Runtime runtime = javaInspector.runtime();
        CompilationUnit newCu = runtime.newCompilationUnitBuilder()
                .setPackageName(targetPackage)
                .setSourceSet(originalType.compilationUnit().sourceSet())
                .setURI(originalType.compilationUnit().uri())
                .build();
        String simpleName = customClassName == null
                ? originalType.simpleName() + "_" + methodInfo.name()
                : customClassName;
        TypeInfo frame = runtime
                .newTypeInfo(newCu, simpleName);
        frame.builder().setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .computeAccess();

        Data data = new Data(methodInfo, frame);
        visit(data, methodInfo);
        data.addDummyInterfaceMethods();

        data.fieldMap.values().stream().sorted(Comparator.comparing(FieldInfo::name)).forEach(field -> {
            // add each field to its actual owner (the frame, or a stub type), mirroring how methods are added
            field.owner().builder().addField(field);
        });
        // deepest types first, so a nested stub is committed and added to its (still open) enclosing stub before
        // that enclosing stub is itself committed; includes the package 'namespace' stubs
        List<TypeInfo> allStubs = new ArrayList<>(data.typeMap.values());
        allStubs.addAll(data.namespaceMap.values());
        if (data.originalTypeStub != null) allStubs.add(data.originalTypeStub);
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
        // a running counter handing every numeric constant a distinct value: such constants can be used as switch
        // 'case' labels, which the compiler evaluates and requires to be distinct
        int nextNumericConstant;
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
        // a stub carrying the original type's simple name (e.g. 'C'), nested in the frame ('C_method'). The frame is
        // renamed, so a self-reference written with the original name ('C.DAYS') no longer resolves to it; this stub
        // gives that name back. Created lazily, only when such a reference is seen
        TypeInfo originalTypeStub;

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

        // the stub bearing the original type's simple name, nested in the frame, to host members referenced through
        // that name (e.g. the static 'C.DAYS' inside class C, isolated into 'C_method'). Lazily created on first use
        TypeInfo originalTypeStub() {
            if (originalTypeStub == null) {
                originalTypeStub = runtime.newTypeInfo(frame, originalType.simpleName());
                originalTypeStub.builder()
                        .setTypeNature(runtime.typeNatureClass())
                        .setParentClass(runtime.objectParameterizedType())
                        .setSource(runtime.noSource())
                        .setAccess(runtime.accessPackage());
            }
            return originalTypeStub;
        }

        // 'ds' are the detailed sources of the element that references 'typeInfo' in the pasted method text (or null
        // if unavailable); they tell us how the reference was written, which decides where to place a primary type.
        private TypeInfo ensureType(TypeInfo typeInfo, DetailedSources ds) {
            if (typeInfo.isPrimitive()) return typeInfo;
            // the original type, referenced by its own name (a 'C' parameter/local, 'new C()', 'C.staticMethod()'),
            // resolves to the stub carrying that name -- the frame has been renamed and no longer answers to 'C'
            if (typeInfo == originalType) return originalTypeStub();
            if (isJdkType(typeInfo)) return typeInfo;

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
            boolean isInterfaceField = interfaceStubs.contains(owner);
            // a numeric constant (an interface field, or a class 'static final' field) may appear as a switch 'case'
            // label; those must be distinct compile-time constants, so hand each one a unique value
            boolean numericConstant = newPt.isNumeric()
                                      && (isInterfaceField || fieldInfo.isStatic() && fieldInfo.isFinal());
            // an interface field is implicitly 'public static final', so it must have an initializer (a bare
            // 'String NAME;' does not compile in an interface); a class field may leave it empty
            Expression initializer = numericConstant ? runtime.newInt(nextNumericConstant++)
                    : isInterfaceField ? runtime.nullValue(newPt)
                    : runtime.newEmptyExpression();
            FieldInfo.Builder fieldBuilder = newField.builder().setInitializer(initializer)
                    .setAccess(runtime.accessPackage());
            if (fieldInfo.isStatic()) fieldBuilder.addFieldModifier(runtime.fieldModifierStatic());
            // a class numeric constant needs 'final' to be a constant variable usable as a switch 'case' label;
            // an interface field is implicitly final, so no explicit modifier is needed (or printed) there
            if (numericConstant && !isInterfaceField) fieldBuilder.addFieldModifier(runtime.fieldModifierFinal());
            fieldBuilder.commit();
            fieldMap.put(fieldInfo, newField);
        }

        private void ensureMethodInfo(TypeInfo owner, MethodInfo methodInfo) {
            if (isJdkType(owner)) return;
            // a method inherited from a JDK supertype (e.g. ArrayList.get() from java.util.ArrayList on a custom
            // subclass) resolves via the reproduced real supertype; stubbing it would leak that supertype's type
            // parameters (e.g. 'E get(int)') into the stub, which are not in scope
            if (isJdkType(methodInfo.typeInfo())) return;
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
            // a method that overrides a public supertype method on a class stub must be public -- an override cannot
            // reduce visibility. This covers java.lang.Object methods (toString/equals/...) as well as inherited
            // interface methods (e.g. a custom 'ArrayList<I> extends java.util.ArrayList<I>' overriding Collection.add).
            // computeAccess() derives the access from the modifier; an interface method is public implicitly
            boolean overridesPublic = methodInfo.isOverloadOfJLOMethod()
                                      || methodInfo.overrides().stream().anyMatch(o -> o.access().isPublic());
            if (!ownerIsInterface && overridesPublic) {
                newMethod.builder().addMethodModifier(runtime.methodModifierPublic());
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

        // an interface's abstract method together with the type-argument map that turns its (interface) type
        // parameters into the concrete types the implementing stub sees (e.g. {E -> Long} for 'Iterable<Long>')
        private record AbstractMethod(MethodInfo method, Map<NamedType, ParameterizedType> typeArguments) {
        }

        // a concrete class stub that implements an interface ('LongVector implements Iterable<Long>') and is
        // instantiated ('new LongVector()') cannot be abstract, so it must provide (dummy) implementations of the
        // interface's abstract methods, or it does not compile
        private void addDummyInterfaceMethods() {
            // fixpoint: adding a dummy implementation can reference a type that was not stubbed during the visit
            // (e.g. it appears only in an interface method's signature), creating a new stub which itself may need
            // dummy implementations. Keep going until every stub in the map has been processed.
            Set<TypeInfo> processed = new HashSet<>();
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Map.Entry<TypeInfo, TypeInfo> entry : new ArrayList<>(typeMap.entrySet())) {
                    TypeInfo original = entry.getKey();
                    TypeInfo stub = entry.getValue();
                    if (!processed.add(stub)) continue;
                    changed = true;
                    // only concrete classes need (dummy) implementations; interfaces and annotations cannot have
                    // method bodies, and an annotation implicitly implements java.lang.annotation.Annotation
                    if (interfaceStubs.contains(stub) || stub.isInterface() || stub.isAnnotation()) continue;
                    Map<String, AbstractMethod> required = new LinkedHashMap<>();
                    // collect from the ORIGINAL interfaces: their type arguments are original types, so the translated
                    // method signatures are original types that ensureTypes maps to existing stubs. Iterating the
                    // stub's own (already reproduced) interfaces would translate to stub types and feed them back into
                    // ensureType, stubbing them a second time (a stub-of-a-stub nested under a copy of the frame)
                    for (ParameterizedType itf : original.interfacesImplemented()) {
                        collectAbstractMethods(itf, required);
                    }
                    if (required.isEmpty()) continue;
                    Set<String> present = new HashSet<>();
                    stub.methodStream().forEach(m -> present.add(methodKey(m)));
                    for (AbstractMethod am : required.values()) {
                        if (present.add(methodKey(am.method))) {
                            addDummyImplementation(stub, am);
                        }
                    }
                }
            }
        }

        private void collectAbstractMethods(ParameterizedType interfaceType, Map<String, AbstractMethod> result) {
            TypeInfo itf = interfaceType.typeInfo();
            if (itf == null || !itf.isInterface()) return;
            Map<NamedType, ParameterizedType> typeArguments = interfaceType.initialTypeParameterMap();
            itf.methodStream()
                    .filter(m -> m.isAbstract() && !m.isStatic() && !m.isDefault())
                    .forEach(m -> result.putIfAbsent(methodKey(m), new AbstractMethod(m, typeArguments)));
            for (ParameterizedType superInterface : itf.interfacesImplemented()) {
                collectAbstractMethods(superInterface.applyTranslation(runtime, typeArguments), result);
            }
        }

        private void addDummyImplementation(TypeInfo stub, AbstractMethod am) {
            MethodInfo abstractMethod = am.method;
            MethodInfo dummy = runtime.newMethod(stub, abstractMethod.name(), runtime.methodTypeMethod());
            abstractMethod.parameters().forEach(pi -> {
                ParameterizedType type = ensureTypes(pi.parameterizedType().applyTranslation(runtime, am.typeArguments));
                ParameterInfo np = dummy.builder().addParameter(pi.name(), type);
                np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(false).commit();
            });
            ParameterizedType returnType = ensureTypes(abstractMethod.returnType()
                    .applyTranslation(runtime, am.typeArguments));
            Block.Builder mb = runtime.newBlockBuilder();
            if (!returnType.isVoid()) {
                mb.addStatement(runtime.newReturnBuilder().setExpression(runtime.nullValue(returnType)).build());
            }
            dummy.builder()
                    .addMethodModifier(runtime.methodModifierPublic())
                    .setReturnType(returnType)
                    .setAccess(runtime.accessPublic())
                    .setSource(runtime.noSource())
                    .computeAccess()
                    .setMethodBody(mb.build())
                    .commit();
            stub.builder().addMethod(dummy);
        }

        private static String methodKey(MethodInfo m) {
            return m.name() + "/" + m.parameters().size();
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
                // a bare type-expression is the qualifier of a static access; the field/method/constructor cases
                // already stub its owner (routing a written 'X.member' to the original-type stub). Skip the original
                // type here so an implicit self-qualifier ('staticMethod()', 'LOGGER') does not materialise an empty
                // 'class X' stub; other types still get stubbed (e.g. a written 'Other.member')
                case TypeExpression te -> {
                    if (te.parameterizedType().typeInfo() != data.originalType) {
                        data.ensureTypes(te.parameterizedType(), ds(te));
                    }
                }
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
                    // an unqualified static self-call ('helper()') has a synthetic type-expression object (no source)
                    // and belongs on the frame; a written 'C.helper()' has a real source and is routed through the
                    // original-type stub via ensureTypes(object) below, so 'C.' keeps resolving
                    if (mc.object() == null
                        || mc.object().source() == null
                           && mc.methodInfo().isStatic() && mc.methodInfo().typeInfo() == data.originalType
                        || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = data.frame;
                    } else {
                        ParameterizedType firstOwner = data.ensureTypes(mc.object().parameterizedType(), ds(mc.object()));
                        owner = firstOwner.typeInfo();
                    }
                    data.ensureMethodInfo(owner, mc.methodInfo());
                }
                case MethodReference mr -> {
                    // 'scope::method' (or 'Type::new'): the referenced method must be stubbed just like a call. A
                    // written scope ('C::helper', source present) routes through the original-type stub via
                    // ensureTypes(scope); only a synthetic scope or 'this::' belongs on the frame
                    TypeInfo owner;
                    if (mr.scope().source() == null && mr.methodInfo().typeInfo() == data.originalType
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
                            // still stub the scope's own type (it may be referenced nowhere else), but place the
                            // field on its DECLARING type, not on the scope type. An inherited field accessed via a
                            // subtype ('paymentPeriod.residualValue', declared on PeriodData) belongs on the supertype
                            // stub: the subtype inherits it via 'extends', and an access via the supertype resolves
                            // too. Placing it on the scope type would, with the fieldMap dedup, drop it from the
                            // supertype stub when the same field is later accessed there
                            data.ensureTypes(fr.scope().parameterizedType(), ds(fr.scope()));
                            TypeInfo declaringType = fr.fieldInfo().owner();
                            // a field reached through the original type's own name ('C.DAYS') must land on a stub
                            // carrying that name, not on the renamed frame, or the verbatim 'C.DAYS' will not resolve
                            owner = declaringType == data.originalType ? data.originalTypeStub()
                                    : data.ensureType(declaringType, null);
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
