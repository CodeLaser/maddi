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
import io.codelaser.maddi.cst.api.expression.TextBlock;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.output.element.TextBlockFormatting;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;

public class TextBlockImpl extends StringConstantImpl implements TextBlock {
    private final TextBlockFormatting textBlockFormatting;

    public TextBlockImpl(List<Comment> comments,
                         Source source,
                         ParameterizedType stringPt,
                         String constant,
                         TextBlockFormatting textBlockFormatting) {
        super(comments, source, stringPt, constant);
        this.textBlockFormatting = textBlockFormatting;
    }

    @Override
    public TextBlockFormatting textBlockFormatting() {
        return textBlockFormatting;
    }

    @Override
    public Expression withSource(Source source) {
        return new TextBlockImpl(comments(), source, parameterizedType(), constant(), textBlockFormatting);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextBlock that)) return false;
        return constant().equals(that.constant());
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return new OutputBuilderImpl().add(new TextImpl(constant(), textBlockFormatting));
    }

    @Override
    public String toString() {
        return "textBlock@" + source().compact2();
    }
}
