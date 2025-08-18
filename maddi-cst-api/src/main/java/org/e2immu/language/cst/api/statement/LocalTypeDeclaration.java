package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.TypeInfo;

public interface LocalTypeDeclaration extends Statement {

    TypeInfo typeInfo();

    LocalTypeDeclaration withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setTypeInfo(TypeInfo typeInfo);

        LocalTypeDeclaration build();
    }

    String NAME = "localTypeDeclaration";

    @Override
    default String name() {
        return NAME;
    }
}
