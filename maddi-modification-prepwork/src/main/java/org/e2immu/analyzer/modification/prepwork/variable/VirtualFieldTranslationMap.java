package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;

import java.util.stream.Stream;

public interface VirtualFieldTranslationMap extends TranslationMap {
    void put(TypeParameter in, ParameterizedType out);

    Stream<Link> upgrade(Link link, Link tLink, FieldReference fr);
}
