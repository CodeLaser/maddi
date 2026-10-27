/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.expression;

import io.codelaser.maddi.annotation.Independent;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.translate.TranslationMap;

import java.util.List;

/**
 * An annotation use, {@code @SomeAnnotation(key = value, ...)}. Modelled as an expression so it can be
 * attached to any {@link Element}. The annotation type is {@link #typeInfo()} and its arguments are the
 * {@link #keyValuePairs()}; the {@code extractXxx} helpers read individual attribute values.
 */
public interface AnnotationExpression extends Expression {

    /**
     * One {@code key = value} attribute of an annotation.
     */
    interface KV {
        /**
         * @return the attribute name.
         */
        String key();

        /**
         * @return {@code true} when this is the default attribute (in Java, the one named {@code value}),
         * which may be written without {@code key =}.
         */
        boolean keyIsDefault();

        KV rewire(@Independent(hc = true) InfoMapView infoMap);

        KV translate(TranslationMap translationMap);

        /**
         * @return the attribute value expression.
         */
        Expression value();
    }

    /**
     * @return the annotation type.
     */
    TypeInfo typeInfo();

    /**
     * @return the {@code key = value} attributes, in order.
     */
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
