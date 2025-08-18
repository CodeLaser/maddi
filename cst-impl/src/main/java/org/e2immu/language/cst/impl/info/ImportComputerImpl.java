package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.QualificationImpl;
import org.e2immu.language.cst.impl.output.TypeNameImpl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportComputerImpl implements ImportComputer {
    private final int minStar;
    private final Function<String, Collection<TypeInfo>> typesPerPackage;

    public ImportComputerImpl() {
        this(4, null);
    }

    public ImportComputerImpl(int minStar, Function<String, Collection<TypeInfo>> typesPerPackage) {
        this.minStar = minStar;
        this.typesPerPackage = typesPerPackage;
    }

    private static class PerPackage {
        final List<TypeInfo> types = new LinkedList<>();
    }

    public Result go(TypeInfo typeInfo, Qualification q) {
        Set<TypeInfo> typesReferenced = typeInfo.typesReferenced()
                .filter(Element.TypeReference::explicit)
                .map(Element.TypeReference::typeInfo)
                .map(TypeInfo::primaryType)
                .filter(ImportComputerImpl::allowInImport)
                .collect(Collectors.toSet());
        Map<String, PerPackage> typesPerPackage = new HashMap<>();
        QualificationImpl qualification;
        if (q == null) {
            qualification = new QualificationImpl(false, TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, null);
        } else {
            qualification = new QualificationImpl(q.doNotQualifyImplicit(), q.typeNameRequired(), q.decorator());
        }
        String myPackage = typeInfo.packageName();
        typesReferenced.forEach(ti -> {
            String packageName = ti.packageName();
            if (packageName != null && !myPackage.equals(packageName)) {
                boolean doImport = qualification.addTypeReturnImport(ti);
                if (doImport) {
                    PerPackage perPackage = typesPerPackage.computeIfAbsent(packageName, p -> new PerPackage());
                    perPackage.types.add(ti);
                }
            }
        });
        // IMPROVE static fields and methods
        // IMPROVE order of imports: for now, we simply do alphabetic, and ensure there are no conflicts
        Set<String> imports = new TreeSet<>();
        for (Map.Entry<String, PerPackage> e : typesPerPackage.entrySet()) {
            PerPackage perPackage = e.getValue();
            if (perPackage.types.size() < minStar || conflict(e.getKey(), typesReferenced)) {
                for (TypeInfo ti : perPackage.types) {
                    imports.add(ti.fullyQualifiedName());
                }
            } else {
                imports.add(perPackage.types.getFirst().packageName() + ".*");
            }
        }
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
