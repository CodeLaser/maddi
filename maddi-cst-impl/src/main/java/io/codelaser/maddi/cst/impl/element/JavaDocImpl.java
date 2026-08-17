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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Formatter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
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
 * Convention: the "comment" string in {@link MultiLineCommentImpl} has placeholders {\\d+}, one for each tag in this object.
 */
public class JavaDocImpl extends MultiLineCommentImpl implements JavaDoc {

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
            Element rewired = resolvedReference == null ? null : resolvedReference.rewire(infoMap);
            List<TypeInfo> rewiredParams = referencedParameterTypes.stream()
                    .map(t -> (TypeInfo) t.rewire(infoMap)).toList();
            return new TagImpl(tagIdentifier, content, rewired, source, sourceOfReference, blockTag, rewiredParams);
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
        StringBuilder sb = new StringBuilder();
        Pattern p = Pattern.compile("\\{(\\d+)}");
        Matcher m = p.matcher(super.comment());
        while (m.find()) {
            int tagIndex = Integer.parseInt(m.group(1));
            Tag tag = tags.get(tagIndex);
            m.appendReplacement(sb, tag.toString());
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
        return new JavaDocImpl(source(), super.comment(), tags.stream().map(t -> t.rewire(infoMap)).toList());
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
