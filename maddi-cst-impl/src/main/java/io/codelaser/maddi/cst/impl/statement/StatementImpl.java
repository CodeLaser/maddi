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

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.PropertyValueMapImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.List;

public abstract class StatementImpl extends ElementImpl implements Statement {

    private final List<Comment> comments;
    private final List<AnnotationExpression> annotations;
    private final Source source;
    private final int complexity;
    private final String label;
    // the analysis overlay is manual hidden content (road §050), exactly as on InfoImpl and the expression
    // trio -- this was the ONE statement-side store without the annotation, and it held the whole statement
    // family at FinalFields-after-mark through the independence loop (docs/eventual-info-hierarchy.md)
    @IgnoreModifications
    private final PropertyValueMap propertyValueMap = new PropertyValueMapImpl();

    protected StatementImpl(List<Comment> comments,
                            Source source,
                            List<AnnotationExpression> annotations,
                            int complexity,
                            String label) {
        this.complexity = complexity;
        this.source = source;
        this.annotations = annotations;
        this.comments = comments == null ? List.of() : List.copyOf(comments);
        this.label = label;
    }

    protected StatementImpl() {
        this(List.of(), null, List.of(), 1, null);
    }

    protected OutputBuilder outputBuilder(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (!comments.isEmpty()) {
            ob.add(comments.stream().map(c -> c.print(qualification)).collect(OutputBuilderImpl
                    .joining(SpaceEnum.NONE, GuideImpl.multipleComments())));
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
