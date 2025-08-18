package org.e2immu.language.cst.api.type;

import org.e2immu.language.cst.api.output.element.Keyword;

public interface TypeNature {
    boolean isClass();

    boolean isEnum();

    boolean isInterface();

    boolean isRecord();

    boolean isStatic(); // is true for all but inner classes in Java

    boolean isAnnotation();

    /*
    A stub type is created during parsing, because both source and byte code are missing.
    The type's inspection data gets filled up as well as possible, so that we don't have to stop
    parsing.
     */
    boolean isStub();

    boolean isPackageInfo();

    Keyword keyword();
}
