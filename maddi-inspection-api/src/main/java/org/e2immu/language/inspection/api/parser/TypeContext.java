package org.e2immu.language.inspection.api.parser;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;

/*
NOT to be used by byte code inspection: exclusive to parser system!
 */
public interface TypeContext {
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

    List<TypeInfo> typesInSamePackage(String packageName);
}
