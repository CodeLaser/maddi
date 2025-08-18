package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RecordPatternImpl extends ElementImpl implements RecordPattern {
    private final List<Comment> comments;
    private final Source source;

    private final boolean unnamedPattern;
    private final LocalVariable localVariable;
    private final ParameterizedType recordType;
    private final List<RecordPattern> patterns;

    public RecordPatternImpl(List<Comment> comments, Source source,
                             boolean unnamedPattern,
                             LocalVariable localVariable,
                             ParameterizedType recordType,
                             List<RecordPattern> patterns) {
        this.comments = comments;
        this.source = source;
        this.unnamedPattern = unnamedPattern;
        this.localVariable = localVariable;
        this.recordType = recordType;
        this.patterns = patterns;
        assert unnamedPattern && localVariable == null && recordType == null && patterns == null
               || !unnamedPattern && localVariable != null && recordType == null && patterns == null
               || !unnamedPattern && localVariable == null && recordType != null && patterns != null;
    }

    public static class Builder extends ElementImpl.Builder<RecordPattern.Builder> implements RecordPattern.Builder {
        private boolean unnamedPattern;
        private LocalVariable localVariable;
        private ParameterizedType recordType;
        private List<RecordPattern> patterns;

        @Override
        public Builder setUnnamedPattern(boolean unnamedPattern) {
            this.unnamedPattern = unnamedPattern;
            return this;
        }

        @Override
        public Builder setLocalVariable(LocalVariable localVariable) {
            this.localVariable = localVariable;
            return this;
        }

        @Override
        public Builder setRecordType(ParameterizedType recordType) {
            this.recordType = recordType;
            return this;
        }

        @Override
        public Builder setPatterns(List<RecordPattern> patterns) {
            this.patterns = patterns;
            return this;
        }

        @Override
        public RecordPattern build() {
            return new RecordPatternImpl(comments, source, unnamedPattern, localVariable, recordType, patterns);
        }
    }

    @Override
    public boolean unnamedPattern() {
        return unnamedPattern;
    }

    @Override
    public LocalVariable localVariable() {
        return localVariable;
    }

    @Override
    public ParameterizedType recordType() {
        return recordType;
    }

    @Override
    public List<RecordPattern> patterns() {
        return patterns;
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public List<Comment> comments() {
        return comments;
    }

    @Override
    public RecordPattern translate(TranslationMap translationMap) {
        List<Comment> tComments = translateComments(translationMap);
        if (localVariable != null) {
            LocalVariable tVar = localVariable.translate(translationMap);
            if (tVar != localVariable || tComments != comments) {
                return new RecordPatternImpl(tComments, source, false, tVar, null, null);
            }
            return this;
        }
        if (recordType != null) {
            ParameterizedType tRecordType = translationMap.translateType(recordType);
            List<RecordPattern> tPatterns = patterns.stream().map(rp -> rp.translate(translationMap))
                    .collect(translationMap.toList(patterns));
            if (recordType != tRecordType || tPatterns != patterns || tComments != comments) {
                return new RecordPatternImpl(tComments, source, false, null, tRecordType, tPatterns);
            }
            return this;
        }
        if (tComments != comments) {
            return new RecordPatternImpl(tComments, source, true, null, null, null);
        }
        return this;
    }

    @Override
    public Element rewire(InfoMap infoMap) {
        if (localVariable != null) {
            return new RecordPatternImpl(comments, source, false,
                    (LocalVariable) localVariable.rewire(infoMap), null, null);
        }
        if (recordType != null) {
            return new RecordPatternImpl(rewireComments(infoMap), source, false, null,
                    recordType.rewire(infoMap),
                    patterns.stream().map(rp -> (RecordPattern) rp.rewire(infoMap)).toList());
        }
        return this;
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        if (predicate.test(this)) {
            if (recordType != null) {
                patterns.forEach(p -> p.visit(predicate));
            }
        }
    }

    @Override
    public void visit(Visitor visitor) {
        if (visitor.beforeElement(this)) {
            if (recordType != null) {
                patterns.forEach(p -> p.visit(visitor));
            }
        }
        visitor.afterElement(this);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        OutputBuilder ob = new OutputBuilderImpl();
        if (unnamedPattern) ob.add(SymbolEnum.UNDERSCORE);
        else if (localVariable != null) {
            ob.add(localVariable.parameterizedType().print(qualification, false, DiamondEnum.SHOW_ALL))
                    .add(SpaceEnum.ONE).add(new TextImpl(localVariable.simpleName()));
        } else {
            ob.add(recordType.print(qualification, false, DiamondEnum.SHOW_ALL))
                    .add(patterns.stream()
                            .map(p -> p.print(qualification))
                            .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                    SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
        }
        return ob;
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.concat(Stream.ofNullable(localVariable),
                patterns == null ? Stream.of() :
                        patterns.stream().flatMap(p -> p.variables(descendMode)));
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return Stream.empty();
    }
}
