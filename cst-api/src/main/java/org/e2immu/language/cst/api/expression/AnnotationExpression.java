package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;

public interface AnnotationExpression extends Expression {

    interface KV {
        String key();

        /* in Java, that would be "value" */
        boolean keyIsDefault();

        KV rewire(InfoMap infoMap);

        KV translate(TranslationMap translationMap);

        Expression value();
    }

    TypeInfo typeInfo();

    List<KV> keyValuePairs();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder addKeyValuePair(String key, Expression value);

        @Fluent
        Builder setKeyValuesPairs(List<KV> kvs);

        @Fluent
        Builder setTypeInfo(TypeInfo typeInfo);

        AnnotationExpression build();
    }

    int[] extractIntArray(String key);

    String[] extractStringArray(String key);

    boolean extractBoolean(String key);

    String extractString(String key, String defaultValue);

    TypeInfo extractTypeInfo(String type);

    List<Float> extractFloatArray(String key);

    AnnotationExpression withKeyValuePair(String key, Expression value);
}
