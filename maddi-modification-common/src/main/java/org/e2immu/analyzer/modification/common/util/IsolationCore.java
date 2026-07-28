package org.e2immu.analyzer.modification.common.util;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.TypeNameRequired;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
/**
 * The stub graph that {@link IsolateMethod} and {@link IsolateClass} both build: every type, method, field and
 * annotation the isolated code reaches, reduced to something that compiles on the JDK alone.
 * <p>
 * The two differ in exactly one thing — <b>where a stub is declared</b> — and that is the {@link #placeStub} hook.
 * {@code IsolateMethod} has one compilation unit to work with, so it nests every stub inside the frame, and has to
 * choose between a frame-level slot (reachable by simple name) and a chain of namespace stubs (reachable fully
 * qualified) — a choice the pasted text dictates and which cost this class most of its defects.
 * {@code IsolateClass} emits a project, so it puts each stub in its own compilation unit in the package the
 * original came from: both spellings then resolve the way they do in real Java, through the package and an import.
 */
abstract class IsolationCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolationCore.class);

    protected final JavaInspector javaInspector;
    protected final Runtime runtime;
    /** the type whose code is being isolated: its own members are pasted verbatim, everything else is stubbed */
    final TypeInfo originalType;

    IsolationCore(JavaInspector javaInspector, TypeInfo originalType) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
        this.originalType = originalType;
    }

    /** Create the stub for {@code original} — already placed where this isolator wants it, not yet populated. */
    abstract TypeInfo placeStub(TypeInfo original, DetailedSources ds);

    /** The type standing in for {@link #originalType}, which self-references in the pasted text resolve to. */
    abstract TypeInfo originalTypeStub();

    /** The type owning an unqualified self-reference ({@code helper()}, {@code this.field}). */
    abstract TypeInfo selfType();

    /**
     * Told how every reference spells a type, before the {@code typeMap} short-circuit. Only an isolator whose
     * placement depends on the spelling has anything to record — see {@code IsolateMethod.MethodStubs}.
     */
    void recordPlacementEvidence(TypeInfo typeInfo, DetailedSources ds) {
    }

    /**
     * A stub nested inside one frame can stay package-private, and {@link IsolateMethod} leaves it so. A stub that
     * lives in its own compilation unit in its own package cannot: every reference to it crosses a package
     * boundary, so the type, its members and any nested stub have to be public — and a nested one static, or it
     * cannot be named without an enclosing instance.
     */
    boolean stubsCrossPackageBoundaries() {
        return false;
    }

    void applyStubTypeAccess(TypeInfo stub) {
        if (stubsCrossPackageBoundaries()) {
            stub.builder().addTypeModifier(runtime.typeModifierPublic());
            if (enclosingTypeOrNull(stub) != null) stub.builder().addTypeModifier(runtime.typeModifierStatic());
            stub.builder().setAccess(runtime.accessPublic());
        } else {
            stub.builder().setAccess(runtime.accessPackage());
        }
    }

    /** package-private members are unreachable once a stub lives in its own package; see above */
    void applyStubMemberAccess(MethodInfo.Builder builder) {
        if (stubsCrossPackageBoundaries()) builder.addMethodModifier(runtime.methodModifierPublic());
    }

    final Map<TypeInfo, TypeInfo> typeMap = new HashMap<>();
    // keyed by (owner, method), NOT by method alone: the same declared method can be reached through two
    // different receiver types ('customerCtx.theCustomers.getObjectInfo()' and another CustomerPar-like
    // holder both inheriting it), and each receiver's stub needs its own copy. Keying by the method alone
    // gave the first owner the stub and left every later one without it, so the call did not resolve and the
    // whole frame was dropped (closed-core ExportJob.insertRecords).
    record OwnedMethod(TypeInfo owner, MethodInfo method) {
    }
    final Map<OwnedMethod, MethodInfo> methodMap = new HashMap<>();
    // annotation attributes are keyed by method alone: they are always stubbed on their own annotation type
    final Map<MethodInfo, MethodInfo> attributeMap = new HashMap<>();
    final Map<FieldInfo, FieldInfo> fieldMap = new HashMap<>();
    // original type parameter -> the freshly created one on the corresponding stub type
    final Map<TypeParameter, TypeParameter> typeParameterMap = new HashMap<>();
    final Set<TypeInfo> jdkTypesToImport = new HashSet<>(); // TODO
    // the JDK types the pasted text names by their simple name ('Date', not 'java.sql.Date'). That spelling is
    // verbatim source and cannot be changed, so on a simple-name clash this is the type that keeps the import;
    // see arbitrateJdkImports
    final Set<TypeInfo> jdkNamedSimplyInSource = new HashSet<>();
    // a running counter handing every numeric constant a distinct value: such constants can be used as switch
    // 'case' labels, which the compiler evaluates and requires to be distinct
    int nextNumericConstant;

    final Set<TypeInfo> interfaceStubs = new HashSet<>();
    // stub types of annotation nature: their members are attributes, not methods (see ensureAnnotationAttribute).
    // Kept as a set rather than asking the stub, because the stub's nature is only readable once it is committed
    final Set<TypeInfo> annotationStubs = new HashSet<>();

    TypeInfo ensureType(TypeInfo typeInfo, DetailedSources ds) {
        if (typeInfo.isPrimitive()) return typeInfo;
        // the original type, referenced by its own name (a 'C' parameter/local, 'new C()', 'C.staticMethod()'),
        // resolves to the stub carrying that name -- the frame has been renamed and no longer answers to 'C'
        if (typeInfo == originalType) return originalTypeStub();
        if (isJdkType(typeInfo)) {
            // written out in the pasted text, and without its package: that simple name is now spoken for
            if (ds != null && ds.detail(typeInfo.packageName()) == null) {
                jdkNamedSimplyInSource.add(typeInfo);
            }
            return typeInfo;
        }

        // record how THIS reference is written, before the typeMap short-circuit: a later reference is exactly
        // what the first one may have got wrong, so its evidence has to be collected too
        recordPlacementEvidence(typeInfo, ds);

        TypeInfo inMap = typeMap.get(typeInfo);
        if (inMap != null) return inMap;
        LOGGER.info("Creating type {}", typeInfo);
        TypeInfo stub = placeStub(typeInfo, ds);
        typeMap.put(typeInfo, stub); // before recursion: type bounds / fields may refer back to this stub
        boolean isInterface = typeInfo.isInterface() && !typeInfo.isAnnotation();
        if (isInterface) interfaceStubs.add(stub);
        if (typeInfo.isAnnotation()) annotationStubs.add(stub);
        stub.builder().setParentClass(reproducedParentClass(typeInfo))
                // reproduce the nature: an annotation must stay '@interface' (a use '@Marker' would not compile),
                // an interface must stay 'interface' (so subtypes 'implements'/'extends' it and overload
                // resolution / generic bounds in the pasted text resolve as in the original); everything else is
                // a class
                .setTypeNature(typeInfo.isAnnotation() ? runtime.typeNatureAnnotation()
                        : isInterface ? runtime.typeNatureInterface()
                        : runtime.typeNatureClass())
                .setSource(runtime.noSource());
        applyStubTypeAccess(stub);
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
        // A FUNCTIONAL interface must keep its single abstract method, or a lambda in the verbatim text has
        // nothing to target: 'ParameterUtil.filter(ctx, null, x -> ...)' against an empty
        // 'interface RowFilter<T> { }' cannot be typed, and maddi's scanner NPEs on it. ensureMethodInfo
        // would make it 'default' (interface stubs keep their bodies so implementors need not override), which
        // leaves the interface non-functional -- so the SAM is reproduced abstract, here.
        if (isInterface) {
            MethodInfo sam = typeInfo.singleAbstractMethod();
            if (sam != null) ensureAbstractMethod(stub, sam);
        }
        return stub;
    }


    /**
     * Was the enclosing type spelled out at the reference site ({@code Outer.Inner}) rather than the member
     * type being named on its own ({@code Inner}, via an import)? The parser records a {@link Source} for
     * every type it writes out, so the enclosing type having a position in this element's detailed sources
     * IS the evidence that it was written.
     */
    boolean enclosingWritten(TypeInfo enclosingType, DetailedSources ds) {
        return ds != null && ds.detail(enclosingType) != null;
    }


    // reproduce the real superclass so the subtype chain holds (e.g. a custom exception keeps 'extends Exception',
    // a 'Dog' keeps 'extends Animal'); but 'extends Record'/'extends Enum' are compiler-managed and illegal to
    // write, and an interface has no superclass, so those default to Object
    ParameterizedType reproducedParentClass(TypeInfo typeInfo) {
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

    ParameterizedType ensureTypes(ParameterizedType pt) {
        return ensureTypes(pt, null);
    }

    ParameterizedType ensureTypes(ParameterizedType pt, DetailedSources ds) {
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

    void ensureField(TypeInfo owner, FieldInfo fieldInfo) {
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
                .setAccess(stubsCrossPackageBoundaries() ? runtime.accessPublic() : runtime.accessPackage());
        if (stubsCrossPackageBoundaries() && !isInterfaceField) {
            fieldBuilder.addFieldModifier(runtime.fieldModifierPublic());
        }
        if (fieldInfo.isStatic()) fieldBuilder.addFieldModifier(runtime.fieldModifierStatic());
        // a class numeric constant needs 'final' to be a constant variable usable as a switch 'case' label;
        // an interface field is implicitly final, so no explicit modifier is needed (or printed) there
        if (numericConstant && !isInterfaceField) fieldBuilder.addFieldModifier(runtime.fieldModifierFinal());
        fieldBuilder.commit();
        fieldMap.put(fieldInfo, newField);
    }

    /**
     * Replace type parameters that are not in scope where the stub method is being declared by their
     * erasure — the first bound, or {@code Object}.
     * <p>
     * A stub method is placed on the type the call went THROUGH, which need not be the type that declares
     * it. {@code class Filter implements RowFilter} (raw) inherits {@code accept(T)} from a generic
     * interface; the stub lands on {@code Filter}, where {@code T} is not in scope, and the frame is dropped
     * on "Type T not found". The compiler erases in exactly this situation, so do we. Measured on
     * closed-core: three isolates, all raw implementations of a generic filter/command interface.
     */
    private ParameterizedType eraseOutOfScope(ParameterizedType pt, TypeInfo owner, MethodInfo newMethod) {
        TypeParameter tp = pt.typeParameter();
        if (tp != null) {
            if (inScope(tp, owner, newMethod)) return pt;
            ParameterizedType bound = tp.typeBounds().isEmpty() ? null : tp.typeBounds().getFirst();
            TypeInfo erased = bound != null && bound.typeInfo() != null
                    ? bound.typeInfo() : runtime.objectParameterizedType().typeInfo();
            return runtime.newParameterizedType(erased, pt.arrays(), pt.wildcard(), List.of());
        }
        if (pt.typeInfo() == null || pt.parameters().isEmpty()) return pt;
        List<ParameterizedType> params = pt.parameters().stream()
                .map(p -> eraseOutOfScope(p, owner, newMethod)).toList();
        return runtime.newParameterizedType(pt.typeInfo(), pt.arrays(), pt.wildcard(), params);
    }

    /**
     * The stub that a {@code super.m()} call should declare {@code m} on: the stub of the type that really
     * declares it, when we have one, else the stub of the isolated type's own parent.
     */
    TypeInfo superTypeStubOf(MethodInfo methodInfo) {
        TypeInfo declaring = methodInfo.typeInfo();
        TypeInfo stub = typeMap.get(declaring);
        if (stub != null) return stub;
        if (declaring != originalType) {
            TypeInfo created = ensureType(declaring, null);
            if (created != declaring) return created;
        }
        ParameterizedType parent = originalType.parentClass();
        if (parent != null && parent.typeInfo() != null) {
            TypeInfo parentStub = typeMap.get(parent.typeInfo());
            if (parentStub != null) return parentStub;
        }
        return selfType();
    }

    /** A type parameter is usable in a method declared on {@code owner} if the method itself declares it, or
     *  the declaring type does — including an enclosing type, whose parameters are in scope in nested ones. */
    private boolean inScope(TypeParameter tp, TypeInfo owner, MethodInfo newMethod) {
        var tpOwner = tp.getOwner();
        if (tpOwner.isRight()) return tpOwner.getRight() == newMethod;
        for (TypeInfo t = owner; t != null; ) {
            if (t == tpOwner.getLeft()) return true;
            var enclosing = t.compilationUnitOrEnclosingType();
            t = enclosing.isRight() ? enclosing.getRight() : null;
        }
        return false;
    }

    void ensureMethodInfo(TypeInfo owner, MethodInfo methodInfo) {
        if (isJdkType(owner)) return;
        // a method inherited from a JDK supertype (e.g. ArrayList.get() from java.util.ArrayList on a custom
        // subclass) resolves via the reproduced real supertype; stubbing it would leak that supertype's type
        // parameters (e.g. 'E get(int)') into the stub, which are not in scope
        if (isJdkType(methodInfo.typeInfo())) return;
        MethodInfo inMap = methodMap.get(new OwnedMethod(owner, methodInfo));
        if (inMap != null) return;
        // a member of an '@interface' is an attribute, which has a shape of its own
        if (annotationStubs.contains(owner)) {
            ensureAnnotationAttribute(owner, methodInfo);
            return;
        }
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
            ParameterizedType newType = eraseOutOfScope(ensureTypes(pi.parameterizedType()), owner, newMethod);
            ParameterInfo newParam = newMethod.builder().addParameter(pi.name(), newType);
            newParam.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
        });
        Block.Builder mb = runtime.newBlockBuilder();
        ParameterizedType newReturnType = eraseOutOfScope(ensureTypes(methodInfo.returnType()), owner, newMethod);
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
        if (!ownerIsInterface && (overridesPublic || stubsCrossPackageBoundaries())) {
            newMethod.builder().addMethodModifier(runtime.methodModifierPublic());
        }
        newMethod.builder()
                .setReturnType(newReturnType)
                .setAccess(runtime.accessPackage())
                .setSource(runtime.noSource())
                .computeAccess()
                .setMethodBody(mb.build())
                .commit();
        // Two DIFFERENT original methods can reduce to the same stub signature: ParameterUtil declares
        // '<T extends IParameterCtx> T filterByID(T, long[])' and '<T extends IParameter> T
        // filterByID(T, long[])', which erase to the same thing, so stubbing both gives the owner a duplicate
        // method. Reuse the one already there: the call sites resolve to it just the same, and the alternative
        // is a unit that does not compile -- or maddi's own MethodMapImpl.addToReturn assertion, which is what
        // stopped six of the hundred class isolates from being produced at all.
        MethodInfo clash = erasureClash(owner, newMethod);
        if (clash != null) {
            LOGGER.info("Stub of {} would duplicate {} on {}; reusing it", methodInfo, clash, owner);
            methodMap.put(new OwnedMethod(owner, methodInfo), clash);
            return;
        }
        LOGGER.info("Adding method {}", newMethod);
        owner.builder().addMethod(newMethod);
        methodMap.put(new OwnedMethod(owner, methodInfo), newMethod);
    }

    /** Reproduce {@code sam} on {@code stub} as an abstract method, so the stub stays a functional interface. */
    private void ensureAbstractMethod(TypeInfo stub, MethodInfo sam) {
        OwnedMethod key = new OwnedMethod(stub, sam);
        if (methodMap.containsKey(key)) return;
        MethodInfo newMethod = runtime.newMethod(stub, sam.name(), runtime.methodTypeAbstractMethod());
        sam.parameters().forEach(pi -> {
            ParameterInfo np = newMethod.builder().addParameter(pi.name(), ensureTypes(pi.parameterizedType()));
            np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
        });
        newMethod.builder()
                .setReturnType(ensureTypes(sam.returnType()))
                .setAccess(runtime.accessPublic())
                .setSource(runtime.noSource())
                .setMethodBody(runtime.emptyBlock())
                .commit();
        if (erasureClash(stub, newMethod) != null) return;
        stub.builder().addMethod(newMethod);
        methodMap.put(key, newMethod);
    }

    /**
     * A method already on {@code owner} that {@code candidate} would duplicate: same name, same arity, and the
     * same erased parameter types. Java resolves overloads on erasures, so two stubs agreeing there cannot both
     * be declared.
     */
    private static MethodInfo erasureClash(TypeInfo owner, MethodInfo candidate) {
        String key = erasureKey(candidate);
        return Stream.concat(owner.builder().constructors().stream(), owner.builder().methods().stream())
                .filter(m -> m != candidate && erasureKey(m).equals(key))
                .findFirst().orElse(null);
    }

    private static String erasureKey(MethodInfo m) {
        StringBuilder sb = new StringBuilder(m.name()).append('(');
        for (ParameterInfo pi : m.parameters()) {
            ParameterizedType pt = pi.parameterizedType();
            TypeParameter tp = pt.typeParameter();
            // a type parameter erases to its first bound, or Object; that is what overload resolution sees
            String erased = tp != null
                    ? tp.typeBounds().isEmpty() || tp.typeBounds().getFirst().typeInfo() == null
                    ? "java.lang.Object" : tp.typeBounds().getFirst().typeInfo().fullyQualifiedName()
                    : pt.typeInfo() == null ? "?" : pt.typeInfo().fullyQualifiedName();
            sb.append(erased).append('[').append(pt.arrays()).append("],");
        }
        return sb.append(')').toString();
    }

    // an interface's abstract method together with the type-argument map that turns its (interface) type
    // parameters into the concrete types the implementing stub sees (e.g. {E -> Long} for 'Iterable<Long>')
    private record AbstractMethod(MethodInfo method, Map<NamedType, ParameterizedType> typeArguments) {
    }

    // a concrete class stub that implements an interface ('LongVector implements Iterable<Long>') and is
    // instantiated ('new LongVector()') cannot be abstract, so it must provide (dummy) implementations of the
    // interface's abstract methods, or it does not compile
