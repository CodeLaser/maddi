package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.Lombok;
import org.e2immu.util.internal.util.GetSetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LombokImpl(Runtime runtime) implements Lombok {
    private static final String LOMBOK_DOT = "lombok.";
    private static final Logger LOGGER = LoggerFactory.getLogger(LombokImpl.class);

    @Override
    public void handleField(FieldInfo fieldInfo) {

        AnnotationExpression getter0 = fieldInfo.builder().haveAnnotation(LOMBOK_DOT + "Getter");
        if (getter0 != null) {
            addGetter(fieldInfo);
        } else if (!fieldInfo.isStatic()) {
            AnnotationExpression getter1 = fieldInfo.owner()
                    .builder().haveAnnotation(LOMBOK_DOT + "Getter");
            if (getter1 != null) {
                addGetter(fieldInfo);
            }
        }
    }

    private void addGetter(FieldInfo fieldInfo) {
        String getterName = GetSetHelper.getterName(fieldInfo.name(), fieldInfo.type().isBoolean());
        TypeInfo owner = fieldInfo.owner();
        LOGGER.info("Handle {}", fieldInfo);
        if (owner.methodStream().noneMatch(mi -> getterName.equals(mi.name()) && mi.parameters().isEmpty())) {
            Source source = runtime.noSource();
            FieldReference fr = runtime.newFieldReference(fieldInfo);
            VariableExpression ve = runtime.newVariableExpressionBuilder().setSource(source)
                    .setVariable(fr).build();
            Statement returnStatement = runtime.newReturnBuilder().setSource(source).setExpression(ve).build();
            Block body = runtime.newBlockBuilder().addStatement(returnStatement).setSource(source).build();
            MethodInfo method = runtime.newMethod(owner, getterName, runtime.methodTypeMethod());
            method.builder()
                    .setMethodBody(body)
                    .setReturnType(fieldInfo.type())
                    .setSynthetic(true)
                    .setSource(source)
                    .commitParameters().commit();
            owner.builder().addMethod(method);
        }
    }
}
