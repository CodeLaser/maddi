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

/**
 * Prints a {@link CompilationUnit} as a Kotlin file: the {@code package} line, the imports the
 * {@link ImportComputer} finds the printed code needs, and the types. A Java static import is an ordinary Kotlin
 * import ({@code import a.B.c}); names that are Kotlin keywords are escaped segment by segment. Comments are not
 * printed: the CST's comments are Java's, the licence header first among them.
 */
public record KotlinCompilationUnitPrinter(CompilationUnit compilationUnit, boolean formatter2) {

    public OutputBuilder print(ImportComputer importComputer, Qualification qualification) {
        CompilationUnitPrinterImpl.ImportDataImpl importData = new CompilationUnitPrinterImpl(compilationUnit, formatter2)
                .computeImportData(importComputer, qualification);
        KotlinContext.resetLabels();
        OutputBuilder out = new OutputBuilderImpl();
        String packageName = compilationUnit.packageName();
        if (!packageName.isEmpty()) {
            out.add(KeywordImpl.PACKAGE).add(SpaceEnum.ONE).add(new TextImpl(KotlinNames.dotted(packageName)))
                    .add(SpaceEnum.NEWLINE);
        }
        importData.imports().forEach(i -> out.add(KeywordImpl.IMPORT).add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinNames.dotted(i.importString().replaceFirst("^static\\s+", ""))))
                .add(SpaceEnum.NEWLINE));
        for (TypeInfo typeInfo : compilationUnit.types()) {
            if (typeInfo.typeNature().isPackageInfo()) continue;
            out.add(SpaceEnum.NEWLINE).add(new KotlinTypePrinter(typeInfo, formatter2).print(importData, true))
                    .add(SpaceEnum.NEWLINE);
        }
        return out;
    }
}
