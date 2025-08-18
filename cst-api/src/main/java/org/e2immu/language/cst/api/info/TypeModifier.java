package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.element.Keyword;

public interface TypeModifier {
    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    boolean isAbstract();

    boolean isFinal();

    boolean isStatic();

    boolean isSealed();

    boolean isNonSealed();

    Keyword keyword();

    default boolean isAccessModifier() {
        return isPrivate() || isProtected() || isPublic();
    }
}
