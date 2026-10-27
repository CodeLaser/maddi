/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.info;

import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;

import java.util.List;

public interface TypePrinter {

    List<TypeModifier> minimalModifiers(TypeInfo typeInfo);

    OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration);

    OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration);

    OutputBuilder print(CompilationUnitPrinter.ImportData importData,
                        boolean doTypeDeclaration,
                        MethodPrinterFactory methodPrinterFactory,
                        FieldPrinterFactory fieldPrinterFactory,
                        EnclosedTypePrinterFactory enclosedTypePrinterFactory);

    interface MethodPrinterFactory {
        MethodPrinter create(TypeInfo typeInfo, MethodInfo methodInfo, boolean formatter2);
    }

    interface FieldPrinterFactory {
        FieldPrinter create(FieldInfo fieldInfo, boolean formatter2);
    }

    interface EnclosedTypePrinterFactory {
        TypePrinter create(TypeInfo typeInfo, boolean formatter2);
    }
}
