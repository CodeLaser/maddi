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

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.CompilationUnitPrinter;
import org.e2immu.language.cst.api.info.ImportComputer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypePrinter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.*;

import java.util.List;

public record CompilationUnitPrinterImpl(CompilationUnit compilationUnit, boolean formatter2)
        implements CompilationUnitPrinter {


    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification) {
        return print(importComputer, qualification, TypePrinterImpl::new,
                MethodPrinterImpl::new, FieldPrinterImpl::new, TypePrinterImpl::new);
    }

    @Override
    public OutputBuilder print(ImportComputer importComputer,
                               Qualification qualification,
                               TypePrinterFactory typePrinterFactory,
                               TypePrinter.MethodPrinterFactory methodPrinterFactory,
                               TypePrinter.FieldPrinterFactory fieldPrinterFactory,
                               TypePrinter.EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        /*
        A module-info.java is a compilation unit with a module declaration and NO types, so it has to be answered
        before anything that walks types(): computeImportData does, and on a module-info that SetOnce was never
        set, so the import computation threw "Not yet set" rather than printing nothing.

        Its imports are printed as written rather than recomputed. Imports are legal in a module-info -- they
        shorten the names in `uses` / `provides ... with` -- but the import computer reasons about type references
        in code, and a module directive is not one, so it would conclude they are all dead.
         */
        ModuleInfo moduleInfo = compilationUnit.moduleInfo();
        if (moduleInfo != null) {
            OutputBuilder ob = new OutputBuilderImpl();
            compilationUnit.comments().forEach(c -> ob.add(c.print(qualification)));
            compilationUnit.importStatements().forEach(i -> ob.add(i.print(qualification)));
            ob.add(moduleInfo.print(qualification));
            compilationUnit.trailingComments().forEach(c -> ob.add(c.print(qualification)));
            return ob;
        }

        ImportDataImpl importData = computeImportData(importComputer, qualification);
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        compilationUnit.comments().forEach(c -> outputBuilder.add(c.print(qualification)));

        String packageName = compilationUnit.packageName();
        if (!packageName.isEmpty()) {
            outputBuilder.add(KeywordImpl.PACKAGE).add(SpaceEnum.ONE).add(new TextImpl(packageName))
                    .add(SymbolEnum.SEMICOLON)
                    .add(SpaceEnum.NEWLINE);
        }
        importData.imports().forEach(i -> {
            i.comments().forEach(c -> outputBuilder.add(c.print(qualification)));
            outputBuilder.add(KeywordImpl.IMPORT)
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl(i.importString()))
                    .add(SymbolEnum.SEMICOLON).add(SpaceEnum.NEWLINE);
        });

        for (TypeInfo typeInfo : compilationUnit.types()) {
            // special case: the package-info.java file
            if (typeInfo.typeNature().isPackageInfo()) {
                OutputBuilder annotationStream = typeInfo.annotations().stream()
                        .map(ae -> ae.print(qualification))
                        .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE));
                return annotationStream.add(SpaceEnum.NEWLINE).add(outputBuilder);
            }
            if (qualification.decorator() != null) {
                qualification.decorator().comments(typeInfo)
                        .forEach(c -> outputBuilder.add(c.print(qualification)));
            }

            // the order is important: we first print, and collect extra imports
            TypePrinter typePrinter = typePrinterFactory.create(typeInfo, formatter2);
            OutputBuilder ob = typePrinter.print(importData, true, methodPrinterFactory,
                    fieldPrinterFactory, enclosedTypePrinterFactory);

            // then add the imports
            if (qualification.decorator() != null && typeInfo.isPrimaryType()) {
                qualification.decorator().importStatements().
                        forEach(is -> outputBuilder.add(is.print(qualification)));
            }
            // and the printed matter
            outputBuilder.add(ob);
        }
        compilationUnit.trailingComments().forEach(c -> outputBuilder.add(c.print(qualification)));
        return outputBuilder;
    }

    public record ImportDataImpl(List<ImportComputer.ImportDetails> imports,
                                 Qualification insideType,
                                 Qualification qualification) implements ImportData {
    }

    @Override
    public ImportDataImpl computeImportData(ImportComputer importComputer, Qualification qualification) {
        ImportComputer.Result res = importComputer.go(compilationUnit, qualification);
        Qualification insideType = res.qualification();
        assert insideType != null;

        return new ImportDataImpl(res.imports(), insideType, qualification);
    }
}
