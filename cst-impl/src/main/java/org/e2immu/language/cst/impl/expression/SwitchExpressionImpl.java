package org.e2immu.language.cst.impl.expression;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.element.Visitor;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Precedence;
import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.element.ElementImpl;
import org.e2immu.language.cst.impl.expression.util.ExpressionComparator;
import org.e2immu.language.cst.impl.expression.util.InternalCompareToException;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.output.GuideImpl;
import org.e2immu.language.cst.impl.output.KeywordImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SymbolEnum;

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
        return new SwitchExpressionImpl(comments(), source(), trSelector, translatedSwitchEntries, trType);
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
                .add(SymbolEnum.LEFT_PARENTHESIS)
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
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.concat(selector.typesReferenced(), entries.stream().flatMap(SwitchEntry::typesReferenced));
    }

    @Override
    public Expression rewire(InfoMap infoMap) {
        return new SwitchExpressionImpl(comments(), source(), selector.rewire(infoMap),
                entries.stream().map(e ->(SwitchEntry) e.rewire(infoMap)).toList(), parameterizedType.rewire(infoMap));
    }
}
