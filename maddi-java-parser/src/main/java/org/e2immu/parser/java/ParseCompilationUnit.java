package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.e2immu.support.Either;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/*
Second round! ScanCompilationUnit has already run.
We parse the import statements, add them to the compilation unit object, and then continue with the
actual parsing of types.
 */
public class ParseCompilationUnit extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseCompilationUnit.class);

    private final Context rootContext;

    public ParseCompilationUnit(Context rootContext) {
        super(rootContext.runtime(), new Parsers(rootContext.runtime()));
        this.rootContext = rootContext;
    }

    public List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>>
    parse(org.e2immu.language.cst.api.element.CompilationUnit compilationUnit, CompilationUnit cu) {
        assert compilationUnit.packageName() != null;
        assert compilationUnit.uri() != null;
        try {
            return internalParse(compilationUnit, cu);
        } catch (Summary.FailFastException ffe) {
            throw ffe;
        } catch (RuntimeException re) {
            //re.printStackTrace(System.err);
            LOGGER.error("Caught exception parsing compilation unit {}", compilationUnit.uri());
            rootContext.summary().addParseException(new Summary.ParseException(compilationUnit, compilationUnit,
                    re.getMessage(), re));
            return List.of();
        }
    }

    private List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>>
    internalParse(org.e2immu.language.cst.api.element.CompilationUnit compilationUnit, CompilationUnit cu) {
        assert compilationUnit.packageName() != null;

        Context newContext = rootContext.newCompilationUnit(compilationUnit);
        TypeContext typeContext = newContext.typeContext();
        AtomicBoolean mustDelayForStaticImportTypeHierarchy = new AtomicBoolean();
        handleImportStatements(compilationUnit, typeContext, mustDelayForStaticImportTypeHierarchy, false);

        // then, with lower priority, add type names from the same package
        rootContext.typeContext().typesInSamePackage(compilationUnit.packageName())
                .forEach(ti -> typeContext.addToContext(ti, TypeContext.SAME_PACKAGE_PRIORITY));

        // finally, add * imports
        handleImportStatements(compilationUnit, typeContext, mustDelayForStaticImportTypeHierarchy, true);

        List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>> types = new ArrayList<>();
        String uriString = compilationUnit.uri().toString();
        if (uriString.endsWith("package-info.java")) {
            TypeInfo typeInfo = buildPackageInfoType(compilationUnit, cu, newContext);
            types.add(Either.left(typeInfo));
            LOGGER.debug("Added {}, test? {}", typeInfo, typeInfo.compilationUnit().sourceSet().test());
        } else if (!uriString.endsWith("module-info.java")) {
            for (TypeDeclaration td : cu.childrenOfType(TypeDeclaration.class)) {
                if (td instanceof EmptyDeclaration) {
                    LOGGER.debug("Skipping empty declaration in {}", compilationUnit.uri());
                } else {
                    Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation> either
                            = parsers.parseTypeDeclaration().parse(newContext, Either.left(compilationUnit), td,
                            mustDelayForStaticImportTypeHierarchy.get());
                    if (either != null) {
                        types.add(either);
                    } // else: error...
                }
            }
        } // else: has been parsed elsewhere
        return types;
    }

    private static void handleImportStatements(org.e2immu.language.cst.api.element.CompilationUnit compilationUnit,
                                               TypeContext typeContext,
                                               AtomicBoolean mustDelayForStaticImportTypeHierarchy,
                                               boolean isStar) {
        compilationUnit.importStatements()
                .stream()
                .filter(is -> isStar == is.isStar())
                .forEach(is -> {
                    if (is.isStatic()) {
                        if (!typeContext.addToStaticImportMap(compilationUnit, is)) {
                            mustDelayForStaticImportTypeHierarchy.set(true);
                        }
                    } else {
                        typeContext.addNonStaticImportToContext(is);
                    }
                });
    }

    private TypeInfo buildPackageInfoType(org.e2immu.language.cst.api.element.CompilationUnit compilationUnit,
                                          CompilationUnit cu,
                                          Context context) {
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        List<Annotation> annotations = packageDeclaration.childrenOfType(Annotation.class);
        String suffix = compilationUnit.sourceSet().test() ? "-test" : "";
        TypeInfo typeInfo = runtime.newTypeInfo(compilationUnit, "package-info" + suffix);
        TypeInfo.Builder builder = typeInfo.builder();
        parseAnnotations(context, builder, annotations);
        builder.setTypeNature(runtime.typeNaturePackageInfo())
                .setParentClass(runtime.objectParameterizedType())
                .setAccess(runtime.accessPublic())
                .setSource(source(cu))
                .commit();
        return typeInfo;
    }

    public Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation> parseDelayedType(ParseTypeDeclaration.DelayedParsingInformation d) {
        return parsers.parseTypeDeclaration().continueParsingTypeDeclaration(d);
    }
}
