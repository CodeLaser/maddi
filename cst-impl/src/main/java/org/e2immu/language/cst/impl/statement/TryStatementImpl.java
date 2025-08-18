package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
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
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.type.DiamondEnum;
import org.e2immu.util.internal.util.ZipLists;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TryStatementImpl extends StatementImpl implements TryStatement {
    private final Block block;
    private final Block finallyBlock;
    private final List<CatchClause> catchClauses;
    private final List<Statement> resources; // either LVC, or VE

    public TryStatementImpl(List<Comment> comments,
                            Source source,
                            List<AnnotationExpression> annotations,
                            String label,
                            List<Statement> resources,
                            Block block,
                            List<CatchClause> catchClauses,
                            Block finallyBlock) {
        super(comments, source, annotations, 1 + block.complexity() + finallyBlock.complexity()
                                             + catchClauses.stream().mapToInt(CatchClause::complexity).sum(), label);
        this.block = Objects.requireNonNull(block);
        this.finallyBlock = Objects.requireNonNull(finallyBlock);
        this.catchClauses = Objects.requireNonNull(catchClauses);
        this.resources = Objects.requireNonNull(resources);
    }

    @Override
    public Statement withBlocks(List<Block> tSubBlocks) {
        List<CatchClause> tCatchClauses;
        if (catchClauses.isEmpty()) {
            tCatchClauses = List.of();
        } else {
            tCatchClauses = ZipLists.zip(catchClauses, tSubBlocks.subList(1, tSubBlocks.size() - 1))
                    .map(z -> z.x().withBlock(z.y()))
                    .collect(TranslationMap.staticToList(catchClauses));
        }
        return new TryStatementImpl(comments(), source(), annotations(), label(), resources, tSubBlocks.get(0),
                tCatchClauses, tSubBlocks.get(tSubBlocks.size() - 1));
    }

    public static class CatchClauseImpl extends ElementImpl implements CatchClause {
        private final List<ParameterizedType> exceptionTypes;
        private final boolean isFinal;
        private final LocalVariable catchVariable;
        private final Block block;
        private final Source source;
        private final List<Comment> comments;
        private final List<AnnotationExpression> annotations;
        private final PropertyValueMap propertyValueMap = new PropertyValueMapImpl();

        public CatchClauseImpl(List<Comment> comments,
                               Source source,
                               List<AnnotationExpression> annotations,
                               List<ParameterizedType> exceptionTypes, boolean isFinal, LocalVariable catchVariable, Block block) {
            this.comments = comments;
            this.annotations = annotations;
            this.source = source;
            this.catchVariable = catchVariable;
            this.exceptionTypes = exceptionTypes;
            this.block = block;
            this.isFinal = isFinal;
        }

        @Override
        public CatchClause withBlock(Block newBlock) {
            return new CatchClauseImpl(comments, source, annotations, exceptionTypes, isFinal, catchVariable, newBlock);
        }

        @Override
        public CatchClause rewire(InfoMap infoMap) {
            return new CatchClauseImpl(comments, source,
                    annotations.stream().map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList(),
                    exceptionTypes.stream().map(et -> et.rewire(infoMap)).toList(),
                    isFinal, (LocalVariable) catchVariable.rewire(infoMap), block.rewire(infoMap));
        }

        public static class Builder extends ElementImpl.Builder<CatchClause.Builder> implements CatchClause.Builder {
            private final List<ParameterizedType> exceptionTypes = new ArrayList<>();
            private LocalVariable catchVariable;
            private boolean isFinal;
            private Block block;

            @Override
            public Builder setBlock(Block block) {
                this.block = block;
                return this;
            }

            @Override
            public CatchClause.Builder addType(ParameterizedType type) {
                exceptionTypes.add(type);
                return this;
            }

            @Override
            public Builder setCatchVariable(LocalVariable catchVariable) {
                this.catchVariable = catchVariable;
                return this;
            }

            @Override
            public Builder setFinal(boolean isFinal) {
                this.isFinal = isFinal;
                return this;
            }

            @Override
            public CatchClause build() {
                return new CatchClauseImpl(comments, source, annotations, exceptionTypes, isFinal, catchVariable, block);
            }
        }

        @Override
        public boolean isFinal() {
            return isFinal;
        }

        @Override
        public LocalVariable catchVariable() {
            return catchVariable;
        }

        @Override
        public List<ParameterizedType> exceptionTypes() {
            return exceptionTypes;
        }

        @Override
        public Block block() {
            return block;
        }

        @Override
        public int complexity() {
            return exceptionTypes.size() + block.complexity();
        }

        @Override
        public List<Comment> comments() {
            return comments;
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public List<AnnotationExpression> annotations() {
            return annotations;
        }

        @Override
        public Stream<Element.TypeReference> typesReferenced() {
            return Stream.concat(exceptionTypes.stream()
                            .map(et -> new ElementImpl.TypeReference(et.typeInfo(), true)),
                    block.typesReferenced());
        }

        @Override
        public Stream<Variable> variables(DescendMode descendMode) {
            return block.variables(descendMode);
        }

        @Override
        public void visit(Predicate<Element> predicate) {
            if (predicate.test(this)) {
                block.visit(predicate);
            }
        }

        @Override
        public void visit(Visitor visitor) {
            block.visit(visitor);
        }

        @Override
        public OutputBuilder print(Qualification qualification) {
            return null;
        }

        @Override
        public CatchClause translate(TranslationMap translationMap) {
            List<ParameterizedType> list = exceptionTypes.stream()
                    .map(translationMap::translateType).collect(translationMap.toList(exceptionTypes));
            Block tBlock = (Block) block.translate(translationMap).get(0);
            if (list != exceptionTypes || tBlock != block || translationMap.isClearAnalysis()) {
                CatchClause cc = new CatchClauseImpl(comments, source, annotations, list, isFinal, catchVariable, tBlock);
                if (!translationMap.isClearAnalysis()) cc.analysis().setAll(analysis());
                return cc;
            }
            return this;
        }

        @Override
        public PropertyValueMap analysis() {
            return propertyValueMap;
        }
    }

    public static class Builder extends StatementImpl.Builder<TryStatement.Builder> implements TryStatement.Builder {
        private Block block;
        private Block finallyBlock;
        private final List<CatchClause> catchClauses = new ArrayList<>();
        private final List<Statement> resources = new ArrayList<>();

        @Override
        public TryStatement.Builder setBlock(Block block) {
            this.block = block;
            return this;
        }

        @Override
        public TryStatement.Builder setFinallyBlock(Block block) {
            this.finallyBlock = block;
            return this;
        }

        @Override
        public TryStatement.Builder addCatchClause(CatchClause catchClause) {
            catchClauses.add(catchClause);
            return this;
        }

        @Override
        public TryStatement.Builder addResource(Statement resource) {
            resources.add(resource);
            return this;
        }

        @Override
        public TryStatement build() {
            return new TryStatementImpl(comments, source, annotations, label, List.copyOf(resources), block, List.copyOf(catchClauses), finallyBlock
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TryStatementImpl that)) return false;
        return Objects.equals(block, that.block) && Objects.equals(finallyBlock, that.finallyBlock)
               && Objects.equals(catchClauses, that.catchClauses) && Objects.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, finallyBlock, catchClauses, resources);
    }

    @Override
    public Block finallyBlock() {
        return finallyBlock;
    }

    @Override
    public List<CatchClause> catchClauses() {
        return catchClauses;
    }

    @Override
    public List<Statement> resources() {
        return resources;
    }

    @Override
    public Block block() {
        return block;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            resources.forEach(r -> r.visit(predicate));
            block.visit(predicate);
            catchClauses.forEach(cc -> cc.visit(predicate));
            if (!finallyBlock.isEmpty()) finallyBlock.visit(predicate);
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeStatement(this)) {
            // we'll use -1 as a special indicator for the resources block
            if (!resources.isEmpty()) {
                visitor.startSubBlock(-1);
                resources.forEach(e -> e.visit(visitor));
                visitor.endSubBlock(-1);
            }
            int i = 0;
            visitor.startSubBlock(i);
            block.visit(visitor);
            visitor.endSubBlock(i);
            for (CatchClause cc : catchClauses) {
                ++i;
                visitor.startSubBlock(i);
                cc.visit(visitor);
                cc.visit(visitor);
                visitor.endSubBlock(i);
            }
            if (!finallyBlock.isEmpty()) {
                ++i;
                visitor.startSubBlock(i);
                finallyBlock.visit(visitor);
                visitor.endSubBlock(i);
            }
        }
        visitor.afterStatement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification).add(KeywordImpl.TRY);
        if (!resources.isEmpty()) {
            Statement last = resources.get(resources.size() - 1);
            outputBuilder.add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(resources.stream().map(st -> {
                                OutputBuilder ob = st.print(qualification);
                                if (st == last) {
                                    ob.removeLast();
                                }
                                return ob;
                            })
                            .collect(OutputBuilderImpl.joining()))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        outputBuilder.add(block.print(qualification));
        for (CatchClause cc : catchClauses) {
            outputBuilder.add(KeywordImpl.CATCH)
                    .add(SymbolEnum.LEFT_PARENTHESIS);
            if (cc.isFinal()) {
                outputBuilder.add(KeywordImpl.FINAL).add(SpaceEnum.ONE);
            }
            outputBuilder
                    .add(cc.exceptionTypes().stream()
                            .map(t -> t.print(qualification, false, DiamondEnum.NO))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.PIPE)))
                    .add(SpaceEnum.ONE)
                    .add(cc.catchVariable().print(qualification))
                    .add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(cc.block().print(qualification));
        }
        if (!finallyBlock.isEmpty() || catchClauses.isEmpty() && resources.isEmpty()) {
            outputBuilder
                    .add(KeywordImpl.FINALLY)
                    .add(finallyBlock.print(qualification));
        }
        return outputBuilder;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(resources.stream().flatMap(r -> r.variables(descendMode)),
                Stream.concat(block.variables(descendMode), Stream.concat(
                        catchClauses.stream().flatMap(cc -> cc.variables(descendMode)),
                        finallyBlock.variables(descendMode))));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(resources.stream().flatMap(Element::typesReferenced),
                Stream.concat(block.typesReferenced(), Stream.concat(catchClauses.stream().flatMap(CatchClause::typesReferenced),
                        finallyBlock.typesReferenced())));
    }

    @Override
    public Stream<Block> otherBlocksStream() {
        return Stream.concat(catchClauses.stream().map(CatchClause::block), Stream.of(finallyBlock));
    }

    @Override
    public boolean hasSubBlocks() {
        return true;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        Block tMain = (Block) block.translate(translationMap).get(0);
        Block tFinally = (Block) finallyBlock.translate(translationMap).get(0);
        List<Statement> tResources = resources.stream()
                .map(st -> {
                    List<Statement> translated = st.translate(translationMap);
                    return translated.isEmpty() ? null : translated.get(0);
                })
                .filter(Objects::nonNull)
                .collect(translationMap.toList(resources));
        List<CatchClause> tCatch = catchClauses.stream()
                .map(cc -> cc.translate(translationMap))
                .collect(translationMap.toList(catchClauses));
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (tMain != block || tFinally != finallyBlock || tCatch != catchClauses
            || tResources != resources || !analysis().isEmpty() && translationMap.isClearAnalysis()
            || tAnnotations != annotations()) {
            TryStatementImpl ts = new TryStatementImpl(comments(), source(), tAnnotations, label(), tResources,
                    tMain, tCatch, tFinally);
            if (!translationMap.isClearAnalysis()) ts.analysis().setAll(analysis());
            return List.of(ts);
        }
        return List.of(this);
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new TryStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(),
                resources.stream().map(s -> s.rewire(infoMap)).toList(),
                block.rewire(infoMap),
                catchClauses.stream().map(cc -> cc.rewire(infoMap)).toList(),
                finallyBlock.rewire(infoMap));
    }
}
