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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.ImportStatement;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.List;

/*
NOT to be used by byte code inspection: exclusive to parser system!
 */
public interface TypeContext {
    int METHOD_TYPE_PARAMETER_PRIORITY = 21; // see TestConstructor2,2
    int CURRENT_TYPE_PRIORITY = 20;
    int TYPE_PARAMETER_PRIORITY = 19;
    int SUBTYPE_PRIORITY = 17;
    int IMPORT_PRIORITY = 10;
    int STATIC_IMPORT_PRIORITY = 9; // on-demand
    int SAME_PACKAGE_PRIORITY = 6;
    int IMPORT_ASTERISK_PACKAGE_PRIORITY = 5;
    int IMPORT_ASTERISK_SUBTYPE_PRIORITY = 5;
    int SUBTYPE_HIERARCHY_PRIORITY = 4;
    int SUBTYPE_HIERARCHY_IN_CONSTRUCTOR_PRIORITY = 4;
    int SUBTYPE_HIERARCHY_ANONYMOUS = 3;
    int IMPORT_ENCLOSING_PRIORITY = 2;

    /*
    return true when all types in the hierarchy have been resolved
     */
    boolean addSubTypesOfHierarchyReturnAllDefined(TypeInfo typeInfo, int priority);

    /*
    return true when all types in the hierarchy of a static * import have been resolved,
    or this was not a * import.
     */
    boolean addToStaticImportMap(CompilationUnit currentCompilationUnit, ImportStatement importStatement);

    FieldInfo findFieldRecursively(TypeInfo typeInfo, String name);

    void addNonStaticImportToContext(ImportStatement importStatement);

    Variable findStaticFieldImport(String name);

    TypeContext newCompilationUnit(CompilationUnit compilationUnit);

    TypeContext newAnonymousClassBody(TypeInfo baseType);

    StaticImportMap importMap();

    CompilationUnit compilationUnit();

    // a.b.X.I.J -> X, I, J
    // X.I.J -> X, I, J
    // J -> J
    // I.J -> I, J,
    List<? extends NamedType> getWithQualification(String name, boolean complain);

    void addToContext(@NotNull NamedType namedType, int priority);

    TypeContext newTypeContext();

    List<TypeInfo> typesInSamePackage(String packageName, SourceSet sourceSetOfRequest);
}
