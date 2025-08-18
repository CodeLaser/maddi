package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.element.Keyword;

public interface FieldModifier {
    default boolean isAccessModifier() {
        return isPublic() || isPrivate() || isProtected();
    }

    boolean isStatic();

    boolean isFinal();

    boolean isVolatile();

    boolean isTransient();

    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    Keyword keyword();
}
