package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.EmptyStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EmptyStatementImpl extends StatementImpl implements EmptyStatement {

    public EmptyStatementImpl(List<Comment> comments, Source source, List<AnnotationExpression> annotations, String label) {
        super(comments, source, annotations, 1, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof EmptyStatementImpl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(1);
    }

    public static class Builder extends StatementImpl.Builder<EmptyStatement.Builder> implements EmptyStatement.Builder {

        @Override
        public EmptyStatement build() {
            return new EmptyStatementImpl(comments, source, annotations, label);
        }
    }

    @Override
    public Expression expression() {
        return null;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return outputBuilder(qualification).add(SymbolEnum.SEMICOLON);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.of();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.of();
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (!analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations())
            return List.of(new EmptyStatementImpl(comments(), source(), tAnnotations, label()));
        return List.of(this);
    }

    @Override
    public boolean hasSubBlocks() {
        return false;
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return this;// no blocks
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new EmptyStatementImpl(comments(), source(), rewireAnnotations(infoMap), label());
    }
}
