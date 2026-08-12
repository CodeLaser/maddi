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
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
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
    /**
     * The types whose code is being isolated: their own members are pasted verbatim, everything else is stubbed.
     * A method isolate has exactly one; a class isolate has one or more.
     * <p>
     * The set is what makes a <b>group</b> isolate different from N separate ones: a reference from one member of
     * the set to another resolves to the REAL, kept type — not to a stub of it. Everything that used to compare a
     * type against "the" original type therefore splits in two, and which of the two is meant is never obvious from
     * the old code: {@link #isIsolated} asks "is this type kept verbatim somewhere in the emitted tree", while
     * {@link #currentOriginalType} answers "which type is {@code this} in the text being pasted right now".
     */
    final List<TypeInfo> originalTypes;
    private final Set<TypeInfo> originalTypeSet;
    /**
     * The isolated type whose member is being visited: {@code this.field}, an unqualified call and an unqualified
     * static member all resolve against IT, not against the set as a whole. Set before each member walk by the
     * isolator; with a single isolated type it never changes.
     */
    TypeInfo currentOriginalType;

    IsolationCore(JavaInspector javaInspector, TypeInfo originalType) {
        this(javaInspector, List.of(originalType));
    }

    IsolationCore(JavaInspector javaInspector, List<TypeInfo> originalTypes) {
        assert !originalTypes.isEmpty();
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
        this.originalTypes = List.copyOf(originalTypes);
        this.originalTypeSet = new HashSet<>(this.originalTypes);
        this.currentOriginalType = this.originalTypes.getFirst();
    }

    /** Is {@code typeInfo} one of the types being isolated, i.e. kept verbatim rather than stubbed? */
    boolean isIsolated(TypeInfo typeInfo) {
        return originalTypeSet.contains(typeInfo);
    }

    /** Create the stub for {@code original} — already placed where this isolator wants it, not yet populated. */
    abstract TypeInfo placeStub(TypeInfo original, DetailedSources ds);

    /**
     * The type standing in for one of the {@link #originalTypes}, which references to it in the pasted text
     * resolve to. A class isolate answers the isolated type itself (it keeps its name); a method isolate has
     * renamed its frame, so it answers a separate stub carrying the original name.
     */
    abstract TypeInfo originalTypeStub(TypeInfo original);

    /** The type owning an unqualified self-reference ({@code helper()}, {@code this.field}). */
    abstract TypeInfo selfType();

    /**
     * A static member named without a scope in the verbatim text, and declared by some OTHER type: only a static
     * import makes that spelling resolve. {@link IsolateMethod} does not need it — it puts such a member on the
     * frame, which is in scope for the whole pasted body — but a class isolate cannot, because the member belongs
     * to a stub in another compilation unit. Returns true if the member was taken over by the isolator, meaning
     * the caller must NOT also declare it on {@link #selfType()}.
     */
    boolean recordStaticImport(TypeInfo owner, String memberName) {
        return false;
    }

    /**
     * Told how every reference spells a type, before the {@code typeMap} short-circuit. Only an isolator whose
     * placement depends on the spelling has anything to record — see {@code IsolateMethod.MethodStubs}.
     */
    void recordPlacementEvidence(TypeInfo typeInfo, DetailedSources ds) {
    }

    /**
     * Told how a reference to another ISOLATED type is spelled. Placement is not in question for those — they are
     * where the original was — but the import list is: an isolated unit naming a sibling isolate by its simple name
     * needs that import, and needs to win the simple name if two types claim it.
     */
    void recordIsolatedReference(TypeInfo typeInfo, DetailedSources ds) {
    }

    /**
     * Will {@code owner} declare {@code methodInfo} anyway, without a stub?
     * <p>
     * {@link IsolateMethod} keeps ONE member, on a frame that is not the type that declared it, so every call the
     * body makes — self-calls included — needs a stub. {@link IsolateClass} keeps ALL of them, verbatim, on a type
     * that IS the original: a self-call resolves to text that is already there, and stubbing it as well declares
     * the method twice. {@code erasureClash} cannot see the collision, because until printing each kept member is
     * a marker method under a generated name.
     * <p>
     * Found by compiling the hundred-class isolate corpus with javac: 76 of the 100 trees had duplicate members,
     * 3727 of them in total. The corpus re-parses cleanly under maddi, which accepts a duplicate declaration —
     * which is why the producer's own verification never saw it.
     */
    boolean alreadyDeclaredWithoutStub(TypeInfo owner, MethodInfo methodInfo) {
        return false;
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

    void applyStubTypeAccess(TypeInfo stub, TypeInfo original) {
        // An ABSTRACT original stays abstract, and that is worth more than faithfulness: an abstract class owes no
        // implementations, so the whole family of "X is not abstract and does not override abstract method m() in
        // I" simply cannot arise for it. Which matters because the dummy pass cannot always see what is owed --
        // a JDK type reached only as a supertype NAME from a class file is materialized without members, so
        // 'javax.xml.transform.SourceLocator' answered methodStream() with nothing and xalan's
        // 'abstract class Expression implements ExpressionNode' (ExpressionNode extends SourceLocator) got no
        // dummies at all. Completing such a type belongs in the inspection layer, not here; reproducing 'abstract'
        // is right on its own terms and happens to make that case moot. Nothing is lost: the verbatim text cannot
        // instantiate a type the original could not instantiate either.
        if (original.isAbstract() && !original.isInterface() && !original.typeNature().isEnum()) {
            stub.builder().addTypeModifier(runtime.typeModifierAbstract());
        }
        if (stubsCrossPackageBoundaries()) {
            stub.builder().addTypeModifier(runtime.typeModifierPublic());
            // 'static' only when the original is: a nested stub has to be nameable without an enclosing instance,
            // but making an INNER class static breaks the one spelling that needs the instance --
            // 'outer.new Inner()' in the verbatim text is then "qualified new of static class" (5 class isolates).
            // TypeInfo.isStatic() answers true for an interface/enum/record and for a primary type as well, which
            // is what we want: those are the cases where the modifier is implicit or meaningless
            if (enclosingTypeOrNull(stub) != null && original.isStatic()) {
                stub.builder().addTypeModifier(runtime.typeModifierStatic());
            }
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
    // keyed by (owner, method), NOT by method alone: one declared method can legitimately be stubbed on several
    // types -- the frame (selfType), a supertype (superTypeStubOf), the type an override has to be declared on.
    // Keying by the method alone gave the first owner the stub and left every later one without it, so the call
    // did not resolve and the whole frame was dropped (closed-core ExportJob.insertRecords).
    // Note this is NOT a copy per RECEIVER any more: a call now lands on the type that declares the method
    // (MyVisitor.declaringOwner), which the receivers inherit it from. Two receivers used to get two copies, and
    // where one of them was the other's abstract parent, the two erased to the same signature without overriding
    // -- javac's "name clash ... yet neither overrides the other", one of the hundred class isolates.
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
    // stub types of enum nature: their constants are synthetic fields (see ensureField), and the compiler declares
    // values()/valueOf() for them (see ensureMethodInfo). Same reason for keeping a set: the nature of a stub is
    // only readable once it has been committed, which is long after these decisions are taken
    final Set<TypeInfo> enumStubs = new HashSet<>();

    /**
     * Per isolated type, everything its own traversal reached: the types whose simple name its pasted text may
     * spell, and therefore the ones its unit may have to import.
     * <p>
     * With a single isolated type this is the whole graph and says nothing. With several it is what keeps one
     * unit's imports out of another's: {@code toImport} is a property of the isolate as a whole, and offering all
     * of it to every unit put {@code import a.b.Client} at the top of {@code p/q/Base.java}. Over-inclusive by
     * construction, which is the safe direction: the text cannot name what the traversal did not reach, so
     * nothing that is needed is missing. Two ways it over-includes, both harmless — a type reached only by a
     * reconstructed signature is in here too (the import computer would have worked that one out for itself),
     * and whatever the dummy/constructor passes reach afterwards is attributed to whichever isolate ran last,
     * those passes having no current type of their own. The cost of either is an unused import.
     */
    final Map<TypeInfo, Set<TypeInfo>> reachedPerIsolatedType = new HashMap<>();

    void recordReached(TypeInfo typeInfo) {
        reachedPerIsolatedType.computeIfAbsent(currentOriginalType, _ -> new HashSet<>()).add(typeInfo);
    }

    TypeInfo ensureType(TypeInfo typeInfo, DetailedSources ds) {
        if (typeInfo.isPrimitive()) return typeInfo;
        // an isolated type, referenced by its own name (a 'C' parameter/local, 'new C()', 'C.staticMethod()'),
        // resolves to the type standing in for it -- for IsolateMethod the stub carrying that name, since the frame
        // has been renamed and no longer answers to 'C'; for IsolateClass the kept type itself. A reference from one
        // isolated type to another lands here too, and this is what keeps it pointing at the real type
        if (isIsolated(typeInfo)) {
            recordIsolatedReference(typeInfo, ds);
            TypeInfo standIn = originalTypeStub(typeInfo);
            recordReached(standIn);
            return standIn;
        }
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
        if (inMap != null) {
            recordReached(inMap);
            return inMap;
        }
        LOGGER.info("Creating type {}", typeInfo);
        TypeInfo stub = placeStub(typeInfo, ds);
        recordReached(stub);
        typeMap.put(typeInfo, stub); // before recursion: type bounds / fields may refer back to this stub
        boolean isInterface = typeInfo.isInterface() && !typeInfo.isAnnotation();
        boolean isEnum = typeInfo.typeNature().isEnum();
        if (isInterface) interfaceStubs.add(stub);
        if (typeInfo.isAnnotation()) annotationStubs.add(stub);
        if (isEnum) enumStubs.add(stub);
        stub.builder().setParentClass(isEnum ? enumParentOf(stub) : reproducedParentClass(typeInfo))
                // reproduce the nature: an annotation must stay '@interface' (a use '@Marker' would not compile),
                // an interface must stay 'interface' (so subtypes 'implements'/'extends' it and overload
                // resolution / generic bounds in the pasted text resolve as in the original), and an enum must stay
                // 'enum' -- a 'case CLASS:' label over a class with static fields is "pattern or enum constant
                // required", and no shape a class can take makes a switch over it compile. Everything else is a
                // class.
                .setTypeNature(typeInfo.isAnnotation() ? runtime.typeNatureAnnotation()
                        : isInterface ? runtime.typeNatureInterface()
                        : isEnum ? runtime.typeNatureEnum()
                        : runtime.typeNatureClass())
                .setSource(runtime.noSource());
        applyStubTypeAccess(stub, typeInfo);
        if (typeInfo.isAnnotation()) {
            // an annotation type must implement java.lang.annotation.Annotation (asserted on commit)
            TypeInfo annotation = javaInspector.compiledTypesManager()
                    .type(java.lang.annotation.Annotation.class);
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
     * Does {@code isolated} inherit from {@code owner}, so that a member of it is in scope unqualified?
     * <p>
     * Reads the ORIGINAL hierarchy, declared parent and interfaces, walked by hand. Deliberately not
     * {@code recursiveSuperTypeStream()}: on a nested type that also answers the ENCLOSING types, which is a
     * different relation and would make an unqualified reference to an outer class's static member look inherited.
     */
    boolean isolatedTypeInherits(TypeInfo isolated, TypeInfo owner) {
        Set<TypeInfo> seen = new HashSet<>();
        Deque<TypeInfo> todo = new ArrayDeque<>();
        todo.add(isolated);
        while (!todo.isEmpty()) {
            TypeInfo t = todo.poll();
            if (!seen.add(t)) continue;
            if (t == owner) return true;
            if (t.parentClass() != null && t.parentClass().typeInfo() != null) {
                todo.add(t.parentClass().typeInfo());
            }
            t.interfacesImplemented().forEach(itf -> {
                if (itf.typeInfo() != null) todo.add(itf.typeInfo());
            });
        }
        return false;
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


    /**
     * {@code java.lang.Enum<E>}, which every enum extends. It is never written out — {@code TypePrinterImpl} skips
     * exactly this parent for an enum, as the language does — but the model insists on it: an enum-natured type
     * whose parent is {@code Object} cannot be committed ({@code TypeInspectionImpl.Builder.commit} asserts it),
     * and {@link #reproducedParentClass} maps {@code java.lang.Enum} to {@code Object} precisely because until now
     * every stub of an enum was a class.
     */
    private ParameterizedType enumParentOf(TypeInfo enumStub) {
        TypeInfo enumType = javaInspector.compiledTypesManager().type(Enum.class);
        return runtime.newParameterizedType(enumType, List.of(enumStub.asSimpleParameterizedType()));
    }

    // reproduce the real superclass so the subtype chain holds (e.g. a custom exception keeps 'extends Exception',
    // a 'Dog' keeps 'extends Animal'); but 'extends Record'/'extends Enum' are compiler-managed and illegal to
    // write, and an interface has no superclass, so those default to Object
    ParameterizedType reproducedParentClass(TypeInfo typeInfo) {
        ParameterizedType parent = typeInfo.parentClass();
        if (parent == null || parent.isJavaLangObject()) return runtime.objectParameterizedType();
        if (compilerManagedParent(parent.typeInfo())) return runtime.objectParameterizedType();
        return ensureTypes(parent);
    }

    /** {@code extends Record} / {@code extends Enum} are the compiler's, illegal to write, and not reproduced. */
    private static boolean compilerManagedParent(TypeInfo typeInfo) {
        if (typeInfo == null) return false;
        String fqn = typeInfo.fullyQualifiedName();
        return "java.lang.Record".equals(fqn) || "java.lang.Enum".equals(fqn);
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

    void ensureField(TypeInfo ownerIn, FieldInfo fieldInfo) {
        TypeInfo owner = stubFor(ownerIn);
        if (owner == null || isJdkType(owner)) return;
        FieldInfo inMap = fieldMap.get(fieldInfo);
        if (inMap != null) return;
        ParameterizedType newPt = ensureTypes(fieldInfo.type());
        FieldInfo newField = runtime.newFieldInfo(fieldInfo.name(), fieldInfo.isStatic(), newPt, owner);
        // An enum constant is not a field declaration but a name in the constant list, and what marks it as one in
        // this model is isSynthetic(): TypePrinterImpl.enumConstantStream prints exactly the synthetic fields of an
        // enum-natured type, as bare names before every other member. That is also how the constant arrives here --
        // FlagHelper.field sets it from Flags.ENUM -- so the test is the original's own flag, and an ordinary
        // 'static final String' field of an enum, which carries no such flag, still stubs as a field.
        if (enumStubs.contains(owner) && fieldInfo.isSynthetic()) {
            newField.builder().setSynthetic(true)
                    // no initializer: the printer accepts an empty one (or a ConstructorCall, for a constant with
                    // arguments) and would throw on anything else. No modifiers either -- 'public static final' is
                    // implicit, and writing it is not legal Java in the constant list
                    .setInitializer(runtime.newEmptyExpression())
                    .setAccess(runtime.accessPublic())
                    .commit();
            fieldMap.put(fieldInfo, newField);
            return;
        }
        boolean isInterfaceField = interfaceStubs.contains(owner);
        // a numeric constant (an interface field, or a class 'static final' field) may appear as a switch 'case'
        // label; those must be distinct compile-time constants, so hand each one a unique value.
        // PRIMITIVE only: ParameterizedType.isNumeric() is true of the boxed types too, and 'static final Long
        // PANEL = 49' is "incompatible types: int cannot be converted to Long". A boxed constant cannot be a case
        // label in the first place, so it needs nothing from this
        boolean numericConstant = newPt.isPrimitiveExcludingVoid() && newPt.isNumeric()
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
     * A stub method need not be placed on the type that declares it: {@code class Filter implements
     * RowFilter} (raw) inherits {@code accept(T)} from a generic interface, and a dummy implementation of it
     * lands on {@code Filter}, where {@code T} is not in scope — the frame is then dropped on "Type T not found".
     * The compiler erases in exactly this situation, so do we. Measured on closed-core: three isolates, all raw
     * implementations of a generic filter/command interface.
     * <p>
     * A CALL no longer arrives here that way: {@code MyVisitor.declaringOwner} places it on its declaring type, so
     * the parameter is in scope and survives. Erasing it there was defect B of
     * {@code docs/handoff-isolateclass-enum-and-generic-stubs.md} — this method was doing its job on a bad owner.
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
        // an isolated type is not in the typeMap, but 'super.m()' inside a subtype that is isolated TOO must still
        // reach it: ensureType answers the kept type, and the declaration is already there, verbatim
        if (declaring != currentOriginalType) {
            TypeInfo created = ensureType(declaring, null);
            if (created != declaring) return created;
        }
        ParameterizedType parent = currentOriginalType.parentClass();
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

    void ensureMethodInfo(TypeInfo ownerIn, MethodInfo methodInfo) {
        TypeInfo owner = stubFor(ownerIn);
        if (owner == null || isJdkType(owner)) return;
        // a method inherited from a JDK supertype (e.g. ArrayList.get() from java.util.ArrayList on a custom
        // subclass) resolves via the reproduced real supertype; stubbing it would leak that supertype's type
        // parameters (e.g. 'E get(int)') into the stub, which are not in scope
        if (isJdkType(methodInfo.typeInfo())) return;
        if (alreadyDeclaredWithoutStub(owner, methodInfo)) return;
        MethodInfo inMap = methodMap.get(new OwnedMethod(owner, methodInfo));
        if (inMap != null) return;
        // a member of an '@interface' is an attribute, which has a shape of its own
        if (annotationStubs.contains(owner)) {
            ensureAnnotationAttribute(owner, methodInfo);
            return;
        }
        // 'values()' and 'valueOf(String)' are declared by the compiler for every enum, so a stub of them is
        // "values() is already defined in Kind". They are real, non-synthetic methods when the enum reached maddi
        // from a class file, which is where an 'E.values()' in the pasted text finds them; name()/ordinal() and the
        // rest come from java.lang.Enum, which the isJdkType test above already drops.
        if (enumStubs.contains(owner) && methodInfo.isStatic()
            && ("values".equals(methodInfo.name()) || "valueOf".equals(methodInfo.name()))) {
            return;
        }
        // a non-static method on an interface stub becomes 'default' (keeps the body): an abstract method would
        // force every implementing class stub to override it
        boolean ownerIsInterface = interfaceStubs.contains(owner);
        MethodInfo newMethod = runtime.newMethod(owner, methodInfo.name(),
                methodInfo.isStatic() ? runtime.methodTypeStaticMethod() :
                        methodInfo.isConstructor() ? runtime.methodTypeConstructor() :
                                ownerIsInterface ? runtime.methodTypeDefaultMethod() : runtime.methodTypeMethod());
        reproduceMethodTypeParameters(methodInfo, newMethod, Map.of());
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
        // the isolated type's own member may OVERRIDE the method being stubbed, and an override may not narrow
        // access or throw more than what it overrides. Both halves have to come from the original, or the kept
        // member -- verbatim text, unchangeable -- stops being a legal override of the stub we just wrote:
        //   'protected void init()'   against a stub made public   -> "attempting to assign weaker access privileges"
        //   'void x() throws SAXException' against a stub with no throws -> "overridden method does not throw ..."
        // 26 and 13 of the hundred class isolates respectively, all in the SAX handler hierarchy.
        methodInfo.exceptionTypes().forEach(et -> newMethod.builder().addExceptionType(ensureTypes(et)));

        // a method that overrides a public supertype method on a class stub must be public -- an override cannot
        // reduce visibility. This covers java.lang.Object methods (toString/equals/...) as well as inherited
        // interface methods (e.g. a custom 'ArrayList<I> extends java.util.ArrayList<I>' overriding Collection.add).
        // computeAccess() derives the access from the modifier; an interface method is public implicitly
        boolean overridesPublic = methodInfo.isOverloadOfJLOMethod()
                                  || methodInfo.overrides().stream().anyMatch(o -> o.access().isPublic());
        if (!ownerIsInterface && (overridesPublic || stubsCrossPackageBoundaries())) {
            // ... but not WIDER than the original either. 'public' is the default for a stub in its own package
            // because every reference to it crosses a package boundary; 'protected' already crosses that boundary
            // for the one caller that matters here, the subclass being isolated.
            boolean keepProtected = !overridesPublic && methodInfo.access().isProtected();
            newMethod.builder().addMethodModifier(keepProtected
                    ? runtime.methodModifierProtected() : runtime.methodModifierPublic());
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

    /**
     * The stub we are allowed to add members to, for a type we were handed as an owner.
     * <p>
     * Owners are supposed to be stubs, and normally are. But a receiver typed by a type parameter erases to that
     * parameter's bound ({@code erasedOwner}), and a type parameter the isolated code declares itself is kept as
     * it is — so its bound is the REAL type. log4j's builder idiom, {@code B extends FileAppender.Builder<B>},
     * produces exactly that, and adding a method to the real committed type trips
     * {@code "Inspection of X has already been committed"}, losing the whole isolate. Four of the hundred
     * class isolates were failing this way.
     *
     * @return a stub safe to modify, or null when there is nothing sensible to modify
     */
    private TypeInfo stubFor(TypeInfo owner) {
        if (owner == null || !owner.hasBeenInspected()) return owner;   // already a stub of ours
        if (isJdkType(owner)) return owner;                             // caller drops it
        TypeInfo stub = ensureType(owner, null);
        return stub != null && !stub.hasBeenInspected() ? stub : null;
    }

    /**
     * Reproduce a method's OWN type parameters ({@code <X> X foo(X x)}) on its stub, so that occurrences of them in
     * the signature stay in scope — {@code ensureTypes} translates each occurrence through {@code typeParameterMap},
     * and {@code eraseOutOfScope} replaces by the bound whatever is not in scope where the stub is declared.
     * Two passes, for a bound that references a sibling parameter or the method's own.
     * <p>
     * All three places that build a method stub need this, and only one of them had it. The two that did not
     * emitted {@code A as(Factory<A> f)} with no {@code <A>} to declare it (the SAM path) and a signature with
     * {@code A} erased to a RAW copy of its own bound (the dummy-implementation path).
     *
     * @param typeArguments substitutions to apply to the bounds, for a supertype implemented with type arguments
     */
    /**
     * The isolated type's own type parameters, on the type that stands in for it. The method-level counterpart
     * is {@link #reproduceMethodTypeParameters}, and the two passes are deliberately the same shape: declare all
     * of them first, registering each in {@code typeParameterMap} so that every later use translates, and only
     * then set the bounds — a bound may name a sibling parameter ({@code <T, B extends List<T>>}), which cannot
     * resolve until both exist.
     */
    void reproduceTypeParameters(TypeInfo original, TypeInfo isolated) {
        List<TypeParameter> origTps = original.typeParameters();
        if (origTps.isEmpty()) return;
        List<TypeParameter> newTps = new ArrayList<>(origTps.size());
        for (TypeParameter origTp : origTps) {
            TypeParameter newTp = runtime.newTypeParameter(origTp.getIndex(), origTp.simpleName(), isolated);
            typeParameterMap.put(origTp, newTp);
            isolated.builder().addOrSetTypeParameter(newTp);
            newTps.add(newTp);
        }
        for (int i = 0; i < newTps.size(); i++) {
            List<ParameterizedType> newBounds = origTps.get(i).typeBounds().stream()
                    .map(this::ensureTypes).toList();
            newTps.get(i).builder().setTypeBounds(newBounds).commit();
        }
    }

    private void reproduceMethodTypeParameters(MethodInfo original, MethodInfo newMethod,
                                               Map<NamedType, ParameterizedType> typeArguments) {
        List<TypeParameter> origTps = original.typeParameters();
        List<TypeParameter> newTps = new ArrayList<>(origTps.size());
        for (TypeParameter origTp : origTps) {
            TypeParameter newTp = runtime.newTypeParameter(origTp.getIndex(), origTp.simpleName(), newMethod);
            typeParameterMap.put(origTp, newTp);
            newMethod.builder().addTypeParameter(newTp);
            newTps.add(newTp);
        }
        for (int i = 0; i < newTps.size(); i++) {
            List<ParameterizedType> newBounds = origTps.get(i).typeBounds().stream()
                    .map(b -> ensureTypes(typeArguments.isEmpty() ? b : b.applyTranslation(runtime, typeArguments)))
                    .toList();
            newTps.get(i).builder().setTypeBounds(newBounds).commit();
        }
    }

    /** Reproduce {@code sam} on {@code stub} as an abstract method, so the stub stays a functional interface. */
    private void ensureAbstractMethod(TypeInfo stub, MethodInfo sam) {
        OwnedMethod key = new OwnedMethod(stub, sam);
        if (methodMap.containsKey(key)) return;
        MethodInfo newMethod = runtime.newMethod(stub, sam.name(), runtime.methodTypeAbstractMethod());
        reproduceMethodTypeParameters(sam, newMethod, Map.of());
        sam.parameters().forEach(pi -> {
            ParameterInfo np = newMethod.builder().addParameter(pi.name(), ensureTypes(pi.parameterizedType()));
            np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(pi.isFinal()).commit();
        });
        // the throws clause, as ensureMethodInfo and addDummyImplementation both do. All three paths build a
        // method stub and each used to reproduce a different subset; where two of them describe the SAME method --
        // here the interface's declaration, there a class's implementation of it -- any disagreement is a
        // compilation error. 'interface CustomCloneable { Object clone() throws CloneNotSupportedException; }'
        // emitted without its throws, against an implementation that had one, is
        // "clone() in X cannot implement clone() in CustomCloneable": 88 of the hundred class isolates, from one
        // omission in this method
        sam.exceptionTypes().forEach(et -> newMethod.builder().addExceptionType(ensureTypes(et)));
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
        return erasureKey(m, Map.of(), null);
    }

    /**
     * {@code name(erasedParam[arrays],…)} — what overriding and overload resolution are decided on.
     *
     * @param typeArguments substituted before erasing, so that the key of an interface's declared method matches
     *                      the key of the implementation an implementing type sees
     */
    private static String erasureKey(MethodInfo m, Map<NamedType, ParameterizedType> typeArguments, Runtime runtime) {
        StringBuilder sb = new StringBuilder(m.name()).append('(');
        for (ParameterInfo pi : m.parameters()) {
            ParameterizedType pt = typeArguments.isEmpty() ? pi.parameterizedType()
                    : pi.parameterizedType().applyTranslation(runtime, typeArguments);
            sb.append(erasedName(pt)).append('[').append(pt.arrays()).append("],");
        }
        return sb.append(')').toString();
    }

    /** a type parameter erases to its first bound, or Object; that is what overload resolution sees */
    private static String erasedName(ParameterizedType pt) {
        TypeParameter tp = pt.typeParameter();
        if (tp != null) {
            return tp.typeBounds().isEmpty() || tp.typeBounds().getFirst().typeInfo() == null
                    ? "java.lang.Object" : tp.typeBounds().getFirst().typeInfo().fullyQualifiedName();
        }
        return pt.typeInfo() == null ? "?" : pt.typeInfo().fullyQualifiedName();
    }

    /**
     * A supertype's abstract method together with the type-argument map that turns its type parameters into the
     * concrete types the implementing stub sees (e.g. {@code {E -> Long}} for {@code Iterable<Long>}).
     *
     * @param raw the supertype is generic but was implemented RAW ({@code implements Map}), so the map is empty
     *            and the signature to produce is javac's <b>erasure</b> — no type arguments and no wildcards
     */
    private record AbstractMethod(MethodInfo method, Map<NamedType, ParameterizedType> typeArguments, boolean raw) {
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
        originals.addAll(originalTypes);
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
                collectAbstractMethodsOfJdkAncestors(original.parentClass(), required);
                if (required.isEmpty()) continue;
                Set<String> present = new HashSet<>();
                stub.methodStream().forEach(m -> present.add(erasureKey(m)));
                for (AbstractMethod am : required.values()) {
                    if (present.add(erasureKey(am.method, am.typeArguments, runtime))) {
                        // the original may already have the very implementation we are about to invent; see below
                        MethodInfo real = declaredImplementation(original, am);
                        if (real != null) ensureMethodInfo(stub, real);
                        else addDummyImplementation(stub, am);
                    }
                }
            }
        }
    }

    private void collectAbstractMethods(ParameterizedType interfaceType, Map<String, AbstractMethod> result) {
        TypeInfo itf = interfaceType.typeInfo();
        if (itf == null || !itf.isInterface()) return;
        Map<NamedType, ParameterizedType> typeArguments = interfaceType.initialTypeParameterMap();
        // 'implements Map' rather than 'implements Map<K, V>': there is nothing to substitute, and what the class
        // inherits is the ERASURE of every method -- see AbstractMethod.raw and fullyErased
        boolean raw = !itf.typeParameters().isEmpty() && interfaceType.parameters().isEmpty();
        itf.methodStream()
                .filter(m -> m.isAbstract() && !m.isStatic() && !m.isDefault())
                .forEach(m -> result.putIfAbsent(erasureKey(m, typeArguments, runtime),
                        new AbstractMethod(m, typeArguments, raw)));
        for (ParameterizedType superInterface : itf.interfacesImplemented()) {
            collectAbstractMethods(superInterface.applyTranslation(runtime, typeArguments), result);
        }
    }

    /**
     * The abstract methods a stub inherits from an <b>abstract class</b> — {@code class CustomOutputStream extends
     * java.io.OutputStream { }} does not compile without {@code write(int)}.
     * <p>
     * Only JDK ancestors, and that is not a shortcut: a stubbed ancestor is a concrete class whose every method has
     * a body ({@link #ensureMethodInfo}), so it leaves nothing abstract to implement. A JDK ancestor is kept as
     * itself, with its real abstract methods intact, and is the only kind that can. Eight of the hundred class
     * isolates: {@code OutputStream.write(int)}, {@code InputStream.read()}, {@code Transformer.getErrorListener()}.
     * <p>
     * Each ancestor's OWN abstract methods only. An abstract class that leaves an interface method unimplemented
     * without redeclaring it is not covered; every case measured redeclares.
     * <p>
     * <b>The walk stops at the first concrete class</b>, and that is the whole correctness argument: a concrete
     * ancestor implements everything above it, so nothing further up is still owed. Walking the chain
     * unconditionally instead reaches {@code AbstractList.get(int)} and {@code AbstractCollection.iterator()}
     * through the perfectly concrete {@code java.util.ArrayList}, and adds dummies that override methods which are
     * not abstract at all — "get(int) in ListStack cannot override get(int) in java.util.AbstractList", because the
     * type arguments do not survive that far either.
     */
    private void collectAbstractMethodsOfJdkAncestors(ParameterizedType classType,
                                                      Map<String, AbstractMethod> result) {
        TypeInfo ti = classType == null ? null : classType.typeInfo();
        if (ti == null || ti.isJavaLangObject() || !isJdkType(ti) || !ti.isAbstract()) return;
        // A parent that is not REPRODUCED owes the stub nothing: a record stub extends Object, not
        // java.lang.Record, so the three abstract methods java.lang.Record declares (toString, hashCode, equals)
        // are not inherited and dummies for them are pure noise
        if (compilerManagedParent(ti)) return;
        Map<NamedType, ParameterizedType> typeArguments = classType.initialTypeParameterMap();
        boolean raw = !ti.typeParameters().isEmpty() && classType.parameters().isEmpty();
        ti.methodStream()
                .filter(m -> m.isAbstract() && !m.isStatic() && !m.isDefault())
                .forEach(m -> result.putIfAbsent(erasureKey(m, typeArguments, runtime),
                        new AbstractMethod(m, typeArguments, raw)));
        // compose the substitutions as we climb, exactly as the interface walk does: the abstract method may be
        // declared several levels up, on a type parameter this level has already bound
        ParameterizedType parent = ti.parentClass();
        if (parent != null) {
            collectAbstractMethodsOfJdkAncestors(parent.applyTranslation(runtime, typeArguments), result);
        }
    }

    /**
     * The implementation {@code original} <b>already declares</b> for an inherited abstract method, if it declares
     * one. A dummy is a guess at a declaration; this is the declaration, and where it exists it is strictly better.
     * <p>
     * The dummy pass reaches a type whose own members were never referenced, so its stub is empty and the pass
     * concludes the interface obligation is unmet — while the original satisfies it in its own text, complete with
     * a {@code throws} clause. Inventing one WITHOUT that clause (which is the right default, §6: giving dummies
     * the interface's exceptions costs 24 trees) then contradicts every subtype that overrides the method
     * faithfully: <i>"startDocument() in X cannot override startDocument() in Y; overridden method does not throw
     * org.xml.sax.SAXException"</i>. Four trees of the closed-core corpus, one of which {@code isolate-class.md}
     * §7 had filed as needing a reconciling pass over the finished stub graph. No reconciling is needed once the
     * two declarations are the SAME declaration.
     * <p>
     * Only the type's own methods, deliberately. A concrete implementation further up is inherited by the original
     * too, and stubbing it here would declare an override the original never wrote — the ancestor gets its own
     * turn in the fixpoint if it is stubbed at all.
     * <p>
     * ⛔ <b>Nothing generic.</b> A dummy is BUILT for the implementing type — {@code am.typeArguments} substituted,
     * out-of-scope parameters erased to their bounds — while this copies a declaration written against the
     * declaring type's own type parameters. Where those differ the two erase alike without overriding, which is
     * javac's <i>"name clash: allMatch(Predicate&lt;ELEMENT&gt;) … have the same erasure, yet neither overrides the
     * other"</i>: 3 trees at 44 errors each, all assertj, the first time this was tried without the guard. The
     * exchange is only safe when there is nothing to substitute, so the two would agree on every type anyway —
     * which is exactly the case the corpus needed it for ({@code startDocument()}, {@code getProperty(String)}).
     *
     * @return null when nothing is declared here, i.e. when a dummy really is the only thing available
     */
    private MethodInfo declaredImplementation(TypeInfo original, AbstractMethod am) {
        if (am.raw || !am.typeArguments.isEmpty()) return null;
        String key = erasureKey(am.method, am.typeArguments, runtime);
        return original.methodStream()
                .filter(m -> !m.isAbstract() && !m.isStatic() && m.typeParameters().isEmpty()
                             && erasureKey(m).equals(key) && noTypeParameters(m))
                .findFirst().orElse(null);
    }

    /** neither the return type nor any parameter mentions a type parameter, at any depth */
    private static boolean noTypeParameters(MethodInfo m) {
        return !mentionsTypeParameter(m.returnType())
               && m.parameters().stream().noneMatch(pi -> mentionsTypeParameter(pi.parameterizedType()));
    }

    private static boolean mentionsTypeParameter(ParameterizedType pt) {
        return pt.typeParameter() != null || pt.parameters().stream().anyMatch(IsolationCore::mentionsTypeParameter);
    }

    private void addDummyImplementation(TypeInfo stub, AbstractMethod am) {
        MethodInfo abstractMethod = am.method;
        MethodInfo dummy = runtime.newMethod(stub, abstractMethod.name(), runtime.methodTypeMethod());
        // the method's OWN type parameters: without them they are not in scope where the dummy is declared, so
        // eraseOutOfScope replaces each by its bound -- and drops that bound's own type arguments, which is how
        // assertj's '<ASSERT extends AbstractAssert<?,?>> ASSERT asInstanceOf(InstanceOfAssertFactory<?, ASSERT>)'
        // became 'AbstractAssert asInstanceOf(InstanceOfAssertFactory<?, AbstractAssert>)': a RAW AbstractAssert
        // as the type argument for 'ASSERT extends AbstractAssert<?,?>', not within its own bound. 3 isolates.
        reproduceMethodTypeParameters(abstractMethod, dummy, am.typeArguments);
        // eraseOutOfScope: a RAW 'implements RowFilter' has no type arguments, so applyTranslation
        // substitutes nothing and the interface's own 'T' survives into a dummy implementation on a class
        // that does not declare it -- the frame is then dropped on "Type T not found". Erasing to the
        // bound is what the compiler does for a raw supertype. (Three closed-core isolates.)
        abstractMethod.parameters().forEach(pi -> {
            ParameterizedType type = am.raw ? fullyErased(pi.parameterizedType())
                    : eraseOutOfScope(ensureTypes(pi.parameterizedType()
                    .applyTranslation(runtime, am.typeArguments)), stub, dummy);
            ParameterInfo np = dummy.builder().addParameter(pi.name(), type);
            np.builder().setVarArgs(pi.isVarArgs()).setIsFinal(false).commit();
        });
        ParameterizedType returnType = am.raw ? fullyErased(abstractMethod.returnType())
                : eraseOutOfScope(ensureTypes(abstractMethod.returnType()
                .applyTranslation(runtime, am.typeArguments)), stub, dummy);
        Block.Builder mb = runtime.newBlockBuilder();
        if (!returnType.isVoid()) {
            mb.addStatement(runtime.newReturnBuilder().setExpression(runtime.nullValue(returnType)).build());
        }
        // NO throws clause, deliberately, and this is the one place in the isolator where declaring LESS than the
        // original is the right answer. A dummy stands for an IMPLEMENTATION, and an implementation may narrow --
        // most do. Give it the interface's exceptions instead and every verbatim call site has to handle them:
        // "unreported exception java.io.IOException; must be caught or declared to be thrown", 24 of the hundred
        // class isolates. It was tried, to fix the one tree where the disagreement bites the other way (a stub of
        // the same method built by ensureMethodInfo, WITH the original's exceptions, overriding a dummy without
        // them -- "cannot override … overridden method does not throw SAXNotSupportedException"). One tree
        // against twenty-four. Reconciling the two properly needs a pass over the finished stub graph, matching
        // each stub method against what it overrides; see docs/isolate-class.md.
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

    /**
     * javac's erasure of a type: the raw type, no type arguments and no wildcard.
     * <p>
     * What a class implementing a generic supertype RAW inherits. {@code eraseOutOfScope} is not enough here: it
     * substitutes an out-of-scope type parameter by its bound but keeps the wildcard around it, so
     * {@code tryAdvance(Consumer<? super T>)} of a raw {@code implements Spliterator} became
     * {@code tryAdvance(Consumer<? super Object>)} — which has the right erasure and yet neither overrides nor
     * implements the raw {@code tryAdvance(Consumer)}, so javac reports both "is not abstract and does not
     * override" and "name clash … yet neither overrides the other" for the same method (one class isolate,
     * commons-collections implementing raw {@code java.util.Map}).
     */
    private ParameterizedType fullyErased(ParameterizedType pt) {
        TypeParameter tp = pt.typeParameter();
        if (tp != null) {
            ParameterizedType bound = tp.typeBounds().isEmpty() ? null : tp.typeBounds().getFirst();
            TypeInfo erased = bound != null && bound.typeInfo() != null
                    ? bound.typeInfo() : runtime.objectParameterizedType().typeInfo();
            return runtime.newParameterizedType(ensureType(erased, null), pt.arrays());
        }
        if (pt.typeInfo() == null) return pt;
        return runtime.newParameterizedType(ensureType(pt.typeInfo(), null), pt.arrays());
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
            if (!"java.lang".equals(t.packageName())) {
                jdkTypesToImport.add(t);
                recordReached(t);
            }
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
        ParameterizedType returnType = ensureTypes(origAttr.returnType());
        // An attribute is stubbed because SOME use names it; every OTHER use of the annotation then has to supply
        // it, and a bare '@Test' is "annotation @Test is missing a default value for the element 'expected'". Two
        // class isolates and 200 of their errors, both JUnit 4 test classes with one @Test(expected=...) among
        // hundreds of plain ones. A default value costs nothing -- nothing in an isolate is ever read -- so give
        // every attribute one, and the shape follows the printer: an attribute carrying a default is NOT abstract,
        // and its default is a single return statement (MethodPrinterImpl.annotationDefaultValue).
        Expression defaultValue = annotationDefaultValue(returnType);
        MethodInfo newAttr = runtime.newMethod(stub, origAttr.name(),
                defaultValue == null ? runtime.methodTypeAbstractMethod() : runtime.methodTypeMethod());
        newAttr.builder()
                .setReturnType(returnType)
                .setAccess(runtime.accessPackage())
                .setSource(runtime.noSource())
                // empty (not null) body when there is no default: it is printed as ';' since the method is then
                // abstract, but a null body would trip the import computer's methodBody().typesReferenced()
                .setMethodBody(defaultValue == null ? runtime.emptyBlock()
                        : runtime.newBlockBuilder()
                        .addStatement(runtime.newReturnBuilder().setExpression(defaultValue).build()).build())
                .commit();
        stub.builder().addMethod(newAttr);
        methodMap.put(key, newAttr);
        return newAttr;
    }

    /**
     * A value of {@code pt} usable as an annotation attribute's {@code default}, or null when we cannot make one.
     * <p>
     * An annotation value must be a constant, an array of them, a class literal, an enum constant or a nested
     * annotation — {@code null} is not among them, so {@link Runtime#nullValue} is of no use here. The last two
     * are the ones this does not produce: an enum constant needs a constant that exists on the stub, and a nested
     * annotation needs every one of ITS attributes to have a default. Both return null, leaving the attribute
     * abstract exactly as before — which is right whenever the original had no default either, and short of the
     * mark only for a bare use of an annotation whose default was of one of those two kinds.
     */
    private Expression annotationDefaultValue(ParameterizedType pt) {
        if (pt.arrays() > 0) {
            return runtime.newArrayInitializerBuilder().setExpressions(List.of())
                    .setCommonType(runtime.newParameterizedType(pt.typeInfo(), pt.arrays() - 1)).build();
        }
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo == null) return null;
        if (pt.isPrimitiveExcludingVoid()) {
            if (typeInfo.isBoolean()) return runtime.newBoolean(false);
            if (typeInfo.isChar()) return runtime.newChar('\0');
            if (typeInfo.isLong()) return runtime.newLong(0L);
            if (typeInfo.isFloat()) return runtime.newFloat(0f);
            if (typeInfo.isDouble()) return runtime.newDouble(0d);
            if (typeInfo.isShort()) return runtime.newShort((short) 0);
            if (typeInfo.isByte()) return runtime.newByte((byte) 0);
            return runtime.newInt(0);
        }
        if ("java.lang.String".equals(typeInfo.fullyQualifiedName())) return runtime.newStringConstant("");
        if ("java.lang.Class".equals(typeInfo.fullyQualifiedName())) {
            // the type argument is a BOUND to satisfy: 'Class<? extends Throwable> expected()' takes Throwable.class,
            // and Object.class would be "incompatible types"
            ParameterizedType bound = pt.parameters().isEmpty() ? null : pt.parameters().getFirst();
            TypeInfo literal = bound == null || bound.typeInfo() == null
                    ? runtime.objectParameterizedType().typeInfo() : bound.typeInfo();
            // the builder takes the REFERENCED type ('String.class' -> String) and derives the expression's own
            // type ('Class<String>') from it. Setting parameterizedType as well overwrites the first with the
            // second, and the literal prints as 'Class.class': "incompatible types: Class<Class> cannot be
            // converted to Class<? extends Throwable>"
            return runtime.newClassExpressionBuilder(literal.asSimpleParameterizedType()).build();
        }
        return null;
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

        /**
         * The type a called method's stub belongs on: its DECLARING type when the call went through a subtype.
         * <p>
         * The scope type is still stubbed by the caller — it may be referenced nowhere else — but the method is
         * placed where the original declares it, exactly as the {@code FieldReference} case below places an
         * inherited field. Two reasons, and the second is what a class isolate trips over: the {@code methodMap}
         * dedup is keyed by (owner, method), so the same declared method reached through two subtypes would be
         * declared twice and dropped from the supertype stub; and the scope type is the WRONG place for anything
         * generic. {@code ItemStack extends ListStack<Item>} inherits {@code T pop()}; on {@code ItemStack} the
         * {@code T} is out of scope, so {@code eraseOutOfScope} erases it and the stub reads
         * {@code public Object pop()} — "incompatible types: Object cannot be converted to Item" at every call
         * site. On {@code ListStack<T>} the parameter is in scope, and {@code extends ListStack<Item>} substitutes
         * it back for the caller, which is how the original resolved the call in the first place.
         * <p>
         * There is no "declared on the scope type itself" case to write: {@code ensureType} of that type IS the
         * scope's stub. The fallbacks are the receivers that have no stub to speak of — a JDK type (which
         * {@code ensureMethodInfo} drops anyway, and which must not be stubbed), and a local or anonymous declaring
         * type, which has no name a stub could be placed under. {@code scope} is itself already a stub, except when
         * the receiver was typed by a type parameter the isolated code declares: {@code ensureTypes} keeps those as
         * they are, so {@code erasedOwner} then answers the real bound and {@code stubFor} sorts it out.
         */
        private TypeInfo declaringOwner(MethodInfo methodInfo, ParameterizedType scopeType) {
            TypeInfo scope = erasedOwner(scopeType);
            TypeInfo declaring = methodInfo.typeInfo();
            if (declaring.enclosingMethod() != null || isJdkType(declaring)) return scope;
            // reached through an isolated type's own name: the type standing in for it owns the method (IsolateMethod
            // renames the frame, so its self-stub and its frame are two different types). For a sibling isolate the
            // declaration is kept verbatim there, and ensureMethodInfo will decline to stub it a second time
            if (isIsolated(declaring)) return originalTypeStub(declaring);
            return ensureType(declaring, null);
        }

        /**
         * Everything a type declared INSIDE a kept body contributes — an anonymous class or a local one. The
         * declaration itself is verbatim text and needs no stub; what it references does, and nothing else reaches
         * it, because {@code TypeInfo.visit()} is unsupported and the block visitor does not descend into a member
         * type. So: its supertypes, its members' signatures and bodies, and its fields' initializers.
         */
        private void visitNestedTypeBodies(TypeInfo nested, DetailedSources ds) {
            if (nested.parentClass() != null) ensureTypes(nested.parentClass(), ds);
            nested.interfacesImplemented().forEach(itf -> ensureTypes(itf, ds));
            nested.constructorAndMethodStream().forEach(mi -> {
                mi.parameters().forEach(pi -> ensureTypes(pi.parameterizedType(), ds(pi)));
                if (mi.hasReturnValue()) ensureTypes(mi.returnType(), ds(mi));
                Block body = mi.methodBody();
                if (body != null) body.visit(this);
                // 'new Handler() { public Object getObject(String s) {...} }' is verbatim text carrying an
                // override, so the supertype STUB has to declare what it overrides -- otherwise javac reports
                // "does not override or implement a method from a supertype" (17 of the hundred class isolates).
                // Same rule IsolateClass applies to the kept members themselves; these are kept just as verbatim.
                mi.overrides().forEach(overridden -> {
                    TypeInfo declaringStub = ensureType(overridden.typeInfo(), null);
                    if (declaringStub != null && declaringStub != overridden.typeInfo()) {
                        ensureMethodInfo(declaringStub, overridden);
                    }
                });
            });
            nested.fields().forEach(fi -> {
                ensureTypes(fi.type(), detailedSources(fi.source()));
                if (fi.initializer() != null) fi.initializer().visit(this);
            });
        }

        @Override
        public boolean test(Element element) {
            switch (element) {
                // a bare type-expression is the qualifier of a static access; the field/method/constructor cases
                // already stub its owner (routing a written 'X.member' to the original-type stub). Skip the original
                // type here so an implicit self-qualifier ('staticMethod()', 'LOGGER') does not materialise an empty
                // 'class X' stub; other types still get stubbed (e.g. a written 'Other.member')
                case TypeExpression te -> {
                    // ... only the type we are IN: a written 'Other.member' naming a sibling isolate is evidence
                    // for the import list, and must not be skipped
                    if (te.parameterizedType().typeInfo() != currentOriginalType) {
                        ensureTypes(te.parameterizedType(), ds(te));
                    }
                }
                case LocalVariableCreation lvc -> ensureTypes(lvc.localVariable().parameterizedType(), ds(lvc));
                // A class declared INSIDE a kept method body ('class ByWeight implements Comparator<Data> {…}').
                // The declaration is verbatim text and needs no stub of its own, but everything it references does,
                // and nothing else reaches it: TypeInfo.visit() is unsupported and the block visitor does not
                // descend into a member type's bodies. So a field read only there ('x.weight') was never stubbed
                // and the isolate failed on "cannot find symbol: variable weight". The anonymous-class branch below
                // was already doing this for its own kind; a local class is the same thing with a name.
                case LocalTypeDeclaration ltd -> visitNestedTypeBodies(ltd.typeInfo(), ds(ltd));
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
                // 'super(enabledLogging)' in a kept constructor. An explicit constructor invocation is neither a
                // MethodCall nor a ConstructorCall, so nothing reached it, and the supertype stub was left with
                // only its implicit no-arg constructor: "constructor Base cannot be applied to given types".
                // addDefaultConstructorsWhereExtended is the mirror image of this -- it supplies the NO-ARG
                // constructor a stub needs when it declares others -- and neither covers the other's case.
                // Measured on the closed-core class-isolate corpus, 2026-08-09: 24 of the 54 trees that did not
                // compile, 9 of them subclasses of a single base class.
                case ExplicitConstructorInvocation eci -> {
                    // 'this(...)' targets a constructor of the isolated type itself, which keeps its own
                    // constructors verbatim, so only the 'super' direction needs a stub
                    if (eci.isSuper() && eci.methodInfo() != null) {
                        ensureMethodInfo(superTypeStubOf(eci.methodInfo()), eci.methodInfo());
                    }
                }
                case ConstructorCall cc -> {
                    if (cc.anonymousClass() != null) {
                        // the supertype is what the verbatim text names ('new Comparator<X>() {...}'), and an
                        // interface has no constructor, so the cc.constructor() branch below never reaches it
                        visitNestedTypeBodies(cc.anonymousClass(), ds(cc));
                    }
                    if (cc.constructor() != null) {
                        // stub the constructed type first, so the constructor lands on the stub, not the real type
                        ParameterizedType constructed = ensureTypes(cc.parameterizedType(), ds(cc));
                        // 'new IParameter[n]' carries a SYNTHETIC constructor -- Factory.newArrayCreationConstructor,
                        // one int parameter per dimension -- which stands for the array creation, not for a declaration
                        // the constructed type has. Stubbing it wrote 'IParameter(int dim0) { }' into the type, and
                        // when that type is an interface the emitted unit is not even syntactically Java: javac says
                        // "<identifier> expected". The array type itself is already reproduced by ensureTypes above.
                        if (!cc.constructor().isSyntheticArrayConstructor()) {
                            ensureMethodInfo(constructed.typeInfo(), cc.constructor());
                        }
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
                    } else if ((mc.object() == null || mc.object().source() == null)
                               && mc.methodInfo().isStatic()
                               && mc.methodInfo().typeInfo() != currentOriginalType
                               && recordStaticImport(mc.methodInfo().typeInfo(), mc.methodInfo().name())) {
                        // taken over by a static import: stub it on its real owner, not on the isolated type
                        TypeInfo declaringStub = ensureType(mc.methodInfo().typeInfo(), null);
                        ensureMethodInfo(declaringStub, mc.methodInfo());
                        return true;
                    } else if (mc.object() == null
                        || mc.object().source() == null
                           && mc.methodInfo().isStatic() && mc.methodInfo().typeInfo() == currentOriginalType
                        || mc.object() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = selfType();
                    } else {
                        ParameterizedType scopeType = ensureTypes(mc.object().parameterizedType(), ds(mc.object()));
                        owner = declaringOwner(mc.methodInfo(), scopeType);
                    }
                    ensureMethodInfo(owner, mc.methodInfo());
                }
                case MethodReference mr -> {
                    // 'scope::method' (or 'Type::new'): the referenced method must be stubbed just like a call. A
                    // written scope ('C::helper', source present) routes through the original-type stub via
                    // ensureTypes(scope); only a synthetic scope or 'this::' belongs on the frame
                    TypeInfo owner;
                    if (mr.scope().source() == null && mr.methodInfo().typeInfo() == currentOriginalType
                        || mr.scope() instanceof VariableExpression ve && ve.variable() instanceof This) {
                        owner = selfType();
                    } else {
                        ParameterizedType scopeType = ensureTypes(mr.scope().parameterizedType(), ds(mr.scope()));
                        owner = declaringOwner(mr.methodInfo(), scopeType);
                    }
                    // 'Policy[]::new' carries the same synthetic array-creation constructor as 'new Policy[n]',
                    // and stubbing it writes 'Policy(int dim0) { }' into the type -- for an interface, not even
                    // syntactically Java. The ConstructorCall branch above has always guarded against it; this one
                    // did not, and one class isolate reached it (…toArray(ICheckRMTaskPolicy[]::new))
                    if (mr.methodInfo().isSyntheticArrayConstructor()) return true;
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
                        if (fr.isDefaultScope() && fr.fieldInfo().isStatic()
                            && fr.fieldInfo().owner() != currentOriginalType
                            && recordStaticImport(fr.fieldInfo().owner(), fr.fieldInfo().name())) {
                            // 'REMOVE_SHARES' with no scope, declared on FormulaOperatorConstant: a
                            // static import is the only thing that makes the verbatim spelling resolve
                            owner = ensureType(fr.fieldInfo().owner(), null);
                        } else if (fr.isDefaultScope() || fr.scope() instanceof VariableExpression sve
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
                            // a field reached through an isolated type's own name ('C.DAYS') must land on the type
                            // carrying that name, not on the renamed frame, or the verbatim 'C.DAYS' will not resolve
                            owner = isIsolated(declaringType) ? originalTypeStub(declaringType)
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
