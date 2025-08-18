package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.BreakOrContinueStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;

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
    public Stream<Element.TypeReference> typesReferenced() {
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
