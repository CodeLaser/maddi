package org.e2immu.language.cst.impl.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;

public abstract class StatementImpl extends ElementImpl implements Statement {

    private final List<Comment> comments;
    private final List<AnnotationExpression> annotations;
    private final Source source;
    private final int complexity;
    private final String label;
    private final PropertyValueMap propertyValueMap = new PropertyValueMapImpl();

    protected StatementImpl(List<Comment> comments,
                            Source source,
                            List<AnnotationExpression> annotations,
                            int complexity,
                            String label) {
        this.complexity = complexity;
        this.source = source;
        this.annotations = annotations;
        this.comments = comments;
        this.label = label;
    }

    protected StatementImpl() {
        this(List.of(), null, List.of(), 1, null);
    }

    protected OutputBuilder outputBuilder(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (!comments.isEmpty()) {
            ob.add(comments.stream()
                    .map(c -> c.print(qualification)).collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE)));
            ob.add(SpaceEnum.NEWLINE);
        }
        if (!annotations.isEmpty()) {
            ob.add(annotations().stream()
                    .map(ae -> ae.print(qualification)).collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            ob.add(SpaceEnum.NEWLINE);
        }
        if (label != null) {
            ob.add(new TextImpl(label)).add(SymbolEnum.COLON_LABEL).add(SpaceEnum.ONE_IS_NICE_EASY_SPLIT);
            ob.add(SpaceEnum.ONE);
        }
        return ob;
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
    public List<Comment> comments() {
        return comments;
    }

    @Override
    public int complexity() {
        return complexity;
    }

    @Override
    public String label() {
        return label;
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<B extends Statement.Builder<?>> extends ElementImpl.Builder<B> implements Statement.Builder<B> {
        protected String label;

        @Fluent
        public B setLabel(String label) {
            this.label = label;
            return (B) this;
        }
    }

    protected boolean hasBeenTranslated(List<Statement> resultOfTranslation, Statement statement) {
        return resultOfTranslation.size() != 1 || resultOfTranslation.get(0) != statement;
    }

    protected Block ensureBlock(List<Statement> resultOfTranslation) {
        if (resultOfTranslation.size() == 1 && resultOfTranslation.get(0) instanceof Block block) {
            return block;
        }
        return new BlockImpl.Builder().addStatements(resultOfTranslation).build();
    }

    @Override
    public boolean alwaysEscapes() {
        return analysis().getOrDefault(PropertyImpl.ALWAYS_ESCAPES, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public PropertyValueMap analysis() {
        return propertyValueMap;
    }
}
