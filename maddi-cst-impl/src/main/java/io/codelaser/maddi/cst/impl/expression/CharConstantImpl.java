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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.CharConstant;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Predefined;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;

public class CharConstantImpl extends ConstantExpressionImpl<Character> implements CharConstant {

    private final char constant;
    private final ParameterizedType parameterizedType;
    // how the literal was written, when that is not how its value prints (#49); null otherwise
    private final String sourceSpelling;

    public CharConstantImpl(Predefined predefined, char constant) {
        this(List.of(), null, predefined.charParameterizedType(), constant);
    }

    public CharConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, char constant) {
        this(comments, source, parameterizedType, constant, null);
    }

    public CharConstantImpl(List<Comment> comments, Source source, ParameterizedType parameterizedType, char constant,
                            String sourceSpelling) {
        super(comments, source, '\0' == constant ? 1 : 2);
        this.parameterizedType = parameterizedType;
        this.constant = constant;
        this.sourceSpelling = sourceSpelling;
    }

    @Override
    public Expression withSource(Source source) {
        return new CharConstantImpl(comments(), source, parameterizedType, constant, sourceSpelling);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharConstantImpl that = (CharConstantImpl) o;
        return constant == that.constant;
    }

    @Override
    public int hashCode() {
        return constant;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String text = sourceSpelling != null ? sourceSpelling : "'" + escaped(constant) + "'";
        return new OutputBuilderImpl().add(new TextImpl(text));
    }

    @Override
    public String sourceSpelling() {
        return sourceSpelling;
    }

    @Override
    public CharConstantImpl withSourceSpelling(String sourceSpelling) {
        return new CharConstantImpl(comments(), source(), parameterizedType, constant, sourceSpelling);
    }

    public static String escaped(char constant) {
        return switch (constant) {
            case '\0' -> "\\0";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\f' -> "\\f";
            case '\'' -> "\\'";
            case '\"' -> "\\\"";
            case '\\' -> "\\\\";
            default -> {
                if (constant >= 32 && constant <= 127) yield Character.toString(constant);
                String hex = Integer.toString(constant, 16);
                yield "\\u" + "0".repeat(Math.max(0, 4 - hex.length())) + hex;
            }
        };
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_CHAR;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        return constant - ((CharConstant) expression).constant();
    }

    @Override
    public Character constant() {
        return constant;
    }
}
