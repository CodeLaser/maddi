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

package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.TextBlock;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.element.TextBlockFormatting;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.TextImpl;

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
