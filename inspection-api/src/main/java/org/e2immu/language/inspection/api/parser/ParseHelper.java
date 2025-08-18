package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;

import java.util.List;

public interface ParseHelper {

    List<AnnotationExpression.KV> parseAnnotationExpression(TypeInfo annotationType, Object annotation, Context context);

    Expression parseExpression(Context context, String index, ForwardType forward, Object expression);

    JavaDoc.Tag parseJavaDocReferenceInTag(Context context, Info info, JavaDoc.Tag tag);

    void resolveMethodInto(MethodInfo.Builder methodInfoBuilder, Context context, ForwardType forwardType,
                           Object eci, Object expression, List<Statement> recordAssignments);
}
