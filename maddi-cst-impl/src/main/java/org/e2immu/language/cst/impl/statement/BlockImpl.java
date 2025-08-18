package org.e2immu.language.cst.impl.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.GuideImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BlockImpl extends StatementImpl implements Block {
    private final List<Statement> statements;
    private final List<Comment> trailingComments;

    public BlockImpl() {
        this.statements = List.of();
        this.trailingComments = List.of();
    }

    public BlockImpl(List<Comment> comments,
                     Source source,
                     List<AnnotationExpression> annotations,
                     String label,
                     List<Statement> statements,
                     List<Comment> trailingComments) {
        super(comments, source, annotations,
                1 + statements.stream().mapToInt(Statement::complexity).sum(), label);
        this.statements = statements;
        this.trailingComments = trailingComments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockImpl block = (BlockImpl) o;
        return Objects.equals(statements, block.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(statements);
    }

    @Override
    public List<Statement> statements() {
        return statements;
    }

    @Override
    public List<Comment> trailingComments() {
        return trailingComments;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            statements.forEach(statement -> statement.visit(predicate));
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            statements.forEach(statement -> statement.visit(visitor));
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification);
        outputBuilder.add(SymbolEnum.LEFT_BRACE);
        if (!statements.isEmpty() || !trailingComments.isEmpty()) {
            Stream<OutputBuilder> stream1 = statements.stream()
                    .filter(s -> !s.isSynthetic())
                    .map(s -> s.print(qualification));
            Stream<OutputBuilder> stream2 = trailingComments.stream()
                    .map(c -> c.print(qualification));
            outputBuilder.add(Stream.concat(stream1, stream2)
                    .collect(OutputBuilderImpl.joining(SpaceEnum.NONE, GuideImpl.generatorForBlock())));
        }

        outputBuilder.add(SymbolEnum.RIGHT_BRACE);
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return statements.stream().flatMap(s -> s.variables(descendMode));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return statements.stream().flatMap(Statement::typesReferenced);
    }

    public static class Builder extends StatementImpl.Builder<Block.Builder> implements Block.Builder {
        private final List<Statement> statements = new ArrayList<>();
        private final List<Comment> trailingComments = new ArrayList<>();

        @Override
        public Builder addTrailingComments(List<Comment> trailingComments) {
            this.trailingComments.addAll(trailingComments);
            return this;
        }

        @Override
        @Fluent
        public Builder addStatement(Statement statement) {
            assert statement != null;
            statements.add(statement);
            return this;
        }

        @Override
        @Fluent
        public Builder addStatements(List<Statement> statements) {
            this.statements.addAll(statements);
            return this;
        }

        @Override
        @Fluent
        public Builder addStatement(int index, Statement statement) {
            assert statement != null;
            statements.add(index, statement);
            return this;
        }

        @Override
        @Fluent
        public Builder addStatements(int index, List<Statement> statements) {
            this.statements.addAll(index, statements);
            return this;
        }

        @Override
        public Block build() {
            return new BlockImpl(comments, source, annotations, label,
                    List.copyOf(statements), List.copyOf(trailingComments));
        }

        @Override
        public List<Statement> statements() {
            return statements;
        }
    }

    @Override
    public boolean hasSubBlocks() {
        return true;
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        return tSubBlocks.get(0);
    }

    @Override
    public Block block() {
        return this;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) {
            assert direct.size() == 1 && direct.get(0) instanceof Block;
            return direct;
        }
        boolean change = !analysis().isEmpty() && translationMap.isClearAnalysis();
        List<Statement> tStatements = new ArrayList<>(2 * statements.size());
        for (Statement statement : statements) {
            List<Statement> tStatement = statement.translate(translationMap);
            tStatements.addAll(tStatement);
            change |= tStatement.size() != 1 || tStatement.get(0) != statement;
        }
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        change |= tAnnotations != annotations();
        if (change) {
            Block newB = new BlockImpl(comments(), source(), tAnnotations, label(), tStatements, trailingComments);
            if (!translationMap.isClearAnalysis()) newB.analysis().setAll(analysis());
            return List.of(newB);
        }
        return List.of(this);
    }

    @Override
    public Block remove(Statement toRemove) {
        List<Statement> newList = new ArrayList<>(statements.size());
        for (Statement statement : statements) {
            if (!toRemove.equals(statement)) {
                if (statement instanceof Block b) {
                    newList.add(b.remove(toRemove));
                } else {
                    newList.add(statement);
                }
            }
        }
        return new BlockImpl(comments(), source(), annotations(), label(), newList, trailingComments);
    }

    @Override
    public Statement findStatementByIndex(String index) {
        return findStatementByIndex(index, 0);
    }

    private Statement findStatementByIndex(String index, int pos) {
        int dot = index.indexOf('.', pos);
        String sub = index.substring(pos, dot < 0 ? index.length() : dot);
        Statement statement = statements.get(Integer.parseInt(sub));
        if (dot < 0) {
            return statement;
        }
        int dot2 = index.indexOf('.', dot + 1);
        int blockIndex = Integer.parseInt(index.substring(dot + 1, dot2 < 0 ? index.length() : dot2));
        Block block = blockIndex == 0 ? statement.block()
                : statement.subBlockStream().skip(blockIndex).findFirst().orElseThrow();
        if (dot2 < 0) return block;
        return ((BlockImpl) block).findStatementByIndex(index, dot2 + 1);
    }

    @Override
    public Block rewire(InfoMap infoMap) {
        return new BlockImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                statements.stream().map(s -> s.rewire(infoMap)).toList(), trailingComments);
    }
}
