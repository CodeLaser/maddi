package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.util.internal.util.ListUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SwitchEntryImpl implements SwitchEntry {
    private final List<Expression> conditions;
    private final RecordPattern patternVariable;
    private final Expression whenExpression;
    private final Statement statement;
    private final Source source;
    private final List<Comment> comments;

    public SwitchEntryImpl(List<Comment> comments,
                           Source source,
                           List<Expression> conditions,
                           RecordPattern patternVariable,
                           Expression whenExpression,
                           Statement statement) {
        this.conditions = conditions;
        this.patternVariable = patternVariable;
        this.whenExpression = whenExpression;
        this.statement = statement;
        this.comments = comments;
        this.source = source;
    }

    @Override
    public int compareTo(SwitchEntry o) {
        return ListUtil.compare(conditions, o.conditions());
    }

    @Override
    public SwitchEntry withStatement(Statement statement) {
        return new SwitchEntryImpl(comments, source, conditions, patternVariable, whenExpression, statement);
    }

    @Override
    public int complexity() {
        return (patternVariable != null ? 1 : 0)
               + conditions.stream().mapToInt(Expression::complexity).sum()
               + whenExpression.complexity() + statement().complexity();
    }

    @Override
    public List<Comment> comments() {
        return comments;
    }

    @Override
    public List<Expression> conditions() {
        return conditions;
    }

    @Override
    public RecordPattern patternVariable() {
        return patternVariable;
    }

    @Override
    public Block statementAsBlock() {
        if (statement instanceof Block b) return b;
        // NOTE: using the statement's source is not correct index-wise, but it may be better than nothing
        return new BlockImpl(List.of(), statement.source(), List.of(), null, List.of(statement), List.of());
    }

    @Override
    public Expression whenExpression() {
        return whenExpression;
    }

    @Override
    public Statement statement() {
        return statement;
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        boolean containsDefault = conditions.stream().anyMatch(Expression::isEmpty);
        boolean first = true;
        if (containsDefault) {
            outputBuilder.add(KeywordImpl.DEFAULT);
            first = false;
            if (conditions.size() > 1) {
                outputBuilder.add(SymbolEnum.COMMA).add(KeywordImpl.CASE);
            }
        } else {
            outputBuilder.add(KeywordImpl.CASE);
            outputBuilder.add(SpaceEnum.ONE);
        }
        for (Expression condition : conditions) {
            if (!condition.isEmpty()) {
                if (first) {
                    first = false;
                } else {
                    outputBuilder.add(SymbolEnum.COMMA);
                }
                outputBuilder.add(condition.print(QualificationImpl.SIMPLE_ONLY));
            }
        }
        outputBuilder.add(SymbolEnum.LAMBDA);
        outputBuilder.add(statement().print(qualification));
        return outputBuilder;
    }

    @Override
    public SwitchEntry translate(TranslationMap translationMap) {
        List<Expression> tConditions = conditions.stream().map(c -> c.translate(translationMap))
                .collect(translationMap.toList(conditions));
        RecordPattern tPattern = patternVariable == null ? null
                : patternVariable.translate(translationMap);
        Expression tWhen = whenExpression == null ? null : whenExpression.translate(translationMap);
        List<Statement> tStatements = statement.translate(translationMap);
        Statement tStatement = tStatements.isEmpty() ? null : tStatements.getFirst();
        if (tConditions == conditions && tPattern == patternVariable && tWhen == whenExpression
            && tStatement == statement) {
            return this;
        }
        if (tStatement == null) return null; // a way for the entry to disappear
        return new SwitchEntryImpl(comments, source, tConditions, tPattern, tWhen, tStatement);
    }

    @Override
    public SwitchEntry rewire(InfoMap infoMap) {
        return new SwitchEntryImpl(comments, source,
                conditions.stream().map(e -> e.rewire(infoMap)).toList(),
                patternVariable == null ? null : (RecordPattern) patternVariable.rewire(infoMap),
                whenExpression.rewire(infoMap), statement.rewire(infoMap));
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            if (patternVariable != null) patternVariable.visit(predicate);
            whenExpression.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        // nothing a t m
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return descendMode.isYes() ? variableStreamDescend() : variableStreamDoNotDescend();
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return patternVariable == null || patternVariable.localVariable() != null
                ? Stream.of()
                : patternVariable.variableStreamDoNotDescend();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.concat(Stream.concat(patternVariable == null ? Stream.of() : patternVariable.variableStreamDescend(),
                        whenExpression.variableStreamDescend()),
                statement.variableStreamDescend());
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(whenExpression.typesReferenced(), statement.typesReferenced());
    }

    public static class EntryBuilderImpl extends ElementImpl.Builder<Builder> implements Builder {
        private final List<Expression> conditions = new ArrayList<>();
        private RecordPattern patternVariable;
        private Expression whenExpression;
        private Statement statement;

        @Override
        public Builder addConditions(Collection<Expression> expressions) {
            this.conditions.addAll(expressions);
            return this;
        }

        @Override
        public Builder setStatement(Statement statement) {
            this.statement = statement;
            return this;
        }

        @Override
        public Builder setPatternVariable(RecordPattern patternVariable) {
            this.patternVariable = patternVariable;
            return this;
        }

        @Override
        public Builder setWhenExpression(Expression whenExpression) {
            this.whenExpression = whenExpression;
            return this;
        }

        @Override
        public SwitchEntry build() {
            return new SwitchEntryImpl(List.copyOf(comments), source,
                    List.copyOf(conditions), patternVariable, whenExpression, statement);
        }
    }
}
