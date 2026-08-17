package io.codelaser.maddi.modification.prepwork.variable;

import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.stream.Stream;

public interface VirtualFieldTranslationMap extends TranslationMap {
    void put(TypeParameter in, ParameterizedType out);

    Stream<Link> upgrade(Link link, Link tLink, FieldReference fr);
}
