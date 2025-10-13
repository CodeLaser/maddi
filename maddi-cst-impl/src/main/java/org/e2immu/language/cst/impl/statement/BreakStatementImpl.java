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

package org.e2immu.language.cst.impl.statement;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.BreakStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.SymbolEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Objects;

public class BreakStatementImpl extends BreakOrContinueStatementImpl implements BreakStatement {
    public BreakStatementImpl(List<Comment> comments, Source source,
                              List<AnnotationExpression> annotationExpressions, String label, String goToLabel) {
        super(comments, source, annotationExpressions, label, goToLabel);
    }

    public static class Builder extends StatementImpl.Builder<BreakStatement.Builder> implements BreakStatement.Builder {

        private String goToLabel;

        @Override
        public Builder setGoToLabel(String goToLabel) {
            this.goToLabel = goToLabel;
            return this;
        }

        @Override
        public BreakStatement build() {
            return new BreakStatementImpl(comments, source, annotations, label, goToLabel);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BreakStatement bs && Objects.equals(goToLabel(), bs.goToLabel());
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = outputBuilder(qualification).add(KeywordImpl.BREAK);
        if (goToLabel() != null) {
            outputBuilder.add(SpaceEnum.ONE).add(new TextImpl(goToLabel()));
        }
        outputBuilder.add(SymbolEnum.SEMICOLON);
        return outputBuilder;
    }

    @Override
    public List<Statement> translate(TranslationMap translationMap) {
        List<Statement> direct = translationMap.translateStatement(this);
        if (hasBeenTranslated(direct, this)) return direct;
        List<AnnotationExpression> tAnnotations = translateAnnotations(translationMap);
        if (!analysis().isEmpty() && translationMap.isClearAnalysis() || tAnnotations != annotations()) {
            BreakStatement bs = new BreakStatementImpl(comments(), source(), tAnnotations, label(), goToLabel());
            return List.of(bs);
        }
        return List.of(this);
    }

    @Override
    public BreakStatement withSource(Source newSource) {
        return new BreakStatementImpl(comments(), newSource, annotations(), label(), goToLabel());
    }

    @Override
    public Statement rewire(InfoMap infoMap) {
        return new BreakStatementImpl(comments(), source(), rewireAnnotations(infoMap), label(), goToLabel());
    }
}
