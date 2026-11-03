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
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Convention: the "comment" string in {@link MultiLineCommentImpl} may hold a {@link JavaDoc#placeholder} for each tag
 * in this object, which {@link #comment()} replaces by the tag as written. The maddi parser writes them; the openjdk
 * front end writes its tags as they were written, and has none.
 */
public class JavaDocImpl extends MultiLineCommentImpl implements JavaDoc {

    private static final Pattern PLACEHOLDER = Pattern.compile(JavaDoc.PLACEHOLDER_OPEN + "(\\d+)"
                                                               + JavaDoc.PLACEHOLDER_CLOSE);

    public static class TagImpl implements Tag {
        private final TagIdentifier tagIdentifier;
        private final String content;
        private final Element resolvedReference;
        private final Source source;
        private final Source sourceOfReference;
        private final boolean blockTag;
        private final List<TypeInfo> referencedParameterTypes;

        public TagImpl(TagIdentifier tagIdentifier, String content, Element resolvedReference, Source source,
                       Source sourceOfReference,
                       boolean blockTag) {
            this(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag, List.of());
        }

        public TagImpl(TagIdentifier tagIdentifier, String content, Element resolvedReference, Source source,
                       Source sourceOfReference,
                       boolean blockTag,
                       List<TypeInfo> referencedParameterTypes) {
            this.tagIdentifier = tagIdentifier;
            this.content = content;
            this.resolvedReference = resolvedReference;
            this.source = source;
            this.sourceOfReference = sourceOfReference;
            this.referencedParameterTypes = List.copyOf(referencedParameterTypes);
            this.blockTag = blockTag;
        }

        @Override
        public TagIdentifier identifier() {
            return tagIdentifier;
        }

        @Override
        public boolean blockTag() {
            return blockTag;
        }

        @Override
        public Source source() {
            return source;
        }

        @Override
        public Source sourceOfReference() {
            return sourceOfReference;
        }

        @Override
        public Element resolvedReference() {
            return resolvedReference;
        }

        @Override
        public List<TypeInfo> referencedParameterTypes() {
            return referencedParameterTypes;
        }

        @Override
        public String content() {
            return content;
        }

        @Override
        public Tag rewire(InfoMapView infoMap) {
            if (resolvedReference == null && referencedParameterTypes.isEmpty()) return this;
            // an Info goes through the map: TypeInfo.rewire and its siblings refuse, by design
            Element rewired = switch (resolvedReference) {
                case null -> null;
                case TypeInfo typeInfo -> infoMap.typeInfo(typeInfo);
                case MethodInfo methodInfo -> infoMap.methodInfo(methodInfo);
                case FieldInfo fieldInfo -> infoMap.fieldInfo(fieldInfo);
                default -> resolvedReference.rewire(infoMap);
            };
            List<TypeInfo> rewiredParams = referencedParameterTypes.stream().map(infoMap::typeInfo).toList();
            return new TagImpl(tagIdentifier, content, rewired, source == null ? null : source.rewire(infoMap),
                    sourceOfReference == null ? null : sourceOfReference.rewire(infoMap), blockTag, rewiredParams);
        }

        @Override
        public Tag translate(TranslationMap translationMap) {
            List<TypeInfo> translatedParams = referencedParameterTypes.stream()
                    .map(t -> translationMap.translateType(t.asSimpleParameterizedType()).typeInfo())
                    .toList();
            boolean paramsChanged = !translatedParams.equals(referencedParameterTypes);
            if (resolvedReference instanceof Info info) {
                List<? extends Info> infos = info.translate(translationMap);
                if (infos.size() != 1 || infos.getFirst() != info) {
                    return new TagImpl(tagIdentifier, content, infos.getFirst(), source, sourceOfReference, blockTag,
                            translatedParams);
                }
            }
            if (paramsChanged) {
                return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag,
                        translatedParams);
            }
            return this;
        }

        @Override
        public String toString() {
            if (blockTag) {
                return "@" + tagIdentifier.identifier + (content.isEmpty() ? "" : " " + content);
            }
            return "{@" + tagIdentifier.identifier + " " + content + "}";
        }

        @Override
        public Tag withResolvedReference(Element resolvedReference) {
            return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag,
                    referencedParameterTypes);
        }

        @Override
        public Tag withReferencedParameterTypes(List<TypeInfo> referencedParameterTypes) {
            return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag,
                    referencedParameterTypes);
        }

        @Override
        public Tag withSource(Source source) {
            return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag,
                    referencedParameterTypes);
        }
    }

    private final List<Tag> tags;

    public JavaDocImpl(Source source, String comment, List<Tag> tags) {
        super(source, comment, true);
        this.tags = tags;
    }

    @Override
    public List<Tag> tags() {
        return tags;
    }

    @Override
    public String commentWithPlaceholders() {
        return super.comment();
    }

    @Override
    public String comment() {
        String stored = super.comment();
        if (stored.indexOf(JavaDoc.PLACEHOLDER_OPEN) < 0) return stored;
        StringBuilder sb = new StringBuilder();
        Matcher m = PLACEHOLDER.matcher(stored);
        while (m.find()) {
            int tagIndex = Integer.parseInt(m.group(1));
            // a tag list that lost an entry (withTags) leaves its placeholder as it was, rather than throwing
            String replacement = tagIndex < tags.size() ? tags.get(tagIndex).toString() : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public int complexity() {
        return tags.size();
    }

    @Override
    public List<Comment> comments() {
        return List.of();
    }

    @Override
    public JavaDoc rewire(InfoMapView infoMap) {
        return new JavaDocImpl(source() == null ? null : source().rewire(infoMap), super.comment(),
                tags.stream().map(t -> t.rewire(infoMap)).toList());
    }

    @Override
    public JavaDoc translate(TranslationMap translationMap) {
        List<Tag> translatedTags = tags.stream().map(tag -> tag.translate(translationMap))
                .collect(TranslationMap.staticToList(tags));
        if (translatedTags == tags) {
            return this;
        }
        JavaDoc result = new JavaDocImpl(source(), super.comment(), translatedTags);
        return translationMap.postTranslationHandler(this, result);
    }

    @Override
    public JavaDoc withTags(List<Tag> newTags) {
        return new JavaDocImpl(source(), super.comment(), newTags);
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeJavaDoc(this);
        visitor.afterJavaDoc(this);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<Variable> variableStreamDescend() {
        return Stream.empty();
    }

    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return tags.stream().flatMap(JavaDocImpl::typesReferencedInTag).filter(Objects::nonNull);
    }

    // the reference target, plus the types named in a member reference's parameter list ({@link T#m(P)}): the file
    // needs P to resolve exactly as it needs T, so P is a reference too
    private static Stream<Element.TypeReference> typesReferencedInTag(Tag tag) {
        Stream<Element.TypeReference> params = tag.referencedParameterTypes().stream()
                .map(t -> typeReference(tag, t));
        return Stream.concat(Stream.of(typeReference(tag)), params);
    }

    private static Element.TypeReference typeReference(Tag tag) {
        if (tag.resolvedReference() instanceof Info info) {
            return typeReference(tag, info.typeInfo());
        }
        return null;
    }

    private static Element.TypeReference typeReference(Tag tag, TypeInfo typeInfo) {
        if (typeInfo != null) {
            TypeReferenceNature trn;
            TypeInfo qualifier;
            if (tag.source() == null || tag.source().detailedSources() == null) {
                trn = TypeReferenceNature.IMPLICIT;
                qualifier = null;
            } else {
                DetailedSources ds = tag.source().detailedSources();
                Source s = ds.detail(typeInfo);
                if (s == null) {
                    trn = TypeReferenceNature.IMPLICIT;
                    qualifier = null;
                } else {
                    trn = TypeReferenceNature.EXPLICIT;
                    qualifier = ds.qualifier(typeInfo);
                }
            }
            return new ElementImpl.TypeReference(typeInfo, trn, qualifier);
        }
        return null;
    }

    /**
     * ⛔ A ONE-LINE JAVADOC IS STILL A JAVADOC. {@link MultiLineCommentImpl#print} picks its delimiters on
     * whether the content holds a newline, so {@code /** a one-liner *}{@code /} was re-emitted as
     * {@code /* a one-liner *}{@code /} — legal, compiles, and invisible to the javadoc tool, which is
     * documentation silently deleted. Only the refactoring levers see it (nothing else re-prints a parsed
     * comment), and they see it on every moved member whose javadoc fits one line. The {@code /**} form is
     * this class's whole identity, so it is chosen here rather than derived from the text.
     */
    @Override
    public OutputBuilder print(Qualification qualification) {
        return multilinePrint();
    }

    private static final Pattern STAR = Pattern.compile("^\\s*\\*\\s?");

    // still here while we retain the maddi-parser
    @Deprecated
    private static boolean linesStartWithStar(String[] split) {
        int cnt = (int) Arrays.stream(split).filter(line -> STAR.matcher(line).find()).count();
        return cnt > 1;
    }

    @Override
    protected OutputBuilder multilinePrint() {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForMultilineComment();
        String text = "/**\n" + comment() + "\n/";
        String[] split = text.split("\n");
        if (linesStartWithStar(split)) return super.multilinePrint(); // maddi-parsed, rather than openjdk
        OutputBuilder firstLine = new OutputBuilderImpl().add(new TextImpl(split[0]));
        OutputBuilder joinedText = Stream.concat(Stream.of(firstLine), Arrays.stream(split).skip(1)
                        .map(line -> new OutputBuilderImpl()
                                .add(new TextImpl(Formatter.HARD_SPACE + "*" + (line.isBlank() ? ""
                                        : (line.equals("/") ? "/" : " " + line.trim()))))))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, gg));
        return new OutputBuilderImpl().add(joinedText);
    }

    @Override
    public String toString() {
        return "javaDoc@" + source().compact2();
    }
}
