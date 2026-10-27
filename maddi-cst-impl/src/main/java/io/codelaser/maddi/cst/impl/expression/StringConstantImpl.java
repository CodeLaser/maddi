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
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.runtime.Predefined;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.util.StringUtil;

import java.util.List;
import java.util.Objects;

public class StringConstantImpl extends ConstantExpressionImpl<String> implements StringConstant {
    private final ParameterizedType stringPt;
    private final String constant;

    public StringConstantImpl(Predefined predefined, String constant) {
        this(List.of(), null, predefined.stringParameterizedType(), constant);
    }

    public StringConstantImpl(List<Comment> comments, Source source, ParameterizedType stringPt, String constant) {
        super(comments, source, constant.isEmpty() ? 1 : 2);
        this.stringPt = Objects.requireNonNull(stringPt);
        this.constant = Objects.requireNonNull(constant);
    }

    @Override
    public Expression withSource(Source source) {
        return new StringConstantImpl(comments(), source, stringPt, constant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringConstantImpl that)) return false;
        return constant.equals(that.constant);
    }

    @Override
    public int hashCode() {
        return constant.hashCode();
    }

    @Override
    public String constant() {
        return constant;
    }

    @Override
    public ParameterizedType parameterizedType() {
        return stringPt;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_CONSTANT_STRING;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        StringConstant sc = (StringConstant) expression;
        return constant.compareTo(sc.constant());
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        String quoted = StringUtil.quote(constant);
        return new OutputBuilderImpl().add(new TextImpl(quoted));
    }

    @Override
    public String toString() {
        return "stringConstant@" + source().compact2();
    }
}
