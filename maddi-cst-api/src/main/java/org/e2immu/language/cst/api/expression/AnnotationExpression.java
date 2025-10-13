/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