/**
     * A stub that another stub extends must offer a no-argument constructor.
     * <p>
     * {@code class TwoChannelAxisOperation extends AxisOperation { }} has an implicit constructor, which calls an
     * implicit {@code super()} — and the reproduced {@code AxisOperation} has only the constructor an actual
     * {@code new AxisOperation(int)} in the isolated code caused us to stub. The subclass then does not compile,
     * and four Axis2 class isolates were dropped on it.
     * <p>
     * Only stubs that are actually extended get one, and only when they have constructors but no no-arg one: a
     * stub extending {@code Object} already has what it needs, so the ordinary isolate is unchanged.
     */
    void addDefaultConstructorsWhereExtended() {
        // read the hierarchy from the ORIGINALS, which are committed: a stub's parentClass() is not yet
        // readable at this point, so asking the stubs found nothing at all and this pass silently did nothing
        Set<TypeInfo> extended = new HashSet<>();
        Set<TypeInfo> originals = new HashSet<>(typeMap.keySet());
        originals.add(originalType);
        for (TypeInfo original : originals) {
            ParameterizedType parent = original.parentClass();
            if (parent == null || parent.typeInfo() == null) continue;
            TypeInfo parentStub = typeMap.get(parent.typeInfo());
            if (parentStub != null) extended.add(parentStub);
        }
        for (TypeInfo stub : typeMap.values()) {
            if (!extended.contains(stub) || interfaceStubs.contains(stub) || annotationStubs.contains(stub)) continue;
            // BOTH lists: TypeInfo.Builder.addMethod files everything under methods(), addConstructor under
            // constructors(), and ensureMethodInfo adds constructor stubs with addMethod -- so constructors()
            // alone is empty here and this pass quietly did nothing
            List<MethodInfo> constructors = Stream.concat(stub.builder().constructors().stream(),
                            stub.builder().methods().stream())
                    .filter(MethodInfo::isConstructor).toList();
            if (constructors.isEmpty()) continue;      // the implicit no-arg constructor is there already
            if (constructors.stream().anyMatch(ctor -> ctor.parameters().isEmpty())) continue;
            MethodInfo noArg = runtime.newConstructor(stub);
            noArg.builder().setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                    .setSource(runtime.noSource())
                    .setMethodBody(runtime.emptyBlock())
                    .setAccess(stubsCrossPackageBoundaries() ? runtime.accessPublic() : runtime.accessPackage());
            if (stubsCrossPackageBoundaries()) noArg.builder().addMethodModifier(runtime.methodModifierPublic());
            noArg.builder().commit();
            stub.builder().addMethod(noArg);
            LOGGER.info("Added a no-arg constructor to {}, which is extended by another stub", stub);
        }
    }

    void addDummyInterfaceMethods() {
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
        // eraseOutOfScope: a RAW 'implements RowFilter' has no type arguments, so applyTranslation
        // substitutes nothing and the interface's own 'T' survives into a dummy implementation on a class
        // that does not declare it -- the frame is then dropped on "Type T not found". Erasing to the
        // bound is what the compiler does for a raw supertype. (Three closed-core isolates.)
        abstractMethod.parameters().forEach(pi -> {
            ParameterizedType type = eraseOutOfScope(
                    ensureTypes(pi.parameterizedType().applyTranslation(runtime, am.typeArguments)),
                    stub, dummy);
            ParameterInfo np = dummy.builder().addParameter(pi.name(), type);
            np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(false).commit();
        });
        ParameterizedType returnType = eraseOutOfScope(ensureTypes(abstractMethod.returnType()
                .applyTranslation(runtime, am.typeArguments)), stub, dummy);
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

    /**
     * Is this a type the isolated frame can simply refer to, rather than one it has to stub?
     * <p>
     * The criterion is <b>"does running it need an external jar"</b>: everything shipped with the JDK is
     * kept as itself and imported; everything that would need a library on the class path is stubbed. That
     * is recorded per source set — a type resolved from a {@code jmod} has {@code partOfJdk} — so it is a
     * fact about where the type came from, not a guess from its name.
     * <p>
     * A package-name test cannot make this distinction. {@code javax.xml.namespace.QName} ships in module
     * {@code java.xml} and {@code javax.xml.stream.XMLStreamReader} in the same module, while
     * {@code javax.servlet.*} needs a jar; all three start with {@code javax.}. The old {@code java.}-prefix
     * test therefore stubbed the first two, and because they are then reached through a namespace chain the
     * printer emitted the nonsense {@code Product_serialize_35.javax.xml.namespace.QName}.
     * <p>
     * {@code java.*} is kept as a fallback for the case where a source set is unavailable: the JVM forbids
     * user code in {@code java.*}, so anything in that namespace is JDK by construction.
     */
    boolean isJdkType(TypeInfo owner) {
        String packageName = owner.packageName();
        if (packageName == null) return true;
        if (!partOfJdk(owner) && !packageName.startsWith("java.")) return false;
        // java.lang is imported implicitly; anything else needs an explicit import in the frame. For a
        // NESTED JDK type, import its enclosing types too: the pasted text is verbatim source and may write
        // either form, and 'import java.util.Map.Entry' does not make a written 'Map.Entry' resolve -- the
        // parser then invents a stub type 'Map.Entry' with no methods and the call 'entry.getKey()' is
        // unresolved, taking the whole frame with it.
        for (TypeInfo t = owner; t != null; ) {
            if (!"java.lang".equals(t.packageName())) jdkTypesToImport.add(t);
            var enclosing = t.compilationUnitOrEnclosingType();
            t = enclosing.isRight() ? enclosing.getRight() : null;
        }
        return true;
    }

    // an annotation present in the pasted text ('@Marker', '@Named("x")') needs its '@interface' stubbed, plus
    // every attribute it actually uses, plus any types its attribute values reference (e.g. 'SomeClass.class')
    void ensureAnnotations(Element annotated, MyVisitor visitor) {
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
                if (origAttr != null) {
                    MethodInfo newAttr = ensureAnnotationAttribute(stub, origAttr);
                    attributeMap.put(origAttr, newAttr);
                }
                kv.value().visit(visitor);
            }
        }
    }

    /**
     * A member of an {@code @interface} is an attribute, not a method: implicitly abstract, and it may have
     * neither parameters nor a body. It is reached two ways — from an annotation use in the pasted text
     * ({@code @Named("x")}, via {@link #ensureAnnotations}) and from a read on an annotation instance
     * ({@code named.value()}, via {@link #ensureMethodInfo}) — which is why both go through here: the ordinary
     * stub shape (a body returning null) yields an {@code @interface} that will not compile, and that the
     * printer cannot even render.
     */
    private MethodInfo ensureAnnotationAttribute(TypeInfo stub, MethodInfo origAttr) {
        OwnedMethod key = new OwnedMethod(stub, origAttr);
        MethodInfo inMap = methodMap.get(key);
        if (inMap != null) return inMap;
        MethodInfo newAttr = runtime.newMethod(stub, origAttr.name(), runtime.methodTypeAbstractMethod());
        newAttr.builder()
                .setReturnType(ensureTypes(origAttr.returnType()))
                .setAccess(runtime.accessPackage())
                .setSource(runtime.noSource())
                // empty (not null) body: it is printed as ';' since the method is abstract, but a
                // null body would trip the import computer's methodBody().typesReferenced()
                .setMethodBody(runtime.emptyBlock())
                .commit();
        stub.builder().addMethod(newAttr);
        methodMap.put(key, newAttr);
        return newAttr;
    }

    /**
     * Everything one method contributes to the stub graph: its signature (parameters, return type, thrown
     * exceptions), its annotations and those of its parameters, and its body. A class isolate calls this once
     * per kept method; a method isolate once, for the one.
     */
    void visitMethod(MethodInfo methodInfo) {
        MyVisitor myVisitor = new MyVisitor();
        for (ParameterInfo pi : methodInfo.parameters()) {
            ensureTypes(pi.parameterizedType(), detailedSources(pi.source()));
            ensureAnnotations(pi, myVisitor);
        }
        if (methodInfo.hasReturnValue()) {
            ensureTypes(methodInfo.returnType(), detailedSources(methodInfo.source()));
        }
        // A checked exception can appear ONLY in the throws clause: 'void m() throws SQLException' whose body
        // merely calls JDBC never mentions SQLException anywhere the body visitor can see it. Without this the
        // frame neither imports nor stubs it, and the pasted signature does not resolve -- the whole compilation
        // unit is then dropped on an unresolved symbol. TestIsolateMethod7Exceptions did not catch it because
        // its exception is also 'throw new'n in the body; closed-core's ExportJob.insertRecords is
        // the real-world case that does not.
        for (ParameterizedType exceptionType : methodInfo.exceptionTypes()) {
            ensureTypes(exceptionType, detailedSources(methodInfo.source()));
        }
        // annotations on the isolated method (and its parameters) appear verbatim in the pasted text
        ensureAnnotations(methodInfo, myVisitor);

        methodInfo.methodBody().visit(myVisitor);
    }

    class MyVisitor implements Predicate<Element> {
        // detailed sources of an element (per-element, when the parse enabled them), null otherwise; they record
        // how each type reference was written (simple, enclosing-qualified, or package-qualified)
        private DetailedSources ds(Element e) {
            Source s = e == null ? null : e.source();
            return s == null ? null : s.detailedSources();
        }

        /**
         * The type on which a call's receiver declares its methods. Normally that is simply the receiver's
         * {@code TypeInfo}, but a receiver typed by a TYPE PARAMETER ({@code T t; t.compareTo(x)}) has none: the
         * method lives on the parameter's erasure -- its first bound, or {@code Object} when unbounded -- which
         * is where the compiler resolves it too. Returning null here made {@code isJdkType} throw an NPE.
         */
        private TypeInfo erasedOwner(ParameterizedType pt) {
            TypeInfo typeInfo = pt.typeInfo();
            if (typeInfo != null) return typeInfo;
            if (pt.typeParameter() != null) {
                List<ParameterizedType> bounds = pt.typeParameter().typeBounds();
                if (!bounds.isEmpty()) {
                    TypeInfo bound = bounds.getFirst().typeInfo();
                    if (bound != null) return bound;
                }
            }
            return runtime.objectParameterizedType().typeInfo();
        }

        @Override
        public boolean test(Element element) {
            switch (element) {
                // a bare type-expression is the qualifier of a static access; the field/method/constructor cases
                // already stub its owner (routing a written 'X.member' to the original-type stub). Skip the original
                // type here so an implicit self-qualifier ('staticMethod()', 'LOGGER') does not materialise an empty
                // 'class X' stub; other types still get stubbed (e.g. a written 'Other.member')
                case TypeExpression te -> {
                    if (te.parameterizedType().typeInfo() != originalType) {
                        ensureTypes(te.parameterizedType(), ds(te));
                    }
                }
                case LocalVariableCreation lvc -> ensureTypes(lvc.localVariable().parameterizedType(), ds(lvc));
                case InstanceOf instanceOf -> ensureTypes(instanceOf.testType(), ds(instanceOf));
                case Cast cast -> ensureTypes(cast.parameterizedType(), ds(cast));
                case ClassExpression classExpression ->
                        ensureTypes(classExpression.parameterizedType(), ds(classExpression));
                case Lambda lambda -> {
                    for (ParameterInfo pi : lambda.parameters()) {
                        ensureTypes(pi.parameterizedType(), ds(pi));
                    }
                    lambda.methodBody().visit(this);
                }
                // a type named ONLY in a catch clause is reached no other way: the body mentions the variable,
                // never the type. 'catch (EmptyStackException e)' then leaves the frame without the import and
                // the whole unit is dropped -- the throws-clause case with the same shape is handled in
                // visitMethod. 21 of the 37 units still failing on the hundred-class corpus were this.
                case TryStatement ts -> ts.catchClauses().forEach(cc ->
                        cc.exceptionTypes().forEach(et -> ensureTypes(et, ds(ts))));
                case ConstructorCall cc -> {
                    if (cc.anonymousClass() != null) {
                        // the supertype is what the verbatim text names ('new Comparator<X>() {...}'), and an
                        // interface has no constructor, so the cc.constructor() branch below never reaches it
                        TypeInfo anonymous = cc.anonymousClass();
                        if (anonymous.parentClass() != null) ensureTypes(anonymous.parentClass(), ds(cc));
                        anonymous.interfacesImplemented().forEach(itf -> ensureTypes(itf, ds(cc)));
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
                        ParameterizedType constructed = ensureTypes(cc.parameterizedType(), ds(cc));
                        ensureMethodInfo(constructed.typeInfo(), cc.constructor());
                    }
                }
                case MethodCall mc -> {
                    TypeInfo owner;
                    // an unqualified static self-call ('helper()') has a synthetic type-expression object (no source)
                    // and belongs on the frame; a written 'C.helper()' has a real source and is routed through the
                    // original-type stub via ensureTypes(object) below, so 'C.' keeps resolving
                    if (mc.object() instanceof VariableExpression sve
                        && sve.variable() instanceof This thisVar && thisVar.writeSuper()) {
                        // 'super.getSession(...)': the method has to land on the SUPERTYPE's stub, not on the
                        // isolated type -- which usually declares a method of that very name, that being why the
                        // body writes 'super.' at all. Sending it to selfType() left the supertype without the
                        // declaration, and the unit was dropped (maddi's scanner NPEs rather than reporting it)
                        owner = superTypeStubOf(mc.methodInfo());
                    } else if (mc.object() == null
                        || mc.object().source() == null
                           && mc.methodInfo().isStatic() && mc.methodInfo().typeInfo() == originalType
                        || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = selfType();
                    } else {
                        ParameterizedType firstOwner = ensureTypes(mc.object().parameterizedType(), ds(mc.object()));
                        owner = erasedOwner(firstOwner);
                    }
                    ensureMethodInfo(owner, mc.methodInfo());
                }
                case MethodReference mr -> {
                    // 'scope::method' (or 'Type::new'): the referenced method must be stubbed just like a call. A
                    // written scope ('C::helper', source present) routes through the original-type stub via
                    // ensureTypes(scope); only a synthetic scope or 'this::' belongs on the frame
                    TypeInfo owner;
                    if (mr.scope().source() == null && mr.methodInfo().typeInfo() == originalType
                        || mr.scope() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = selfType();
                    } else {
                        ParameterizedType scopeType = ensureTypes(mr.scope().parameterizedType(), ds(mr.scope()));
                        owner = erasedOwner(scopeType);
                    }
                    ensureMethodInfo(owner, mr.methodInfo());
                }
                case VariableExpression ve -> {
                    if (ve.variable() instanceof FieldReference fr) {
                        TypeInfo owner;
                        // An explicit 'this.' scope means the same thing as no scope at all: the field belongs on
                        // the frame, which stands in for the type declaring the isolated method. Without this it
                        // was routed to its DECLARING type instead -- the original-type stub nested in the frame --
                        // and 'this.customerCtx' in the pasted body then resolved to nothing, because the frame
                        // does not extend that stub. The MethodCall branch above already treats 'this.' this way;
                        // this branch did not (closed-core ExportJob.insertRecords).
                        if (fr.isDefaultScope() || fr.scope() instanceof VariableExpression sve
                                                   && sve.variable() instanceof This) {
                            owner = selfType();
                        } else {
                            // still stub the scope's own type (it may be referenced nowhere else), but place the
                            // field on its DECLARING type, not on the scope type. An inherited field accessed via a
                            // subtype ('paymentPeriod.residualValue', declared on PeriodData) belongs on the supertype
                            // stub: the subtype inherits it via 'extends', and an access via the supertype resolves
                            // too. Placing it on the scope type would, with the fieldMap dedup, drop it from the
                            // supertype stub when the same field is later accessed there
                            ensureTypes(fr.scope().parameterizedType(), ds(fr.scope()));
                            TypeInfo declaringType = fr.fieldInfo().owner();
                            // a field reached through the original type's own name ('C.DAYS') must land on a stub
                            // carrying that name, not on the renamed frame, or the verbatim 'C.DAYS' will not resolve
                            owner = declaringType == originalType ? originalTypeStub()
                                    : ensureType(declaringType, null);
                        }
                        ensureField(owner, fr.fieldInfo());
                    }
                }
                default -> {

                }
            }
            return true;
        }
    }

    /**
     * Whether {@code typeInfo} ships with the JDK, i.e. was resolved from a {@code jmod} rather than from a jar
     * on the class path. See {@link #isJdkType} for why this is the criterion.
     */
    static boolean partOfJdk(TypeInfo typeInfo) {
        CompilationUnit compilationUnit = typeInfo.compilationUnit();
        SourceSet sourceSet = compilationUnit == null ? null : compilationUnit.sourceSet();
        return sourceSet != null && sourceSet.partOfJdk();
    }

    static DetailedSources detailedSources(Source source) {
        return source == null ? null : source.detailedSources();
    }

    static TypeInfo enclosingTypeOrNull(TypeInfo typeInfo) {
        var e = typeInfo.compilationUnitOrEnclosingType();
        return e.isRight() ? e.getRight() : null;
    }

    // number of enclosing types up to the compilation unit (a primary type has depth 0)
    static int enclosingDepth(TypeInfo typeInfo) {
        int depth = 0;
        var e = typeInfo.compilationUnitOrEnclosingType();
        while (e.isRight()) {
            depth++;
            e = e.getRight().compilationUnitOrEnclosingType();
        }
        return depth;
    }
}
