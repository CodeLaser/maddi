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

package org.e2immu.language.cst.impl.element;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.output.GuideImpl;
import org.e2immu.language.cst.impl.output.OutputBuilderImpl;
import org.e2immu.language.cst.impl.output.SpaceEnum;
import org.e2immu.language.cst.impl.output.TextImpl;

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

        public TagImpl(TagIdentifier tagIdentifier, String content, Element resolvedReference, Source source,
                       Source sourceOfReference,
                       boolean blockTag) {
            this.tagIdentifier = tagIdentifier;
            this.content = content;
            this.resolvedReference = resolvedReference;
            this.source = source;
            this.sourceOfReference = sourceOfReference;
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
        public String content() {
            return content;
        }

        @Override
        public Tag rewire(InfoMap infoMap) {
            if (resolvedReference == null) return this;
            return new TagImpl(tagIdentifier, content, (Info) resolvedReference.rewire(infoMap), source,
                    sourceOfReference, blockTag);
        }

        @Override
        public Tag translate(TranslationMap translationMap) {
            if (resolvedReference instanceof Info info) {
                List<? extends Info> infos = info.translate(translationMap);
                if (infos.size() != 1 || infos.getFirst() != info) {
                    return new TagImpl(tagIdentifier, content, infos.getFirst(), source, sourceOfReference, blockTag);
                }
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
            return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag);
        }

        @Override
        public Tag withSource(Source source) {
            return new TagImpl(tagIdentifier, content, resolvedReference, source, sourceOfReference, blockTag);
        }
    }

    private final List<Tag> tags;

    public JavaDocImpl(Source source, String comment, List<Tag> tags) {
        super(source, comment);
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
    public JavaDoc rewire(InfoMap infoMap) {
        return new JavaDocImpl(source(), super.comment(), tags.stream().map(t -> t.rewire(infoMap)).toList());
    }

    @Override
    public JavaDoc translate(TranslationMap translationMap) {
        List<Tag> translatedTags = tags.stream().map(tag -> tag.translate(translationMap))
                .collect(TranslationMap.staticToList(tags));
        if (translatedTags == tags) {
            return this;
        }
        return new JavaDocImpl(source(), super.comment(), translatedTags);
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
    public Stream<TypeReference> typesReferenced() {
        return tags.stream().map(this::typeReference).filter(Objects::nonNull);
    }

    private Element.TypeReference typeReference(Tag tag) {
        if (tag.resolvedReference() instanceof TypeInfo typeInfo) {
            return new ElementImpl.TypeReference(typeInfo, !tag.content().startsWith("#"));
        }
        return null;
    }

    @Override
    public String toString() {
        return "javaDoc@" + source().compact2();
    }

    // IMPROVE spacing! See TestJavaDoc,1
    @Override
    public OutputBuilder print(Qualification qualification) {
        GuideImpl.GuideGenerator gg = GuideImpl.generatorForMultilineComment();
        String text = "/*" + comment() + "*/";
        OutputBuilder joinedText = Arrays.stream(text.split("\n"))
                .filter(line -> !line.isBlank())
                .map(line -> new OutputBuilderImpl().add(new TextImpl(line.trim())))
                .collect(OutputBuilderImpl.joining(SpaceEnum.NEWLINE, gg));
        return new OutputBuilderImpl().add(joinedText);
    }
}
