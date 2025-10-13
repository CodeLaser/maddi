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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.support.Either;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*
First round, we only prepare a type map.
 */
public class ScanCompilationUnit extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanCompilationUnit.class);

    private final Summary summary;

    public ScanCompilationUnit(Summary summary, Runtime runtime) {
        super(runtime, new Parsers(runtime));
        this.summary = summary;
    }

    public record ScanResult(Map<String, TypeInfo> sourceTypes,
                             org.e2immu.language.cst.api.element.CompilationUnit compilationUnit) {
    }

    public ScanResult scan(URI uri,
                           SourceSet sourceSet,
                           FingerPrint fingerPrint,
                           CompilationUnit cu,
                           boolean addDetailedSources) {
        try {
            return internalScan(uri, sourceSet, fingerPrint, cu, addDetailedSources);
        } catch (Summary.FailFastException ffe) {
            throw ffe;
        } catch (RuntimeException re) {
            //re.printStackTrace(System.err);
            LOGGER.error("Caught exception scanning compilation unit {}", uri);
            summary.addParseException(new Summary.ParseException(uri, cu, "Caught exception scanning compilation unit", re));
            return new ScanResult(Map.of(), null);
        }
    }

    private ScanResult internalScan(URI uri,
                                    SourceSet sourceSet,
                                    FingerPrint fingerPrint,
                                    CompilationUnit cu,
                                    boolean addDetailedSources) {
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        String packageName;
        Source s1 = source(cu);
        Source source;
        List<org.e2immu.language.cst.api.element.Comment> comments;
        if (packageDeclaration == null) {
            source = s1;
            packageName = "";
            comments = List.of();
        } else {
            Name name = packageDeclaration.firstChildOfType(Name.class);
            packageName = Objects.requireNonNullElse(name.toString(), "");
            source = addDetailedSources
                    ? s1.withDetailedSources(runtime.newDetailedSourcesBuilder().put(packageName, source(name)).build())
                    : s1;
            comments = comments(packageDeclaration);
        }
        org.e2immu.language.cst.api.element.CompilationUnit.Builder compilationUnitBuilder
                = runtime.newCompilationUnitBuilder()
                .addComments(comments)
                .setSource(source)
                .setURI(uri)
                .setPackageName(packageName)
                .setSourceSet(sourceSet)
                .setFingerPrint(fingerPrint);
        for (ImportDeclaration id : cu.childrenOfType(ImportDeclaration.class)) {
            ImportStatement importStatement = parseImportDeclaration(id);
            compilationUnitBuilder.addImportStatement(importStatement);
        }
        org.e2immu.language.cst.api.element.CompilationUnit compilationUnit = compilationUnitBuilder.build();
        Map<String, TypeInfo> sourceTypes = recursivelyFindTypes(Either.left(compilationUnit), null, cu,
                addDetailedSources);
        return new ScanResult(sourceTypes, compilationUnit);
    }

    private Map<String, TypeInfo> recursivelyFindTypes(Either<org.e2immu.language.cst.api.element.CompilationUnit, TypeInfo> parent,
                                                       TypeInfo typeInfoOrNull,
                                                       Node body,
                                                       boolean addDetailedSources) {
        Map<String, TypeInfo> map = new HashMap<>();
        for (Node node : body) {
            if (node instanceof TypeDeclaration td && !(node instanceof EmptyDeclaration)) {
                handleTypeDeclaration(parent, typeInfoOrNull, td, map, addDetailedSources);
            }
        }
        return Map.copyOf(map);
    }

    /*
    We extract type nature and type modifiers here, because we'll need them in sibling subtype declarations.
     */
    private void handleTypeDeclaration(Either<org.e2immu.language.cst.api.element.CompilationUnit, TypeInfo> enclosing,
                                       TypeInfo typeInfoOrNull,
                                       TypeDeclaration td,
                                       Map<String, TypeInfo> map,
                                       boolean addDetailedSources) {
        Identifier identifier = td.firstChildOfType(Identifier.class);
        String typeName = identifier.getSource();
        TypeInfo typeInfo;
        if (enclosing.isLeft()) {
            if (typeInfoOrNull != null) {
                typeInfo = typeInfoOrNull;
                assert typeInfo.simpleName().equals(typeName);
            } else {
                typeInfo = runtime.newTypeInfo(enclosing.getLeft(), typeName);
            }
        } else {
            TypeInfo enclosingType = enclosing.getRight();
            typeInfo = runtime.newTypeInfo(enclosingType, typeName);
            enclosingType.builder().addSubType(typeInfo);
        }
        Node sub = handleTypeModifiers(td, typeInfo, addDetailedSources);
        map.put(typeInfo.fullyQualifiedName(), typeInfo);
        if (sub != null) {
            map.putAll(recursivelyFindTypes(Either.right(typeInfo), typeInfoOrNull, sub, addDetailedSources));
        }

        Node next = identifier.nextSibling();
        if (next instanceof TypeParameters typeParameters) {
            int j = 1;
            int typeParameterIndex = 0;
            while (j < typeParameters.size()) {
                Node tp = typeParameters.get(j);
                TypeParameter typeParameter = parseTypeParameterDoNotInspect(tp, typeInfo, typeParameterIndex);
                typeInfo.builder().addOrSetTypeParameter(typeParameter);
                j += 2; // skip the ',' or '>' delimiter
                ++typeParameterIndex;
            }
        }
    }

    private ImportStatement parseImportDeclaration(ImportDeclaration id) {
        boolean isStatic = id.get(1) instanceof KeyWord kw && Token.TokenType.STATIC.equals(kw.getType());
        int i = isStatic ? 2 : 1;
        String importString = id.get(i).getSource();
        ImportStatement.Builder builder = runtime.newImportStatementBuilder();
        builder.setSource(source(id))
                .addComments(comments(id))
                .setIsStatic(isStatic);

        if (id.get(i + 1) instanceof Delimiter d && Token.TokenType.DOT.equals(d.getType())
            && id.get(i + 2) instanceof Operator o && Token.TokenType.STAR.equals(o.getType())) {
            return builder.setImport(importString + ".*").build();
        }
        return builder.setImport(importString).build();
    }
}
