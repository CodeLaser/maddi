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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.List;

public abstract class ExpressionImpl extends ElementImpl implements Expression {
    private final int complexity;
    private final Source source;
    private final List<Comment> comments;

    protected ExpressionImpl(int complexity) {
        this(null, null, complexity);
    }

    protected ExpressionImpl(List<Comment> comments, Source source, int complexity) {
        this.complexity = complexity;
        this.source = source;
        this.comments = comments == null ? List.of() : List.copyOf(comments);
    }

    @Override
    public int complexity() {
        return complexity;
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public List<Comment> comments() {
        return comments;
    }

    @NotNull
    protected OutputBuilder outputInParenthesis(Qualification qualification, Precedence precedence, Expression expression) {
        if (precedence.greaterThan(expression.precedence())) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(expression.print(qualification)).add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return expression.print(qualification);
    }

    @Override
    public int compareTo(Expression v) {
        if (this == v || equals(v)) return 0;
        return ExpressionComparator.SINGLETON.compare(this, v);
    }
}
