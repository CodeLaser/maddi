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

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Precedence;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.SwitchEntry;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.expression.util.InternalCompareToException;
import io.codelaser.maddi.cst.impl.expression.util.PrecedenceEnum;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SwitchExpressionImpl extends ExpressionImpl implements SwitchExpression {
    private final Expression selector;
    private final List<SwitchEntry> entries;
    private final ParameterizedType parameterizedType;

    public SwitchExpressionImpl(List<Comment> comments, Source source, Expression selector,
                                List<SwitchEntry> entries, ParameterizedType parameterizedType) {
        super(comments, source,
                10 + selector.complexity() + entries.stream().mapToInt(SwitchEntry::complexity).sum());
        this.selector = selector;
        this.entries = entries;
        this.parameterizedType = parameterizedType;
    }

    @Override
    public Expression withSource(Source source) {
        return new SwitchExpressionImpl(comments(), source, selector, entries, parameterizedType);
    }

    public static class BuilderImpl extends ElementImpl.Builder<SwitchExpression.Builder>
            implements SwitchExpression.Builder {
        private Expression selector;
        private final List<SwitchEntry> entries = new ArrayList<>();
        private ParameterizedType parameterizedType;

        @Override
        public SwitchExpression.Builder setParameterizedType(ParameterizedType parameterizedType) {
            this.parameterizedType = parameterizedType;
            return this;
        }

        @Override
        public SwitchExpression.Builder setSelector(Expression selector) {
            this.selector = selector;
            return this;
        }

        @Override
        public SwitchExpression.Builder addSwitchEntries(Collection<SwitchEntry> switchEntries) {
            this.entries.addAll(switchEntries);
            return this;
        }

        @Override
        public SwitchExpression build() {
            return new SwitchExpressionImpl(comments, source, selector, List.copyOf(entries), parameterizedType);
        }
    }

    @Override
    public Expression selector() {
        return selector;
    }

    @Override
    public List<SwitchEntry> entries() {
        return entries;
    }

    @Override
    public SwitchExpression withSelector(Expression newSelector) {
        return new SwitchExpressionImpl(comments(), source(), newSelector, entries, parameterizedType);
    }

    @Override
    public ParameterizedType parameterizedType() {
        return parameterizedType;
    }

    @Override
    public Precedence precedence() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public int order() {
        return ExpressionComparator.ORDER_SWITCH;
    }

    @Override
    public int internalCompareTo(Expression expression) {
        if (expression instanceof SwitchExpression se) {
            int c = selector.compareTo(se.selector());
            if (c != 0) return c;
        }
        throw new InternalCompareToException();
    }

    @Override
    public Expression translate(TranslationMap translationMap) {
        Expression translated = translationMap.translateExpression(this);
        if (translated == null) return this;
        if (translated != this) return translated;

        Expression trSelector = selector.translate(translationMap);
        List<SwitchEntry> translatedSwitchEntries = entries.stream()
                .map(se -> se.translate(translationMap)).toList();
        ParameterizedType trType = translationMap.translateType(parameterizedType);
        if (trSelector == selector && translatedSwitchEntries == entries && trType == parameterizedType) {
            return this;
        }
        Expression result = new SwitchExpressionImpl(comments(), source(), trSelector, translatedSwitchEntries, trType);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        selector.visit(predicate);
        int i = 0;
        for (SwitchEntry entry : entries) {
            entry.conditions().forEach(e -> e.visit(predicate));
            if (entry.patternVariable() != null) entry.patternVariable().visit(predicate);
            entry.whenExpression().visit(predicate);
            entry.statement().visit(predicate);
            i++;
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeExpression(this)) {
            selector.visit(visitor);
            int i = 0;
            for (SwitchEntry entry : entries) {
                entry.conditions().forEach(e -> e.visit(visitor));
                if (entry.patternVariable() != null) entry.patternVariable().visit(visitor);
                entry.whenExpression().visit(visitor);
                visitor.startSubBlock(i);
                entry.statement().visit(visitor);
                visitor.endSubBlock(i);
                i++;
            }
        }
        visitor.afterExpression(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder outputBuilder = new OutputBuilderImpl().add(KeywordImpl.SWITCH)
                .add(SymbolEnum.LEFT_PARENTHESIS_AFTER_KEYWORD)
                .add(selector.print(qualification))
                .add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(SymbolEnum.LEFT_BRACE);
        GuideImpl.GuideGenerator guideGenerator = GuideImpl.generatorForBlock();
        outputBuilder.add(guideGenerator.start());
        int i = 0;
        for (SwitchEntry entry : entries) {
            if (i > 0) outputBuilder.add(guideGenerator.mid());
            outputBuilder.add(entry.print(qualification));
            i++;
        }
        return outputBuilder.add(guideGenerator.end()).add(SymbolEnum.RIGHT_BRACE);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(selector.variables(descendMode),
                entries.stream().flatMap(e -> e.variables(descendMode)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return Stream.concat(selector.typesReferenced(predicate), entries.stream().flatMap(switchEntry -> switchEntry.typesReferenced(predicate)));
    }

    @Override
    public Expression rewire(InfoMapView infoMap) {
        return new SwitchExpressionImpl(comments(), source(), selector.rewire(infoMap),
                entries.stream().map(e ->(SwitchEntry) e.rewire(infoMap)).toList(), parameterizedType.rewire(infoMap));
    }
}
