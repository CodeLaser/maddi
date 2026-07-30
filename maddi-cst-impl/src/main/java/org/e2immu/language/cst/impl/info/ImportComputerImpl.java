/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.impl.output.TypeNameImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportComputerImpl implements ImportComputer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportComputerImpl.class);

    private final int minStar;
    private final Function<String, Collection<TypeInfo>> typesPerPackage;
    private final Set<TypeInfo> extra = new HashSet<>();
    private final Set<TypeInfo> doNotImport = new HashSet<>();
    private final Set<String> extraStaticImports = new HashSet<>();

    public ImportComputerImpl() {
        this(4, null);
    }

    public ImportComputerImpl(int minStar, Function<String, Collection<TypeInfo>> typesPerPackage) {
        this.minStar = minStar;
        this.typesPerPackage = typesPerPackage;
    }

    @Override
    public void add(TypeInfo typeInfo) {
        extra.add(typeInfo);
    }

    @Override
    public void addStaticImport(String importString) {
        extraStaticImports.add(importString);
    }

    @Override
    public void doNotImport(TypeInfo typeInfo) {
        doNotImport.add(typeInfo);
    }

    /**
     * The qualifier recorded for a reference is the one the AUTHOR wrote, and that is the name verbatim text
     * needs in scope: {@code HashMap.Entry} needs {@code java.util.HashMap}. A printer, on the other hand,
     * renders a nested type down its DECLARING chain -- {@code Map.Entry} -- so that name has to resolve too.
     * The two coincide except when a nested type is named through a type that INHERITS it, and there a consumer
     * that copies text and one that prints the CST need different imports; hand out both rather than pick.
     * Returns {@code null} whenever the ordinary single import suffices.
     *
     * @see org.e2immu.language.cst.api.element.DetailedSources#qualifier(TypeInfo)
     */
    private static TypeInfo declaringQualifierIfDifferent(TypeInfo typeInfo, TypeInfo writtenQualifier) {
        if (writtenQualifier == typeInfo) return null; // written without qualification
        if (typeInfo.compilationUnitOrEnclosingType().isLeft()) return null; // top-level: nothing encloses it
        TypeInfo declaring = typeInfo.compilationUnitOrEnclosingType().getRight();
        return declaring == writtenQualifier ? null : declaring;
    }

    private static class PerPackage {
        final List<TypeInfo> types = new LinkedList<>();

        @Override
        public String toString() {
            return types.toString();
        }
    }

    public Result go(CompilationUnit compilationUnit, Qualification q) {
        QualificationImpl qualification;
        if (q == null) {
            qualification = new QualificationImpl(false,
                    TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, null);
        } else {
            qualification = new QualificationImpl(q.doNotQualifyImplicit(), q.typeNameRequired(), q.decorator());
        }

        Set<String> reservedNames = compilationUnit.types().stream()
                .flatMap(TypeInfo::recursiveSubTypeStream)
                .map(TypeInfo::simpleName).collect(Collectors.toUnmodifiableSet());
        // a type declared in this compilation unit owns its simple name: a referenced type with the same simple
        // name must be fully-qualified rather than imported, or the two would collide on the bare name
        reservedNames.forEach(qualification::reserveSimpleNameAgainstImport);
        compilationUnit.types().stream()
                .flatMap(ti -> ti.subTypes().stream()).forEach(qualification::addUnqualifiedType);

        /*
        there are 2 mechanisms to determine imports: duplicate naming (addTypeReturnImport)
        and TypeReferenceNature.FULLY_QUALIFIED.
         */
        Set<TypeInfo> typesReferenced = new HashSet<>(extra);
        boolean keepPrimary = qualification.typeNameRequired() == TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE;
        compilationUnit.types().stream()
                .flatMap(typeInfo -> typeInfo.typesReferenced(null))
                .filter(Element.TypeReference::explicit)
                .forEach(tr -> {
                    TypeInfo typeToImport = tr.typeToImport();
                    if (typeToImport != null) {
                        TypeInfo toImport;
                        if (keepPrimary) {
                            toImport = tr.typeInfo().primaryType();
                        } else if (reservedNames.contains(typeToImport.simpleName())) {
                            // see e.g. TestComposer, class OfField<F extends TypeDescriptor.OfField<F>>
                            toImport = typeToImport.primaryType();
                        } else {
                            toImport = typeToImport;
                        }
                        if (allowInImport(toImport)) {
                            typesReferenced.add(toImport);
                        }
                        if (!keepPrimary) {
                            TypeInfo declaring = declaringQualifierIfDifferent(tr.typeInfo(), typeToImport);
                            if (declaring != null) {
                                TypeInfo alsoImport = reservedNames.contains(declaring.simpleName())
                                        ? declaring.primaryType() : declaring;
                                if (allowInImport(alsoImport)) {
                                    typesReferenced.add(alsoImport);
                                }
                            }
                        }
                    } else {
                        qualification.addTypeNotImported(tr.typeInfo());
                    }
                });
        LOGGER.debug("Types referenced in {}: {}", compilationUnit, typesReferenced);

        String myPackage = compilationUnit.packageName();
        Map<String, PerPackage> typesPerPackage = new HashMap<>();
        // a type the caller has vetoed keeps its place in typesReferenced -- so conflict() still sees it and can
        // suppress an on-demand import that would collide with it -- but is never imported itself
        doNotImport.forEach(qualification::addTypeNotImported);
        typesReferenced.forEach(ti -> {
            String packageName = ti.packageName();
            // Sharing a package puts a TOP-LEVEL type's simple name in scope, but not a nested one's: to write
            // 'PaymentDocumentValues' for 'GenerateCompletePDParameters.PaymentDocumentValues' you need the
            // import even from inside its own package.
            //
            // Only for a type the CALLER asked for, though. When the computer merely discovers a same-package
            // nested type by walking the unit, the printer renders it 'Outer.Inner' and no import is wanted --
            // importing it anyway rewrites perfectly good output ('Front.Helpers' -> 'import a.b.Front.Helpers'
            // plus a bare 'Helpers'). A caller that pastes verbatim text the computer cannot read, as the
            // isolators do, adds such a type explicitly and does get the import.
            boolean sameUnit = ti.primaryType().compilationUnit() == compilationUnit;
            boolean inScopeWithoutImport = sameUnit
                                           || myPackage.equals(packageName)
                                              && (ti.isPrimaryType() || !extra.contains(ti));
            if (packageName != null && !inScopeWithoutImport && !doNotImport.contains(ti)) {
                boolean doImport = qualification.addTypeReturnImport(ti);
                LOGGER.debug("Do import of {}? {}", ti, doImport);
                if (doImport) {
                    PerPackage perPackage = typesPerPackage.computeIfAbsent(packageName, p -> new PerPackage());
                    perPackage.types.add(ti);
                }
            }
        });

        LOGGER.debug("Types per package: {}", typesPerPackage);
        Map<String, List<Comment>> originalComments = compilationUnit
                .importStatements().stream()
                .collect(Collectors.toUnmodifiableMap(ImportStatement::importString, Element::comments));

        // IMPROVE static fields and methods
        // IMPROVE order of imports: for now, we simply do alphabetic, and ensure there are no conflicts
        List<ImportDetails> imports = new ArrayList<>();
        for (Map.Entry<String, PerPackage> e : typesPerPackage.entrySet()) {
            PerPackage perPackage = e.getValue();
            if (perPackage.types.size() < minStar || conflict(e.getKey(), typesReferenced)) {
                for (TypeInfo ti : perPackage.types) {
                    String importString = ti.fullyQualifiedName();
                    imports.add(new ImportDetails(importString, originalComments.getOrDefault(importString, List.of())));
                }
            } else {
                List<Comment> comments = perPackage.types.stream().flatMap(ti ->
                                originalComments.getOrDefault(ti.fullyQualifiedName(), List.of()).stream())
                        .toList();
                imports.add(new ImportDetails(perPackage.types.getFirst().packageName() + ".*", comments));
            }
        }
        imports.sort(Comparator.comparing(ImportDetails::importString));
        /*
        Static imports are NOT computed: nothing in a CST asks for one, since the printer renders a member
        reference qualified and a static import is then dead (TestVariousPrint2Issues 'static import issue'
        pins that). Only a caller pasting verbatim text knows it needs one; addStaticImport is how it says so.
         */
        extraStaticImports.stream().sorted()
                .forEach(is -> imports.add(new ImportDetails("static " + is, originalComments.getOrDefault(is, List.of()))));
        return new Result(imports, qualification);
    }

    private boolean conflict(String packageWithStar, Set<TypeInfo> typesReferenced) {
        if (typesPerPackage == null) return true;
        Collection<TypeInfo> inPackageWithStar = typesPerPackage.apply(packageWithStar);
        Set<String> publicSimpleNamesAsSet = inPackageWithStar.stream()
                .filter(TypeInfo::isPubliclyAccessible)
                .map(TypeInfo::simpleName)
                .collect(Collectors.toUnmodifiableSet());
        for (TypeInfo referenced : typesReferenced) {
            if (!referenced.packageName().equals(packageWithStar) && publicSimpleNamesAsSet.contains(referenced.simpleName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean allowInImport(TypeInfo typeInfo) {
        return !"java.lang".equals(typeInfo.packageName())
               && !typeInfo.isPrimitiveExcludingVoid() && !typeInfo.isVoid();
    }
}
