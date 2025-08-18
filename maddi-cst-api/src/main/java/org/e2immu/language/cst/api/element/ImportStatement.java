package org.e2immu.language.cst.api.element;

import org.e2immu.annotation.Fluent;

public interface ImportStatement extends Element {

    String importString();

    boolean isStatic();

    default boolean isStar() {
        return importString().endsWith(".*");
    }

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder setImport(String importString);

        @Fluent
        Builder setIsStatic(boolean isStatic);

        ImportStatement build();
    }
}
