package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;

import java.util.List;

public interface TypePrinter {

    List<TypeModifier> minimalModifiers(TypeInfo typeInfo);

    OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration);
}
