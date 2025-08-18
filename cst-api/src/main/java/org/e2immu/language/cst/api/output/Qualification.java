package org.e2immu.language.cst.api.output;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;

public interface Qualification {
    boolean doNotQualifyImplicit();

    boolean isFullyQualifiedNames();

    boolean isSimpleOnly();

    TypeNameRequired qualifierRequired(TypeInfo typeInfo);

    boolean qualifierRequired(MethodInfo methodInfo);

    boolean qualifierRequired(Variable variable);

    TypeNameRequired typeNameRequired();

    interface Decorator {
        List<Comment> comments(Element element);

        List<AnnotationExpression> annotations(Element element);

        List<ImportStatement> importStatements();
    }

    Decorator decorator();
}
