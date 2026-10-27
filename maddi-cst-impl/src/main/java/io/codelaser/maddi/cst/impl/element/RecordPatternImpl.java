/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;

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
        this.comments = comments == null ? List.of() : List.copyOf(comments);
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
    public ParameterizedType parameterizedType() {
        if (localVariable != null) return localVariable.parameterizedType();
        if (recordType != null) return recordType;
        assert unnamedPattern;
        return null;
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
                RecordPattern result = new RecordPatternImpl(tComments, source, false, tVar,
                        null, null);
                return translationMap.postTranslationHandler(this, result);
            }
            return this;
        }
        if (recordType != null) {
            ParameterizedType tRecordType = translationMap.translateType(recordType);
            List<RecordPattern> tPatterns = patterns.stream().map(rp -> rp.translate(translationMap))
                    .collect(translationMap.toList(patterns));
            if (recordType != tRecordType || tPatterns != patterns || tComments != comments) {
                RecordPattern result = new RecordPatternImpl(tComments, source, false,
                        null, tRecordType, tPatterns);
                return translationMap.postTranslationHandler(this, result);
            }
            return this;
        }
        if (tComments != comments) {
            RecordPattern result = new RecordPatternImpl(tComments, source, true, null,
                    null, null);
            return translationMap.postTranslationHandler(this, result);
        }
        return this;
    }

    @Override
    public Element rewire(InfoMapView infoMap) {
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
                    .add(SpaceEnum.ONE);
            // ⚠ A TYPE PATTERN MAY HAVE AN UNNAMED VARIABLE: 'case Square _ ->' (Java 21). That is NOT the
            // 'unnamedPattern' above -- this class's own invariant says that one carries no localVariable at
            // all, and it is the bare '_' of a record deconstruction ('case Point(int x, _)'). Here the TYPE
            // must still be printed, followed by '_'. Without this, simpleName() is the empty string and
            // TextImpl's constructor (assert !text.isBlank()) throws, so the whole print fails.
            // Found on trino 2026-08-13 through extract.extractCompanion, which re-prints whatever it moves:
            // io.trino.util.variant.VariantWriter's switch came out as 'case VariantType ->', which javac
            // rejects with "type pattern expected". ANY verb that relocates such code hits this.
            String simpleName = localVariable.simpleName();
            if (simpleName == null || simpleName.isBlank()) {
                ob.add(SymbolEnum.UNDERSCORE);
            } else {
                ob.add(new TextImpl(simpleName));
            }
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
    public Stream<Element.TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (localVariable != null) {
            return localVariable.parameterizedType().typesReferenced(TypeReferenceNature.EXPLICIT, source.detailedSources());
        }
        if (recordType != null) {
            return Stream.concat(recordType.typesReferenced(TypeReferenceNature.EXPLICIT, source().detailedSources()),
                    patterns.stream().flatMap(recordPattern -> recordPattern.typesReferenced(predicate)));
        }
        return Stream.empty();
    }
}
