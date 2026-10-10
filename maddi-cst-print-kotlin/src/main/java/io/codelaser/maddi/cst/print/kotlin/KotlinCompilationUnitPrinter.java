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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prints a {@link CompilationUnit} as a Kotlin file: the {@code package} line, the imports the
 * {@link ImportComputer} finds the printed code needs, and the types. A Java static import is an ordinary Kotlin
 * import ({@code import a.B.c}); names that are Kotlin keywords are escaped segment by segment. Comments are not
 * printed: the CST's comments are Java's, the licence header first among them.
 */
public record KotlinCompilationUnitPrinter(CompilationUnit compilationUnit, boolean formatter2,
                                           KotlinPrintOptions options) {

    public KotlinCompilationUnitPrinter(CompilationUnit compilationUnit, boolean formatter2) {
        this(compilationUnit, formatter2, KotlinPrintOptions.DEFAULT);
    }

    /** The Kotlin text, and the messages about what it does differently from the Java, or could not translate. */
    public record Result(OutputBuilder output, List<KotlinPrintMessage> messages) {
    }

    public OutputBuilder print(ImportComputer importComputer, Qualification qualification) {
        return printWithMessages(importComputer, qualification).output();
    }

    /** As {@link #print}, with the {@link KotlinPrintMessage}s of this file, in the order they were printed. */
    public Result printWithMessages(ImportComputer importComputer, Qualification qualification) {
        KotlinPrintOptions previous = KotlinContext.options(options);
        KotlinContext.takeMessages();
        try {
            OutputBuilder output = printFile(importComputer, qualification);
            return new Result(output, KotlinContext.takeMessages());
        } finally {
            KotlinContext.options(previous);
        }
    }

    private OutputBuilder printFile(ImportComputer importComputer, Qualification qualification) {
        CompilationUnitPrinterImpl.ImportDataImpl importData = new CompilationUnitPrinterImpl(compilationUnit, formatter2)
                .computeImportData(importComputer, qualification);
        KotlinContext.resetLabels();
        OutputBuilder out = new OutputBuilderImpl();
        String packageName = compilationUnit.packageName();
        if (!packageName.isEmpty()) {
            out.add(KeywordImpl.PACKAGE).add(SpaceEnum.ONE).add(new TextImpl(KotlinNames.dotted(packageName)))
                    .add(SpaceEnum.NEWLINE);
        }
        KotlinContext.takeReferencedTypes();
        KotlinContext.takeFileHelpers();
        OutputBuilder types = new OutputBuilderImpl();
        for (TypeInfo typeInfo : compilationUnit.types()) {
            if (typeInfo.typeNature().isPackageInfo()) continue;
            types.add(SpaceEnum.NEWLINE).add(new KotlinTypePrinter(typeInfo, formatter2).print(importData, true))
                    .add(SpaceEnum.NEWLINE);
        }
        // a type Kotlin maps to its own is not imported: `import java.util.List` would shadow kotlin.collections'
        List<String> imports = new ArrayList<>(importData.imports().stream().map(i -> i.importString())
                .filter(i -> !KotlinTypeName.isMapped(i)).map(i -> i.replaceFirst("^static\\s+", "")).toList());
        imports.addAll(missingImports(KotlinContext.takeReferencedTypes(), imports));
        imports.forEach(i -> out.add(KeywordImpl.IMPORT).add(SpaceEnum.ONE).add(new TextImpl(KotlinNames.dotted(i)))
                .add(SpaceEnum.NEWLINE));
        out.add(types);
        KotlinContext.takeFileHelpers().forEach(h -> out.add(SpaceEnum.NEWLINE).add(new TextImpl(h)).add(SpaceEnum.NEWLINE));
        return out;
    }

    /**
     * The types the Kotlin names by their simple name, but Java did not import: a diamond's arguments
     * ({@code bstat.setExprents(new ArrayList<>())} is {@code ArrayList<Exprent?>()}), a verdict's. Not those Kotlin
     * sees anyway (this package, java.lang), and not one whose simple name an import or a declaration already takes.
     */
    private List<String> missingImports(Set<TypeInfo> referenced, List<String> imports) {
        Set<String> taken = new HashSet<>();
        imports.forEach(i -> taken.add(i.substring(i.lastIndexOf('.') + 1)));
        compilationUnit.types().forEach(t -> taken.add(t.simpleName()));
        List<String> missing = new ArrayList<>();
        for (TypeInfo type : referenced) {
            String fqn = type.fullyQualifiedName();
            String packageName = type.packageName();
            if (packageName.equals(compilationUnit.packageName()) || "java.lang".equals(packageName)
                || KotlinTypeName.isMapped(fqn) || imports.contains(fqn) || imports.contains(packageName + ".*")
                || !taken.add(type.simpleName())) {
                continue;
            }
            missing.add(fqn);
        }
        return missing;
    }
}
