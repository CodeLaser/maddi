package org.e2immu.parser.java;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.LocalTypeDeclaration;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.TypeContext;
import org.parsers.java.ast.TypeDeclaration;

public class ParseLocalTypeDeclaration extends CommonParse {
    public ParseLocalTypeDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public LocalTypeDeclaration parse(Context context, String index, TypeDeclaration classDeclaration) {
        assert context.enclosingMethod() != null;
        Context newContext = context.newLocalTypeDeclaration();

        TypeInfo typeInfo = parsers.parseTypeDeclaration().parseLocal(newContext, context.enclosingMethod(), classDeclaration);
        newContext.resolver().resolve(false);
        context.typeContext().addToContext(typeInfo, TypeContext.CURRENT_TYPE_PRIORITY);
        return runtime.newLocalTypeDeclarationBuilder()
                .setTypeInfo(typeInfo)
                .setSource(source(index, classDeclaration))
                .build();
    }
}
