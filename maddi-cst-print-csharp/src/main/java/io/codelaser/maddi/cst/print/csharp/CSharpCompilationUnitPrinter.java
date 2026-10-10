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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Prints a {@link CompilationUnit} as a C# file: the {@code using} directives, a file-scoped {@code namespace}, and
 * the types. Java imports types, C# imports namespaces; the directives follow from the types the printed code names by
 * their simple names:
 * <ul>
 *   <li>a type of another namespace, {@code a.b.C}, needs {@code using A.B;};</li>
 *   <li>a nested type of another file, {@code a.b.C.D}, and a static import, {@code import static a.b.C.m}, need
 *   {@code using static A.B.C;}, which brings C's static members and nested types into scope;</li>
 *   <li>the BCL namespaces the printed code needs ({@code System.Diagnostics} for {@code Debug.Assert}, …) come
 *   along.</li>
 * </ul>
 * A library package keeps its Java name until the BCL mapping takes it over. Comments are not printed: the CST's
 * comments are Java's.
 */
public record CSharpCompilationUnitPrinter(CompilationUnit compilationUnit, boolean formatter2) {

    /** The C# text, and the messages about what it does differently from the Java, or could not translate. */
    public record Result(OutputBuilder output, List<CSharpPrintMessage> messages) {
    }

    public OutputBuilder print(ImportComputer importComputer, Qualification qualification) {
        return printWithMessages(importComputer, qualification).output();
    }

    /** As {@link #print}, with the {@link CSharpPrintMessage}s of this file, in the order they were printed. */
    public Result printWithMessages(ImportComputer importComputer, Qualification qualification) {
        CSharpContext.reset();
        try {
            CSharpContext.privateReachedFromOutside(CSharpAccess.reachedFromOutside(compilationUnit));
            OutputBuilder output = printFile(importComputer, qualification);
            return new Result(output, CSharpContext.messages());
        } finally {
            CSharpContext.reset();
        }
    }

    private OutputBuilder printFile(ImportComputer importComputer, Qualification qualification) {
        CompilationUnitPrinterImpl.ImportDataImpl importData = new CompilationUnitPrinterImpl(compilationUnit, formatter2)
                .computeImportData(importComputer, qualification);
        OutputBuilder types = new OutputBuilderImpl();
        for (TypeInfo typeInfo : compilationUnit.types()) {
            if (typeInfo.typeNature().isPackageInfo()) continue;
            OutputBuilder type = new CSharpTypePrinter(typeInfo, formatter2).print(importData, true);
            if (!type.isEmpty()) types.add(SpaceEnum.NEWLINE).add(type).add(SpaceEnum.NEWLINE);
            for (TypeInfo h : hoisted(typeInfo, new ArrayList<>())) {
                OutputBuilder hoisted = new CSharpTypePrinter(h, formatter2).print(importData, true);
                if (!hoisted.isEmpty()) types.add(SpaceEnum.NEWLINE).add(hoisted).add(SpaceEnum.NEWLINE);
            }
        }

        String namespace = CSharpNames.namespace(compilationUnit.packageName(), !compilationUnit.externalLibrary());
        Set<String> usings = new TreeSet<>(CSharpContext.usings());
        Set<String> usingStatics = new TreeSet<>();
        for (TypeInfo referenced : CSharpContext.referenced()) {
            TypeInfo primary = referenced.primaryType();
            if (primary == null || compilationUnit.types().contains(primary)) continue;
            if (referenced.isPrimaryType() || CSharpNames.hoisted(referenced)) {
                usings.add(CSharpNames.namespace(referenced));
            } else {
                // a nested type by its simple name: its enclosing type's members are in scope
                TypeInfo enclosing = referenced.compilationUnitOrEnclosingType().getRight();
                String ns = CSharpNames.namespace(primary);
                usingStatics.add((ns.isEmpty() ? "" : ns + ".") + CSharpTypeName.fromPrimaryType(enclosing));
            }
        }
        for (ImportComputer.ImportDetails i : importData.imports()) {
            if (i.importString().startsWith("static ")) usingStatics.add(staticImportOwner(i.importString()));
        }
        usings.remove(namespace);
        usings.remove("");

        OutputBuilder out = new OutputBuilderImpl();
        usings.forEach(u -> out.add(CSharpKeyword.USING).add(SpaceEnum.ONE).add(new TextImpl(u))
                .add(SymbolEnum.SEMICOLON).add(SpaceEnum.NEWLINE));
        usingStatics.forEach(u -> out.add(CSharpKeyword.USING).add(SpaceEnum.ONE).add(KeywordImpl.STATIC)
                .add(SpaceEnum.ONE).add(new TextImpl(u)).add(SymbolEnum.SEMICOLON).add(SpaceEnum.NEWLINE));
        if (!namespace.isEmpty()) {
            if (!usings.isEmpty() || !usingStatics.isEmpty()) out.add(SpaceEnum.NEWLINE);
            out.add(CSharpKeyword.NAMESPACE).add(SpaceEnum.ONE).add(new TextImpl(namespace)).add(SymbolEnum.SEMICOLON)
                    .add(SpaceEnum.NEWLINE);
        }
        return out.add(types);
    }

    /** The nested types of {@code typeInfo}, at any depth, that are printed in the namespace. */
    private static List<TypeInfo> hoisted(TypeInfo typeInfo, List<TypeInfo> found) {
        for (TypeInfo sub : typeInfo.subTypes()) {
            if (sub.isSynthetic()) continue;
            if (CSharpNames.hoisted(sub)) found.add(sub);
            hoisted(sub, found);
        }
        return found;
    }

    /**
     * The type of a static import, {@code A.B.C} for {@code static a.b.C.m} and {@code static a.b.C.*}. The package
     * ends at the first segment that starts with a capital, Java's convention for a type.
     */
    private static String staticImportOwner(String importString) {
        String[] segments = importString.replaceFirst("^static\\s+", "").split("\\.");
        int firstType = 0;
        while (firstType < segments.length - 1 && !Character.isUpperCase(segments[firstType].charAt(0))) firstType++;
        String packageName = String.join(".", Arrays.copyOfRange(segments, 0, firstType));
        boolean library = packageName.startsWith("java.") || packageName.startsWith("javax.");
        String namespace = CSharpNames.namespace(packageName, !library);
        String type = String.join(".", Arrays.copyOfRange(segments, firstType, segments.length - 1));
        return namespace.isEmpty() ? type : namespace + "." + type;
    }
}
