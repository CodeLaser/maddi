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

public class IsolateMethod {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolateMethod.class);
    public static final String METHOD_MARKER_NAME = "_method_to_be_replaced_";

    private final JavaInspector javaInspector;
    private final Runtime runtime;
    private final String targetPackage;
    // 'Frame.p.q.Value' rather than 'Value'; see OutOfScopeQualified
    private final TypeNameRequired qualifiedFromPrimaryType;

    public IsolateMethod(JavaInspector javaInspector, String targetPackage) {
        this.javaInspector = javaInspector;
        this.runtime = javaInspector.runtime();
        this.targetPackage = targetPackage;
        this.qualifiedFromPrimaryType = runtime.qualificationQualifyFromPrimaryType().typeNameRequired();
    }

    /**
     * @param jdkTypesToImport    the JDK types the frame references and that may be imported
     * @param jdkTypesNotToImport JDK types the frame references whose simple name is owned by another type in
     *                            {@code jdkTypesToImport}; they must be printed fully qualified. See
     *                            {@link #arbitrateJdkImports}
     */
    public record Result(TypeInfo typeInfo, Set<TypeInfo> jdkTypesToImport, Set<TypeInfo> jdkTypesNotToImport) {
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
        // adding the winner is not enough: the computer also collects the types it finds while walking the unit, so
        // the loser would be imported anyway -- and a single-type import beats the on-demand one the winner may
        // have been collapsed into. It has to be vetoed explicitly.
        result.jdkTypesNotToImport.forEach(importComputer::doNotImport);
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
        TypePrinter.MethodPrinterFactory methodPrinterFactory = (owner, mi, formatter2) -> {
            if (mi.name().equals(METHOD_MARKER_NAME)) {
                return _ -> runtime.newOutputBuilder().add(runtime.newText(methodString));
            }
            if (overrideSuper != null && mi.typeInfo() == overrideSuper) {
                return _ -> runtime.newMethodPrinter(owner, mi, formatter2)
                        .print(runtime.qualificationQualifyFromPrimaryType());
            }
            return q -> runtime.newMethodPrinter(owner, mi, formatter2)
                    .print(new OutOfScopeQualified(q, owner));
        };
        TypePrinter.FieldPrinterFactory fieldPrinterFactory =
                (fi, formatter2) -> (q, asRecordParameter) -> runtime.newFieldPrinter(fi, formatter2)
                        .print(new OutOfScopeQualified(q, fi.owner()), asRecordParameter);
        // the nested stubs are where the out-of-scope references live, and a nested type is printed by the
        // *enclosed* factory -- whose two-argument print() resets to the default printers. Re-inject on the way down
        TypePrinter.EnclosedTypePrinterFactory enclosedTypePrinterFactory =
                new PropagatingTypePrinterFactory(methodPrinterFactory, fieldPrinterFactory);
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(importComputer, qualification,
                        runtime::newTypePrinter,
                        methodPrinterFactory,
                        fieldPrinterFactory,
                        enclosedTypePrinterFactory);
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }

    public Result isolate(MethodInfo methodInfo) {
        return isolate(methodInfo, null);
    }

    /**
     * One traversal: a fresh compilation unit, a fresh frame, and every stub the method reaches. Run twice per
     * isolate — see {@link #isolate(MethodInfo, String)} for why — so it must not touch anything outside the
     * {@link MethodStubs} it returns. It does not: it only reads the original CST and constructs new, uncommitted CST
     * objects, which the probe round simply drops.
     *
     * @param known the placement evidence of a previous round, or null to decide on the reference at hand
     */
    private MethodStubs buildData(MethodInfo methodInfo, org.e2immu.language.cst.api.runtime.Runtime runtime,
                           String simpleName, Placement known) {
        TypeInfo originalType = methodInfo.typeInfo().primaryType();
        CompilationUnit newCu = runtime.newCompilationUnitBuilder()
                .setPackageName(targetPackage)
                .setSourceSet(originalType.compilationUnit().sourceSet())
                .setURI(originalType.compilationUnit().uri())
                .build();
        TypeInfo frame = runtime.newTypeInfo(newCu, simpleName);
        frame.builder().setSource(runtime.noSource())
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addTypeModifier(runtime.typeModifierPublic())
                .computeAccess();

        MethodStubs data = new MethodStubs(methodInfo, frame, known);
        // The frame stands in for the type that declares the method, so it must stand in for that type's type
        // parameters too. A method of 'class Query<T>' whose body says 'T' is pasted verbatim into the frame,
        // and with no '<T>' on the frame that T resolves to nothing -- the unit is dropped on "Type T not
        // found". Mapping them as well lets ensureTypes translate reconstructed signatures onto the frame's own
        // parameters instead of leaving the originals dangling. (Three of closed-core's isolates, including
        // GetInvoiceQuery.getInvoiceQuery on a generic query class.)
        List<TypeParameter> declaringTps = methodInfo.typeInfo().typeParameters();
        List<TypeParameter> frameTps = new ArrayList<>(declaringTps.size());
        for (TypeParameter origTp : declaringTps) {
            TypeParameter newTp = runtime.newTypeParameter(origTp.getIndex(), origTp.simpleName(), frame);
            data.typeParameterMap.put(origTp, newTp);
            frame.builder().addOrSetTypeParameter(newTp);
            frameTps.add(newTp);
        }
        // bounds in a second pass: a bound may reference a sibling parameter, or this one ('T extends X<T>')
        for (int i = 0; i < frameTps.size(); i++) {
            List<ParameterizedType> bounds = declaringTps.get(i).typeBounds().stream()
                    .map(data::ensureTypes).toList();
            frameTps.get(i).builder().setTypeBounds(bounds).commit();
        }
        data.visitMethod(methodInfo);
        data.addDummyInterfaceMethods();
        data.addDefaultConstructorsWhereExtended();
        return data;
    }

    public Result isolate(MethodInfo methodInfo, String customClassName) {
        TypeInfo originalType = methodInfo.typeInfo().primaryType();
        org.e2immu.language.cst.api.runtime.Runtime runtime = javaInspector.runtime();
        String simpleName = customClassName == null
                ? originalType.simpleName() + "_" + methodInfo.name()
                : customClassName;

        /*
         Where a stub goes is decided by how the VERBATIM text names it, and one type can be named more than once.
         Deciding on the first reference is wrong whenever a reconstructed one -- which carries no evidence at all
         -- happens to arrive before a written one: the type lands in the frame, and a later
         'com.example.legacy.parameter.ObjectId' in the pasted body then names something that does not exist.
         (closed-core RecordDTO.set: thirteen package-siblings in the namespace chain, that one in the frame.)

         A stub's enclosing type is final, set when it is constructed, so placement cannot be corrected afterwards.
         Instead the whole traversal runs twice: a probe round whose stubs are thrown away and whose only output is
         the Placement evidence, then the round that keeps them, seeded with it. Both rounds execute exactly the
         same code, so -- unlike a scan-only mode, or a second visitor that re-implements which constructs carry a
         written type name -- there is nothing that can drift out of step with the traversal it describes.
         */
        MethodStubs probe = buildData(methodInfo, runtime, simpleName, null);
        MethodStubs data = buildData(methodInfo, runtime, simpleName, probe.observed);
        TypeInfo frame = data.frame;
        CompilationUnit newCu = frame.compilationUnit();

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
                .sorted(Comparator.comparingInt(IsolationCore::enclosingDepth).reversed()
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
        Set<TypeInfo> importable = arbitrateJdkImports(data);
        Set<TypeInfo> notImportable = new HashSet<>(data.jdkTypesToImport);
        notImportable.removeAll(importable);
        return new Result(frame, importable, notImportable);
    }

    /**
     * Two JDK types can share a simple name — {@code java.sql.Date} and {@code java.util.Date} being the case that
     * cost us an isolate. Only one of them can be imported into the frame, because a single-type import beats an
     * on-demand one: with both {@code import java.sql.*} (the import computer collapses four or more types of a
     * package) and {@code import java.util.Date} in scope, every bare {@code Date} in the pasted text means
     * {@code java.util.Date}, and {@code PreparedStatement.setDate(int, java.sql.Date)} then rejects it.
     * <p>
     * The pasted text is verbatim and cannot be respelled, so the type it names by its simple name is the one that
     * has to own the name. The loser is simply left out of the import computer, which prints it fully qualified
     * wherever a reconstructed signature mentions it — the same mechanism {@link #addJdkImports} already relies on
     * for a stub that shadows its own JDK supertype.
     */
    private static Set<TypeInfo> arbitrateJdkImports(MethodStubs data) {
        Map<String, List<TypeInfo>> bySimpleName = new HashMap<>();
        for (TypeInfo t : data.jdkTypesToImport) {
            bySimpleName.computeIfAbsent(t.simpleName(), _ -> new ArrayList<>()).add(t);
        }
        Set<TypeInfo> importable = new HashSet<>();
        for (Map.Entry<String, List<TypeInfo>> entry : bySimpleName.entrySet()) {
            List<TypeInfo> candidates = entry.getValue();
            if (candidates.size() == 1) {
                importable.add(candidates.getFirst());
                continue;
            }
            // sorted first, so the outcome does not depend on the iteration order of a hash set
            List<TypeInfo> sorted = candidates.stream()
                    .sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
            TypeInfo winner = sorted.stream().filter(data.jdkNamedSimplyInSource::contains)
                    .findFirst().orElse(sorted.getFirst());
            importable.add(winner);
            LOGGER.info("Simple name '{}' claimed by {} JDK types; importing {}, qualifying the rest",
                    entry.getKey(), candidates.size(), winner);
        }
        return importable;
    }

    private static boolean isOverride(MethodInfo methodInfo) {
        return methodInfo.annotations().stream()
                .anyMatch(ae -> "java.lang.Override".equals(ae.typeInfo().fullyQualifiedName()));
    }

    // an abstract class 'X_method_super' declaring the isolated method as abstract (signature reproduced with the
    // frame's stubbed types); the frame extends it, so an '@Override' on the method resolves to a real supertype method
    private TypeInfo createOverrideSupertype(MethodStubs data, MethodInfo methodInfo, CompilationUnit newCu, TypeInfo frame) {
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

    /**
     * How the pasted text spells each type it names. Gathered by a probe round over the very same traversal that
     * builds the stubs, and fed back into the round that keeps them; see {@link #isolate(MethodInfo, String)}.
     * <p>
     * A written reference is verbatim text whose spelling cannot be changed, so it dictates where the stub has to
     * be declared: {@code Helper} needs the type nested directly in the frame (a class inside a namespace chain is
     * not in scope by its simple name), {@code p.q.Helper} needs the chain. A reference we reconstruct carries no
     * such constraint — {@code OutOfScopeQualified} prints it whichever way resolves.
     */
    static final class Placement {
        final Set<TypeInfo> writtenQualified = new HashSet<>();
        final Set<TypeInfo> writtenSimply = new HashSet<>();
        // a member type named through its enclosing type ('Outer.Inner'), which requires the nesting be reproduced
        final Set<TypeInfo> writtenWithEnclosing = new HashSet<>();
    }

    /**
     * Placement for a single compilation unit: every stub is nested inside the frame, either at its top level
     * (reachable by simple name) or in a chain of namespace stubs reproducing its package (reachable fully
     * qualified). Which one is right is dictated by how the pasted text spells the type, over all its
     * references — hence {@link Placement} and the probe round.
     */
    class MethodStubs extends IsolationCore {
        private final TypeInfo frame;
        // what this round observes; becomes the 'known' of the next round
        final Placement observed = new Placement();
        // what the probe round observed, or null in the probe round itself (then placement falls back to deciding
        // on the reference at hand, which is what it did before this was introduced)
        private final Placement known;
        // package name -> a stub 'namespace' type reproducing that package, so a type referenced by its fully
        // qualified name (e.g. 'org.slf4j.Logger') keeps resolving and does not collide with a simply-named stub
        final Map<String, TypeInfo> namespaceMap = new HashMap<>();
        // simple names of the original type's member types: they are always referenced simply, so they get first
        // claim on a frame slot; an external type with the same simple name must fall back to a namespace stub
        final Set<String> originalMemberSimpleNames = new HashSet<>();
        // simple name -> the (original) primary type that occupies that slot in the frame; used to detect a genuine
        // simple-name collision, the situation that in source forces a fully-qualified (namespace) reference
        final Map<String, TypeInfo> frameSimpleNameClaims = new HashMap<>();
        // a stub carrying the original type's simple name (e.g. 'C'), nested in the frame ('C_method'). The frame is
        // renamed, so a self-reference written with the original name ('C.DAYS') no longer resolves to it; this stub
        // gives that name back. Created lazily, only when such a reference is seen
        TypeInfo originalTypeStub;

        MethodStubs(MethodInfo originalMethod, TypeInfo frame, Placement known) {
            super(IsolateMethod.this.javaInspector, originalMethod.primaryType());
            this.frame = frame;
            this.known = known;
            originalType.recursiveSubTypeStream()
                    .filter(t -> t != originalType)
                    .forEach(t -> originalMemberSimpleNames.add(t.simpleName()));
        }

        @Override
        TypeInfo selfType() {
            return frame;
        }

        @Override
        TypeInfo placeStub(TypeInfo typeInfo, DetailedSources ds) {
            // reproduce the nesting: a member type 'Ext.Pair' becomes 'Pair' nested in the stub of 'Ext', so that
            // qualified references in the (verbatim) method text keep resolving. Types nested in the original type,
            // and primary types, are nested directly in the frame.
            var enclosing = typeInfo.compilationUnitOrEnclosingType();
            TypeInfo enclosingStub;
            if (enclosing.isRight() && enclosing.getRight() == originalType) {
                // nested in the original type: the frame stands in for that type, so nest directly in it
                enclosingStub = frame;
            } else if (enclosing.isRight()
                       && !enclosingWrittenOverAllReferences(typeInfo, enclosing.getRight(), ds)
                       && nestingWouldHide(typeInfo)) {
                // A MEMBER TYPE REACHED WITHOUT SYNTACTIC EVIDENCE (ds == null): the reference comes from the
                // reconstructed model -- a stubbed field's type, a supertype, a type argument -- not from a
                // written-out 'Outer.Inner' in the pasted text. Reproducing the nesting there puts the
                // declaration in a sibling branch of the frame, where Java's scoping does not resolve its simple
                // name, and the whole unit is dropped on "Type Inner not found". Measured on closed-core:
                // ParamDouble declared five levels deep and used two levels deep; RowFilter and
                // DataSourceType likewise -- four of the ten isolates that would not parse back.
                //
                // A type nested DIRECTLY in the frame is in scope throughout the frame, including inside every
                // other stub, so that is where such a reference has to go. When ds IS present the reference was
                // written out and the nesting is what makes it resolve, so the branch below reproduces it --
                // the same evidence rule the primary-type branches use.
                frameSimpleNameClaims.put(typeInfo.simpleName(), typeInfo);
                enclosingStub = frame;
            } else if (enclosing.isRight()) {
                enclosingStub = ensureType(enclosing.getRight(), ds);
            } else if (Boolean.TRUE.equals(writtenPlacement(typeInfo))
                       || known == null && ds != null && ds.detail(typeInfo.packageName()) != null) {
                // the text writes the package out (fully-qualified, e.g. 'a.b.C') SOMEWHERE -- not necessarily in
                // the reference that got us here first: reproduce the package as a chain of namespace stubs so the
                // qualified reference still resolves
                enclosingStub = namespaceStub(typeInfo.packageName());
            } else if (Boolean.FALSE.equals(writtenPlacement(typeInfo)) || known == null && ds != null) {
                // the text names it simply somewhere: only a type nested directly in the frame is in scope by its
                // simple name throughout the frame
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
            return runtime.newTypeInfo(enclosingStub, typeInfo.simpleName());
        }

        // no detailed sources means the reference is reconstructed, not written, and constrains nothing
        @Override
        void recordPlacementEvidence(TypeInfo typeInfo, DetailedSources ds) {
            if (ds == null) return;
            var enclosing = typeInfo.compilationUnitOrEnclosingType();
            if (enclosing.isRight()) {
                if (enclosingWritten(enclosing.getRight(), ds)) observed.writtenWithEnclosing.add(typeInfo);
            } else if (ds.detail(typeInfo.packageName()) != null) {
                observed.writtenQualified.add(typeInfo);
            } else {
                observed.writtenSimply.add(typeInfo);
            }
        }



        /**
         * Where {@code typeInfo} has to be declared, over <i>all</i> the references the pasted text makes to it:
         * {@code TRUE} for a namespace chain, {@code FALSE} for directly in the frame, {@code null} when the text
         * never names it (every reference is reconstructed) and the caller is free to choose.
         */
        private Boolean writtenPlacement(TypeInfo typeInfo) {
            if (known == null) return null;
            boolean simply = known.writtenSimply.contains(typeInfo);
            boolean qualified = known.writtenQualified.contains(typeInfo);
            if (simply && qualified) {
                // no single declaration is in scope both ways. The simple spelling is by far the commoner one, so
                // it keeps the frame slot and the qualified reference is the one that will not resolve
                LOGGER.warn("{} is written both simply and package-qualified; the qualified references will not"
                            + " resolve in the isolate", typeInfo);
                return Boolean.FALSE;
            }
            return simply ? Boolean.FALSE : qualified ? Boolean.TRUE : null;
        }

        private boolean enclosingWrittenOverAllReferences(TypeInfo typeInfo, TypeInfo enclosingType,
                                                          DetailedSources ds) {
            if (known == null) return enclosingWritten(enclosingType, ds);
            return known.writtenWithEnclosing.contains(typeInfo);
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

        /**
         * Would nesting this member type under its enclosing type's stub put its simple name out of scope?
         * <p>
         * It would, unless the frame already needs that name for something else. Java resolves a nested type's
         * simple name only inside its own enclosing type, so a stub nested two or more levels deep is invisible
         * to stubs in other branches -- while a stub nested directly in the frame is visible everywhere in it.
         * The one case where the nesting must be kept is a genuine simple-name collision: two different types
         * with the same simple name cannot both occupy the frame's top level, and in the original source such a
         * reference is necessarily written qualified, which the nested placement reproduces.
         */
        private boolean nestingWouldHide(TypeInfo typeInfo) {
            String simpleName = typeInfo.simpleName();
            if (originalMemberSimpleNames.contains(simpleName)) return false;
            TypeInfo claimer = frameSimpleNameClaims.get(simpleName);
            return claimer == null || claimer == typeInfo;
        }
    }


    /**
     * The pasted body and a reconstructed signature disagree about how deeply nested stubs are spelled, and both
     * have to reach the same declaration.
     * <p>
     * A type written fully qualified in the source ({@code (com.example.legacy.parameter.ParamDouble) value}) is
     * reproduced as a namespace stub, nested {@code frame → com → example → legacy → parameter}: the body is
     * verbatim text, so nothing else would keep it resolving. But a signature we reconstruct — a stub field, a
     * stub method's return or parameter type — is printed with simple names, and {@code ParamDouble} does not
     * resolve from a sibling branch of the nesting tree. Flattening the type into the frame is not an option: it
     * would break the 18 fully-qualified casts in that one method alone.
     * <p>
     * So the reconstructed side is the one that gives way. This qualification prints a referenced stub as
     * {@code Frame.com.example.legacy.parameter.ParamDouble} — but only where the simple name genuinely does not
     * resolve, so the ordinary isolate stays as readable as it was.
     */
    /**
     * {@link TypePrinter}'s two-argument {@code print} falls back to the default method/field/type printers, so a
     * custom printer handed to the compilation-unit printer only ever reaches the top-level types. This factory
     * hands it back down at every level of nesting, which is where the stubs -- and therefore the out-of-scope
     * references of {@link OutOfScopeQualified} -- actually are.
     */
    private class PropagatingTypePrinterFactory implements TypePrinter.EnclosedTypePrinterFactory {
        private final TypePrinter.MethodPrinterFactory methodPrinterFactory;
        private final TypePrinter.FieldPrinterFactory fieldPrinterFactory;

        private PropagatingTypePrinterFactory(TypePrinter.MethodPrinterFactory methodPrinterFactory,
                                              TypePrinter.FieldPrinterFactory fieldPrinterFactory) {
            this.methodPrinterFactory = methodPrinterFactory;
            this.fieldPrinterFactory = fieldPrinterFactory;
        }

        @Override
        public TypePrinter create(TypeInfo typeInfo, boolean formatter2) {
            TypePrinter delegate = runtime.newTypePrinter(typeInfo, formatter2);
            return new TypePrinter() {
                @Override
                public List<TypeModifier> minimalModifiers(TypeInfo typeInfo) {
                    return delegate.minimalModifiers(typeInfo);
                }

                @Override
                public OutputBuilder print(ImportComputer importComputer, Qualification qualification,
                                           boolean doTypeDeclaration) {
                    return delegate.print(importComputer, qualification, doTypeDeclaration);
                }

                @Override
                public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration) {
                    return delegate.print(importData, doTypeDeclaration, methodPrinterFactory, fieldPrinterFactory,
                            PropagatingTypePrinterFactory.this);
                }

                @Override
                public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                                           MethodPrinterFactory mpf, FieldPrinterFactory fpf,
                                           EnclosedTypePrinterFactory etpf) {
                    return delegate.print(importData, doTypeDeclaration, mpf, fpf, etpf);
                }
            };
        }
    }

    private class OutOfScopeQualified implements Qualification {
        private final Qualification delegate;
        private final TypeInfo from;

        private OutOfScopeQualified(Qualification delegate, TypeInfo from) {
            this.delegate = delegate;
            this.from = from;
        }

        @Override
        public TypeNameRequired qualifierRequired(TypeInfo typeInfo) {
            if (outOfScope(typeInfo)) return qualifiedFromPrimaryType;
            return delegate.qualifierRequired(typeInfo);
        }

        /**
         * Is {@code typeInfo}'s simple name unreachable from {@link #from}? A member type is in scope in its own
         * enclosing type and in everything nested inside it, so we walk {@code from} outwards and look for the
         * type itself or its enclosing type. Supertype members are not considered: over-qualifying is harmless
         * (the qualified form resolves wherever the simple one does), under-qualifying is what breaks the parse.
         */
        private boolean outOfScope(TypeInfo typeInfo) {
            // not declared here (a JDK type, or the @Override supertype): the import computer decides
            if (typeInfo.compilationUnit() != from.compilationUnit()) return false;
            if (typeInfo.isPrimaryType()) return false;
            TypeInfo enclosing = IsolationCore.enclosingTypeOrNull(typeInfo);
            for (TypeInfo t = from; t != null; t = IsolationCore.enclosingTypeOrNull(t)) {
                if (t == typeInfo || t == enclosing) return false;
            }
            return true;
        }

        @Override
        public boolean doNotQualifyImplicit() {
            return delegate.doNotQualifyImplicit();
        }

        @Override
        public boolean isFullyQualifiedNames() {
            return delegate.isFullyQualifiedNames();
        }

        @Override
        public boolean isSimpleOnly() {
            return delegate.isSimpleOnly();
        }

        @Override
        public boolean qualifierRequired(MethodInfo methodInfo) {
            return delegate.qualifierRequired(methodInfo);
        }

        @Override
        public boolean qualifierRequired(Variable variable) {
            return delegate.qualifierRequired(variable);
        }

        @Override
        public TypeNameRequired typeNameRequired() {
            return delegate.typeNameRequired();
        }

        @Override
        public Decorator decorator() {
            return delegate.decorator();
        }

        @Override
        public void addField(FieldInfo fieldInfo) {
            delegate.addField(fieldInfo);
        }

        @Override
        public void addUnqualifiedType(TypeInfo typeInfo) {
            delegate.addUnqualifiedType(typeInfo);
        }

        @Override
        public void addThis(This thisVar) {
            delegate.addThis(thisVar);
        }

        @Override
        public void addMethodUnlessOverride(MethodInfo methodInfo) {
            delegate.addMethodUnlessOverride(methodInfo);
        }
    }
}
