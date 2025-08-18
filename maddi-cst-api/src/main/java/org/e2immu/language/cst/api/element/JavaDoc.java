package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.Arrays;
import java.util.List;
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
            .collect(Collectors.toUnmodifiableMap(v -> v.identifier, v -> v));

    static TagIdentifier identifier(String string) {
        return TAG_IDENTIFIER_MAP.get(string);
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
