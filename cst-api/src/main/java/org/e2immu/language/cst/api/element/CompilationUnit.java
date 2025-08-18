package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.Fluent;

import java.net.URI;
import java.util.List;

public interface CompilationUnit extends Element {

    URI uri();

    String packageName();

    List<ImportStatement> importStatements();

    SourceSet sourceSet();

    FingerPrint fingerPrintOrNull();

    /**
     * Can be set only once! If set during building phase, this method may not be called.
     *
     * @param fingerPrint the fingerprint to be set
     */
    void setFingerPrint(FingerPrint fingerPrint);

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder addImportStatement(ImportStatement importStatement);

        @Fluent
        Builder setURI(URI uri);

        // to avoid having to catch exceptions in PredefinedImpl
        @Fluent
        Builder setURIString(String s);

        @Fluent
        Builder setPackageName(String packageName);

        @Fluent
        Builder setSourceSet(SourceSet sourceSet);

        @Fluent
        Builder setFingerPrint(FingerPrint fingerPrint);

        CompilationUnit build();
    }

    // helper method here, set==null for primitives
    default boolean partOfJdk() {
        SourceSet set = sourceSet();
        return set == null || set.partOfJdk();
    }

    // helper method here, set==null for primitives;  partOfJdk() implies externalLibrary()
    default boolean externalLibrary() {
        SourceSet set = sourceSet();
        return set == null || set.externalLibrary();
    }
}
