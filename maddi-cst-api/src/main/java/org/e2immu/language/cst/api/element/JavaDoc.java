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

package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.info.InfoMapView;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;

/**
 * A structured {@code /** … *}{@code /} Javadoc comment, parsed into a list of {@link Tag} entries.
 * <p>
 * Extends {@link MultiLineComment} with structured access to block and inline Javadoc tags
 * (e.g. {@code @param}, {@code @return}, {@code {@link …}}) via {@link #tags()}.
 * References inside tags (e.g. the type in {@code @throws} or {@code {@link}}) are resolved
 * to the corresponding {@link Element} during inspection.
 */
public interface JavaDoc extends MultiLineComment {

    /** All recognised Javadoc block and inline tag kinds. */
    enum TagIdentifier {
        AUTHOR("author"),
        CODE("code"),
        DEPRECATED("deprecated"),
        DOC_ROOT("docRoot"),
        EXCEPTION("exception"),
        HIDDEN("hidden"),
        INDEX("index"),
        INHERIT_DOC("inheritDoc"),
        LINK("link"),
        LINK_PLAIN("linkplain"),
        LITERAL("literal"),
        PARAM("param"),
        PROVIDES("provides"),
        RETURN("return"),
        SEE("see"),
        SERIAL("serial"),
        SERIAL_DATA("serialData"),
        SERIAL_FIELD("serialField"),
        SINCE("since"),
        SNIPPET("snippet"),
        SPEC("spec"),
        SUMMARY("summary"),
        SYSTEM_PROPERTY("systemProperty"),
        THROWS("throws"),
        USES("uses"),
        VALUE("value"),
        VERSION("version"),
        UNKNOWN_INLINE_TAG("unknownInlineTag"),
        UNKNOWN_BLOCK_TAG("unknownBlockTag");

        public final String identifier;

        TagIdentifier(String identifier) {
            this.identifier = identifier;
        }

        /** Returns {@code true} if this tag carries a code reference (e.g. {@code @see}, {@code {@link}}). */
        public boolean isReference() {
            return this == SEE || this == LINK || this == LINK_PLAIN || this == THROWS;
        }

        /**
         * Returns the number of leading arguments when this tag is used as a block tag.
         * {@code @param} and {@code @throws} each take one argument (the parameter/type name).
         */
        public int argumentsAsBlockTag() {
            if (this == PARAM || this == THROWS || isReference()) return 1;
            return 0;
        }
    }

    /** A single parsed tag inside a Javadoc comment. */
    interface Tag {
        /** Returns the kind of this tag. */
        TagIdentifier identifier();

        /** Returns {@code true} if this is a block tag (starts at column 1 with {@code @}). */
        boolean blockTag();

        /** Returns the source position of this tag. */
        Source source();

        /** Returns the source position of the reference argument inside this tag, if any. */
        Source sourceOfReference();

        /** Returns the CST element the reference in this tag was resolved to, or {@code null}. */
        Element resolvedReference();

        /** Returns the text content of this tag. */
        String content();

        Tag rewire(InfoMapView infoMap);

        Tag translate(TranslationMap translationMap);

        /** Returns a copy of this tag with the given resolved reference. */
        Tag withResolvedReference(Element resolvedReference);

        /** Returns a copy of this tag with the given source position. */
        Tag withSource(Source source);
    }

    /** Returns the list of all tags in this Javadoc comment, in source order. */
    List<Tag> tags();

    /** Returns a translated copy of this Javadoc comment as described by {@code translationMap}. */
    JavaDoc translate(TranslationMap translationMap);

    /** Returns a copy of this Javadoc comment with a different tag list. */
    JavaDoc withTags(List<Tag> newTags);

    /**
     * Returns the raw Javadoc text with inline tag references replaced by placeholders,
     * for use in serialisation or diff computation.
     */
    String commentWithPlaceholders();

}
