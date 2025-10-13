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
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public interface JavaDoc extends MultiLineComment {

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
        VERSION("version");

        public final String identifier;

        TagIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public boolean isReference() {
            return this == SEE || this == LINK || this == LINK_PLAIN || this == THROWS;
        }

        public int argumentsAsBlockTag() {
            if (this == PARAM || this == THROWS || isReference()) return 1;
            return 0;
        }
    }

    Map<String, TagIdentifier> TAG_IDENTIFIER_MAP = Arrays.stream(TagIdentifier.values())
            .collect(Collectors.toUnmodifiableMap(v -> v.identifier.toLowerCase(), v -> v));

    static TagIdentifier identifier(String string) {
        return TAG_IDENTIFIER_MAP.get(string.toLowerCase());
    }


    interface Tag {
        TagIdentifier identifier();

        boolean blockTag();

        Source source();

        Source sourceOfReference();

        Element resolvedReference();

        String content();

        Tag rewire(InfoMap infoMap);

        Tag translate(TranslationMap translationMap);

        Tag withResolvedReference(Element resolvedReference);

        Tag withSource(Source source);
    }

    List<Tag> tags();

    JavaDoc translate(TranslationMap translationMap);

    JavaDoc withTags(List<Tag> newTags);

    String commentWithPlaceholders();

}
