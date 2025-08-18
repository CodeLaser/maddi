package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;

import java.util.List;

public interface Resolver {
    void add(Info info,
             Info.Builder<?> infoBuilder,
             ForwardType forwardType,
             Object explicitConstructorInvocation,
             Object toResolve,
             Context newContext,
             List<Statement> recordAssignments);

    void addRecordField(FieldInfo recordField);

    void add(TypeInfo.Builder builder);

    void addAnnotationTodo(Info.Builder<?> infoBuilder,
                           TypeInfo annotationType,
                           AnnotationExpression.Builder ab,
                           int indexInAnnotationList,
                           Object annotation,
                           Context context);

    // add to the to-do list, but only for overrides
    void addRecordAccessor(MethodInfo accessor);

    void addJavadoc(Info info, Info.Builder<?> infoBuilder, Context context, JavaDoc javaDoc);

    Resolver newEmpty();

    void resolve(boolean primary);

    ParseHelper parseHelper();
}
