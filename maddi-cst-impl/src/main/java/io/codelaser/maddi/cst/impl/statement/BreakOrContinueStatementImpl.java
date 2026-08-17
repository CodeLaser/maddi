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

package io.codelaser.maddi.cst.impl.statement;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.BreakOrContinueStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class BreakOrContinueStatementImpl extends StatementImpl implements BreakOrContinueStatement {
    private final String goToLabel;

    protected BreakOrContinueStatementImpl(List<Comment> comments, Source source,
                                           List<AnnotationExpression> annotationExpressions,
                                       String label, String goToLabel) {
        super(comments, source, annotationExpressions, 1, label);
        this.goToLabel = goToLabel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(goToLabel);
    }

    @Override
    public String goToLabel() {
        return goToLabel;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeStatement(this);
        visitor.afterStatement(this);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.empty();
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }
}
